/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.android.glrenderer;

import java.util.ArrayList;
import java.util.Collections;

import org.oscim.android.MapView;
import org.oscim.android.glrenderer.MapRenderer.TilesData;
import org.oscim.android.mapgenerator.JobTile;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;

import android.util.FloatMath;
import android.util.Log;

// for lack of a better name... maybe TileManager?

/**
 * Update list of visible tiles and passes them to MapRenderer, when not available tiles are created and added to
 * JobQueue (mapView.addJobs) for loading by MapGenerator class
 */

class TileLoader {
	private static final String TAG = "TileLoader";

	private static final int MAX_TILES_IN_QUEUE = 40;

	private static MapView mMapView;

	// new jobs for the MapWorkers
	private static ArrayList<JobTile> mJobList;

	// all tiles currently referenced
	private static ArrayList<MapTile> mTiles;
	// private static ArrayList<MapTile> mTilesActive;
	// tiles that have new data to upload, see passTile()
	private static ArrayList<MapTile> mTilesLoaded;

	// current center tile, values used to check if position has
	// changed for updating current tile list
	private static long mTileX, mTileY;
	private static float mPrevScale;
	private static byte mPrevZoom;
	private static boolean mInitial;

	// private static MapPosition mCurPosition, mDrawPosition;
	private static int mWidth, mHeight;

	private static TilesData newTiles;

	static void init(MapView mapView, int w, int h, int numTiles) {
		mMapView = mapView;

		ShortPool.init();
		QuadTree.init();

		mJobList = new ArrayList<JobTile>();
		mTiles = new ArrayList<MapTile>();
		mTilesLoaded = new ArrayList<MapTile>(30);

		mInitial = true;
		mWidth = w;
		mHeight = h;

		newTiles = new TilesData(numTiles);
	}

	static synchronized void updateMap(boolean clear) {

		boolean changedPos = false;
		boolean changedZoom = false;

		if (mMapView == null || mMapView.getMapPosition() == null)
			return;

		MapPosition mapPosition = mMapView.getMapPosition().getMapPosition();

		if (mapPosition == null) {
			Log.d(TAG, ">>> no map position");
			return;
		}

		if (clear) {
			mInitial = true;
			synchronized (MapRenderer.lock) {

				for (MapTile t : mTiles)
					clearTile(t);

				mTiles.clear();
				mTilesLoaded.clear();
				QuadTree.init();
			}
		}

		byte zoomLevel = mapPosition.zoomLevel;
		float scale = mapPosition.scale;

		long tileX = MercatorProjection.pixelXToTileX(mapPosition.x, zoomLevel)
				* Tile.TILE_SIZE;
		long tileY = MercatorProjection.pixelYToTileY(mapPosition.y, zoomLevel)
				* Tile.TILE_SIZE;

		int zdir = 0;
		if (mInitial || mPrevZoom != zoomLevel) {
			changedZoom = true;
			mPrevScale = scale;
		}
		else if (tileX != mTileX || tileY != mTileY) {
			if (mPrevScale - scale > 0 && scale > 1.2)
				zdir = 1;
			mPrevScale = scale;
			changedPos = true;
		}
		else if (mPrevScale - scale > 0.2 || mPrevScale - scale < -0.2) {
			if (mPrevScale - scale > 0 && scale > 1.2)
				zdir = 1;
			mPrevScale = scale;
			changedPos = true;
		}

		if (mInitial) {
			// mCurPosition = mapPosition;
			mInitial = false;
		}

		mTileX = tileX;
		mTileY = tileY;
		mPrevZoom = zoomLevel;

		if (changedZoom) {
			// need to update visible list first when zoom level changes
			// as scaling is relative to the tiles of current zoom-level
			updateVisibleList(mapPosition, 0);
		} else {
			// pass new position to glThread
			MapRenderer.updatePosition(mapPosition);
		}

		if (!MapView.debugFrameTime)
			mMapView.requestRender();

		if (changedPos)
			updateVisibleList(mapPosition, zdir);

		if (changedPos || changedZoom) {
			int remove = mTiles.size() - MapRenderer.CACHE_TILES;
			if (remove > 50)
				limitCache(mapPosition, remove);
		}

		limitLoadQueue();

	}

	private static boolean updateVisibleList(MapPosition mapPosition, int zdir) {
		double x = mapPosition.x;
		double y = mapPosition.y;
		byte zoomLevel = mapPosition.zoomLevel;
		float scale = mapPosition.scale;

		double add = 1.0f / scale;
		int offsetX = (int) ((mWidth >> 1) * add) + Tile.TILE_SIZE;
		int offsetY = (int) ((mHeight >> 1) * add) + Tile.TILE_SIZE;

		long pixelRight = (long) x + offsetX;
		long pixelBottom = (long) y + offsetY;
		long pixelLeft = (long) x - offsetX;
		long pixelTop = (long) y - offsetY;

		int tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, zoomLevel);
		int tileTop = MercatorProjection.pixelYToTileY(pixelTop, zoomLevel);
		int tileRight = MercatorProjection.pixelXToTileX(pixelRight, zoomLevel);
		int tileBottom = MercatorProjection.pixelYToTileY(pixelBottom, zoomLevel);

