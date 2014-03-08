package org.oscim.theme.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.XmlThemeBuilder;
import org.oscim.theme.styles.RenderStyle;
import org.xml.sax.Attributes;

public class RuleBuilder {
	boolean positiveRule;

	int zoom;
	int element;
	int selector;

	AttributeMatcher keyMatcher;
	AttributeMatcher valueMatcher;

	ArrayList<RenderStyle> renderStyles = new ArrayList<RenderStyle>(4);
	ArrayList<RuleBuilder> subRules = new ArrayList<RuleBuilder>(4);

	private static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_KEY =
	        new HashMap<List<String>, AttributeMatcher>();
	private static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_VALUE =
	        new HashMap<List<String>, AttributeMatcher>();

	private static final String STRING_NEGATION = "~";
	private static final String STRING_EXCLUSIVE = "-";
	private static final String STRING_WILDCARD = "*";

	private static final int SELECT_FIRST = 1 << 0;
	private static final int SELECT_WHEN_MATCHED = 1 << 1;

	public RuleBuilder(boolean positive, int element, int zoom, int selector,
	        AttributeMatcher keyMatcher, AttributeMatcher valueMatcher) {
		this.positiveRule = positive;
		this.element = element;
		this.zoom = zoom;
		this.selector = selector;
		this.keyMatcher = keyMatcher;
		this.valueMatcher = valueMatcher;
	}

	private static RuleBuilder createRule(Stack<RuleBuilder> ruleStack, int element, String keys,
	        String values, byte zoomMin, byte zoomMax, int selector) {

		// zoom-level bitmask
		int zoom = 0;
		for (int z = zoomMin; z <= zoomMax && z < 32; z++)
			zoom |= (1 << z);

		List<String> keyList = null, valueList = null;
		boolean negativeRule = false;
		boolean exclusionRule = false;

		AttributeMatcher keyMatcher, valueMatcher = null;

		if (values == null) {
			valueMatcher = AnyMatcher.getInstance();
		} else {
			valueList = new ArrayList<String>(Arrays.asList(values.split("\\|")));
			if (valueList.remove(STRING_NEGATION))
				negativeRule = true;
			else if (valueList.remove(STRING_EXCLUSIVE))
				exclusionRule = true;
			else {
				valueMatcher = getValueMatcher(valueList);
				valueMatcher = RuleOptimizer.optimize(valueMatcher, ruleStack);
			}
		}

		if (keys == null) {
			if (negativeRule || exclusionRule) {
				throw new ThemeException("negative rule requires key");
			}
			keyMatcher = AnyMatcher.getInstance();
		} else {
			keyList = new ArrayList<String>(Arrays.asList(keys.split("\\|")));
			keyMatcher = getKeyMatcher(keyList);

			if ((keyMatcher instanceof AnyMatcher) && (negativeRule || exclusionRule)) {
				throw new ThemeException("negative rule requires key");
			}

			if (negativeRule) {
				AttributeMatcher m = new NegativeMatcher(keyList, valueList, false);
				return new RuleBuilder(false, element, zoom, selector, m, null);
			} else if (exclusionRule) {
				AttributeMatcher m = new NegativeMatcher(keyList, valueList, true);
				return new RuleBuilder(false, element, zoom, selector, m, null);
			}

			keyMatcher = RuleOptimizer.optimize(keyMatcher, ruleStack);
		}

		return new RuleBuilder(true, element, zoom, selector, keyMatcher, valueMatcher);
	}

	private static AttributeMatcher getKeyMatcher(List<String> keyList) {
		if (STRING_WILDCARD.equals(keyList.get(0))) {
			return AnyMatcher.getInstance();
		}

		AttributeMatcher attributeMatcher = MATCHERS_CACHE_KEY.get(keyList);
		if (attributeMatcher == null) {
			if (keyList.size() == 1) {
				attributeMatcher = new SingleKeyMatcher(keyList.get(0));
			} else {
				attributeMatcher = new MultiKeyMatcher(keyList);
			}
			MATCHERS_CACHE_KEY.put(keyList, attributeMatcher);
		}
		return attributeMatcher;
	}

	private static AttributeMatcher getValueMatcher(List<String> valueList) {
		if (STRING_WILDCARD.equals(valueList.get(0))) {
			return AnyMatcher.getInstance();
		}

		AttributeMatcher attributeMatcher = MATCHERS_CACHE_VALUE.get(valueList);
		if (attributeMatcher == null) {
			if (valueList.size() == 1) {
				attributeMatcher = new SingleValueMatcher(valueList.get(0));
			} else {
				attributeMatcher = new MultiValueMatcher(valueList);
			}
			MATCHERS_CACHE_VALUE.put(valueList, attributeMatcher);
		}
		return attributeMatcher;
	}

	private static void validate(byte zoomMin, byte zoomMax) {
		if (zoomMin < 0)
			throw new ThemeException("zoom-min must not be negative: " + zoomMin);
		else if (zoomMax < 0)
			throw new ThemeException("zoom-max must not be negative: " + zoomMax);
		else if (zoomMin > zoomMax)
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

		return createRule(ruleStack, element, keys, values, zoomMin, zoomMax, selector);
	}

	public Rule onComplete() {
		MATCHERS_CACHE_KEY.clear();
		MATCHERS_CACHE_VALUE.clear();

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

		if (positiveRule)
			return new PositiveRule(element, zoom, selector, keyMatcher,
			                        valueMatcher, rules, styles);
		else
			return new NegativeRule(element, zoom, selector, keyMatcher,
			                        rules, styles);
	}

	public void addStyle(RenderStyle style) {
		renderStyles.add(style);
	}

	public void addSubRule(RuleBuilder rule) {
		subRules.add(rule);
	}

}
