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

import org.oscim.theme.renderinstruction.Text;
import org.oscim.utils.OBB2D;

import android.util.Log;

public class TextItem {
	private final static String TAG = TextItem.class.getName();
	private final static int MAX_POOL = 250;

	private static Object lock = new Object();
	private static TextItem pool;
	private static int count = 0;
	private static int inPool = 0;

	public static TextItem get() {
		synchronized (lock) {
			if (pool == null) {
				count++;
				return new TextItem();
			}

			inPool--;
			TextItem ti = pool;
			pool = pool.next;

			ti.next = null;
			ti.active = 0;
			return ti;
		}
	}

	//	public static void append(TextItem ti, TextItem in) {
	//		if (ti == null)
	//			return;
	//		if (ti.next == null) {
	//			in.next = ti.next;
	//			ti.next = in;
	//		}
	//
	//		TextItem t = ti;
	//		while (t.next != null)
	//			t = t.next;
	//
	//		in.next = t.next;
	//		t.next = in;
	//	}

	public static void release(TextItem ti) {
		if (ti == null)
			return;

		if (inPool > MAX_POOL) {
			while (ti != null) {
				TextItem next = ti.next;

				// drop references
				ti.string = null;
				ti.text = null;
				ti.n1 = null;
				ti.n2 = null;
				ti = next;
				count--;
			}
		}

		synchronized (lock) {
			while (ti != null) {
				TextItem next = ti.next;
				ti.next = pool;

				// drop references
				ti.string = null;
				ti.text = null;
				ti.n1 = null;
				ti.n2 = null;

				pool = ti;

				ti = next;

				inPool++;
			}
		}
	}

	public static void printPool() {
		Log.d(TAG, "in pool " + inPool + " / " + count);
	}

	public TextItem set(float x, float y, String string, Text text) {
		this.x = x;
		this.y = y;
		this.string = string;
		this.text = text;
		this.width = text.paint.measureText(string);
		return this;
	}

	public TextItem move(TextItem ti, float dx, float dy) {
		this.x = dx + ti.x;
		this.y = dy + ti.y;
		this.string = ti.string;
		this.text = ti.text;
		this.width = ti.width;
		this.length = ti.length;
		return this;

	}

	/* copy properties from 'ti' and add offset
	 *
	 * */
	public TextItem move(TextItem ti, float dx, float dy, float scale) {
		this.x = dx + (ti.x * scale);
		this.y = dy + (ti.y * scale);
		this.string = ti.string;
		this.text = ti.text;
		this.width = ti.width;
		this.length = ti.length;
		return this;
	}

	public void setAxisAlignedBBox(){
		this.x1 = x - width / 2;
		this.y1 = y - text.fontHeight / 2;
		this.x2 = x + width / 2;
		this.y2 = y + text.fontHeight / 2;
	}

	public static boolean bboxOverlaps(TextItem it1, TextItem it2, float add) {
		if (it1.y1 < it1.y2) {
			if (it2.y1 < it2.y2)
				return (it1.x1 - add < it2.x2)
						&& (it2.x1 < it1.x2 + add)
						&& (it1.y1 - add < it2.y2)
						&& (it2.y1 < it1.y2 + add);

			// flip it2
			return (it1.x1 - add < it2.x2)
					&& (it2.x1 < it1.x2 + add)
					&& (it1.y1 - add < it2.y1)
					&& (it2.y2 < it1.y2 + add);
		}

		// flip it1
		if (it2.y1 < it2.y2)
			return (it1.x1 - add < it2.x2)
					&& (it2.x1 < it1.x2 + add)
					&& (it1.y2 - add < it2.y2)
					&& (it2.y1 < it1.y1 + add);

		// flip both
		return (it1.x1 - add < it2.x2)
				&& (it2.x1 < it1.x2 + add)
				&& (it1.y2 - add < it2.y1)
				&& (it2.y2 < it1.y1 + add);
	}

	public TextItem next;

	public float x, y;
	public String string;
	public Text text;
	public float width;
	public float x1, y1, x2, y2;
	public short length;

	// link to next/prev label of the way
	public TextItem n1;
	public TextItem n2;

	public byte origin;

	public int active;
	public OBB2D bbox;
}
