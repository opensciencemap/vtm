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

import java.util.Stack;

final class RuleOptimizer {

	private static AttributeMatcher optimizeKeyMatcher(AttributeMatcher attributeMatcher,
	        Stack<RuleBuilder> ruleStack) {
		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i).positiveRule) {
				RuleBuilder positiveRule = ruleStack.get(i);

				if (positiveRule.keyMatcher != null
				        && positiveRule.keyMatcher.isCoveredBy(attributeMatcher)) {
					return null;
				}
			}
		}

		return attributeMatcher;
	}

	private static AttributeMatcher optimizeValueMatcher(
	        AttributeMatcher attributeMatcher, Stack<RuleBuilder> ruleStack) {
		for (int i = 0, n = ruleStack.size(); i < n; ++i) {
			if (ruleStack.get(i).positiveRule) {
				RuleBuilder positiveRule = ruleStack.get(i);

				if (positiveRule.valueMatcher != null
				        && positiveRule.valueMatcher.isCoveredBy(attributeMatcher)) {
					return null;
				}
			}
		}

		return attributeMatcher;
	}

	static AttributeMatcher optimize(AttributeMatcher attributeMatcher,
	        Stack<RuleBuilder> ruleStack) {
		if (attributeMatcher instanceof AnyMatcher)
			return attributeMatcher;// return null;
		else if (attributeMatcher instanceof NegativeMatcher) {
			return attributeMatcher;
		} else if (attributeMatcher instanceof SingleKeyMatcher) {
			return optimizeKeyMatcher(attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof SingleValueMatcher) {
			return optimizeValueMatcher(attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof MultiKeyMatcher) {
			return optimizeKeyMatcher(attributeMatcher, ruleStack);
		} else if (attributeMatcher instanceof MultiValueMatcher) {
			return optimizeValueMatcher(attributeMatcher, ruleStack);
		}
		throw new IllegalArgumentException("unknown AttributeMatcher: "
		        + attributeMatcher);
	}

	// static ClosedMatcher optimize(ClosedMatcher closedMatcher, Stack<Rule> ruleStack) {
	// if (closedMatcher == null) {
	// return null;
	// }
	//
	// if (closedMatcher instanceof AnyMatcher) {
	// return null;
	// }
	//
	// for (int i = 0, n = ruleStack.size(); i < n; ++i) {
	// ClosedMatcher matcher = ruleStack.get(i).mClosedMatcher;
	// if (matcher == null)
	// return null;
	//
	// if (matcher.isCoveredBy(closedMatcher)) {
	// return null; // AnyMatcher.getInstance();
	//
	// } else if (!closedMatcher.isCoveredBy(ruleStack.get(i).mClosedMatcher)) {
	// LOG.warning("unreachable rule (closed)");
	// }
	// }
	//
	// return closedMatcher;
	// }
	//
	// static ElementMatcher optimize(ElementMatcher elementMatcher, Stack<Rule> ruleStack) {
	//
	// if (elementMatcher == null) {
	// return null;
	// }
	//
	// if (elementMatcher instanceof AnyMatcher) {
	// return null;
	// }
	//
	// for (int i = 0, n = ruleStack.size(); i < n; ++i) {
	// ElementMatcher matcher = ruleStack.get(i).mElementMatcher;
	//
	// if (matcher == null)
	// return null;
	//
	// if (matcher.isCoveredBy(elementMatcher)) {
	// return null; // AnyMatcher.getInstance();
	//
	// } else if (!elementMatcher.isCoveredBy(ruleStack.get(i).mElementMatcher)) {
	// LOG.warning("unreachable rule (e)");
	// }
	// }
	//
	// return elementMatcher;
	// }

	/**
	 * Private constructor to prevent instantiation from other classes.
	 */
	private RuleOptimizer() {
		// do nothing
	}
}
