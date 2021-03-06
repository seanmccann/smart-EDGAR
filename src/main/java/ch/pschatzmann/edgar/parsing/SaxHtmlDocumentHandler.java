package ch.pschatzmann.edgar.parsing;

import java.io.Serializable;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.pschatzmann.edgar.base.Fact;
import ch.pschatzmann.edgar.base.Fact.Attribute;
import ch.pschatzmann.edgar.base.Fact.Type;
import ch.pschatzmann.edgar.base.FactValue;
import ch.pschatzmann.edgar.base.IndexAPI;
import ch.pschatzmann.edgar.base.XBRL;
import ch.pschatzmann.edgar.utils.Utils;

/**
 * Sax Parser which loads the iHBRL html documents
 * 
 * @author pschatzmann
 *
 */

public class SaxHtmlDocumentHandler extends DefaultHandler implements Serializable {
	private static final Logger LOG = Logger.getLogger(SaxHtmlDocumentHandler.class);
	private StringBuffer value = new StringBuffer();
	private Stack<Fact> factStack = new Stack();
	private IndexAPI index;
	private Fact fact;
	private int level = 0;
	//private boolean ignoreHtml = true;
	private XBRL xbrl;
	private long line = 0;

	/**
	 * Constructor
	 * 
	 * @param xbrl
	 * @param factRoot
	 * @param isFactFile
	 */
	public SaxHtmlDocumentHandler(XBRL xbrl, Fact factRoot, boolean isFactFile) {
		this.xbrl = xbrl;
		this.fact = factRoot;
		this.index = xbrl.getIndex();
		//this.ignoreHtml = xbrl.isIgnoreHtml();
		factStack.push(factRoot);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		Fact newfact = null;
		line++;
		value.setLength(0);

		Type type = getType(localName, uri);
		switch (type) {
		case value:
			newfact = processValue(uri, qName, attributes);
			break;
		case html:
			newfact = processHtml(qName, attributes);
			break;
		default:
			newfact = processFact(attributes, type);
			break;
		}

		factStack.push(newfact);
		fact = newfact;
		level++;

	}

	private Fact processFact(Attributes attributes, Type type) {
		Fact newfact;
		newfact = new Fact(xbrl, type, level, line);
		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			newfact.put(name, value);
		}
		createRelationship(newfact, fact);
		return newfact;
	}

	private Fact processHtml(String qName, Attributes attributes) {
		Fact newfact;
		newfact = new HtmlFact(xbrl, Type.html, level, line);
		newfact.put("tag", qName);
		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			newfact.put(name, value);
		}
		createRelationship(newfact, fact);
		return newfact;
	}

	private Fact processValue(String uri, String qName, Attributes attributes) {
		Fact newfact;
		newfact = new FactValue(xbrl, Type.value, level, line);
		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			if (name.equals("name")) {
				newfact.put(Attribute.parameterName.name(), Utils.lastPath(value));
			} else {
				newfact.put(name, value);
			}
		}
		if (attributes.getLength() > 0) {
			newfact.put("uri", uri);
			int pos = qName.indexOf(":");
			newfact.put("prefix", qName.substring(0, Math.max(0, pos)));
			createRelationship(newfact, fact);
		}
		return newfact;
	}

	private Type getType(String localName, String uri) {
		Type result = null;
		if (uri.endsWith("xhtml")) {
			result = Type.html;
		} else {
			// import is reserved, we map it to importX
			localName = "import".equals(localName) ? "importX" : localName;
			try {
				result = Type.valueOf(localName);
			} catch (IllegalArgumentException ex) {
				result = Type.value;
			}
		}
		return result;
	}

	private void createRelationship(Fact fact, Fact parent) {
		parent.addChild(fact);
		fact.addParent(parent);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		level--;
		String str = value.toString().trim();
		if (!str.isEmpty()) {
			String name = localName;
			if (fact.getType() == Type.value || fact.getType()==Type.html) {
				name = Attribute.value.name();				
				String fmt = Utils.str(fact.getAttribute("format"));
				if (fmt.matches("ixt:num.*|ixt:zero.*")) {
					str = str.replaceAll(",", "");
				}
			}

			IValueFormatter f = this.xbrl.getValueFormatter(fact.getDataType(str));
			fact.put(name, f.format(str));
			fact.index();
			
		}
		factStack.pop();
		fact = factStack.peek();
				
		value = new StringBuffer();
	}


	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		value.append(new String(ch, start, length));
	}

	
	public void endDocument() throws SAXException {
		value.setLength(0);
		factStack.clear();
		index = null;
		xbrl = null;
	}
	
	public class HtmlFact extends Fact {
		public HtmlFact(XBRL xbrl, Type type, int level, long line) {
			super(xbrl, type, level, line);
		}
	}
	
}