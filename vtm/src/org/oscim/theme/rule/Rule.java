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

	Rule(int element, int zoom, int selector, Rule[] subRules, RenderStyle[] styles) {
		this.element = element;
		this.zoom = zoom;
		this.subRules = subRules;
		this.styles = styles;

	}

	abstract boolean matchesTags(Tag[] tags);

	public boolean matchElement(int type, Tag[] tags, int zoomLevel,
	        List<RenderStyle> matchingList) {

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
				for (RenderStyle ri : mRenderInstructions)
					matchingList.add(ri);
			}

			// this rule did match
			return true;
		}

		// this rule did not match
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
