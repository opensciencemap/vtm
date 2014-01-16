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
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to use the renderer.elements for drawing
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
	 * the Overlay is _compiled_. NOTE: required by setMatrix utility
	 * functions to draw this layer fixed to the map
	 */
	protected MapPosition mMapPosition;

	public final ElementLayers layers;

	public ElementRenderer() {
		layers = new ElementLayers();
		mMapPosition = new MapPosition();
	}

	/**
	 * Render all 'layers'
	 */
	@Override
	protected synchronized void render(MapPosition curPos, Matrices m) {
		MapPosition pos = mMapPosition;

		float div = FastMath.pow(pos.zoomLevel - curPos.zoomLevel);

		layers.vbo.bind();
		GLState.test(false, false);
		GLState.blend(true);
		int simple = (curPos.tilt < 1 ? 1 : 0);

		if (layers.baseLayers != null) {
			setMatrix(curPos, m, true);

			for (RenderElement l = layers.baseLayers; l != null;) {
				switch (l.type) {
					case RenderElement.POLYGON:
						l = PolygonLayer.Renderer.draw(curPos, l, m, true, 1, false);
						break;

					case RenderElement.LINE:
						l = LineLayer.Renderer.draw(layers, l, curPos, m, div, simple);
						break;

					case RenderElement.TEXLINE:
						l = LineTexLayer.Renderer.draw(layers, l, curPos, m, div);
						break;

					case RenderElement.MESH:
						l = MeshLayer.Renderer.draw(pos, l, m);
						break;

					default:
						log.debug("invalid layer");
						l = l.next;
						break;
				}
			}
		}

		if (layers.textureLayers != null) {
			setMatrix(curPos, m, false);

			float scale = (float) (pos.scale / curPos.scale);

			for (RenderElement l = layers.textureLayers; l != null;) {
				switch (l.type) {
					case RenderElement.BITMAP:
						l = BitmapLayer.Renderer.draw(l, m, 1, 1);
						break;

					default:
						l = TextureLayer.Renderer.draw(l, scale, m);
				}
			}
		}
	}

	/**
	 * Compiles all layers into one BufferObject. Sets renderer to be ready
	 * when successful. When no data is available (layer.getSize() == 0) then
	 * BufferObject will be released and layers will not be rendered.
	 */
	protected void compile() {
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
		// add fill coordinates
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

		// wrap around date-line
		//	while (x < -1)
		//		x += 1.0;
		//	while (x > 2)
		//		x -= 1.0;

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
