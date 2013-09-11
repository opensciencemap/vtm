/*
 * Copyright 2013 Hannes Janetzek
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

import org.oscim.backend.GL20;
import org.oscim.core.MapPosition;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.RenderElement;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.renderer.elements.TextureLayer;
import org.oscim.utils.FastMath;

/**
 * Base class to use the renderer.sublayers for drawing
 */
public abstract class ElementRenderer extends LayerRenderer {

	public final ElementLayers layers;

	public ElementRenderer() {
		layers = new ElementLayers();
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

	@Override
	protected void compile() {
		int newSize = layers.getSize();
		if (newSize <= 0) {
			BufferObject.release(layers.vbo);
			layers.vbo = null;
			setReady(false);
			return;
		}

		if (layers.vbo == null)
			layers.vbo = BufferObject.get(GL20.GL_ARRAY_BUFFER, newSize);

		if (MapRenderer.uploadLayers(layers, newSize, true))
			setReady(true);
	}
}
