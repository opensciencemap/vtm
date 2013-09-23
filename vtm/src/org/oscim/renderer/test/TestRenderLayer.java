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
package org.oscim.renderer.test;

import org.oscim.core.MapPosition;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.TextItem;

public class TestRenderLayer extends ElementRenderer {

	TextItem labels;

	float drawScale;

	private boolean first = true;

	public TestRenderLayer() {

		// draw a rectangle
		//LineLayer ll = (LineLayer) layers.getLayer(1, RenderElement.LINE);
		//ll.line = new Line(Color.BLUE, 1.0f, Cap.BUTT);
		//ll.width = 2;
		//		float[] points = {
		//				-100, -100,
		//				100, -100,
		//				100, 100,
		//				-100, 100,
		//				-100, -100
		//				};
		//short[] index = { (short) points.length };
		//ll.addLine(points, index, true);

		//		LineTexLayer lt = layers.getLineTexLayer(2);
		//		lt.line = new Line(Color.BLUE, 1.0f, 8);
		//		lt.width = 8;
		//		lt.addLine(points, null);
		//
		//		float[] points2 = {
		//				-200, -200,
		//				200, -200,
		//				200, 200,
		//				-200, 200,
		//				-200, -200
		//				};
		//		lt = layers.getLineTexLayer(3);
		//		lt.line = new Line(Color.BLUE, 1.0f, 16);
		//		lt.width = 8;
		//		lt.addLine(points2, null);

		//
		// PolygonLayer pl = (PolygonLayer) layers.getLayer(0, RenderElement.POLYGON);
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

		//SymbolLayer sl = new SymbolLayer();
		//SymbolItem it = new SymbolItem();
		//
		//it.x = 0;
		//it.y = 0;
		//// billboard always faces camera
		//it.billboard = true;
		//
		//try {
		//	it.bitmap = BitmapUtils.createBitmap("file:/sdcard/cheshire.png");
		//} catch (IOException e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
		//sl.addSymbol(it);
		//
		//SymbolItem it2 = new SymbolItem();
		//it2.bitmap = it.bitmap;
		//it2.x = 0;
		//it2.y = 0;
		//// billboard always faces camera
		//it2.billboard = false;
		//
		//sl.addSymbol(it2);
		//sl.fixed = false;
		//
		//layers.textureLayers = sl;

		// TextLayer tl = new TextLayer();
		// Text t = Text.createText(20, 2, Color.WHITE, Color.BLACK, false);
		// TextItem ti = new TextItem(0, 0, "check one, check two", t);
		// ti.x1 = 0;
		// ti.y1 = 0;
		// ti.x2 = (short) Tile.SIZE;
		// ti.y2 = (short) Tile.SIZE;
		//
		// tl.addText(ti);
		//
		// layers.textureLayers = tl;
	}

	@Override
	protected synchronized void update(MapPosition curPos, boolean positionChanged,
	        Matrices matrices) {
		// keep position constant (or update layer relative to new position)
		//mMapPosition.copy(curPos);

		if (first) {
			// fix at initial position
			mMapPosition.copy(curPos);

			first = false;
			//((SymbolLayer) (layers.textureLayers)).prepare();

			// pass layers to be uploaded and drawn to GL Thread
			// afterwards never modify 'layers' outside of this function!
			compile();
		}
	}

}
