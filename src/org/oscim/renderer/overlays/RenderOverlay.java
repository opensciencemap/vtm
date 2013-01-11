/*
 * Copyright 2012, Hannes Janetzek
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
import org.oscim.core.Tile;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.LineRenderer;
import org.oscim.renderer.PolygonRenderer;
import org.oscim.renderer.TextureRenderer;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.opengl.Matrix;

public abstract class RenderOverlay {

	protected final MapView mMapView;
	// keep the Position for which the Overlay is rendered
	protected MapPosition mMapPosition;

	// current Layers to draw
	public final Layers layers;

	// flag to set when data is ready for (re)compilation.
	public boolean newData;

	// flag set by GLRenderer when data is compiled
	public boolean isReady;

	public BufferObject vbo;

	protected float[] mvp = new float[16];

	public RenderOverlay(MapView mapView) {
		mMapView = mapView;
		mMapPosition = new MapPosition();
		layers = new Layers();
	}

	/**
	 * Utility: update mMapPosition
	 * @return true if position has changed
	 */
	protected boolean updateMapPosition() {
		return mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);
	}

	// /////////////// called from GLRender Thread ////////////////////////
	// use synchronized (this){} when updating 'layers' from another thread

	/**
	 * @param curPos TODO
	 * @param positionChanged
	 *            true when MapPosition has changed
	 * @param tilesChanged
	 *            true when current tiles changed
	 */
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {
		// // keep position constant (or update layer relative to new position)
		// mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);
		//
		// if (first) {
		// // fix at initial position
		// // mapView.getMapViewPosition().getMapPosition(mMapPosition, null);
		// first = false;
		//
		// // pass layers to be uploaded and drawn to GL Thread
		// // afterwards never modify 'layers' outside of this function!
		// newData = true;
		// }
	}

	/**
	 * Default overlay render function
	 * @param pos
	 *            current MapPosition
	 * @param mv
	 *            current model-view matrix
	 * @param proj
	 *            current projection matrix
	 */
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {
		setMatrix(pos, mv);
		float div = FastMath.pow(mMapPosition.zoomLevel - pos.zoomLevel);

		Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);
		GLState.test(false, false);

		for (Layer l = layers.layers; l != null;) {
			if (l.type == Layer.POLYGON) {
				GLES20.glDisable(GLES20.GL_BLEND);
				l = PolygonRenderer.draw(pos, l, mvp, true, false);
			} else {
				GLES20.glEnable(GLES20.GL_BLEND);
				l = LineRenderer.draw(pos, l, mvp, 1 / div, 0, layers.lineOffset);
			}
		}

		for (Layer l = layers.textureLayers; l != null;) {
			l = TextureRenderer.draw(l, (mMapPosition.scale / pos.scale) * div, proj, mv);
		}
	}

	/**
	 * Utility: set matrix to scale relative to zoomlevel
	 * @param curPos ...
	 * @param matrix ...
	 */
	protected void setMatrix(MapPosition curPos, float[] matrix) {
		MapPosition oPos = mMapPosition;

		float div = FastMath.pow(oPos.zoomLevel - curPos.zoomLevel);

		// translate relative to map center
		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		// flip around date-line
		// FIXME not sure if this is correct!
		float max = (Tile.TILE_SIZE << oPos.zoomLevel);
		if (x < -max / 2)
			x = max + x;
		else if (x > max / 2)
			x = x - max;

		// scale to current tile world coordinates
		float scale = curPos.scale / div;

		// set scale to be relative to current scale
		float s = (curPos.scale / oPos.scale) / div;

		GlUtils.setMatrix(matrix, x * scale, y * scale,
				s / GLRenderer.COORD_MULTIPLIER);

		Matrix.multiplyMM(matrix, 0, curPos.viewMatrix, 0, matrix, 0);
	}
}
