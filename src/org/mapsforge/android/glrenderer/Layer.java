/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.glrenderer;

import java.util.LinkedList;

class Layer {
	LinkedList<PoolItem> pool;
	protected PoolItem curItem;

	int verticesCnt;
	int offset;

	final int layer;
	final int color;

	Layer(int l, int c) {
		color = c;
		layer = l;
		verticesCnt = 0;
	}

	float[] getNextItem() {
		curItem.used = PoolItem.SIZE;
		curItem = LayerPool.get();
		pool.add(curItem);
		return curItem.vertices;
	}
}
