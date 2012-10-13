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
package org.oscim.renderer;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.utils.FastMath;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public abstract class Overlay {

	protected final MapView mMapView;
	protected MapPosition mMapPosition;
	protected final Layers layers;

	// flag to set when data is ready for (re)compilation.
	protected boolean newData;

	// flag set by GLRenderer when data is compiled
	protected boolean isReady;

	protected BufferObject vbo;

	Overlay(MapView mapView) {
		mMapView = mapView;
		mMapPosition = new MapPosition();
		layers = new Layers();
	}

	synchronized boolean onTouch(boolean down) {
		Log.d("...", "Overlay handle onTouch " + down);
		return true;
	}

	/**
	 * update mMapPosition
	 * 
	 * @return true if position has changed
	 */
	protected boolean updateMapPosition() {
		return mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);
	}

	// /////////////// called from GLRender Thread ////////////////////////
	// use synchronized (this){} when updating 'layers' from another thread

	/**
	 * @param positionChanged
	 *            true when MapPosition has changed
	 * @param tilesChanged
	 *            true when loaded tiles changed
	 */
	synchronized void update(boolean positionChanged, boolean tilesChanged) {
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

	float[] mvp = new float[16];

	synchronized void render(MapPosition pos, float[] mv, float[] proj) {
		float div = setMatrix(pos, mv);

		Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);

		for (Layer l = layers.layers; l != null;) {
			if (l.type == Layer.POLYGON) {
				GLES20.glDisable(GLES20.GL_BLEND);
				l = PolygonRenderer.draw(pos, l, mvp, true, false);
			} else {
				GLES20.glEnable(GLES20.GL_BLEND);
				l = LineRenderer.draw(pos, l, mvp, 1 / div, 0, layers.lineOffset);
			}
		}

		// float scale = curPos.scale / div;

		for (Layer l = layers.textureLayers; l != null;) {
			l = TextureRenderer.draw(l, (mMapPosition.scale / pos.scale) * div, proj, mv,
					layers.texOffset);
		}
	}

	private float setMatrix(MapPosition curPos, float[] matrix) {

		MapPosition oPos = mMapPosition;

		byte z = oPos.zoomLevel;
		// int diff = curPos.zoomLevel - z;
		float div = FastMath.pow(z - curPos.zoomLevel);
		// if (diff < 0)
		// div = (1 << -diff);
		// else if (diff > 0)
		// div = (1.0f / (1 << diff));

		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		// flip around date-line
		float max = (Tile.TILE_SIZE << z);
		if (x < -max / 2)
			x = max + x;
		else if (x > max / 2)
			x = x - max;

		float scale = curPos.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;

		scale = (curPos.scale / oPos.scale) / div;
		// scale to tile to world coordinates
		scale /= GLRenderer.COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;

		Matrix.multiplyMM(matrix, 0, curPos.viewMatrix, 0, matrix, 0);

		return div;
	}
}
