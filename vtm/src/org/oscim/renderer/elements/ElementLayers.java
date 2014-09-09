/*
 * Copyright 2012-2014 Hannes Janetzek
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

import static org.oscim.renderer.elements.RenderElement.HAIRLINE;
import static org.oscim.renderer.elements.RenderElement.LINE;
import static org.oscim.renderer.elements.RenderElement.MESH;
import static org.oscim.renderer.elements.RenderElement.POLYGON;
import static org.oscim.renderer.elements.RenderElement.TEXLINE;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is primarily intended for rendering the vector elements of a
 * MapTile. It can be used for other purposes as well but some optimizations
 * (and limitations) probably wont make sense in different contexts.
 */
public class ElementLayers extends TileData {

	static final Logger log = LoggerFactory.getLogger(ElementLayers.class);

	public final static int[] VERTEX_SHORT_CNT = {
	        4, // LINE_VERTEX
	        6, // TEXLINE_VERTEX
	        2, // POLY_VERTEX
	        2, // MESH_VERTEX
	        4, // EXTRUSION_VERTEX
	        2, // HAIRLINE_VERTEX
	};

	private final static int TEXTURE_VERTEX_SHORTS = 6;
	private final static int SHORT_BYTES = 2;

	/** mixed Polygon- and LineLayer */
	private RenderElement baseLayers;

	/** Text- and SymbolLayer */
	private RenderElement textureLayers;

	/**
	 * VBO holds all vertex data to draw lines and polygons after compilation.
	 * Layout:
	 * 16 bytes fill coordinates,
	 * n bytes polygon vertices,
	 * m bytes lines vertices
	 * ...
	 */
	public BufferObject vbo;
	public BufferObject ibo;

	/**
	 * To not need to switch VertexAttribPointer positions all the time:
	 * 1. polygons are packed in VBO at offset 0
	 * 2. lines afterwards at lineOffset
	 * 3. other layers keep their byte offset in offset
	 */
	public int[] offset = { 0, 0 };

	private RenderElement mCurLayer;

	/**
	 * add the LineLayer for a level with a given Line style. Levels are
	 * ordered from bottom (0) to top
	 */
	public LineLayer addLineLayer(int level, LineStyle style) {
		LineLayer l = (LineLayer) getLayer(level, LINE);
		if (l == null)
			return null;
		// FIXME l.scale = style.width;
		l.scale = 1;
		l.line = style;
		return l;
	}

	public PolygonLayer addPolygonLayer(int level, AreaStyle style) {
		PolygonLayer l = (PolygonLayer) getLayer(level, POLYGON);
		if (l == null)
			return null;
		l.area = style;
		return l;
	}

	public MeshLayer addMeshLayer(int level, AreaStyle style) {
		MeshLayer l = (MeshLayer) getLayer(level, MESH);
		if (l == null)
			return null;
		l.area = style;
		return l;
	}

	public HairLineLayer addHairLineLayer(int level, LineStyle style) {
		HairLineLayer ll = getHairLineLayer(level);
		if (ll == null)
			return null;
		ll.line = style;

		return ll;
	}

	/**
	 * Get or add the LineLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public LineLayer getLineLayer(int level) {
		return (LineLayer) getLayer(level, LINE);
	}

	/**
	 * Get or add the MeshLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public MeshLayer getMeshLayer(int level) {
		return (MeshLayer) getLayer(level, MESH);
	}

	/**
	 * Get or add the PolygonLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public PolygonLayer getPolygonLayer(int level) {
		return (PolygonLayer) getLayer(level, POLYGON);
	}

	/**
	 * Get or add the TexLineLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public LineTexLayer getLineTexLayer(int level) {
		return (LineTexLayer) getLayer(level, TEXLINE);
	}

	/**
	 * Get or add the TexLineLayer for a level. Levels are ordered from
	 * bottom (0) to top
	 */
	public HairLineLayer getHairLineLayer(int level) {
		return (HairLineLayer) getLayer(level, HAIRLINE);
	}

