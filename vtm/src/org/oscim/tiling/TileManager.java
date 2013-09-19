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

package org.oscim.tiling;

import static org.oscim.tiling.MapTile.STATE_LOADING;
import static org.oscim.tiling.MapTile.STATE_NEW_DATA;
import static org.oscim.tiling.MapTile.STATE_NONE;

import java.util.ArrayList;
import java.util.Arrays;

import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;
import org.oscim.utils.ScanBox;
import org.oscim.utils.quadtree.QuadTree;
import org.oscim.utils.quadtree.QuadTreeIndex;

public class TileManager {
	static final String TAG = TileManager.class.getName();

	private int mCacheLimit;
	private int mMinZoom;
	private int mMaxZoom;

	// limit number tiles with new data not uploaded to GL
	// TODO this should depend on the number of tiles displayed
	private static final int MAX_TILES_IN_QUEUE = 40;
	// cache limit threshold
	private static final int CACHE_THRESHOLD = 30;

	private final Map mMap;
	private final Viewport mViewport;

	// cache for all tiles
	private MapTile[] mTiles;

	// actual number of tiles in mTiles
	private int mTilesCount;

	// current end position in mTiles
	private int mTilesSize;

	// counter for tiles with new data not
	// yet uploaded to GL
	private volatile int mTilesForUpload;

	// new tile jobs for MapWorkers
	private final ArrayList<MapTile> mJobs;

	// counter to check whether current TileSet has changed
	private int mUpdateSerial;

	// lock for TileSets while updating MapTile locks
	private final Object mTilelock = new Object();

	// need to keep track of TileSets to clear on reset.
	// private final ArrayList<TileSet> mTileSets = new ArrayList<TileSet>(4);

	private TileSet mCurrentTiles;
	/* package */TileSet mNewTiles;

	// job queue filled in TileManager and polled by TileLoaders
	final JobQueue jobQueue;

	private final QuadTreeIndex<MapTile> mIndex = new QuadTreeIndex<MapTile>() {

		@Override
		public MapTile create(int x, int y, int z) {
			QuadTree<MapTile> t = super.add(x, y, z);
			t.item = new MapTile(x, y, (byte) z);
			t.item.rel = t;

			return t.item;
		}

		@Override
		public void remove(MapTile t) {
			if (t.rel == null) {
				Log.d(TAG, "BUG already removed " + t);
				return;
			}

			super.remove(t.rel);

			t.rel.item = null;
			t.rel = null;
		}
	};

	private final float[] mMapPlane = new float[8];

	//private final TileLayer<?> mTileLayer;

	public TileManager(Map map, int minZoom, int maxZoom, int cacheLimit) {
		mMap = map;
		//mTileLayer = tileLayer;
		mMaxZoom = maxZoom;
		mMinZoom = minZoom;
		mCacheLimit = cacheLimit;

		mViewport = map.getViewport();

		jobQueue = new JobQueue();
		mJobs = new ArrayList<MapTile>();
		mTiles = new MapTile[mCacheLimit];

		mTilesSize = 0;
		mTilesForUpload = 0;

		mUpdateSerial = 0;
	}

	public void destroy() {
		// there might be some leaks in here
		// ... free static pools
	}

	private int[] mZoomTable;

	public void setZoomTable(int[] zoomLevel) {
		mZoomTable = zoomLevel;

	}

	public void init(boolean first) {

		// sync with GLRender thread
		// ... and labeling thread?
		synchronized (MapRenderer.drawlock) {

			if (!first) {
				// pass VBOs and VertexItems back to pools
				for (int i = 0; i < mTilesSize; i++)
					clearTile(mTiles[i]);
			}

			// FIXME any of this still needed?
			// mInitialized is set when surface changed
			// and VBOs might be lost
			// VertexPool.init();
			// clear cache index
			// QuadTree.init();

			// clear references to cached MapTiles
			Arrays.fill(mTiles, null);
			mTilesSize = 0;
			mTilesCount = 0;

			// set up TileSet large enough to hold current tiles
			int num = Math.max(mMap.getWidth(), mMap.getHeight());
			int size = Tile.SIZE >> 1;
			int numTiles = (num * num) / (size * size) * 4;

			mNewTiles = new TileSet(numTiles);
			mCurrentTiles = new TileSet(numTiles);
			Log.d(TAG, "max tiles: " + numTiles);

		}
	}

