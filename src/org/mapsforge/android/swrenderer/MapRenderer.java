/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mapsforge.android.swrenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.mapsforge.android.DebugSettings;
import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.JobParameters;
import org.mapsforge.android.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.mapgenerator.TileCacheKey;
import org.mapsforge.android.mapgenerator.TileDistanceSort;
import org.mapsforge.android.utils.GlUtils;
import org.mapsforge.core.MapPosition;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Tile;

import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

/**
 * 
 */
public class MapRenderer implements org.mapsforge.android.IMapRenderer {
	// private static String TAG = "MapRenderer";

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

	private int mProgram;
	private int muMVPMatrixHandle;
	private int maPositionHandle;
	private int maTextureHandle;
	private int muScaleHandle;
	private FloatBuffer mVertices;
	private float[] mMatrix = new float[16];

	private int mWidth, mHeight;
	private double mDrawX, mDrawY;
	private long mTileX, mTileY;
	private float mMapScale;
	private DebugSettings mDebugSettings;
	private JobParameters mJobParameter;
	private MapPosition mMapPosition, mPrevMapPosition;

	private ArrayList<MapGeneratorJob> mJobList;

	ArrayList<Integer> mTextures;
	MapView mMapView;

	GLMapTile[] currentTiles;
	GLMapTile[] newTiles;
	int currentTileCnt = 0;

	private TileCacheKey mTileCacheKey;
	private LinkedHashMap<TileCacheKey, GLMapTile> mTiles;
	private ArrayList<GLMapTile> mTileList;

	private boolean processedTile = true;

	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;

	private boolean mInitial;

	private final TileDistanceSort tileDistanceSort = new TileDistanceSort();

