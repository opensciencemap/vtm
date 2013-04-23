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

import static org.oscim.layers.tile.MapTile.STATE_NEW_DATA;
import static org.oscim.layers.tile.MapTile.STATE_READY;

import org.oscim.core.MapPosition;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.overlays.RenderOverlay;
import org.oscim.utils.ScanBox;
import org.oscim.view.MapView;

import android.util.Log;
public class TileRenderLayer extends RenderOverlay {
	private final static String TAG = TileRenderLayer.class.getName();

	private final float[] mBoxCoords;
	private final TileManager mTileManager;
	public TileRenderLayer(MapView mapView, TileManager tileManager) {
		super(mapView);
		mTileManager = tileManager;
		mBoxCoords = new float[8];
	}

	boolean mFaded;

	public void setFaded(boolean faded){
		mFaded = faded;
	}

	@Override
	public void update(MapPosition curPos, boolean positionChanged, boolean tilesChanged,
			Matrices matrices) {
		int serial = 0;

		mMapPosition.copy(curPos);

		if (mDrawTiles != null)
			serial = mDrawTiles.getSerial();

		synchronized (tilelock) {
			// get current tiles to draw
			mDrawTiles = mTileManager.getActiveTiles(mDrawTiles);
		}

		if (mDrawTiles == null || mDrawTiles.cnt == 0)
			return;

		if (positionChanged)
			mMapView.getMapViewPosition().getMapViewProjection(mBoxCoords);

		boolean changed = false;
		//boolean positionChanged = false;

		// check if the tiles have changed...
		if (serial != mDrawTiles.getSerial()) {
			changed = true;
			// FIXME needed?
			//positionChanged = true;
		}

		int tileCnt = mDrawTiles.cnt;
		MapTile[] tiles = mDrawTiles.tiles;

		if (changed || positionChanged)
			updateTileVisibility();

		tileCnt += mNumTileHolder;

		/* prepare tile for rendering */
		int uploadCnt = compileTileLayers(tiles, tileCnt);

		tilesChanged |= (uploadCnt > 0);

		TileRenderer.draw(tiles, tileCnt, curPos, matrices, mFaded);
	}

	@Override
	public void compile() {

	}

	@Override
	public void render(MapPosition pos, Matrices m) {


	}

	/** compile tile layer data and upload to VBOs */
	private static int compileTileLayers(MapTile[] tiles, int tileCnt) {
		int uploadCnt = 0;

		for (int i = 0; i < tileCnt; i++) {
			MapTile tile = tiles[i];

			if (!tile.isVisible)
				continue;

			if (tile.state == STATE_READY)
				continue;

			if (tile.state == STATE_NEW_DATA) {
				uploadTileData(tile);
				continue;
			}

			if (tile.holder != null) {
				// load tile that is referenced by this holder
				if (tile.holder.state == STATE_NEW_DATA)
					uploadTileData(tile.holder);

				tile.state = tile.holder.state;
				continue;
			}

			// check near relatives than can serve as proxy
			if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
				MapTile rel = tile.rel.parent.item;
				if (rel.state == STATE_NEW_DATA)
					uploadTileData(rel);

				// dont load child proxies
				continue;
			}

			for (int c = 0; c < 4; c++) {
				if ((tile.proxies & 1 << c) == 0)
					continue;

				MapTile rel = tile.rel.get(i);
				if (rel != null && rel.state == STATE_NEW_DATA)
					uploadTileData(rel);
			}
		}

		if (uploadCnt > 0)
			GLRenderer.checkBufferUsage(false);

