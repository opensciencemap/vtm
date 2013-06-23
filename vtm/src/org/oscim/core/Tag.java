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
package org.oscim.core;

/**
 * A tag represents an immutable key-value pair.
 */

// TODO: use own stringshare method instead of internalized strings

public class Tag {
	private static final char KEY_VALUE_SEPARATOR = '=';
	/**
	 * The key of the house number OpenStreetMap tag.
	 */
	public static final String TAG_KEY_HOUSE_NUMBER = "addr:housenumber";

	/**
	 * The key of the name OpenStreetMap tag.
	 */
	public static final String TAG_KEY_NAME = "name";

	/**
	 * The key of the reference OpenStreetMap tag.
	 */
	public static final String TAG_KEY_REF = "ref";

	/**
	 * The key of the elevation OpenStreetMap tag.
	 */
	public static final String TAG_KEY_ELE = "ele";

	public static final String TAG_KEY_AMENITY = "amenity";

	/**
	 * The key of the elevation OpenStreetMap tag.
	 */
	public static final String TAG_KEY_BUILDING = "building";
	public static final String TAG_KEY_HIGHWAY = "highway";
	public static final String TAG_KEY_LANDUSE = "landuse";
	public static final String VALUE_YES = "yes";
	public static final String VALUE_NO = "no";

	public static final String KEY_HEIGHT = "height";
	public static final String KEY_MIN_HEIGHT = "min_height";

	/**
	 * The key of this tag.
	 */
	public final String key;

	/**
	 * The value of this tag.
	 */
	public String value;

	private int hashCodeValue = 0;
	private final boolean intern;

	/**
	 * @param tag
	 *            the textual representation of the tag.
	 */

	public Tag(String tag) {
		int splitPosition = tag.indexOf(KEY_VALUE_SEPARATOR);
		if (splitPosition < 0) {
			System.out.println("TAG:" + tag);
		}
		this.key = tag.substring(0, splitPosition).intern();
		this.value = tag.substring(splitPosition + 1).intern();
		this.intern = true;
	}

	/**
	 * @param key
	 *            the key of the tag.
	 * @param value
	 *            the value of the tag.
	 */
	public Tag(String key, String value) {
		this.key = (key == null ? null : key.intern());
		this.value = (value == null ? null : value.intern());
		this.intern = true;
	}

	/**
	 * @param key
	 *            the key of the tag.
	 * @param value
	 *            the value of the tag.
	 * @param intern
	 *            true when value string should be intern()alized.
	 */
	public Tag(String key, String value, boolean intern) {
		this.key = key.intern();

		if (intern)
			this.value = (value == null ? null : value.intern());
		else
			this.value = value;

		this.intern = intern;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tag)) {
			return false;
		}
		Tag other = (Tag) obj;

		if (this.key != other.key)
			return false;

		if (this.intern && other.intern) {
			if (this.value == other.value)
				return true;
		} else {
			if (this.value.equals(other.value))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (this.hashCodeValue == 0)
			this.hashCodeValue = calculateHashCode();

		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Tag [key=");
		stringBuilder.append(this.key);
		stringBuilder.append(", value=");
		stringBuilder.append(this.value);
		stringBuilder.append("]");
		return stringBuilder.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + ((this.key == null) ? 0 : this.key.hashCode());
		result = 31 * result + ((this.value == null) ? 0 : this.value.hashCode());
		return result;
	}
}
