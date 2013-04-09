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

public abstract class SyncPool<T extends Inlist<T>> {
	protected final int maxFill;
	protected int fill;
	protected int count;

	protected T pool;

	public SyncPool(){
		maxFill = 100;
	}

	public SyncPool(int maxItemsInPool) {
		maxFill = maxItemsInPool;
		fill = 0;
	}

	/**
	 * @param items number of initial items
	 */
	public void init(int items){

	}

	/**
	 * @param item release resources
	 */
	protected void clearItem(T item) {

	}

	protected abstract T createItem();

	public void release(T item) {
		if (item == null)
			return;

		clearItem(item);

		if (fill < maxFill) {
			synchronized (this) {
				fill++;

				item.next = pool;
				pool = item;
			}
		} else{
			count--;
		}
	}

	public void releaseAll(T item) {
		if (item == null)
			return;

		if (fill > maxFill)
		while (item != null) {
			clearItem(item);
			item = item.next;
			count--;
		}

		if (item == null)
			return;

		synchronized (this) {
			while (item != null) {
				T next = item.next;
				clearItem(item);

				item.next = pool;
				pool = item;
				fill++;

				item = next;
			}
		}
	}

	// remove 'item' from 'list' and add back to pool
	public T release(T list, T item) {
		if (item == null)
			return list;

		T ret = list;

		if (item == list) {
			ret = item.next;
		} else {
			for (T prev = list, it = list.next; it != null; it = it.next) {
				if (it == item) {
					prev.next = it.next;
				}
				prev = it;
			}
		}

		clearItem(item);

		if (fill < maxFill) {
			synchronized (this) {
				fill++;

				item.next = pool;
				pool = item;
			}
		} else{
			count--;
		}

		return ret;
	}

	public T get() {
		if (pool == null){
			count++;
			return createItem();
		}

		synchronized (this) {
			fill--;

			T ret = pool;
			pool = pool.next;

			ret.next = null;
			return ret;
		}
	}
}