	public TextLayer addTextLayer(TextLayer textLayer) {
		textLayer.next = textureLayers;
		textureLayers = textLayer;
		return textLayer;
	}

	/**
	 * Set new Base-Layers and clear previous.
	 */
	public void setBaseLayers(RenderElement layers) {
		for (RenderElement l = baseLayers; l != null; l = l.next)
			l.clear();

		baseLayers = layers;
	}

	public RenderElement getBaseLayers() {
		return baseLayers;
	}

	/**
	 * Set new TextureLayers and clear previous.
	 */
	public void setTextureLayers(TextureLayer tl) {
		for (RenderElement l = textureLayers; l != null; l = l.next)
			l.clear();

		textureLayers = tl;
	}

	public RenderElement getTextureLayers() {
		return textureLayers;
	}

	private RenderElement getLayer(int level, int type) {
		RenderElement layer = null;

		if (mCurLayer != null && mCurLayer.level == level) {
			layer = mCurLayer;
			if (layer.type != type) {
				log.error("BUG wrong layer {} {} on layer {}",
				          Integer.valueOf(layer.type),
				          Integer.valueOf(type),
				          Integer.valueOf(level));

				throw new IllegalArgumentException();
			}
			return layer;
		}

		RenderElement l = baseLayers;
		if (l == null || l.level > level) {
			/* insert new layer at start */
			l = null;
		} else {
			while (true) {
				/* found layer */
				if (l.level == level) {
					layer = l;
					break;
				}
				/* insert layer between current and next layer */
				if (l.next == null || l.next.level > level)
					break;

				l = l.next;
			}
		}

		if (layer == null) {
			/* add a new RenderElement */
			if (type == LINE)
				layer = new LineLayer(level);
			else if (type == POLYGON)
				layer = new PolygonLayer(level);
			else if (type == TEXLINE)
				layer = new LineTexLayer(level);
			else if (type == MESH)
				layer = new MeshLayer(level);
			else if (type == HAIRLINE)
				layer = new HairLineLayer(level);

			if (layer == null)
				throw new IllegalArgumentException();

			if (l == null) {
				/** insert at start */
				layer.next = baseLayers;
				baseLayers = layer;
			} else {
				layer.next = l.next;
				l.next = layer;
			}
		}

		/* check if found layer matches requested type */
		if (layer.type != type) {
			log.error("BUG wrong layer {} {} on layer {}",
			          Integer.valueOf(layer.type),
			          Integer.valueOf(type),
			          Integer.valueOf(level));

			throw new IllegalArgumentException();
		}

		mCurLayer = layer;

		return layer;
	}

	private int countVboSize() {
		int vboShorts = 0;

		for (RenderElement l = baseLayers; l != null; l = l.next)
			vboShorts += l.numVertices * VERTEX_SHORT_CNT[l.type];

		for (RenderElement l = textureLayers; l != null; l = l.next)
			vboShorts += l.numVertices * TEXTURE_VERTEX_SHORTS;

		return vboShorts;
	}

	private int countIboSize() {
		int numIndices = 0;

		for (RenderElement l = baseLayers; l != null; l = l.next)
			numIndices += l.numIndices;

		for (RenderElement l = textureLayers; l != null; l = l.next)
			numIndices += l.numIndices;

		return numIndices;
	}

	public void setFrom(ElementLayers layers) {
		setBaseLayers(layers.baseLayers);
		setTextureLayers((TextureLayer) layers.textureLayers);

		mCurLayer = null;
		layers.baseLayers = null;
		layers.textureLayers = null;
		layers.mCurLayer = null;
	}

	/** cleanup only when layers are not used by tile or layer anymore! */
	public void clear() {
		/* NB: set null calls clear() on each layer! */
		setBaseLayers(null);
		setTextureLayers(null);
		mCurLayer = null;

		vbo = BufferObject.release(vbo);
		ibo = BufferObject.release(ibo);
	}

