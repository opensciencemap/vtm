package org.mapsforge.android.maps.glrenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.mapsforge.android.maps.utils.FastMath;
import org.mapsforge.core.Tile;

import android.util.SparseArray;

class PolygonLayers {
	private static final int NUM_VERTEX_FLOATS = 2;
	private static final float[] mFillCoords = {
			-2, Tile.TILE_SIZE + 1,
			Tile.TILE_SIZE + 1, Tile.TILE_SIZE + 1,
			-2, -2,
			Tile.TILE_SIZE + 1, -2 };

	private static byte[] mByteFillCoords = null;

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
			if (color == l.color)
				return l;

			return getLayer(layer + 1, color, fade);
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
			ByteBuffer bbuf =
					ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder());
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

	ByteBuffer compileLayerData(ByteBuffer buf) {
		ByteBuffer bbuf = buf;

		array = new PolygonLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			PolygonLayer l = layers.valueAt(i);
			array[i] = l;
			size += l.verticesCnt;
		}

		size *= NUM_VERTEX_FLOATS;

		if (buf == null || buf.capacity() < size * 2) {
			bbuf = ByteBuffer.allocateDirect(size * 2).order(ByteOrder.nativeOrder());
		} else {
			bbuf.position(0);
		}

		byte[] data = new byte[PoolItem.SIZE * 2];

		if (mByteFillCoords == null) {
			mByteFillCoords = new byte[16];
			FastMath.convertFloatToHalf(mFillCoords[0], mByteFillCoords, 0);
			FastMath.convertFloatToHalf(mFillCoords[1], mByteFillCoords, 2);
			FastMath.convertFloatToHalf(mFillCoords[2], mByteFillCoords, 4);
			FastMath.convertFloatToHalf(mFillCoords[3], mByteFillCoords, 6);
			FastMath.convertFloatToHalf(mFillCoords[4], mByteFillCoords, 8);
			FastMath.convertFloatToHalf(mFillCoords[5], mByteFillCoords, 10);
			FastMath.convertFloatToHalf(mFillCoords[6], mByteFillCoords, 12);
			FastMath.convertFloatToHalf(mFillCoords[7], mByteFillCoords, 14);
		}

		bbuf.put(mByteFillCoords, 0, 16);
		int pos = 4;

		for (int i = 0, n = array.length; i < n; i++) {
			PolygonLayer l = array[i];

			for (int k = 0, m = l.pool.size(); k < m; k++) {
				PoolItem item = l.pool.get(k);
				PoolItem.toHalfFloat(item, data);
				bbuf.put(data, 0, item.used * 2);
			}

			l.offset = pos;
			pos += l.verticesCnt;

			LayerPool.add(l.pool);
			l.pool = null;
		}

		bbuf.position(0);

		// not needed for drawing
		layers = null;

		return bbuf;
	}
}
