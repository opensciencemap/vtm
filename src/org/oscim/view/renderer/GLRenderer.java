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

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_POLYGON_OFFSET_FILL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.oscim.core.Tile;
import org.oscim.theme.RenderTheme;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapPosition;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;
import org.oscim.view.renderer.MapRenderer.TilesData;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.FloatMath;
import android.util.Log;

public class GLRenderer implements GLSurfaceView.Renderer {

	private static final String TAG = "SurfaceRenderer";

	private static final int MB = 1024 * 1024;
	private static final int SHORT_BYTES = 2;
	private static final int CACHE_TILES_MAX = 250;
	private static final int LIMIT_BUFFERS = 16 * MB;

	static final float COORD_MULTIPLIER = 8.0f;

	static int CACHE_TILES = CACHE_TILES_MAX;

	private final MapView mMapView;
	private final MapViewPosition mMapViewPosition;

	private static MapPosition mMapPosition;

	private static ArrayList<VertexBufferObject> mVBOs;

	private static int mWidth, mHeight;

	private static int rotateBuffers = 2;
	private static ShortBuffer shortBuffer[];
	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	private static float[] mMVPMatrix = new float[16];
	private static float[] mProjMatrix = new float[16];
	private static float[] mTileCoords = new float[8];

	// mNextTiles is set by TileLoader and swapped with
	// mDrawTiles in onDrawFrame in GL thread.
	private static TilesData mNextTiles;
	/* package */static TilesData mDrawTiles;

	// flag set by updateVisibleList when current visible tiles
	// changed. used in onDrawFrame to flip mNextTiles/mDrawTiles
	private static boolean mUpdateTiles;

	private float[] mClearColor = null;

	// number of tiles drawn in one frame
	private static short mDrawCount = 0;

	private static boolean mUpdateColor = false;

	// drawlock to synchronize Main- and GL-Thread
	static ReentrantLock tilelock = new ReentrantLock();
	static ReentrantLock drawlock = new ReentrantLock();

	/* package */static int mHolderCount;

	// scanline fill class used to check tile visibility
	private static ScanBox mScanBox = new ScanBox() {
		@Override
		void setVisible(int y, int x1, int x2) {
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
			// of date line...
			for (int x = x1; x < x2; x++) {
				MapTile holder = null;
				MapTile tile = null;
				boolean found = false;

				int xx = x;

				if (x >= 0 && x < xmax)
					continue;

				if (x < 0)
					xx = xmax + x;
				else
					xx = x - xmax;

				if (xx < 0 || xx >= xmax)
					continue;

				for (int i = cnt; i < cnt + mHolderCount; i++)
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

				// Log.d(TAG, "add placeholder " + y + " " + x + ">>" + xx + " "
				// + tile);

				holder = new MapTile(x, y, mZoom);
				holder.isVisible = true;
				holder.holder = tile;
				tiles[cnt + mHolderCount++] = holder;
			}
		}
	};

	/**
	 * @param mapView
	 *            the MapView
	 */
	public GLRenderer(MapView mapView) {
		Log.d(TAG, "init MapRenderer");

		mMapView = mapView;
		mMapViewPosition = mapView.getMapViewPosition();
		mMapPosition = new MapPosition();
		mMapPosition.init();

		Matrix.setIdentityM(mMVPMatrix, 0);

		// add half pixel to tile clip/fill coordinates to avoid rounding issues
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

		shortBuffer = new ShortBuffer[rotateBuffers];

		for (int i = 0; i < rotateBuffers; i++) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(MB >> 2)
					.order(ByteOrder.nativeOrder());

			shortBuffer[i] = bbuf.asShortBuffer();
			shortBuffer[i].put(mFillCoords, 0, 8);
		}

