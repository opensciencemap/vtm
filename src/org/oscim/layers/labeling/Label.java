/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers.labeling;

import org.oscim.layers.tile.MapTile;
import org.oscim.renderer.sublayers.TextItem;
import org.oscim.utils.OBB2D;

class Label extends TextItem {
	TextItem item;

	//Link blocking;
	//Link blockedBy;
	// shared list of all label for a tile
	//Link siblings;

	MapTile tile;

	//public byte origin;
	public int active;
	public OBB2D bbox;

	public TextItem move(TextItem ti, float dx, float dy, float scale) {
		this.x = (dx + ti.x) * scale;
		this.y = (dy + ti.y) * scale;
		return this;
	}

	public void clone(TextItem ti) {
		this.string = ti.string;
		this.text = ti.text;
		this.width = ti.width;
		this.length = ti.length;
	}

	public void setAxisAlignedBBox() {
		this.x1 = x - width / 2;
		this.y1 = y - text.fontHeight / 2;
		this.x2 = x + width / 2;
		this.y2 = y + text.fontHeight / 2;
	}

	static int comparePriority(Label l1, Label l2){


		return 0;
	}
}