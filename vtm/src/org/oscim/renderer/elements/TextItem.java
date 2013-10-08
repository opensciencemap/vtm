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
package org.oscim.renderer.elements;

import org.oscim.theme.renderinstruction.Text;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

public class TextItem extends Inlist<TextItem> {
	//static final Logger log = LoggerFactory.getLogger(TextItem.class);
	private final static int MAX_POOL = 250;

	public final static SyncPool<TextItem> pool = new SyncPool<TextItem>(MAX_POOL) {

		@Override
		protected TextItem createItem() {
			return new TextItem();
		}

		@Override
		protected boolean clearItem(TextItem ti) {
			// drop references
			ti.string = null;
			ti.text = null;
			ti.n1 = null;
			ti.n2 = null;
			return true;
		}
	};

	public static TextItem copy(TextItem orig) {

		TextItem ti = pool.get();

		ti.x = orig.x;
		ti.y = orig.y;

		ti.x1 = orig.x1;
		ti.y1 = orig.y1;
		ti.x2 = orig.x2;
		ti.y2 = orig.y2;

		return ti;
	}

	public static boolean shareText(TextItem ti1, TextItem ti2) {
		if (ti1.text != ti2.text)
			return false;

		if (ti1.string == ti2.string)
			return true;

		if (ti1.string.equals(ti2.string)) {
			// make strings unique, should be done only once..
			ti1.string = ti2.string;
			return true;
		}

		return false;
	}

	public TextItem set(float x, float y, String string, Text text) {
		this.x = x;
		this.y = y;
		this.string = string;
		this.text = text;
		this.x1 = 0;
		this.y1 = 0;
		this.x2 = 1;
		this.y2 = 0;
		this.width = text.paint.measureText(string);
		return this;
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

	// link to next node
	//public TextItem next;

	// center
	public float x, y;

	// label text
	public String string;

	// text style
	public Text text;

	// label width
	public float width;

	// left and right corner of segment
	public float x1, y1, x2, y2;

	// segment length
	public short length;

	// link to next/prev label of the way
	public TextItem n1;
	public TextItem n2;

	public byte edges;

	@Override
	public String toString() {
		return x + " " + y + " " + string;
	}
}