		mJobList.clear();

		// set non processed tiles to isLoading == false
		mMapView.addJobs(null);

		int tiles = 0;
		if (newTiles == null)
			return false;

		int max = newTiles.tiles.length - 1;

		for (int yy = tileTop; yy <= tileBottom; yy++) {
			for (int xx = tileLeft; xx <= tileRight; xx++) {

				if (tiles == max)
					break;

				MapTile tile = QuadTree.getTile(xx, yy, zoomLevel);

				if (tile == null) {
					tile = new MapTile(xx, yy, zoomLevel);

					QuadTree.add(tile);
					mTiles.add(tile);
				}

				newTiles.tiles[tiles++] = tile;

				if (!(tile.isLoading || tile.newData || tile.isReady)) {
					mJobList.add(tile);
				}

				if (zdir > 0 && zoomLevel > 0) {
					// prefetch parent
					MapTile parent = tile.rel.parent.tile;

					if (parent == null) {
						parent = new MapTile(xx >> 1, yy >> 1, (byte) (zoomLevel - 1));

						QuadTree.add(parent);
						mTiles.add(parent);
					}

					if (!(parent.isLoading || parent.isReady || parent.newData)) {
						if (!mJobList.contains(parent))
							mJobList.add(parent);
					}
				}
			}
		}

		// pass new tile list to glThread
		newTiles.cnt = tiles;
		newTiles = MapRenderer.updateTiles(mapPosition, newTiles);

		if (mJobList.size() > 0) {
			updateTileDistances(mJobList, mapPosition);
			Collections.sort(mJobList);
			mMapView.addJobs(mJobList);
		}

