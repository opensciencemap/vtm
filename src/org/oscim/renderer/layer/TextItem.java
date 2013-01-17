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

public class TextItem {
	private static Object lock = new Object();
	private static TextItem pool;

	public static TextItem get() {
		synchronized (lock) {
			if (pool == null)
				return new TextItem();

			TextItem ti = pool;
			pool = pool.next;

			ti.next = null;

			return ti;
		}
	}

	public static void release(TextItem ti) {
		if (ti == null)
			return;

		synchronized (lock) {
			while (ti != null) {
				TextItem next = ti.next;

				ti.next = pool;
				pool = ti;

				ti = next;
			}
		}
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
		return this;
	}

	public TextItem move(TextItem ti, float dx, float dy, float scale) {
		this.x = dx + (ti.x * scale);
		this.y = dy + (ti.y * scale);
		this.string = ti.string;
		this.text = ti.text;
		this.width = ti.width;
		return this;
	}

	public TextItem next;

	public float x, y;
	public String string;
	public Text text;
	public float width;
	public short x1, y1, x2, y2;
	public short length;
	// public byte placement
}