	@Override
	protected void dispose() {
		clear();
	}

	public void prepare() {
		for (RenderElement l = baseLayers; l != null; l = l.next)
			l.prepare();

		for (RenderElement l = textureLayers; l != null; l = l.next)
			l.prepare();
	}

	public void bind() {
		if (vbo != null)
			vbo.bind();

		if (ibo != null)
			ibo.bind();

	}

	public boolean compile(boolean addFill) {

		int vboSize = countVboSize();

		if (vboSize <= 0) {
			// FIXME just clear?
			vbo = BufferObject.release(vbo);
			ibo = BufferObject.release(ibo);
			return false;
		}

		if (addFill)
			vboSize += 8;

		ShortBuffer vboData = MapRenderer.getShortBuffer(vboSize);

		if (addFill)
			vboData.put(fillCoords, 0, 8);

		ShortBuffer iboData = null;

		int iboSize = countIboSize();
		if (iboSize > 0) {
			iboData = MapRenderer.getShortBuffer(iboSize);
		}

		//>>compile(vboData, iboData, addFill);
		int pos = addFill ? 4 : 0;

		for (RenderElement l = baseLayers; l != null; l = l.next) {
			if (l.type == POLYGON) {
				l.compile(vboData, iboData);

				//log.debug("offset {} {}", l.offset, pos);
				l.vertexOffset = pos;
				pos += l.numVertices;
			}
		}

		offset[LINE] = vboData.position() * SHORT_BYTES;
		pos = 0;
		for (RenderElement l = baseLayers; l != null; l = l.next) {
			if (l.type == LINE) {
				l.compile(vboData, iboData);

				l.vertexOffset = pos;
				pos += l.numVertices;
			}
		}

		//offset[TEXLINE] = size * SHORT_BYTES;

		for (RenderElement l = baseLayers; l != null; l = l.next) {
			if (l.type == TEXLINE || l.type == MESH || l.type == HAIRLINE) {
				l.compile(vboData, iboData);
			}
		}

		for (RenderElement l = textureLayers; l != null; l = l.next) {
			l.compile(vboData, iboData);
		}
		//<<
		if (vboSize != vboData.position()) {
			log.debug("wrong vertex buffer size: "
			        + " new size: " + vboSize
			        + " buffer pos: " + vboData.position()
			        + " buffer limit: " + vboData.limit()
			        + " buffer fill: " + vboData.remaining());
			return false;
		}

		if (iboSize > 0 && iboSize != iboData.position()) {
			log.debug("wrong indice buffer size: "
			        + " new size: " + iboSize
			        + " buffer pos: " + iboData.position()
			        + " buffer limit: " + iboData.limit()
			        + " buffer fill: " + iboData.remaining());
			return false;
		}

		if (vbo == null)
			vbo = BufferObject.get(GL20.GL_ARRAY_BUFFER, vboSize);

		vbo.loadBufferData(vboData.flip(), vboSize * 2);

		if (iboSize > 0) {
			if (ibo == null)
				ibo = BufferObject.get(GL20.GL_ELEMENT_ARRAY_BUFFER, iboSize);

			ibo.loadBufferData(iboData.flip(), iboSize * 2);
		}

		return true;
	}

	private static short[] fillCoords;

	static {
		short s = (short) (Tile.SIZE * MapRenderer.COORD_SCALE);
		fillCoords = new short[] { 0, s, s, s, 0, 0, s, 0 };
	}

	public static void initRenderer(GL20 gl) {
		RenderElement.GL = gl;

		LineLayer.Renderer.init();
		LineTexLayer.Renderer.init();
		PolygonLayer.Renderer.init();
		TextureLayer.Renderer.init();
		BitmapLayer.Renderer.init();
		MeshLayer.Renderer.init();
		HairLineLayer.Renderer.init();

		TextureItem.init(gl);
	}
}