		return uploadCnt;
	}

	private static void uploadTileData(MapTile tile) {
		tile.state = STATE_READY;

		if (tile.layers == null)
			return;

		int newSize = tile.layers.getSize();
		if (newSize > 0) {

			if (tile.layers.vbo == null)
				tile.layers.vbo = BufferObject.get(newSize);

			if (!GLRenderer.uploadLayers(tile.layers, newSize, true)) {
				Log.d(TAG, "BUG uploadTileData " + tile + " failed!");

				BufferObject.release(tile.layers.vbo);
				tile.layers.vbo = null;
				tile.layers.clear();
				tile.layers = null;
			}
		}
	}

	private final Object tilelock = new Object();

	/** set tile isVisible flag true for tiles that intersect view */
	private void updateTileVisibility() {

		// lock tiles while updating isVisible state
		synchronized (tilelock) {
			MapPosition pos = mMapPosition;
			MapTile[] tiles = mDrawTiles.tiles;

			int tileZoom = tiles[0].zoomLevel;

			for (int i = 0; i < mDrawTiles.cnt; i++)
				tiles[i].isVisible = false;

			// count placeholder tiles
			mNumTileHolder = 0;

			// check visibile tiles
			mScanBox.scan(pos.x, pos.y, pos.scale, tileZoom, mBoxCoords);
		}
	}

	// get a TileSet of currently visible tiles
	public TileSet getVisibleTiles(TileSet td) {
		if (mDrawTiles == null)
			return td;

		// ensure tiles keep visible state
		synchronized (tilelock) {

			MapTile[] newTiles = mDrawTiles.tiles;
			int cnt = mDrawTiles.cnt;

			if (td == null)
				td = new TileSet(newTiles.length);

			// unlock previous tiles
			for (int i = 0; i < td.cnt; i++)
				td.tiles[i].unlock();

			// lock tiles to not be removed from cache
			td.cnt = 0;
			for (int i = 0; i < cnt; i++) {
				MapTile t = newTiles[i];
				if (t.isVisible && t.state == STATE_READY) {
					t.lock();
					td.tiles[td.cnt++] = t;
				}
			}
		}
		return td;
	}

	public void releaseTiles(TileSet td) {
		for (int i = 0; i < td.cnt; i++) {
			td.tiles[i].unlock();
			td.tiles[i] = null;
		}
		td.cnt = 0;
	}


	// Add additional tiles that serve as placeholer when flipping
		// over date-line.
		// I dont really like this but cannot think of a better solution:
		// the other option would be to run scanbox each time for upload,
		// drawing, proxies and text layer. needing to add placeholder only
		// happens rarely, unless you live on Fidschi

		/* package */int mNumTileHolder;
		/* package */TileSet mDrawTiles;

		// scanline fill class used to check tile visibility
		private final ScanBox mScanBox = new ScanBox() {
			@Override
			protected void setVisible(int y, int x1, int x2) {
				int cnt = mDrawTiles.cnt;

				MapTile[] tiles = mDrawTiles.tiles;

				for (int i = 0; i < cnt; i++) {
					MapTile t = tiles[i];
					if (t.tileY == y && t.tileX >= x1 && t.tileX < x2)
						t.isVisible = true;
				}

				int xmax = 1 << mZoom;
				if (x1 >= 0 && x2 < xmax)
					return;

				// add placeholder tiles to show both sides
				// of date line. a little too complicated...
				for (int x = x1; x < x2; x++) {
					MapTile holder = null;
					MapTile tile = null;
					boolean found = false;

					if (x >= 0 && x < xmax)
						continue;

					int xx = x;
					if (x < 0)
						xx = xmax + x;
					else
						xx = x - xmax;

					if (xx < 0 || xx >= xmax)
						continue;

					for (int i = cnt; i < cnt + mNumTileHolder; i++)
						if (tiles[i].tileX == x && tiles[i].tileY == y) {
							found = true;
							break;
						}

					if (found)
						continue;

					for (int i = 0; i < cnt; i++)
						if (tiles[i].tileX == xx && tiles[i].tileY == y) {
							tile = tiles[i];
							break;
						}

					if (tile == null)
						continue;

					holder = new MapTile(x, y, (byte) mZoom);
					holder.isVisible = true;
					holder.holder = tile;
					tile.isVisible = true;
					tiles[cnt + mNumTileHolder++] = holder;
				}
			}
		};
}
