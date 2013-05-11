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
package org.oscim.core;

public class TagSet {
	public static TagSet EMPTY_TAG_SET = new TagSet();

	public Tag[] tags;
	public int numTags;

	private TagSet() {

	}

	public TagSet(int count) {
		tags = new Tag[count];
	}

	public void clear() {
		numTags = 0;
	}

	/** find Tag by key - NOTE: key must be internal() */
	public Tag get(String key) {
		for (int i = 0; i < numTags; i++) {
			if (tags[i].key == key)
				return tags[i];
		}
		return null;
	}
	public boolean containsKey(String key){
		for (int i = 0; i < numTags; i++) {
			if (tags[i].key == key)
				return true;
		}
		return false;
	}

	public String getValue(String key){
		for (int i = 0; i < numTags; i++) {
			if (tags[i].key == key)
				return tags[i].value;
		}
		return null;
	}

	public boolean contains(String key, String value){
		for (int i = 0; i < numTags; i++) {
			if (tags[i].key == key)
				return value.equals(tags[i].value);
		}
		return false;
	}

	public void add(Tag tag) {
		if (numTags >= tags.length) {
			Tag[] tmp = tags;
			tags = new Tag[numTags + 4];
			System.arraycopy(tmp, 0, tags, 0, numTags);
		}
		tags[numTags++] = tag;
	}


	public boolean contains(Tag tag) {
		for (int i = 0; i < numTags; i++) {
			Tag t = tags[i];
			if (t == tag || (t.key == tag.key && (t.value == t.value)))
				return true;
		}
		return false;
	}

	public boolean hasKey(String[] keys) {
		for (int i = 0; i < numTags; i++) {
			Tag t = tags[i];
			for (String key : keys)
				if (key == t.key)
					return true;
		}
		return false;
	}

	public boolean hasKey(String key) {
		for (int i = 0; i < numTags; i++) {
			Tag t = tags[i];
			if (key == t.key)
				return true;
		}
		return false;
	}

	public boolean hasValue(String[] vals) {
		for (int i = 0; i < numTags; i++) {
			Tag t = tags[i];
			for (String value : vals)
				if (value == t.value)
					return true;
		}
		return false;
	}

	public boolean hasValue(String value) {
		for (int i = 0; i < numTags; i++) {
			Tag t = tags[i];
			if (value == t.value)
				return true;
		}
		return false;
	}
}
