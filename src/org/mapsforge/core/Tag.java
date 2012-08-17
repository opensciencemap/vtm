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
package org.mapsforge.core;

/**
 * A tag represents an immutable key-value pair.
 */
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

	/**
	 * The key of this tag.
	 */
	public final String key;

	/**
	 * The value of this tag.
	 */
	public String value;

	private transient int hashCodeValue;

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
		this.hashCodeValue = calculateHashCode();
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

		this.hashCodeValue = calculateHashCode();
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
		this.hashCodeValue = calculateHashCode();
	}

	public Tag(String key, String value, boolean intern) {
		this.key = (key == null ? null : key);
		this.value = (value == null ? null : value);
		this.hashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tag)) {
			return false;
		}
		Tag other = (Tag) obj;

		if ((this.key != other.key) || (this.value != other.value))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
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
