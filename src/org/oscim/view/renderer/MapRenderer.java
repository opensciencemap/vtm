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
package org.oscim.view.renderer;

import java.util.ArrayList;
import java.util.Collections;

import org.oscim.core.Tile;
import org.oscim.theme.RenderTheme;
import org.oscim.utils.GlConfigChooser;
import org.oscim.view.MapPosition;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;
import org.oscim.view.generator.JobTile;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.FloatMath;
import android.util.Log;

// FIXME to many 'Renderer', this one needs a better name.. TileLoader?
public class MapRenderer extends GLSurfaceView {
	private final static String TAG = "MapRenderer";
	private GLRenderer mRenderer;

	private static final int MAX_TILES_IN_QUEUE = 40;
	private static final int CACHE_THRESHOLD = 50;

	private static MapView mMapView;

	private static final MapPosition mMapPosition = new MapPosition();
	private final MapViewPosition mMapViewPosition;

	// new jobs for the MapWorkers
	private static ArrayList<JobTile> mJobList;

	// all tiles currently referenced
	private static ArrayList<MapTile> mTiles;

	// tiles that have new data to upload, see passTile()
	private static ArrayList<MapTile> mTilesLoaded;

	// TODO current boundary tiles, values used to check if position has
	// changed for updating current tile list

	private static boolean mInitial;

	// private static MapPosition mCurPosition, mDrawPosition;
	private static int mWidth = 0, mHeight = 0;

	// maps zoom-level to available zoom-levels in MapDatabase
	// e.g. 16->16, 15->16, 14->13, 13->13, 12->13,....
	// private static int[] mZoomLevels;

	private static float[] mTileCoords = new float[8];
	private static int[] mBoundaryTiles = new int[8];

	// used for passing tiles to be rendered from TileLoader(Main-Thread) to
	// GLThread
	static final class TilesData {
		int cnt = 0;
		final MapTile[] tiles;

		TilesData(int numTiles) {
			tiles = new MapTile[numTiles];
		}
	}

	/* package */static TilesData mCurrentTiles;

	private static ScanBox mScanBox = new ScanBox() {

		@Override
		void setVisible(int y, int x1, int x2) {
			MapTile[] tiles = mCurrentTiles.tiles;
			int cnt = mCurrentTiles.cnt;
			int max = mCurrentTiles.tiles.length;
			int xmax = 1 << mZoom;

			for (int x = x1; x < x2; x++) {
				// MapTile holder = null;
				MapTile tile = null;

				// boolean found = false;
				if (cnt == max) {
					Log.d(TAG, "reached max currentTiles " + max);
					break;
				}
				int xx = x;

				if (x < 0 || x >= xmax) {
					// flip-around date line
					if (x < 0)
						xx = xmax + x;
					else
						xx = x - xmax;

					if (xx < 0 || xx >= xmax) {
						// Log.d(TAG, "tile out of bounds " + y + " " + xx);
						continue;
					}
				}

				for (int i = 0; i < cnt; i++)
					if (tiles[i].tileX == xx && tiles[i].tileY == y) {
						tile = tiles[i];
						break;
					}

				if (tile == null) {
					tile = addTile(xx, y, mZoom, 0);
					tiles[cnt++] = tile;
				}
			}
			mCurrentTiles.cnt = cnt;
		}
	};

	public MapRenderer(Context context, MapView mapView) {
		super(context);

		mMapView = mapView;
		mMapViewPosition = mapView.getMapViewPosition();

		Log.d(TAG, "init GLSurfaceLayer");
		setEGLConfigChooser(new GlConfigChooser());
		setEGLContextClientVersion(2);

		// setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
		mRenderer = new GLRenderer(mMapView);
		setRenderer(mRenderer);

		// if (!debugFrameTime)
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		mJobList = new ArrayList<JobTile>();
		mTiles = new ArrayList<MapTile>();
		mTilesLoaded = new ArrayList<MapTile>(30);

		VertexPool.init();
		QuadTree.init();

		mInitial = true;
	}

