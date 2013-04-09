/*
 * Copyright 2012, 2013 Hannes Janetzek
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

import org.oscim.renderer.BufferObject;

import android.util.Log;

public class Layers {
	private final static String TAG = Layers.class.getName();

	// mixed Polygon- and LineLayer
	public Layer baseLayers;
	public Layer textureLayers;
	public Layer extrusionLayers;

	// VBO holds all vertex data to draw lines and polygons
	// after are compilation.
	// Layout:
	//   16 bytes fill coordinates,
	//   n bytes polygon vertices,
	//   m bytes lines vertices
	//  ...
	public BufferObject vbo;

	// To not need to switch VertexAttribPointer positions all the time:
	// 1. polygons are packed in VBO at offset 0
	// 2. lines afterwards at lineOffset
	// 3. other layers keep their byte offset in Layer.offset
	public int lineOffset;
	public int texLineOffset;

	// time when layers became first rendered (in uptime)
	// used for animations
	public long time;

	private Layer mCurLayer;

	// get or add the Line- or PolygonLayer for a level.
	public Layer getLayer(int level, byte type) {
		Layer l = baseLayers;
		Layer layer = null;

		if (mCurLayer != null && mCurLayer.level == level) {
			layer = mCurLayer;
		} else {

			if (l == null || l.level > level) {
				// insert new layer at start
				l = null;
			} else {
				while (true) {
					// found layer
					if (l.level == level) {
						layer = l;
						break;
					}

					// insert new layer between current and next layer
					if (l.next == null || l.next.level > level)
						break;

					l = l.next;
				}
			}

			if (layer == null) {
				// add a new Layer
				if (type == Layer.LINE)
					layer = new LineLayer(level);
				else if (type == Layer.POLYGON)
					layer = new PolygonLayer(level);
				else if (type == Layer.TEXLINE)
					layer = new LineTexLayer(level);
				else
					// TODO throw execption
					return null;

				if (l == null) {
					// insert at start
					layer.next = baseLayers;
					baseLayers = layer;
				} else {
					layer.next = l.next;
					l.next = layer;
				}
			}
		}

		if (layer.type != type) {
			// check if found layer matches requested type
			Log.d(TAG, "BUG wrong layer " + layer.type + " " + type);
			// TODO throw exception
			return null;
		}

		mCurLayer = layer;

		return layer;
	}

	private final static int[] VERTEX_SHORT_CNT = {
			4, // LINE_VERTEX_SHORTS
			2, // POLY_VERTEX_SHORTS
			6, // TEXLINE_VERTEX_SHORTS
	};

	private final static int TEXTURE_VERTEX_SHORTS = 6;

	private final static int SHORT_BYTES = 2;

	public int getSize() {
		int size = 0;

		for (Layer l = baseLayers; l != null; l = l.next)
			size += l.verticesCnt * VERTEX_SHORT_CNT[l.type];

		for (Layer l = textureLayers; l != null; l = l.next)
			size += l.verticesCnt * TEXTURE_VERTEX_SHORTS;

		return size;
	}

	public void compile(ShortBuffer sbuf, boolean addFill) {
		// offset from fill coordinates
		int pos = 0;
		int size = 0;

		if (addFill){
			pos = 4;
			size = 8;
		}

		size += addLayerItems(sbuf, baseLayers, Layer.POLYGON, pos);

		lineOffset = size * SHORT_BYTES;
		size += addLayerItems(sbuf, baseLayers, Layer.LINE, 0);

		texLineOffset = size * SHORT_BYTES;
		for (Layer l = baseLayers; l != null; l= l.next){
			if (l.type == Layer.TEXLINE){
				// HACK, see LineTexLayer
				//sbuf.position(sbuf.position() + 6);
				addPoolItems(l, sbuf);
				//l.offset -= 12;

				sbuf.position(sbuf.position() + 6);
			}
		}

		//size += addLayerItems(sbuf, baseLayers, Layer.TEXLINE, 0);

		for (Layer l = textureLayers; l != null; l = l.next) {
			TextureLayer tl = (TextureLayer) l;
			tl.compile(sbuf);
		}

		// extrusion layers are compiled by extrusion overlay
		//		for (Layer l = extrusionLayers; l != null; l = l.next) {
		//			ExtrusionLayer tl = (ExtrusionLayer) l;
		//			tl.compile(sbuf);
		//		}
	}

	// optimization for Line- and PolygonLayer:
	// collect all pool items and add back in one go
	private static int addLayerItems(ShortBuffer sbuf, Layer l, byte type, int pos) {
		VertexItem last = null, items = null;
		int size = 0;

		for (; l != null; l = l.next) {
			if (l.type != type)
				continue;

			for (VertexItem it = l.vertexItems; it != null; it = it.next) {
				if (it.next == null){
					size += it.used;
					sbuf.put(it.vertices, 0, it.used);
				}
				else{
					size += VertexItem.SIZE;
					sbuf.put(it.vertices, 0, VertexItem.SIZE);
				}
				last = it;
			}
			if (last == null)
				continue;

			l.offset = pos;

			pos += l.verticesCnt;

			last.next = items;
			items = l.vertexItems;
			last = null;

			l.vertexItems = null;
			l.curItem = null;
		}
		VertexItem.pool.releaseAll(items);

		return size;
	}

	static void addPoolItems(Layer l, ShortBuffer sbuf) {
		// offset of layer data in vbo
		l.offset = sbuf.position() * SHORT_BYTES;

		for (VertexItem it = l.vertexItems; it != null; it = it.next) {
			if (it.next == null)
				sbuf.put(it.vertices, 0, it.used);
			else
				sbuf.put(it.vertices, 0, VertexItem.SIZE);
		}

		VertexItem.pool.releaseAll(l.vertexItems);
		l.vertexItems = null;
	}

	// cleanup only when layers are not used by tile or overlay anymore!
	public void clear() {

		// clear line and polygon layers directly
		Layer l = baseLayers;
		while (l != null) {
			if (l.vertexItems != null) {
				VertexItem.pool.releaseAll(l.vertexItems);
				l.vertexItems = null;
				l.curItem = null;
			}
			l = l.next;
		}

		for (l = textureLayers; l != null; l = l.next) {
			l.clear();
		}

		for (l = extrusionLayers; l != null; l = l.next) {
			l.clear();
		}
		baseLayers = null;
		textureLayers = null;
		extrusionLayers = null;
		mCurLayer = null;
		//		if (vbo != null){
		//			BufferObject.release(vbo);
		//			vbo = null;
		//		}
	}
}
