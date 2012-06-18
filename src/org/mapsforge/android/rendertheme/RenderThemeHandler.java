/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.rendertheme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.mapsforge.android.rendertheme.renderinstruction.Area;
import org.mapsforge.android.rendertheme.renderinstruction.Caption;
import org.mapsforge.android.rendertheme.renderinstruction.Circle;
import org.mapsforge.android.rendertheme.renderinstruction.Line;
import org.mapsforge.android.rendertheme.renderinstruction.LineSymbol;
import org.mapsforge.android.rendertheme.renderinstruction.PathText;
import org.mapsforge.android.rendertheme.renderinstruction.Symbol;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler to parse XML render theme files.
 */
public class RenderThemeHandler extends DefaultHandler {
	private static final Logger LOG = Logger.getLogger(RenderThemeHandler.class.getName());

	private static enum Element {
		RENDER_THEME, RENDERING_INSTRUCTION, RULE;
	}

	private static final String ELEMENT_NAME_RENDER_THEME = "rendertheme";
	private static final String ELEMENT_NAME_RULE = "rule";
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

	/**
	 * @param inputStream
	 *            an input stream containing valid render theme XML data.
	 * @return a new RenderTheme which is created by parsing the XML data from the input stream.
	 * @throws SAXException
	 *             if an error occurs while parsing the render theme XML.
	 * @throws ParserConfigurationException
	 *             if an error occurs while creating the XML parser.
	 * @throws IOException
	 *             if an I/O error occurs while reading from the input stream.
	 */
	public static RenderTheme getRenderTheme(InputStream inputStream) throws SAXException,
			ParserConfigurationException, IOException {
		RenderThemeHandler renderThemeHandler = new RenderThemeHandler();
		XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
		xmlReader.setContentHandler(renderThemeHandler);
		xmlReader.parse(new InputSource(inputStream));
		return renderThemeHandler.mRenderTheme;
	}

	/**
	 * Logs the given information about an unknown XML attribute.
	 * 
	 * @param element
	 *            the XML element name.
	 * @param name
	 *            the XML attribute name.
	 * @param value
	 *            the XML attribute value.
	 * @param attributeIndex
	 *            the XML attribute index position.
	 */
	public static void logUnknownAttribute(String element, String name, String value, int attributeIndex) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("unknown attribute in element ");
		stringBuilder.append(element);
		stringBuilder.append(" (");
		stringBuilder.append(attributeIndex);
		stringBuilder.append("): ");
		stringBuilder.append(name);
		stringBuilder.append('=');
		stringBuilder.append(value);
		LOG.info(stringBuilder.toString());
	}

	private Rule mCurrentRule;
	private final Stack<Element> mElementStack = new Stack<Element>();
	private int mLevel;
	private RenderTheme mRenderTheme;
	private final Stack<Rule> mRuleStack = new Stack<Rule>();

	@Override
	public void endDocument() {
		if (mRenderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		mRenderTheme.setLevels(mLevel);
		mRenderTheme.complete();
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		mElementStack.pop();

		if (ELEMENT_NAME_RULE.equals(localName)) {
			mRuleStack.pop();
			if (mRuleStack.empty()) {
				mRenderTheme.addRule(mCurrentRule);
			} else {
				mCurrentRule = mRuleStack.peek();
			}
		}
	}

	@Override
	public void error(SAXParseException exception) {
		LOG.log(Level.SEVERE, null, exception);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		try {
			if (ELEMENT_NAME_RENDER_THEME.equals(localName)) {
				checkState(localName, Element.RENDER_THEME);
				mRenderTheme = RenderTheme.create(localName, attributes);
			}

			else if (ELEMENT_NAME_RULE.equals(localName)) {
				checkState(localName, Element.RULE);
				Rule rule = Rule.create(localName, attributes, mRuleStack);
				if (!mRuleStack.empty()) {
					mCurrentRule.addSubRule(rule);
				}
				mCurrentRule = rule;
				mRuleStack.push(mCurrentRule);
			}

			else if ("area".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Area area = Area.create(localName, attributes, mLevel++);
				mRuleStack.peek().addRenderingInstruction(area);
			}

			else if ("caption".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Caption caption = Caption.create(localName, attributes);
				mCurrentRule.addRenderingInstruction(caption);
			}

			else if ("circle".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Circle circle = Circle.create(localName, attributes, mLevel++);
				mCurrentRule.addRenderingInstruction(circle);
			}

			else if ("line".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = Line.create(localName, attributes, mLevel++);
				mCurrentRule.addRenderingInstruction(line);
			}

			else if ("outline".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = Line.create(localName, attributes, mLevel++);
				mRenderTheme.addOutlineLayer(line);
				// mCurrentRule.addRenderingInstruction(line);
			}

			else if ("lineSymbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = LineSymbol.create(localName, attributes);
				mCurrentRule.addRenderingInstruction(lineSymbol);
			}

			else if ("pathText".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				PathText pathText = PathText.create(localName, attributes);
				mCurrentRule.addRenderingInstruction(pathText);
			}

			else if ("symbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = Symbol.create(localName, attributes);
				mCurrentRule.addRenderingInstruction(symbol);
			}

			else {
				throw new SAXException("unknown element: " + localName);
			}
		} catch (IllegalArgumentException e) {
			throw new SAXException(null, e);
		} catch (IOException e) {
			throw new SAXException(null, e);
		}
	}

	@Override
	public void warning(SAXParseException exception) {
		LOG.log(Level.SEVERE, null, exception);
	}

	private void checkElement(String elementName, Element element) throws SAXException {
		switch (element) {
			case RENDER_THEME:
				if (!mElementStack.empty()) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RULE:
				Element parentElement = mElementStack.peek();
				if (parentElement != Element.RENDER_THEME && parentElement != Element.RULE) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;

			case RENDERING_INSTRUCTION:
				if (mElementStack.peek() != Element.RULE) {
					throw new SAXException(UNEXPECTED_ELEMENT + elementName);
				}
				return;
		}

		throw new SAXException("unknown enum value: " + element);
	}

	private void checkState(String elementName, Element element) throws SAXException {
		checkElement(elementName, element);
		mElementStack.push(element);
	}
}
