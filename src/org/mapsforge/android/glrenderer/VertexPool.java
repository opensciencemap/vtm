/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapsforge.android.glrenderer;

import android.annotation.SuppressLint;

class VertexPool {
	private static final int POOL_LIMIT = 8192;

	@SuppressLint("UseValueOf")
	private static final Boolean lock = new Boolean(true);

	static private PoolItem pool = null;
	static private int count = 0;

	static void init() {
	}

	static PoolItem get() {
		synchronized (lock) {

			if (count == 0)
				return new PoolItem();

			count--;

			PoolItem it = pool;
			pool = pool.next;
			it.used = 0;
			it.next = null;
			return it;
		}
	}

	static void add(PoolItem items) {
		if (items == null)
			return;

		synchronized (lock) {
			PoolItem last = items;

			// limit pool items
			while (count < POOL_LIMIT) {
				if (last.next == null) {
					break;
				}
				last = last.next;
				count++;
			}

			// clear references
			PoolItem tmp2, tmp = last.next;
			while (tmp != null) {
				tmp2 = tmp;
				tmp = tmp.next;
				tmp2.next = null;
			}

			last.next = pool;
			pool = items;
		}
	}
}