		return true;
	}

	private static void clearTile(MapTile t) {
		t.newData = false;
		t.isLoading = false;
		t.isReady = false;

		LineLayers.clear(t.lineLayers);
		PolygonLayers.clear(t.polygonLayers);

		t.labels = null;
		t.lineLayers = null;
		t.polygonLayers = null;

		if (t.vbo != null) {
			MapRenderer.addVBO(t.vbo);
			t.vbo = null;
		}

		QuadTree.remove(t);
	}

	private static boolean childIsActive(MapTile t) {
		MapTile c = null;

		for (int i = 0; i < 4; i++) {
			if (t.rel.child[i] == null)
				continue;

			c = t.rel.child[i].tile;
			if (c != null && c.isActive && !(c.isReady || c.newData))
				return true;
		}

		return false;
	}

	// FIXME still the chance that one jumped two zoomlevels between
	// cur and draw. should use reference counter instead
	private static boolean tileInUse(MapTile t) {
		byte z = mPrevZoom;

		if (t.isActive) {
			return true;

		} else if (t.zoomLevel == z + 1) {
			MapTile p = t.rel.parent.tile;

			if (p != null && p.isActive && !(p.isReady || p.newData))
				return true;

		} else if (t.zoomLevel == z + 2) {
			MapTile p = t.rel.parent.parent.tile;

			if (p != null && p.isActive && !(p.isReady || p.newData))
				return true;

		} else if (t.zoomLevel == z - 1) {
			if (childIsActive(t))
				return true;

		} else if (t.zoomLevel == z - 2) {
			for (QuadTree c : t.rel.child) {
				if (c == null)
					continue;

				if (c.tile != null && childIsActive(c.tile))
					return true;
			}
		} else if (t.zoomLevel == z - 3) {
			for (QuadTree c : t.rel.child) {
				if (c == null)
					continue;

				for (QuadTree c2 : c.child) {
					if (c2 == null)
						continue;

					if (c2.tile != null && childIsActive(c2.tile))
						return true;
				}
			}
		}
		return false;
	}

	private static void updateTileDistances(ArrayList<?> tiles,
			MapPosition mapPosition) {
		int h = (Tile.TILE_SIZE >> 1);
		byte zoom = mapPosition.zoomLevel;
		long x = (long) mapPosition.x;
		long y = (long) mapPosition.y;

		int diff;
		long dx, dy;

		// TODO this could need some fixing, and optimization
		// to consider move/zoom direction

		for (int i = 0, n = tiles.size(); i < n; i++) {
			JobTile t = (JobTile) tiles.get(i);
			diff = (t.zoomLevel - zoom);

			if (diff == 0) {
				dx = (t.pixelX + h) - x;
				dy = (t.pixelY + h) - y;
				// t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)) * 0.25f;
				t.distance = FloatMath.sqrt((dx * dx + dy * dy)) * 0.25f;
			} else if (diff > 0) {
				// tile zoom level is child of current

				if (diff < 3) {
					dx = ((t.pixelX + h) >> diff) - x;
					dy = ((t.pixelY + h) >> diff) - y;
				}
				else {
					dx = ((t.pixelX + h) >> (diff >> 1)) - x;
					dy = ((t.pixelY + h) >> (diff >> 1)) - y;
				}
				// t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy));
				t.distance = FloatMath.sqrt((dx * dx + dy * dy));

			} else {
				// tile zoom level is parent of current
				dx = ((t.pixelX + h) << -diff) - x;
				dy = ((t.pixelY + h) << -diff) - y;

				// t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)) * (-diff * 0.5f);
				t.distance = FloatMath.sqrt((dx * dx + dy * dy)) * (-diff * 0.5f);
			}
		}
	}

	private static void limitCache(MapPosition mapPosition, int remove) {
		int removes = remove;

		int size = mTiles.size();
		// int tmp = size;

		// remove orphaned tiles
		for (int i = 0; i < size;) {
			MapTile cur = mTiles.get(i);
			// make sure tile cannot be used by GL or MapWorker Thread
			if ((!cur.isActive) && (!cur.isLoading) && (!cur.newData)
					&& (!cur.isReady) && (!tileInUse(cur))) {

				clearTile(cur);
				mTiles.remove(i);
				removes--;
				size--;
				// Log.d(TAG, "remove empty tile" + cur);
				continue;
			}
			i++;
		}

		// Log.d(TAG, "remove tiles: " + removes + " " + size + " " + tmp);

		if (removes <= 0)
			return;

		updateTileDistances(mTiles, mapPosition);
		Collections.sort(mTiles);

		// boolean printAll = false;

		for (int i = 1; i <= removes; i++) {

			MapTile t = mTiles.remove(size - i);

			synchronized (t) {
				if (t.isActive) {
					// dont remove tile used by renderthread
					Log.d(TAG, "X not removing active " + t + " " + t.distance);

					// if (printAll) {
					// printAll = false;
					// for (GLMapTile tt : mTiles)
					// Log.d(TAG, ">>>" + tt + " " + tt.distance);
					// }
					mTiles.add(t);
				} else if ((t.isReady || t.newData) && tileInUse(t)) {
					// check if this tile could be used as proxy
					Log.d(TAG, "X not removing proxy: " + t + " " + t.distance);
					mTiles.add(t);
				} else if (t.isLoading) {
					// FIXME !!! if we add tile back on next limit cache
					// this will be removed. clearTile could interfere with
					// MapGenerator... clear in passTile().
					Log.d(TAG, "X cancel loading " + t + " " + t.distance);
					t.isLoading = false;
					// mTiles.add(t);

				} else {
					clearTile(t);
				}
			}
		}
	}

	private static void limitLoadQueue() {
		int size = mTilesLoaded.size();

		if (size < MAX_TILES_IN_QUEUE)
			return;

		synchronized (mTilesLoaded) {

			// remove uploaded tiles
			for (int i = 0; i < size;) {
				MapTile t = mTilesLoaded.get(i);
				// rel == null means tile is already removed by limitCache
				if (!t.newData || t.rel == null) {
					mTilesLoaded.remove(i);
					size--;
					continue;
				}
				i++;
			}

			// clear loaded but not used tiles
			if (size < MAX_TILES_IN_QUEUE)
				return;
			// Log.d(TAG, "queue: " + mTilesLoaded.size() + " " + size + " "
			// + (size - MAX_TILES_IN_QUEUE / 2));

			for (int i = 0, n = size - MAX_TILES_IN_QUEUE / 2; i < n; n--) {

				MapTile t = mTilesLoaded.get(i);

				synchronized (t) {
					if (t.rel == null) {
						mTilesLoaded.remove(i);
						continue;
					}

					if (tileInUse(t)) {
						// Log.d(TAG, "keep unused tile data: " + t + " " + t.isActive);
						i++;
						continue;
					}

					// Log.d(TAG, "remove unused tile data: " + t);
					mTilesLoaded.remove(i);
					mTiles.remove(t);
					clearTile(t);
				}
			}
		}
	}

	/**
	 * Manage tiles that have data to be uploaded to gl
	 * 
	 * @param tile
	 *            tile processed by MapGenerator
	 */
	static void addTileLoaded(MapTile tile) {
		synchronized (mTilesLoaded) {
			mTilesLoaded.add(tile);
		}
	}
}
