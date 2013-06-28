/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.theme;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.oscim.backend.BitmapUtils;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.Log;
import org.oscim.backend.XMLReaderAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureAtlas.Rect;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.AreaLevel;
import org.oscim.theme.renderinstruction.Circle;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.LineSymbol;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.renderinstruction.Symbol;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.theme.rule.Rule;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 handler to parse XML render theme files.
 */
public class RenderThemeHandler extends DefaultHandler {
	private final static String TAG = RenderThemeHandler.class.getName();

	private static final int RENDER_THEME_VERSION = 1;

	private static enum Element {
		RENDER_THEME, RENDERING_INSTRUCTION, RULE, STYLE, ATLAS;
	}

	private static final String ELEMENT_NAME_RENDER_THEME = "rendertheme";
	private static final String ELEMENT_NAME_MATCH = "m";
	private static final String ELEMENT_NAME_STYLE_TEXT = "style-text";
	private static final String ELEMENT_NAME_STYLE_AREA = "style-area";
	private static final String ELEMENT_NAME_STYLE_LINE = "style-line";
	private static final String ELEMENT_NAME_STYLE_OUTLINE = "style-outline";
	private static final String ELEMENT_NAME_USE_STYLE_PATH_TEXT = "use-text";
	private static final String ELEMENT_NAME_USE_STYLE_AREA = "use-area";
	private static final String ELEMENT_NAME_USE_STYLE_LINE = "use-line";
	private static final String ELEMENT_NAME_USE_STYLE_OUTLINE = "use-outline";
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

	/**
	 * @param inputStream
	 *            an input stream containing valid render theme XML data.
	 * @return a new RenderTheme which is created by parsing the XML data from
	 *         the input stream.
	 * @throws SAXException
	 *             if an error occurs while parsing the render theme XML.
	 * @throws ParserConfigurationException
	 *             if an error occurs while creating the XML parser.
	 * @throws IOException
	 *             if an I/O error occurs while reading from the input stream.
	 */
	public static IRenderTheme getRenderTheme(InputStream inputStream)
	        throws SAXException, IOException {

		RenderThemeHandler renderThemeHandler = new RenderThemeHandler();

		new XMLReaderAdapter().parse(renderThemeHandler, inputStream);

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
	public static void logUnknownAttribute(String element, String name,
	                                       String value, int attributeIndex) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("unknown attribute in element ");
		stringBuilder.append(element);
		stringBuilder.append(" (");
		stringBuilder.append(attributeIndex);
		stringBuilder.append("): ");
		stringBuilder.append(name);
		stringBuilder.append('=');
		stringBuilder.append(value);
		Log.d(TAG, stringBuilder.toString());
	}

	private final ArrayList<Rule> mRulesList = new ArrayList<Rule>();
	private Rule mCurrentRule;

	private final Stack<Element> mElementStack = new Stack<Element>();
	private final Stack<Rule> mRuleStack = new Stack<Rule>();
	private final HashMap<String, RenderInstruction> tmpStyleHash =
	        new HashMap<String, RenderInstruction>(10);
	private TextureAtlas mTextureAtlas;
	private int mLevel;
	private RenderTheme mRenderTheme;

	@Override
	public void endDocument() {
		if (mRenderTheme == null) {
			throw new IllegalArgumentException("missing element: rules");
		}

		mRenderTheme.complete(mRulesList, mLevel);
		// mRenderTheme.mTextureAtlas = mTextureAtlas;

		mTextureAtlas = null;
		mRulesList.clear();
		tmpStyleHash.clear();
		mRuleStack.clear();
		mElementStack.clear();
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		mElementStack.pop();

		if (ELEMENT_NAME_MATCH.equals(localName)) {
			mRuleStack.pop();
			if (mRuleStack.empty()) {
				mRulesList.add(mCurrentRule);
			} else {
				mCurrentRule = mRuleStack.peek();
			}
		}
	}

	@Override
	public void error(SAXParseException exception) {
		Log.d(TAG, exception.getMessage());
	}

	@Override
	public void warning(SAXParseException exception) {
		Log.d(TAG, exception.getMessage());
	}

