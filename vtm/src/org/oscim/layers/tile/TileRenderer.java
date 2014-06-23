/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.layers.tile.MapTile.State.READY;

import org.oscim.backend.GL20;
import org.oscim.core.MapPosition;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.utils.ScanBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TileRenderer extends LayerRenderer {
	static final Logger log = LoggerFactory.getLogger(TileRenderer.class);

	/** fade-in time */
	protected static final float FADE_TIME = 500;
	protected static final int MAX_TILE_LOAD = 1;

	private TileManager mTileManager;

	protected final TileSet mDrawTiles;
	protected int mProxyTileCnt;

	private int mOverdraw = 0;
	private float mAlpha = 1;

	protected int mRenderOverdraw;
	protected float mRenderAlpha;

	private int mUploadSerial;

	public TileRenderer() {
		mUploadSerial = 0;
		mDrawTiles = new TileSet();
	}

	protected void setTileManager(TileManager tileManager) {
		mTileManager = tileManager;
	}

	/**
	 * Current number of frames drawn, used to not draw a
	 * tile twice per frame.
	 */
	protected int mDrawSerial;

	/**
	 * Threadsafe
	 */
	public synchronized void setOverdrawColor(int color) {
		mOverdraw = color;
	}

	/**
	 * Threadsafe
	 */
	public synchronized void setBitmapAlpha(float alpha) {
		mAlpha = alpha;
	}

	/**
	 * synced with clearTiles, setOverdrawColor and setBitmapAlpha
	 */
	@Override
	protected synchronized void update(GLViewport v) {

		if (mAlpha == 0) {
			mTileManager.releaseTiles(mDrawTiles);
			return;
		}

		/* get current tiles to draw */
		boolean tilesChanged;
		synchronized (tilelock) {
			tilesChanged = mTileManager.getActiveTiles(mDrawTiles);
		}

		if (mDrawTiles.cnt == 0)
			return;

		/* keep constant while rendering frame */
		mRenderAlpha = mAlpha;
		mRenderOverdraw = mOverdraw;

		int tileCnt = mDrawTiles.cnt;
		MapTile[] tiles = mDrawTiles.tiles;

		if (tilesChanged || v.changed()) {
			updateTileVisibility(v.pos, v.plane);
		}

		tileCnt += mProxyTileCnt;

		/* prepare tiles for rendering */
		if (compileTileLayers(tiles, tileCnt) > 0) {
			mUploadSerial++;
			BufferObject.checkBufferUsage(false);
		}

		mDrawSerial++;
	}

	@Override
	protected void render(GLViewport v) {
		/* render in update() so that tiles cannot vanish in between. */
	}

	public void clearTiles() {
		/* Clear all references to MapTiles as all current
		 * tiles will also be removed from TileManager. */
		//mDrawTiles = new TileSet();
		mDrawTiles.tiles = new MapTile[1];
		mDrawTiles.cnt = 0;
	}

	/** compile tile layer data and upload to VBOs */
	private static int compileTileLayers(MapTile[] tiles, int tileCnt) {
		int uploadCnt = 0;

		for (int i = 0; i < tileCnt; i++) {
			MapTile tile = tiles[i];

			if (!tile.isVisible)
				continue;

			if (tile.state == READY)
				continue;

			if (tile.state == NEW_DATA) {
				uploadCnt += uploadTileData(tile);
				continue;
			}

			if (tile.holder != null) {
				/* load tile that is referenced by this holder */
				if (tile.holder.state == NEW_DATA)
					uploadCnt += uploadTileData(tile.holder);

				tile.state = tile.holder.state;
				continue;
			}

			/* check near relatives than can serve as proxy */
			if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
				MapTile t = tile.node.parent.item;
				if (t.state == NEW_DATA)
					uploadCnt += uploadTileData(t);

				/* dont load child proxies */
				continue;
			}

			for (int c = 0; c < 4; c++) {
				if ((tile.proxies & 1 << c) == 0)
					continue;

				MapTile t = tile.node.child(i);
				if (t != null && t.state == NEW_DATA)
					uploadCnt += uploadTileData(t);
			}
			if (uploadCnt >= MAX_TILE_LOAD) {
				break;
			}
		}
		return uploadCnt;
	}

	private static int uploadTileData(MapTile tile) {
		tile.state = READY;
		ElementLayers layers = tile.getLayers();

		/* tile might contain extrusion or label layers */
		if (layers == null)
			return 1;

		int newSize = layers.getSize();
		if (newSize <= 0)
			return 1;

		if (layers.vbo == null)
			layers.vbo = BufferObject.get(GL20.GL_ARRAY_BUFFER, newSize);

		if (!ElementRenderer.uploadLayers(layers, newSize, true)) {
			log.error("{} uploadTileData failed!", tile);
			layers.vbo = BufferObject.release(layers.vbo);
			layers.clear();
			/* throw Exception? */
			//FIXME tile.layers = null;
			return 0;
		}

		return 1;
	}

	private final Object tilelock = new Object();

	/** set tile isVisible flag true for tiles that intersect view */
	private void updateTileVisibility(MapPosition pos, float[] box) {

		/* lock tiles while updating isVisible state */
		synchronized (tilelock) {
			MapTile[] tiles = mDrawTiles.tiles;

			int tileZoom = tiles[0].zoomLevel;

			for (int i = 0; i < mDrawTiles.cnt; i++)
				tiles[i].isVisible = false;

			if (tileZoom > pos.zoomLevel + 2 || tileZoom < pos.zoomLevel - 4) {
				//log.debug("skip: zoomlevel diff " + (tileZoom - pos.zoomLevel));
				return;
			}
			/* count placeholder tiles */
			mProxyTileCnt = 0;

			/* check visibile tiles */
			mScanBox.scan(pos.x, pos.y, pos.scale, tileZoom, box);
		}
	}

	/**
	 * Update tileSet with currently visible tiles get a TileSet of currently
	 * visible tiles
	 */
	public boolean getVisibleTiles(TileSet tileSet) {
		if (tileSet == null)
			return false;

		if (mDrawTiles == null) {
			releaseTiles(tileSet);
			return false;
		}

		int prevSerial = tileSet.serial;

		/* ensure tiles keep visible state */
		synchronized (tilelock) {

			MapTile[] newTiles = mDrawTiles.tiles;
			int cnt = mDrawTiles.cnt;

			/* unlock previous tiles */
			tileSet.releaseTiles();

			/* ensure same size */
			if (tileSet.tiles.length != newTiles.length) {
				tileSet.tiles = new MapTile[newTiles.length];
			}

			/* lock tiles to not be removed from cache */
			tileSet.cnt = 0;
			for (int i = 0; i < cnt; i++) {
				MapTile t = newTiles[i];
				if (t.isVisible && t.state == READY) {
					t.lock();
					tileSet.tiles[tileSet.cnt++] = t;
				}
			}

			tileSet.serial = mUploadSerial;
		}

		return prevSerial != tileSet.serial;
	}

	public void releaseTiles(TileSet tileSet) {
		tileSet.releaseTiles();
	}

	/** scanline fill class used to check tile visibility */
	private final ScanBox mScanBox = new ScanBox() {
		@Override
		protected void setVisible(int y, int x1, int x2) {

			MapTile[] tiles = mDrawTiles.tiles;
			int cnt = mDrawTiles.cnt;

			for (int i = 0; i < cnt; i++) {
				MapTile t = tiles[i];
				if (t.tileY == y && t.tileX >= x1 && t.tileX < x2)
					t.isVisible = true;
			}

			/* add placeholder tiles to show both sides
			 * of date line. a little too complicated... */
			int xmax = 1 << mZoom;
			if (x1 >= 0 && x2 < xmax)
				return;

			O: for (int x = x1; x < x2; x++) {
				if (x >= 0 && x < xmax)
					continue;

				int xx = x;
				if (x < 0)
					xx = xmax + x;
				else
					xx = x - xmax;

				if (xx < 0 || xx >= xmax)
					continue;

				for (int i = cnt; i < cnt + mProxyTileCnt; i++)
					if (tiles[i].tileX == x && tiles[i].tileY == y)
						continue O;

				MapTile tile = null;
				for (int i = 0; i < cnt; i++)
					if (tiles[i].tileX == xx && tiles[i].tileY == y) {
						tile = tiles[i];
						break;
					}

				if (tile == null)
					continue;

				if (cnt + mProxyTileCnt >= tiles.length) {
					//log.error(" + mNumTileHolder");
					break;
				}
				MapTile holder = new MapTile(null, x, y, (byte) mZoom);
				holder.isVisible = true;
				holder.holder = tile;
				tile.isVisible = true;
				tiles[cnt + mProxyTileCnt++] = holder;
			}
		}
	};

	protected long getMinFade(MapTile t, int proxyLevel) {
		long minFade = MapRenderer.frametime - 50;
		if (proxyLevel <= 0) {
			for (int c = 0; c < 4; c++) {
				MapTile ci = t.node.child(c);
				if (ci == null)
					continue;

				if (ci.fadeTime > 0 && ci.fadeTime < minFade)
					minFade = ci.fadeTime;

				/* when drawing the parent of the current level
				 * we also check if the children of current level
				 * are visible */
				if (proxyLevel > -2) {
					long m = getMinFade(ci, proxyLevel - 1);
					if (m < minFade)
						minFade = m;
				}
			}
		}
		if (proxyLevel >= -1) {
			MapTile p = t.node.parent();
			if (p != null) {
				if (p.fadeTime > 0 && p.fadeTime < minFade)
					minFade = p.fadeTime;

				if (proxyLevel >= 0) {
					if ((p = p.node.parent()) != null) {
						if (p.fadeTime > 0 && p.fadeTime < minFade)
							minFade = p.fadeTime;
					}
				}
			}
		}
		return minFade;
	}
}
