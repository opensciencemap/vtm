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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.MapTile;
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

public class MapRenderer implements org.mapsforge.android.IMapRenderer {
	private static final String TAG = "MapRenderer";

	private static final int MB = 1024 * 1024;
	private static final int SHORT_BYTES = 2;
	static final float COORD_MULTIPLIER = 8.0f;

	private static final int MAX_TILES_IN_QUEUE = 40;
	private static final int CACHE_TILES_MAX = 250;
	private static final int LIMIT_BUFFERS = 16 * MB;

	private static int CACHE_TILES = CACHE_TILES_MAX;

	private final MapView mMapView;
	private static ArrayList<MapTile> mJobList;
	private static ArrayList<VertexBufferObject> mVBOs;

	// all tiles currently referenced
	private static ArrayList<GLMapTile> mTiles;

	// tiles that have new data to upload, see passTile()
	private static ArrayList<GLMapTile> mTilesLoaded;

	private static int mWidth, mHeight;
	private static float mAspect;

	// current center tile, values used to check if position has
	// changed for updating current tile list
	private static long mTileX, mTileY;
	private static float mPrevScale;
	private static byte mPrevZoom;

	private static int rotateBuffers = 2;
	private static ShortBuffer shortBuffer[];
	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	class TilesData {
		int cnt = 0;
		final GLMapTile[] tiles;

		TilesData(int numTiles) {
			tiles = new GLMapTile[numTiles];
		}
	}

	private static float[] mMVPMatrix = new float[16];
	// private static float[] mRotateMatrix = new float[16];
	private static float[] mProjMatrix = new float[16];

	// newTiles is set in updateVisibleList and swapped
	// with curTiles on main thread. curTiles is swapped
	// with drawTiles in onDrawFrame in GL thread.
	private static TilesData newTiles, curTiles, drawTiles;

	// draw position is updated from current position in onDrawFrame
	// keeping the position consistent while drawing
	private static MapPosition mCurPosition, mDrawPosition;

	// flag set by updateVisibleList when current visible tiles
	// changed. used in onDrawFrame to flip curTiles/drawTiles
	private static boolean mUpdateTiles;

	private static boolean mInitial;
	private static short mDrawCount = 0;

	private float[] mClearColor = null;
	private static long mRedrawCnt = 0;
	private static boolean mUpdateColor = false;

	/**
	 * @param mapView
	 *            the MapView
	 */
	public MapRenderer(MapView mapView) {
		Log.d(TAG, "init MapRenderer");

		mMapView = mapView;

		if (mInitial)
			return;

		mJobList = new ArrayList<MapTile>();
		mTiles = new ArrayList<GLMapTile>();
		mTilesLoaded = new ArrayList<GLMapTile>(30);

		Matrix.setIdentityM(mMVPMatrix, 0);

		mInitial = true;
		mUpdateTiles = false;

		TreeTile.init();
	}

	private static int updateTileDistances(ArrayList<?> tiles,
			MapPosition mapPosition) {
		int h = (Tile.TILE_SIZE >> 1);
		byte zoom = mapPosition.zoomLevel;
		long x = mTileX + h;
		long y = mTileY + h;
		int diff;
		long dx, dy;
		int cnt = 0;

		// TODO this could need some fixing..
		// and be optimized to consider move/zoom direction
		for (int i = 0, n = tiles.size(); i < n; i++) {
			MapTile t = (MapTile) tiles.get(i);
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
			if (t.rel.child[i] == null)
				continue;

			c = t.rel.child[i].tile;
			if (c != null && c.isActive && !(c.isReady || c.newData))
				return true;
		}

		return false;
	}

	// FIXME still the chance that one jumped two zoomlevels between cur and draw...
	// and this is a bit heavy in the first place
	private static boolean tileInUse(GLMapTile t) {
		byte z = mPrevZoom;

		if (t.isActive) {
			return true;
		} else if (t.zoomLevel == z + 1) {
			GLMapTile p = t.rel.parent.tile;

			if (p != null && p.isActive && !(p.isReady || p.newData))
				return true;
		} else if (t.zoomLevel == z + 2) {
			GLMapTile p = t.rel.parent.parent.tile;

			if (p != null && p.isActive && !(p.isReady || p.newData))
				return true;
		} else if (t.zoomLevel == z - 1) {
			if (childIsActive(t))
				return true;
		} else if (t.zoomLevel == z - 2) {
			for (int i = 0; i < 4; i++) {
				if (t.rel.child[i] == null)
					continue;

				GLMapTile child = t.rel.child[i].tile;
				if (child != null && childIsActive(child))
					return true;
			}
		} else if (t.zoomLevel == z - 3) {
			for (int i = 0; i < 4; i++) {
				if (t.rel.child[i] == null)
					continue;
				TreeTile c = t.rel.child[i];

				for (int j = 0; j < 4; j++) {
					if (c.child[j] == null)
						continue;
					GLMapTile child = c.child[j].tile;
					if (child != null && childIsActive(child))
						return true;
				}
			}
		}
		return false;
	}

