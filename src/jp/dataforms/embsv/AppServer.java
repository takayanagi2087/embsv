package jp.dataforms.embsv;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import jp.dataforms.embsv.gui.EmbSvFrame;
import jp.dataforms.embsv.util.ContextXml;
import jp.dataforms.embsv.util.FileUtil;
import jp.dataforms.embsv.util.JarUtil;
import lombok.Data;
import lombok.Getter;

/**
 * 組み込みTomcatアプリケーションサーバー。
 * <pre>
 * Webアプリケーションの実行。
 * java -jar envsv.jar -start <warファイル|アプリケーションパス>
 * Webアプリケーションの停止。
 * java -jar envsv.jar -stop 
 * warファイルにTomcatを組み込む。
 * java -jar envsv.jar -emb <入力warファイル> <出力warファイル>
 * </pre>
 * 
 */
public class AppServer {
	/**
	 * Logger。
	 */
	private static Logger logger = LogManager.getLogger(AppServer.class);

	
	/**
	 * リソース。
	 */
	private static ResourceBundle resource = ResourceBundle.getBundle("jp.dataforms.embsv.AppServer");

	/**
	 * 実行モード。
	 */
	private enum Mode {
		HELP,
		START,
		STOP,
		EMB,
	}
	
	
	
	/**
	 * 設定情報。
	 */
	@Data
	public static class Conf {
		/**
		 * GUIの無いコマンドラインモード。
		 */
		public static final String MODE_CMDLINE = "cmdline";
		/**
		 * タスクトレイモード。
		 */
		public static final String MODE_TASKTRAY = "tasktray";
		/**
		 * ウインドモード。
		 */
		public static final String MODE_WINDOW = "window";
		
		/**
		 * コマンドラインモード。
		 */
		private String mode = MODE_CMDLINE;
		/**
		 * httpポート。
		 */
		private int port = 8080;
		/**
		 * Shutdownポート。
		 */
		private int shutdownPort = 8005;
		/**
		 * ブラウザ起動フラグ。
		 */
		private boolean browser = false;
		/**
		 * ブラウザのコマンドライン。
		 */
		private List<String> browserCommandLine = new ArrayList<String>();
	}
	
	/**
	 * 設定情報。
	 */
	@Getter
	private static Conf conf = new Conf();
	
	/**
	 * アプリケーションパス。
	 */
	private static String appPath = null;
	
	/**
	 * 入力するwarファイル。
	 */
	private static String inputWar = null;
	
	/**
	 * 出力するwarファイル。
	 */
	private static String outputWar = null;
	
	/**
	 * Context Path。
	 */
//	private String context = null;
	
	/**
	 * アプリケーションパスのリスト。
	 */
	private List<File> webAppList = null;
	
	/**
	 * Tomcat。
	 */
	private Tomcat tomcat = null; 
	
	
	/**
	 * 設定ファイルを展開します。
	 * @return 設定ファイルのパス。
	 * @throws Exception 例外。
	 */
	private static String writeConfFile() throws Exception {
		String jarname = JarUtil.getJarPath(AppServer.class);
		File jarfile = new File(jarname);
		String conf = jarfile.getParent() + File.separator + "embsv.conf.jsonc";
		File conffile = new File(conf);
		if (!conffile.exists()) {
			try (InputStream is = AppServer.class.getResourceAsStream("conf/embsv.conf.jsonc")) {
				try (FileOutputStream os = new FileOutputStream(conffile)) {
					FileUtil.copyStream(is, os);
				}
			}
		}
		return conffile.getAbsolutePath();
	}
	
