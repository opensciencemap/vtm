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

public class Inlist<T extends Inlist<T>> {

	private final static boolean debug = false;

	public T next;

	static <T extends Inlist<T>> int size(Inlist<T> list) {
		int count = 0;
		for (Inlist<T> l = list; l != null; l = l.next)
			count++;
		return count;
	}

	public static <T extends Inlist<T>> Inlist<T> remove(Inlist<T> list, Inlist<T> item) {
		if (item == list) {
			return item.next;
		}
		for (Inlist<T> prev = list, it = list.next; it != null; it = it.next) {

			if (it == item) {
				prev.next = it.next;
				return list;
			}
			prev = it;
		}

		return list;
	}


	static <T extends Inlist<T>> Inlist<T> prepend(T list, T item) {
		item.next = list;
		return item;
	}

	static <T extends Inlist<T>> Inlist<T> append(T list, T item) {

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
}
