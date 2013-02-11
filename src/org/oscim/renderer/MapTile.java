/*
 * Copyright 2012, 2013 OpenScienceMap
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
package org.oscim.renderer;

import org.oscim.generator.JobTile;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.TextItem;

/**
 * Extends Tile class for concurrent use in TileManager,
 * TileGenerator and GLRenderer threads.
 *
 * @author Hannes Janetzek
 */
public final class MapTile extends JobTile {

	/**
	 * Tile data set by TileGenerator.
	 */
	public TextItem labels;
	public Layers layers;

	/**
	 * Tile is in view region. Set by GLRenderer.
	 */
	public boolean isVisible;

	/**
	 * VBO holds all vertex data to draw lines and polygons when
	 * 'layers' are compiled. layout:
	 * 16 bytes fill coordinates,
	 * n bytes polygon vertices,
	 * m bytes lines vertices
	 */
	BufferObject vbo;

	/**
	 * Pointer to access relatives in QuadTree
	 */
	public QuadTree rel;

	int lastDraw = 0;

	// keep track which tiles are locked as proxy for this tile
	public final static int PROXY_CHILD1 = 1 << 0;
	public final static int PROXY_CHILD2 = 1 << 1;
	public final static int PROXY_CHILD3 = 1 << 2;
	public final static int PROXY_CHILD4 = 1 << 3;
	public final static int PROXY_PARENT = 1 << 4;
	public final static int PROXY_GRAMPA = 1 << 5;
	public final static int PROXY_HOLDER = 1 << 6;

	public byte proxies;

	// check which labels were joined
	//	public final static int JOIN_T = 1 << 0;
	//	public final static int JOIN_B = 1 << 1;
	//	public final static int JOIN_L = 1 << 2;
	//	public final static int JOIN_R = 1 << 3;
	//	public final static int JOINED = 15;
	//	public byte joined;

	// counting the tiles that use this tile as proxy
	byte refs;

	// up to 255 Threads may lock a tile
	byte locked;

	// only used GLRenderer when this tile sits in for another tile.
	// e.g. x:-1,y:0,z:1 for x:1,y:0
	MapTile holder;

	MapTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
	}

	/**
	 * @return true if tile could be referenced by another thread
	 */
	boolean isLocked() {
		return locked > 0 || refs > 0;
	}

	void lock() {
		if (locked++ > 0)
			return;

		// lock all tiles that could serve as proxy
		MapTile p = rel.parent.tile;
		if (p != null && (p.state != 0)) {
			proxies |= PROXY_PARENT;
			p.refs++;
		}

		p = rel.parent.parent.tile;
		if (p != null && (p.state != 0)) {
			proxies |= PROXY_GRAMPA;
			p.refs++;
		}

		for (int j = 0; j < 4; j++) {
			if (rel.child[j] != null) {
				p = rel.child[j].tile;
				if (p != null && (p.state != 0)) {
					proxies |= (1 << j);
					p.refs++;
				}
			}
		}
	}

	void unlock() {
		if (--locked > 0 || proxies == 0)
			return;

		if ((proxies & PROXY_PARENT) != 0)
			rel.parent.tile.refs--;

		if ((proxies & PROXY_GRAMPA) != 0)
			rel.parent.parent.tile.refs--;

		for (int i = 0; i < 4; i++) {
			if ((proxies & (1 << i)) != 0)
				rel.child[i].tile.refs--;
		}
		proxies = 0;
	}
}
