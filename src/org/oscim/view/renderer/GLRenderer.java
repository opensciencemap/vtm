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
import static android.opengl.GLES20.GL_STENCIL_BUFFER_BIT;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.theme.RenderTheme;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

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
	private static ArrayList<VertexBufferObject> mVBOs;

	private static int mWidth, mHeight;

	private static int rotateBuffers = 2;
	private static ShortBuffer shortBuffer[];
	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	private static float[] mMVPMatrix = new float[16];
	private static float[] mRotateMatrix = new float[16];
	private static float[] mTmpMatrix = new float[16];
	private static float[] mTmp2Matrix = new float[16];

	private static float[] mProjMatrix = new float[16];
	private static float[] mProjMatrixI = new float[16];

	// mNextTiles is set by TileLoader and swapped with
	// mDrawTiles in onDrawFrame in GL thread.
	private static TilesData mNextTiles, mDrawTiles;

	// flag set by updateVisibleList when current visible tiles
	// changed. used in onDrawFrame to flip mNextTiles/mDrawTiles
	private static boolean mUpdateTiles;

	private static MapPosition mCurPosition;

	private float[] mClearColor = null;

	// number of tiles drawn in one frame
	private static short mDrawCount = 0;

	private static boolean mUpdateColor = false;

	// lock to synchronize Main- and GL-Thread
	static ReentrantLock tilelock = new ReentrantLock();

	// used for passing tiles to be rendered from TileLoader(Main-Thread) to
	// GLThread
	static class TilesData {
		int cnt = 0;
		final MapTile[] tiles;

		TilesData(int numTiles) {
			tiles = new MapTile[numTiles];
		}
	}

	/**
	 * @param mapView
	 *            the MapView
	 */
	public GLRenderer(MapView mapView) {
		Log.d(TAG, "init MapRenderer");

		mMapView = mapView;
		Matrix.setIdentityM(mMVPMatrix, 0);

		mUpdateTiles = false;
	}

	/**
	 * called by TileLoader when only position changed
	 * 
	 * @param mapPosition
	 *            current MapPosition
	 */
	static void updatePosition(MapPosition mapPosition) {
		mCurPosition = mapPosition;
	}

	/**
	 * called by TileLoader when list of active tiles changed
	 * 
	 * @param mapPosition
	 *            current MapPosition
	 * @param tiles
	 *            active tiles
	 * @return mNextTiles (the previously active tiles)
	 */
	static TilesData updateTiles(MapPosition mapPosition, TilesData tiles) {
		GLRenderer.tilelock.lock();

		mCurPosition = mapPosition;

		// unlock previously active tiles
		for (int i = 0; i < mNextTiles.cnt; i++) {
			MapTile t = mNextTiles.tiles[i];
			boolean found = false;

			for (int j = 0; j < tiles.cnt; j++) {
				if (tiles.tiles[j] == t) {
					found = true;
					break;
				}
			}
			if (found)
				continue;

			for (int j = 0; j < mDrawTiles.cnt; j++) {
				if (mDrawTiles.tiles[j] == t) {
					found = true;
					break;
				}
			}
			if (found)
				continue;

			t.unlock();
		}

		TilesData tmp = mNextTiles;
		mNextTiles = tiles;

		// lock tiles (and their proxies) to not be removed from cache
		for (int i = 0; i < mNextTiles.cnt; i++) {
			MapTile t = mNextTiles.tiles[i];
			if (!t.isActive)
				t.lock();
		}

		for (int j = 0; j < mDrawTiles.cnt; j++) {
			MapTile t = mDrawTiles.tiles[j];
			if (!t.isActive)
				t.lock();
		}

		// GLThread flips mNextTiles with mDrawTiles
		mUpdateTiles = true;

		GLRenderer.tilelock.unlock();

		return tmp;
	}

	/**
	 * called by TileLoader. when tile is removed from cache, reuse its vbo.
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
		ShortBuffer sbuf = null;

		// use multiple buffers to avoid overwriting buffer while current
		// data is uploaded (or rather the blocking which is probably done to
		// avoid this)
		if (uploadCnt >= rotateBuffers) {
			uploadCnt = 0;
			GLES20.glFlush();
		}

		// Upload line data to vertex buffer object
		synchronized (tile) {
			if (!tile.newData)
				return false;

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

			// Log.d(TAG, "uploadTileData, " + tile);
			GLES20.glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

			sbuf = shortBuffer[uploadCnt];

			// add fill coordinates
			newSize += 8;

			// FIXME probably not a good idea to do this in gl thread...
			if (sbuf == null || sbuf.capacity() < newSize) {
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
				// Log.d(TAG, "reuse buffer " + tile.vbo.size + " " + newSize);
			} else {
				mBufferMemoryUsage -= tile.vbo.size;
				tile.vbo.size = newSize;
				GLES20.glBufferData(GL_ARRAY_BUFFER, tile.vbo.size, sbuf, GL_DYNAMIC_DRAW);
				mBufferMemoryUsage += tile.vbo.size;
			}

			uploadCnt++;

			tile.isReady = true;
			tile.newData = false;
		}
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

	private static boolean isVisible(MapPosition mapPosition, MapTile tile) {
		float dx, dy, scale, div = 1;
		int diff = mapPosition.zoomLevel - tile.zoomLevel;

		if (diff < 0)
			div = (1 << -diff);
		else if (diff > 0)
			div = (1.0f / (1 << diff));

		scale = mapPosition.scale / div;
		dx = (float) (tile.pixelX - mapPosition.x * div);
		dy = (float) (tile.pixelY - mapPosition.y * div);

		int size = Tile.TILE_SIZE;
		int sx = (int) (dx * scale);
		int sy = (int) (dy * scale);

		// FIXME little hack, need to do scanline check or sth
		// this kindof works for typical screen aspect
		if (mRotate) {
			int ssize = mWidth > mHeight ? mWidth : mHeight;

			if (sy > ssize / 2 || sx > ssize / 2
					|| sx + size * scale < -ssize / 2
					|| sy + size * scale < -ssize / 2) {
				tile.isVisible = false;
				return false;
			}
		} else {
			if (sy > mHeight / 2 || sx > mWidth / 2
					|| sx + size * scale < -mWidth / 2
					|| sy + size * scale < -mHeight / 2) {
				tile.isVisible = false;
				return false;
			}
		}
		tile.isVisible = true;

		return true;
	}

	private static boolean mRotate = false;

	private static void setMatrix(float[] matrix, MapPosition mapPosition, MapTile tile,
			float div, boolean project) {
		float x, y, scale;

		scale = mapPosition.scale / (div * COORD_MULTIPLIER);
		x = (float) (tile.pixelX - mapPosition.x * div);
		y = (float) (tile.pixelY - mapPosition.y * div);

		Matrix.setIdentityM(matrix, 0);

		// scale to tile to world coordinates
		Matrix.scaleM(matrix, 0, scale, scale, 1);

		// translate relative to map center
		Matrix.translateM(matrix, 0,
				x * COORD_MULTIPLIER,
				-(y + Tile.TILE_SIZE) * COORD_MULTIPLIER,
				-1); // put on near plane

		if (mRotate)
			Matrix.multiplyMM(matrix, 0, mRotateMatrix, 0, matrix, 0);

		if (project)
			Matrix.multiplyMM(matrix, 0, mProjMatrix, 0, matrix, 0);
	}

	// private float[] mv = new float[4];
	// private float[] mu = { 1, 1, -1, 1 };
	//
	// private float[] mUnprojMatrix = new float[16];
	//
	// private void unproject(MapPosition pos, float x, float y) {
	// mu[0] = x;
	// mu[1] = y;
	// mu[2] = -1;
	//
	// // add tilt
	// Matrix.multiplyMV(mv, 0, mTmpMatrix, 0, mu, 0);
	// // Log.d(TAG, ">> " + mv[0] + " " + mv[1] + " " + mv[2]);
	//
	// Matrix.multiplyMV(mv, 0, mUnprojMatrix, 0, mv, 0);
	// float size = Tile.TILE_SIZE * pos.scale;
	// if (mv[3] != 0) {
	// float w = 1 / mv[3];
	// float xx = Math.round(((mv[0] * w) / size) * 100) / 100f;
	// float yy = Math.round(((mv[1] * w) / size) * 100) / 100f;
	// Log.d(TAG, " " + xx + " " + yy);
	// }
	// }

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0;

		MapPosition mapPosition;

		if (MapView.debugFrameTime)
			start = SystemClock.uptimeMillis();

		// Note: it seems faster to also clear the stencil buffer even
		// when not needed. probaly otherwise it is masked out from the
		// depth buffer as they share the same memory region afaik
		GLES20.glDepthMask(true);
		GLES20.glStencilMask(0xFF);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
				| GLES20.GL_DEPTH_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);

		// get position and current tiles to draw
		GLRenderer.tilelock.lock();
		mapPosition = mCurPosition;
		if (mUpdateTiles) {
			TilesData tmp = mDrawTiles;
			mDrawTiles = mNextTiles;
			mNextTiles = tmp;
			mUpdateTiles = false;
		}
		GLRenderer.tilelock.unlock();

		if (mDrawTiles == null)
			return;

		// if (mRotate != (mMapView.enableRotation || mMapView.enableCompass)) {
		// Matrix.setIdentityM(mMVPMatrix, 0);
		// mRotate = mMapView.enableRotation || mMapView.enableCompass;
		// }

		mRotate = mMapView.enableRotation || mMapView.enableCompass;

		if (mRotate) {
			// rotate map
			Matrix.setRotateM(mRotateMatrix, 0, mapPosition.angle, 0, 0, 1);

			// tilt map
			float angle = 15f / (mHeight / 2);
			Matrix.setRotateM(mTmpMatrix, 0, -angle, 1, 0, 0);

			// move camera center back to map center
			Matrix.translateM(mTmpMatrix, 0,
					0, (float) Math.tan(Math.toRadians(angle)), 0);

			// apply first rotation, then tilt
			Matrix.multiplyMM(mRotateMatrix, 0, mTmpMatrix, 0, mRotateMatrix, 0);

			// // get unproject matrix
			// Matrix.setIdentityM(mTmp2Matrix, 0);
			// float s = mapPosition.scale;
			// Matrix.translateM(mTmp2Matrix, 0,
			// (float) (mapPosition.x * s),
			// (float) (mapPosition.y * s), 0);
			//
			// Matrix.multiplyMM(mTmp2Matrix, 0, mRotateMatrix, 0, mTmp2Matrix,
			// 0);
			//
			// // Matrix.invertM(mTmpMatrix, 0, mTmp2Matrix, 0);
			// // (AB)^-1 = B^-1*A^-1
			// Matrix.multiplyMM(mUnprojMatrix, 0, mTmp2Matrix, 0, mProjMatrixI,
			// 0);
			//
			// // set tilt of screen coords
			// Matrix.setRotateM(mTmpMatrix, 0, -15, 1, 0, 0);
			//
			// // unproject(mapPosition, 0, 0);
			// unproject(mapPosition, -1, -1); // top-left
			// unproject(mapPosition, 1, -1); // top-right
			// unproject(mapPosition, -1, 1); // bottom-left
			// unproject(mapPosition, 1, 1); // bottom-right
		}

		if (mUpdateColor && mClearColor != null) {
			GLES20.glClearColor(mClearColor[0], mClearColor[1], mClearColor[2],
					mClearColor[3]);
			mUpdateColor = false;
		}

		int tileCnt = mDrawTiles.cnt;
		MapTile[] tiles = mDrawTiles.tiles;

		uploadCnt = 0;
		int updateTextures = 0;

		// check visible tiles, upload new vertex data
		for (int i = 0; i < tileCnt; i++) {
			MapTile tile = tiles[i];

			if (!isVisible(mapPosition, tile))
				continue;

			if (tile.texture == null && TextRenderer.drawToTexture(tile))
				updateTextures++;

			if (tile.newData) {
				uploadTileData(tile);
				continue;
			}

			if (!tile.isReady) {
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

		if (updateTextures > 0)
			TextRenderer.compileTextures();

		GLES20.glEnable(GL_DEPTH_TEST);
		GLES20.glEnable(GL_POLYGON_OFFSET_FILL);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && tiles[i].isReady)
				drawTile(mapPosition, tiles[i]);
		}

		// proxies are clipped to the region where nothing was drawn to depth
		// buffer. TODO draw all parent before grandparent
		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && !tiles[i].isReady)
				drawProxyTile(mapPosition, tiles[i]);
		}
		// GlUtils.checkGlError("end draw");

		GLES20.glDisable(GL_POLYGON_OFFSET_FILL);
		GLES20.glDisable(GL_DEPTH_TEST);

		mDrawCount = 0;
		mDrawSerial++;

		GLES20.glEnable(GL_BLEND);
		int z = mapPosition.zoomLevel;
		float s = mapPosition.scale;

		int zoomLevelDiff = Math.max(z - TileGenerator.STROKE_MAX_ZOOM_LEVEL, 0);
		float scale = (float) Math.pow(1.4, zoomLevelDiff);
		if (scale < 1)
			scale = 1;

		if (z >= TileGenerator.STROKE_MAX_ZOOM_LEVEL)
			TextRenderer.beginDraw(FloatMath.sqrt(s) / scale, mProjMatrix);
		else
			TextRenderer.beginDraw(s, mProjMatrix);

		// s = (float) 1.0 / COORD_MULTIPLIER;
		// TextRenderer.beginDraw(s, mProjMatrix);

		for (int i = 0; i < tileCnt; i++) {
			if (!tiles[i].isVisible || tiles[i].texture == null)
				continue;

			setMatrix(mMVPMatrix, mapPosition, tiles[i], 1, false);
			TextRenderer.drawTile(tiles[i], mMVPMatrix);
		}
		TextRenderer.endDraw();

		// TODO call overlay renderer

		if (MapView.debugFrameTime) {
			GLES20.glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start));
		}
	}

	// used to not draw a tile twice per frame...
	private static byte mDrawSerial = 0;

	private static void drawTile(MapPosition mapPosition, MapTile tile) {
		// draw parents only once
		if (tile.lastDraw == mDrawSerial)
			return;

		float div = 1;

		int diff = mapPosition.zoomLevel - tile.zoomLevel;

		if (diff < 0)
			div = (1 << -diff);
		else if (diff > 0)
			div = (1.0f / (1 << diff));

		tile.lastDraw = mDrawSerial;

		int z = mapPosition.zoomLevel;
		float s = mapPosition.scale;
		float[] mvp = mMVPMatrix;

		setMatrix(mvp, mapPosition, tile, div, true);

		GLES20.glPolygonOffset(0, mDrawCount++);

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

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
				GLES20.glDisable(GL_BLEND);

				pl = PolygonRenderer.drawPolygons(pl, lnext, mvp, z, s, !clipped);

				clipped = true;

			} else {
				// XXX nasty
				if (!clipped) {
					PolygonRenderer.drawPolygons(null, 0, mvp, z, s, true);
					clipped = true;
				}

				GLES20.glEnable(GL_BLEND);
				ll = LineRenderer.drawLines(tile, ll, pnext, mvp, div, z, s);
			}
		}
	}

	// TODO could use tile.proxies here
	private static boolean drawProxyChild(MapPosition mapPosition, MapTile tile) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if (tile.rel.child[i] == null)
				continue;

			MapTile c = tile.rel.child[i].tile;
			if (c == null)
				continue;

			if (!isVisible(mapPosition, c)) {
				drawn++;
				continue;
			}

			if (c.isReady) {
				drawTile(mapPosition, c);
				drawn++;
			}
		}
		return drawn == 4;
	}

	// TODO could use tile.proxies here
	private static void drawProxyTile(MapPosition mapPosition, MapTile tile) {
		boolean drawn = false;
		if (mapPosition.scale > 1.5f) {
			// prefer drawing children
			if (!drawProxyChild(mapPosition, tile)) {
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile t = tile.rel.parent.tile;
					if (t.isReady) {
						drawTile(mapPosition, t);
						drawn = true;
					}
				}

				if (!drawn && (tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
					MapTile t = tile.rel.parent.parent.tile;
					if (t.isReady)
						drawTile(mapPosition, t);

				}
			}
		} else {
			// prefer drawing parent
			MapTile t = tile.rel.parent.tile;

			if (t != null && t.isReady) {
				drawTile(mapPosition, t);

			} else if (!drawProxyChild(mapPosition, tile)) {

				if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
					t = tile.rel.parent.parent.tile;
					if (t.isReady)
						drawTile(mapPosition, t);
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

		// Matrix.orthoM(mProjMatrix, 0, -0.5f / mAspect, 0.5f / mAspect, -0.5f,
		// 0.5f, -1, 1);

		Matrix.frustumM(mProjMatrix, 0,
				-0.5f * width,
				0.5f * width,
				-0.5f * height,
				0.5f * height, 1, 2);

		Matrix.invertM(mProjMatrixI, 0, mProjMatrix, 0);

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

		if (mClearColor != null) {
			GLES20.glClearColor(mClearColor[0], mClearColor[1],
					mClearColor[2], mClearColor[3]);
		} else {
			GLES20.glClearColor(0.98f, 0.98f, 0.97f, 1.0f);
		}

		GlUtils.checkGlError("onSurfaceChanged");

		GLES20.glClear(GL_STENCIL_BUFFER_BIT);

		mMapView.redrawMap();
	}

	void clearTiles(int numTiles) {
		mDrawTiles = new TilesData(numTiles);
		mNextTiles = new TilesData(numTiles);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		Log.d(TAG, "Extensions: " + ext);

		shortBuffer = new ShortBuffer[rotateBuffers];

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

		LineRenderer.init();
		PolygonRenderer.init();
		TextRenderer.init();

		mNewSurface = true;

		// glEnable(GL_SCISSOR_TEST);
		// glScissor(0, 0, mWidth, mHeight);
		GLES20.glClearStencil(0);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

	}

	private boolean mNewSurface;

}
