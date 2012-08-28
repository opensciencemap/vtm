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

import java.util.List;

import org.mapsforge.core.Tag;

class NegativeMatcher implements AttributeMatcher {
	private final String[] mKeyList;
	private final String[] mValueList;
	private final boolean mExclusive;

	NegativeMatcher(List<String> keyList, List<String> valueList, boolean exclusive) {
		mKeyList = new String[keyList.size()];
		for (int i = 0; i < mKeyList.length; i++)
			mKeyList[i] = keyList.get(i).intern();

		mValueList = new String[valueList.size()];
		for (int i = 0; i < mValueList.length; i++)
			mValueList[i] = valueList.get(i).intern();

		mExclusive = exclusive;
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		return false;
	}

	@Override
	public boolean matches(Tag[] tags) {
		if (keyListDoesNotContainKeys(tags)) {
			return true;
		}

		for (Tag tag : tags) {
			for (String value : mValueList)
				if (value == tag.value)
					return !mExclusive;
		}
		return mExclusive;
	}

	private boolean keyListDoesNotContainKeys(Tag[] tags) {
		for (Tag tag : tags) {
			for (String key : mKeyList)
				if (key == tag.key)
					return false;

		}
		return true;
	}
}
