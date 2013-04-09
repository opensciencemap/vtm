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

import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class SymbolItem extends Inlist<SymbolItem> {

	public final static SyncPool<SymbolItem> pool = new SyncPool<SymbolItem>() {

		@Override
		protected SymbolItem createItem() {
			return new SymbolItem();
		}

		@Override
		protected void clearItem(SymbolItem it) {
			// drop references
			it.drawable = null;
			it.bitmap = null;
		}
	};

	public Bitmap bitmap;
	public Drawable drawable;
	public float x;
	public float y;
	public boolean billboard;
	public int state;

	// center, top, bottom, left, right, top-left...
	//	byte placement;
}