		mUpdateTiles = false;
	}

	/**
	 * Called by TileLoader when list of active tiles changed. the list is
	 * copied to mNextTiles to be used in next call to onDrawFrame
	 * 
	 * @param tiles
	 *            active tiles
	 */
	static void updateTiles(TilesData tiles) {

		MapTile[] newTiles = tiles.tiles;

		// lock tiles (and their proxies) to not be removed from cache
		for (int i = 0, n = tiles.cnt; i < n; i++)
			newTiles[i].lock();

		// dont flip next/drawTiles while copying
		GLRenderer.tilelock.lock();

		MapTile[] nextTiles = mNextTiles.tiles;

		// unlock previously active tiles
		for (int i = 0, n = mNextTiles.cnt; i < n; i++)
			nextTiles[i].unlock();

		// copy newTiles to nextTiles
		System.arraycopy(newTiles, 0, nextTiles, 0, tiles.cnt);

		mNextTiles.cnt = tiles.cnt;

		// flip next/drawTiles in next onDrawFrame
		mUpdateTiles = true;

		GLRenderer.tilelock.unlock();
	}

	/**
	 * called by TileLoader. when tile is removed from cache reuse its vbo.
	 * 
	 * @param vbo
	 *            the VBO
	 */
	static void addVBO(VertexBufferObject vbo) {
		synchronized (mVBOs) {
			mVBOs.add(vbo);
		}
	}

	void setVBO(MapTile tile) {
		synchronized (mVBOs) {
			int numVBOs = mVBOs.size();

			if (numVBOs > 0 && tile.vbo == null) {
				tile.vbo = mVBOs.remove(numVBOs - 1);
			}
		}
	}

	void setRenderTheme(RenderTheme t) {
		int bg = t.getMapBackground();
		float[] c = new float[4];
		c[3] = (bg >> 24 & 0xff) / 255.0f;
		c[0] = (bg >> 16 & 0xff) / 255.0f;
		c[1] = (bg >> 8 & 0xff) / 255.0f;
		c[2] = (bg >> 0 & 0xff) / 255.0f;
		mClearColor = c;
		mUpdateColor = true;
	}

	private int uploadCnt = 0;

	private boolean uploadTileData(MapTile tile) {
		// Upload line data to vertex buffer object
		// Log.d(TAG, "uploadTileData, " + tile);

		int lineSize = LineRenderer.sizeOf(tile.lineLayers);
		int polySize = PolygonRenderer.sizeOf(tile.polygonLayers);
		int newSize = lineSize + polySize;

		if (newSize == 0) {
			LineRenderer.clear(tile.lineLayers);
			PolygonRenderer.clear(tile.polygonLayers);
			tile.lineLayers = null;
			tile.polygonLayers = null;
			tile.newData = false;
			return false;
		}

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

		// use multiple buffers to avoid overwriting buffer while current
		// data is uploaded (or rather the blocking which is probably done to
		// avoid overwriting)
		if (uploadCnt >= rotateBuffers) {
			uploadCnt = 0;
			// GLES20.glFlush();
		}

		ShortBuffer sbuf = shortBuffer[uploadCnt];

		// add fill coordinates
		newSize += 8;

		// probably not a good idea to do this in gl thread...
		if (sbuf.capacity() < newSize) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(newSize * SHORT_BYTES)
					.order(ByteOrder.nativeOrder());
			sbuf = bbuf.asShortBuffer();
			shortBuffer[uploadCnt] = sbuf;
			sbuf.put(mFillCoords, 0, 8);
		}

		sbuf.clear();
		sbuf.position(8);

		PolygonRenderer.compileLayerData(tile.polygonLayers, sbuf);

		tile.lineOffset = (8 + polySize);
		if (tile.lineOffset != sbuf.position())
			Log.d(TAG, "tiles lineoffset is wrong: " + tile + " "
					+ tile.lineOffset + " "
					+ sbuf.position() + " "
					+ sbuf.limit() + " "
					+ sbuf.remaining() + " "
					+ PolygonRenderer.sizeOf(tile.polygonLayers) + " "
					+ tile.rel);

		tile.lineOffset *= SHORT_BYTES;

		LineRenderer.compileLayerData(tile.lineLayers, sbuf);

		sbuf.flip();

		if (newSize != sbuf.remaining()) {
			Log.d(TAG, "tiles wrong: " + tile + " "
					+ newSize + " "
					+ sbuf.position() + " "
					+ sbuf.limit() + " "
					+ sbuf.remaining() + " "
					+ LineRenderer.sizeOf(tile.lineLayers)
					+ tile.isLoading + " "
					+ tile.rel);

			tile.newData = false;
			return false;
		}
		newSize *= SHORT_BYTES;

		// reuse memory allocated for vbo when possible and allocated
		// memory is less then four times the new data
		if (tile.vbo.size > newSize && tile.vbo.size < newSize * 4
				&& mBufferMemoryUsage < LIMIT_BUFFERS) {
			GLES20.glBufferSubData(GL_ARRAY_BUFFER, 0, newSize, sbuf);
		} else {
			mBufferMemoryUsage -= tile.vbo.size;
			tile.vbo.size = newSize;
			GLES20.glBufferData(GL_ARRAY_BUFFER, tile.vbo.size, sbuf, GL_DYNAMIC_DRAW);
			mBufferMemoryUsage += tile.vbo.size;
		}

		uploadCnt++;

		tile.isReady = true;
		tile.newData = false;

		return true;
	}

	private static void checkBufferUsage() {
		if (mBufferMemoryUsage < LIMIT_BUFFERS) {
			if (CACHE_TILES < CACHE_TILES_MAX)
				CACHE_TILES += 50;
			return;
		}

		// try to clear some unused vbo when exceding limit
		Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / MB + "MB");

		int vboIds[] = new int[10];
		VertexBufferObject[] tmp = new VertexBufferObject[10];

		int removed = 0;
		synchronized (mVBOs) {
			for (VertexBufferObject vbo : mVBOs) {

				if (vbo.size == 0)
					continue;

				mBufferMemoryUsage -= vbo.size;
				vbo.size = 0;

				// this should free allocated memory but it does not.
				// on HTC it causes OOM exception?!
				// glBindBuffer(GL_ARRAY_BUFFER, vbo.id);
				// glBufferData(GL_ARRAY_BUFFER, 0, null,
				// GLES20.GL_STATIC_DRAW);

				// recreate vbo instead
				vboIds[removed] = vbo.id;
				tmp[removed++] = vbo;

				if (removed == 10)
					break;
			}
		}

		if (removed > 0) {
			GLES20.glDeleteBuffers(removed, vboIds, 0);
			GLES20.glGenBuffers(removed, vboIds, 0);

			for (int i = 0; i < removed; i++)
				tmp[i].id = vboIds[i];

			Log.d(TAG, "now: " + mBufferMemoryUsage / MB + "MB");
		}

		if (mBufferMemoryUsage > LIMIT_BUFFERS && CACHE_TILES > 100)
			CACHE_TILES -= 50;
	}

	private static boolean mRotate = false;

	private static void setMatrix(float[] matrix, MapTile tile,
			float div, boolean project) {
		MapPosition mapPosition = mMapPosition;

		float x = (float) (tile.pixelX - mapPosition.x * div);
		float y = (float) (tile.pixelY - mapPosition.y * div);
		float scale = mapPosition.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;

		// scale to tile to world coordinates
		scale /= COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;

		if (mRotate)
			Matrix.multiplyMM(matrix, 0, mapPosition.rotation, 0, matrix, 0);

		if (project)
			Matrix.multiplyMM(matrix, 0, mProjMatrix, 0, matrix, 0);
	}

	private static float scaleDiv(MapTile t) {
		float div = 1;
		int diff = mMapPosition.zoomLevel - t.zoomLevel;
		if (diff < 0)
			div = (1 << -diff);
		else if (diff > 0)
			div = (1.0f / (1 << diff));
		return div;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0;
		// prevent main thread recreating all tiles (updateMap)
		// while rendering is going. not have seen this happen
		// yet though
		GLRenderer.drawlock.lock();

		if (MapView.debugFrameTime)
			start = SystemClock.uptimeMillis();

		if (mUpdateColor) {
			float cc[] = mClearColor;
			GLES20.glClearColor(cc[0], cc[1], cc[2], cc[3]);
			mUpdateColor = false;
		}

		// Note: it seems faster to also clear the stencil buffer even
		// when not needed. probaly otherwise it is masked out from the
		// depth buffer as they share the same memory region afaik
		GLES20.glDepthMask(true);
		GLES20.glStencilMask(0xFF);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
				| GLES20.GL_DEPTH_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);

		// get current tiles to draw
		if (mUpdateTiles) {
			GLRenderer.tilelock.lock();
			TilesData tmp = mDrawTiles;
			mDrawTiles = mNextTiles;
			mNextTiles = tmp;
			mUpdateTiles = false;
			GLRenderer.tilelock.unlock();
		}

		if (mDrawTiles == null || mDrawTiles.cnt == 0) {
			GLRenderer.drawlock.unlock();
			return;
		}

		mRotate = mMapView.enableRotation || mMapView.enableCompass;

		// get current MapPosition, set mTileCoords (mapping of screen to model
		// coordinates)
		MapPosition mapPosition = mMapPosition;
		float[] coords = mTileCoords;
		boolean changed = mMapViewPosition.getMapPosition(mapPosition, coords);

		int tileCnt = mDrawTiles.cnt;
		MapTile[] tiles = mDrawTiles.tiles;

		if (changed) {
			// get visible tiles
			for (int i = 0; i < tileCnt; i++)
				tiles[i].isVisible = false;

			// relative zoom-level, 'tiles' could not have been updated after
			// zoom-level changed.
			float div = scaleDiv(tiles[0]);

			float s = Tile.TILE_SIZE;
			float scale = mapPosition.scale / div;
			double px = mapPosition.x * div;
			double py = mapPosition.y * div;

			for (int i = 0; i < 8; i += 2) {
				coords[i + 0] = (float) ((px + coords[i + 0] / scale) / s);
				coords[i + 1] = (float) ((py + coords[i + 1] / scale) / s);
			}

			mHolderCount = 0;
			mScanBox.scan(coords, tiles[0].zoomLevel);
			tileCnt += mHolderCount;
		}

		uploadCnt = 0;
		int updateTextures = 0;

		// check visible tiles, upload new vertex data
		for (int i = 0; i < tileCnt; i++) {
			MapTile tile = tiles[i];

			// if (!isVisible(mapPosition, tile))
			// continue;
			if (!tile.isVisible)
				continue;

			if (MapView.staticLabeling) {
				if (tile.texture == null && TextRenderer.drawToTexture(tile))
					updateTextures++;
			}

			if (tile.newData) {
				uploadTileData(tile);
				continue;
			}
			if (tile.holder != null) {
				if (tile.holder.newData) {
					uploadTileData(tile.holder);
				}
				tile.isReady = tile.holder.isReady;
			} else if (!tile.isReady) {
				// check near relatives if they can serve as proxy
				MapTile rel = tile.rel.parent.tile;
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

		if (uploadCnt > 0)
			checkBufferUsage();

		if (MapView.staticLabeling) {
			if (updateTextures > 0)
				TextRenderer.compileTextures();
		}

		GLES20.glEnable(GL_DEPTH_TEST);
		GLES20.glEnable(GL_POLYGON_OFFSET_FILL);

		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.isReady)
				drawTile(t);
		}

		// proxies are clipped to the region where nothing was drawn to depth
		// buffer. TODO draw all parent before grandparent
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && !t.isReady && (t.holder == null))
				drawProxyTile(t);
		}
		// GlUtils.checkGlError("end draw");

		GLES20.glDisable(GL_POLYGON_OFFSET_FILL);
		GLES20.glDisable(GL_DEPTH_TEST);

		mDrawCount = 0;
		mDrawSerial++;

		if (MapView.staticLabeling) {
			GLES20.glEnable(GL_BLEND);
			int z = mapPosition.zoomLevel;
			float s = mapPosition.scale;

			int zoomLevelDiff = Math.max(z - TileGenerator.STROKE_MAX_ZOOM_LEVEL, 0);
			float scale = (float) Math.pow(1.4, zoomLevelDiff);
			if (scale < 1)
				scale = 1;

			if (z >= TileGenerator.STROKE_MAX_ZOOM_LEVEL)
				TextRenderer.beginDraw(scale / FloatMath.sqrt(s), mProjMatrix);
			else
				TextRenderer.beginDraw(1 / s, mProjMatrix);

			for (int i = 0; i < tileCnt; i++) {
				MapTile t = tiles[i];
				if (!t.isVisible)
					continue;

				if (t.holder == null) {
					if (t.texture != null) {
						setMatrix(mMVPMatrix, t, 1, false);
						TextRenderer.drawTile(t, mMVPMatrix);
					}
				} else {
					if (t.holder.texture != null) {
						setMatrix(mMVPMatrix, t, 1, false);
						TextRenderer.drawTile(t.holder, mMVPMatrix);
					}
				}
			}
			TextRenderer.endDraw();
		}

		// TODO call overlay renderer here

		if (MapView.debugFrameTime) {
			GLES20.glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start));
		}
		GLRenderer.drawlock.unlock();
	}

	// used to not draw a tile twice per frame.
	private static int mDrawSerial = 0;

	private static void drawTile(MapTile tile) {
		// draw parents only once
		if (tile.lastDraw == mDrawSerial)
			return;

		float div = scaleDiv(tile);
		float[] mvp = mMVPMatrix;
		MapPosition pos = mMapPosition;

		tile.lastDraw = mDrawSerial;

		setMatrix(mvp, tile, div, true);

		if (tile.holder != null)
			tile = tile.holder;

		GLES20.glPolygonOffset(0, mDrawCount++);

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

		LineLayer ll = tile.lineLayers;
		PolygonLayer pl = tile.polygonLayers;

		boolean clipped = false;
		int simpleShader = mRotate ? 0 : 1;

		for (; pl != null || ll != null;) {
			int lnext = Integer.MAX_VALUE;
			int pnext = Integer.MAX_VALUE;

			if (ll != null)
				lnext = ll.layer;

			if (pl != null)
				pnext = pl.layer;

			if (pl != null && pnext < lnext) {
				GLES20.glDisable(GL_BLEND);
				pl = PolygonRenderer.drawPolygons(pos, pl, lnext, mvp, !clipped);
				clipped = true;
			} else {
				// FIXME
				if (!clipped) {
					PolygonRenderer.drawPolygons(pos, null, 0, mvp, true);
					clipped = true;
				}
				GLES20.glEnable(GL_BLEND);
				ll = LineRenderer.drawLines(pos, ll, pnext, mvp, div,
						simpleShader, tile.lineOffset);
			}
		}
	}

	// TODO should check tile.proxies here
	private static boolean drawProxyChild(MapTile tile) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if (tile.rel.child[i] == null)
				continue;

			MapTile c = tile.rel.child[i].tile;
			if (c == null)
				continue;

			// if (!isVisible(c)) {
			// drawn++;
			// continue;
			// }

			if (c.isReady) {
				drawTile(c);
				drawn++;
			}
		}
		return drawn == 4;
	}

	private static void drawProxyTile(MapTile tile) {
		int diff = mMapPosition.zoomLevel - tile.zoomLevel;

		boolean drawn = false;
		if (mMapPosition.scale > 1.5f || diff < 0) {
			// prefer drawing children
			if (!drawProxyChild(tile)) {
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile t = tile.rel.parent.tile;
					if (t.isReady) {
						drawTile(t);
						drawn = true;
					}
				}

				if (!drawn && (tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
					MapTile t = tile.rel.parent.parent.tile;
					if (t.isReady)
						drawTile(t);

				}
			}
		} else {
			// prefer drawing parent
			MapTile t = tile.rel.parent.tile;

			if (t != null && t.isReady) {
				drawTile(t);

			} else if (!drawProxyChild(tile)) {

				if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
					t = tile.rel.parent.parent.tile;
					if (t.isReady)
						drawTile(t);
				}
			}
		}
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		Log.d(TAG, "SurfaceChanged:" + width + " " + height);

		if (width <= 0 || height <= 0)
			return;

		boolean changed = true;
		if (mWidth == width || mHeight == height)
			changed = false;

		mWidth = width;
		mHeight = height;

		float s = 0.5f;
		// use this to scale only the view to see better which tiles are
		// rendered
		// s = 1.0f;
		Matrix.frustumM(mProjMatrix, 0, -s * width, s * width,
				s * height, -s * height, 1, 2);
		Matrix.translateM(mProjMatrix, 0, 0, 0, -1);

		// set to zero: we modify the z value with polygon-offset for clipping
		mProjMatrix[10] = 0;
		mProjMatrix[14] = 0;

		GLES20.glViewport(0, 0, width, height);

		if (!changed && !mNewSurface) {
			mMapView.redrawMap();
			return;
		}
		mNewSurface = false;

		mBufferMemoryUsage = 0;

		int numTiles = (mWidth / (Tile.TILE_SIZE / 2) + 2)
				* (mHeight / (Tile.TILE_SIZE / 2) + 2);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + (numTiles * 2));
		int[] mVboIds = new int[numVBO];
		GLES20.glGenBuffers(numVBO, mVboIds, 0);
		GlUtils.checkGlError("glGenBuffers");

		mVBOs = new ArrayList<VertexBufferObject>(numVBO);

		for (int i = 1; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		// Set up textures
		TextRenderer.setup(numTiles);

		if (mClearColor != null)
			mUpdateColor = true;

		// FIXME this should be synchronized
		mMapView.redrawMap();
	}

	// FIXME this is a bit too spaghetti
	void clearTiles(int numTiles) {
		mDrawTiles = new TilesData(numTiles);
		mNextTiles = new TilesData(numTiles);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		Log.d(TAG, "Extensions: " + ext);

		LineRenderer.init();
		PolygonRenderer.init();
		TextRenderer.init();

		mNewSurface = true;
		// mUpdateColor = true;

		// glEnable(GL_SCISSOR_TEST);
		// glScissor(0, 0, mWidth, mHeight);
		GLES20.glClearStencil(0);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

	}

	private boolean mNewSurface;

}

