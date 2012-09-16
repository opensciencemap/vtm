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
public class Tag {
	private static final char KEY_VALUE_SEPARATOR = '=';
	/**
	 * The key of the house number OpenStreetMap tag.
	 */
	public static final String TAG_KEY_HOUSE_NUMBER = "addr:housenumber".intern();

	/**
	 * The key of the name OpenStreetMap tag.
	 */
	public static final String TAG_KEY_NAME = "name".intern();

	/**
	 * The key of the reference OpenStreetMap tag.
	 */
	public static final String TAG_KEY_REF = "ref".intern();

	/**
	 * The key of the elevation OpenStreetMap tag.
	 */
	public static final String TAG_KEY_ELE = "ele".intern();

	/**
	 * The key of this tag.
	 */
	public final String key;

	/**
	 * The value of this tag.
	 */
	public String value;

	private transient int hashCodeValue = 0;

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
	}

	public Tag(String tag, boolean hashValue) {
		int splitPosition = tag.indexOf(KEY_VALUE_SEPARATOR);
		if (splitPosition < 0) {
			System.out.println("TAG:" + tag);
		}
		this.key = tag.substring(0, splitPosition).intern();
		if (!hashValue)
			this.value = tag.substring(splitPosition + 1);
		else
			this.value = tag.substring(splitPosition + 1).intern();
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
	}

	/**
	 * @param key
	 *            the key of the tag.
	 * @param value
	 *            the value of the tag.
	 * @param intern
	 *            true when string should be intern()alized.
	 */
	public Tag(String key, String value, boolean intern) {
		if (intern) {
			this.key = (key == null ? null : key.intern());
			this.value = (value == null ? null : value.intern());
		}
		else {
			this.key = key;
			this.value = value;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tag)) {
			return false;
		}
		Tag other = (Tag) obj;

		if ((this.key == other.key) && (this.value == other.value))
			return true;

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
