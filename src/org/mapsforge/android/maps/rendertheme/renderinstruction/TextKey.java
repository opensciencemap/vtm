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
package org.mapsforge.android.maps.rendertheme.renderinstruction;

import org.mapsforge.core.Tag;

final class TextKey {
	static String getInstance(String key) {
		if (Tag.TAG_KEY_ELE.equals(key)) {
			return Tag.TAG_KEY_ELE;
		} else if (Tag.TAG_KEY_HOUSE_NUMBER.equals(key)) {
			return Tag.TAG_KEY_HOUSE_NUMBER;
		} else if (Tag.TAG_KEY_NAME.equals(key)) {
			return Tag.TAG_KEY_NAME;
		} else if (Tag.TAG_KEY_REF.equals(key)) {
			return Tag.TAG_KEY_REF;
		} else {
			throw new IllegalArgumentException("invalid key: " + key);
		}
	}
}
