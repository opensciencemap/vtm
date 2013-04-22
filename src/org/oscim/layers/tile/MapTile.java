/*
 * Copyright 2012, 2013 Hannes Janetzek
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
package org.oscim.layers.tile;

import org.oscim.core.Tile;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.TextItem;
import org.oscim.utils.quadtree.QuadTree;

/**
 * Extends Tile class to hold state and data for concurrent use in
 * TileManager (Main Thread), TileLoader (Worker Threads) and
 * Rendering (GL Thread).
 */
public final class MapTile extends Tile {

	public final static int STATE_NONE = 0;

	/**
	 * STATE_LOADING means the tile is about to be loaded / loading.
	 * it belongs to TileLoader thread.
	 */
	public final static int STATE_LOADING = 1 << 0;

	/**
	 * STATE_NEW_DATA: tile data is prepared for rendering. While
	 * 'locked' it belongs to GL Thread.
	 */
	public final static int STATE_NEW_DATA = 1 << 1;

	/**
	 * STATE_READY: tile data is uploaded to GL.While
	 * 'locked' it belongs to GL Thread.
	 */
	public final static int STATE_READY = 1 << 2;

	/**
	 * TBD
	 */
	public final static int STATE_ERROR = 1 << 3;

	/**
	 * absolute tile coordinates: tileX,Y / Math.pow(2, zoomLevel)
	 */
	public double x;
	public double y;

	/** STATE_* */
	public byte state;

	/**
	 * distance from map center
	 */
	public float distance;

	/**
	 * Tile data set by TileLoader.
	 */
	public TextItem labels;
	public Layers layers;

	/**
	 * Tile is in view region. Set by GLRenderer.
	 */
	public boolean isVisible;

	/**
	 * Pointer to access relatives in QuadTree
	 */
	public QuadTree<MapTile> rel;

	/** to avoid drawing a tile twice per frame
	 * FIXME what if multiple layers use the same tile? */
	int lastDraw = 0;

	public final static int PROXY_CHILD1 = 1 << 0;
	public final static int PROXY_CHILD2 = 1 << 1;
	public final static int PROXY_CHILD3 = 1 << 2;
	public final static int PROXY_CHILD4 = 1 << 3;
	public final static int PROXY_PARENT = 1 << 4;
	public final static int PROXY_GRAMPA = 1 << 5;
	public final static int PROXY_HOLDER = 1 << 6;

	/** keep track which tiles are locked as proxy for this tile */
	public byte proxies;

	/** counting the tiles that use this tile as proxy */
	byte refs;

	/** up to 255 Threads may lock a tile */
	byte locked;

	// only used GLRenderer when this tile sits in for another tile.
	// e.g. x:-1,y:0,z:1 for x:1,y:0
	MapTile holder;

	MapTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
		this.x = (double) tileX / (1 << zoomLevel);
		this.y = (double) tileY / (1 << zoomLevel);
	}

	/**
	 * @return true when tile might be referenced by another thread.
	 */
	boolean isLocked() {
		return locked > 0 || refs > 0;
	}

	void lock() {
		if (locked++ > 0)
			return;

		// lock all tiles that could serve as proxy
		MapTile p = rel.parent.item;
		if (p != null && (p.state != 0)) {
			proxies |= PROXY_PARENT;
			p.refs++;
		}

		p = rel.parent.parent.item;
		if (p != null && (p.state != 0)) {
			proxies |= PROXY_GRAMPA;
			p.refs++;
		}

		for (int j = 0; j < 4; j++) {
			if ((p = rel.get(j)) == null || p.state == 0)
				continue;

			proxies |= (1 << j);
			p.refs++;
		}
	}

	void unlock() {
		if (--locked > 0 || proxies == 0)
			return;

		if ((proxies & PROXY_PARENT) != 0)
			rel.parent.item.refs--;

		if ((proxies & PROXY_GRAMPA) != 0)
			rel.parent.parent.item.refs--;

		for (int i = 0; i < 4; i++) {
			if ((proxies & (1 << i)) != 0)
				rel.get(i).refs--;
		}
		proxies = 0;
	}

	public void addLabel(TextItem t) {
		t.next = labels;
		labels = t;
	}

	public void clearState() {
		state = STATE_NONE;
	}

	/**
	 * @return true if tile is loading, has new data or is ready for rendering
	 */
	public boolean isActive() {
		return state > STATE_NONE && state < STATE_ERROR;
	}

	public void setLoading() {
		//if (state != STATE_NONE)
		//Log.d(TAG, "wrong state: " + state);

		state = STATE_LOADING;
	}

}
