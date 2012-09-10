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
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_STENCIL_BUFFER_BIT;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glClearStencil;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glViewport;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.JobTile;
import org.mapsforge.android.rendertheme.RenderTheme;
import org.mapsforge.android.utils.GlUtils;
import org.mapsforge.core.MapPosition;
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

	private static final int CACHE_TILES_MAX = 200;
	private static final int LIMIT_BUFFERS = 16 * MB;

	static int CACHE_TILES = CACHE_TILES_MAX;

	private final MapView mMapView;
	private static ArrayList<VertexBufferObject> mVBOs;

	private static int mWidth, mHeight;
	private static float mAspect;

	private static int rotateBuffers = 2;
	private static ShortBuffer shortBuffer[];
	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	private static float[] mMVPMatrix = new float[16];
	private static float[] mRotateMatrix = new float[16];
	private static float[] mProjMatrix = new float[16];
	private static float[] mRotTMatrix = new float[16];

	// curTiles is set by TileLoader and swapped with
	// drawTiles in onDrawFrame in GL thread.
	private static TilesData curTiles, drawTiles;

	// flag set by updateVisibleList when current visible tiles
	// changed. used in onDrawFrame to flip curTiles/drawTiles
	private static boolean mUpdateTiles;

	private static MapPosition mCurPosition;

	private float[] mClearColor = null;

	// number of tiles drawn in one frame
	private static short mDrawCount = 0;

	private static boolean mUpdateColor = false;

	// lock to synchronize Main- and GL-Thread
	static Object lock = new Object();

	// used for passing tiles to be rendered from TileLoader(Main-Thread) to GLThread
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
	public MapRenderer(MapView mapView) {
		Log.d(TAG, "init MapRenderer");

		mMapView = mapView;
		Matrix.setIdentityM(mMVPMatrix, 0);

		mUpdateTiles = false;
	}

	/**
	 * called by MapView when position or map settings changes
	 */
	@Override
	public void updateMap(boolean clear) {
		TileLoader.updateMap(clear);
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
	 * @return curTiles (the previously active tiles)
	 */
	static TilesData updateTiles(MapPosition mapPosition, TilesData tiles) {
		synchronized (MapRenderer.lock) {

			mCurPosition = mapPosition;

			for (int i = 0; i < curTiles.cnt; i++)
				curTiles.tiles[i].isActive = false;

			TilesData tmp = curTiles;
			curTiles = tiles;

			for (int i = 0; i < curTiles.cnt; i++)
				curTiles.tiles[i].isActive = true;

			for (int j = 0; j < drawTiles.cnt; j++)
				drawTiles.tiles[j].isActive = true;

			mUpdateTiles = true;

			return tmp;
		}
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

	/**
	 * called from MapWorker Thread when tile is loaded by MapGenerator
	 */
	@Override
	public synchronized boolean passTile(JobTile jobTile) {
		MapTile tile = (MapTile) jobTile;

		if (!tile.isLoading) {
			// no one should be able to use this tile now, mapgenerator passed it,
			// glthread does nothing until newdata is set.
			Log.d(TAG, "passTile: canceled " + tile);
			TileLoader.addTileLoaded(tile);
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

		TileLoader.addTileLoaded(tile);

		return true;
	}

	// depthRange: -1 to 1, bits: 2^24 => 2/2^24 one step
	// ... asus has just 16 bit?!
	// private static final float depthStep = 0.00000011920928955078125f;

	private static boolean mRotate = false;

	private static void setMatrix(MapPosition mapPosition, MapTile tile, float div,
			int offset) {
		float x, y, scale;

		if (mRotate) {
			scale = (float) (1.0 * mapPosition.scale / (mHeight * div));
			x = (float) (tile.pixelX - mapPosition.x * div);
			y = (float) (tile.pixelY - mapPosition.y * div);

			Matrix.setIdentityM(mMVPMatrix, 0);

			Matrix.scaleM(mMVPMatrix, 0, scale / COORD_MULTIPLIER,
					scale / COORD_MULTIPLIER, 1);

			Matrix.translateM(mMVPMatrix, 0,
					x * COORD_MULTIPLIER,
					-(y + Tile.TILE_SIZE) * COORD_MULTIPLIER,
					-1 + offset * 0.01f);

			Matrix.multiplyMM(mMVPMatrix, 0, mRotateMatrix, 0, mMVPMatrix, 0);

			Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);
		}
		else {
			scale = (float) (2.0 * mapPosition.scale / (mHeight * div));
			x = (float) (tile.pixelX - mapPosition.x * div);
			y = (float) (tile.pixelY - mapPosition.y * div);

			mMVPMatrix[12] = x * scale * mAspect;
			mMVPMatrix[13] = -(y + Tile.TILE_SIZE) * scale;
			// increase the 'distance' with each tile drawn.
			mMVPMatrix[14] = -1 + offset * 0.01f; // depthStep; // 0.01f;
			mMVPMatrix[0] = scale * mAspect / COORD_MULTIPLIER;
			mMVPMatrix[5] = scale / COORD_MULTIPLIER;
		}

	}

	private static boolean isVisible(MapPosition mapPosition, MapTile tile, float div) {
		double dx, dy, scale;

		if (div == 0) {
			dx = tile.pixelX - mapPosition.x;
			dy = tile.pixelY - mapPosition.y;
			scale = mapPosition.scale;
		} else {
			dx = tile.pixelX - mapPosition.x * div;
			dy = tile.pixelY - mapPosition.y * div;
			scale = mapPosition.scale / div;
		}
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

	private int uploadCnt = 0;

	private boolean uploadTileData(MapTile tile) {
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
		// try to clear some unused vbo when exceding limit
		if (mBufferMemoryUsage > LIMIT_BUFFERS) {
			Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / MB + "MB");

			glBindBuffer(GL_ARRAY_BUFFER, 0);
			int buf[] = new int[1];

			synchronized (mVBOs) {
				for (VertexBufferObject vbo : mVBOs) {

					if (vbo.size == 0)
						continue;

					mBufferMemoryUsage -= vbo.size;

					// this should free allocated memory but it does not.
					// on HTC it causes oom exception?!

					// glBindBuffer(GL_ARRAY_BUFFER, vbo.id);
					// glBufferData(GL_ARRAY_BUFFER, 0, null, GLES20.GL_STATIC_DRAW);

					// recreate vbo instead
					buf[0] = vbo.id;
					GLES20.glDeleteBuffers(1, buf, 0);
					GLES20.glGenBuffers(1, buf, 0);
					vbo.id = buf[0];

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
		MapPosition mapPosition;

		if (MapView.debugFrameTime)
			start = SystemClock.uptimeMillis();

		mRotate = mMapView.enableRotation;

		if (mUpdateColor && mClearColor != null) {
			glClearColor(mClearColor[0], mClearColor[1], mClearColor[2], mClearColor[3]);
			mUpdateColor = false;
		}

		GLES20.glDepthMask(true);

		// Note: having the impression it is faster to also clear the
		// stencil buffer even when not needed. probaly otherwise it
		// is masked out from the depth buffer as they share the same
		// memory region afaik
		glClear(GLES20.GL_COLOR_BUFFER_BIT
				| GLES20.GL_DEPTH_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);

		// get position and current tiles to draw
		synchronized (MapRenderer.lock) {
			mapPosition = mCurPosition;

			if (mUpdateTiles) {
				TilesData tmp = drawTiles;
				drawTiles = curTiles;
				curTiles = tmp;
				mUpdateTiles = false;
			}
		}

		int tileCnt = drawTiles.cnt;
		MapTile[] tiles = drawTiles.tiles;

		uploadCnt = 0;
		int updateTextures = 0;

		// check visible tiles, upload new vertex data
		for (int i = 0; i < tileCnt; i++) {
			MapTile tile = tiles[i];

			if (!isVisible(mapPosition, tile, 1))
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

		if (mRotate) {
			Matrix.setRotateM(mRotateMatrix, 0, mapPosition.angle, 0, 0, 1);
			Matrix.transposeM(mRotTMatrix, 0, mRotateMatrix, 0);
		} else {
			Matrix.setIdentityM(mRotTMatrix, 0);
		}

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && tiles[i].isReady) {
				drawTile(mapPosition, tiles[i], 1);
			}
		}

		// proxies are clipped to the region where nothing was drawn to depth buffer
		// TODO draw all parent before grandparent
		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible && !tiles[i].isReady) {
				drawProxyTile(mapPosition, tiles[i]);
			}
		}
		// GlUtils.checkGlError("end draw");

		glDisable(GLES20.GL_DEPTH_TEST);

		mDrawCount = 0;
		mDrawSerial++;

		glEnable(GL_BLEND);
		int z = mapPosition.zoomLevel;
		float s = mapPosition.scale;

		int zoomLevelDiff = Math.max(z - MapGenerator.STROKE_MAX_ZOOM_LEVEL, 0);
		float scale = (float) Math.pow(1.4, zoomLevelDiff);
		if (scale < 1)
			scale = 1;

		if (z >= MapGenerator.STROKE_MAX_ZOOM_LEVEL)
			TextRenderer.beginDraw(FloatMath.sqrt(s) / scale, mRotTMatrix);
		else
			TextRenderer.beginDraw(s, mRotTMatrix);

		for (int i = 0; i < tileCnt; i++) {
			if (!tiles[i].isVisible || tiles[i].texture == null)
				continue;

			setMatrix(mapPosition, tiles[i], 1, 0);
			TextRenderer.drawTile(tiles[i], mMVPMatrix);
		}
		TextRenderer.endDraw();

		if (MapView.debugFrameTime) {
			GLES20.glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start));
		}
	}

	// used to not draw a tile twice per frame...
	private static byte mDrawSerial = 0;

	private static void drawTile(MapPosition mapPosition, MapTile tile, float div) {
		// draw parents only once
		if (tile.lastDraw == mDrawSerial)
			return;

		tile.lastDraw = mDrawSerial;

		int z = mapPosition.zoomLevel;
		float s = mapPosition.scale;

		// mDrawCount is used to calculation z offset.
		// (used for depth clipping)
		setMatrix(mapPosition, tile, div, mDrawCount++);

		glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

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

				clipped = true;

			} else {
				// XXX nasty
				if (!clipped) {
					PolygonLayers.drawPolygons(null, 0, mMVPMatrix, z, s, true);
					clipped = true;
				}

				glEnable(GL_BLEND);

				ll = LineLayers.drawLines(tile, ll, pnext, mMVPMatrix, div, z, s);
			}
		}
	}

	private static boolean drawProxyChild(MapPosition mapPosition, MapTile tile) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if (tile.rel.child[i] == null)
				continue;

			MapTile c = tile.rel.child[i].tile;
			if (c == null)
				continue;

			if (!isVisible(mapPosition, c, 2)) {
				drawn++;
				continue;
			}

			if (c.isReady) {
				drawTile(mapPosition, c, 2);
				drawn++;
			}
		}
		return drawn == 4;
	}

	private static void drawProxyTile(MapPosition mapPosition, MapTile tile) {

		if (mapPosition.scale > 1.5f) {
			// prefer drawing children
			if (!drawProxyChild(mapPosition, tile)) {
				MapTile t = tile.rel.parent.tile;
				if (t != null) {
					if (t.isReady) {
						drawTile(mapPosition, t, 0.5f);
					} else {
						MapTile p = t.rel.parent.tile;
						if (p != null && p.isReady)
							drawTile(mapPosition, p, 0.25f);
					}
				}
			}
		} else {
			// prefer drawing parent
			MapTile t = tile.rel.parent.tile;

			if (t != null && t.isReady) {
				drawTile(mapPosition, t, 0.5f);

			} else if (!drawProxyChild(mapPosition, tile)) {

				// need to check rel.parent here, t could alread be root
				if (t != null) {
					t = t.rel.parent.tile;

					if (t != null && t.isReady)
						drawTile(mapPosition, t, 0.25f);
				}
			}
		}
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		Log.d(TAG, "SurfaceChanged:" + width + " " + height);

		drawTiles = curTiles = null;
		mBufferMemoryUsage = 0;

		if (width <= 0 || height <= 0)
			return;

		mWidth = width;
		mHeight = height;
		mAspect = (float) height / width;

		Matrix.orthoM(mProjMatrix, 0, -0.5f / mAspect, 0.5f / mAspect, -0.5f, 0.5f, -1, 1);

		// Matrix.frustumM(mProjMatrix, 0, -0.5f / mAspect, 0.5f / mAspect, -0.5f, 0.7f,
		// 1, 100);

		glViewport(0, 0, width, height);

		int numTiles = (mWidth / (Tile.TILE_SIZE / 2) + 2)
				* (mHeight / (Tile.TILE_SIZE / 2) + 2);

		TileLoader.init(mMapView, width, height, numTiles);

		drawTiles = new TilesData(numTiles);
		curTiles = new TilesData(numTiles);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + (numTiles * 2));
		int[] mVboIds = new int[numVBO];
		glGenBuffers(numVBO, mVboIds, 0);
		GlUtils.checkGlError("glGenBuffers");

		mVBOs = new ArrayList<VertexBufferObject>(numVBO);

		for (int i = 1; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		// Set up textures
		TextRenderer.init(numTiles);

		if (mClearColor != null) {
			glClearColor(mClearColor[0], mClearColor[1],
					mClearColor[2], mClearColor[3]);
		} else {
			glClearColor(0.98f, 0.98f, 0.97f, 1.0f);
		}

		glClearStencil(0);
		glClear(GL_STENCIL_BUFFER_BIT);

		// glEnable(GL_SCISSOR_TEST);
		// glScissor(0, 0, mWidth, mHeight);

		glDisable(GLES20.GL_CULL_FACE);

		glBlendFunc(GLES20.GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		GlUtils.checkGlError("onSurfaceChanged");

		mMapView.redrawTiles();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		Log.d(TAG, "Extensions: " + ext);

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
