package net.marcomichel.ed.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class GameConfigParser {

	private static final Logger log = Logger.getLogger(GameConfigParser.class.getName());

	private String file;

	public GameConfigParser(String file) {
		super();
		this.file = file;
	}

	private Document parseGameConfig(String file) throws ParserConfigurationException, SAXException, IOException {
		 DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		 DocumentBuilder builder = factory.newDocumentBuilder();
		 Document document = builder.parse(file);
		 return document;
	}

	private String getConfigValue(Document doc, String xpathExpr) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xPath.compile(xpathExpr);
		String attributeValue = (String) expr.evaluate(doc, XPathConstants.STRING);
		return attributeValue;
	}

	public String getLogFileBaseName() throws IOException {
		try {
			final Document doc = parseGameConfig(file);
			final String baseName = getConfigValue(doc, "//Network/@LogFile");
			log.fine("Log-File base name is " + baseName);
			return baseName;
		} catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
			log.log(Level.SEVERE, "Error parsing Game-Config", e);
			throw new IOException("Error parsing Game-Config.");
		}

	}

	public boolean isVerboseLogging() throws IOException {
		String logging = "";
		try {
			final Document doc = parseGameConfig(file);
			logging = getConfigValue(doc, "//Network/@VerboseLogging");
		} catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
			log.log(Level.SEVERE, "Error parsing Game-Config", e);
			throw new IOException("Error parsing Game-Config.");
		}
		log.fine("VerboseLogging is " + logging);

		return ("1".equals(logging) ? true : false);
	}

	public String setVerboseLogging() throws IOException {
		// Copy original File
		log.fine("Copy GameConfigFile");
		Path copySourcePath = Paths.get(file);
		Path copyTargetPath = Paths.get(file + ".edfootprints-watcher.bak");
		Files.copy( copySourcePath, copyTargetPath, StandardCopyOption.REPLACE_EXISTING );

		try {
			log.fine("Parsing GameConfigFile and set verbose logging");
			Document doc = parseGameConfig(file);
			Element e = (Element) doc.getElementsByTagName("Network").item(0);
			e.setAttribute("VerboseLogging", "1");

			log.fine("Transforming and writing config to file");
			// Use a Transformer for output
		    TransformerFactory tFactory =
		    TransformerFactory.newInstance();
		    Transformer transformer =
		    tFactory.newTransformer();

		    DOMSource source = new DOMSource(doc);
		    StreamResult result = new StreamResult(new File(file));
		    transformer.transform(source, result);
		} catch (ParserConfigurationException | SAXException e) {
			log.log(Level.SEVERE, "Error parsing Game-Config", e);
			throw new IOException("Error parsing Game-Config.");
		} catch (TransformerException e1) {
			log.log(Level.SEVERE, "Error transforming and writing new GameConfigFile", e1);
			throw new IOException("Error transforming and writing new GameConfigFile");
		}

		return copyTargetPath.toString();
	}

}