	/**
	 * 1. Update mCurrentTiles TileSet of currently visible tiles. 2. Add not
	 * yet loaded (or loading) tiles to JobQueue. 3. Manage cache
	 * 
	 * @param pos
	 *            current MapPosition
	 */
	public synchronized boolean update(MapPosition pos) {
		// clear JobQueue and set tiles to state == NONE.
		// one could also append new tiles and sort in JobQueue
		// but this has the nice side-effect that MapWorkers dont
		// start with old jobs while new jobs are calculated, which
		// should increase the chance that they are free when new
		// jobs come in.
		jobQueue.clear();

		// load some tiles more than currently visible (* 0.75)
		double scale = pos.scale * 0.9f;

		int tileZoom = FastMath.clamp(pos.zoomLevel, mMinZoom, mMaxZoom);

		if (mZoomTable != null) {
			int match = 0;
			for (int z : mZoomTable) {
				if (z <= tileZoom && z > match)
					match = z;
			}
			if (match == 0)
				return false;

			tileZoom = match;
		}

		mViewport.getMapViewProjection(mMapPlane);

		// scan visible tiles. callback function calls 'addTile'
		// which updates mNewTiles
		mNewTiles.cnt = 0;
		mScanBox.scan(pos.x, pos.y, scale, tileZoom, mMapPlane);

		MapTile[] newTiles = mNewTiles.tiles;
		int newCnt = mNewTiles.cnt;

		MapTile[] curTiles = mCurrentTiles.tiles;
		int curCnt = mCurrentTiles.cnt;

		boolean changed = (newCnt != curCnt);

		Arrays.sort(newTiles, 0, newCnt, TileSet.coordComparator);

		if (!changed) {
			// compare if any tile has changed
			for (int i = 0; i < newCnt; i++) {
				if (newTiles[i] != curTiles[i]) {
					changed = true;
					break;
				}
			}
		}

		if (changed) {
			synchronized (mTilelock) {
				// lock new tiles
				mNewTiles.lockTiles();

				// unlock previous tiles
				mCurrentTiles.releaseTiles();

				// make new tiles current
				TileSet tmp = mCurrentTiles;
				mCurrentTiles = mNewTiles;
				mNewTiles = tmp;

				mUpdateSerial++;
			}

			// request rendering as tiles changed
			mMap.render();
		}

		/* Add tile jobs to queue */
		if (mJobs.isEmpty())
			return false;

		MapTile[] jobs = new MapTile[mJobs.size()];
		jobs = mJobs.toArray(jobs);
		updateTileDistances(jobs, jobs.length, pos);

		// sets tiles to state == LOADING
		jobQueue.setJobs(jobs);

		mJobs.clear();

		/* limit cache items */
		int remove = mTilesCount - mCacheLimit;

		if (remove > CACHE_THRESHOLD ||
		    mTilesForUpload > MAX_TILES_IN_QUEUE)

			limitCache(pos, remove);

		return true;
	}

	/** only used in setmapDatabase -- deprecate? */
	public void clearJobs() {
		jobQueue.clear();
	}

	/**
	 * Retrive a TileSet of current tiles. Tiles remain locked in cache until
	 * the set is unlocked by either passing it again to this function or to
	 * releaseTiles. If passed TileSet is null it will be allocated.
	 * 
	 * @param tileSet
	 *            to be updated
	 * @return true if TileSet has changed
	 */
	public boolean getActiveTiles(TileSet tileSet) {
		if (mCurrentTiles == null)
			return false;

		if (tileSet == null)
			return false;

		if (tileSet.serial == mUpdateSerial)
			return false;

		// dont flip mNew/mCurrentTiles while copying
		synchronized (mTilelock) {
			tileSet.setTiles(mCurrentTiles);
			tileSet.serial = mUpdateSerial;
		}

		return true;
	}