	private static void limitCache(int remove) {

		for (int j = mTiles.size() - 1, cnt = 0; cnt < remove && j > 0; j--) {

			GLMapTile t = mTiles.remove(j);

			synchronized (t) {

				if (t.isActive) {
					// dont remove tile used by renderthread or mapgenerator
					Log.d(TAG, "X not removing active " + t + " " + t.distance);
					mTiles.add(t);
				} else if ((t.isReady || t.newData) && tileInUse(t)) {
					// check if this tile could be used as proxy
					// for not yet drawn active tile
					Log.d(TAG, "X not removing proxy: " + t);
					mTiles.add(t);
				} else {
					if (t.isLoading) {
						Log.d(TAG, ">>> cancel loading " + t + " " + t.distance);
						t.isCanceled = true;
					}

					clearTile(t);
					cnt++;
				}
			}
		}
	}

	private boolean updateVisibleList(MapPosition mapPosition, int zdir) {
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
		mMapView.addJobs(null);

		int tiles = 0;
		if (newTiles == null)
			return false;

		int max = newTiles.tiles.length - 1;

		for (int yy = tileTop; yy <= tileBottom; yy++) {
			for (int xx = tileLeft; xx <= tileRight; xx++) {

				if (tiles == max)
					break;

				GLMapTile tile = TreeTile.getTile(xx, yy, zoomLevel);

				if (tile == null) {
					tile = new GLMapTile(xx, yy, zoomLevel);

					TreeTile.add(tile);
					mTiles.add(tile);
				}

				newTiles.tiles[tiles++] = tile;

				if (!(tile.isLoading || tile.newData || tile.isReady)) {
					mJobList.add(tile);
				}

				if (zdir > 0 && zoomLevel > 0) {
					// prefetch parent
					GLMapTile parent = tile.rel.parent.tile;

					if (parent == null) {
						parent = new GLMapTile(xx >> 1, yy >> 1, (byte) (zoomLevel - 1));

						TreeTile.add(parent);
						mTiles.add(parent);
					}

					if (!(parent.isLoading || parent.isReady || parent.newData)) {
						if (!mJobList.contains(parent))
							mJobList.add(parent);
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

			mCurPosition = mapPosition;

			mUpdateTiles = true;
		}

		if (mJobList.size() > 0) {
			updateTileDistances(mJobList, mapPosition);
			Collections.sort(mJobList);
			mMapView.addJobs(mJobList);
		}

		int removes = mTiles.size() - CACHE_TILES;

		if (removes > 10) {
			updateTileDistances(mTiles, mapPosition);
			Collections.sort(mTiles);
			limitCache(removes);
		}

		return true;
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
			t.vbo = null;
		}

		TreeTile.remove(t);
	}

	/**
	 * called by MapView when position or map settings changes
	 */
	@Override
	public synchronized void redrawTiles(boolean clear) {
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

				for (GLMapTile t : mTiles)
					clearTile(t);

				mTiles.clear();
				mTilesLoaded.clear();
				TreeTile.init();
				curTiles.cnt = 0;
				mBufferMemoryUsage = 0;
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
			mCurPosition = mapPosition;
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
			synchronized (this) {
				// do not change position while drawing
				mCurPosition = mapPosition;
			}
		}

		if (!MapView.debugFrameTime)
			mMapView.requestRender();

		if (changedPos)
			updateVisibleList(mapPosition, zdir);

		synchronized (mTilesLoaded) {
			int size = mTilesLoaded.size();
			if (size < MAX_TILES_IN_QUEUE)
				return;

			// remove uploaded tiles
			for (int i = 0; i < size;) {
				GLMapTile t = mTilesLoaded.get(i);
				// rel == null means tile is already removed by limitCache
				if (!t.newData || t.rel == null) {
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
						if (t.rel == null) {
							mTilesLoaded.remove(size);
							continue;
						}

						if (tileInUse(t)) {
							// Log.d(TAG, "keep unused tile data: " + t + " " + t.isActive);
							continue;
						}

						mTilesLoaded.remove(size);
						mTiles.remove(t);
						// Log.d(TAG, "remove unused tile data: " + t);
						clearTile(t);
					}
				}
			}
		}
	}

