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

import org.oscim.core.Tag;
import org.oscim.theme.rule.RuleBuilder.RuleType;
import org.oscim.theme.styles.RenderStyle;

class NegativeRule extends Rule {

	public final String[] keys;
	public final String[] values;

	/* (-) 'exclusive negation' matches when either KEY is not present
	 * or KEY is present and any VALUE is NOT present
	 * 
	 * (\) 'except negation' matches when KEY is present
	 * none items of VALUE is present (TODO).
	 * (can be emulated by <m k="a"><m k=a v="-|b|c">...</m></m>)
	 * 
	 * (~) 'non-exclusive negation' matches when either KEY is not present
	 * or KEY is present and any VALUE is present */

	public final boolean exclusive;

	NegativeRule(RuleType type, int element, int zoom, int selector,
	        String[] keys, String[] values,
	        Rule[] subRules, RenderStyle[] styles) {
		super(element, zoom, selector, subRules, styles);

		for (int i = 0; i < keys.length; i++)
			keys[i] = keys[i].intern();

		for (int i = 0; i < values.length; i++)
			values[i] = values[i].intern();

		this.keys = keys;
		this.values = values;
		this.exclusive = type == RuleType.EXCLUDE;
	}

	@Override
	public boolean matchesTags(Tag[] tags) {
		if (keyListDoesNotContainKeys(tags))
			return true;

		for (Tag tag : tags)
			for (String value : values)
				if (value == tag.value)
					return !exclusive;

		return exclusive;
	}

	private boolean keyListDoesNotContainKeys(Tag[] tags) {
		for (Tag tag : tags)
			for (String key : keys)
				if (key == tag.key)
					return false;

		return true;
	}
}
