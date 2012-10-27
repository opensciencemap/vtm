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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class SymbolItem {
	private static Object lock = new Object();
	private static SymbolItem pool;

	public static SymbolItem get() {
		synchronized (lock) {
			if (pool == null)
				return new SymbolItem();

			SymbolItem ti = pool;
			pool = pool.next;

			ti.next = null;

			return ti;
		}
	}

	public static void release(SymbolItem ti) {
		if (ti == null)
			return;

		synchronized (lock) {
			while (ti != null) {
				SymbolItem next = ti.next;

				ti.drawable = null;
				ti.bitmap = null;

				ti.next = pool;
				pool = ti;

				ti = next;
			}
		}
	}

	SymbolItem next;

	public Bitmap bitmap;
	public Drawable drawable;
	public float x;
	public float y;
	public boolean billboard;
	public int state;

	// center, top, bottom, left, right, top-left...
	//	byte placement;

}
