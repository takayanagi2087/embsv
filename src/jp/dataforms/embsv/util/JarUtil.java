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
	 * jarファイルを展開します。
	 * @param jarfile jarファイル。
	 * @param dstfile 展開先。
	 */
	public static void extractJar(final File jarfile, final File dstfile) throws Exception {
		JarUtil.extractJar(jarfile, dstfile, true);
	}
	
	/**
	 * jarファイルを展開します。
	 * @param jarfile jarファイル。
	 * @param dstfile 展開先。
	 * @param clean 既に存在するファイルを削除する。
	 */
	public static void extractJar(final File jarfile, final File dstfile, final boolean clean) throws Exception {
		logger.info(jarfile.getAbsolutePath() + " => " + dstfile);
		if (!dstfile.exists()) {
			// 無い場合は作成。
			dstfile.mkdirs();
		} else {
			if (clean) {
				// 既にあった場合は中をクリア。
				FileUtils.cleanDirectory(dstfile);
			}
		}
		try (JarFile jar = new JarFile(jarfile)) {
			Enumeration<?> files = jar.entries();
			while (files.hasMoreElements()) {
			    ZipEntry entry = (ZipEntry) files.nextElement();
//			    logger.info("entry:" + entry.getName());
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

	/*public static void createJar(final File path, final File jarfile) throws Exception {
		try (JarFile jar = new JarFile(jarfile)) {
		}
	}*/
}
