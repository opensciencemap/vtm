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

public class Rule {
	public final static RenderStyle[] EMPTY_STYLE = new RenderStyle[0];
	public final static Rule[] EMPTY_RULES = new Rule[0];

	private final Rule[] subRules;
	public final RenderStyle[] styles;

	private final int zoom;
	private final int element;
	private final boolean selectFirstMatch;
	private final boolean selectWhenMatched;

	Rule(int element, int zoom, int selector, Rule[] subRules, RenderStyle[] styles) {
		this.element = element;
		this.zoom = zoom;

		this.subRules = (subRules == null) ? EMPTY_RULES : subRules;
		this.styles = (styles == null) ? EMPTY_STYLE : styles;

		selectFirstMatch = (selector & Selector.FIRST) != 0;
		selectWhenMatched = (selector & Selector.WHEN_MATCHED) != 0;
	}

	boolean matchesTags(Tag[] tags) {
		return true;
	}

	public boolean matchElement(int type, Tag[] tags, int zoomLevel, List<RenderStyle> result) {

		if (((element & type) != 0) && ((zoom & zoomLevel) != 0) && (matchesTags(tags))) {
			boolean matched = false;

			if (subRules != EMPTY_RULES) {
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

			if (styles == EMPTY_STYLE)
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

	public void dispose() {
		for (RenderStyle ri : styles)
			ri.dispose();

		for (Rule subRule : subRules)
			subRule.dispose();
	}

	public void scaleTextSize(float scaleFactor) {
		for (RenderStyle ri : styles)
			ri.scaleTextSize(scaleFactor);

		for (Rule subRule : subRules)
			subRule.scaleTextSize(scaleFactor);
	}

	public void updateStyles() {
		for (RenderStyle ri : styles)
			ri.update();

		for (Rule subRule : subRules)
			subRule.updateStyles();
	}

	public static class RuleVisitor {
		public void apply(Rule r) {
			for (Rule subRule : r.subRules)
				this.apply(subRule);
		}
	}

	public static class TextSizeVisitor extends RuleVisitor {
		float scaleFactor = 1;

		public void setScaleFactor(float scaleFactor) {
			this.scaleFactor = scaleFactor;
		}

		@Override
		public void apply(Rule r) {
			for (RenderStyle ri : r.styles)
				ri.scaleTextSize(scaleFactor);
			super.apply(r);
		}
	}

	public static class UpdateVisitor extends RuleVisitor {
		@Override
		public void apply(Rule r) {
			for (RenderStyle ri : r.styles)
				ri.update();
			super.apply(r);
		}
	}

	public void apply(RuleVisitor v) {
		v.apply(this);
	}
}
