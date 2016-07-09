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
public abstract class Pool<T extends Inlist<?>> {
    protected T mPool;
    protected int mLimit;
    protected int mFill;

    /**
     * @param item release resources
     * @return whether item should be added to
     * pool. use to manage pool size manually
     */
    protected boolean clearItem(T item) {
        return true;
    }

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

        if (!clearItem(item))
            return null;

        ((Inlist) item).next = mPool;
        mPool = item;

        return null;
    }

    /**
     * Release 'list' to pool.
     * <p/>
     * Usage list = pool.releaseAll(list), to ensure to not keep a reference to
     * list!
     */
    @CheckReturnValue
    public T releaseAll(T list) {
        if (list == null)
            return null;

        while (list != null) {

            T next = (T) list.next;

            clearItem(list);

            ((Inlist) list).next = mPool;
            mPool = list;

            list = next;
        }
        return null;
    }

    /**
     * remove 'item' from 'list' and add back to pool
     */
    public T release(T list, T item) {
        if (item == null)
            return list;

        clearItem(item);

        return (T) Inlist.remove((Inlist) list, item);
    }

    /**
     * get an item from pool
     */
    public T get() {
        if (mPool == null)
            return createItem();

        Inlist ret = mPool;
        mPool = (T) mPool.next;

        ret.next = null;
        return (T) ret;
    }

    protected abstract T createItem();
}