//
// private static boolean isVisible(MapTile tile) {
// float dx, dy, scale, div = 1;
// MapPosition mapPosition = mMapPosition;
// int diff = mapPosition.zoomLevel - tile.zoomLevel;
//
// if (diff < 0)
// div = (1 << -diff);
// else if (diff > 0)
// div = (1.0f / (1 << diff));
//
// scale = mapPosition.scale / div;
// dx = (float) (tile.pixelX - mapPosition.x * div);
// dy = (float) (tile.pixelY - mapPosition.y * div);
//
// int size = Tile.TILE_SIZE;
// int sx = (int) (dx * scale);
// int sy = (int) (dy * scale);
//
// // FIXME little hack, need to do scanline check or sth
// // this kindof works for typical screen aspect
// if (mRotate) {
// int ssize = mWidth > mHeight ? mWidth : mHeight;
// ssize += Tile.TILE_SIZE;
// if (sy > ssize / 2 || sx > ssize / 2
// || sx + size * scale < -ssize / 2
// || sy + size * scale < -ssize / 2) {
// tile.isVisible = false;
// return false;
// }
// } else {
// if (sy > mHeight / 2 || sx > mWidth / 2
// || sx + size * scale < -mWidth / 2
// || sy + size * scale < -mHeight / 2) {
// tile.isVisible = false;
// return false;
// }
// }
// tile.isVisible = true;
//
// return true;
// }