	/**
	 * コマンドライン引数から実行モードを判定します。
	 * @param args コマンドライン奇数。
	 * @return 実行モード。
	 */
	private static Mode parseMode(String[] args) {
		Mode mode = null;
		for (int i = 0; i < args.length; i++) {
			if (mode == null && "-start".equals(args[i])) {
				if (i + 1 < args.length) {
					AppServer.appPath = args[i + 1];
					mode = Mode.START;
					i++;
				}
			}
			if (mode == null && "-stop".equals(args[i])) {
				mode = Mode.STOP;
			}
			if (mode == null && "-emb".equals(args[i])) {
				if (i + 2 < args.length) {
					mode = Mode.EMB;
					AppServer.inputWar = args[i + 1];
					AppServer.outputWar = args[i + 2];
					i += 2;
				}
			}
			if (mode == null && "-help".equals(args[i])) {
				mode = Mode.HELP;
			}
			if ("-browser".equals(args[i])) {
				AppServer.conf.browser = true;
			}
			if ("-mode".equals(args[i])) {
				if (i + 1 < args.length) {
					String m = args[i + 1];
					if (Conf.MODE_CMDLINE.equals(m) || Conf.MODE_TASKTRAY.equals(m) || Conf.MODE_WINDOW.equals(m)) {
						AppServer.conf.mode = args[i + 1];
					}
					i++;
				}
			}
			if ("-port".equals(args[i])) {
				if (i + 1 < args.length) {
					AppServer.conf.port = Integer.parseInt(args[i + 1]);
					i++;
				}
			}
			if ("-shutdownPort".equals(args[i])) {
				if (i + 1 < args.length) {
					AppServer.conf.shutdownPort = Integer.parseInt(args[i + 1]);
					i++;
				}
			}
		}
		if (mode == null) {
			// 何も指定されていない場合*.warファイルだったらそのwarファイルを実行。
			String jarname = JarUtil.getJarPath(AppServer.class);
			if (Pattern.matches(".+\\.war$", jarname)) {
				AppServer.appPath = jarname;
				mode = Mode.START;
			} else {
				// 何も指定されていない場合はHELP。
				mode = Mode.HELP;
			}
		}
		return mode;
	}

	/**
	 * warファイルを転嫁します。
	 * @param warfile warファイルのパス。
	 * @param overwrite 上書きフラグ。
	 * @return 展開されたパス。
	 * @throws Exception 例外。
	 */
	private String extractWar(final String warfile, final boolean overwrite) throws Exception {
		File wf = new File(warfile);
		String jarpath = JarUtil.getJarPath(AppServer.class);
		String expath = (new File(jarpath)).getParent() + File.separator 
				+ "webapps" + File.separator + wf.getName();
		expath = expath.replaceAll("\\.war$", "");
		File exfile = new File(expath);
		logger.debug("extract:" + warfile + " => " + expath);
		if (overwrite || exfile.exists() == false || exfile.lastModified() < wf.lastModified()) {
			JarUtil.extractJar(wf, new File(expath));
		}
		return expath;
	}
	
	/**
	 * warファイルを転嫁します。
	 * @param warfile warファイルのパス。
	 * @return 展開されたパス。
	 * @throws Exception 例外。
	 */
	private String extractWar(final String warfile) throws Exception {
		return this.extractWar(warfile, false);
	}
	
	/**
	 * META-INF/context.xmlのデータベースパスを設定します。
	 * @param expath warの展開パス。
	 * @throws Exception 例外。
	 */
	private void setDataSourcePath(final String expath) throws Exception {
		logger.debug("setDataSourcePath expath:" + expath);
		File exFile = new File(expath);
		String apname = exFile.getName();
		String dbpath = exFile.getParentFile().getParent() + File.separator + "javadb" + File.separator + apname;
		logger.debug("setDataSourcePath dbpath:" + dbpath);
		String contextXml = expath + File.separator + "META-INF" + File.separator + "context.xml";
		File contextXmlFile = new File(contextXml);
		if (contextXmlFile.exists()) {
			ContextXml xml = new ContextXml(new File(contextXml));
			if (xml.getDatasource() != null) {
				xml.setDatabasePath(new File(dbpath));
				xml.save();
			} else {
				logger.warn(AppServer.resource.getString("message.datasourcenotfound"));
			}
		} else {
			logger.warn(contextXml + AppServer.resource.getString("message.contextxmlnotfound"));
		}
	}
	
	/**
	 * Webアプリケーションのリストを取得します。
	 * @return Webあぷりけーし
	 */
	private List<File> getWebAppList() {
		List<File> list = new ArrayList<File>();
		String jarpath = JarUtil.getJarPath(AppServer.class);
		File jarfile = new File(jarpath);
		File webapps = new File(jarfile.getParent() + File.separator + "webapps");
		File[] applist = webapps.listFiles();
		for (File app: applist) {
			list.add(app);			
		}
		return list;
	}
	
