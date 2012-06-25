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

import android.util.SparseArray;

class PolygonLayers {
	private static final int NUM_VERTEX_FLOATS = 2;
	private static final float[] mFillCoords = { -2, Tile.TILE_SIZE + 1,
			Tile.TILE_SIZE + 1, Tile.TILE_SIZE + 1, -2,
			-2, Tile.TILE_SIZE + 1, -2 };

	private static short[] mByteFillCoords = null;

	private SparseArray<PolygonLayer> layers;

	PolygonLayer[] array = null;
	int size;

	PolygonLayers() {
		layers = new SparseArray<PolygonLayer>(10);
		size = 4;
	}

	PolygonLayer getLayer(int layer, int color, int fade) {
		PolygonLayer l = layers.get(layer);
		if (l != null) {
			// if (color == l.color)
			return l;

			// return getLayer(layer + 1, color, fade);
		}

		l = new PolygonLayer(layer, color, fade);
		layers.put(layer, l);
		return l;
	}

	FloatBuffer compileLayerData(FloatBuffer buf) {
		FloatBuffer fbuf = buf;

		array = new PolygonLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			PolygonLayer l = layers.valueAt(i);
			array[i] = l;
			size += l.verticesCnt;
		}

		size *= NUM_VERTEX_FLOATS;

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 4).order(
					ByteOrder.nativeOrder());
			// Log.d("GLMap", "allocate buffer " + size);
			fbuf = bbuf.asFloatBuffer();
		} else {
			fbuf.position(0);
		}

		fbuf.put(mFillCoords, 0, 8);
		int pos = 4;

		for (int i = 0, n = array.length; i < n; i++) {
			PolygonLayer l = array[i];

			for (PoolItem item : l.pool) {
				fbuf.put(item.vertices, 0, item.used);
			}

			l.offset = pos;
			pos += l.verticesCnt;

			LayerPool.add(l.pool);
			l.pool = null;
		}

		fbuf.position(0);

		// not needed for drawing
		layers = null;

		return fbuf;
	}

	ShortBuffer compileLayerData(ShortBuffer buf) {
		ShortBuffer sbuf = buf;

		array = new PolygonLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			PolygonLayer l = layers.valueAt(i);
			array[i] = l;
			size += l.verticesCnt;
		}

		size *= NUM_VERTEX_FLOATS;

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 2).order(
					ByteOrder.nativeOrder());
			sbuf = bbuf.asShortBuffer();
		} else {
			sbuf.position(0);
		}

		short[] data = new short[PoolItem.SIZE];

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

		for (int i = 0, n = array.length; i < n; i++) {
			PolygonLayer l = array[i];

			for (int k = 0, m = l.pool.size(); k < m; k++) {
				PoolItem item = l.pool.get(k);
				PoolItem.toHalfFloat(item, data);
				sbuf.put(data, 0, item.used);
			}

			l.offset = pos;
			pos += l.verticesCnt;

			LayerPool.add(l.pool);
			l.pool = null;
		}

		sbuf.position(0);

		// not needed for drawing
		layers = null;

		return sbuf;
	}
}
