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

import java.util.List;

import org.oscim.core.Tag;
import org.oscim.theme.styles.RenderStyle;

public abstract class Rule {

	private final Rule[] subRules;
	private final RenderStyle[] styles;

	private final int zoom;
	private final int element;
	private final boolean selectFirstMatch;
	private final boolean selectWhenMatched;

	Rule(int element, int zoom, int selector, Rule[] subRules, RenderStyle[] styles) {
		this.element = element;
		this.zoom = zoom;
		this.subRules = subRules;
		this.styles = styles;

		selectFirstMatch = (selector & Selector.FIRST) != 0;
		selectWhenMatched = (selector & Selector.WHEN_MATCHED) != 0;
	}

	abstract boolean matchesTags(Tag[] tags);

	public boolean matchElement(int type, Tag[] tags, int zoomLevel, List<RenderStyle> result) {

		if (((element & type) != 0) && ((zoom & zoomLevel) != 0) && (matchesTags(tags))) {
			boolean matched = false;

			if (subRules != null) {
				if (selectFirstMatch) {
					/* only add first matching rule and when-matched rules iff a
					 * previous rule matched */
					for (Rule r : subRules) {
						/* continue if matched xor selectWhenMatch */
						if (matched ^ r.selectWhenMatched)
							continue;

						if (r.matchElement(type, tags, zoomLevel, result))
							matched = true;
					}
				} else {
					/* add all rules and when-matched rules iff a previous rule
					 * matched */
					for (Rule r : subRules) {
						if (r.selectWhenMatched && !matched)
							continue;

						if (r.matchElement(type, tags, zoomLevel, result))
							matched = true;
					}
				}
			}

			if (styles == null)
				/* matched if styles where added */
				return matched;

			/* add instructions for this rule */
			for (RenderStyle ri : styles)
				result.add(ri);

			/* this rule did not match */
			return true;
		}

		/* this rule did not match */
		return false;
	}

	public void onDestroy() {
		if (styles != null)
			for (RenderStyle ri : styles)
				ri.destroy();

		if (subRules != null)
			for (Rule subRule : subRules)
				subRule.onDestroy();
	}

	public void scaleTextSize(float scaleFactor) {
		if (styles != null)
			for (RenderStyle ri : styles)
				ri.scaleTextSize(scaleFactor);

		if (subRules != null)
			for (Rule subRule : subRules)
				subRule.scaleTextSize(scaleFactor);
	}

	public void updateStyles() {
		if (styles != null)
			for (RenderStyle ri : styles)
				ri.update();

		if (subRules != null)
			for (Rule subRule : subRules)
				subRule.updateStyles();
	}

	public static class RuleVisitor {
		boolean apply(Rule r) {
			if (r.subRules != null)
				for (Rule subRule : r.subRules)
					this.apply(subRule);

			return true;
		}
	}

	public static class UpdateVisitor extends RuleVisitor {
		@Override
		boolean apply(Rule r) {
			if (r.styles != null)
				for (RenderStyle ri : r.styles)
					ri.update();

			return super.apply(r);
		}
	}

	public boolean apply(RuleVisitor v) {

		return v.apply(this);
	}
}