	/**
	 * @param mapView
	 *            the MapView
	 */
	public MapRenderer(MapView mapView) {
		mMapView = mapView;
		mDebugSettings = mapView.getDebugSettings();
		mMapScale = 1;

		float[] vertices = {
				0, 0, 0, 0, 0.5f,
				0, 1, 0, 0, 0,
				1, 0, 0, 0.5f, 0.5f,
				1, 1, 0, 0.5f, 0 };

		mVertices = ByteBuffer.allocateDirect(4 * TRIANGLE_VERTICES_DATA_STRIDE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mVertices.put(vertices);

		mTextures = new ArrayList<Integer>();
		mJobList = new ArrayList<MapGeneratorJob>();

		mTiles = new LinkedHashMap<TileCacheKey, GLMapTile>(100);
		mTileList = new ArrayList<GLMapTile>();

		mTileCacheKey = new TileCacheKey();
		mInitial = true;
	}

	private void limitCache(byte zoom, int remove) {
		long x = mTileX;
		long y = mTileY;
		int diff;

		for (GLMapTile t : mTileList) {

			diff = (t.zoomLevel - zoom);

			if (diff != 0)
			{
				float z = (diff > 0) ? (1 << diff) : 1.0f / (1 << -diff);
				t.distance = (long) (Math.abs((t.tileX) * z - x) + Math.abs((t.tileY) * z
						- y));
				t.distance *= 2 * diff * diff;
			} else {
				t.distance = (Math.abs(t.tileX - x) + Math.abs(t.tileY - y));
			}
		}

		Collections.sort(mTileList, tileDistanceSort);

		for (int j = mTileList.size() - 1, cnt = 0; cnt < remove; j--, cnt++) {
			GLMapTile t = mTileList.remove(j);

			mTileCacheKey.set(t.tileX, t.tileY, t.zoomLevel);
			mTiles.remove(mTileCacheKey);

			for (int i = 0; i < 4; i++) {
				if (t.child[i] != null)
					t.child[i].parent = null;
			}
			if (t.parent != null) {
				for (int i = 0; i < 4; i++) {
					if (t.parent.child[i] == t)
						t.parent.child[i] = null;
				}
			}
			if (t.hasTexture()) {
				synchronized (mTextures) {
					mTextures.add(Integer.valueOf(t.getTexture()));
				}
			}
		}
	}

	private boolean updateVisibleList(long x, long y, byte zoomLevel) {
		float scale = mMapPosition.scale;
		double add = 1.0f / scale;
		int offsetX = (int) ((mWidth >> 1) * add);
		int offsetY = (int) ((mHeight >> 1) * add);

		long pixelRight = x + offsetX;
		long pixelBottom = y + offsetY;
		long pixelLeft = x - offsetX;
		long pixelTop = y - offsetY;

		long tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, zoomLevel);
		long tileTop = MercatorProjection.pixelYToTileY(pixelTop, zoomLevel);
		long tileRight = MercatorProjection.pixelXToTileX(pixelRight, zoomLevel);
		long tileBottom = MercatorProjection.pixelYToTileY(pixelBottom, zoomLevel);

		mJobList.clear();

		// IMapGenerator mapGenerator = mMapView.getMapGenerator();

		int tiles = 0;
		for (long tileY = tileTop - 1; tileY <= tileBottom + 1; tileY++) {
			for (long tileX = tileLeft - 1; tileX <= tileRight + 1; tileX++) {

				GLMapTile tile = mTiles.get(mTileCacheKey.set(tileX, tileY, zoomLevel));

				if (tile == null) {
					tile = new GLMapTile(tileX, tileY, zoomLevel);
					TileCacheKey key = new TileCacheKey(mTileCacheKey);
					mTiles.put(key, tile);

					mTileCacheKey.set((tileX >> 1), (tileY >> 1), (byte) (zoomLevel - 1));
					tile.parent = mTiles.get(mTileCacheKey);

					long xx = tileX << 1;
					long yy = tileY << 1;
					byte z = (byte) (zoomLevel + 1);

					tile.child[0] = mTiles.get(mTileCacheKey.set(xx, yy, z));
					tile.child[1] = mTiles.get(mTileCacheKey.set(xx + 1, yy, z));
					tile.child[2] = mTiles.get(mTileCacheKey.set(xx, yy + 1, z));
					tile.child[3] = mTiles.get(mTileCacheKey.set(xx + 1, yy + 1, z));

					mTileList.add(tile);
				}

				newTiles[tiles++] = tile;

				if (!tile.isReady || (tile.getScale() != scale)) {
					tile.isLoading = true;
					// approximation for TileScheduler
					if (tileY < tileTop || tileY > tileBottom || tileX < tileLeft
							|| tileX > tileRight)
						tile.isVisible = false;
					else
						tile.isVisible = true;

					MapGeneratorJob job = new MapGeneratorJob(tile, mJobParameter,
							mDebugSettings);
					job.setScale(scale);
					mJobList.add(job);
				}
			}
		}

		synchronized (this) {

			limitCache(zoomLevel, (mTiles.size() - 200));

			for (int i = 0; i < tiles; i++)
				currentTiles[i] = newTiles[i];
			currentTileCnt = tiles;

			mDrawX = x;
			mDrawY = y;
			mMapScale = scale;
		}

		if (mJobList.size() > 0) {
			mMapView.addJobs(mJobList);
		}

		return true;
	}

	/**
	 * 
	 */
	@Override
	public synchronized void redrawTiles(boolean clear) {

		boolean update = false;

		mMapPosition = mMapView.getMapPosition().getMapPosition();

		long x = (long) MercatorProjection.longitudeToPixelX(
				mMapPosition.geoPoint.getLongitude(),
				mMapPosition.zoomLevel);
		long y = (long) MercatorProjection
				.latitudeToPixelY(mMapPosition.geoPoint.getLatitude(),
						mMapPosition.zoomLevel);

		long tileX = MercatorProjection.pixelXToTileX(x, mMapPosition.zoomLevel);
		long tileY = MercatorProjection.pixelYToTileY(y, mMapPosition.zoomLevel);
		float scale = mMapPosition.scale;

		if (mInitial) {
			mInitial = false;
			mPrevMapPosition = mMapPosition;
			mTileX = tileX;
			mTileY = tileY;
			update = true;
		} else if (mPrevMapPosition.zoomLevel != mMapPosition.zoomLevel) {
			update = true;
		} else if (mMapScale != scale) {
			update = true;
		} else if (tileX != mTileX || tileY != mTileY) {
			update = true;
		}

		mTileX = tileX;
		mTileY = tileY;

		if (update) {
			// do not change list while drawing
			// synchronized (this) {
			mPrevMapPosition = mMapPosition;
			updateVisibleList(x, y, mMapPosition.zoomLevel);
		}
		else {
			synchronized (this) {
				mDrawX = x;
				mDrawY = y;
			}
		}

		mMapView.requestRender();
	}

	private MapGeneratorJob mMapGeneratorJob = null;

