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
package org.oscim.utils.pool;

/**
 * Utility class for making poolable objects.
 * Instead of using an additional list to hold pool items just extend this
 * class.
 * 
 * Also handy for objects that exist in only *one list* at a time.
 * */

public class Inlist<T extends Inlist<T>> {

	private final static boolean debug = false;

	public T next;

	static <T extends Inlist<T>> int size(T list) {
		int count = 0;
		for (Inlist<T> l = list; l != null; l = l.next)
			count++;
		return count;
	}

	public static <T extends Inlist<T>> T remove(T list, T item) {
		if (item == list) {
			T head = item.next;
			item.next = null;
			return head;
		}
		for (Inlist<T> prev = list, it = list.next; it != null; it = it.next) {

			if (it == item) {
				prev.next = item.next;
				item.next = null;
				return list;
			}
			prev = it;
		}

		return list;
	}

	public static <T extends Inlist<T>> T get(T list, int i) {
		if (i < 0)
			return null;

		while (--i > 0 && list != null)
			list = list.next;

		if (i == 0)
			return list;

		return null;
	}

	//	public static <T extends Inlist<T>> T insertAt(T list, T item, int i){
	//		if (i < 0)
	//			return null;
	//
	//		while (--i > 0 && list != null)
	//			list = list.next;
	//
	//		if (i == 0)
	//			return list;
	//
	//		return null;
	//	}

	public static <T extends Inlist<T>> T push(T list, T item) {
		item.next = list;
		return item;
	}

	public static <T extends Inlist<T>> T append(T list, T item) {

		if (debug) {
			if (item.next != null) {
				// warn
				item.next = null;
			}
		}

		if (list == null)
			return item;

		Inlist<T> it = list;

		while (it.next != null)
			it = it.next;

		it.next = item;

		return list;
	}

	public static <T extends Inlist<T>> T prependRelative(T list, T item, T other) {

		if (debug) {
			if (item.next != null) {
				// warn
				item.next = null;
			}
		}

		if (list == null)
			throw new IllegalArgumentException("Inlist.prependRelative 'list' is null");

		if (list == other) {
			item.next = list;
			return item;
		}

		Inlist<T> it = list;

		while (it != null && it.next != other)
			it = it.next;

		if (it == null)
			throw new IllegalArgumentException("Inlist.prependRelative 'other' not in 'list'");

		item.next = it.next;
		it.next = item;

		return list;
	}
}
