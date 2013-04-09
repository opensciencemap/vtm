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

public class Pool<T extends Inlist<T>> {

	T pool;

	public void release(T item) {
		if (item == null)
			return;
		item.next = pool;
		pool = item;
	}

	// remove 'item' from 'list' and add back to pool
	public void release(T list, T item) {
		if (item == null)
			return;

		if (item == list) {
			item.next = pool;
			pool = item;
			return;
		}

		for (T prev = list, it = list.next; it != null; it = it.next) {

			if (it == item) {
				prev.next = it.next;

				item.next = pool;
				pool = item;
				return;
			}
			prev = it;
		}
	}

	public T get() {
		if (pool == null)
			return null;

		T ret = pool;
		pool = pool.next;

		ret.next = null;
		return ret;
	}
}