	/**
	 * Update list of visible tiles and passes them to MapRenderer, when not
	 * available tiles are created and added to JobQueue (mapView.addJobs) for
	 * loading by TileGenerator class
	 * 
	 * @param clear
	 *            whether to clear and reload all tiles
	 */
	public void updateMap(boolean clear) {
		boolean changedPos = false;

		if (mMapView == null)
			return;

		if (clear || mInitial) {
			// make sure onDrawFrame is not running
			GLRenderer.drawlock.lock();
			// remove all tiles references
			Log.d(TAG, "CLEAR");
			for (MapTile t : mTiles)
				clearTile(t);

			mTiles.clear();
			mTilesLoaded.clear();
			QuadTree.init();

			// set up TileData arrays that are passed to gl-thread
			int num = mWidth;
			if (mWidth < mHeight)
				num = mHeight;

			int size = Tile.TILE_SIZE >> 1;

			int numTiles = (num * num) / (size * size) * 4;

			mRenderer.clearTiles(numTiles);
			mCurrentTiles = new TilesData(numTiles);

			// MapInfo mapInfo = mMapView.getMapDatabase().getMapInfo();
			// if (mapInfo != null)
			// mZoomLevels = mapInfo.zoomLevel;
			GLRenderer.drawlock.unlock();

			changedPos = true;
			mInitial = false;
		}

		MapPosition mapPosition = mMapPosition;
		mMapViewPosition.getMapPosition(mapPosition, mTileCoords);

		float s = Tile.TILE_SIZE;
		// load some additional tiles more than currently visible
		float scale = mapPosition.scale * 0.75f;
		double px = mapPosition.x;
		double py = mapPosition.y;
		float[] coords = mTileCoords;
		int zdir = 0;

		for (int i = 0; i < 8; i += 2) {
			coords[i + 0] = (float) ((px + coords[i + 0] / scale) / s);
			coords[i + 1] = (float) ((py + coords[i + 1] / scale) / s);
		}

		for (int i = 0; i < 8; i++)
			if (mBoundaryTiles[i] != (int) coords[i]) {
				changedPos = true;
				break;
			}

		for (int i = 0; i < 8; i++)
			mBoundaryTiles[i] = (int) coords[i];

		// TODO all following should probably be done in an idler instead
		// to drain queued events. need to check how android handles things.

		if (changedPos) {
			updateVisibleList(mapPosition, zdir);

			if (!MapView.debugFrameTime)
				requestRender();

			int remove = mTiles.size() - GLRenderer.CACHE_TILES;
			if (remove > CACHE_THRESHOLD)
				limitCache(mapPosition, remove);

			limitLoadQueue();
		} else {
			if (!MapView.debugFrameTime)
				requestRender();
		}
	}

	/**
	 * set mCurrentTiles for the visible tiles and pass it to GLRenderer, add
	 * jobs for not yet loaded tiles
	 * 
	 * @param mapPosition
	 *            the current MapPosition
	 * @param zdir
	 *            zoom direction
	 */
	private static void updateVisibleList(MapPosition mapPosition, int zdir) {

		mJobList.clear();
		// set non processed tiles to isLoading == false
		mMapView.addJobs(null);
		mCurrentTiles.cnt = 0;
		mScanBox.scan(mTileCoords, mapPosition.zoomLevel);
		// Log.d(TAG, "visible: " + mCurrentTiles.cnt + "/" +
		// mCurrentTiles.tiles.length);
		GLRenderer.updateTiles(mCurrentTiles);

		// note: this sets isLoading == true for all job tiles
		if (mJobList.size() > 0) {
			updateTileDistances(mJobList, mapPosition);
			Collections.sort(mJobList);
			mMapView.addJobs(mJobList);
		}
	}

