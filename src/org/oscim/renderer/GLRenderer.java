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
package org.oscim.renderer;

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

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer.TilesData;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.theme.RenderTheme;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

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

	public static final float COORD_MULTIPLIER = 8.0f;

	static int CACHE_TILES = CACHE_TILES_MAX;

	private final MapView mMapView;
	private final MapViewPosition mMapViewPosition;

	private static MapPosition mMapPosition;

	// private static ArrayList<BufferObject> mVBOs;

	static int mWidth, mHeight;

	private static int rotateBuffers = 2;
	private static ShortBuffer shortBuffer[];
	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	private static float[] mMVPMatrix = new float[16];
	private static float[] mProjMatrix = new float[16];
	private static float[] mTmpMatrix = new float[16];
	private static float[] mTileCoords = new float[8];
	private static float[] mDebugCoords = new float[8];

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

	// Add additional tiles that serve as placeholer when flipping
	// over date-line.
	// I dont really like this but cannot think of a better solution:
	// the other option would be to run scanbox each time for upload,
	// drawing, proxies and text layer. needing to add placeholder only
	// happens rarely, unless you live on Fidschi
	/* package */static int mHolderCount;

	static boolean[] vertexArray = { false, false };

	// TODO
	final class GLState {
		boolean blend = false;
		boolean depth = false;
	}

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

				if (xx < 0 || xx >= xmax) {
					// Log.d(TAG, "out of bounds " + y + " " + x + "/" + xx);
					continue;
				}
				for (int i = cnt; i < cnt + mHolderCount; i++)
					if (tiles[i].tileX == x && tiles[i].tileY == y) {
						found = true;
						break;
					}

				if (found) {
					// Log.d(TAG, "already added " + y + " " + x + "/" + xx);
					continue;
				}

				for (int i = 0; i < cnt; i++)
					if (tiles[i].tileX == xx && tiles[i].tileY == y) {
						tile = tiles[i];
						break;
					}

				if (tile == null) {
					// Log.d(TAG, "not found " + y + " " + x + "/" + xx);
					continue;
				}

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

	private static ArrayList<Overlay> mOverlays;

	/**
	 * Called by TileLoader when list of active tiles changed. the list is
	 * copied to mNextTiles to be used in next call to onDrawFrame
	 * 
	 * @param tiles
	 *            active tiles
	 * @param overlays
	 *            ...
	 */
	static void updateTiles(TilesData tiles, ArrayList<Overlay> overlays) {

		MapTile[] newTiles = tiles.tiles;

		// lock tiles (and their proxies) to not be removed from cache
		for (int i = 0, n = tiles.cnt; i < n; i++)
			newTiles[i].lock();

		mOverlays = overlays;

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

	private static int uploadCnt = 0;

	private static boolean uploadLayers(Layers layers, BufferObject vbo, boolean addFill) {

		int newSize = layers.getSize();
		if (newSize == 0)
			return false;

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, vbo.id);

		// use multiple buffers to avoid overwriting buffer while current
		// data is uploaded (or rather the blocking which is probably done to
		// avoid overwriting)
		if (uploadCnt >= rotateBuffers) {
			uploadCnt = 0;
		}

		ShortBuffer sbuf = shortBuffer[uploadCnt];

		// add fill coordinates
		if (addFill)
			newSize += 8;

		// probably not a good idea to do this in gl thread...
		if (sbuf.capacity() < newSize) {
			sbuf = ByteBuffer
					.allocateDirect(newSize * SHORT_BYTES)
					.order(ByteOrder.nativeOrder())
					.asShortBuffer();

			shortBuffer[uploadCnt] = sbuf;
			if (addFill)
				sbuf.put(mFillCoords, 0, 8);
		} else {
			sbuf.clear();
			if (addFill)
				sbuf.put(mFillCoords, 0, 8);
			// if (addFill)
			// sbuf.position(8);
		}

		layers.compile(sbuf, addFill);
		sbuf.flip();

		if (newSize != sbuf.remaining()) {
			Log.d(TAG, "wrong size: "
					+ newSize + " "
					+ sbuf.position() + " "
					+ sbuf.limit() + " "
					+ sbuf.remaining());

			// tile.newData = false;
			return false;
		}
		newSize *= SHORT_BYTES;

		// reuse memory allocated for vbo when possible and allocated
		// memory is less then four times the new data
		if (vbo.size > newSize && vbo.size < newSize * 4
				&& mBufferMemoryUsage < LIMIT_BUFFERS) {
			GLES20.glBufferSubData(GL_ARRAY_BUFFER, 0, newSize, sbuf);
		} else {
			mBufferMemoryUsage -= vbo.size;
			vbo.size = newSize;
			GLES20.glBufferData(GL_ARRAY_BUFFER, vbo.size, sbuf, GL_DYNAMIC_DRAW);
			mBufferMemoryUsage += vbo.size;
		}

		uploadCnt++;

		return true;
	}

	private static boolean uploadTileData(MapTile tile) {

		if (uploadLayers(tile.layers, tile.vbo, true))
			tile.isReady = true;

		tile.newData = false;

		return tile.isReady;
	}

	private static boolean uploadOverlayData(Overlay overlay) {

		if (uploadLayers(overlay.layers, overlay.vbo, true))
			overlay.isReady = true;

		overlay.newData = false;

		return overlay.isReady;
	}

	private static void checkBufferUsage() {
		// try to clear some unused vbo when exceding limit

		if (mBufferMemoryUsage < LIMIT_BUFFERS) {
			if (CACHE_TILES < CACHE_TILES_MAX)
				CACHE_TILES += 50;
			return;
		}

		Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / MB + "MB");

		mBufferMemoryUsage -= BufferObject.limitUsage(2 * MB);

		Log.d(TAG, "now: " + mBufferMemoryUsage / MB + "MB");

		if (mBufferMemoryUsage > LIMIT_BUFFERS && CACHE_TILES > 100)
			CACHE_TILES -= 50;
	}

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

		Matrix.multiplyMM(matrix, 0, mapPosition.viewMatrix, 0, matrix, 0);

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

		if (mUpdateTiles) {
			// get current tiles to draw
			GLRenderer.tilelock.lock();
			mUpdateTiles = false;

			TilesData tmp = mDrawTiles;
			mDrawTiles = mNextTiles;
			mNextTiles = tmp;
			GLRenderer.tilelock.unlock();

			// force update of mapPosition
			mMapPosition.zoomLevel = -1;
		}

		if (mDrawTiles == null || mDrawTiles.cnt == 0) {
			GLRenderer.drawlock.unlock();
			return;
		}

		// get current MapPosition, set mTileCoords (mapping of screen to model
		// coordinates)
		MapPosition mapPosition = mMapPosition;
		float[] coords = mTileCoords;
		boolean changed = mMapViewPosition.getMapPosition(mapPosition, coords);

		for (Overlay overlay : mOverlays) {
			overlay.update(mMapView);
		}

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
			float px = (float) mapPosition.x * div;
			float py = (float) mapPosition.y * div;

			for (int i = 0; i < 8; i += 2) {
				coords[i + 0] = (px + coords[i + 0] / scale) / s;
				coords[i + 1] = (py + coords[i + 1] / scale) / s;
			}

			mHolderCount = 0;
			mScanBox.scan(coords, tiles[0].zoomLevel);
		}

		tileCnt += mHolderCount;

		// Log.d(TAG, "visible: " + tileCnt);

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
				// load tile that is referenced by this holder
				if (tile.holder.newData)
					uploadTileData(tile.holder);

				tile.isReady = tile.holder.isReady;

			} else if (!tile.isReady) {
				// check near relatives than can serve as proxy
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile rel = tile.rel.parent.tile;
					if (rel.newData)
						uploadTileData(rel);

					continue;
				}
				for (int c = 0; c < 4; c++) {
					if ((tile.proxies & 1 << c) == 0)
						continue;

					MapTile rel = tile.rel.child[c].tile;
					if (rel != null && rel.newData)
						uploadTileData(rel);
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
		// buffer.
		// TODO draw all parent before grandparent
		// TODO draw proxies for placeholder...
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

			int zoomLevelDiff = Math.max(z - TileGenerator.STROKE_MAX_ZOOM_LEVEL,
					0);
			float scale = (float) Math.pow(1.4, zoomLevelDiff);
			if (scale < 1)
				scale = 1;

			if (z >= TileGenerator.STROKE_MAX_ZOOM_LEVEL)
				TextRenderer.beginDraw(scale / FloatMath.sqrt(s), mProjMatrix);
			else
				TextRenderer.beginDraw(1f / s, mProjMatrix);

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

		// call overlay renderer
		for (Overlay overlay : mOverlays) {
			if (overlay.newData) {
				if (overlay.vbo == null)
					overlay.vbo = BufferObject.get();

				if (overlay.vbo == null)
					continue;

				if (uploadOverlayData(overlay))
					overlay.isReady = true;
			}
			if (!overlay.isReady)
				continue;

			// setMatrix(mMVPMatrix, overlay);
			overlay.render(mMapPosition, mMVPMatrix, mProjMatrix);
		}

		if (MapView.debugFrameTime) {
			GLES20.glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start));
		}

		if (debugView) {
			float mm = 0.5f;
			float min = -mm;
			float max = mm;
			float ymax = mm * mHeight / mWidth;
			mDebugCoords[0] = min;
			mDebugCoords[1] = ymax;
			mDebugCoords[2] = max;
			mDebugCoords[3] = ymax;
			mDebugCoords[4] = min;
			mDebugCoords[5] = -ymax;
			mDebugCoords[6] = max;
			mDebugCoords[7] = -ymax;

			PolygonRenderer.debugDraw(mProjMatrix, mDebugCoords, 0);

			mapPosition.zoomLevel = -1;
			mMapViewPosition.getMapPosition(mapPosition, mDebugCoords);
			Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0,
					mapPosition.viewMatrix, 0);
			PolygonRenderer.debugDraw(mMVPMatrix, mDebugCoords, 1);
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

		boolean clipped = false;
		int simpleShader = 0; // mRotate ? 0 : 1;

		for (Layer l = tile.layers.layers; l != null;) {

			switch (l.type) {
				case Layer.POLYGON:

					GLES20.glDisable(GL_BLEND);
					l = PolygonRenderer.draw(pos, l, mvp, !clipped, true);
					clipped = true;
					break;

				case Layer.LINE:
					if (!clipped) {
						PolygonRenderer.draw(pos, null, mvp, true, true);
						clipped = true;
					}

					GLES20.glEnable(GL_BLEND);
					l = LineRenderer.draw(pos, l, mvp, div, simpleShader,
							tile.layers.lineOffset);
					break;
			}
		}

		if (tile.layers.symbolLayers != null) {
			setMatrix(mvp, tile, div, false);

			for (Layer l = tile.layers.symbolLayers; l != null;) {
				l = TextureRenderer.draw(l, 1, mProjMatrix, mvp,
						tile.layers.symbolOffset);
			}
		}
	}

	private static boolean drawProxyChild(MapTile tile) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if ((tile.proxies & 1 << i) == 0)
				continue;

			MapTile c = tile.rel.child[i].tile;

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

		float s = MapViewPosition.VIEW_SCALE;
		float aspect = mHeight / (float) mWidth;

		Matrix.frustumM(mProjMatrix, 0, -1 * s, 1 * s,
				aspect * s, -aspect * s, MapViewPosition.VIEW_NEAR,
				MapViewPosition.VIEW_FAR);

		Matrix.setIdentityM(mTmpMatrix, 0);
		Matrix.translateM(mTmpMatrix, 0, 0, 0, -MapViewPosition.VIEW_DISTANCE);
		Matrix.multiplyMM(mProjMatrix, 0, mProjMatrix, 0, mTmpMatrix, 0);

		if (debugView) {
			// modify this to scale only the view, to see better which tiles are
			// rendered
			Matrix.setIdentityM(mMVPMatrix, 0);
			Matrix.scaleM(mMVPMatrix, 0, 0.5f, 0.5f, 1);
			Matrix.multiplyMM(mProjMatrix, 0, mMVPMatrix, 0, mProjMatrix, 0);
		}

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
		BufferObject.init(numVBO);

		// Set up textures
		TextRenderer.setup(numTiles);

		if (mClearColor != null)
			mUpdateColor = true;

		vertexArray[0] = false;
		vertexArray[1] = false;

		// FIXME this should be synchronized
		mMapView.redrawMap();
	}

	// FIXME this is a bit too spaghetti
	void clearTiles(int numTiles) {
		mDrawTiles = new TilesData(numTiles);
		mNextTiles = new TilesData(numTiles);
		mMapPosition.zoomLevel = -1;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		// Log.d(TAG, "Extensions: " + ext);

		LineRenderer.init();
		PolygonRenderer.init();
		TextRenderer.init();
		TextureRenderer.init();
		TextureObject.init(10);

		// glEnable(GL_SCISSOR_TEST);
		// glScissor(0, 0, mWidth, mHeight);
		GLES20.glClearStencil(0);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		mNewSurface = true;
	}

	private boolean mNewSurface;

	private static final boolean debugView = false;
}
