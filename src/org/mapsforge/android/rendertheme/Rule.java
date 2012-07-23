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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Pattern;

import org.mapsforge.android.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.core.Tag;
import org.xml.sax.Attributes;

abstract class Rule {
	private static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_KEY = new HashMap<List<String>, AttributeMatcher>();
	private static final Map<List<String>, AttributeMatcher> MATCHERS_CACHE_VALUE = new HashMap<List<String>, AttributeMatcher>();
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\|");
	private static final String STRING_NEGATION = "~";
	private static final String STRING_WILDCARD = "*";

	private static Rule createRule(Stack<Rule> ruleStack, int element, String keys,
			String values, int closed,
			byte zoomMin, byte zoomMax) {

		List<String> keyList = new ArrayList<String>(Arrays.asList(SPLIT_PATTERN
				.split(keys)));
		List<String> valueList = new ArrayList<String>(Arrays.asList(SPLIT_PATTERN
				.split(values)));

		if (valueList.remove(STRING_NEGATION)) {
			AttributeMatcher attributeMatcher = new NegativeMatcher(keyList, valueList);
			return new NegativeRule(element, closed, zoomMin, zoomMax,
					attributeMatcher);
		}

		AttributeMatcher keyMatcher = getKeyMatcher(keyList);
		AttributeMatcher valueMatcher = getValueMatcher(valueList);

		keyMatcher = RuleOptimizer.optimize(keyMatcher, ruleStack);
		valueMatcher = RuleOptimizer.optimize(valueMatcher, ruleStack);

		return new PositiveRule(element, closed, zoomMin, zoomMax,
				keyMatcher, valueMatcher);
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

	private static void validate(String elementName, String keys, String values,
			byte zoomMin, byte zoomMax) {
		if (keys == null) {
			throw new IllegalArgumentException("missing attribute k for element: "
					+ elementName);
		} else if (values == null) {
			throw new IllegalArgumentException("missing attribute v for element: "
					+ elementName);
		} else if (zoomMin < 0) {
			throw new IllegalArgumentException("zoom-min must not be negative: "
					+ zoomMin);
		} else if (zoomMax < 0) {
			throw new IllegalArgumentException("zoom-max must not be negative: "
					+ zoomMax);
		} else if (zoomMin > zoomMax) {
			throw new IllegalArgumentException(
					"zoom-min must be less or equal zoom-max: " + zoomMin);
		}
	}

	static Rule create(String elementName, Attributes attributes, Stack<Rule> ruleStack) {
		int element = Element.ANY;
		String keys = null;
		String values = null;
		int closed = Closed.ANY;
		byte zoomMin = 0;
		byte zoomMax = Byte.MAX_VALUE;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("e".equals(name)) {
				String val = value.toUpperCase(Locale.ENGLISH);
				if ("WAY".equals(val))
					element = Element.WAY;
				else if ("NODE".equals(val))
					element = Element.NODE;
			} else if ("k".equals(name)) {
				keys = value;
			} else if ("v".equals(name)) {
				values = value;
			} else if ("closed".equals(name)) {
				String val = value.toUpperCase(Locale.ENGLISH);
				if ("YES".equals(val))
					closed = Closed.YES;
				else if ("NO".equals(val))
					closed = Closed.NO;
			} else if ("zoom-min".equals(name)) {
				zoomMin = Byte.parseByte(value);
			} else if ("zoom-max".equals(name)) {
				zoomMax = Byte.parseByte(value);
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(elementName, keys, values, zoomMin, zoomMax);
		return createRule(ruleStack, element, keys, values, closed, zoomMin, zoomMax);
	}

	private ArrayList<RenderInstruction> mRenderInstructions;
	private ArrayList<Rule> mSubRules;

	private Rule[] mSubRuleArray;
	private RenderInstruction[] mRenderInstructionArray;

	final byte mZoomMax;
	final byte mZoomMin;
	final int mElement;
	final int mClosed;

	Rule(int element, int closed, byte zoomMin,
			byte zoomMax) {

		mClosed = closed;
		mElement = element;
		mZoomMin = zoomMin;
		mZoomMax = zoomMax;

		mRenderInstructions = new ArrayList<RenderInstruction>(4);
		mSubRules = new ArrayList<Rule>(4);
	}

	void addRenderingInstruction(RenderInstruction renderInstruction) {
		mRenderInstructions.add(renderInstruction);
	}

	void addSubRule(Rule rule) {
		mSubRules.add(rule);
	}

	abstract boolean matchesNode(Tag[] tags, byte zoomLevel);

	abstract boolean matchesWay(Tag[] tags, byte zoomLevel, int closed);

	void matchNode(IRenderCallback renderCallback, Tag[] tags, byte zoomLevel) {
		if (matchesNode(tags, zoomLevel)) {
			for (int i = 0, n = mRenderInstructionArray.length; i < n; i++)
				mRenderInstructionArray[i].renderNode(renderCallback, tags);

			for (int i = 0, n = mSubRuleArray.length; i < n; i++)
				mSubRuleArray[i].matchNode(renderCallback, tags, zoomLevel);

		}
	}

	void matchWay(IRenderCallback renderCallback, Tag[] tags, byte zoomLevel,
			int closed,
			List<RenderInstruction> matchingList) {

		if (matchesWay(tags, zoomLevel, closed)) {
			for (int i = 0, n = mRenderInstructionArray.length; i < n; i++)
				matchingList.add(mRenderInstructionArray[i]);

			for (int i = 0, n = mSubRuleArray.length; i < n; i++)
				mSubRuleArray[i].matchWay(renderCallback, tags, zoomLevel, closed,
						matchingList);

		}
	}

	void onComplete() {
		MATCHERS_CACHE_KEY.clear();
		MATCHERS_CACHE_VALUE.clear();

		mRenderInstructionArray = new RenderInstruction[mRenderInstructions.size()];

		for (int i = 0, n = mRenderInstructions.size(); i < n; i++)
			mRenderInstructionArray[i] = mRenderInstructions.get(i);

		mSubRuleArray = new Rule[mSubRules.size()];

		for (int i = 0, n = mSubRules.size(); i < n; i++)
			mSubRuleArray[i] = mSubRules.get(i);

		mRenderInstructions.clear();
		mRenderInstructions = null;
		mSubRules.clear();
		mSubRules = null;

		for (int i = 0, n = mSubRuleArray.length; i < n; i++)
			mSubRuleArray[i].onComplete();

	}

	void onDestroy() {
		for (int i = 0, n = mRenderInstructionArray.length; i < n; i++)
			mRenderInstructionArray[i].destroy();

		for (int i = 0, n = mSubRuleArray.length; i < n; i++)
			mSubRuleArray[i].onDestroy();

	}

	void scaleStrokeWidth(float scaleFactor) {
		for (int i = 0, n = mRenderInstructionArray.length; i < n; i++)
			mRenderInstructionArray[i].scaleStrokeWidth(scaleFactor);

		for (int i = 0, n = mSubRuleArray.length; i < n; i++)
			mSubRuleArray[i].scaleStrokeWidth(scaleFactor);

	}

	void scaleTextSize(float scaleFactor) {
		for (int i = 0, n = mRenderInstructionArray.length; i < n; i++)
			mRenderInstructionArray[i].scaleTextSize(scaleFactor);

		for (int i = 0, n = mSubRuleArray.length; i < n; i++)
			mSubRuleArray[i].scaleTextSize(scaleFactor);

	}
}
