package org.oscim.theme.rule;

import java.util.ArrayList;
import java.util.Stack;

import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.rule.Rule.Element;
import org.oscim.theme.rule.Rule.NegativeRule;
import org.oscim.theme.rule.Rule.PositiveRuleK;
import org.oscim.theme.rule.Rule.PositiveRuleKV;
import org.oscim.theme.rule.Rule.PositiveRuleMultiKV;
import org.oscim.theme.rule.Rule.PositiveRuleV;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.RenderStyle.StyleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			if (keyList == null || keyList.length == 0)
				throw new ThemeException("negative rule requires key");
		}

		return new RuleBuilder(type, keyList, valueList);
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

		int numKeys = keys.length;
		int numVals = values.length;

		if (numKeys == 0 && numVals == 0)
			return new Rule(element, zoom, selector, rules, styles);

		for (int i = 0; i < numVals; i++)
			values[i] = values[i].intern();

		for (int i = 0; i < numKeys; i++)
			keys[i] = keys[i].intern();

		if (type != RuleType.POSITIVE)
			return new NegativeRule(type, element, zoom, selector,
			                        keys, values, rules, styles);

		if (numKeys == 1 && numKeys == 0) {
			return new PositiveRuleK(element, zoom, selector, keys[0],
			                         rules, styles);
		}

		if (numKeys == 0 && numVals == 1) {
			return new PositiveRuleV(element, zoom, selector, values[0],
			                         rules, styles);
		}

		if (numKeys == 1 && numVals == 1)
			return new PositiveRuleKV(element, zoom, selector,
			                          keys[0], values[0], rules, styles);

		return new PositiveRuleMultiKV(element, zoom, selector,
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