	/**
	 * Unlock tiles and clear all item references.
	 * 
	 * @param tiles
	 */
	public void releaseTiles(TileSet tileSet) {
		tileSet.releaseTiles();
	}

	/* package */MapTile addTile(int x, int y, int zoomLevel) {
		MapTile tile;

		// tile = QuadTree.getTile(x, y, zoomLevel);
		tile = mIndex.getTile(x, y, zoomLevel);

		if (tile == null) {
			tile = mIndex.create(x, y, zoomLevel);
			mJobs.add(tile);
			addToCache(tile);
		} else if (!tile.isActive()) {
			mJobs.add(tile);
		}

		if ((zoomLevel > 2) && (mZoomTable == null)) {
			boolean add = false;

			// prefetch parent
			MapTile p = tile.rel.parent.item;

			if (p == null) {
				p = mIndex.create(x >> 1, y >> 1, zoomLevel - 1);

				addToCache(p);
				add = true;
			}

			if (add || !p.isActive()) {
				// hack to not add tile twice
				p.state = STATE_LOADING;
				mJobs.add(p);
			}
		}

		return tile;
	}

	private void addToCache(MapTile tile) {

		if (mTilesSize == mTiles.length) {
			if (mTilesSize > mTilesCount) {
				TileDistanceSort.sort(mTiles, 0, mTilesSize);
				// sorting also repacks the 'sparse' filled array
				// so end of mTiles is at mTilesCount now
				mTilesSize = mTilesCount;
			}

			if (mTilesSize == mTiles.length) {
				Log.d(TAG, "realloc tiles " + mTilesSize);
				MapTile[] tmp = new MapTile[mTiles.length + 20];
				System.arraycopy(mTiles, 0, tmp, 0, mTilesCount);
				mTiles = tmp;
			}
		}

		mTiles[mTilesSize++] = tile;
		mTilesCount++;
	}

	private void clearTile(MapTile t) {
		if (t == null)
			return;

		t.clear();

		mIndex.remove(t);

		// QuadTree.remove(t);
		t.state = STATE_NONE;

		mTilesCount--;
	}

	private static void updateTileDistances(MapTile[] tiles, int size, MapPosition pos) {
		// TODO there is probably a better quad-tree distance function

		int zoom = 20;
		long x = (long) (pos.x * (1 << zoom));
		long y = (long) (pos.y * (1 << zoom));

		for (int i = 0; i < size; i++) {
			MapTile t = tiles[i];
			if (t == null)
				continue;

			int diff = (zoom - t.zoomLevel);
			long dx, dy;

			if (diff == 0) {
				dx = t.tileX - x;
				dy = t.tileY - y;
			} else { // diff > 0
				long mx = x >> diff;
				long my = y >> diff;

				dx = t.tileX - mx;
				dy = t.tileY - my;
			}

			int dz = (pos.zoomLevel - t.zoomLevel);
			if (dz == 0)
				dz = 1;
			else if (dz < -1)
				dz *= 0.75;

			t.distance = (dx * dx + dy * dy) * (dz * dz);
		}
	}

