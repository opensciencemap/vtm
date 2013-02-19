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

	public BufferObject vbo;

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

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);
		GLState.test(false, false);

		if (layers.layers != null) {
			setMatrix(pos, m, true);

			for (Layer l = layers.layers; l != null;) {
				if (l.type == Layer.POLYGON) {
					l = PolygonRenderer.draw(pos, l, m.mvp, true, false);
				} else {
					l = LineRenderer.draw(pos, l, m.mvp, div, 0, layers.lineOffset);
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
			BufferObject.release(vbo);
			vbo = null;
			isReady = false;
			return;
		}

		if (vbo == null) {
			vbo = BufferObject.get(0);

			if (vbo == null)
				return;
		}

		if (newSize > 0) {
			if (GLRenderer.uploadLayers(layers, vbo, newSize, true))
				isReady = true;
		}
	}
}
