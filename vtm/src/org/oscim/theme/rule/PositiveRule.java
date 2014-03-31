/*
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

import org.oscim.core.Tag;
import org.oscim.theme.styles.RenderStyle;

class PositiveRule {

	static class PositiveRuleK extends Rule {
		private final String mKey;

		PositiveRuleK(int element, int zoom, int selector, String key,
		        Rule[] subRules, RenderStyle[] styles) {

			super(element, zoom, selector, subRules, styles);
			mKey = key;
		}

		@Override
		boolean matchesTags(Tag[] tags) {
			for (Tag tag : tags)
				if (mKey == tag.key)
					return true;

			return false;
		}
	}

	static class PositiveRuleV extends Rule {
		private final String mValue;

		PositiveRuleV(int element, int zoom, int selector, String value,
		        Rule[] subRules, RenderStyle[] styles) {

			super(element, zoom, selector, subRules, styles);
			mValue = value;
		}

		@Override
		boolean matchesTags(Tag[] tags) {
			for (Tag tag : tags)
				if (mValue == tag.value)
					return true;

			return false;
		}
	}

	static class PositiveRuleKV extends Rule {
		private final String mKey;
		private final String mValue;

		PositiveRuleKV(int element, int zoom, int selector, String key, String value,
		        Rule[] subRules, RenderStyle[] styles) {

			super(element, zoom, selector, subRules, styles);
			mKey = key;
			mValue = value;
		}

		@Override
		boolean matchesTags(Tag[] tags) {
			for (Tag tag : tags)
				if (mKey == tag.key)
					return (mValue == tag.value);

			return false;
		}
	}

	static class PositiveRuleMultiKV extends Rule {
		private final String mKeys[];
		private final String mValues[];

		PositiveRuleMultiKV(int element, int zoom, int selector, String keys[], String values[],
		        Rule[] subRules, RenderStyle[] styles) {

			super(element, zoom, selector, subRules, styles);
			if (keys.length == 0) {
				mKeys = null;
			} else {
				for (int i = 0; i < keys.length; i++)
					keys[i] = keys[i].intern();
				mKeys = keys;
			}

			if (values.length == 0) {
				mValues = null;
			} else {
				for (int i = 0; i < values.length; i++)
					values[i] = values[i].intern();
				mValues = values;
			}
		}

		@Override
		boolean matchesTags(Tag[] tags) {
			if (mKeys == null) {
				for (Tag tag : tags) {
					for (String value : mValues) {
						if (value == tag.value)
							return true;
					}
				}
				return false;
			}

			for (Tag tag : tags)
				for (String key : mKeys) {
					if (key == tag.key) {
						if (mValues == null)
							return true;

						for (String value : mValues) {
							if (value == tag.value)
								return true;
						}
					}
				}
			return false;
		}
	}

	public static Rule create(int element, int zoom, int selector,
	        String[] keys, String values[], Rule[] subRules, RenderStyle[] styles) {
		int numKeys = keys.length;
		int numVals = values.length;

		if (numKeys == 0 && numVals == 0)
			return new Rule(element, zoom, selector, subRules, styles);

		for (int i = 0; i < numVals; i++)
			values[i] = values[i].intern();

		for (int i = 0; i < numKeys; i++)
			keys[i] = keys[i].intern();

		if (numKeys == 1 && numKeys == 0) {
			return new PositiveRuleK(element, zoom, selector, keys[0], subRules, styles);
		}

		if (numKeys == 0 && numVals == 1) {
			return new PositiveRuleV(element, zoom, selector, values[0], subRules, styles);
		}

		if (numKeys == 1 && numVals == 1)
			return new PositiveRuleKV(element, zoom, selector, keys[0], values[0], subRules, styles);

		return new PositiveRuleMultiKV(element, zoom, selector, keys, values, subRules, styles);

	}
}
