package net.marcomichel.ed.parser;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
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
	
	private String getVerboseLogging(Document doc) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xPath.compile("//Network/@VerboseLogging");
		String attributeValue = (String) expr.evaluate(doc, XPathConstants.STRING);
		return attributeValue;
	}
	
	public boolean isVerboseLogging() throws IOException {
		String logging = "";
		try {
			Document doc = parseGameConfig(file);
			logging = getVerboseLogging(doc);
		} catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
			log.log(Level.SEVERE, "Error parsing Game-Config", e);
		}
		log.fine("VerboseLogging is " + logging);
		
		return ("1".equals(logging) ? true : false);
	}
	
}
