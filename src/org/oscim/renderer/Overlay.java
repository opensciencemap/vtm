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

import java.io.IOException;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.SymbolItem;
import org.oscim.renderer.layer.SymbolLayer;
import org.oscim.theme.renderinstruction.BitmapUtils;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.opengl.GLES20;
import android.opengl.Matrix;

public class Overlay {

	BufferObject vbo;
	Layers layers;
	TextItem labels;
	TextTexture texture;
	// flag to set when data is ready for (re)compilation.
	boolean newData;
	boolean isReady;
	MapPosition mMapPosition;

	float drawScale;

	private boolean first = true;

	Overlay() {
		mMapPosition = new MapPosition();

		layers = new Layers();

		LineLayer ll = (LineLayer) layers.getLayer(1, Layer.LINE);
		ll.line = new Line(Color.BLUE, 1.0f, Cap.BUTT);
		ll.width = 2;
		float[] points = { -100, -100, 100, -100, 100, 100, -100, 100, -100,
				-100 };
		short[] index = { (short) points.length };
		ll.addLine(points, index, false);
		//
		// PolygonLayer pl = (PolygonLayer) layers.getLayer(0, Layer.POLYGON);
		// pl.area = new Area(Color.argb(128, 255, 0, 0));
		//
		// float[] ppoints = {
		// 0, 256,
		// 0, 0,
		// 256, 0,
		// 256, 256,
		// };
		// short[] pindex = { (short) ppoints.length };
		// pl.addPolygon(ppoints, pindex);

		SymbolLayer sl = new SymbolLayer();
		SymbolItem it = new SymbolItem();

		it.x = 0;
		it.y = 0;
		// billboard always faces camera
		it.billboard = true;

		try {
			it.bitmap = BitmapUtils.createBitmap("file:/sdcard/cheshire.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sl.addSymbol(it);

		SymbolItem it2 = new SymbolItem();
		it2.bitmap = it.bitmap;
		it2.x = 0;
		it2.y = 0;
		// billboard always faces camera
		it2.billboard = false;

		sl.addSymbol(it2);

		layers.symbolLayers = sl;
	}

	synchronized boolean onTouch() {

		return true;
	}

	// /////////////// called from GLRender Thread ////////////////////////
	// use synchronized (this){} when updating 'layers' from another thread

	synchronized void update(MapView mapView) {
		// keep position constant (or update layer relative to new position)
		// mapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		if (first) {
			// fix at initial position
			mapView.getMapViewPosition().getMapPosition(mMapPosition, null);
			first = false;

			// pass layers to be uploaded and drawn to GL Thread
			// afterwards never modify 'layers' outside of this function!
			newData = true;
		}
	}

	float[] mvp = new float[16];

	synchronized void render(MapPosition pos, float[] mv, float[] proj) {
		float div = 1;

		setMatrix(pos, mv);

		Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);

		for (Layer l = layers.layers; l != null;) {
			if (l.type == Layer.POLYGON) {
				GLES20.glDisable(GLES20.GL_BLEND);
				l = PolygonRenderer.draw(pos, l, mvp, true, false);
			} else {
				GLES20.glEnable(GLES20.GL_BLEND);
				l = LineRenderer.draw(pos, l, mvp, div, 0, layers.lineOffset);
			}
		}

		for (Layer l = layers.symbolLayers; l != null;) {
			l = TextureRenderer.draw(l, 1, proj, mv, layers.symbolOffset);
		}
	}

	private void setMatrix(MapPosition mPos, float[] matrix) {

		MapPosition oPos = mMapPosition;
		float div = 1;
		byte z = oPos.zoomLevel;
		int diff = mPos.zoomLevel - z;

		if (diff < 0)
			div = (1 << -diff);
		else if (diff > 0)
			div = (1.0f / (1 << diff));

		float x = (float) (oPos.x - mPos.x * div);
		float y = (float) (oPos.y - mPos.y * div);

		// flip around date-line
		float max = (Tile.TILE_SIZE << z);
		if (x < -max / 2)
			x = max + x;
		else if (x > max / 2)
			x = x - max;

		float scale = mPos.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;

		scale = (mPos.scale / oPos.scale) / div;
		// scale to tile to world coordinates
		scale /= GLRenderer.COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;

		Matrix.multiplyMM(matrix, 0, mPos.viewMatrix, 0, matrix, 0);
	}
}
