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

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.HairLineLayer;
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

	public static final Logger log = LoggerFactory.getLogger(ElementRenderer.class);

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
	 * Render all 'buckets'
	 */
	@Override
	protected synchronized void render(GLViewport v) {
		MapPosition layerPos = mMapPosition;

		layers.bind();

		GLState.test(false, false);
		GLState.blend(true);

		float div = (float) (v.pos.scale / layerPos.scale);

		RenderElement l = layers.getBaseLayers();

		if (l != null)
			setMatrix(v, true);

		while (l != null) {
			if (l.type == POLYGON) {
				l = PolygonLayer.Renderer.draw(l, v, 1, true);
				continue;
			}
			if (l.type == LINE) {
				l = LineLayer.Renderer.draw(l, v, div, layers);
				continue;
			}
			if (l.type == TEXLINE) {
				l = LineTexLayer.Renderer.draw(l, v, div, layers);
				// rebind
				layers.ibo.bind();
				continue;
			}
			if (l.type == MESH) {
				l = MeshLayer.Renderer.draw(l, v);
				continue;
			}
			if (l.type == RenderElement.HAIRLINE) {
				l = HairLineLayer.Renderer.draw(l, v);
				continue;
			}

			log.error("invalid layer {}", l.type);
			break;
		}

		l = layers.getTextureLayers();
		if (l != null)
			setMatrix(v, false);
		while (l != null) {
			if (l.type == BITMAP) {
				l = BitmapLayer.Renderer.draw(l, v, 1, 1);
				continue;
			}
			if (l.type == SYMBOL) {
				l = TextureLayer.Renderer.draw(layers, l, v, div);
				continue;
			}
			log.error("invalid layer {}", l.type);
			break;
		}
	}

	/**
	 * Compile all layers into one BufferObject. Sets renderer to be ready
	 * when successful. When no data is available (layer.countVboSize() == 0)
	 * then BufferObject will be released and layers will not be rendered.
	 */
	protected synchronized void compile() {
		boolean ok = layers.compile(true);
		setReady(ok);
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition.
	 * Use this to 'stick' your layer to the map. Note: Vertex coordinates
	 * are assumed to be scaled by MapRenderer.COORD_SCALE (== 8).
	 * 
	 * @param v
	 *            GLViewport
	 * @param project
	 *            if true apply view- and projection, or just view otherwise.
	 */
	protected void setMatrix(GLViewport v, boolean project) {
		MapPosition oPos = mMapPosition;

		double tileScale = Tile.SIZE * v.pos.scale;

		double x = oPos.x - v.pos.x;
		double y = oPos.y - v.pos.y;

		if (mFlipOnDateLine) {
			//wrap around date-line
			while (x < 0.5)
				x += 1.0;
			while (x > 0.5)
				x -= 1.0;
		}

		v.mvp.setTransScale((float) (x * tileScale),
		                    (float) (y * tileScale),
		                    (float) (v.pos.scale / oPos.scale)
		                            / MapRenderer.COORD_SCALE);

		v.mvp.multiplyLhs(project ? v.viewproj : v.view);
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition and applies
	 * view-projection-matrix.
	 */
	protected void setMatrix(GLViewport v) {
		setMatrix(v, true);
	}
}