	/**
	 * called by MapWorkers when tile is loaded
	 */
	@Override
	public synchronized boolean passTile(MapTile mapTile) {
		GLMapTile tile = (GLMapTile) mapTile;

		if (tile.isCanceled) {
			// no one should be able to use this tile now, mapgenerator passed it,
			// glthread does nothing until newdata is set.
			Log.d(TAG, "passTile: canceled " + tile);
			tile.isLoading = false;
			return true;
		}

		synchronized (mVBOs) {
			int numVBOs = mVBOs.size();

			if (numVBOs > 0 && tile.vbo == null) {
				tile.vbo = mVBOs.remove(numVBOs - 1);
			}

			if (tile.vbo == null) {
				Log.d(TAG, "no VBOs left for " + tile);
				tile.isLoading = false;
				return false;
			}
		}

		tile.newData = true;
		tile.isLoading = false;

		if (!MapView.debugFrameTime)
			mMapView.requestRender();

		synchronized (mTilesLoaded) {
			mTilesLoaded.add(0, tile);
		}

		return true;
	}

	private static void setMatrix(GLMapTile tile, float div) {
		float x, y, scale;

		scale = (float) (2.0 * mDrawPosition.scale / (mHeight * div));
		x = (float) (tile.pixelX - mDrawPosition.x * div);
		y = (float) (tile.pixelY - mDrawPosition.y * div);

		mMVPMatrix[12] = x * scale * mAspect;
		mMVPMatrix[13] = -(y + Tile.TILE_SIZE) * scale;
		mMVPMatrix[0] = scale * mAspect / COORD_MULTIPLIER;
		mMVPMatrix[5] = scale / COORD_MULTIPLIER;

		// Matrix.setIdentityM(mMVPMatrix, 0);
		//
		// Matrix.scaleM(mMVPMatrix, 0, scale / COORD_MULTIPLIER,
		// scale / COORD_MULTIPLIER, 1);
		//
		// Matrix.translateM(mMVPMatrix, 0, x * COORD_MULTIPLIER, -(y + Tile.TILE_SIZE)
		// * COORD_MULTIPLIER, 0);
		//
		// Matrix.setRotateM(mRotateMatrix, 0, mDrawPosition.angle, 0, 0, 1);
		//
		// Matrix.multiplyMM(mMVPMatrix, 0, mRotateMatrix, 0, mMVPMatrix, 0);
		//
		// Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

	}

	private static boolean setTileScissor(GLMapTile tile, float div) {
		double dx, dy, scale;

		if (div == 0) {
			dx = tile.pixelX - mDrawPosition.x;
			dy = tile.pixelY - mDrawPosition.y;
			scale = mDrawPosition.scale;
		} else {
			dx = tile.pixelX - mDrawPosition.x * div;
			dy = tile.pixelY - mDrawPosition.y * div;
			scale = mDrawPosition.scale / div;
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

			int lineSize = LineLayers.sizeOf(tile.lineLayers);
			int polySize = PolygonLayers.sizeOf(tile.polygonLayers);
			int newSize = lineSize + polySize;

			if (newSize == 0) {
				LineLayers.clear(tile.lineLayers);
				PolygonLayers.clear(tile.polygonLayers);
				tile.lineLayers = null;
				tile.polygonLayers = null;
				tile.newData = false;
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

			tile.lineOffset = (8 + polySize);
			if (tile.lineOffset != sbuf.position())
				Log.d(TAG, "tiles lineoffset is wrong: " + tile + " "
						+ tile.lineOffset + " "
						+ sbuf.position() + " "
						+ sbuf.limit() + " "
						+ sbuf.remaining() + " "
						+ PolygonLayers.sizeOf(tile.polygonLayers) + " "
						+ tile.isCanceled + " "
						+ tile.isLoading + " "
						+ tile.rel);

			tile.lineOffset *= SHORT_BYTES;

			LineLayers.compileLayerData(tile.lineLayers, sbuf);

			sbuf.flip();

			if (newSize != sbuf.remaining())
				Log.d(TAG, "tiles wrong: " + tile + " "
						+ newSize + " "
						+ sbuf.position() + " "
						+ sbuf.limit() + " "
						+ sbuf.remaining() + " "
						+ LineLayers.sizeOf(tile.lineLayers)
						+ tile.isCanceled + " "
						+ tile.isLoading + " "
						+ tile.rel);

			newSize *= SHORT_BYTES;

			if (tile.vbo.size > newSize && tile.vbo.size < newSize * 4
					&& mBufferMemoryUsage < LIMIT_BUFFERS) {
				GLES20.glBufferSubData(GL_ARRAY_BUFFER, 0, newSize, sbuf);
			} else {
				mBufferMemoryUsage -= tile.vbo.size;
				tile.vbo.size = newSize;
				glBufferData(GL_ARRAY_BUFFER, tile.vbo.size, sbuf, GL_DYNAMIC_DRAW);
				mBufferMemoryUsage += tile.vbo.size;
			}

			uploadCnt++;

			tile.isReady = true;
			tile.newData = false;
		}
		return true;
	}

	private static void checkBufferUsage() {
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
			}

			glBindBuffer(GL_ARRAY_BUFFER, 0);
			Log.d(TAG, " > " + mBufferMemoryUsage / MB + "MB");

			if (mBufferMemoryUsage > LIMIT_BUFFERS && CACHE_TILES > 100)
				CACHE_TILES -= 50;

		} else if (CACHE_TILES < CACHE_TILES_MAX) {
			CACHE_TILES += 50;
		}
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0;

