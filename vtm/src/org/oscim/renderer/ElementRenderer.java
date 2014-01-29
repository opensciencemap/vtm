/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.renderer;

import static org.oscim.renderer.elements.RenderElement.BITMAP;
import static org.oscim.renderer.elements.RenderElement.LINE;
import static org.oscim.renderer.elements.RenderElement.MESH;
import static org.oscim.renderer.elements.RenderElement.POLYGON;
import static org.oscim.renderer.elements.RenderElement.SYMBOL;
import static org.oscim.renderer.elements.RenderElement.TEXLINE;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.renderer.elements.RenderElement;
import org.oscim.renderer.elements.TextureLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to use the renderer.elements for drawing.
 * 
 * All methods that modify 'layers' MUST be synchronized!
 */
public abstract class ElementRenderer extends LayerRenderer {

	static final Logger log = LoggerFactory.getLogger(ElementRenderer.class);

	private static short[] fillCoords;

	static {
		short s = (short) (Tile.SIZE * MapRenderer.COORD_SCALE);
		fillCoords = new short[] { 0, s, s, s, 0, 0, s, 0 };
	}

	/**
	 * Use mMapPosition.copy(position) to keep the position for which
	 * the Overlay is *compiled*. NOTE: required by setMatrix utility
	 * functions to draw this layer fixed to the map
	 */
	protected MapPosition mMapPosition;

	/** Wrap around dateline */
	protected boolean mFlipOnDateLine = true;

	/** Layer data for rendering */
	public final ElementLayers layers;

	public ElementRenderer() {
		layers = new ElementLayers();
		mMapPosition = new MapPosition();
	}

	/**
	 * Render all 'layers'
	 */
	@Override
	protected synchronized void render(MapPosition pos, Matrices m) {
		MapPosition layerPos = mMapPosition;

		layers.vbo.bind();
		GLState.test(false, false);
		GLState.blend(true);

		float div = (float) (pos.scale / layerPos.scale);

		RenderElement l = layers.getBaseLayers();

		if (l != null)
			setMatrix(pos, m, true);

		while (l != null) {
			if (l.type == POLYGON) {
				l = PolygonLayer.Renderer.draw(pos, l, m, true, 1, false);
				continue;
			}
			if (l.type == LINE) {
				l = LineLayer.Renderer.draw(layers, l, pos, m, div);
				continue;
			}
			if (l.type == TEXLINE) {
				l = LineTexLayer.Renderer.draw(layers, l, pos, m, div);
				continue;
			}
			if (l.type == MESH) {
				l = MeshLayer.Renderer.draw(pos, l, m);
				continue;
			}
			log.debug("invalid layer {}", l.type);
			break;
		}

		l = layers.getTextureLayers();
		if (l != null)
			setMatrix(pos, m, false);
		while (l != null) {
			if (l.type == BITMAP) {
				l = BitmapLayer.Renderer.draw(l, m, 1, 1);
				continue;
			}
			if (l.type == SYMBOL) {
				l = TextureLayer.Renderer.draw(l, 1 / div, m);
				continue;
			}
			log.debug("invalid layer {}", l.type);
			break;
		}
	}

	/**
	 * Compiles all layers into one BufferObject. Sets renderer to be ready
	 * when successful. When no data is available (layer.getSize() == 0) then
	 * BufferObject will be released and layers will not be rendered.
	 */
	protected synchronized void compile() {

		int newSize = layers.getSize();

		if (newSize <= 0) {
			layers.vbo = BufferObject.release(layers.vbo);
			setReady(false);
			return;
		}

		if (layers.vbo == null)
			layers.vbo = BufferObject.get(GL20.GL_ARRAY_BUFFER, newSize);

		if (uploadLayers(layers, newSize, true))
			setReady(true);
	}

	public static boolean uploadLayers(ElementLayers layers, int newSize,
	        boolean addFill) {

		if (addFill)
			newSize += 8;

		ShortBuffer sbuf = MapRenderer.getShortBuffer(newSize);

		if (addFill)
			sbuf.put(fillCoords, 0, 8);

		layers.compile(sbuf, addFill);

		if (newSize != sbuf.position()) {
			log.debug("wrong size: "
			        + " new size: " + newSize
			        + " buffer pos: " + sbuf.position()
			        + " buffer limit: " + sbuf.limit()
			        + " buffer fill: " + sbuf.remaining());
			return false;
		}

		layers.vbo.loadBufferData(sbuf.flip(), newSize * 2);
		return true;
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition.
	 * Use this to 'stick' your layer to the map. Note: Vertex coordinates
	 * are assumed to be scaled by MapRenderer.COORD_SCALE (== 8).
	 * 
	 * @param position
	 *            current MapPosition
	 * @param matrices
	 *            current Matrices
	 * @param project
	 *            if true apply view- and projection, or just view otherwise.
	 */
	protected void setMatrix(MapPosition position, Matrices matrices, boolean project) {
		MapPosition oPos = mMapPosition;

		double tileScale = Tile.SIZE * position.scale;

		double x = oPos.x - position.x;
		double y = oPos.y - position.y;

		if (mFlipOnDateLine) {
			//wrap around date-line
			while (x < 0.5)
				x += 1.0;
			while (x > 0.5)
				x -= 1.0;
		}

		matrices.mvp.setTransScale((float) (x * tileScale),
		                           (float) (y * tileScale),
		                           (float) (position.scale / oPos.scale)
		                                   / MapRenderer.COORD_SCALE);

		matrices.mvp.multiplyLhs(project ? matrices.viewproj : matrices.view);
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition and add
	 * matrices.viewproj
	 * 
	 * @param position ...
	 * @param matrices ...
	 */
	protected void setMatrix(MapPosition position, Matrices matrices) {
		setMatrix(position, matrices, true);
	}
}