	/* package */
	static MapTile addTile(int x, int y, byte zoomLevel, int zdir) {
		MapTile tile;

		tile = QuadTree.getTile(x, y, zoomLevel);

		if (tile == null) {
			tile = new MapTile(x, y, zoomLevel);

			QuadTree.add(tile);
			mTiles.add(tile);
		}

		// if (!fetchProxy && !tile.isActive()) {
		if (!tile.isActive()) {
			mJobList.add(tile);
		}

		// mCurrentTiles.tiles[tiles++] = tile;

		// if (fetchChildren) {
		// byte z = (byte) (zoomLevel + 1);
		// for (int i = 0; i < 4; i++) {
		// int cx = (xx << 1) + (i % 2);
		// int cy = (yy << 1) + (i >> 1);
		//
		// MapTile c = QuadTree.getTile(cx, cy, z);
		//
		// if (c == null) {
		// c = new MapTile(cx, cy, z);
		//
		// QuadTree.add(c);
		// mTiles.add(c);
		// }
		//
		// if (!c.isActive()) {
		// mJobList.add(c);
		// }
		// }
		// }

		// if (fetchParent || (!fetchProxy && zdir > 0 && zoomLevel > 0)) {
		if (zdir > 0 && zoomLevel > 0) {
			// prefetch parent
			MapTile p = tile.rel.parent.tile;

			if (p == null) {
				p = new MapTile(x >> 1, y >> 1, (byte) (zoomLevel - 1));

				QuadTree.add(p);
				mTiles.add(p);
				mJobList.add(p);

			} else if (!p.isActive()) {
				if (!mJobList.contains(p))
					mJobList.add(p);
			}
		}
		return tile;
	}

	private static void clearTile(MapTile t) {

		t.newData = false;
		t.isLoading = false;
		t.isReady = false;

		LineRenderer.clear(t.lineLayers);
		PolygonRenderer.clear(t.polygonLayers);

		t.labels = null;
		t.lineLayers = null;
		t.polygonLayers = null;

		if (t.vbo != null) {
			GLRenderer.addVBO(t.vbo);
			t.vbo = null;
		}
		if (t.texture != null)
			t.texture.tile = null;

		QuadTree.remove(t);
	}

