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
 * Also handy for objects that exist in only *one list* at a time, if you
 * are *REALLY* sure about it. Better do not use it! :)
 */
public class Inlist<T extends Inlist<T>> {

	public T next;

	/**
	 * Push 'item' onto 'list'.
	 * 
	 * @param list the list
	 * @param item the item
	 * @return the new head of 'list' (item)
	 */
	public static <T extends Inlist<T>> T push(T list, T item) {
		item.next = list;
		return item;
	}

	/**
	 * Get size of 'list'.
	 * 
	 * @param list the list
	 * @return the number of items in 'list'
	 */
	static <T extends Inlist<T>> int size(T list) {
		int count = 0;
		for (Inlist<T> l = list; l != null; l = l.next)
			count++;
		return count;
	}

	/**
	 * Removes the 'item' from 'list'.
	 * 
	 * @param list the list
	 * @param item the item
	 * @return the new head of 'list'
	 */
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

	/**
	 * Gets the 'item' with index 'i'.
	 * 
	 * @param list the list
	 * @param i the index
	 * @return the item or null
	 */
	public static <T extends Inlist<T>> T get(T list, int i) {
		if (i < 0)
			return null;

		while (--i > 0 && list != null)
			list = list.next;

		if (i == 0)
			return list;

		return null;
	}

	/**
	 * Append 'item' to 'list'. 'item' may not be in another list,
	 * i.e. item.next must be null
	 * 
	 * @param list the list
	 * @param item the item
	 * @return the new head of 'list'
	 */
	public static <T extends Inlist<T>> T appendItem(T list, T item) {

		if (item.next != null)
			throw new IllegalArgumentException("'item' is list");

		if (list == null)
			return item;

		Inlist<T> it = list;

		while (it.next != null)
			it = it.next;

		it.next = item;

		return list;
	}

	/**
	 * Append list 'other' to 'list'.
	 * 
	 * @param list the list
	 * @param other the other
	 * @return the head of 'list'
	 */
	public static <T extends Inlist<T>> T appendList(T list, T other) {

		if (list == null)
			return other;

		if (list == other)
			return list;

		Inlist<T> it = list;

		while (it.next != null) {
			if (it.next == other)
				throw new IllegalArgumentException("'other' alreay in 'list'");

			it = it.next;
		}
		it.next = other;

		return list;
	}

	/**
	 * Get last item in from list.
	 * 
	 * @param list the list
	 * @return the last item
	 */
	public static <T extends Inlist<T>> T last(T list) {
		while (list != null) {
			if (list.next == null)
				return list;
			list = list.next;
		}
		return null;
	}

	/**
	 * Prepend 'item' relative to 'other'.
	 * 
	 * @param list the list
	 * @param item the item
	 * @param other the other list
	 * @return the new head of list
	 */
	public static <T extends Inlist<T>> T prependRelative(T list, T item, T other) {

		if (item.next != null)
			throw new IllegalArgumentException("'item' is list");

		if (list == null)
			throw new IllegalArgumentException("'list' is null");

		if (list == other) {
			item.next = list;
			return item;
		}

		Inlist<T> it = list;

		while (it != null && it.next != other)
			it = it.next;

		if (it == null)
			throw new IllegalArgumentException("'other' not in 'list'");

		item.next = it.next;
		it.next = item;

		return list;
	}
}
