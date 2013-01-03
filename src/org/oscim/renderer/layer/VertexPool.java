/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.renderer.layer;

import android.util.Log;

public class VertexPool {
	private static final int POOL_LIMIT = 6000;

	static private VertexPoolItem pool = null;
	static private int count = 0;
	static private int countAll = 0;

	public static synchronized void init() {
		count = 0;
		countAll = 0;
		pool = null;
	}

	//	public static VertexPoolItem get(VertexPoolItem prev) {
	//		VertexPoolItem it = get();
	//		if (prev != null) {
	//			prev.next = it;
	//			prev.used = VertexPoolItem.SIZE;
	//		}
	//		return it;
	//	}

	public static synchronized VertexPoolItem get() {

		if (pool == null && count > 0) {
			Log.d("VertexPool", "XXX wrong count: " + count);
		}
		if (pool == null) {
			countAll++;
			return new VertexPoolItem();
		}

		count--;

		if (count < 0) {
			int c = 0;

			for (VertexPoolItem tmp = pool; tmp != null; tmp = tmp.next)
				c++;

			Log.d("VertexPool", "XXX wrong count: " + count + " left" + c);
			return new VertexPoolItem();
		}

		VertexPoolItem it = pool;
		pool = pool.next;
		it.used = 0;
		it.next = null;
		return it;
	}

	// private static float load = 1.0f;
	// private static int loadCount = 0;

	public static synchronized void release(VertexPoolItem items) {
		if (items == null)
			return;

		// int pall = countAll;
		// int pcnt = count;

		// limit pool items
		if (countAll < POOL_LIMIT) {

			VertexPoolItem last = items;

			while (true) {
				count++;
				// load += (float) last.used / VertexPoolItem.SIZE;
				// loadCount++;

				if (last.next == null)
					break;

				last = last.next;
			}

			last.next = pool;
			pool = items;
			// Log.d("Pool", "added: " + (count - pcnt) + " " + count + " " +
			// countAll
			// + " load: " + (load / loadCount));

		} else {
			// int cleared = 0;
			VertexPoolItem prev, tmp = items;
			while (tmp != null) {
				prev = tmp;
				tmp = tmp.next;

				countAll--;

				// load += (float) prev.used / VertexPoolItem.SIZE;
				// loadCount++;

				prev.next = null;

			}
			// Log.d("Pool", "dropped: " + (pall - countAll) + " " + count + " "
			// + countAll + " load: " + (load / loadCount));
		}
	}
}
