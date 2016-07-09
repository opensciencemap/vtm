/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class SyncPool<T extends Inlist<?>> {
    protected final int mMaxFill;
    protected final boolean mClearItems;

    protected int mFill;
    protected T mPool;

    public SyncPool(int maxItemsInPool) {
        this(maxItemsInPool, true);
    }

    public SyncPool(int maxItemsInPool, boolean clearItems) {
        mMaxFill = maxItemsInPool;
        mFill = 0;
        mClearItems = clearItems;
    }

    public int getFill() {
        return mFill;
    }

    /**
     * To be implemented by subclass.
     *
     * @param items number of initial items
     */
    public void init(int items) {
        mFill = 0;
        mPool = null;
    }

    public synchronized void clear() {
        while (mPool != null) {
            freeItem(mPool);
            mPool = (T) mPool.next;
        }
    }

    /**
     * @param item set initial state
     * @return 'true' when item should be added back to pool,
     * 'false' when freeItem should be called.
     */
    protected boolean clearItem(T item) {
        return true;
    }

    /**
     * @param item release resources
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
     * <p/>
     * Usage item = pool.release(item), to ensure to not keep a reference to
     * item!
     */
    @CheckReturnValue
    public T release(T item) {
        if (item == null)
            return null;

        if (mClearItems && !clearItem(item)) {
            // dont add back to pool
            freeItem(item);
            return null;
        }
        if (mFill < mMaxFill) {
            synchronized (this) {
                mFill++;

                ((Inlist) item).next = (T) mPool;
                mPool = item;
            }
        } else if (mClearItems) {
            freeItem(item);
        }
        return null;
    }

    /**
     * Release 'list' to pool.
     * <p/>
     * Usage list = pool.releaseAll(list), to ensure to not keep a reference to
     * list!
     */
    @CheckReturnValue
    public T releaseAll(T item) {
        if (item == null)
            return null;

        if (mFill > mMaxFill) {
            while (item != null) {
                if (mClearItems) {
                    clearItem(item);
                    freeItem(item);
                }
                item = (T) item.next;
            }
            return null;
        }

        synchronized (this) {
            while (item != null) {
                T next = (T) item.next;

                if (mClearItems && !clearItem(item)) {
                    // dont add back to pool
                    freeItem(item);
                    item = next;
                    continue;
                }

                mFill++;

                ((Inlist) item).next = (T) mPool;
                mPool = item;

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
            if (mPool == null) {
                return createItem();
            }

            mFill--;

            T ret = mPool;
            mPool = (T) mPool.next;

            ret.next = null;
            return ret;
        }
    }
}
