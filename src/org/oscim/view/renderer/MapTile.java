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
package org.oscim.view.renderer;

import org.oscim.view.generator.JobTile;

class MapTile extends JobTile {

	/**
	 * VBO layout: - 16 bytes fill coordinates, n bytes polygon vertices, m
	 * bytes lines vertices
	 */
	VertexBufferObject vbo;

	/**
	 * polygonOffset in vbo is always 16 bytes,
	 */
	int lineOffset;

	TextTexture texture;

	/**
	 * Tile data set by TileGenerator:
	 */
	LineLayer lineLayers;
	PolygonLayer polygonLayers;
	TextItem labels;

	/**
	 * tile is used by render thread. set by updateVisibleList (main thread).
	 */
	boolean isActive;

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
	 * pointer to access relatives in TileTree
	 */
	QuadTree rel;

	byte lastDraw = 0;

	// keep track which tiles are locked as proxy for this tile
	final static int PROXY_PARENT = 16;
	final static int PROXY_GRAMPA = 32;
	// 1-8: children
	byte proxies;

	// counting the tiles that use this tile as proxy
	byte refs;

	boolean isLocked() {
		return isActive || refs > 0;
	}

	// void unref() {
	// if (refs == 0) {
	// Log.d("MapTile", "XXX already unrefd " + this);
	// return;
	// }
	// refs--;
	// }
	//
	// void ref() {
	// refs++;
	// }

	void lock() {

		isActive = true;

		if (isReady || newData)
			return;

		MapTile p = rel.parent.tile;

		if (p != null && (p.isReady || p.newData || p.isLoading)) {
			proxies |= PROXY_PARENT;
			p.refs++;
		} else {
			p = rel.parent.parent.tile;
			if (p != null && (p.isReady || p.newData || p.isLoading)) {
				proxies |= PROXY_GRAMPA;
				p.refs++;
			}
		}
		for (int j = 0; j < 4; j++) {
			if (rel.child[j] != null) {
				p = rel.child[j].tile;
				if (p != null && (p.isReady || p.newData || p.isLoading)) {
					proxies |= (1 << j);
					p.refs++;
				}
			}
		}
	}

	void unlock() {
		isActive = false;

		if (proxies == 0)
			return;

		if ((proxies & (1 << 4)) != 0) {
			MapTile p = rel.parent.tile;
			p.refs--;
		} else if ((proxies & (1 << 5)) != 0) {
			MapTile p = rel.parent.parent.tile;
			p.refs--;
		}

		for (int i = 0; i < 4; i++) {
			if ((proxies & (1 << i)) != 0) {
				rel.child[i].tile.refs--;
			}
		}
		proxies = 0;
	}

	MapTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
	}

}
