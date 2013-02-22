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
package org.oscim.renderer.overlays;

import org.oscim.core.MapPosition;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.LineRenderer;
import org.oscim.renderer.LineTexRenderer;
import org.oscim.renderer.PolygonRenderer;
import org.oscim.renderer.TextureRenderer;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.utils.FastMath;
import org.oscim.view.MapView;

import android.opengl.GLES20;

// Base class to use the Layers drawing 'API'
public abstract class BasicOverlay extends RenderOverlay {

	public final Layers layers;

	protected float[] mvp = new float[16];

	public BasicOverlay(MapView mapView) {
		super(mapView);
		layers = new Layers();
	}

	/**
	 * use synchronized when modifying layers
	 */
	@Override
	public synchronized void render(MapPosition pos, Matrices m) {

		float div = FastMath.pow(mMapPosition.zoomLevel - pos.zoomLevel);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, layers.vbo.id);
		GLState.test(false, false);

		if (layers.baseLayers != null) {
			setMatrix(pos, m, true);

			for (Layer l = layers.baseLayers; l != null;) {
				switch (l.type) {
					case Layer.POLYGON:
						l = PolygonRenderer.draw(pos, l, m.mvp, true, false);
						break;
					case Layer.LINE:
						l = LineRenderer.draw(layers, l, pos, m.mvp, div, 0);
						break;
					case Layer.TEXLINE:
						l = LineTexRenderer.draw(layers, l, pos, m.mvp, div);
						break;
				}
			}
		}

		if (layers.textureLayers != null) {
			setMatrix(pos, m, false);

			float scale = (mMapPosition.scale / pos.scale) * div;

			for (Layer l = layers.textureLayers; l != null;) {
				l = TextureRenderer.draw(l, scale, m.proj, m.mvp);
			}
		}
	}

	@Override
	public void compile() {
		int newSize = layers.getSize();
		if (newSize == 0) {
			BufferObject.release(layers.vbo);
			layers.vbo = null;
			isReady = false;
			return;
		}

		if (layers.vbo == null) {
			layers.vbo = BufferObject.get(0);

			if (layers.vbo == null)
				return;
		}

		if (newSize > 0) {
			if (GLRenderer.uploadLayers(layers, newSize, true))
				isReady = true;
		}
	}
}
