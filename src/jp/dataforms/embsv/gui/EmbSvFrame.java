package jp.dataforms.embsv.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jp.dataforms.embsv.AppServer;
import jp.dataforms.embsv.AppServer.Conf;
import jp.dataforms.embsv.util.FileUtil;

/**
 * GUIフレーム。
 */
public class EmbSvFrame extends JFrame {
	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(EmbSvFrame.class); 
	
	/**
	 * システム名称。
	 */
	private static final String SYSTEM_NAME = "Embedded Server";

	/**
	 * UID.
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * リソース。
	 */
	private static ResourceBundle resource = ResourceBundle.getBundle("jp.dataforms.embsv.gui.EmbSvFrame");

	/**
	 * アイコンイメージ。
	 */
	private Image iconImage = null;

	/**
	 * アプリケーション。
	 */
	private JList<String> appList = null;

	/**
	 * Webアプリケーションリスト。
	 */
	private List<File> webAppList = null;
	
	/**
	 * タスクトレイアイコン。
	 */
	private TrayIcon icon = null;
	
	/**
	 * 内容表示領域。
	 */
	private JPanel contentPane;

	/**
	 * アプリケーションサーバのインスタンス。
	 */
	private AppServer appServer = null;
	
	/**
	 * Launch the application.
	 */
/*	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmbSvFrame frame = new EmbSvFrame();
					frame.setTaskTrayIcon();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
*/
	/**
	 * GUIを表示します。
	 * @param server 
	 * @param webAppList Webアプリケーションリスト。
	 */
	public static void showGui(final AppServer server, final List<File> webAppList) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EmbSvFrame frame = new EmbSvFrame(webAppList);
					if (Conf.MODE_TASKTRAY.equals(AppServer.getConf().getMode())) {
						frame.setTaskTrayIcon();
					} else if (Conf.MODE_WINDOW.equals(AppServer.getConf().getMode())) {
						frame.setVisible(true);
						frame.setExtendedState(ICONIFIED);
					}
//					frame.setWebAppList();
					frame.appServer = server;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
	/**
	 * Create the frame.
	 * @param webAppList Webアプリケーションリスト。
	 */
	public EmbSvFrame(final List<File> webAppList) throws Exception {
		setTitle(SYSTEM_NAME);
		this.webAppList = webAppList;
		this.iconImage = ImageIO.read(Thread.currentThread().getContextClassLoader().getResourceAsStream("embsv.png"));
		this.setIconImage(this.iconImage);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.setFrameMenu();
		
		this.setBounds(100, 100, 346, 237);
		this.contentPane = new JPanel();
		this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.contentPane.setLayout(new BorderLayout(0, 0));
		this.setContentPane(this.contentPane);

		// タイトルパネル
		{
			JPanel titlePanel = new JPanel();
			this.contentPane.add(titlePanel, BorderLayout.NORTH);
			JLabel label = new JLabel(EmbSvFrame.resource.getString("menuitem.applist"));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			titlePanel.add(label);

		}
		// アプリケーションリスト
		{
			
			this.appList = new JList<String>();
			JScrollPane scrollPane = new JScrollPane(this.appList);
			appList.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						try {
							int idx = appList.getSelectedIndex();
							File f = EmbSvFrame.this.webAppList.get(idx);
							EmbSvFrame.this.appServer.runBrowser(f);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});
			this.contentPane.add(scrollPane, BorderLayout.CENTER);
		}
		// ボタンパネル
		{
			JPanel buttonPanel = new JPanel();
			this.contentPane.add(buttonPanel, BorderLayout.SOUTH);
			JButton closeButton = new JButton(EmbSvFrame.resource.getString("button.close"));
			closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Conf conf = AppServer.getConf();
					if (Conf.MODE_TASKTRAY.equals(conf.getMode())) {
						EmbSvFrame.this.setVisible(false);
					} else {
						EmbSvFrame.this.appServer.stop();
						System.exit(0);
					}
				}
			});
			buttonPanel.add(closeButton);
		}
		this.setWebAppList();
	}
	
	/**
	 * ウインドウのメニューを設定します。
	 */
	private void setFrameMenu() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu(EmbSvFrame.resource.getString("menu.file"));
		JMenuItem exitItem = new JMenuItem(EmbSvFrame.resource.getString("menuitem.exit"));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.appServer.stop();
				System.exit(0);
			}
		});
		fileMenu.add(exitItem);
		JMenu helpMenu = new JMenu(EmbSvFrame.resource.getString("menu.help"));
		JMenuItem aboutItem = new JMenuItem(EmbSvFrame.resource.getString("menuitem.about"));
		aboutItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.about();
			}
		});
		helpMenu.add(aboutItem);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);
		this.setJMenuBar(menuBar);
	}

	/**
	 * タスクトレイを設定します。
	 * @throws Exception 例外。
	 */
	private void setTaskTrayIcon() throws Exception {
		// トレイアイコン生成
		Dimension d = SystemTray.getSystemTray().getTrayIconSize();
		Image img = this.iconImage.getScaledInstance((int) d.getWidth() - 2, (int) d.getHeight() - 2, Image.SCALE_SMOOTH);
		this.icon = new TrayIcon(img, SYSTEM_NAME);
		// イベント登録
		icon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.setVisible(true);
			}
		});
		// ポップアップメニュー
		PopupMenu menu = new PopupMenu();
		for (File f: this.webAppList) {
			String contextPath = "/" + f.getName();
			MenuItem showMenu = new MenuItem(EmbSvFrame.resource.getString("menuitem.show") + " (" + contextPath + ")");
			showMenu.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					EmbSvFrame.this.appServer.runBrowser(f);
				}
			});
			menu.add(showMenu);
		}
		MenuItem about = new MenuItem(EmbSvFrame.resource.getString("menuitem.about"));
		about.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.about();
			}
		});

		// 終了メニュー
		MenuItem exitItem = new MenuItem(EmbSvFrame.resource.getString("menuitem.exit"));
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				EmbSvFrame.this.appServer.stop();
				System.exit(0);
			}
		});
		// メニューにメニューアイテムを追加
		menu.add(about);
		menu.add(exitItem);
		icon.setPopupMenu(menu);

		// タスクトレイに格納
		SystemTray.getSystemTray().add(icon);
	}
	
	/**
	 * Webアプリケーションのリストを設定します。
	 */
	private void setWebAppList() {
		DefaultListModel<String> model = new DefaultListModel<String>();
		for (File f: webAppList) {
			model.addElement("/" + f.getName());
		}
		this.appList.setModel(model);
	}
	
	/**
	 * バージョン情報。
	 */
	private void about() {
		try (InputStream is = this.getClass().getResourceAsStream("res/version.txt")) {
			byte[] b = FileUtil.readInputStream(is);
			String vinf = new String(b);
			JOptionPane.showMessageDialog(null, vinf);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}
}
