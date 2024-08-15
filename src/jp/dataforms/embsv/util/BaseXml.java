package jp.dataforms.embsv.util;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import lombok.Getter;

/**
 * Webアプリケーション中のXMLのベースクラス。
 *
 */
public class BaseXml {
	/**
	 * XML Document。
	 */
	@Getter
	private Document document = null;
	
	/**
	 * XMLファイル名。
	 */
	@Getter
	private File xmlFile = null;

	/**
	 * コンストラクタ。
	 * @param xml XML。
	 * @throws Exception 例外。
	 */
	public BaseXml(final File xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// DOM Documentインスタンス用ファクトリの生成
		DocumentBuilder builder = factory.newDocumentBuilder();
		// 解析とDocumentインスタンスの取得
		this.document = builder.parse(xml);
		this.xmlFile = xml;
	}
	
	/**
	 * XMLファイルを出力します。
	 * @throws Exception 例外。
	 */
	public void save() throws Exception {
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer tf = factory.newTransformer();
		tf.setOutputProperty("encoding", "UTF-8");
		tf.transform(new DOMSource(this.document), new StreamResult(this.xmlFile));
	}
}