	private void limitCache(MapPosition pos, int remove) {
		MapTile[] tiles = mTiles;
		int size = mTilesSize;

		// count tiles that have new data
		mTilesForUpload = 0;
		int newTileCnt = 0;

		// remove tiles that were never loaded
		for (int i = 0; i < size; i++) {
			MapTile t = tiles[i];
			if (t == null)
				continue;

			if (t.state == STATE_NEW_DATA)
				newTileCnt++;

			// make sure tile cannot be used by GL or MapWorker Thread
			if ((t.state != 0) || t.isLocked()) {
				continue;
			}
			clearTile(t);
			tiles[i] = null;
			remove--;
		}

		if (remove > 10 || newTileCnt > MAX_TILES_IN_QUEUE) {
			updateTileDistances(tiles, size, pos);

			TileDistanceSort.sort(tiles, 0, size);

			// sorting also repacks the 'sparse' filled array
			// so end of mTiles is at mTilesCount now
			size = mTilesSize = mTilesCount;

			// Log.d(TAG, "remove:" + remove + "  new:" + newTileCnt);
			// Log.d(TAG, "cur: " + mapPosition);

			for (int i = size - 1; i >= 0 && remove > 0; i--) {
				MapTile t = tiles[i];
				if (t.isLocked()) {
					// dont remove tile used by GLRenderer, or somewhere else
					Log.d(TAG, "locked " + t
					           + " " + t.distance
					           + " " + (t.state == STATE_NEW_DATA)
					           + " " + (t.state == STATE_LOADING)
					           + " " + pos.zoomLevel);
					// try again in next run.
				} else if (t.state == STATE_LOADING) {
					// NOTE: when set loading to false the tile could be
					// added to load queue again while still processed in
					// MapTileLoader => need tile.cancel flag.
					// t.isLoading = false;
					Log.d(TAG, "cancel loading " + t
					           + " " + t.distance);
				} else {
					// clear unused tile

					if (t.state == STATE_NEW_DATA) {
						// Log.d(TAG, "limitCache: clear unused " + t
						// + " " + t.distance);
						newTileCnt--;
					}

					remove--;
					clearTile(t);
					tiles[i] = null;
				}
			}

			remove = (newTileCnt - MAX_TILES_IN_QUEUE) + 10;
			// int r = remove;
			for (int i = size - 1; i >= 0 && remove > 0; i--) {
				MapTile t = tiles[i];
				if (t != null && t.state == STATE_NEW_DATA) {
					if (!t.isLocked()) {
						clearTile(t);
						tiles[i] = null;
						remove--;
						newTileCnt--;
					}
				}
			}

			mTilesForUpload += newTileCnt;
			// Log.d(TAG, "cleanup load queue " + tilesForUpload + "/" + r +
			// " - " + remove);
		}
	}

	/**
	 * called from MapWorker Thread when tile is loaded by MapTileLoader
	 * 
	 * @param tile
	 *            Tile ready for upload in TileRenderLayer
	 * @return caller does not care
	 */
	public void passTile(MapTile tile, boolean success) {

		if (!success) {
			tile.clear();
			return;
		}

		tile.state = STATE_NEW_DATA;

		// is volatile
		mTilesForUpload++;

		// locked means the tile is visible or referenced by
		// a tile that might be visible.
		if (tile.isLocked())
			mMap.render();
	}

	private final ScanBox mScanBox = new ScanBox() {

		@Override
		protected void setVisible(int y, int x1, int x2) {
			MapTile[] tiles = mNewTiles.tiles;
			int cnt = mNewTiles.cnt;
			int maxTiles = tiles.length;

			int xmax = 1 << mZoom;

			for (int x = x1; x < x2; x++) {
				MapTile tile = null;

				if (cnt == maxTiles) {
					Log.d(TAG, "reached maximum tiles " + maxTiles);
					break;
				}
				int xx = x;

				if (x < 0 || x >= xmax) {
					// flip-around date line
					if (x < 0)
						xx = xmax + x;
					else
						xx = x - xmax;

					if (xx < 0 || xx >= xmax)
						continue;
				}

				// check if tile is already added
				for (int i = 0; i < cnt; i++)
					if (tiles[i].tileX == xx && tiles[i].tileY == y) {
						tile = tiles[i];
						break;
					}

				if (tile == null) {
					tile = addTile(xx, y, mZoom);
					tiles[cnt++] = tile;
				}
			}
			mNewTiles.cnt = cnt;
		}
	};
}
