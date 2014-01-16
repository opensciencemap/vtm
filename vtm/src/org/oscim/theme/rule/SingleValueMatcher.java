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

class SingleValueMatcher implements AttributeMatcher {
	private final String mValue;

	SingleValueMatcher(String value) {
		mValue = value.intern();
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		Tag[] tags = { new Tag(null, mValue) };

		return attributeMatcher == this || attributeMatcher.matches(tags);
	}

	@Override
	public boolean matches(Tag[] tags) {
		for (Tag tag : tags)
			if (mValue == tag.value)
				return true;

		return false;
	}
}
