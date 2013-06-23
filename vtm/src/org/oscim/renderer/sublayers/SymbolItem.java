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
package org.oscim.renderer.sublayers;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.PointF;
import org.oscim.renderer.atlas.TextureRegion;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;


public class SymbolItem extends Inlist<SymbolItem> {

	public final static SyncPool<SymbolItem> pool = new SyncPool<SymbolItem>() {

		@Override
		protected SymbolItem createItem() {
			return new SymbolItem();
		}

		@Override
		protected void clearItem(SymbolItem it) {
			// drop references
			it.bitmap = null;
			it.symbol = null;
			it.offset = null;
		}
	};

	public boolean billboard;
	public float x;
	public float y;

	public TextureRegion symbol;
	public Bitmap bitmap;
	public PointF offset;

}
