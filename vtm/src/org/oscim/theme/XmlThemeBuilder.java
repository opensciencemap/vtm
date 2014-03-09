/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.XMLReaderAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.backend.canvas.Paint.FontFamily;
import org.oscim.backend.canvas.Paint.FontStyle;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.renderer.atlas.TextureAtlas.Rect;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.renderer.elements.TextureItem;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.rule.Rule;
import org.oscim.theme.rule.RuleBuilder;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Circle;
import org.oscim.theme.styles.Extrusion;
import org.oscim.theme.styles.Line;
import org.oscim.theme.styles.LineSymbol;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.Symbol;
import org.oscim.theme.styles.Text;
import org.oscim.theme.styles.Text.TextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class XmlThemeBuilder extends DefaultHandler {
	static final Logger log = LoggerFactory.getLogger(XmlThemeBuilder.class);

	private static final int RENDER_THEME_VERSION = 1;

	private static enum Element {
		RENDER_THEME, RENDERING_INSTRUCTION, RULE, STYLE, ATLAS;
	}

	private static final String ELEMENT_NAME_RENDER_THEME = "rendertheme";
	private static final String ELEMENT_NAME_MATCH = "m";
	private static final String UNEXPECTED_ELEMENT = "unexpected element: ";

	//private static final String IMG_PATH = "styles/";
	private static final String IMG_PATH = "";

	private static final String LINE_STYLE = "L";
	private static final String OUTLINE_STYLE = "O";
	private static final String AREA_STYLE = "A";
	private static final String TEXT_STYLE = "T";

	/**
	 * @param inputStream
	 *            an input stream containing valid render theme XML data.
	 * @return a new RenderTheme which is created by parsing the XML data from
	 *         the input stream.
	 * @throws SAXException
	 *             if an error occurs while parsing the render theme XML.
	 * @throws IOException
	 *             if an I/O error occurs while reading from the input stream.
	 */
	public static IRenderTheme read(InputStream inputStream)
	        throws SAXException, IOException {

		XmlThemeBuilder renderThemeHandler = new XmlThemeBuilder();

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
		StringBuilder sb = new StringBuilder();
		sb.append("unknown attribute in element ");
		sb.append(element);
		sb.append(" (");
		sb.append(attributeIndex);
		sb.append("): ");
		sb.append(name);
		sb.append('=');
		sb.append(value);
		log.debug(sb.toString());
	}

	private final ArrayList<RuleBuilder> mRulesList = new ArrayList<RuleBuilder>();
	private final Stack<Element> mElementStack = new Stack<Element>();
	private final Stack<RuleBuilder> mRuleStack = new Stack<RuleBuilder>();
	private final HashMap<String, RenderStyle> mStyles =
	        new HashMap<String, RenderStyle>(10);

	private final TextBuilder mTextBuilder = new TextBuilder();

	private RuleBuilder mCurrentRule;
	private TextureAtlas mTextureAtlas;

	private int mLevels = 0;
	private int mMapBackground = 0xffffffff;
	private float mBaseTextSize = 1;

	private RenderTheme mRenderTheme;

	@Override
	public void endDocument() {

		Rule[] rules = new Rule[mRulesList.size()];
		for (int i = 0, n = rules.length; i < n; i++)
			rules[i] = mRulesList.get(i).onComplete();

		mRenderTheme = new RenderTheme(mMapBackground, mBaseTextSize, rules, mLevels);

		mRulesList.clear();
		mStyles.clear();
		mRuleStack.clear();
		mElementStack.clear();

		mTextureAtlas = null;
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
		log.debug(exception.getMessage());
	}

	@Override
	public void warning(SAXParseException exception) {
		log.debug(exception.getMessage());
	}

	@Override
	public void startElement(String uri, String localName, String qName,
	        Attributes attributes) throws SAXException {
		try {
			if (ELEMENT_NAME_RENDER_THEME.equals(localName)) {
				checkState(localName, Element.RENDER_THEME);
				createRenderTheme(localName, attributes);

			} else if (ELEMENT_NAME_MATCH.equals(localName)) {
				checkState(localName, Element.RULE);
				RuleBuilder rule = RuleBuilder.create(localName, attributes, mRuleStack);
				if (!mRuleStack.empty()) {
					mCurrentRule.addSubRule(rule);
				}
				mCurrentRule = rule;
				mRuleStack.push(mCurrentRule);

			} else if ("style-text".equals(localName)) {
				checkState(localName, Element.STYLE);
				Text text = createText(localName, attributes, false);
				mStyles.put(TEXT_STYLE + text.style, text);

			} else if ("style-area".equals(localName)) {
				checkState(localName, Element.STYLE);
				handleAreaElement(localName, attributes, true);

			} else if ("style-line".equals(localName)) {
				checkState(localName, Element.STYLE);
				handleLineElement(localName, attributes, true);

			} else if ("outline-layer".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Line line = createLine(null, localName, attributes, mLevels++, true);
				mStyles.put(OUTLINE_STYLE + line.style, line);

			} else if ("area".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				handleAreaElement(localName, attributes, false);

			} else if ("caption".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Text text = createText(localName, attributes, true);
				mCurrentRule.addStyle(text);
			} else if ("circle".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Circle circle = createCircle(localName, attributes, mLevels++);
				mCurrentRule.addStyle(circle);

			} else if ("line".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				handleLineElement(localName, attributes, false);

			} else if ("lineSymbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				LineSymbol lineSymbol = createLineSymbol(localName, attributes);
				mCurrentRule.addStyle(lineSymbol);

			} else if ("text".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				String style = attributes.getValue("use");
				if (style == null) {
					Text text = createText(localName, attributes, false);
					mCurrentRule.addStyle(text);
				} else {
					Text pt = (Text) mStyles.get(TEXT_STYLE + style);
					if (pt != null)
						mCurrentRule.addStyle(pt);
					else
						log.debug("BUG not a path text style: " + style);
				}

			} else if ("symbol".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Symbol symbol = createSymbol(localName, attributes);
				mCurrentRule.addStyle(symbol);

			} else if ("outline".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				addOutline(attributes.getValue("use"));

			} else if ("extrusion".equals(localName)) {
				checkState(localName, Element.RENDERING_INSTRUCTION);
				Extrusion extrusion = createExtrusion(localName, attributes, mLevels++);
				mCurrentRule.addStyle(extrusion);

			} else if ("atlas".equals(localName)) {
				checkState(localName, Element.ATLAS);
				createAtlas(localName, attributes);

			} else if ("rect".equals(localName)) {
				checkState(localName, Element.ATLAS);
				createTextureRegion(localName, attributes);

			} else {
				log.debug("unknown element: " + localName);
				//throw new SAXException("unknown element: " + localName);
			}
		} catch (ThemeException e) {
			throw new SAXException(null, e);
		} catch (IOException e) {
			throw new SAXException(null, e);
		}
	}

	private TextureRegion getAtlasRegion(String src) {
		if (mTextureAtlas == null)
			return null;

		TextureRegion texture = mTextureAtlas.getTextureRegion(src);

		if (texture == null)
			log.debug("missing texture atlas item '" + src + "'");

		return texture;
	}

	private void handleLineElement(String localName, Attributes attributes, boolean isStyle)
	        throws SAXException {

		String use = attributes.getValue("use");
		Line style = null;

		if (use != null) {
			style = (Line) mStyles.get(LINE_STYLE + use);
			if (style == null) {
				log.debug("missing line style 'use': " + use);
				return;
			}
		}

		Line line = createLine(style, localName, attributes, mLevels++, false);

		if (isStyle) {
			mStyles.put(LINE_STYLE + line.style, line);
		} else {
			mCurrentRule.addStyle(line);
			// Note 'outline' will not be inherited, it's just a 
			// shorcut to add the outline RenderInstruction.
			addOutline(attributes.getValue("outline"));
		}
	}

	/**
	 * @param line
	 *            optional: line style defaults
	 * @param level
	 *            the drawing level of this instruction.
	 * @param isOutline
	 *            is outline layer
	 * @return a new Line with the given rendering attributes.
	 */
	private static Line createLine(Line line, String elementName, Attributes attributes,
	        int level, boolean isOutline) {

		// Style name
		String style = null;
		float width = 0;
		Cap cap = Cap.ROUND;

		// Extras
		int fade = -1;
		boolean fixed = false;
		float blur = 0;

		// Stipple
		int stipple = 0;
		float stippleWidth = 1;

		int color = Color.TRANSPARENT;
		int stippleColor = Color.BLACK;

		if (line != null) {
			color = line.color;
			fixed = line.fixed;
			fade = line.fade;
			cap = line.cap;
			blur = line.blur;
			stipple = line.stipple;
			stippleColor = line.stippleColor;
			stippleWidth = line.stippleWidth;
		}

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("id".equals(name))
				style = value;

			else if ("src".equals(name))
				;// src = value;

			else if ("use".equals(name))
				;// ignore

			else if ("outline".equals(name))
				;// ignore

			else if ("stroke".equals(name))
				color = Color.parseColor(value);

			else if ("width".equals(name) || "stroke-width".equals(name))
				width = Float.parseFloat(value);

			else if ("cap".equals(name) || "stroke-linecap".equals(name))
				cap = Cap.valueOf(value.toUpperCase());

			else if ("fix".equals(name))
				fixed = Boolean.parseBoolean(value);

			else if ("stipple".equals(name))
				stipple = Integer.parseInt(value);

			else if ("stipple-stroke".equals(name))
				stippleColor = Color.parseColor(value);

			else if ("stipple-width".equals(name))
				stippleWidth = Float.parseFloat(value);

			else if ("fade".equals(name))
				fade = Integer.parseInt(value);

			else if ("min".equals(name))
				; //min = Float.parseFloat(value);

			else if ("blur".equals(name))
				blur = Float.parseFloat(value);

			else if ("style".equals(name))
				; // ignore

			else if ("dasharray".equals(name))
				; // TBD

			else
				logUnknownAttribute(elementName, name, value, i);
		}

		// inherit properties from 'line'
		if (line != null) {
			// use stroke width relative to 'line'
			width = line.width + width;
			if (width <= 0)
				width = 1;

		} else if (!isOutline) {
			validateNonNegative("width", width);
		}

		return new Line(level, style, color, width, cap, fixed,
		                stipple, stippleColor, stippleWidth,
		                fade, blur, isOutline);
	}

	private void handleAreaElement(String localName, Attributes attributes, boolean isStyle)
	        throws SAXException {

		String use = attributes.getValue("use");
		Area style = null;

		if (use != null) {
			style = (Area) mStyles.get(AREA_STYLE + use);
			if (style == null) {
				log.debug("missing area style 'use': " + use);
				return;
			}
		}

		Area area = createArea(style, localName, attributes, mLevels);
		mLevels += 2;

		if (isStyle) {
			mStyles.put(AREA_STYLE + area.style, area);
		} else {
			mCurrentRule.addStyle(area);
		}
	}

	/**
	 * @return a new Area with the given rendering attributes.
	 */
	private static Area createArea(Area area, String elementName, Attributes attributes, int level) {
		String src = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 1;
		int fade = -1;
		int blend = -1;
		int blendFill = Color.TRANSPARENT;
		String style = null;

		TextureItem texture = null;

		if (area != null) {
			fill = area.color;
			blend = area.blend;
			blendFill = area.blendColor;
			fade = area.fade;
			// TODO texture = area.texture

			if (area.outline != null) {
				stroke = area.outline.color;
				strokeWidth = area.outline.width;
			}
		}

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("id".equals(name))
				style = value;

			else if ("use".equals(name))
				;// ignore

			else if ("src".equals(name))
				src = value;

			else if ("fill".equals(name))
				fill = Color.parseColor(value);

			else if ("stroke".equals(name))
				stroke = Color.parseColor(value);

			else if ("stroke-width".equals(name))
				strokeWidth = Float.parseFloat(value);

			else if ("fade".equals(name))
				fade = Integer.parseInt(value);

			else if ("blend".equals(name))
				blend = Integer.parseInt(value);

			else if ("blend-fill".equals(name))
				blendFill = Color.parseColor(value);

			else
				logUnknownAttribute(elementName, name, value, i);
		}
		validateNonNegative("stroke-width", strokeWidth);

		if (src != null) {
			try {
				Bitmap b = CanvasAdapter.g.loadBitmapAsset(src);
				if (b != null)
					texture = new TextureItem(b, true);
			} catch (Exception e) {
				log.debug(e.getMessage());
			}
		}
		return new Area(style, fill, stroke, strokeWidth, fade, level, blend,
		                blendFill, texture);
	}

	private void addOutline(String style) {
		if (style != null) {
			Line line = (Line) mStyles.get(OUTLINE_STYLE + style);
			if (line != null && line.outline)
				mCurrentRule.addStyle(line);
			else
				log.debug("BUG not an outline style: " + style);
		}
	}

	private void createAtlas(String elementName, Attributes attributes) throws IOException {
		String img = null;

		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("img".equals(name)) {
				img = value;
			} else {
				XmlThemeBuilder.logUnknownAttribute(elementName, name, value, i);
			}
		}
		validateExists("img", img, elementName);

		Bitmap bitmap = CanvasAdapter.g.loadBitmapAsset(IMG_PATH + img);
		mTextureAtlas = new TextureAtlas(bitmap);
	}

	private void createTextureRegion(String elementName, Attributes attributes) {
		String regionName = null;
		Rect r = null;

		for (int i = 0, n = attributes.getLength(); i < n; i++) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("id".equals(name)) {
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
				XmlThemeBuilder.logUnknownAttribute(elementName, name, value, i);
			}
		}
		validateExists("id", regionName, elementName);
		validateExists("pos", r, elementName);

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

	private void createRenderTheme(String elementName, Attributes attributes) {
		Integer version = null;
		int mapBackground = Color.WHITE;
		float baseStrokeWidth = 1;
		float baseTextSize = 1;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("schemaLocation".equals(name))
				continue;

			else if ("version".equals(name))
				version = Integer.valueOf(Integer.parseInt(value));

			else if ("map-background".equals(name))
				mapBackground = Color.parseColor(value);

			else if ("base-stroke-width".equals(name))
				baseStrokeWidth = Float.parseFloat(value);

			else if ("base-text-size".equals(name))
				baseTextSize = Float.parseFloat(value);

			else
				XmlThemeBuilder.logUnknownAttribute(elementName, name, value, i);

		}

		validateExists("version", version, elementName);

		if (version.intValue() != RENDER_THEME_VERSION)
			throw new ThemeException("invalid render theme version:"
			        + version);

		validateNonNegative("base-stroke-width", baseStrokeWidth);
		validateNonNegative("base-test-size", baseTextSize);

		mMapBackground = mapBackground;
		mBaseTextSize = baseTextSize;
	}

	/**
	 * @param caption
	 *            ...
	 * @return a new Text with the given rendering attributes.
	 */
	private Text createText(String elementName, Attributes attributes, boolean caption) {
		TextBuilder b = mTextBuilder.reset();

		b.caption = caption;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("id".equals(name))
				b.style = value;

			else if ("k".equals(name))
				b.textKey = value.intern();

			else if ("font-family".equals(name))
				b.fontFamily = FontFamily.valueOf(value.toUpperCase());

			else if ("style".equals(name))
				b.fontStyle = FontStyle.valueOf(value.toUpperCase());

			else if ("size".equals(name))
				b.fontSize = Float.parseFloat(value);

			else if ("fill".equals(name))
				b.color = Color.parseColor(value);

			else if ("stroke".equals(name))
				b.stroke = Color.parseColor(value);

			else if ("stroke-width".equals(name))
				b.strokeWidth = Float.parseFloat(value);

			else if ("caption".equals(name))
				b.caption = Boolean.parseBoolean(value);

			else if ("priority".equals(name))
				b.priority = Integer.parseInt(value);

			else if ("dy".equals(name))
				// NB: minus..
				b.dy = -Float.parseFloat(value);

			else if ("symbol".equals(name))
				b.texture = getAtlasRegion(value);

			else
				logUnknownAttribute(elementName, name, value, i);
		}

		validateExists("k", b.textKey, elementName);
		validateNonNegative("size", b.fontSize);
		validateNonNegative("stroke-width", b.strokeWidth);

		return b.buildInternal();
	}

	/**
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

			if ("r".equals(name) || "radius".equals(name))
				radius = Float.valueOf(Float.parseFloat(value));

			else if ("scale-radius".equals(name))
				scaleRadius = Boolean.parseBoolean(value);

			else if ("fill".equals(name))
				fill = Color.parseColor(value);

			else if ("stroke".equals(name))
				stroke = Color.parseColor(value);

			else if ("stroke-width".equals(name))
				strokeWidth = Float.parseFloat(value);

			else
				logUnknownAttribute(elementName, name, value, i);
		}

		validateExists("r", radius, elementName);
		validateNonNegative("radius", radius);
		validateNonNegative("stroke-width", strokeWidth);

		return new Circle(radius, scaleRadius, fill, stroke, strokeWidth, level);
	}

	/**
	 * @return a new LineSymbol with the given rendering attributes.
	 */
	private static LineSymbol createLineSymbol(String elementName, Attributes attributes) {
		String src = null;
		boolean alignCenter = false;
		boolean repeat = false;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name))
				src = value;

			else if ("align-center".equals(name))
				alignCenter = Boolean.parseBoolean(value);

			else if ("repeat".equals(name))
				repeat = Boolean.parseBoolean(value);

			else
				logUnknownAttribute(elementName, name, value, i);
		}

		validateExists("src", src, elementName);
		return new LineSymbol(src, alignCenter, repeat);
	}

	/**
	 * @return a new Symbol with the given rendering attributes.
	 */
	private Symbol createSymbol(String elementName, Attributes attributes) {
		String src = null;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("src".equals(name))
				src = value;
			else
				logUnknownAttribute(elementName, name, value, i);
		}

		validateExists("src", src, elementName);

		return new Symbol(getAtlasRegion(src));
	}

	private Extrusion createExtrusion(String elementName, Attributes attributes, int level) {
		int colorSide = 0;
		int colorTop = 0;
		int colorLine = 0;
		int defaultHeight = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("side-color".equals(name))
				colorSide = Color.parseColor(value);

			else if ("top-color".equals(name))
				colorTop = Color.parseColor(value);

			else if ("line-color".equals(name))
				colorLine = Color.parseColor(value);

			else if ("default-height".equals(name))
				defaultHeight = Integer.parseInt(value);

			else
				logUnknownAttribute(elementName, name, value, i);
		}

		return new Extrusion(level, colorSide, colorTop, colorLine, defaultHeight);
	}

	private static void validateNonNegative(String name, float value) {
		if (value < 0)
			throw new ThemeException(name + " must not be negative: "
			        + value);
	}

	private static void validateExists(String name, Object obj, String elementName) {
		if (obj == null)
			throw new ThemeException("missing attribute " + name
			        + " for element: " + elementName);
	}
}