	/**
	 * Webアプリケーションを開始します。
	 * @throws Exception 例外。
	 */
	private void start(final String path) throws Exception {
		AppServer.writeConfFile();
		File appfile = new File(path);
		// 展開されたWebアプリケーションのパスを指定された場合、1件のアプリのみを実行。
		this.webAppList = new ArrayList<File>();
		this.webAppList.add(appfile);
		if (Pattern.matches(".+\\.war$", path)) {
			String expath = this.extractWar(path);
			this.setDataSourcePath(expath);
			appfile = new File(expath);
			this.webAppList = this.getWebAppList();
		}
		this.startPath(this.webAppList);
	}

	/**
	 * TCPのポートを確認しTOMCATが起動しているかどうかを確認する。
	 * @return 起動している場合true。
	 * @throws IOException 例外。
	 */
	private boolean isStarted() throws IOException {
		boolean started = false;
		try {
			ServerSocket socket = new ServerSocket(AppServer.conf.getPort());
			try {
				;//
			} finally {
				socket.close();
			}
		} catch (BindException ex) {
			started = true;
			ex.printStackTrace();
		}
		return started;
	}

	/**
	 * 停止コマンド。
	 */
	private static final String SHUTDOWN_COMMAND = "shutdown";

	/**
	 * localhostのIPアドレス。
	 */
	private static final String LOCALHOST = "127.0.0.1";


