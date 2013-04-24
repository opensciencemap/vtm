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

	public SyncPool() {
		maxFill = 100;
		fill = 0;
		count = 0;
	}

	public SyncPool(int maxItemsInPool) {
		maxFill = maxItemsInPool;
		fill = 0;
		count = 0;
	}

	public int getCount() {
		return count;
	}

	public int getFill() {
		return fill;
	}

	/**
	 * @param items number of initial items
	 */
	public void init(int items) {
		count = items;
		fill = items;
	}

	/**
	 * @param item set initial state
	 */
	protected void clearItem(T item) {

	}

	/**
	 * @param item release resources
	 */
	protected void freeItem(T item) {

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
		} else {
			freeItem(item);
			count--;
		}
	}

	public void releaseAll(T item) {
		if (item == null)
			return;

		if (fill > maxFill) {
			while (item != null) {
				clearItem(item);
				freeItem(item);
				count--;

				item = item.next;
			}
			return;
		}

		synchronized (this) {
			while (item != null) {
				T next = item.next;

				clearItem(item);
				fill++;

				item.next = pool;
				pool = item;

				item = next;
			}
		}
	}

	public T get() {

		synchronized (this) {
			if (pool == null) {
				count++;
				return createItem();
			}

			fill--;

			T ret = pool;
			pool = pool.next;

			ret.next = null;
			return ret;
		}
	}
}
