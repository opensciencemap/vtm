/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.renderer.BufferObject;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.pool.Inlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElementLayers {
	static final Logger log = LoggerFactory.getLogger(ElementLayers.class);

	public static void initRenderer(GL20 gl) {
		RenderElement.GL = gl;

		LineLayer.Renderer.init();
		LineTexLayer.Renderer.init();
		PolygonLayer.Renderer.init();
		TextureLayer.Renderer.init();
		BitmapLayer.Renderer.init();
		MeshLayer.Renderer.init();

		TextureItem.init(gl, 0);
	}

	// FIXME use one ArrayList for these!

	// mixed Polygon- and LineLayer
	public RenderElement baseLayers;
	// Text- and SymbolLayer
	public RenderElement textureLayers;
	//
	public RenderElement extrusionLayers;

	// VBO holds all vertex data to draw lines and polygons
	// after compilation.
	// Layout:
	//   16 bytes fill coordinates,
	//   n bytes polygon vertices,
	//   m bytes lines vertices
	//  ...
	public BufferObject vbo;

	// To not need to switch VertexAttribPointer positions all the time:
	// 1. polygons are packed in VBO at offset 0
	// 2. lines afterwards at lineOffset
	// 3. other layers keep their byte offset in RenderElement.offset
	public int lineOffset;
	public int texLineOffset;

	// time when layers became first rendered (in uptime)
	// used for animations
	public long time;

	private RenderElement mCurLayer;

	/**
	 * add the LineLayer for a level with a given Line style. Levels are
	 * ordered from bottom (0) to top
	 */
	public LineLayer addLineLayer(int level, Line style) {
		LineLayer ll = (LineLayer) getLayer(level, RenderElement.LINE);
		if (ll == null)
			return null;

		ll.width = style.width;
		ll.line = style;

		return ll;
	}

	/**
	 * Get or add the LineLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public LineLayer getLineLayer(int level) {
		return (LineLayer) getLayer(level, RenderElement.LINE);
	}

	/**
	 * Get or add the MeshLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public MeshLayer getMeshLayer(int level) {
		return (MeshLayer) getLayer(level, RenderElement.MESH);
	}

	/**
	 * Get or add the PolygonLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public PolygonLayer getPolygonLayer(int level) {
		return (PolygonLayer) getLayer(level, RenderElement.POLYGON);
	}

	/**
	 * Get or add the TexLineLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public LineTexLayer getLineTexLayer(int level) {
		return (LineTexLayer) getLayer(level, RenderElement.TEXLINE);
	}

	public TextLayer addTextLayer(TextLayer textLayer) {
		textLayer.next = textureLayers;
		textureLayers = textLayer;
		return textLayer;
	}

	private RenderElement getLayer(int level, byte type) {
		RenderElement l = baseLayers;
		RenderElement renderElement = null;

		if (!(type == RenderElement.LINE
		        || type == RenderElement.POLYGON
		        || type == RenderElement.TEXLINE
		        || type == RenderElement.MESH))
			throw new IllegalArgumentException("invalid layer type");

		if (mCurLayer != null && mCurLayer.level == level) {
			renderElement = mCurLayer;
		} else {

			if (l == null || l.level > level) {
				// insert new layer at start
				l = null;
			} else {
				while (true) {
					// found layer
					if (l.level == level) {
						renderElement = l;
						break;
					}

					// insert new layer between current and next layer
					if (l.next == null || l.next.level > level)
						break;

					l = l.next;
				}
			}

			if (renderElement == null) {
				// add a new RenderElement
				if (type == RenderElement.LINE)
					renderElement = new LineLayer(level);
				else if (type == RenderElement.POLYGON)
					renderElement = new PolygonLayer(level);
				else if (type == RenderElement.TEXLINE)
					renderElement = new LineTexLayer(level);
				else if (type == RenderElement.MESH)
					renderElement = new MeshLayer(level);

				if (renderElement == null)
					throw new IllegalArgumentException();

				if (l == null) {
					// insert at start
					renderElement.next = baseLayers;
					baseLayers = renderElement;
				} else {
					renderElement.next = l.next;
					l.next = renderElement;
				}
			}
		}

		if (renderElement.type != type) {
			// check if found layer matches requested type
			log.debug("BUG wrong layer " + renderElement.type + " " + type +
			        " on layer " + renderElement.level);

			throw new IllegalArgumentException();
		}

		mCurLayer = renderElement;

		return renderElement;
	}

	private final static int[] VERTEX_SHORT_CNT = {
	        4, // LINE_VERTEX_SHORTS
	        2, // POLY_VERTEX_SHORTS
	        2, // MESH_VERTEX_SHORTS
	        6, // TEXLINE_VERTEX_SHORTS
	};

	private final static int TEXTURE_VERTEX_SHORTS = 6;

	private final static int SHORT_BYTES = 2;

	public int getSize() {
		int size = 0;

		for (RenderElement l = baseLayers; l != null; l = l.next)
			size += l.verticesCnt * VERTEX_SHORT_CNT[l.type];

		for (RenderElement l = textureLayers; l != null; l = l.next)
			size += l.verticesCnt * TEXTURE_VERTEX_SHORTS;

		return size;
	}

	public void compile(ShortBuffer sbuf, boolean addFill) {
		// offset from fill coordinates
		int pos = 0;
		int size = 0;

		if (addFill) {
			pos = 4;
			size = 8;
		}

		size += addLayerItems(sbuf, baseLayers, RenderElement.POLYGON, pos);

		lineOffset = size * SHORT_BYTES;
		size += addLayerItems(sbuf, baseLayers, RenderElement.LINE, 0);

		texLineOffset = size * SHORT_BYTES;

		for (RenderElement l = baseLayers; l != null; l = l.next) {
			if (l.type == RenderElement.TEXLINE) {
				addPoolItems(l, sbuf);
				// add additional vertex for interleaving,
				// see TexLineLayer.
				sbuf.position(sbuf.position() + 6);
			}
		}

		for (RenderElement l = baseLayers; l != null; l = l.next) {
			if (l.type == RenderElement.MESH)
				l.compile(sbuf);
		}

		for (RenderElement l = textureLayers; l != null; l = l.next) {
			//TextureLayer tl = (TextureLayer) l;
			l.compile(sbuf);
		}

		// extrusion layers are compiled by extrusion overlay
		//		for (RenderElement l = extrusionLayers; l != null; l = l.next) {
		//			ExtrusionLayer tl = (ExtrusionLayer) l;
		//			tl.compile(sbuf);
		//		}
	}

	// optimization for Line- and PolygonLayer:
	// collect all pool items and add back in one go
	private static int addLayerItems(ShortBuffer sbuf, RenderElement l, byte type, int pos) {
		VertexItem last = null, items = null;
		int size = 0;

		for (; l != null; l = l.next) {
			if (l.type != type)
				continue;

			for (VertexItem it = l.vertexItems; it != null; it = it.next) {
				if (it.next == null) {
					size += it.used;
					sbuf.put(it.vertices, 0, it.used);
				}
				else {
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
		items = VertexItem.pool.releaseAll(items);

		return size;
	}

	static void addPoolItems(RenderElement l, ShortBuffer sbuf) {
		// offset of layer data in vbo
		l.offset = sbuf.position() * SHORT_BYTES;

		for (VertexItem it = l.vertexItems; it != null; it = it.next) {
			if (it.next == null)
				sbuf.put(it.vertices, 0, it.used);
			else
				sbuf.put(it.vertices, 0, VertexItem.SIZE);
		}

		l.vertexItems = VertexItem.pool.releaseAll(l.vertexItems);
	}

	// cleanup only when layers are not used by tile or overlay anymore!
	public void clear() {

		// clear line and polygon layers directly
		for (RenderElement l = baseLayers; l != null; l = l.next) {
			if (l.vertexItems != null) {
				l.vertexItems = VertexItem.pool.releaseAll(l.vertexItems);
				l.curItem = null;
			}
			l.verticesCnt = 0;
		}

		for (RenderElement l = textureLayers; l != null; l = l.next) {
			l.clear();
		}

		for (RenderElement l = extrusionLayers; l != null; l = l.next) {
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

	public void add(ExtrusionLayer l) {
		extrusionLayers = Inlist.appendItem(extrusionLayers, l);
	}

	public ExtrusionLayer getExtrusionLayers() {
		return (ExtrusionLayer) extrusionLayers;
	}

}