	@Override
	public void startElement(String uri, String localName, String qName,
	                         Attributes attributes) throws SAXException {
		try {
			if (ELEMENT_NAME_RENDER_THEME.equals(localName)) {
				checkState(localName, Element.RENDER_THEME);
				mRenderTheme = createRenderTheme(localName, attributes);
			}

			else if (ELEMENT_NAME_MATCH.equals(localName)) {
				checkState(localName, Element.RULE);
				Rule rule = Rule.create(localName, attributes, mRuleStack);
				if (!mRuleStack.empty()) {
					mCurrentRule.addSubRule(rule);
				}
				mCurrentRule = rule;
				mRuleStack.push(mCurrentRule);
			}

			else if (ELEMENT_NAME_STYLE_TEXT.equals(localName)) {
				checkState(localName, Element.STYLE);
				Text text = createText(localName, attributes, false);
				tmpStyleHash.put("t" + text.style, text);
			}

			else if (ELEMENT_NAME_STYLE_AREA.equals(localName)) {
				checkState(localName, Element.STYLE);
				Area area = createArea(localName, attributes, 0);
				tmpStyleHash.put("a" + area.style, area);
			}

			else if (ELEMENT_NAME_STYLE_LINE.equals(localName)) {
				checkState(localName, Element.STYLE);
				String style = null;
				if ((style = attributes.getValue("from")) != null) {
					RenderInstruction ri = tmpStyleHash.get("l" + style);
					if (ri instanceof Line) {
						Line line = createLine((Line) ri, localName, attributes, 0,
						                       false);
						tmpStyleHash.put("l" + line.style, line);
					}
					else {
						Log.d(TAG, "not a style: " + style);
					}
				} else {
					Line line = createLine(null, localName, attributes, 0, false);
					tmpStyleHash.put("l" + line.style, line);
				}
			}

			else if (ELEMENT_NAME_STYLE_OUTLINE.equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = createLine(null, localName, attributes, mLevel++, true);
				tmpStyleHash.put("o" + line.style, line);
				// outlineLayers.add(line);
			}

			else if ("area".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Area area = createArea(localName, attributes, mLevel++);
				// mRuleStack.peek().addRenderingInstruction(area);
				mCurrentRule.addRenderingInstruction(area);
			}

			else if ("caption".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Text text = createText(localName, attributes, true);
				mCurrentRule.addRenderingInstruction(text);

				if (text.symbol != null) {
					if ((text.texture = mTextureAtlas.getTextureRegion(text.symbol)) == null)
						Log.d(TAG, "missing texture atlas item '" + text.symbol + "'");
					else
						Log.d(TAG, "using atlas item '" + text.symbol + "'");
				}
			}

			else if ("circle".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Circle circle = createCircle(localName, attributes, mLevel++);
				mCurrentRule.addRenderingInstruction(circle);
			}

			else if ("line".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = createLine(null, localName, attributes, mLevel++, false);
				mCurrentRule.addRenderingInstruction(line);
			}

			else if ("lineSymbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = createLineSymbol(localName, attributes);
				mCurrentRule.addRenderingInstruction(lineSymbol);
			}

			else if ("text".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Text text = createText(localName, attributes, false);
				mCurrentRule.addRenderingInstruction(text);
			}

			else if ("symbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = createSymbol(localName, attributes);
				mCurrentRule.addRenderingInstruction(symbol);

				if ((symbol.texture = mTextureAtlas.getTextureRegion(symbol.src)) == null)
					Log.d(TAG, "missing texture atlas item '" + symbol.src + "'");
				else
					Log.d(TAG, "using atlas item '" + symbol.src + "'");
			}

			else if (ELEMENT_NAME_USE_STYLE_LINE.equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				String style = attributes.getValue("name");
				if (style != null) {
					Line line = (Line) tmpStyleHash.get("l" + style);
					if (line != null) {
						Line newLine = createLine(line, localName, attributes,
						                          mLevel++, false);

						mCurrentRule.addRenderingInstruction(newLine);
					}
					else
						Log.d(TAG, "BUG: not a line style: " + style);
				}
			} else if (ELEMENT_NAME_USE_STYLE_OUTLINE.equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				String style = attributes.getValue("name");
				if (style != null) {
					Line line = (Line) tmpStyleHash.get("o" + style);
					if (line != null && line.outline)
						mCurrentRule.addRenderingInstruction(line);
					else
						Log.d(TAG, "BUG not an outline style: " + style);
				}
			} else if (ELEMENT_NAME_USE_STYLE_AREA.equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				String style = attributes.getValue("name");
				if (style != null) {
					Area area = (Area) tmpStyleHash.get("a" + style);
					if (area != null)
						mCurrentRule.addRenderingInstruction(new AreaLevel(area,
						                                                   mLevel++));
					else
						Log.d(TAG, "BUG not an area style: " + style);
				}
			} else if (ELEMENT_NAME_USE_STYLE_PATH_TEXT.equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				String style = attributes.getValue("name");
				if (style != null) {
					Text pt = (Text) tmpStyleHash.get("t" + style);
					if (pt != null)
						mCurrentRule.addRenderingInstruction(pt);
					else
						Log.d(TAG, "BUG not a path text style: " + style);
				}
			} else if ("atlas".equals(localName)) {
				checkState(localName, Element.ATLAS);
				createAtlas(localName, attributes);
			} else if ("rect".equals(localName)) {
				checkState(localName, Element.ATLAS);
				createTextureRegion(localName, attributes);
			} else {
				throw new SAXException("unknown element: " + localName);
			}
		} catch (IllegalArgumentException e) {
			throw new SAXException(null, e);
		} catch (IOException e) {
			throw new SAXException(null, e);
		}
	}

