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

import java.io.IOException;

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.SymbolItem;
import org.oscim.renderer.layer.SymbolLayer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.theme.renderinstruction.BitmapUtils;
import org.oscim.view.MapView;

public class TestOverlay extends BasicOverlay {

	TextItem labels;

	float drawScale;

	private boolean first = true;

	public TestOverlay(MapView mapView) {
		super(mapView);

		//		LineLayer ll = (LineLayer) layers.getLayer(1, Layer.LINE);
		//		ll.line = new Line(Color.BLUE, 1.0f, Cap.BUTT);
		//		ll.width = 2;
		//		float[] points = { -100, -100, 100, -100, 100, 100, -100, 100, -100, -100 };
		//		short[] index = { (short) points.length };
		//		ll.addLine(points, index, false);

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
		sl.fixed = false;

		layers.textureLayers = sl;

		// TextLayer tl = new TextLayer();
		// Text t = Text.createText(20, 2, Color.WHITE, Color.BLACK, false);
		// TextItem ti = new TextItem(0, 0, "check one, check two", t);
		// ti.x1 = 0;
		// ti.y1 = 0;
		// ti.x2 = (short) Tile.TILE_SIZE;
		// ti.y2 = (short) Tile.TILE_SIZE;
		//
		// tl.addText(ti);
		//
		// layers.textureLayers = tl;
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {
		// keep position constant (or update layer relative to new position)
		//mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		if (first) {
			// fix at initial position
			updateMapPosition();

			first = false;
			((SymbolLayer) (layers.textureLayers)).prepare();

			// pass layers to be uploaded and drawn to GL Thread
			// afterwards never modify 'layers' outside of this function!
			newData = true;
		}
	}

}
