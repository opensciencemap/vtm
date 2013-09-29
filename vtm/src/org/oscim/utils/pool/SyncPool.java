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

import javax.annotation.CheckReturnValue;

public abstract class SyncPool<T extends Inlist<T>> {
	protected final int maxFill;
	protected int fill;

	protected T pool;

	public SyncPool() {
		maxFill = 100;
		fill = 0;
	}

	public SyncPool(int maxItemsInPool) {
		maxFill = maxItemsInPool;
		fill = 0;
	}

	public int getFill() {
		return fill;
	}

	/**
	 * @param items
	 *            number of initial items. NOTE: default does nothing!
	 */
	public void init(int items) {
	}

	/**
	 * @param item
	 *            set initial state
	 * @return true when item should be added back to pool
	 *         when returning false freeItem will not be called.
	 */
	protected boolean clearItem(T item) {
		return true;
	}

	/**
	 * @param item
	 *            release resources
	 */
	protected void freeItem(T item) {

	}

	/**
	 * Creates the item. To be implemented by subclass.
	 * 
	 * @return the item
	 */
	protected abstract T createItem();

	/**
	 * Release 'item' to pool.
	 * <p>
	 * Usage item = pool.release(item), to ensure to not keep a reference to
	 * item!
	 */
	@CheckReturnValue
	public T release(T item) {
		if (item == null)
			return null;

		if (!clearItem(item)) {
			// dont add back to pool
			freeItem(item);
			return null;
		}
		if (fill < maxFill) {
			synchronized (this) {
				fill++;

				item.next = pool;
				pool = item;
			}
		} else {
			freeItem(item);
		}
		return null;
	}

	/**
	 * Release 'list' to pool.
	 * <p>
	 * Usage list = pool.releaseAll(list), to ensure to not keep a reference to
	 * list!
	 */
	@CheckReturnValue
	public T releaseAll(T item) {
		if (item == null)
			return null;

		if (fill > maxFill) {
			while (item != null) {
				clearItem(item);
				freeItem(item);
				item = item.next;
			}
			return null;
		}

		synchronized (this) {
			while (item != null) {
				T next = item.next;

				if (!clearItem(item)) {
					// dont add back to pool
					freeItem(item);
					item = next;
					continue;
				}

				fill++;

				item.next = pool;
				pool = item;

				item = next;
			}
		}
		return null;
	}

	/**
	 * Gets an 'item' from pool, if pool is empty a new
	 * item will be created by createItem().
	 * 
	 * @return the item
	 */
	public T get() {

		synchronized (this) {
			if (pool == null) {
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
