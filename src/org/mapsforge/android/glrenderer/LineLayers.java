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
import java.nio.ShortBuffer;

class LineLayers {
	private static int NUM_VERTEX_FLOATS = 4;

	// static FloatBuffer compileLayerData(LineLayer layers, FloatBuffer buf) {
	// FloatBuffer fbuf = buf;
	// int size = 0;
	//
	// for (LineLayer l = layers; l != null; l = l.next)
	// size += l.verticesCnt;
	//
	// size *= NUM_VERTEX_FLOATS;
	//
	// if (buf == null || buf.capacity() < size) {
	// ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 4).order(
	// ByteOrder.nativeOrder());
	// fbuf = bbuf.asFloatBuffer();
	// } else {
	// fbuf.clear();
	// }
	// int pos = 0;
	//
	// PoolItem last = null, items = null;
	//
	// for (LineLayer l = layers; l != null; l = l.next) {
	// if (l.isOutline)
	// continue;
	//
	// for (PoolItem item = l.pool; item != null; item = item.next) {
	// fbuf.put(item.vertices, 0, item.used);
	// last = item;
	// }
	// l.offset = pos;
	// pos += l.verticesCnt;
	//
	// if (last != null) {
	// last.next = items;
	// items = l.pool;
	// }
	//
	// l.pool = null;
	// }
	//
	// VertexPool.add(items);
	//
	// fbuf.flip();
	//
	// return fbuf;
	// }
	//
	// static ShortBuffer compileLayerData(LineLayer layers, ShortBuffer buf) {
	// int size = 0;
	// ShortBuffer sbuf = buf;
	//
	// for (LineLayer l = layers; l != null; l = l.next)
	// size += l.verticesCnt;
	//
	// size *= NUM_VERTEX_FLOATS;
	//
	// if (buf == null || buf.capacity() < size) {
	// ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 2).order(
	// ByteOrder.nativeOrder());
	// sbuf = bbuf.asShortBuffer();
	// } else {
	// sbuf.clear();
	// }
	// int pos = 0;
	//
	// short[] data = new short[PoolItem.SIZE];
	//
	// PoolItem last = null, items = null;
	//
	// for (LineLayer l = layers; l != null; l = l.next) {
	// if (l.isOutline)
	// continue;
	//
	// for (PoolItem item = l.pool; item != null; item = item.next) {
	// PoolItem.toHalfFloat(item, data);
	// sbuf.put(data, 0, item.used);
	// last = item;
	// }
	//
	// l.offset = pos;
	// pos += l.verticesCnt;
	//
	// if (last != null) {
	// last.next = items;
	// items = l.pool;
	// }
	//
	// l.pool = null;
	// }
	//
	// VertexPool.add(items);
	//
	// sbuf.flip();
	//
	// return sbuf;
	// }
	//
	// static void clear(LineLayer layer) {
	// for (LineLayer l = layer; l != null; l = l.next) {
	// if (l.pool != null)
	// VertexPool.add(l.pool);
	// }
	// }

	static ShortBuffer compileLayerData(LineLayer layers, ShortBuffer buf) {
		int size = 0;
		ShortBuffer sbuf = buf;

		for (LineLayer l = layers; l != null; l = l.next)
			size += l.verticesCnt;

		size *= NUM_VERTEX_FLOATS;

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 2).order(
					ByteOrder.nativeOrder());
			sbuf = bbuf.asShortBuffer();
		} else {
			sbuf.clear();
		}
		int pos = 0;

		// short[] data = new short[PoolItem.SIZE];

		ShortItem last = null, items = null;

		for (LineLayer l = layers; l != null; l = l.next) {
			if (l.isOutline)
				continue;

			for (ShortItem item = l.pool; item != null; item = item.next) {
				// PoolItem.toHalfFloat(item, data);
				// sbuf.put(data, 0, item.used);
				sbuf.put(item.vertices, 0, item.used);
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

		ShortPool.add(items);

		sbuf.flip();

		return sbuf;
	}

	static void clear(LineLayer layer) {
		for (LineLayer l = layer; l != null; l = l.next) {
			if (l.pool != null)
				ShortPool.add(l.pool);
		}
	}
}
