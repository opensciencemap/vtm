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
package org.oscim.theme;

import org.oscim.core.Tag;

class MatchingCacheKey {
	int mHashCodeValue;
	Tag[] mTags;

	MatchingCacheKey() {
	}

	MatchingCacheKey(Tag[] tags) {
		mTags = tags;
		mHashCodeValue = calculateHashCode();
	}

	MatchingCacheKey(MatchingCacheKey key) {
		mTags = key.mTags;
		mHashCodeValue = key.mHashCodeValue;
	}

	void set(Tag[] tags) {
		mTags = tags;

		int result = 7;

		for (int i = 0, n = mTags.length; i < n; i++) {
			if (mTags[i] == null)
				break;
			result = 31 * result + mTags[i].hashCode();
		}
		result = 31 * result;

		mHashCodeValue = result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		MatchingCacheKey other = (MatchingCacheKey) obj;

		if (mTags == null) {
			return (other.mTags == null);
		} else if (other.mTags == null)
			return false;

		int length = mTags.length;
		if (length != other.mTags.length) {
			return false;
		}

		for (int i = 0; i < length; i++) {
			if (mTags[i] == other.mTags[i])
				continue;
			if (mTags[i].key == other.mTags[i].key && mTags[i].value == other.mTags[i].value)
				continue;

			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return mHashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;

		for (int i = 0, n = mTags.length; i < n; i++) {
			if (mTags[i] == null) // FIXME
				break;
			result = 31 * result + mTags[i].hashCode();
		}
		result = 31 * result;

		return result;
	}
}
