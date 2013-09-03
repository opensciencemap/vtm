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
package org.oscim.layers.tile.test;

import org.oscim.view.Map;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.test.TestTileLayer.TestTileLoader;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.renderer.sublayers.LineLayer;
import org.oscim.theme.renderinstruction.Line;

import org.oscim.backend.Log;

public class TestTileLayer extends TileLayer<TestTileLoader> {
	final static String TAG = TestTileLayer.class.getName();

	public TestTileLayer(Map map) {
		super(map);
	}

	@Override
	protected TestTileLoader createLoader(TileManager tm) {
		return new TestTileLoader(tm);
	}

	static class TestTileLoader extends TileLoader {
		public TestTileLoader(TileManager tileManager) {
			super(tileManager);
		}

		GeometryBuffer mGeom = new GeometryBuffer(128, 16);
		Line mLineStyle = new Line(Color.BLUE, 2f, Cap.ROUND);

		@Override
		public boolean executeJob(MapTile tile) {
			Log.d(TAG, "load tile " + tile);
			tile.layers = new Layers();

			LineLayer ll = tile.layers.getLineLayer(0);
			ll.line = mLineStyle;
			ll.width = 2;

			int m = 20;
			int s = Tile.SIZE - m * 2;
			GeometryBuffer g = mGeom;

			g.clear();
			g.startLine();
			g.addPoint(m, m);
			g.addPoint(m, s);
			g.addPoint(s, s);
			g.addPoint(s, m);
			g.addPoint(m, m);

			ll.addLine(g);

			return true;
		}

		@Override
		public void cleanup() {
		}
	}
}