	private static void updateTileDistances(ArrayList<?> tiles, MapPosition mapPosition) {
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
				// t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)) *
				// 0.25f;
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

				// t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)) *
				// (-diff * 0.5f);
				t.distance = FloatMath.sqrt((dx * dx + dy * dy)) * (-diff * 0.5f);
			}
		}
	}

	private static void limitCache(MapPosition mapPosition, int remove) {
		int size = mTiles.size();

		// remove orphaned tiles
		for (int i = 0; i < size;) {
			MapTile t = mTiles.get(i);
			// make sure tile cannot be used by GL or MapWorker Thread
			if (t.isLocked() || t.isActive()) {
				i++;
			} else {
				// Log.d(TAG, "remove empty tile" + t);
				clearTile(t);
				mTiles.remove(i);
				remove--;
				size--;
			}
		}

		if (remove <= 0)
			return;

		updateTileDistances(mTiles, mapPosition);
		Collections.sort(mTiles);

		for (int i = 1; i < remove; i++) {

			MapTile t = mTiles.remove(size - i);

			synchronized (t) {
				if (t.isLocked()) {
					// dont remove tile used by renderthread
					Log.d(TAG, "X not removing " + t
							// + " " + t.isLocked
							+ " " + t.distance);

					mTiles.add(t);
					continue;
				}

				if (t.isLoading) {
					// NOTE: if we add tile back then on next limitCache
					// the tile will be removed. clearTile could interfere with
					// MapGenerator. so clear in passTile() instead.
					// mTiles.add(t);
					t.isLoading = false;
					Log.d(TAG, "X cancel loading " + t + " " + t.distance);
					continue;
				}

				clearTile(t);
			}
		}
	}

	private static void limitLoadQueue() {
		int size = mTilesLoaded.size();

		if (size < MAX_TILES_IN_QUEUE)
			return;

		synchronized (mTilesLoaded) {

			// remove tiles uploaded to vbo
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

			if (size < MAX_TILES_IN_QUEUE)
				return;

			// Log.d(TAG, "queue: " + mTilesLoaded.size() + " " + size + " "
			// + (size - MAX_TILES_IN_QUEUE / 2));

			// clear loaded but not used tiles
			for (int i = 0, n = size - MAX_TILES_IN_QUEUE / 2; i < n; n--) {

				MapTile t = mTilesLoaded.get(i);

				synchronized (t) {
					if (t.rel == null) {
						mTilesLoaded.remove(i);
						continue;
					}

					if (t.isLocked()) {
						// Log.d(TAG, "keep unused tile data: " + t + " " +
						// t.isActive);
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
	 * called from MapWorker Thread when tile is loaded by TileGenerator
	 * 
	 * @param jobTile
	 *            ...
	 * @return ...
	 */
	public synchronized boolean passTile(JobTile jobTile) {
		MapTile tile = (MapTile) jobTile;

		if (!tile.isLoading) {
			// no one should be able to use this tile now, TileGenerator passed
			// it, GL-Thread does nothing until newdata is set.
			Log.d(TAG, "passTile: canceled " + tile);
			synchronized (mTilesLoaded) {
				mTilesLoaded.add(tile);
			}
			return true;
		}

		mRenderer.setVBO(tile);

		if (tile.vbo == null) {
			Log.d(TAG, "no VBOs left for " + tile);
			tile.isLoading = false;
			return false;
		}

		tile.newData = true;
		tile.isLoading = false;

		if (!MapView.debugFrameTime)
			requestRender();

		synchronized (mTilesLoaded) {
			mTilesLoaded.add(tile);
		}

		return true;
	}

	public void setRenderTheme(RenderTheme t) {
		if (mRenderer != null)
			mRenderer.setRenderTheme(t);

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {

		Log.d(TAG, "onSizeChanged" + w + " " + h);
		mWidth = w;
		mHeight = h;

		if (mWidth > 0 && mHeight > 0)
			mInitial = true;

		super.onSizeChanged(w, h, oldw, oldh);
	}

	// private static void updateVisibleList(MapPosition mapPosition, int zdir)
	// {
	// double x = mapPosition.x;
	// double y = mapPosition.y;
	// byte zoomLevel = mapPosition.zoomLevel;
	// float scale = mapPosition.scale;
	//
	// double add = 1.0f / scale;
	// int offsetX = (int) ((mWidth >> 1) * add) + Tile.TILE_SIZE;
	// int offsetY = (int) ((mHeight >> 1) * add) + Tile.TILE_SIZE;
	//
	// long pixelRight = (long) x + offsetX;
	// long pixelBottom = (long) y + offsetY;
	// long pixelLeft = (long) x - offsetX;
	// long pixelTop = (long) y - offsetY;
	//
	// int tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, zoomLevel);
	// int tileTop = MercatorProjection.pixelYToTileY(pixelTop, zoomLevel);
	// int tileRight = MercatorProjection.pixelXToTileX(pixelRight, zoomLevel);
	// int tileBottom = MercatorProjection.pixelYToTileY(pixelBottom,
	// zoomLevel);
	//
	// mJobList.clear();
	//
	// // set non processed tiles to isLoading == false
	// mMapView.addJobs(null);
	//
	// int tiles = 0;
	// int max = mCurrentTiles.tiles.length - 1;
	//
	// // boolean fetchChildren = false;
	// // boolean fetchParent = false;
	// // boolean fetchProxy = false;
	// // if (mZoomLevels != null) {
	// // // check MapDatabase zoom-level-mapping
	// // if (mZoomLevels[zoomLevel] == 0) {
	// // mCurrentTiles.cnt = 0;
	// // mCurrentTiles = GLRenderer.updateTiles(mCurrentTiles);
	// // return;
	// // }
	// //
	// // if (mZoomLevels[zoomLevel] > zoomLevel) {
	// // fetchChildren = true;
	// // fetchProxy = true;
	// //
	// // } else if (mZoomLevels[zoomLevel] < zoomLevel) {
	// // fetchParent = true;
	// // fetchProxy = true;
	// // }
	// // }
	//
	// for (int yy = tileTop; yy <= tileBottom; yy++) {
	// for (int xx = tileLeft; xx <= tileRight; xx++) {
	//
	// if (tiles == max)
	// break;
	//
	// // MapTile tile =
	// addTile(xx, yy, zoomLevel, zdir);
	// // mCurrentTiles.tiles[tiles++] = tile;
	// }
	// }
	//
	// // pass new tile list to glThread
	// mCurrentTiles.cnt = tiles;
	// mCurrentTiles = GLRenderer.updateTiles(mCurrentTiles);
	//
	// // note: this sets isLoading == true for all job tiles
	// if (mJobList.size() > 0) {
	// updateTileDistances(mJobList, mapPosition);
	// Collections.sort(mJobList);
	// mMapView.addJobs(mJobList);
	// }
	// }
}
