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
package org.oscim.theme.rule;

import org.oscim.core.Tag;

final class AnyMatcher implements AttributeMatcher {
	private static final AnyMatcher INSTANCE = new AnyMatcher();

	static AnyMatcher getInstance() {
		return INSTANCE;
	}

	/**
	 * Private constructor to prevent instantiation from other classes.
	 */
	private AnyMatcher() {
		// do nothing
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		return attributeMatcher == this;
	}

	@Override
	public boolean matches(Tag[] tags) {
		return true;
	}
}