	@Override
	public boolean passTile(MapGeneratorJob mapGeneratorJob) {

		mMapGeneratorJob = mapGeneratorJob;
		processedTile = false;
		mMapView.requestRender();

		return true;
	}

	private boolean drawTile(GLMapTile tile, int level, float height) {

		// do not recurse more than two parents
		if (level > 2)
			return true;

		if (!tile.hasTexture()) {
			// draw parent below current zoom level tiles
			float h = height > 0 ? height * 2 : 0.1f;

			if (level <= 2 && tile.parent != null)
				return drawTile(tile.parent, level + 1, h);

			return false;
		}

		float z = 1;
		double drawX = mDrawX;
		double drawY = mDrawY;
		// translate all pixel coordinates * 'zoom factor difference'
		// TODO clip tile when drawing parent
		int diff = tile.zoomLevel - mMapPosition.zoomLevel;
		if (diff != 0) {
			if (diff > 0) {
				z = (1 << diff);
			} else {
				z = 1.0f / (1 << -diff);
			}
			drawX = MercatorProjection
					.longitudeToPixelX(mMapPosition.geoPoint.getLongitude(),
							tile.zoomLevel);
			drawY = MercatorProjection
					.latitudeToPixelY(mMapPosition.geoPoint.getLatitude(), tile.zoomLevel);

		}

		float mapScale = mMapScale / z;
		int tileSize = Tile.TILE_SIZE;
		float size = tileSize * mapScale;

		float x = (float) ((tile.pixelX) - drawX) * mapScale;
		float y = (float) ((tile.pixelY + tileSize) - drawY) * mapScale;

		if (x + size < -mWidth / 2 || x > mWidth / 2) {
			// Log.i(TAG, tile + " skip X " + x + " " + y);
			tile.isVisible = false;
			return true;
		}
		if (y < -mHeight / 2 || y - size > mHeight / 2) {
			// Log.i(TAG, tile + " skip Y " + x + " " + y);
			tile.isVisible = false;
			return true;
		}

		// Log.i(TAG, tile + " draw " + x + " " + y);
		tile.isVisible = true;

		// set drawn tile scale (texture size)
		GLES20.glUniform1f(muScaleHandle, tile.getScale());

		Matrix.setIdentityM(mMatrix, 0);
		// map tile GL coordinates to screen coordinates
		Matrix.scaleM(mMatrix, 0, 2.0f * (tileSize * z) / mWidth, 2.0f * (tileSize * z)
				/ mHeight, 1);

		// scale tile
		Matrix.scaleM(mMatrix, 0, mapScale / z, mapScale / z, 1);

		// translate tile
		Matrix.translateM(mMatrix, 0, (x / size), -(y / size), height);

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.getTexture());
		// GlUtils.checkGlError("glBindTexture");

		GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMatrix, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		return true;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		// boolean loadedTexture = false;
		GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
		GLES20.glClearColor(0.95f, 0.95f, 0.94f, 1.0f);
		// GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glUseProgram(mProgram);
		GlUtils.checkGlError("glUseProgram");

		mVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
		GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mVertices);
		GlUtils.checkGlError("glVertexAttribPointer maPosition");

		GLES20.glEnableVertexAttribArray(maPositionHandle);
		GlUtils.checkGlError("glEnableVertexAttribArray maPositionHandle");

		mVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
		GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
				TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mVertices);
		GlUtils.checkGlError("glVertexAttribPointer maTextureHandle");

		GLES20.glEnableVertexAttribArray(maTextureHandle);
		GlUtils.checkGlError("glEnableVertexAttribArray maTextureHandle");

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

		GLMapTile tile, child, child2;

		GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

		// lock position and currentTiles while drawing
		synchronized (this) {
			if (mMapGeneratorJob != null) {

				tile = (GLMapTile) mMapGeneratorJob.tile;
				// TODO tile bitmaps texture to smaller parts avoiding uploading full
				// bitmap when not necessary
				if (tile.getTexture() >= 0) {
					// reuse tile texture
					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.getTexture());
					GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
							mMapGeneratorJob.getBitmap());
				} else if (mTextures.size() > 0) {
					// reuse texture from previous tiles
					Integer texture;
					texture = mTextures.remove(mTextures.size() - 1);

					GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.intValue());
					GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
							mMapGeneratorJob.getBitmap());
					tile.setTexture(texture.intValue());
				} else {
					// create texture
					tile.setTexture(GlUtils.loadTextures(mMapGeneratorJob.getBitmap()));
				}

				tile.setScale(mMapGeneratorJob.getScale());
				tile.isReady = true;
				tile.isLoading = false;

				mMapGeneratorJob = null;
				processedTile = true;
				// loadedTexture = true;
			}
			int tileSize = (int) (Tile.TILE_SIZE * mMapScale);
			int hWidth = mWidth >> 1;
			int hHeight = mHeight >> 1;
			for (int i = 0, n = currentTileCnt; i < n; i++) {
				tile = currentTiles[i];

				float x = (float) (tile.pixelX - mDrawX);
				float y = (float) (tile.pixelY - mDrawY);

				// clip rendering to tile boundaries
				GLES20.glScissor(
						hWidth + (int) (x * mMapScale) - 2,
						hHeight - (int) (y * mMapScale) - tileSize - 2,
						tileSize + 4, tileSize + 4);

				if (drawTile(tile, 0, 0.0f))
					continue;

				// or two zoom level above
				for (int k = 0; k < 4; k++) {
					if (((child = tile.child[k]) != null)) {

						if (drawTile(child, 2, 0.1f))
							continue;

						for (int j = 0; j < 4; j++)
							if ((child2 = child.child[j]) != null)
								drawTile(child2, 2, 0.1f);
					}
				}
			}
		}
		// FIXME
		// if (loadedTexture) {
		// synchronized (mMapWorker) {
		// mMapWorker.notify();
		// }
		// }
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		mWidth = width;
		mHeight = height;

		int tiles = (mWidth / Tile.TILE_SIZE + 4) * (mHeight / Tile.TILE_SIZE + 4);
		currentTiles = new GLMapTile[tiles];
		newTiles = new GLMapTile[tiles];

		GLES20.glViewport(0, 0, width, height);

		mDebugSettings = mMapView.getDebugSettings();
		mJobParameter = mMapView.getJobParameters();

		mTiles.clear();
		mTileList.clear();
		mTextures.clear();
		mInitial = true;
		mMapView.redrawTiles();
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

		mProgram = GlUtils.createProgram(mVertexShader, mFragmentShader);
		if (mProgram == 0) {
			return;
		}
		maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
		GlUtils.checkGlError("glGetAttribLocation aPosition");
		if (maPositionHandle == -1) {
			throw new RuntimeException("Could not get attrib location for aPosition");
		}
		maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
		GlUtils.checkGlError("glGetAttribLocation aTextureCoord");
		if (maTextureHandle == -1) {
			throw new RuntimeException("Could not get attrib location for aTextureCoord");
		}
		muScaleHandle = GLES20.glGetUniformLocation(mProgram, "uScale");
		GlUtils.checkGlError("glGetAttribLocation uScale");
		if (muScaleHandle == -1) {
			throw new RuntimeException("Could not get attrib location for uScale");
		}

		muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		GlUtils.checkGlError("glGetUniformLocation uMVPMatrix");
		if (muMVPMatrixHandle == -1) {
			throw new RuntimeException("Could not get attrib location for uMVPMatrix");
		}

		// GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		// GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glCullFace(GLES20.GL_BACK);
		GLES20.glFrontFace(GLES20.GL_CW);
		GLES20.glEnable(GLES20.GL_CULL_FACE);

	}

	private final String mVertexShader = "precision highp float;\n" +
			"uniform float uScale;\n" +
			"uniform mat4 uMVPMatrix;\n" + "attribute vec4 aPosition;\n" +
			"attribute vec2 aTextureCoord;\n" +
			"varying vec2 vTextureCoord;\n" + "void main() {\n" +
			"  gl_Position = uMVPMatrix * aPosition;\n" +
			"  vTextureCoord = aTextureCoord * uScale;\n" +
			"}\n";

	private final String mFragmentShader = "precision highp float;\n" +
			"uniform float uScale;\n" +
			"varying vec2 vTextureCoord;\n" +
			"uniform sampler2D sTexture;\n" +
			"void main() {\n" +
			"  gl_FragColor = texture2D(sTexture, vTextureCoord); \n" +
			"}\n";

	@Override
	public boolean processedTile() {
		return processedTile;
	}

	@Override
	public IMapGenerator createMapGenerator() {
		return new MapGenerator();
	}
}