	private void createAtlas(String elementName, Attributes attributes) throws IOException {
		String img = null;

		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("img".equals(name)) {
				img = value;
			} else if ("name".equals(name)) {
				// img = value;
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}
		if (img == null)
			throw new IllegalArgumentException(
			                                   "missing attribute 'img' for element: "
			                                           + elementName);

		Bitmap bitmap = CanvasAdapter.g.loadBitmapAsset("styles/" + img);
		mTextureAtlas = new TextureAtlas(bitmap);
	}

	private void createTextureRegion(String elementName, Attributes attributes) {
		String regionName = null;
		Rect r = null;

		for (int i = 0, n = attributes.getLength(); i < n; i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("name".equals(name)) {
				regionName = value;
			} else if ("pos".equals(name)) {
				String[] pos = value.split(" ");
				if (pos.length == 4) {
					r = new Rect(Integer.parseInt(pos[0]),
					             Integer.parseInt(pos[1]),
					             Integer.parseInt(pos[2]),
					             Integer.parseInt(pos[3]));
				}
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}
		if (regionName == null || r == null)
			throw new IllegalArgumentException(
			                                   "missing attribute 'name' or 'rect' for element: "
			                                           + elementName);

		mTextureAtlas.addTextureRegion(regionName.intern(), r);
	}


	private void checkElement(String elementName, Element element) throws SAXException {
		Element parentElement;
		switch (element) {
		case RENDER_THEME:
			if (!mElementStack.empty()) {
				throw new SAXException(UNEXPECTED_ELEMENT + elementName);
			}
			return;

		case RULE:
			parentElement = mElementStack.peek();
			if (parentElement != Element.RENDER_THEME
			    && parentElement != Element.RULE) {
				throw new SAXException(UNEXPECTED_ELEMENT + elementName);
			}
			return;

		case STYLE:
			parentElement = mElementStack.peek();
			if (parentElement != Element.RENDER_THEME) {
				throw new SAXException(UNEXPECTED_ELEMENT + elementName);
			}
			return;

		case RENDERING_INSTRUCTION:
			if (mElementStack.peek() != Element.RULE) {
				throw new SAXException(UNEXPECTED_ELEMENT + elementName);
			}
			return;
		case ATLAS:
			parentElement = mElementStack.peek();
			// FIXME
			if (parentElement != Element.RENDER_THEME
			    && parentElement != Element.ATLAS) {
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


	static RenderTheme createRenderTheme(String elementName, Attributes attributes) {
		Integer version = null;
		int mapBackground = Color.WHITE;
		float baseStrokeWidth = 1;
		float baseTextSize = 1;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("schemaLocation".equals(name)) {
				continue;
			} else if ("version".equals(name)) {
				version = Integer.valueOf(Integer.parseInt(value));
			} else if ("map-background".equals(name)) {
				mapBackground = Color.parseColor(value);
			} else if ("base-stroke-width".equals(name)) {
				baseStrokeWidth = Float.parseFloat(value);
			} else if ("base-text-size".equals(name)) {
				baseTextSize = Float.parseFloat(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		if (version == null) {
			throw new IllegalArgumentException("missing attribute version for element:"
			                                   + elementName);
		} else if (version.intValue() != RENDER_THEME_VERSION) {
			throw new IllegalArgumentException("invalid render theme version:" + version);
		} else if (baseStrokeWidth < 0) {
			throw new IllegalArgumentException("base-stroke-width must not be negative: "
			                                   + baseStrokeWidth);
		} else if (baseTextSize < 0) {
			throw new IllegalArgumentException("base-text-size must not be negative: "
			                                   + baseTextSize);
		}

		return new RenderTheme(mapBackground, baseStrokeWidth, baseTextSize);
	}

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param caption
	 *            ...
	 * @return a new Text with the given rendering attributes.
	 */
	private static Text createText(String elementName, Attributes attributes, boolean caption) {
		String textKey = null;
		FontFamily fontFamily = FontFamily.DEFAULT;
		FontStyle fontStyle = FontStyle.NORMAL;
		float fontSize = 0;
		int fill = Color.BLACK;
		int stroke = Color.BLACK;
		float strokeWidth = 0;
		String style = null;
		float dy = 0;
		int priority = Integer.MAX_VALUE;
		String symbol = null;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			if ("name".equals(name))
				style = value;
			else if ("k".equals(name)) {
				textKey = value.intern();
			} else if ("font-family".equals(name)) {
				fontFamily = FontFamily.valueOf(value.toUpperCase());
			} else if ("font-style".equals(name)) {
				fontStyle = FontStyle.valueOf(value.toUpperCase());
			} else if ("font-size".equals(name)) {
				fontSize = Float.parseFloat(value);
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("caption".equals(name)) {
				caption = Boolean.parseBoolean(value);
			} else if ("priority".equals(name)) {
				priority = Integer.parseInt(value);
			} else if ("dy".equals(name)) {
				dy = Float.parseFloat(value);
			} else if ("symbol".equals(name)) {
				symbol = value;
			} else {
				logUnknownAttribute(elementName, name, value, i);
			}
		}

		validateText(elementName, textKey, fontSize, strokeWidth);

		return new Text(style, textKey, fontFamily, fontStyle, fontSize, fill, stroke, strokeWidth,
		                dy, caption, symbol, priority);
	}

	private static void validateText(String elementName, String textKey, float fontSize,
	                                 float strokeWidth) {
		if (textKey == null) {
			throw new IllegalArgumentException("missing attribute k for element: "
			                                   + elementName);
		} else if (fontSize < 0) {
			throw new IllegalArgumentException("font-size must not be negative: "
			                                   + fontSize);
		} else if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: "
			                                   + strokeWidth);
		}
	}

	// private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	/**
	 * @param line
	 *            ...
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @param isOutline
	 *            ...
	 * @return a new Line with the given rendering attributes.
	 */
	private static Line createLine(Line line, String elementName, Attributes attributes,
	                               int level, boolean isOutline) {

		// Style name
		String style = null;
		// Bitmap
		// String src = null;

		float width = 0;
		Cap cap = Cap.ROUND;

		// Extras
		int fade = -1;
		boolean fixed = false;
		float blur = 0;
		float min = 0;

		// Stipple
		int stipple = 0;
		float stippleWidth = 0;

		int color = Color.RED;

		int stippleColor = Color.BLACK;

		if (line != null) {
			color = line.color;
			fixed = line.fixed;
			fade = line.fade;
			cap = line.cap;
			blur = line.blur;
			min = line.min;
			stipple = line.stipple;
			stippleColor = line.stippleColor;
			stippleWidth = line.stippleWidth;
		}

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("name".equals(name))
				style = value;
			else if ("src".equals(name)) {
				// src = value;
			} else if ("stroke".equals(name)) {
				color = Color.parseColor(value);
			} else if ("width".equals(name)) {
				width = Float.parseFloat(value);
			} else if ("cap".equals(name)) {
				cap = Cap.valueOf(value.toUpperCase());
			} else if ("fix".equals(name)) {
				fixed = Boolean.parseBoolean(value);
			} else if ("stipple".equals(name)) {
				stipple = Integer.parseInt(value);
			} else if ("stipple-stroke".equals(name)) {
				stippleColor = Color.parseColor(value);
			} else if ("stipple-width".equals(name)) {
				stippleWidth = Float.parseFloat(value);
			} else if ("fade".equals(name)) {
				fade = Integer.parseInt(value);
			} else if ("min".equals(name)) {
				min = Float.parseFloat(value);
			} else if ("blur".equals(name)) {
				blur = Float.parseFloat(value);
			} else if ("from".equals(name)) {
				// ignore
			} else {
				logUnknownAttribute(elementName, name, value, i);
			}
		}

		// inherit properties from 'line'
		if (line != null) {
			// use stroke width relative to 'line'
			width = line.width + width;
			if (width <= 0)
				width = 1;

		} else if (!isOutline) {
			validateLine(width);
		}

		return new Line(level, style, color, width, cap, fixed,
		                stipple, stippleColor, stippleWidth,
		                fade, blur, isOutline, min);
	}

	private static void validateLine(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("width must not be negative: "
			                                   + strokeWidth);
		}
	}

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Area with the given rendering attributes.
	 */
	private static Area createArea(String elementName, Attributes attributes, int level) {
		String src = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;
		int fade = -1;
		int blend = -1;
		int blendFill = Color.BLACK;
		String style = null;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);
			if ("name".equals(name))
				style = value;
			else if ("src".equals(name)) {
				src = value;
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else if ("fade".equals(name)) {
				fade = Integer.parseInt(value);
			} else if ("blend".equals(name)) {
				blend = Integer.parseInt(value);
			} else if ("blend-fill".equals(name)) {
				blendFill = Color.parseColor(value);
			} else {
				logUnknownAttribute(elementName, name, value, i);
			}
		}

		validateArea(strokeWidth);
		return new Area(style, src, fill, stroke, strokeWidth, fade, level, blend,
		                blendFill);
	}

	private static void validateArea(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: "
			                                   + strokeWidth);
		}
	}

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Circle with the given rendering attributes.
	 */
	private static Circle createCircle(String elementName, Attributes attributes, int level) {
		Float radius = null;
		boolean scaleRadius = false;
		int fill = Color.TRANSPARENT;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("r".equals(name)) {
				radius = Float.valueOf(Float.parseFloat(value));
			} else if ("scale-radius".equals(name)) {
				scaleRadius = Boolean.parseBoolean(value);
			} else if ("fill".equals(name)) {
				fill = Color.parseColor(value);
			} else if ("stroke".equals(name)) {
				stroke = Color.parseColor(value);
			} else if ("stroke-width".equals(name)) {
				strokeWidth = Float.parseFloat(value);
			} else {
				logUnknownAttribute(elementName, name, value, i);
			}
		}

		validateCircle(elementName, radius, strokeWidth);
		return new Circle(radius, scaleRadius, fill, stroke, strokeWidth, level);
	}

	private static void validateCircle(String elementName, Float radius, float strokeWidth) {
		if (radius == null) {
			throw new IllegalArgumentException("missing attribute r for element: "
			                                   + elementName);
		} else if (radius.floatValue() < 0) {
			throw new IllegalArgumentException("radius must not be negative: " + radius);
		} else if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: "
			                                   + strokeWidth);
		}
	}

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @return a new LineSymbol with the given rendering attributes.
	 */
	private static LineSymbol createLineSymbol(String elementName, Attributes attributes) {
		String src = null;
		boolean alignCenter = false;
		boolean repeat = false;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name)) {
				src = value;
			} else if ("align-center".equals(name)) {
				alignCenter = Boolean.parseBoolean(value);
			} else if ("repeat".equals(name)) {
				repeat = Boolean.parseBoolean(value);
			} else {
				logUnknownAttribute(elementName, name, value, i);
			}
		}

		validateLineSymbol(elementName, src);
		return new LineSymbol(src, alignCenter, repeat);
	}

	private static void validateLineSymbol(String elementName, String src) {
		if (src == null) {
			throw new IllegalArgumentException("missing attribute src for element: "
			                                   + elementName);
		}
	}

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attributes
	 *            the attributes of the XML element.
	 * @return a new Symbol with the given rendering attributes.
	 */
	private static Symbol createSymbol(String elementName, Attributes attributes) {
		String src = null;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name)) {
				src = value;
			} else {
				logUnknownAttribute(elementName, name, value, i);
			}
		}

		validateSymbol(elementName, src);
		return new Symbol(src);
	}

	private static void validateSymbol(String elementName, String src) {
		if (src == null) {
			throw new IllegalArgumentException("missing attribute src for element: "
			                                   + elementName);
		}
	}
}
