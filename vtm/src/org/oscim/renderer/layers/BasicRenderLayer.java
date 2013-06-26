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
package org.oscim.renderer.layers;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.RenderLayer;
import org.oscim.renderer.sublayers.BitmapRenderer;
import org.oscim.renderer.sublayers.Layer;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.renderer.sublayers.LineRenderer;
import org.oscim.renderer.sublayers.LineTexRenderer;
import org.oscim.renderer.sublayers.PolygonRenderer;
import org.oscim.renderer.sublayers.TextureRenderer;
import org.oscim.utils.FastMath;
import org.oscim.view.MapView;

/**
 * Base class to use the renderer.sublayers for drawing
 */
public abstract class BasicRenderLayer extends RenderLayer {

	private static final GL20 GL = GLAdapter.get();

	public final Layers layers;

	public BasicRenderLayer(MapView mapView) {
		super(mapView);
		layers = new Layers();
	}

	/**
	 * Render all 'layers'
	 */
	@Override
	public synchronized void render(MapPosition curPos, Matrices m) {
		MapPosition pos = mMapPosition;

		float div = FastMath.pow(pos.zoomLevel - curPos.zoomLevel);

		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, layers.vbo.id);
		GLState.test(false, false);
		GLState.blend(true);
		int simple = (curPos.tilt < 1 ? 1 : 0);

		if (layers.baseLayers != null) {
			setMatrix(curPos, m, true);

			for (Layer l = layers.baseLayers; l != null;) {
				switch (l.type) {
					case Layer.POLYGON:
						l = PolygonRenderer.draw(curPos, l, m, true, 1, false);
						break;

					case Layer.LINE:
						l = LineRenderer.draw(layers, l, curPos, m, div, simple);
						break;

					case Layer.TEXLINE:
						l = LineTexRenderer.draw(layers, l, curPos, m, div);
						break;
				}
			}
		}

		if (layers.textureLayers != null) {
			setMatrix(curPos, m, false);

			float scale = (float) (pos.scale / curPos.scale);

			for (Layer l = layers.textureLayers; l != null;) {
				switch (l.type) {
					case Layer.BITMAP:
						l = BitmapRenderer.draw(l, 1, m);
						break;

//					case Layer.SYMBOL:
//						l = BitmapRenderer.draw(l, 1, m);
//						break;

					default:
						l = TextureRenderer.draw(l, scale, m);
				}
			}
		}
	}

	@Override
	public void compile() {
		int newSize = layers.getSize();
		if (newSize <= 0) {
			BufferObject.release(layers.vbo);
			layers.vbo = null;
			isReady = false;
			return;
		}

		if (layers.vbo == null) {
			layers.vbo = BufferObject.get(newSize);

			if (layers.vbo == null)
				return;
		}

		if (GLRenderer.uploadLayers(layers, newSize, true))
			isReady = true;
	}
}
