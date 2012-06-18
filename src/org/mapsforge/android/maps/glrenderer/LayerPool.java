/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.glrenderer;

import java.util.LinkedList;

class LayerPool {
	static private LinkedList<PoolItem> pool;
	static private int count;

	static void init() {
		pool = new LinkedList<PoolItem>();
		count = 0;
	}

	static PoolItem get() {
		if (count == 0)
			return new PoolItem();

		PoolItem it;
		synchronized (pool) {
			count--;
			it = pool.pop();
			it.used = 0;
		}

		return it;
	}

	static void add(LinkedList<PoolItem> items) {
		int size = items.size();
		synchronized (pool) {
			while (count < 4096 && size-- > 0) {
				count++;
				pool.add(items.pop());
			}
		}
	}
}
