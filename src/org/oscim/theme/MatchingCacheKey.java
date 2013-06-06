/*
 * Copyright 2013 Hannes Janetzek
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
	int mHash;
	Tag[] mTags;

	MatchingCacheKey() {
	}

	MatchingCacheKey(MatchingCacheKey key) {
		// need to clone tags as they belong to TileDataSource
		mTags = key.mTags.clone();
		mHash = key.mHash;
	}

	// set temporary values for comparison
	boolean set(Tag[] tags, MatchingCacheKey compare) {
		int length = tags.length;

		if (compare != null && length == compare.mTags.length) {
			int i = 0;
			for (; i < length; i++) {
				Tag t1 = tags[i];
				Tag t2 = compare.mTags[i];

				if (!(t1 == t2 || (t1.key == t2.key && t1.value == t2.value)))
					break;
			}
			if (i == length)
				return true;
		}

		int result = 7;
		for (int i = 0; i < length; i++)
			result = 31 * result + tags[i].hashCode();

		mHash = 31 * result;
		mTags = tags;

		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		MatchingCacheKey other = (MatchingCacheKey) obj;

		int length = mTags.length;
		if (length != other.mTags.length)
			return false;

		for (int i = 0; i < length; i++) {
			Tag t1 = mTags[i];
			Tag t2 = other.mTags[i];

			if (!(t1 == t2 || (t1.key == t2.key && t1.value == t2.value)))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return mHash;
	}
}
