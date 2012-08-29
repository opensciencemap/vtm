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

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DITHER;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SCISSOR_TEST;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_STENCIL_BUFFER_BIT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glClearStencil;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glScissor;
import static android.opengl.GLES20.glViewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.mapsforge.android.DebugSettings;
import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.JobParameters;
import org.mapsforge.android.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.mapgenerator.TileCacheKey;
import org.mapsforge.android.mapgenerator.TileDistanceSort;
import org.mapsforge.android.rendertheme.RenderTheme;
import org.mapsforge.android.utils.GlUtils;
import org.mapsforge.core.MapPosition;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Tile;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;

/**
 * TODO - use proxy child/parent tile nearer to current tile (currently it is always parent first) - use stencil instead
 * of scissor mask for rotation - draw up to two parents above current tile, maybe prefetch parent
 */
public class MapRenderer implements org.mapsforge.android.IMapRenderer {
	private static final String TAG = "MapRenderer";
	private static final int MB = 1024 * 1024;

	private static int CACHE_TILES_MAX = 250;
	private static int CACHE_TILES = CACHE_TILES_MAX;
	private static int LIMIT_BUFFERS = 20 * MB;

	private static final int SHORT_BYTES = 2;

	static final float COORD_MULTIPLIER = 8.0f;

	private final MapView mMapView;
	private static ArrayList<MapGeneratorJob> mJobList;
	private static ArrayList<VertexBufferObject> mVBOs;

	private static TileCacheKey mTileCacheKey;
	private static HashMap<TileCacheKey, GLMapTile> mTiles;

	// all tiles currently referenced
	private static ArrayList<GLMapTile> mTileList;

	// tiles that have new data to upload, see passTile()
	private static ArrayList<GLMapTile> mTilesLoaded;

	private static TileDistanceSort mTileDistanceSort;

	private static DebugSettings mDebugSettings;
	private static JobParameters mJobParameter;

	// private static MapPosition mMapPosition;

	private static int mWidth, mHeight;
	private static float mAspect;

	// draw position is updated from current position in onDrawFrame
	// keeping the position consistent while drawing
	private static double mDrawX, mDrawY, mDrawZ, mCurX, mCurY, mCurZ;
	private static float mDrawScale, mCurScale;

	// current center tile
	private static long mTileX, mTileY;
	private static float mLastScale;
	private static byte mLastZoom;

	private static int rotateBuffers = 2;
	private static ShortBuffer shortBuffer[];
	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	// flag set by updateVisibleList when current visible tiles changed.
	// used in onDrawFrame to nextTiles to curTiles
	private static boolean mUpdateTiles;

	class TilesData {
		int cnt = 0;
		final GLMapTile[] tiles;

		TilesData(int numTiles) {
			tiles = new GLMapTile[numTiles];
		}
	}

	private static float[] mMVPMatrix = new float[16];

	// newTiles is set in updateVisibleList and swapped
	// with curTiles on main thread. curTiles is swapped
	// with drawTiles in onDrawFrame in GL thread.
	private static TilesData newTiles, curTiles, drawTiles;

	private static boolean mInitial;
	private static short mDrawCount = 0;

	/**
	 * 
	 */
	public boolean timing = false;

	@Override
	public void setRenderTheme(RenderTheme t) {
		int bg = t.getMapBackground();
		Log.d(TAG, "BG" + bg);
		float[] c = new float[4];
		c[0] = (bg >> 16 & 0xff) / 255.0f;
		c[1] = (bg >> 8 & 0xff) / 255.0f;
		c[2] = (bg >> 0 & 0xff) / 255.0f;
		c[3] = (bg >> 24 & 0xff) / 255.0f;
		mClearColor = c;
	}

	/**
	 * @param mapView
	 *            the MapView
	 */
	public MapRenderer(MapView mapView) {
		Log.d(TAG, "init MapRenderer");

		mMapView = mapView;

		if (mInitial)
			return;

		mDebugSettings = mapView.getDebugSettings();

		mVBOs = new ArrayList<VertexBufferObject>();
		mJobList = new ArrayList<MapGeneratorJob>();

		mTiles = new HashMap<TileCacheKey, GLMapTile>(CACHE_TILES * 2);
		mTileList = new ArrayList<GLMapTile>();
		mTileCacheKey = new TileCacheKey();
		mTilesLoaded = new ArrayList<GLMapTile>(30);

		mTileDistanceSort = new TileDistanceSort();
		Matrix.setIdentityM(mMVPMatrix, 0);
		mInitial = true;
		mUpdateTiles = false;
	}

