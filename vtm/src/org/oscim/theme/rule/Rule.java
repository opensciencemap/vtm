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
package org.oscim.theme.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.oscim.core.Tag;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.xml.sax.Attributes;

public abstract class Rule {
	private static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_KEY =
	        new HashMap<List<String>, AttributeMatcher>();
	private static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_VALUE =
	        new HashMap<List<String>, AttributeMatcher>();

	private static final String STRING_NEGATION = "~";
	private static final String STRING_EXCLUSIVE = "-";
	private static final String STRING_WILDCARD = "*";

	private static Rule createRule(Stack<Rule> ruleStack, int element, String keys,
	        String values, byte zoomMin, byte zoomMax, boolean matchFirst) {

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
				return new NegativeRule(element, zoom, matchFirst, m);
			} else if (exclusionRule) {
				AttributeMatcher m = new NegativeMatcher(keyList, valueList, true);
				return new NegativeRule(element, zoom, matchFirst, m);
			}

			keyMatcher = RuleOptimizer.optimize(keyMatcher, ruleStack);
		}

		return new PositiveRule(element, zoom, matchFirst, keyMatcher, valueMatcher);
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

	public static Rule create(String elementName, Attributes attributes, Stack<Rule> ruleStack) {
		int element = Element.ANY;
		int closed = Closed.ANY;
		String keys = null;
		String values = null;
		byte zoomMin = 0;
		byte zoomMax = Byte.MAX_VALUE;
		boolean matchFirst = false;

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
				matchFirst = "first".equals(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		if (closed == Closed.YES)
			element = Element.POLY;
		else if (closed == Closed.NO)
			element = Element.LINE;

		validate(zoomMin, zoomMax);

		return createRule(ruleStack, element, keys, values, zoomMin, zoomMax, matchFirst);
	}

	private Rule[] mSubRules;
	private RenderInstruction[] mRenderInstructions;

	final int mZoom;
	final int mElement;
	final boolean mMatchFirst;

	static class Builder {
		ArrayList<RenderInstruction> renderInstructions = new ArrayList<RenderInstruction>(4);
		ArrayList<Rule> subRules = new ArrayList<Rule>(4);

		public void clear() {
			renderInstructions.clear();
			renderInstructions = null;
			subRules.clear();
			subRules = null;
		}
	}

	private Builder builder;

	Rule(int type, int zoom, boolean matchFirst) {
		builder = new Builder();
		mElement = type;
		mZoom = zoom;
		mMatchFirst = matchFirst;
	}

	public void addRenderingInstruction(RenderInstruction renderInstruction) {
		builder.renderInstructions.add(renderInstruction);
	}

	public void addSubRule(Rule rule) {
		builder.subRules.add(rule);
	}

	abstract boolean matchesTags(Tag[] tags);

	public boolean matchElement(int type, Tag[] tags, int zoomLevel,
	        List<RenderInstruction> matchingList) {

		if (((mElement & type) != 0) && ((mZoom & zoomLevel) != 0) && (matchesTags(tags))) {

			boolean matched = false;

			// check subrules
			for (Rule subRule : mSubRules) {
				if (subRule.matchElement(type, tags, zoomLevel, matchingList) && mMatchFirst) {
					matched = true;
					break;
				}
			}

			if (!mMatchFirst || matched) {
				// add instructions for this rule
				for (RenderInstruction ri : mRenderInstructions)
					matchingList.add(ri);
			}

			// this rule did match
			return true;
		}

		// this rule did not match
		return false;
	}

	public void onComplete() {
		MATCHERS_CACHE_KEY.clear();
		MATCHERS_CACHE_VALUE.clear();

		mRenderInstructions = new RenderInstruction[builder.renderInstructions.size()];
		builder.renderInstructions.toArray(mRenderInstructions);

		mSubRules = new Rule[builder.subRules.size()];
		builder.subRules.toArray(mSubRules);

		builder.clear();
		builder = null;

		for (Rule subRule : mSubRules)
			subRule.onComplete();
	}

	public void onDestroy() {
		for (RenderInstruction ri : mRenderInstructions)
			ri.destroy();

		for (Rule subRule : mSubRules)
			subRule.onDestroy();
	}

	public void scaleStrokeWidth(float scaleFactor) {
		for (RenderInstruction ri : mRenderInstructions)
			ri.scaleStrokeWidth(scaleFactor);

		for (Rule subRule : mSubRules)
			subRule.scaleStrokeWidth(scaleFactor);
	}

	public void scaleTextSize(float scaleFactor) {
		for (RenderInstruction ri : mRenderInstructions)
			ri.scaleTextSize(scaleFactor);
		for (Rule subRule : mSubRules)
			subRule.scaleTextSize(scaleFactor);
	}
}
