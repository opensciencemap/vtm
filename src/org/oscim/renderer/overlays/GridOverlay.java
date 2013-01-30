/*
 * Copyright 2012 Hannes Janetzek
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
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.renderer.layer.TextLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.view.MapView;

import android.graphics.Color;
import android.graphics.Paint.Cap;
import android.util.Log;

public class GridOverlay extends BasicOverlay {

	private float[] mPoints;
	private short[] mIndex;
	private Text mText;

	public GridOverlay(MapView mapView) {
		super(mapView);

		int size = Tile.TILE_SIZE;
		float[] points = new float[64];
		short[] index = new short[16];

		float pos = -size * 4;

		// vertical lines
		for (int i = 0; i < 8; i++) {
			index[i] = 4;
			// x1,y1,x2,y2
			points[i * 4] = pos + i * size;
			points[i * 4 + 1] = pos + 0;
			points[i * 4 + 2] = pos + i * size;
			points[i * 4 + 3] = pos + size * 8;
		}

		// horizontal lines
		for (int j = 8; j < 16; j++) {
			index[j] = 4;
			points[j * 4] = pos + 0;
			points[j * 4 + 1] = pos + (j - 8) * size;
			points[j * 4 + 2] = pos + size * 8;
			points[j * 4 + 3] = pos + (j - 8) * size;
		}

		mIndex = index;
		mPoints = points;

		// mText = Text.createText(20, 3, Color.BLACK, Color.RED, false);
		mText = Text.createText(22, 0, Color.RED, 0, false);
		// mText = Text.createText(22, 0, Color.RED, 0, true);
	}

	private void addLabels(int x, int y, int z) {
		int size = Tile.TILE_SIZE;

		TextLayer tl = new TextLayer();

		for (int i = -2; i < 2; i++) {
			for (int j = -2; j < 2; j++) {
				TextItem ti = TextItem.get().set(size * j + size / 2, size * i + size / 2,
						(x + j) + " / " + (y + i) + " / " + z, mText);

				// TextItem ti = new TextItem(size * j + size / 2, size * i +
				// size / 2,
				// (x + j) + " / " + (y + i) + " / " + z, mText);

				// rotation, TODO could also be used for slide range
				ti.x1 = 0;
				ti.y1 = 1; // (short) (size / 2);
				ti.x2 = 1; // (short) size;
				ti.y2 = 1; // (short) (size / 2);
				tl.addText(ti);
			}
		}
		tl.prepare();

		layers.textureLayers = tl;
	}

	private int mCurX = -1;
	private int mCurY = -1;
	private byte mCurZ = -1;

	private boolean finished;

	void timerFinished() {
		Log.d("...", "timer finish!");
		finished = true;
		mMapView.redrawMap();
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		updateMapPosition();

		// fix map position to tile coordinates
		float size = Tile.TILE_SIZE;
		int x = (int) (mMapPosition.x / size);
		int y = (int) (mMapPosition.y / size);
		mMapPosition.x = x * size;
		mMapPosition.y = y * size;

		if (!finished)
			mMapPosition.scale = 1;

		// update layers when map moved by at least one tile
		if (x != mCurX || y != mCurY || mMapPosition.zoomLevel != mCurZ) {
			mCurX = x;
			mCurY = y;
			mCurZ = mMapPosition.zoomLevel;

			layers.clear();

			LineLayer ll = (LineLayer) layers.getLayer(1, Layer.LINE);
			ll.line = new Line(Color.BLUE, 1.0f, Cap.BUTT);
			ll.width = 1.5f;
			ll.addLine(mPoints, mIndex, false);

			Log.d("...", "update labels");

			addLabels(x, y, mCurZ);

			newData = true;
			finished = false;
		}
	}
}