	private static int updateTileDistances() {
		int h = (Tile.TILE_SIZE >> 1);
		byte zoom = mLastZoom;
		long x = mTileX + h;
		long y = mTileY + h;
		int diff;
		long dx, dy;
		int cnt = 0;

		// TODO this could need some fixing..
		// and be optimized to consider move/zoom direction
		for (int i = 0, n = mTileList.size(); i < n; i++) {
			GLMapTile t = mTileList.get(i);
			diff = (t.zoomLevel - zoom);
			if (t.isActive)
				cnt++;

			if (diff == 0) {
				dx = (t.pixelX + h) - x;
				dy = (t.pixelY + h) - y;
				t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)) * 0.25f;
				// t.distance = FloatMath.sqrt((dx * dx + dy * dy)) * 0.25f;
			} else if (diff > 0) {
				// tile zoom level is child of current
				dx = ((t.pixelX + h) >> diff) - x;
				dy = ((t.pixelY + h) >> diff) - y;

				// dy *= mAspect;
				t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)) * diff;
				// t.distance = FloatMath.sqrt((dx * dx + dy * dy)) * diff;

			} else {
				// tile zoom level is parent of current
				dx = ((t.pixelX + h) << -diff) - x;
				dy = ((t.pixelY + h) << -diff) - y;

				t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy))
						* (-diff * 0.5f);
				// t.distance = FloatMath.sqrt((dx * dx + dy * dy)) * (-diff * 0.5f);
			}

			// Log.d(TAG, diff + " " + (float) t.distance / Tile.TILE_SIZE + " " + t);
		}
		return cnt;
	}

	private static boolean childIsActive(GLMapTile t) {
		GLMapTile c = null;

		for (int i = 0; i < 4; i++) {
			c = t.child[i];
			if (c != null && c.isActive && !(c.isReady || c.newData))
				return true;
		}

		return false;
	}

	private static boolean tileInUse(GLMapTile t) {
		byte z = mLastZoom;

		if (t.zoomLevel == z + 1) {
			if (t.parent != null
					&& t.parent.isActive
					&& !(t.parent.isReady || t.parent.newData))
				return true;
		} else if (t.zoomLevel == z - 1) {
			if (childIsActive(t))
				return true;

		} else if (t.zoomLevel == z - 2) {
			for (int i = 0; i < 4; i++) {
				if (t.child[i] != null && childIsActive(t.child[i]))
					return true;
			}
		}
		return false;
	}

	private static void limitCache(int remove) {

		for (int j = mTileList.size() - 1, cnt = 0; cnt < remove && j > 0; j--) {

			GLMapTile t = mTileList.remove(j);

			synchronized (t) {
				// dont remove tile used by renderthread or mapgenerator
				// FIXME set tile loading state in main thread
				if (t.isLoading) {
					Log.d(TAG, "cancel loading " + t + " " + (t.zoomLevel - mCurZ)
							+ " " + (t.zoomLevel - mDrawZ) + " " + t.distance);
					t.isCanceled = true;
				} else if (t.isActive || t.isLoading) {
					Log.d(TAG, "X removing active " + t + " " + (t.zoomLevel - mCurZ)
							+ " " + (t.zoomLevel - mDrawZ) + " " + t.distance);
					mTileList.add(t);
					continue;
				} else if (t.isReady || t.newData) {
					// check if this tile is used as proxy for not yet drawn active tile

					if (tileInUse(t)) {
						Log.d(TAG, "X removing proxy: " + t);
						mTileList.add(t);
						continue;
					}
				}

				cnt++;
				mTileCacheKey.set(t.tileX, t.tileY, t.zoomLevel);
				mTiles.remove(mTileCacheKey);

				// clear references to this tile
				for (int i = 0; i < 4; i++) {
					if (t.child[i] != null)
						t.child[i].parent = null;
				}

				if (t.parent != null) {
					for (int i = 0; i < 4; i++) {
						if (t.parent.child[i] == t) {
							t.parent.child[i] = null;
							break;
						}
					}
				}
				clearTile(t);
			}
		}
	}

	private boolean updateVisibleList(double x, double y, int zdir) {
		byte zoomLevel = mLastZoom;
		float scale = mLastScale;
		double add = 1.0f / scale;
		int offsetX = (int) ((mWidth >> 1) * add);
		int offsetY = (int) ((mHeight >> 1) * add);

		long pixelRight = (long) x + offsetX;
		long pixelBottom = (long) y + offsetY;
		long pixelLeft = (long) x - offsetX;
		long pixelTop = (long) y - offsetY;

		long tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, zoomLevel) - 1;
		long tileTop = MercatorProjection.pixelYToTileY(pixelTop, zoomLevel) - 1;
		long tileRight = MercatorProjection.pixelXToTileX(pixelRight, zoomLevel) + 1;
		long tileBottom = MercatorProjection.pixelYToTileY(pixelBottom, zoomLevel) + 1;

		mJobList.clear();
		mJobParameter = mMapView.getJobParameters();

		int tiles = 0;
		if (newTiles == null)
			return false;

		int max = newTiles.tiles.length - 1;
		long limit = (long) Math.pow(2, zoomLevel);

		if (tileTop < 0)
			tileTop = 0;
		if (tileLeft < 0)
			tileLeft = 0;
		if (tileBottom >= limit)
			tileBottom = limit - 1;
		if (tileRight >= limit)
			tileRight = limit - 1;

		for (long yy = tileTop; yy <= tileBottom; yy++) {
			for (long xx = tileLeft; xx <= tileRight; xx++) {
				// FIXME
				if (tiles == max)
					break;

				long tx = xx;// % limit;

				GLMapTile tile = mTiles.get(mTileCacheKey.set(tx, yy, zoomLevel));

				if (tile == null) {
					tile = new GLMapTile(tx, yy, zoomLevel);
					TileCacheKey key = new TileCacheKey(mTileCacheKey);

					// FIXME use sparse matrix or sth.

					if (mTiles.put(key, tile) != null)
						Log.d(TAG, "eeek collision");
					mTileList.add(tile);

					mTileCacheKey.set((tx >> 1), (yy >> 1), (byte) (zoomLevel - 1));
					tile.parent = mTiles.get(mTileCacheKey);
					int idx = (int) ((tx & 0x01) + 2 * (yy & 0x01));

					// set this tile to be child of its parent
					if (tile.parent != null) {
						tile.parent.child[idx] = tile;
					} else if (zdir > 0 && zoomLevel > 0) {
						tile.parent = new GLMapTile(tx >> 1, yy >> 1,
								(byte) (zoomLevel - 1));
						key = new TileCacheKey(mTileCacheKey);
						if (mTiles.put(key, tile.parent) != null)
							Log.d(TAG, "eeek collision");
						mTileList.add(tile.parent);
						setTileChildren(tile.parent);
					}

					setTileChildren(tile);

				}

				newTiles.tiles[tiles++] = tile;

				if (!tile.isReady && !tile.newData && !tile.isLoading) {
					MapGeneratorJob job = new MapGeneratorJob(tile, mJobParameter,
							mDebugSettings);
					mJobList.add(job);
				}

				if (zdir > 0) {
					// prefetch parent
					if (tile.parent != null && !tile.parent.isReady
							&& !tile.parent.newData
							&& !tile.parent.isLoading) {
						MapGeneratorJob job = new MapGeneratorJob(tile.parent,
								mJobParameter,
								mDebugSettings);

						if (!mJobList.contains(job))
							mJobList.add(job);
					}
				}
			}
		}

		newTiles.cnt = tiles;

		// pass new tile list to glThread
		synchronized (this) {
			for (int i = 0; i < curTiles.cnt; i++) {
				boolean found = false;

				for (int j = 0; j < drawTiles.cnt && !found; j++)
					if (curTiles.tiles[i] == drawTiles.tiles[j])
						found = true;

				for (int j = 0; j < newTiles.cnt && !found; j++)
					if (curTiles.tiles[i] == newTiles.tiles[j])
						found = true;

				if (!found)
					curTiles.tiles[i].isActive = false;
			}

			for (int i = 0; i < tiles; i++)
				newTiles.tiles[i].isActive = true;

			TilesData tmp = curTiles;
			curTiles = newTiles;
			curTiles.cnt = tiles;
			newTiles = tmp;

			mCurX = x;
			mCurY = y;
			mCurZ = mLastZoom;
			mCurScale = mLastScale;

			mUpdateTiles = true;
		}

		updateTileDistances();

		if (mJobList.size() > 0)
			mMapView.addJobs(mJobList);

		int removes = mTiles.size() - CACHE_TILES;

		if (removes > 10) {
			Collections.sort(mTileList, mTileDistanceSort);
			limitCache(removes);
		}

		return true;
	}

	// private static ArrayList<GLMapTile> activeList = new ArrayList<GLMapTile>(100);

	private static void setTileChildren(GLMapTile tile) {

		long xx = tile.tileX << 1;
		long yy = tile.tileY << 1;
		byte z = (byte) (tile.zoomLevel + 1);

		tile.child[0] = mTiles.get(mTileCacheKey.set(xx, yy, z));
		tile.child[1] = mTiles.get(mTileCacheKey.set(xx + 1, yy, z));
		tile.child[2] = mTiles.get(mTileCacheKey.set(xx, yy + 1, z));
		tile.child[3] = mTiles.get(mTileCacheKey.set(xx + 1, yy + 1, z));

		// set this tile to be parent of its children
		for (int i = 0; i < 4; i++) {
			if (tile.child[i] != null)
				tile.child[i].parent = tile;
		}
	}

	private static void clearTile(GLMapTile t) {
		t.newData = false;
		t.isLoading = false;
		t.isReady = false;

		LineLayers.clear(t.lineLayers);
		PolygonLayers.clear(t.polygonLayers);

		t.labels = null;
		t.lineLayers = null;
		t.polygonLayers = null;

		if (t.vbo != null) {
			synchronized (mVBOs) {
				mVBOs.add(t.vbo);
			}
		}
		t.vbo = null;
	}

	/**
	 * called by MapView when position or map settings changes
	 */
	@Override
	public void redrawTiles(boolean clear) {
		boolean changedPos = false;
		boolean changedZoom = false;
		MapPosition mapPosition = mMapView.getMapPosition().getMapPosition();

		if (mapPosition == null) {
			Log.d(TAG, ">>> no map position");
			return;
		}

		if (clear) {
			mInitial = true;
			synchronized (this) {
				for (GLMapTile t : mTileList)
					clearTile(t);

				mTileList.clear();
				mTiles.clear();
				curTiles.cnt = 0;
				mBufferMemoryUsage = 0;
			}
		}

		byte zoomLevel = mapPosition.zoomLevel;
		float scale = mapPosition.scale;

		double x = MercatorProjection.longitudeToPixelX(
				mapPosition.geoPoint.getLongitude(), zoomLevel);
		double y = MercatorProjection.latitudeToPixelY(
				mapPosition.geoPoint.getLatitude(), zoomLevel);

		long tileX = MercatorProjection.pixelXToTileX(x, zoomLevel) * Tile.TILE_SIZE;
		long tileY = MercatorProjection.pixelYToTileY(y, zoomLevel) * Tile.TILE_SIZE;

		int zdir = 0;
		if (mInitial || mLastZoom != zoomLevel) {
			changedZoom = true;
			mLastScale = scale;
		}
		else if (tileX != mTileX || tileY != mTileY) {
			if (mLastScale - scale > 0 && scale > 1.2)
				zdir = 1;
			mLastScale = scale;
			changedPos = true;
		}
		else if (mLastScale - scale > 0.2 || mLastScale - scale < -0.2) {
			if (mLastScale - scale > 0 && scale > 1.2)
				zdir = 1;
			mLastScale = scale;
			changedPos = true;
		}
		mInitial = false;

		mTileX = tileX;
		mTileY = tileY;
		mLastZoom = zoomLevel;

		// if (zdir > 0)
		// Log.d(TAG, "prefetch parent");

		if (changedZoom) {
			// need to update visible list first when zoom level changes
			// as scaling is relative to the tiles of current zoom-level
			updateVisibleList(x, y, 0);
		} else {
			// pass new position to glThread
			synchronized (this) {
				// do not change position while drawing
				mCurX = x;
				mCurY = y;
				mCurZ = zoomLevel;
				mCurScale = scale;
				// Log.d(TAG, "draw at:" + tileX + " " + tileY + " " + mCurX + " " + mCurY
				// + " " + mCurZ);
			}
		}

		if (!timing)
			mMapView.requestRender();

		if (changedPos)
			updateVisibleList(x, y, zdir);

		int size = mTilesLoaded.size();
		if (size < MAX_TILES_IN_QUEUE)
			return;

		synchronized (mTilesLoaded) {

			// remove uploaded tiles
			for (int i = 0; i < size;) {
				GLMapTile t = mTilesLoaded.get(i);

				if (!t.newData) {
					mTilesLoaded.remove(i);
					size--;
					continue;
				}
				i++;
			}

			// clear loaded but not used tiles
			if (size > MAX_TILES_IN_QUEUE) {
				while (size-- > MAX_TILES_IN_QUEUE - 20) {
					GLMapTile t = mTilesLoaded.get(size);

					synchronized (t) {
						if (tileInUse(t)) {
							// Log.d(TAG, "keep unused tile data: " + t + " " + t.isActive);
							continue;
						}
						mTilesLoaded.remove(size);
						// Log.d(TAG, "remove unused tile data: " + t);

						clearTile(t);
					}
				}
			}
		}
	}

	private static final int MAX_TILES_IN_QUEUE = 40;

	/**
	 * called by MapWorkers when tile is loaded
	 */
	@Override
	public synchronized boolean passTile(MapGeneratorJob mapGeneratorJob) {
		GLMapTile tile = (GLMapTile) mapGeneratorJob.tile;

		// if (tile.isCanceled) {
		// // no one should be able to use this tile now, mapgenerator passed it,
		// // glthread does nothing until newdata is set.
		// Log.d(TAG, "passTile: canceled " + tile);
		// clearTile(tile);
		// return true;
		// }

		tile.newData = true;
		tile.isLoading = false;

		if (!timing)
			mMapView.requestRender();

		synchronized (mTilesLoaded) {
			mTilesLoaded.add(0, tile);
		}

		return true;
	}

	private static void setMatrix(GLMapTile tile, float div) {
		float x, y, scale;

		scale = (float) (2.0 * mDrawScale / (mHeight * div));
		x = (float) (tile.x - mDrawX * div);
		y = (float) (tile.y - mDrawY * div);

		mMVPMatrix[12] = x * scale * mAspect;
		mMVPMatrix[13] = -y * scale;
		mMVPMatrix[0] = scale * mAspect / COORD_MULTIPLIER;
		mMVPMatrix[5] = scale / COORD_MULTIPLIER;
	}

	private static boolean setTileScissor(GLMapTile tile, float div) {
		double dx, dy, scale;

		if (div == 0) {
			dx = tile.pixelX - mDrawX;
			dy = tile.pixelY - mDrawY;
			scale = mDrawScale;
		} else {
			dx = tile.pixelX - mDrawX * div;
			dy = tile.pixelY - mDrawY * div;
			scale = mDrawScale / div;
		}
		int size = Tile.TILE_SIZE;
		int sx = (int) (dx * scale);
		int sy = (int) (dy * scale);

		if (sy > mHeight / 2 || sx > mWidth / 2
				|| sx + size * scale < -mWidth / 2
				|| sy + size * scale < -mHeight / 2) {
			tile.isVisible = false;
			return false;
		}

		tile.isVisible = true;

		return true;
	}

	private int uploadCnt = 0;

	private boolean uploadTileData(GLMapTile tile) {
		ShortBuffer sbuf = null;

		// use multiple buffers to avoid overwriting buffer while current
		// data is uploaded (or rather the blocking which is probably done to avoid this)
		if (uploadCnt >= rotateBuffers) {
			uploadCnt = 0;
			GLES20.glFlush();
		}

		// Upload line data to vertex buffer object
		synchronized (tile) {
			if (!tile.newData)
				return false;

			synchronized (mVBOs) {
				if (tile.vbo == null) {
					if (mVBOs.size() < 1) {
						Log.d(TAG, "uploadTileData, no VBOs left");
						return false;
					}
					tile.vbo = mVBOs.remove(mVBOs.size() - 1);
				}
			}

			int lineSize = LineLayers.sizeOf(tile.lineLayers);
			int polySize = PolygonLayers.sizeOf(tile.polygonLayers);
			int newSize = lineSize + polySize;

			if (newSize == 0) {
				LineLayers.clear(tile.lineLayers);
				PolygonLayers.clear(tile.polygonLayers);
				tile.lineLayers = null;
				tile.polygonLayers = null;
				return false;
			}

			// Log.d(TAG, "uploadTileData, " + tile);

			glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

			sbuf = shortBuffer[uploadCnt];

			// add fill coordinates
			newSize += 8;

			// FIXME probably not a good idea to do this in gl thread...
			if (sbuf == null || sbuf.capacity() < newSize) {
				ByteBuffer bbuf = ByteBuffer.allocateDirect(newSize * SHORT_BYTES).order(
						ByteOrder.nativeOrder());
				sbuf = bbuf.asShortBuffer();
				shortBuffer[uploadCnt] = sbuf;
				sbuf.put(mFillCoords, 0, 8);
			}

			sbuf.clear();
			sbuf.position(8);

			PolygonLayers.compileLayerData(tile.polygonLayers, sbuf);
			LineLayers.compileLayerData(tile.lineLayers, sbuf);
			sbuf.flip();

			newSize *= SHORT_BYTES;

			if (tile.vbo.size > newSize && tile.vbo.size < newSize * 4) {
				GLES20.glBufferSubData(GL_ARRAY_BUFFER, 0, newSize, sbuf);
			} else {
				mBufferMemoryUsage -= tile.vbo.size;
				tile.vbo.size = newSize;
				glBufferData(GL_ARRAY_BUFFER, tile.vbo.size, sbuf, GL_DYNAMIC_DRAW);
				mBufferMemoryUsage += tile.vbo.size;
			}
			tile.lineOffset = (8 + polySize) * SHORT_BYTES;

			// tile.isLoading = false;

			uploadCnt++;

			tile.isReady = true;
			tile.newData = false;
		}
		return true;
	}

	private float[] mClearColor = null;
	private static long mRedrawCnt = 0;

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0, poly_time = 0, clear_time = 0;

		glClear(GLES20.GL_COLOR_BUFFER_BIT
				| GLES20.GL_DEPTH_BUFFER_BIT
		// | GLES20.GL_STENCIL_BUFFER_BIT
		);

		if (mInitial)
			return;

		if (timing)
			start = SystemClock.uptimeMillis();

		// get position and current tiles to draw
		synchronized (this) {
			mDrawX = mCurX;
			mDrawY = mCurY;
			mDrawZ = mCurZ;
			mDrawScale = mCurScale;

			if (mUpdateTiles) {
				TilesData tmp = drawTiles;
				drawTiles = curTiles;
				curTiles = tmp;
				mUpdateTiles = false;
			}
		}

		int tileCnt = drawTiles.cnt;
		GLMapTile[] tiles = drawTiles.tiles;

		// not really tested, try to clear some vbo when exceding limit
		if (mBufferMemoryUsage > LIMIT_BUFFERS) {
			Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / MB + "MB");

			synchronized (mVBOs) {
				for (VertexBufferObject vbo : mVBOs) {
					if (vbo.size == 0)
						continue;

					mBufferMemoryUsage -= vbo.size;
					glBindBuffer(GL_ARRAY_BUFFER, vbo.id);
					glBufferData(GL_ARRAY_BUFFER, 0, null, GL_DYNAMIC_DRAW);
					vbo.size = 0;
				}
				glBindBuffer(GL_ARRAY_BUFFER, 0);
			}
			Log.d(TAG, " > " + mBufferMemoryUsage / MB + "MB");

			if (CACHE_TILES > 100)
				CACHE_TILES -= 50;
		} else if (CACHE_TILES < CACHE_TILES_MAX) {
			CACHE_TILES += 50;
		}

		uploadCnt = 0;
		int updateTextures = 0;

		// check visible tiles, upload new vertex data
		for (int i = 0; i < tileCnt; i++) {
			GLMapTile tile = tiles[i];

			if (!setTileScissor(tile, 1))
				continue;

			if (tile.texture == null && TextRenderer.drawToTexture(tile))
				updateTextures++;

			if (tile.newData) {
				uploadTileData(tile);
				continue;
			}

			if (!tile.isReady) {
				if (tile.parent != null && tile.parent.newData) {
					uploadTileData(tile.parent);
				} else {
					if (tile.child[0] != null && tile.child[0].newData)
						uploadTileData(tile.child[0]);
					if (tile.child[1] != null && tile.child[1].newData)
						uploadTileData(tile.child[1]);
					if (tile.child[2] != null && tile.child[2].newData)
						uploadTileData(tile.child[2]);
					if (tile.child[3] != null && tile.child[3].newData)
						uploadTileData(tile.child[3]);
				}
			}
		}

		if (updateTextures > 0)
			TextRenderer.compileTextures();

		// if (GlUtils.checkGlOutOfMemory("upload: " + mBufferMemoryUsage)
		// && LIMIT_BUFFERS > MB)
		// LIMIT_BUFFERS -= MB;

		if (timing)
			clear_time = (SystemClock.uptimeMillis() - start);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		glEnable(GLES20.GL_POLYGON_OFFSET_FILL);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && tiles[i].isReady) {
				drawTile(tiles[i], 1);
			}
		}

		// proxies are clipped to the region where nothing was drawn to depth buffer
		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && !tiles[i].isReady) {
				drawProxyTile(tiles[i]);
			}
		}

		glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
		glDisable(GLES20.GL_DEPTH_TEST);

		mDrawCount = 0;

		glEnable(GL_BLEND);

		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		int zoomLevelDiff = Math
				.max((int) mDrawZ - MapGenerator.STROKE_MAX_ZOOM_LEVEL, 0);
		float scale = (float) Math.pow(1.4, zoomLevelDiff);
		if (scale < 1)
			scale = 1;

		if (mDrawZ >= MapGenerator.STROKE_MAX_ZOOM_LEVEL)
			TextRenderer.beginDraw(FloatMath.sqrt(mDrawScale) / scale);
		else
			TextRenderer.beginDraw(mDrawScale);

		for (int i = 0; i < tileCnt; i++) {
			if (!tiles[i].isVisible || tiles[i].texture == null)
				continue;

			setMatrix(tiles[i], 1);
			TextRenderer.drawTile(tiles[i], mMVPMatrix);
		}
		TextRenderer.endDraw();

		if (timing) {
			glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start)
					+ " " + clear_time + " " + poly_time);
		}

		mRedrawCnt++;
	}

	private static void drawTile(GLMapTile tile, float div) {

		if (tile.lastDraw == mRedrawCnt)
			return;

		tile.lastDraw = mRedrawCnt;

		// if (div != 1)
		// Log.d(TAG, "draw proxy " + div + " " + tile);

		setMatrix(tile, div);

		glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);
		LineLayer ll = tile.lineLayers;
		PolygonLayer pl = tile.polygonLayers;

		boolean clipped = false;

		// GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		// glEnable(GLES20.GL_POLYGON_OFFSET_FILL);

		for (; pl != null || ll != null;) {
			int lnext = Integer.MAX_VALUE;
			int pnext = Integer.MAX_VALUE;

			if (ll != null)
				lnext = ll.layer;

			if (pl != null)
				pnext = pl.layer;

			if (pl != null && pnext < lnext) {
				glDisable(GL_BLEND);

				pl = PolygonLayers.drawPolygons(pl, lnext, mMVPMatrix, mDrawZ,
						mDrawScale, mDrawCount, !clipped);

				if (!clipped) {
					clipped = true;
					// glDisable(GLES20.GL_DEPTH_TEST);
				}
			} else {
				// nastay
				if (!clipped)
					PolygonLayers.drawPolygons(null, 0, mMVPMatrix, mDrawZ,
							mDrawScale, mDrawCount, true);
				// else {
				// GLES20.glEnable(GLES20.GL_DEPTH_TEST);
				// glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
				// GLES20.glPolygonOffset(0, mDrawCount);
				// }
				glEnable(GL_BLEND);

				ll = LineLayers.drawLines(tile, ll, pnext, mMVPMatrix, div,
						mDrawZ, mDrawScale);

				// glDisable(GLES20.GL_DEPTH_TEST);
				// glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
			}
		}

		// glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
		// glDisable(GLES20.GL_DEPTH_TEST);

		mDrawCount++;
	}

	private static boolean drawProxyChild(GLMapTile tile) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			GLMapTile c = tile.child[i];
			if (c != null) {
				if (!setTileScissor(c, 2)) {
					drawn++;
					continue;
				}

				if (c.isReady) {
					drawTile(c, 2);
					drawn++;
				}
			}
		}
		return drawn == 4;
	}

	private static void drawProxyTile(GLMapTile tile) {
		// if (mDrawScale < 1.5f) {
		// if (!drawProxyChild(tile)) {
		// if (tile.parent != null) {
		// if (tile.parent.isReady) {
		// drawTile(tile.parent, 0.5f);
		// } else {
		// GLMapTile p = tile.parent.parent;
		// if (p != null && p.isReady)
		// drawTile(p, 0.25f);
		// }
		// }
		// }
		// } else {
		if (tile.parent != null && tile.parent.isReady) {
			drawTile(tile.parent, 0.5f);
		} else if (!drawProxyChild(tile)) {
			if (tile.parent != null) {
				GLMapTile p = tile.parent.parent;
				if (p != null && p.isReady)
					drawTile(p, 0.25f);
			}
		}
		// }
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GlUtils.checkGlError("onSurfaceChanged");

		mVBOs.clear();
		mTiles.clear();
		mTileList.clear();
		ShortPool.finish();
		// LineLayers.finish();

		drawTiles = newTiles = curTiles = null;
		mBufferMemoryUsage = 0;
		mInitial = true;

		if (width <= 0 || height <= 0)
			return;

		// STENCIL_BITS = GlConfigChooser.stencilSize;

		mWidth = width;
		mHeight = height;
		mAspect = (float) height / width;

		glViewport(0, 0, width, height);
		GlUtils.checkGlError("glViewport");

		glScissor(0, 0, mWidth, mHeight);

		int numTiles = (mWidth / (Tile.TILE_SIZE / 2) + 2)
				* (mHeight / (Tile.TILE_SIZE / 2) + 2);

		drawTiles = new TilesData(numTiles);
		newTiles = new TilesData(numTiles);
		curTiles = new TilesData(numTiles);

		Log.d(TAG, "using: " + numTiles + " + cache: " + CACHE_TILES);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + numTiles);
		int[] mVboIds = new int[numVBO];
		glGenBuffers(numVBO, mVboIds, 0);
		GlUtils.checkGlError("glGenBuffers");

		for (int i = 1; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		// Set up textures
		TextRenderer.init(numTiles);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GLES20.GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_DITHER);

		if (mClearColor != null) {

			glClearColor(mClearColor[0], mClearColor[1], mClearColor[2],
					mClearColor[3]);
		} else {
			glClearColor(0.98f, 0.98f, 0.97f, 1.0f);
		}
		glClearStencil(0);
		glClear(GL_STENCIL_BUFFER_BIT);

		// glClear(GL_STENCIL_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		// glEnable(GLES20.GL_DEPTH_TEST);

		glEnable(GL_SCISSOR_TEST);

		mDebugSettings = mMapView.getDebugSettings();
		mJobParameter = mMapView.getJobParameters();

		mMapView.redrawTiles();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		Log.d(TAG, "Extensions: " + ext);

		// if (ext.indexOf("GL_OES_vertex_half_float") >= 0) {
		// useHalfFloat = true;
		shortBuffer = new ShortBuffer[rotateBuffers];

		// add half pixel to tile clip/fill coordinates
		// to avoid rounding issues
		short min = -4;
		short max = (short) ((Tile.TILE_SIZE << 3) + 4);
		mFillCoords = new short[8];
		mFillCoords[0] = min;
		mFillCoords[1] = max;
		mFillCoords[2] = max;
		mFillCoords[3] = max;
		mFillCoords[4] = min;
		mFillCoords[5] = min;
		mFillCoords[6] = max;
		mFillCoords[7] = min;

		LineLayers.init();
		PolygonLayers.init();

		GlUtils.checkGlError("onSurfaceCreated");

	}

	@Override
	public boolean processedTile() {
		return true;
	}

	@Override
	public IMapGenerator createMapGenerator() {
		return new MapGenerator();

	}
}
