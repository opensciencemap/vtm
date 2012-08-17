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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.mapsforge.android.utils.FastMath;
import org.mapsforge.core.Tile;

class PolygonLayers {
	private static final int NUM_VERTEX_FLOATS = 2;
	static final float[] mFillCoords = { -2, Tile.TILE_SIZE + 1,
			Tile.TILE_SIZE + 1, Tile.TILE_SIZE + 1, -2,
			-2, Tile.TILE_SIZE + 1, -2 };

	private static short[] mByteFillCoords = null;

	static FloatBuffer compileLayerData(PolygonLayer layers, FloatBuffer buf) {
		FloatBuffer fbuf = buf;
		int size = 4;

		for (PolygonLayer l = layers; l != null; l = l.next)
			size += l.verticesCnt;

		size *= NUM_VERTEX_FLOATS;

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 4).order(
					ByteOrder.nativeOrder());
			// Log.d("GLMap", "allocate buffer " + size);
			fbuf = bbuf.asFloatBuffer();
		} else {
			fbuf.clear();
		}

		fbuf.put(mFillCoords, 0, 8);
		int pos = 4;

		PoolItem last = null, items = null;

		for (PolygonLayer l = layers; l != null; l = l.next) {

			for (PoolItem item = l.pool; item != null; item = item.next) {
				fbuf.put(item.vertices, 0, item.used);
				last = item;
			}
			l.offset = pos;
			pos += l.verticesCnt;

			if (last != null) {
				last.next = items;
				items = l.pool;
			}

			l.pool = null;
		}

		VertexPool.add(items);

		fbuf.flip();

		return fbuf;
	}

	static final short[] tmpItem = new short[PoolItem.SIZE];

	static ShortBuffer compileLayerData(PolygonLayer layers, ShortBuffer buf) {
		ShortBuffer sbuf = buf;
		int size = 4;

		for (PolygonLayer l = layers; l != null; l = l.next)
			size += l.verticesCnt;

		size *= NUM_VERTEX_FLOATS;

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 2).order(
					ByteOrder.nativeOrder());
			sbuf = bbuf.asShortBuffer();
		} else {
			sbuf.clear();
		}

		short[] data = tmpItem;

		if (mByteFillCoords == null) {
			mByteFillCoords = new short[8];
			FastMath.convertFloatToHalf(mFillCoords[0], mByteFillCoords, 0);
			FastMath.convertFloatToHalf(mFillCoords[1], mByteFillCoords, 1);
			FastMath.convertFloatToHalf(mFillCoords[2], mByteFillCoords, 2);
			FastMath.convertFloatToHalf(mFillCoords[3], mByteFillCoords, 3);
			FastMath.convertFloatToHalf(mFillCoords[4], mByteFillCoords, 4);
			FastMath.convertFloatToHalf(mFillCoords[5], mByteFillCoords, 5);
			FastMath.convertFloatToHalf(mFillCoords[6], mByteFillCoords, 6);
			FastMath.convertFloatToHalf(mFillCoords[7], mByteFillCoords, 7);
		}

		sbuf.put(mByteFillCoords, 0, 8);
		int pos = 4;

		PoolItem last = null, items = null;

		for (PolygonLayer l = layers; l != null; l = l.next) {

			for (PoolItem item = l.pool; item != null; item = item.next) {
				PoolItem.toHalfFloat(item, data);
				sbuf.put(data, 0, item.used);
				last = item;
			}

			l.offset = pos;
			pos += l.verticesCnt;

			if (last != null) {
				last.next = items;
				items = l.pool;
			}

			l.pool = null;
		}

		VertexPool.add(items);

		sbuf.flip();

		return sbuf;
	}

	static void clear(PolygonLayer layers) {
		for (PolygonLayer l = layers; l != null; l = l.next) {
			if (l.pool != null)
				VertexPool.add(l.pool);
		}
	}
}
