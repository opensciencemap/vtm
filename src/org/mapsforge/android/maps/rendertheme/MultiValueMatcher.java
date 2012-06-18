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
package org.mapsforge.android.maps.rendertheme;

import java.util.List;

import org.mapsforge.core.Tag;

class MultiValueMatcher implements AttributeMatcher {
	private final String[] mValues;

	MultiValueMatcher(List<String> values) {
		mValues = new String[values.size()];
		for (int i = 0, n = mValues.length; i < n; ++i) {
			mValues[i] = values.get(i).intern();
		}
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		if (attributeMatcher == this) {
			return true;
		}
		Tag[] tags = new Tag[mValues.length];

		int i = 0;
		for (String val : mValues) {
			tags[i++] = new Tag(null, val);
		}
		return attributeMatcher.matches(tags);
	}

	@Override
	public boolean matches(Tag[] tags) {
		for (Tag tag : tags)
			for (String val : mValues)
				if (val == tag.value)
					return true;

		return false;
	}
}
