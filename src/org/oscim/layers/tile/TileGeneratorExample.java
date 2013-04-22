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
package org.oscim.layers.tile;

import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tile;
import org.oscim.graphics.Color;
import org.oscim.graphics.Paint.Cap;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.theme.renderinstruction.Line;


public class TileGeneratorExample implements ITileGenerator {
	GeometryBuffer mGeom = new GeometryBuffer(128, 16);
	Line mLineStyle = new Line(Color.BLUE, 2f, Cap.ROUND);

	@Override
	public boolean executeJob(MapTile tile) {
		LineLayer ll = tile.layers.getLineLayer(0);
		ll.line = mLineStyle;
		ll.width = 2;

		int m = 20;
		int s = Tile.SIZE - m * 2;
		GeometryBuffer g = mGeom;

		g.clear();
		g.startPolygon();
		g.addPoint(m, m);
		g.addPoint(m, s);
		g.addPoint(s, s);
		g.addPoint(s, m);

		ll.addLine(g);

		return true;
	}

	@Override
	public void cleanup() {
	}
}
