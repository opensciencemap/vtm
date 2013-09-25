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

import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

public class VertexItem extends Inlist<VertexItem> {

	private static final int MAX_POOL = 500;

	public final static SyncPool<VertexItem> pool = new SyncPool<VertexItem>(MAX_POOL) {

		@Override
		protected VertexItem createItem() {
			return new VertexItem();
		}

		@Override
		protected boolean clearItem(VertexItem it) {
			it.used = 0;
			return true;
		}
	};

	/**
	 * Add VertexItems back to pool. Make sure to not use the reference
	 * afterwards!
	 * i.e.:
	 * vertexItem.release();
	 * vertexItem = null;
	 * */
	public void release() {
		VertexItem.pool.releaseAll(this);
	}

	public int getSize() {
		int size = used;
		for (VertexItem it = next; it != null; it = it.next)
			size += it.used;

		return size;
	}

	public final short[] vertices = new short[SIZE];

	public int used;

	// must be multiple of
	// 4 (LineLayer/PolygonLayer),
	// 24 (TexLineLayer - one block, i.e. two segments)
	// 24 (TextureLayer)
	public static final int SIZE = 360;
}