		if (MapView.debugFrameTime)
			start = SystemClock.uptimeMillis();

		if (mUpdateColor && mClearColor != null) {
			glClearColor(mClearColor[0], mClearColor[1],
					mClearColor[2], mClearColor[3]);
			mUpdateColor = false;
		}

		glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if (mInitial)
			return;

		// get position and current tiles to draw
		synchronized (this) {
			mDrawPosition = mCurPosition;

			if (mUpdateTiles) {
				TilesData tmp = drawTiles;
				drawTiles = curTiles;
				curTiles = tmp;
				mUpdateTiles = false;
			}
		}

		checkBufferUsage();

		int tileCnt = drawTiles.cnt;
		GLMapTile[] tiles = drawTiles.tiles;

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
				// check near relatives if they can serve as proxy
				GLMapTile rel = tile.rel.parent.tile;
				if (rel != null && rel.newData) {
					uploadTileData(rel);
				} else {
					for (int c = 0; c < 4; c++) {
						if (tile.rel.child[c] == null)
							continue;

						rel = tile.rel.child[c].tile;
						if (rel != null && rel.newData)
							uploadTileData(rel);
					}
				}
			}
		}

		if (updateTextures > 0)
			TextRenderer.compileTextures();

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		glEnable(GLES20.GL_POLYGON_OFFSET_FILL);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && tiles[i].isReady) {
				drawTile(tiles[i], 1);
			}
		}

		// proxies are clipped to the region where nothing was drawn to depth buffer
		// TODO draw all parent before grandparent
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

		int z = mDrawPosition.zoomLevel;
		float s = mDrawPosition.scale;

		int zoomLevelDiff = Math
				.max(z - MapGenerator.STROKE_MAX_ZOOM_LEVEL, 0);
		float scale = (float) Math.pow(1.4, zoomLevelDiff);
		if (scale < 1)
			scale = 1;

		if (z >= MapGenerator.STROKE_MAX_ZOOM_LEVEL)
			TextRenderer.beginDraw(FloatMath.sqrt(s) / scale);
		else
			TextRenderer.beginDraw(s);

		for (int i = 0; i < tileCnt; i++) {
			if (!tiles[i].isVisible || tiles[i].texture == null)
				continue;

			setMatrix(tiles[i], 1);
			TextRenderer.drawTile(tiles[i], mMVPMatrix);
		}
		TextRenderer.endDraw();

		if (MapView.debugFrameTime) {
			glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start));
		}

		mRedrawCnt++;
	}

	private static void drawTile(GLMapTile tile, float div) {

		if (tile.lastDraw == mRedrawCnt)
			return;

		int z = mDrawPosition.zoomLevel;
		float s = mDrawPosition.scale;

		tile.lastDraw = mRedrawCnt;

		setMatrix(tile, div);

		glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);
		GLES20.glPolygonOffset(0, mDrawCount);

		LineLayer ll = tile.lineLayers;
		PolygonLayer pl = tile.polygonLayers;

		boolean clipped = false;

		for (; pl != null || ll != null;) {
			int lnext = Integer.MAX_VALUE;
			int pnext = Integer.MAX_VALUE;

			if (ll != null)
				lnext = ll.layer;

			if (pl != null)
				pnext = pl.layer;

			if (pl != null && pnext < lnext) {
				glDisable(GL_BLEND);

				pl = PolygonLayers.drawPolygons(pl, lnext, mMVPMatrix, z, s, !clipped);

				if (!clipped) {
					clipped = true;
				}
			} else {
				// XXX nastay
				if (!clipped) {
					PolygonLayers.drawPolygons(null, 0, mMVPMatrix, z, s, true);
					clipped = true;
				}
				glEnable(GL_BLEND);

				ll = LineLayers.drawLines(tile, ll, pnext, mMVPMatrix, div, z, s);
			}
		}

		mDrawCount++;
	}

	private static boolean drawProxyChild(GLMapTile tile) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if (tile.rel.child[i] == null)
				continue;

			GLMapTile c = tile.rel.child[i].tile;
			if (c == null)
				continue;

			if (!setTileScissor(c, 2)) {
				drawn++;
				continue;
			}

			if (c.isReady) {
				drawTile(c, 2);
				drawn++;
			}
		}
		return drawn == 4;
	}

	private static void drawProxyTile(GLMapTile tile) {

		if (mDrawPosition.scale > 1.5f) {
			// prefer drawing children
			if (!drawProxyChild(tile)) {
				GLMapTile t = tile.rel.parent.tile;
				if (t != null) {
					if (t.isReady) {
						drawTile(t, 0.5f);
					} else {
						GLMapTile p = t.rel.parent.tile;
						if (p != null && p.isReady)
							drawTile(p, 0.25f);
					}
				}
			}
		} else {
			// prefer drawing parent
			GLMapTile t = tile.rel.parent.tile;

			if (t != null && t.isReady) {
				drawTile(t, 0.5f);

			} else if (!drawProxyChild(tile)) {

				// need to check rel.parent here, t could alread be root
				if (t != null) {
					t = t.rel.parent.tile;

					if (t != null && t.isReady)
						drawTile(t, 0.25f);
				}
			}
		}
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		Log.d(TAG, "SurfaceChanged:" + width + " " + height);
		mTilesLoaded.clear();
		mTiles.clear();

		ShortPool.init();
		TreeTile.init();
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

		Matrix.orthoM(mProjMatrix, 0, -0.5f / mAspect, 0.5f / mAspect, -0.5f, 0.5f, -1, 1);

		glViewport(0, 0, width, height);

		int numTiles = (mWidth / (Tile.TILE_SIZE / 2) + 2)
				* (mHeight / (Tile.TILE_SIZE / 2) + 2);

		drawTiles = new TilesData(numTiles);
		newTiles = new TilesData(numTiles);
		curTiles = new TilesData(numTiles);

		// Log.d(TAG, "using: " + numTiles + " + cache: " + CACHE_TILES);
		GlUtils.checkGlError("pre glGenBuffers");

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + numTiles);
		int[] mVboIds = new int[numVBO];
		glGenBuffers(numVBO, mVboIds, 0);
		GlUtils.checkGlError("glGenBuffers");

		mVBOs = new ArrayList<VertexBufferObject>(numVBO);

		for (int i = 1; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		// Set up textures
		TextRenderer.init(numTiles);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glDisable(GLES20.GL_DEPTH_TEST);
		glDepthMask(false);
		// GLES20.glDepthRangef(0, 1);
		// GLES20.glClearDepthf(1);

		glDisable(GL_DITHER);

		if (mClearColor != null) {
			glClearColor(mClearColor[0], mClearColor[1],
					mClearColor[2], mClearColor[3]);
		} else {
			glClearColor(0.98f, 0.98f, 0.97f, 1.0f);
		}
		glClearStencil(0);
		glClear(GL_STENCIL_BUFFER_BIT);

		glEnable(GL_SCISSOR_TEST);
		glScissor(0, 0, mWidth, mHeight);

		mMapView.redrawTiles();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		// String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		// Log.d(TAG, "Extensions: " + ext);

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
	}

	@Override
	public boolean processedTile() {
		return true;
	}

	@Override
	public IMapGenerator createMapGenerator() {
		return new MapGenerator(mMapView);
	}

	@Override
	public void setRenderTheme(RenderTheme t) {
		int bg = t.getMapBackground();
		float[] c = new float[4];
		c[0] = (bg >> 16 & 0xff) / 255.0f;
		c[1] = (bg >> 8 & 0xff) / 255.0f;
		c[2] = (bg >> 0 & 0xff) / 255.0f;
		c[3] = (bg >> 24 & 0xff) / 255.0f;
		mClearColor = c;
		mUpdateColor = true;
	}

}
