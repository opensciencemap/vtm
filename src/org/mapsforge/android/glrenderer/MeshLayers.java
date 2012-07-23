/*
 * Copyright 2010, 2011, 2012 Hannes Janetzek
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
package org.mapsforge.android.glrenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.util.SparseArray;

public class MeshLayers {
	private static final int NUM_VERTEX_FLOATS = 2;

	private SparseArray<MeshLayer> layers;

	MeshLayer[] array = null;
	int size;

	MeshLayers() {
		layers = new SparseArray<MeshLayer>(10);
		size = 0;
	}

	MeshLayer getLayer(int layer, int color, int fade) {
		MeshLayer l = layers.get(layer);
		if (l != null) {
			return l;
		}

		l = new MeshLayer(layer, color);
		layers.put(layer, l);
		return l;
	}

	FloatBuffer compileLayerData(FloatBuffer buf) {
		FloatBuffer fbuf = buf;

		array = new MeshLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			MeshLayer l = layers.valueAt(i);
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

		int pos = 0;

		for (int i = 0, n = array.length; i < n; i++) {
			MeshLayer l = array[i];

			for (PoolItem item : l.pool) {
				fbuf.put(item.vertices, 0, item.used);

				// for (int j = 0; j < item.used; j++)
				// System.out.println(">" + item.vertices[j]);
			}

			l.offset = pos;
			pos += l.verticesCnt;

			LayerPool.add(l.pool);
			l.pool = null;
		}

		fbuf.position(0);
		// for (int i = 0; i < size; i++)
		// System.out.println("<" + fbuf.get());
		// fbuf.position(0);
		// System.out.println("....... mesh layer size: " + size + " " + array.length);

		// not needed for drawing
		layers = null;

		return fbuf;
	}

	ShortBuffer compileLayerData(ShortBuffer buf) {
		ShortBuffer sbuf = buf;

		array = new MeshLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			MeshLayer l = layers.valueAt(i);
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

		int pos = 0;

		for (int i = 0, n = array.length; i < n; i++) {
			MeshLayer l = array[i];

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

		// System.out.println("....... mesh layer size: " + size + " " + array.length);
		sbuf.position(0);

		// not needed for drawing
		layers = null;

		return sbuf;
	}
}
