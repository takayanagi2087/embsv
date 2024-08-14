package jp.dataforms.embsv.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * jarファイル操作ユーティリティ。
 */
public class JarUtil {
	
	/**
	 * Logger。
	 */
	private static Logger logger = LogManager.getLogger(JarUtil.class);

	/**
	 * 指定されたクラスを含むjarファイルのパスを取得します。
	 * @param cls クラス。
	 * @return jarファイルのパス。
	 */
	public static String getJarPath(final Class<?> cls) {
		String ret = null;
		try {
			String resname = cls.getName().replaceAll("\\.", "/") + ".class";
			logger.debug("resname=" + resname);
			URL classFile = cls.getClassLoader().getResource(resname);
			JarFile jf = ((JarURLConnection)classFile.openConnection()).getJarFile();
			ret = jf.getName();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return ret;

	}
	
	/**
	 * 入力ストリームを出力ストリームにコピーします。
	 * <pre>
	 * 出力ストリーム、入力ストリームともにクローズされません。
	 * </pre>
	 * @param is 入力ストリーム。
	 * @param os 出力ストリーム。
	 * @throws IOException IO例外。
	 */
/*	public static void copyStream(final InputStream is, final OutputStream os) throws IOException {
		byte[] buf = new byte[16 * 1024];
		while (true) {
			int len = is.read(buf);
			if (len <= 0) {
				break;
			}
			os.write(buf, 0, len);
		}
	}
*/
	
	/**
	 * jarファイルを展開します。
	 * @param jarfile jarファイル。
	 * @param dstfile 展開先。
	 */
	public static void extractJar(final File jarfile, final File dstfile) throws Exception {
		logger.info(jarfile.getAbsolutePath() + " => " + dstfile);
		if (!dstfile.exists()) {
			// 無い場合は作成。
			dstfile.mkdirs();
		} else {
			// 既にあった場合は仲をクリア。
			FileUtils.cleanDirectory(dstfile);
		}
		try (JarFile jar = new JarFile(jarfile)) {
			Enumeration<?> files = jar.entries();
			while (files.hasMoreElements()) {
			    ZipEntry entry = (ZipEntry) files.nextElement();
			    logger.info("entry:" + entry.getName());
			    String ap = dstfile.getAbsolutePath() + File.separator + entry.getName();
			    File f = new File(ap);
			    if (entry.isDirectory()) {
			    	f.mkdirs();
			    } else {
			    	if (!f.getParentFile().exists()) {
			    		f.getParentFile().mkdirs();
			    	}
				    try (InputStream is = jar.getInputStream(entry)) {
				    	try (FileOutputStream os = new FileOutputStream(f)) {
				    		FileUtil.copyStream(is, os);
				    	}
				    }
			    }
			}
		}
	}
}
