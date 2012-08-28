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
package org.mapsforge.android.glrenderer;

import android.util.Log;

public class ShortPool {
	private static final int POOL_LIMIT = 6000;

	static private ShortItem pool = null;
	static private int count = 0;
	static private int countAll = 0;

	static synchronized void finish() {
		count = 0;
		countAll = 0;
		pool = null;
	}

	static synchronized ShortItem get() {

		if (pool == null) {
			countAll++;
			return new ShortItem();
		}

		count--;

		if (count < 0) {
			int c = 0;

			for (ShortItem tmp = pool; tmp != null; tmp = tmp.next)
				c++;

			Log.d("ShortPool", "eek wrong count: " + count + " left" + c);
			return new ShortItem();
		}

		ShortItem it = pool;
		pool = pool.next;
		it.used = 0;
		it.next = null;
		return it;
	}

	static synchronized void add(ShortItem items) {
		if (items == null)
			return;

		// limit pool items
		if (countAll < POOL_LIMIT) {

			ShortItem last = items;

			while (true) {
				count++;

				if (last.next == null)
					break;

				last = last.next;
			}

			last.next = pool;
			pool = items;

		} else {
			// int cleared = 0;
			ShortItem prev, tmp = items;
			while (tmp != null) {
				prev = tmp;
				tmp = tmp.next;

				countAll--;

				prev.next = null;

			}
		}
	}
}