	/**
	 * シャッドダウン命令受付スレッド。
	 *
	 */
	private class ShutdownListenThread extends Thread {
		@Override
		public void run() {
			try {
				ServerSocket ss = new ServerSocket();
				try {
					ss.bind(new InetSocketAddress(LOCALHOST, AppServer.conf.getShutdownPort()));
					Socket s = ss.accept();
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
						while (true) {
							String line = reader.readLine();
							if (SHUTDOWN_COMMAND.equals(line)) {
								AppServer.this.stop();
								break;
							}
						}
					} finally {
						s.close();
					}
				} finally {
					ss.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * ブラウザを起動して、指定したアプリケーションを表示します。
	 * @param applist アプリケーションリスト。
	 * @throws Exception 例外。
	 */
	public void runBrowser(final List<File> applist) throws Exception {
		for (File f: applist) {
			this.runBrowser(f);
		}
	}
	
	/**
	 * アプリケーションのURLを取得します。
	 * @param f アプリケーションのパス。
	 * @return アプリケーションのURL。
	 */
	private String getAppURL(final File f) {
		int port = AppServer.conf.getPort();
		String context = "/" + f.getName();
		String ret = "http://localhost:" + port + context;
		return ret;
	}

	/**
	 * ブラウザを起動して、指定したアプリケーションを表示します。
	 * @param f アプリケーションファイル。
	 * @throws Exception 例外。
	 */
	public void runBrowser(File f) {
		try {
			String appurl = this.getAppURL(f);
			List<String> browser = (List<String>) AppServer.conf.getBrowserCommandLine();
			if (browser.size() == 0) {
				Desktop.getDesktop().browse(new URI(appurl));
			} else {
				String[] cmd = new String[browser.size() + 1];
				int idx = 0;
				for (String c: browser) {
					cmd[idx++] = c;
				}
				cmd[idx] = appurl;
				Runtime r = Runtime.getRuntime();
				r.exec(cmd);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * 指定したPathを実行します。
	 * @param applist Webアプリケーションのリスト。
	 * @throws Exception 例外。
	 */
	private void startPath(final List<File> applist) throws Exception {
		// サーバが起動しているかどうかを確認
		boolean started = this.isStarted();
		if (!started) {
			// サーバが起動していない場合はサーバを起動。
			this.tomcat = new Tomcat();
			this.tomcat.setPort(AppServer.conf.getPort());
			this.tomcat.getConnector();
			this.tomcat.enableNaming();
			for (File f: applist) {
				String context = "/" + f.getName();
				this.tomcat.addWebapp(context, f.getAbsolutePath());
			}
			this.tomcat.start();
			// シャットダウンポートの監視スレッド。
			ShutdownListenThread th = new ShutdownListenThread();
			th.start();
			if (!Conf.MODE_CMDLINE.equals(AppServer.getConf().getMode())) {
				EmbSvFrame.showGui(this, this.webAppList);
			}
		}
		String msg = AppServer.resource.getString("message.browser");
		for (File f: applist) {
			String appurl = this.getAppURL(f);
			msg += "\n\t" + appurl;
		}
		logger.info(msg);
		if (AppServer.conf.isBrowser()) {
			this.runBrowser(applist);
		}
		if (started) {
			// サーバが起動していた場合は何もしないで終了する。
			System.exit(0);
		}
	}
	
	/**
	 * アプリケーションの停止。
	 */
	public void stop() {
		try {
			this.tomcat.stop();
			Thread.sleep(3000);
			this.tomcat.destroy();
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * シャットダウンコマンドの送信。
	 * @throws Exception 例外。
	 */
	private void shutdown() throws Exception {
		try (Socket s = new Socket(LOCALHOST, AppServer.conf.getShutdownPort())) {
			try (PrintWriter writer = new PrintWriter(s.getOutputStream(), true)) {
				writer.println(SHUTDOWN_COMMAND);
			}
		}
	}

	/**
	 * AppServerが含まれているjarファイルを展開します。
	 * @throws Exception 例外。
	 */
	private void extactMyself(final String expath) throws Exception {
		String jarname = JarUtil.getJarPath(AppServer.class);
		logger.debug("jarname=" + jarname);
		File jarFile = new File(jarname);
		File dstfile = new File(expath);
		JarUtil.extractJar(jarFile, dstfile, false);
	}
	

	/**
	 * warファイルにサーバ機能を追加する。
	 * @throws Exception 例外。
	 */
	private void embedTomcat() throws Exception {
		logger.debug("inputWar=" + AppServer.inputWar);
		logger.debug("outputWar=" + AppServer.outputWar);
		String expath = this.extractWar(AppServer.inputWar, true);
		logger.debug("expath=" + expath);
		this.extactMyself(expath);
		logger.info(AppServer.resource.getString("message.creating") + AppServer.outputWar);
		FileUtil.createZipFile(AppServer.outputWar, expath);
		logger.info(AppServer.resource.getString("message.created") + AppServer.outputWar);
		logger.info(AppServer.resource.getString("message.cleanup") + expath);
		logger.info(AppServer.resource.getString("message.startcmd") + "\n\tjava -jar " + AppServer.outputWar);
//		logger.info("java -jar " + AppServer.outputWar);
		FileUtils.cleanDirectory(new File(expath));
		new File(expath).delete();
	}
	
	/**
	 * Helpメッセージを表示します。
	 * @throws Exception 例外。
	 */
	private static void help() throws Exception{
		Locale locale = Locale.getDefault();
		logger.debug("langCode=" + locale.getLanguage());
		String help = "help/help_" + locale.getLanguage() + ".txt";
		AppServer.class.getResource(help);
		if (AppServer.class.getResource(help) == null) {
			help = "help/help.txt";
		}
		try (InputStream is = AppServer.class.getResourceAsStream(help)) {
			byte[] b = FileUtil.readInputStream(is);
			String text = new String(b, "utf-8");
			System.out.print(text);
		}
	}

	/**
	 * 設定ファイルを読み込みます。
	 * @throws Exception 例外。
	 */
	private static void readConf() throws Exception {
		String confFile = AppServer.writeConfFile();
		String json = FileUtil.readTextFile(confFile, "utf-8");
		Gson gson = new Gson();
		AppServer.conf = gson.fromJson(json, Conf.class);
		logger.debug("json=\n" + gson.toJson(AppServer.conf));

	}

	
	
	/**
	 * メイン処理。
	 * @param args コマンドライン引数。
	 */
	public static void main(String[] args) {
		try {
			AppServer.readConf();
			Mode mode = AppServer.parseMode(args);
			if (mode == Mode.START) {
				AppServer server = new AppServer();
				server.start(AppServer.appPath);
			} else if (mode == Mode.STOP) {
				AppServer server = new AppServer();
				server.shutdown();
			} else if (mode == Mode.EMB) {
				AppServer server = new AppServer();
				server.embedTomcat();
			} else if (mode == Mode.HELP) {
				AppServer.help();
			}
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

}
