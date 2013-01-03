/*
 * Copyright 2012, 2013 OpenScienceMap
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

import java.nio.ShortBuffer;

import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class Layers {

	// mixed Polygon and Line layers
	public Layer layers;

	public Layer textureLayers;
	public Layer extrusionLayers;

	// To not need to switch VertexAttribPointer positions all the time:
	// 1. polygons are packed in VBO at offset 0
	// 2. lines afterwards at lineOffset
	// 3. other layers keep their byte offset in Layer.offset
	public int lineOffset;

	private Layer mCurLayer;

	// get or add the line- or polygon-layer for a level.
	public Layer getLayer(int level, byte type) {
		Layer l = layers;
		Layer ret = null;

		if (mCurLayer != null && mCurLayer.layer == level) {
			ret = mCurLayer;
		} else if (l == null || l.layer > level) {
			// insert new layer at start
			l = null;
		} else {
			while (true) {
				if (l.layer == level) {
					// found layer
					ret = l;
					break;
				}

				if (l.next == null || l.next.layer > level) {
					// insert new layer between current and next layer
					break;
				}
				l = l.next;
			}
		}
		if (ret == null) {
			if (type == Layer.LINE)
				ret = new LineLayer(level);
			else if (type == Layer.POLYGON)
				ret = new PolygonLayer(level);
			else
				return null;

			if (l == null) {
				// insert at start
				ret.next = layers;
				layers = ret;
			} else {
				ret.next = l.next;
				l.next = ret;
			}
		} else if (ret.type != type) {
			Log.d("...", "wrong layer type " + ret.type + " " + type);
			// FIXME thorw exception
			return null;
		}

		return ret;
	}

	//	public boolean uploadLayers(BufferObject vbo, boolean addFill, boolean limit) {
	//
	//		int newSize = getSize();
	//		if (newSize == 0) {
	//			// Log.d(TAG, "empty");
	//			return true;
	//		}
	//
	//		GLES20.glBindBuffer(GL_ARRAY_BUFFER, vbo.id);
	//
	//		// use multiple buffers to avoid overwriting buffer while current
	//		// data is uploaded (or rather the blocking which is probably done to
	//		// avoid overwriting)
	//		int curBuffer = uploadCnt++ % rotateBuffers;
	//
	//		ShortBuffer sbuf = shortBuffer[curBuffer];
	//
	//		// add fill coordinates
	//		if (addFill)
	//			newSize += 8;
	//
	//		if (sbuf.capacity() < newSize) {
	//			sbuf = ByteBuffer
	//					.allocateDirect(newSize * SHORT_BYTES)
	//					.order(ByteOrder.nativeOrder())
	//					.asShortBuffer();
	//
	//			shortBuffer[curBuffer] = sbuf;
	//		} else {
	//			sbuf.clear();
	//			// if (addFill)
	//			// sbuf.position(8);
	//		}
	//
	//		if (addFill)
	//			sbuf.put(mFillCoords, 0, 8);
	//
	//		compile(sbuf, addFill);
	//		
	//		sbuf.flip();
	//
	//		if (newSize != sbuf.remaining()) {
	//			Log.d(TAG, "wrong size: "
	//					+ newSize + " "
	//					+ sbuf.position() + " "
	//					+ sbuf.limit() + " "
	//					+ sbuf.remaining());
	//			return false;
	//		}
	//		newSize *= SHORT_BYTES;
	//
	//		// reuse memory allocated for vbo when possible and allocated
	//		// memory is less then four times the new data
	//		if (vbo.size > newSize && vbo.size < newSize * 4
	//				&& !limit) {
	//			GLES20.glBufferSubData(GL_ARRAY_BUFFER, 0, newSize, sbuf);
	//		} else {
	//			//mBufferMemoryUsage += newSize - vbo.size;
	//			vbo.size = newSize;
	//			GLES20.glBufferData(GL_ARRAY_BUFFER, vbo.size, sbuf, GL_DYNAMIC_DRAW);
	//			//mBufferMemoryUsage += vbo.size;
	//		}
	//
	//		return true;
	//	}

	private static int LINE_VERTEX_SHORTS = 4;
	private static int POLY_VERTEX_SHORTS = 2;
	private static int TEXTURE_VERTEX_SHORTS = 6;

	//private static int EXTRUSION_VERTEX_SHORTS = 4;

	public int getSize() {

		int size = 0;

		for (Layer l = layers; l != null; l = l.next) {
			if (l.type == Layer.LINE)
				size += l.verticesCnt * LINE_VERTEX_SHORTS;
			else
				size += l.verticesCnt * POLY_VERTEX_SHORTS;
		}

		for (Layer l = textureLayers; l != null; l = l.next)
			size += l.verticesCnt * TEXTURE_VERTEX_SHORTS;

		//for (Layer l = extrusionLayers; l != null; l = l.next)
		//	size += l.verticesCnt * EXTRUSION_VERTEX_SHORTS;

		return size;
	}

	public void compile(ShortBuffer sbuf, boolean addFill) {
		// offset from fill coordinates
		int pos = 0;
		if (addFill)
			pos = 4;

		// add polygons first, needed to get the offsets right...
		addLayerItems(sbuf, layers, Layer.POLYGON, pos);

		lineOffset = sbuf.position() * 2; // * short-bytes
		addLayerItems(sbuf, layers, Layer.LINE, 0);

		for (Layer l = textureLayers; l != null; l = l.next) {
			TextureLayer tl = (TextureLayer) l;
			tl.compile(sbuf);
		}

		//		for (Layer l = extrusionLayers; l != null; l = l.next) {
		//			ExtrusionLayer tl = (ExtrusionLayer) l;
		//			tl.compile(sbuf);
		//		}
	}

	// optimization for lines and polygon: collect all pool items and add back in one go
	private static void addLayerItems(ShortBuffer sbuf, Layer l, byte type, int pos) {
		VertexPoolItem last = null, items = null;

		for (; l != null; l = l.next) {
			if (l.type != type)
				continue;

			for (VertexPoolItem it = l.pool; it != null; it = it.next) {
				if (it.next == null)
					sbuf.put(it.vertices, 0, it.used);
				else
					sbuf.put(it.vertices, 0, VertexPoolItem.SIZE);

				last = it;
			}
			if (last == null)
				continue;

			l.offset = pos;
			pos += l.verticesCnt;

			last.next = items;
			items = l.pool;
			last = null;

			l.pool = null;
			l.curItem = null;
		}
		VertexPool.release(items);
	}

	static void addPoolItems(Layer l, ShortBuffer sbuf) {
		l.offset = sbuf.position() * 2; // (* short-bytes)

		for (VertexPoolItem it = l.pool; it != null; it = it.next) {
			if (it.next == null)
				sbuf.put(it.vertices, 0, it.used);
			else
				sbuf.put(it.vertices, 0, VertexPoolItem.SIZE);
		}

		VertexPool.release(l.pool);
		l.pool = null;
	}

	// cleanup only when layers are not used by tile or overlay anymore!
	public void clear() {
		while (layers != null) {
			Layer l = layers;
			if (l.pool != null) {
				VertexPool.release(l.pool);
				l.pool = null;
				l.curItem = null;
			}
			layers = layers.next;
		}

		Layer l = textureLayers;
		while (l != null) {

			l.clear();

			if (l.pool != null) {
				VertexPool.release(l.pool);
				l.pool = null;
				l.curItem = null;
			}

			l = l.next;
		}
		textureLayers = null;

		l = extrusionLayers;
		while (l != null) {

			l.clear();

			if (l.pool != null) {
				VertexPool.release(l.pool);
				l.pool = null;
				l.curItem = null;
			}

			l = l.next;
		}
		extrusionLayers = null;
	}
}
