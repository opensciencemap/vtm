/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.glrenderer;

import org.mapsforge.android.mapgenerator.MapTile;
import org.mapsforge.core.Tile;

class GLMapTile extends MapTile {

	VertexBufferObject lineVBO;
	VertexBufferObject polygonVBO;

	LineLayers lineLayers;
	PolygonLayers polygonLayers;
	// MeshLayers meshLayers;

	boolean newData;
	boolean loading;

	// pixel coordinates (y-flipped)
	final long x;
	final long y;

	// scissor coordinates
	int sx, sy, sw, sh;

	final GLMapTile[] child = { null, null, null, null };
	GLMapTile parent;

	GLMapTile(long tileX, long tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);

		x = pixelX;
		y = pixelY + Tile.TILE_SIZE;
	}

}
