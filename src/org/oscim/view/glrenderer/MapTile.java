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
package org.oscim.view.glrenderer;

import org.oscim.view.mapgenerator.JobTile;

class MapTile extends JobTile {
	byte lastDraw = 0;

	/**
	 *  VBO layout:
	 * - 16 bytes fill coordinates
	 * - n bytes polygon vertices
	 * - m bytes lines vertices
	 */
	VertexBufferObject vbo;

	/**
	 *  polygonOffset in vbo is always 16 bytes,
	 */
	int lineOffset;

	TextTexture texture;

	/**
	 *  Tile data set by MapGenerator:
	 */
	LineLayer lineLayers;
	PolygonLayer polygonLayers;
	TextItem labels;

	/**
	 * tile has new data to upload to gl
	 */
	boolean newData;

	/**
	 * tile is loaded and ready for drawing.
	 */
	boolean isReady;

	/**
	 * tile is in view region.
	 */
	boolean isVisible;

	/**
	 * tile is used by render thread. set by updateVisibleList (main thread).
	 */
	boolean isActive;

	/**
	 *  pointer to access relatives in TileTree
	 */
	QuadTree rel;

	MapTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
	}

}
