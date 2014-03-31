package org.oscim.theme.rule;

import java.util.ArrayList;
import java.util.Stack;

import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.XmlThemeBuilder;
import org.oscim.theme.styles.RenderStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

public class RuleBuilder {
	final static Logger log = LoggerFactory.getLogger(RuleBuilder.class);

	private final static String[] EMPTY_KV = {};

	public enum RuleType {
		POSITIVE,
		NEGATIVE,
		EXCLUDE
	}

	int zoom;
	int element;
	int selector;
	RuleType type;

	String keys[];
	String values[];

	ArrayList<RenderStyle> renderStyles = new ArrayList<RenderStyle>(4);
	ArrayList<RuleBuilder> subRules = new ArrayList<RuleBuilder>(4);

	private static final String STRING_NEGATION = "~";
	private static final String STRING_EXCLUSIVE = "-";
	private static final String SEPARATOR = "\\|";
	//private static final String STRING_WILDCARD = "*";

	private static final int SELECT_FIRST = 1 << 0;
	private static final int SELECT_WHEN_MATCHED = 1 << 1;

	public RuleBuilder(RuleType type, int element, int zoom, int selector,
	        String[] keys, String[] values) {
		this.type = type;
		this.element = element;
		this.zoom = zoom;
		this.selector = selector;
		this.keys = keys;
		this.values = values;
	}

	public RuleBuilder(RuleType type, String[] keys, String[] values) {
		this.element = Element.ANY;
		this.zoom = ~0;
		this.type = type;
		this.keys = keys;
		this.values = values;
	}

	public RuleBuilder() {
		this.type = RuleType.POSITIVE;
		this.element = Element.ANY;
		this.zoom = ~0;
		this.keys = EMPTY_KV;
		this.values = EMPTY_KV;
	}

	public static RuleBuilder create(Stack<RuleBuilder> ruleStack, String keys, String values) {

		String[] keyList = EMPTY_KV;
		String[] valueList = EMPTY_KV;
		RuleType type = RuleType.POSITIVE;

		if (values != null) {
			if (values.startsWith(STRING_NEGATION)) {
				type = RuleType.NEGATIVE;
				if (values.length() > 2)
					valueList = values.substring(2)
					    .split(SEPARATOR);
			} else if (values.startsWith(STRING_EXCLUSIVE)) {
				type = RuleType.EXCLUDE;
				if (values.length() > 2)
					valueList = values.substring(2)
					    .split(SEPARATOR);
			} else {
				valueList = values.split(SEPARATOR);
			}
		}

		if (keys != null) {
			keyList = keys.split("\\|");
		}

		if (type != RuleType.POSITIVE) {
			if (keyList == null || keyList.length == 0) {
				throw new ThemeException("negative rule requires key");
			}
		}

		return new RuleBuilder(type, keyList, valueList);
	}

	private static void validate(byte zoomMin, byte zoomMax) {
		XmlThemeBuilder.validateNonNegative("zoom-min", zoomMin);
		XmlThemeBuilder.validateNonNegative("zoom-max", zoomMax);
		if (zoomMin > zoomMax)
			throw new ThemeException("zoom-min must be less or equal zoom-max: " + zoomMin);
	}

	public static RuleBuilder create(String elementName, Attributes attributes,
	        Stack<RuleBuilder> ruleStack) {
		int element = Element.ANY;
		int closed = Closed.ANY;
		String keys = null;
		String values = null;
		byte zoomMin = 0;
		byte zoomMax = Byte.MAX_VALUE;
		int selector = 0;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("e".equals(name)) {
				String val = value.toUpperCase();
				if ("WAY".equals(val))
					element = Element.WAY;
				else if ("NODE".equals(val))
					element = Element.NODE;
			} else if ("k".equals(name)) {
				keys = value;
			} else if ("v".equals(name)) {
				values = value;
			} else if ("closed".equals(name)) {
				String val = value.toUpperCase();
				if ("YES".equals(val))
					closed = Closed.YES;
				else if ("NO".equals(val))
					closed = Closed.NO;
			} else if ("zoom-min".equals(name)) {
				zoomMin = Byte.parseByte(value);
			} else if ("zoom-max".equals(name)) {
				zoomMax = Byte.parseByte(value);
			} else if ("select".equals(name)) {
				if ("first".equals(value))
					selector |= SELECT_FIRST;
				if ("when-matched".equals(value))
					selector |= SELECT_WHEN_MATCHED;
			} else {
				XmlThemeBuilder.logUnknownAttribute(elementName, name, value, i);
			}
		}

		if (closed == Closed.YES)
			element = Element.POLY;
		else if (closed == Closed.NO)
			element = Element.LINE;

		validate(zoomMin, zoomMax);

		RuleBuilder b = create(ruleStack, keys, values);
		b.setZoom(zoomMin, zoomMax);
		b.element = element;
		b.selector = selector;
		return b;
	}

	public RuleBuilder setZoom(byte zoomMin, byte zoomMax) {
		// zoom-level bitmask
		zoom = 0;
		for (int z = zoomMin; z <= zoomMax && z < 32; z++)
			zoom |= (1 << z);

		return this;
	}

	public Rule onComplete() {

		RenderStyle[] styles = null;
		Rule[] rules = null;

		if (renderStyles.size() > 0) {
			styles = new RenderStyle[renderStyles.size()];
			renderStyles.toArray(styles);
		}

		if (subRules.size() > 0) {
			rules = new Rule[subRules.size()];
			for (int i = 0; i < rules.length; i++)
				rules[i] = subRules.get(i).onComplete();
		}

		if (type != RuleType.POSITIVE)
			return new NegativeRule(type, element, zoom, selector,
			                        keys, values, rules, styles);
		else
			return PositiveRule.create(element, zoom, selector,
			                           keys, values, rules, styles);
	}

	public void addStyle(RenderStyle style) {
		renderStyles.add(style);
	}

	public void addSubRule(RuleBuilder rule) {
		subRules.add(rule);
	}

	RuleBuilder(boolean positive) {
		this.positiveRule = positive;
		this.element = Element.ANY;
		this.zoom = ~0;
	}

	public static RuleBuilder get() {
		return new RuleBuilder(true);
	}

	public RuleBuilder select(int selector) {
		this.selector = selector;
		return this;
	}

	public RuleBuilder zoom(int zoom) {
		this.zoom = zoom;
		return this;
	}

	public RuleBuilder element(int element) {
		this.element = element;
		return this;
	}
}
