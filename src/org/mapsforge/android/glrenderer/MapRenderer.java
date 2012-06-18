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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.mapsforge.android.DebugSettings;
import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.JobParameters;
import org.mapsforge.android.mapgenerator.MapGenerator;
import org.mapsforge.android.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.mapgenerator.MapWorker;
import org.mapsforge.android.mapgenerator.TileCacheKey;
import org.mapsforge.android.mapgenerator.TileDistanceSort;
import org.mapsforge.android.utils.GlConfigChooser;
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
public class MapRenderer implements org.mapsforge.android.MapRenderer {
	private static final String TAG = "MapRenderer";

	private static final int CACHE_TILES = 250;
	private static final int LIMIT_BUFFERS = 32 * (1024 * 1024);

	private static final int OES_HALF_FLOAT = 0x8D61;
	private static final int FLOAT_BYTES = 4;
	private static final int SHORT_BYTES = 2;
	private static final int POLYGON_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_TEX_OFFSET = 8;

	private static int STENCIL_BITS = 8;

	private final MapView mMapView;
	private final MapWorker mMapWorker;

	private final ArrayList<MapGeneratorJob> mJobList;
	private final ArrayList<VertexBufferObject> mVBOs;
	private final TileCacheKey mTileCacheKey;
	private final HashMap<TileCacheKey, GLMapTile> mTiles;
	private final ArrayList<GLMapTile> mTileList;
	private final TileDistanceSort mTileDistanceSort;

	private DebugSettings mDebugSettings;
	private JobParameters mJobParameter;
	private MapPosition mMapPosition, mPrevMapPosition;

	private int mWidth, mHeight;
	private float mAspect;

	// draw position is updated from current position in onDrawFrame
	// keeping the position consistent while drawing
	private double mDrawX, mDrawY, mDrawZ, mCurX, mCurY, mCurZ;
	private float mDrawScale, mCurScale;

	// current center tile
	private long mTileX, mTileY;

	private FloatBuffer floatBuffer = null;
	private ByteBuffer byteBuffer = null;

	boolean useHalfFloat = false;

	// bytes currently loaded in VBOs
	private int mBufferMemoryUsage;

	// flag set by updateVisibleList when current visible tiles changed.
	// used in onDrawFrame to nextTiles to curTiles
	private boolean mUpdateTiles;

	class TilesData {
		int cnt = 0;
		final GLMapTile[] tiles;

		TilesData(int numTiles) {
			tiles = new GLMapTile[numTiles];
		}
	}

	private float[] mMVPMatrix = new float[16];
	// private float[] mMMatrix = new float[16];
	// private float[] mRMatrix = new float[16];

	// newTiles is set in updateVisibleList and synchronized swapped
	// with nextTiles on main thread.
	// nextTiles is swapped with curTiles in onDrawFrame in GL thread.
	private TilesData newTiles, nextTiles, curTiles;

	private boolean mInitial;

	// shader handles
	private int gLineProgram;
	private int gLineVertexPositionHandle;
	private int gLineTexturePositionHandle;
	private int gLineColorHandle;
	private int gLineMatrixHandle;
	private int gLineWidthHandle;
	private int gLineModeHandle;

	private int gPolygonProgram;
	private int gPolygonVertexPositionHandle;
	private int gPolygonMatrixHandle;
	private int gPolygonColorHandle;

	/**
	 * 
	 */
	public boolean timing = false;

	/**
	 * @param mapView
	 *            the MapView
	 */
	public MapRenderer(MapView mapView) {
		Log.d(TAG, "init MapRenderer");
		mMapView = mapView;
		mMapWorker = mapView.getMapWorker();
		mDebugSettings = mapView.getDebugSettings();

		mVBOs = new ArrayList<VertexBufferObject>();
		mJobList = new ArrayList<MapGeneratorJob>();

		mTiles = new HashMap<TileCacheKey, GLMapTile>(CACHE_TILES * 2);
		mTileList = new ArrayList<GLMapTile>();
		mTileCacheKey = new TileCacheKey();

		mTileDistanceSort = new TileDistanceSort();
		Matrix.setIdentityM(mMVPMatrix, 0);
		mInitial = true;
		mUpdateTiles = false;
	}

	private void updateTileDistances() {
		byte zoom = mMapPosition.zoomLevel;
		long x = mTileX;
		long y = mTileY;
		int diff;
		long dx, dy;
		for (int i = 0, n = mTileList.size(); i < n; i++) {
			GLMapTile t = mTileList.get(i);
			diff = (t.zoomLevel - zoom);

			if (diff != 0) {
				if (diff > 0) {
					dx = (t.tileX << diff) - x;
					dy = (t.tileY << diff) - y;
				} else {
					dx = (t.tileX >> -diff) - x;
					dy = (t.tileY >> -diff) - y;
				}

				t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy));
				t.distance *= (1 + t.zoomLevel);
			} else {
				dx = t.tileX - x;
				dy = t.tileY - y;
				t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy));
			}
		}
	}

	private void limitCache(int remove) {
		byte z = mMapPosition.zoomLevel;

		for (int j = mTileList.size() - 1, cnt = 0; cnt < remove && j > 0; j--, cnt++) {

			GLMapTile t = mTileList.remove(j);
			if (t.isActive) {
				// Log.d(TAG, "EEEK removing active tile");
				mTileList.add(t);
				continue;
			}
			// check if this tile is used as proxy for not yet drawn active tile
			if (t.isDrawn || t.newData || t.isLoading) {
				if (t.zoomLevel == z + 1) {
					if (t.parent != null && t.parent.isActive && !t.parent.isDrawn) {
						mTileList.add(t);
						// Log.d(TAG, "EEEK removing active proxy child");
						continue;
					}
				} else if (t.zoomLevel == z - 1) {
					GLMapTile c = null;
					for (int i = 0; i < 4; i++) {
						c = t.child[i];
						if (c != null && c.isActive && !(c.isDrawn || c.newData))
							break;
						c = null;
					}

					if (c != null) {
						// Log.d(TAG, "EEEK removing active proxy parent");
						mTileList.add(t);
						continue;
					}
				}
			}

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

			if (t.lineVBO != null) {
				synchronized (mVBOs) {
					mVBOs.add(t.lineVBO);
					mVBOs.add(t.polygonVBO);
					t.lineVBO = null;
					t.polygonVBO = null;
				}
			}
		}
	}

	private boolean updateVisibleList(double x, double y) {
		byte zoomLevel = mMapPosition.zoomLevel;
		float scale = mMapPosition.scale;
		double add = 1.0f / scale;
		int offsetX = (int) ((mWidth >> 1) * add);
		int offsetY = (int) ((mHeight >> 1) * add);

		long pixelRight = (long) x + offsetX;
		long pixelBottom = (long) y + offsetY;
		long pixelLeft = (long) x - offsetX;
		long pixelTop = (long) y - offsetY;

		long tileLeft = MercatorProjection.pixelXToTileX(pixelLeft, zoomLevel);
		long tileTop = MercatorProjection.pixelYToTileY(pixelTop, zoomLevel);
		long tileRight = MercatorProjection.pixelXToTileX(pixelRight, zoomLevel);
		long tileBottom = MercatorProjection.pixelYToTileY(pixelBottom, zoomLevel);

		mJobList.clear();
		mJobParameter = mMapView.getJobParameters();

		MapGenerator mapGenerator = mMapView.getMapGenerator();
		int tiles = 0;
		if (newTiles == null)
			return false;

		int max = newTiles.tiles.length - 1;

		for (long tileY = tileTop - 1; tileY <= tileBottom + 1; tileY++) {
			for (long tileX = tileLeft - 1; tileX <= tileRight + 1; tileX++) {
				// FIXME
				if (tiles == max)
					break;

				GLMapTile tile = mTiles.get(mTileCacheKey.set(tileX, tileY, zoomLevel));

				if (tile == null) {
					tile = new GLMapTile(tileX, tileY, zoomLevel);
					TileCacheKey key = new TileCacheKey(mTileCacheKey);
					mTiles.put(key, tile);
					mTileList.add(tile);

					mTileCacheKey.set((tileX >> 1), (tileY >> 1), (byte) (zoomLevel - 1));
					tile.parent = mTiles.get(mTileCacheKey);

					// set this tile to be child of its parent
					if (tile.parent != null) {
						int idx = (int) ((tileX & 0x01) + 2 * (tileY & 0x01));
						tile.parent.child[idx] = tile;
					}

					long xx = tileX << 1;
					long yy = tileY << 1;
					byte z = (byte) (zoomLevel + 1);

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

				newTiles.tiles[tiles++] = tile;

				if (!tile.isDrawn && !tile.isLoading) {
					MapGeneratorJob job = new MapGeneratorJob(tile, mapGenerator, mJobParameter, mDebugSettings);
					mJobList.add(job);
				}
			}
		}

		updateTileDistances();

		int removes = mTiles.size() - CACHE_TILES;

		if (removes > 0)
			Collections.sort(mTileList, mTileDistanceSort);

		synchronized (this) {
			for (int i = 0; i < nextTiles.cnt; i++)
				nextTiles.tiles[i].isActive = false;

			for (int i = 0; i < curTiles.cnt; i++)
				curTiles.tiles[i].isActive = true;

			for (int i = 0; i < tiles; i++)
				newTiles.tiles[i].isActive = true;

			TilesData tmp = nextTiles;
			nextTiles = newTiles;
			nextTiles.cnt = tiles;
			newTiles = tmp;

			mUpdateTiles = true;
		}

		limitCache(removes);

		if (mJobList.size() > 0) {
			mMapView.getJobQueue().setJobs(mJobList);
			synchronized (mMapWorker) {
				mMapWorker.notify();
			}
		}

		return true;
	}

	/**
	 * 
	 */
	@Override
	public synchronized void redrawTiles(boolean clear) {
		boolean changedPos = false;
		boolean changedZoom = false;
		mMapPosition = mMapView.getMapPosition().getMapPosition();

		if (mMapPosition == null) {
			Log.d(TAG, ">>> no map position");
			return;
		}

		if (clear) {
			mInitial = true;

			for (GLMapTile t : mTileList) {
				t.isDrawn = false;
				t.isLoading = false;
				t.newData = false;
			}
		}

		byte zoomLevel = mMapPosition.zoomLevel;
		float scale = mMapPosition.scale;

		double x = MercatorProjection.longitudeToPixelX(mMapPosition.geoPoint.getLongitude(), zoomLevel);
		double y = MercatorProjection.latitudeToPixelY(mMapPosition.geoPoint.getLatitude(), zoomLevel);

		long tileX = MercatorProjection.pixelXToTileX(x, zoomLevel);
		long tileY = MercatorProjection.pixelYToTileY(y, zoomLevel);

		if (mInitial || mPrevMapPosition.zoomLevel != zoomLevel)
			changedZoom = true;
		else if (tileX != mTileX || tileY != mTileY)
			changedPos = true;

		mInitial = false;

		mTileX = tileX;
		mTileY = tileY;
		mPrevMapPosition = mMapPosition;

		if (changedZoom)
			updateVisibleList(x, y);

		synchronized (this) {
			// do not change position while drawing
			mCurX = x;
			mCurY = y;
			mCurZ = zoomLevel;
			mCurScale = scale;
		}

		if (!timing)
			mMapView.requestRender();

		if (changedPos)
			updateVisibleList(x, y);
	}

	@Override
	public boolean passTile(MapGeneratorJob mapGeneratorJob) {
		if (!timing)
			mMapView.requestRender();
		return true;
	}

	private void fillPolygons(int[] colors, int count) {
		boolean blend = false;

		// draw to framebuffer
		GLES20.glColorMask(true, true, true, true);

		// do not modify stencil buffer
		GLES20.glStencilMask(0);

		for (int c = 0; c < count; c++) {
			int color = colors[c];
			float alpha = (color >> 24 & 0xff) / 255f;
			if (alpha != 1) {
				if (!blend) {
					GLES20.glEnable(GLES20.GL_BLEND);
					blend = true;
				}
			} else if (blend) {
				GLES20.glDisable(GLES20.GL_BLEND);
				blend = false;
			}

			GLES20.glUniform4f(gPolygonColorHandle, (color >> 16 & 0xff) / 255f * alpha, (color >> 8 & 0xff) / 255f
					* alpha, (color & 0xff) / 255f * alpha, alpha);

			// set stencil buffer mask used to draw this layer
			GLES20.glStencilFunc(GLES20.GL_EQUAL, 0xff, 1 << c);

			// draw tile fill coordinates
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		}

		if (blend)
			GLES20.glDisable(GLES20.GL_BLEND);
	}

	private int[] mPolyColors;

	private boolean drawPolygons(GLMapTile tile, int diff) {
		float scale, x, y;

		if (tile.polygonLayers == null || tile.polygonLayers.array == null)
			return true;

		GLES20.glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, tile.polygonVBO.id);

		if (useHalfFloat) {
			GLES20.glVertexAttribPointer(gPolygonVertexPositionHandle, 2, OES_HALF_FLOAT, false, 0,
					POLYGON_VERTICES_DATA_POS_OFFSET);
		} else {
			GLES20.glVertexAttribPointer(gPolygonVertexPositionHandle, 2, GLES20.GL_FLOAT, false, 0,
					POLYGON_VERTICES_DATA_POS_OFFSET);
		}

		if (diff == 0) {
			scale = (float) (mDrawScale * 2.0 / mHeight);
			x = (float) (mDrawX - tile.x);
			y = (float) (tile.y - mDrawY);
		} else {
			float z = (diff > 0) ? (1 << diff) : 1.0f / (1 << -diff);
			scale = (float) (mDrawScale * 2.0 / mHeight / z);
			x = (float) (mDrawX * z - tile.x);
			y = (float) (tile.y - mDrawY * z);
		}

		int cnt = 0;
		int[] colors = mPolyColors;

		mMVPMatrix[12] = -x * (scale * mAspect);
		mMVPMatrix[13] = -y * (scale);
		mMVPMatrix[0] = (scale * mAspect);
		mMVPMatrix[5] = (scale);

		GLES20.glUniformMatrix4fv(gPolygonMatrixHandle, 1, false, mMVPMatrix, 0);

		boolean firstPass = true;

		for (int i = 0, n = tile.polygonLayers.array.length; i < n; i++) {
			PolygonLayer l = tile.polygonLayers.array[i];

			if (cnt == 0) {
				// disable drawing to framebuffer
				GLES20.glColorMask(false, false, false, false);

				// never pass the test, i.e. always apply first stencil op (sfail)
				GLES20.glStencilFunc(GLES20.GL_NEVER, 0, 0xff);

				if (firstPass)
					firstPass = false;
				else {
					// clear stencilbuffer
					GLES20.glStencilMask(0xFF);
					GLES20.glClear(GLES20.GL_STENCIL_BUFFER_BIT);

					// clear stencilbuffer (tile region)
					// GLES20.glStencilOp(GLES20.GL_ZERO, GLES20.GL_ZERO, GLES20.GL_ZERO);
					// GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
				}

				// stencil op for stencil method polygon drawing
				GLES20.glStencilOp(GLES20.GL_INVERT, GLES20.GL_INVERT, GLES20.GL_INVERT);
			}

			colors[cnt] = l.color;

			// fade out polygon layers (set in RederTheme)
			if (l.fadeLevel > 0) {
				if (l.fadeLevel >= mDrawZ) {

					// skip layer when faded out
					if (l.fadeLevel > mDrawZ)
						continue;

					// modify alpha channel
					float s = (mDrawScale < 1.3f ? 1.3f : mDrawScale);
					colors[cnt] = (colors[cnt] & 0xffffff) | (byte) ((s - 1) * 0xff) << 24;
				}
			}

			// set stencil mask to draw to
			GLES20.glStencilMask(1 << cnt++);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 8 layers into stencil buffer
			if (cnt == STENCIL_BITS) {
				fillPolygons(colors, cnt);
				cnt = 0;
			}
		}

		if (cnt > 0)
			fillPolygons(colors, cnt);

		return true;
	}

	private boolean drawLines(GLMapTile tile, int diff) {
		float x, y, scale;
		float z = 1;

		if (tile.lineLayers == null || tile.lineLayers.array == null)
			return false;

		GLES20.glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, tile.lineVBO.id);

		if (useHalfFloat) {
			GLES20.glVertexAttribPointer(gLineVertexPositionHandle, 2, OES_HALF_FLOAT, false, 8,
					LINE_VERTICES_DATA_POS_OFFSET);

			GLES20.glVertexAttribPointer(gLineTexturePositionHandle, 2, OES_HALF_FLOAT, false, 8,
					LINE_VERTICES_DATA_TEX_OFFSET >> 1);
		} else {
			GLES20.glVertexAttribPointer(gLineVertexPositionHandle, 2, GLES20.GL_FLOAT, false, 16,
					LINE_VERTICES_DATA_POS_OFFSET);

			GLES20.glVertexAttribPointer(gLineTexturePositionHandle, 2, GLES20.GL_FLOAT, false, 16,
					LINE_VERTICES_DATA_TEX_OFFSET);
		}

		if (diff == 0) {
			scale = (float) (mDrawScale * 2.0 / mHeight);
			x = (float) (mDrawX - tile.x);
			y = (float) (tile.y - mDrawY);
		} else {
			z = (diff > 0) ? (1 << diff) : 1.0f / (1 << -diff);
			scale = (float) (mDrawScale * 2.0 / mHeight / z);
			x = (float) (mDrawX * z - tile.x);
			y = (float) (tile.y - mDrawY * z);
		}

		mMVPMatrix[12] = -x * (scale * mAspect);
		mMVPMatrix[13] = -y * (scale);
		mMVPMatrix[0] = (scale * mAspect);
		mMVPMatrix[5] = (scale);

		GLES20.glUniformMatrix4fv(gLineMatrixHandle, 1, false, mMVPMatrix, 0);

		LineLayer[] layers = tile.lineLayers.array;

		boolean drawOutlines = false;
		boolean drawFixed = false;

		// stroke scale factor (0.85 to give some room for anti-aliasing)
		float wdiv = 0.85f / FloatMath.sqrt(mDrawScale / z);
		// linear scale for fixed lines
		float fdiv = 0.85f / (mDrawScale / z);

		for (int i = 0, n = layers.length; i < n; i++) {
			LineLayer l = layers[i];

			// set line width and mode
			if (i == 0 | l.isOutline != drawOutlines || l.isFixed != drawFixed) {
				drawOutlines = l.isOutline;
				drawFixed = l.isFixed;
				if (drawFixed) {
					GLES20.glUniform1i(gLineModeHandle, 2);
					GLES20.glUniform1f(gLineWidthHandle, fdiv);
				} else if (drawOutlines) {
					GLES20.glUniform1i(gLineModeHandle, 1);
					GLES20.glUniform1f(gLineWidthHandle, wdiv);
				} else {
					GLES20.glUniform1i(gLineModeHandle, 0);
					GLES20.glUniform1f(gLineWidthHandle, wdiv);
				}
			}

			GLES20.glUniform4fv(gLineColorHandle, 1, l.colors, 0);

			if (drawOutlines) {
				for (int j = 0, m = l.outlines.size(); j < m; j++) {
					LineLayer o = l.outlines.get(j);

					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			} else {
				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}

		return true;
	}

	private boolean setTileScissor(GLMapTile tile, float div) {

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

		int sx = (int) (dx * scale);
		int sy = (int) (dy * scale);

		int sw = (int) ((dx + Tile.TILE_SIZE) * scale) - sx;
		int sh = (int) ((dy + Tile.TILE_SIZE) * scale) - sy;

		sx = (mWidth >> 1) + sx;
		sy = (mHeight >> 1) - (sy + sh);

		// shrink width/height to screen intersection
		if (sx < 0) {
			sw += sx;
			sx = 0;
		}

		if (sy < 0) {
			sh += sy;
			sy = 0;
		}

		if (sw + sx > mWidth)
			sw = mWidth - sx;

		if (sh + tile.sy > mHeight)
			sh = mHeight - sy;

		if (sw <= 0 || sh <= 0) {
			tile.isVisible = false;
			return false;

		}

		tile.isVisible = true;
		tile.sx = sx;
		tile.sy = sy;
		tile.sw = sw;
		tile.sh = sh;

		return true;
	}

	private void drawProxyLines(GLMapTile tile) {
		if (tile.parent != null && tile.parent.isDrawn) {
			tile.parent.sx = tile.sx;
			tile.parent.sy = tile.sy;
			tile.parent.sw = tile.sw;
			tile.parent.sh = tile.sh;
			drawLines(tile.parent, -1);
		} else {
			// scissor coordinates already set for polygons
			for (int i = 0; i < 4; i++) {
				GLMapTile c = tile.child[i];
				if (c != null && c.isDrawn && c.isVisible)
					drawLines(c, 1);

			}
		}
	}

	private void drawProxyPolygons(GLMapTile tile) {
		if (tile.parent != null && tile.parent.isDrawn) {
			tile.parent.sx = tile.sx;
			tile.parent.sy = tile.sy;
			tile.parent.sw = tile.sw;
			tile.parent.sh = tile.sh;
			drawPolygons(tile.parent, -1);
		} else {
			for (int i = 0; i < 4; i++) {
				GLMapTile c = tile.child[i];
				if (c != null && c.isDrawn && setTileScissor(c, 2))
					drawPolygons(c, 1);

			}
		}
	}

	private boolean uploadTileData(GLMapTile tile) {
		if (tile.lineVBO == null) {
			// Upload line data to vertex buffer object
			synchronized (mVBOs) {
				if (mVBOs.size() < 2)
					return false;

				tile.lineVBO = mVBOs.remove(mVBOs.size() - 1);
				tile.polygonVBO = mVBOs.remove(mVBOs.size() - 1);
			}
		}
		if (useHalfFloat)
			byteBuffer = tile.lineLayers.compileLayerData(byteBuffer);
		else
			floatBuffer = tile.lineLayers.compileLayerData(floatBuffer);

		if (tile.lineLayers.size > 0) {
			mBufferMemoryUsage -= tile.lineVBO.size;

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, tile.lineVBO.id);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 0, null, GLES20.GL_STATIC_DRAW);

			if (useHalfFloat) {
				tile.lineVBO.size = tile.lineLayers.size * SHORT_BYTES;
				GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, tile.lineVBO.size, byteBuffer, GLES20.GL_STATIC_DRAW);
			} else {
				tile.lineVBO.size = tile.lineLayers.size * FLOAT_BYTES;
				GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, tile.lineVBO.size, floatBuffer, GLES20.GL_STATIC_DRAW);
			}

			mBufferMemoryUsage += tile.lineVBO.size;

		} else {
			tile.lineLayers = null;
		}

		if (useHalfFloat)
			byteBuffer = tile.polygonLayers.compileLayerData(byteBuffer);
		else
			floatBuffer = tile.polygonLayers.compileLayerData(floatBuffer);

		// Upload polygon data to vertex buffer object
		if (tile.polygonLayers.size > 0) {
			mBufferMemoryUsage -= tile.polygonVBO.size;

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, tile.polygonVBO.id);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 0, null, GLES20.GL_STATIC_DRAW);

			if (useHalfFloat) {
				tile.polygonVBO.size = tile.polygonLayers.size * SHORT_BYTES;
				GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, tile.polygonVBO.size, byteBuffer, GLES20.GL_STATIC_DRAW);
			} else {
				tile.polygonVBO.size = tile.polygonLayers.size * FLOAT_BYTES;
				GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, tile.polygonVBO.size, floatBuffer, GLES20.GL_STATIC_DRAW);
			}
			mBufferMemoryUsage += tile.polygonVBO.size;

		} else {
			tile.polygonLayers = null;
		}

		tile.newData = false;
		tile.isDrawn = true;
		tile.isLoading = false;

		return true;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0, poly_time = 0, clear_time = 0;

		if (mMapPosition == null)
			return;

		if (timing)
			start = SystemClock.uptimeMillis();

		GLES20.glStencilMask(0xFF);
		GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);

		synchronized (this) {
			mDrawX = mCurX;
			mDrawY = mCurY;
			mDrawZ = mCurZ;
			mDrawScale = mCurScale;

			if (mUpdateTiles) {
				TilesData tmp = curTiles;
				curTiles = nextTiles;
				nextTiles = tmp;
				mUpdateTiles = false;
			}
		}

		int tileCnt = curTiles.cnt;
		GLMapTile[] tiles = curTiles.tiles;

		if (mBufferMemoryUsage > LIMIT_BUFFERS) {
			Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / (1024 * 1024) + "MB");
			synchronized (mVBOs) {
				for (VertexBufferObject vbo : mVBOs) {
					if (vbo.size == 0)
						continue;
					mBufferMemoryUsage -= vbo.size;
					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo.id);
					GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 0, null, GLES20.GL_STATIC_DRAW);
					vbo.size = 0;

				}
			}
			Log.d(TAG, " > " + mBufferMemoryUsage / (1024 * 1024) + "MB");
		}

		// check visible tiles, set tile clip scissors, upload new vertex data
		for (int i = 0; i < tileCnt; i++) {
			GLMapTile tile = tiles[i];

			if (!setTileScissor(tile, 1))
				continue;

			if (tile.newData) {
				uploadTileData(tile);

				if (timing)
					Log.d(TAG, "buffer upload took: " + (SystemClock.uptimeMillis() - start));

				continue;
			}

			if (!tile.isDrawn) {
				if (tile.parent != null) {
					if (tile.parent.newData)
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

		if (timing)
			clear_time = (SystemClock.uptimeMillis() - start);

		GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
		GLES20.glDisable(GLES20.GL_BLEND);

		// Draw Polygons
		GLES20.glEnable(GLES20.GL_STENCIL_TEST);

		GLES20.glUseProgram(gPolygonProgram);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible) {
				GLMapTile tile = tiles[i];

				if (tile.isDrawn)
					drawPolygons(tile, 0);
				else
					drawProxyPolygons(tile);
			}
		}

		GLES20.glDisable(GLES20.GL_STENCIL_TEST);

		if (timing) {
			GLES20.glFinish();
			poly_time = (SystemClock.uptimeMillis() - start);
		}

		// Draw lines
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glUseProgram(gLineProgram);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible) {
				GLMapTile tile = tiles[i];

				if (tile.isDrawn)
					drawLines(tile, 0);
				else
					drawProxyLines(tile);
			}
		}

		// GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		if (timing) {
			GLES20.glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start) + " " + clear_time + " " + poly_time);
		}

	}

	private int[] mVboIds;

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		mVBOs.clear();
		mTiles.clear();
		mTileList.clear();

		curTiles = newTiles = nextTiles = null;
		mBufferMemoryUsage = 0;

		if (width <= 0 || height <= 0)
			return;

		STENCIL_BITS = GlConfigChooser.stencilSize;
		mPolyColors = new int[STENCIL_BITS];

		mWidth = width;
		mHeight = height;
		mAspect = (float) height / width;

		GLES20.glViewport(0, 0, width, height);

		int tiles = (mWidth / Tile.TILE_SIZE + 4) * (mHeight / Tile.TILE_SIZE + 4);
		curTiles = new TilesData(tiles);
		newTiles = new TilesData(tiles);
		nextTiles = new TilesData(tiles);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + tiles) * 2;
		mVboIds = new int[numVBO];
		GLES20.glGenBuffers(numVBO, mVboIds, 0);

		for (int i = 0; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		mDebugSettings = mMapView.getDebugSettings();
		mJobParameter = mMapView.getJobParameters();

		mInitial = true;
		mMapView.redrawTiles();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set up the program for rendering lines

		gLineProgram = GlUtils.createProgram(Shaders.gLineVertexShader, Shaders.gLineFragmentShader);
		if (gLineProgram == 0) {
			Log.e(TAG, "trying simple line program.");
			gLineProgram = GlUtils.createProgram(Shaders.gLineVertexShader, Shaders.gLineFragmentShaderSimple);
			if (gLineProgram == 0) {
				Log.e(TAG, "Could not create line program.");
				return;
			}
		}

		String ext = GLES20.glGetString(GLES20.GL_EXTENSIONS);

		if (ext.indexOf("GL_OES_vertex_half_float") >= 0)
			useHalfFloat = true;

		Log.d(TAG, "Extensions: " + ext);

		gLineMatrixHandle = GLES20.glGetUniformLocation(gLineProgram, "u_center");
		gLineWidthHandle = GLES20.glGetUniformLocation(gLineProgram, "u_width");
		gLineModeHandle = GLES20.glGetUniformLocation(gLineProgram, "u_mode");
		gLineColorHandle = GLES20.glGetUniformLocation(gLineProgram, "u_color");
		gLineVertexPositionHandle = GLES20.glGetAttribLocation(gLineProgram, "a_position");
		gLineTexturePositionHandle = GLES20.glGetAttribLocation(gLineProgram, "a_st");

		// Set up the program for rendering polygons
		gPolygonProgram = GlUtils.createProgram(Shaders.gPolygonVertexShader, Shaders.gPolygonFragmentShader);
		if (gPolygonProgram == 0) {
			Log.e(TAG, "Could not create polygon program.");
			return;
		}
		gPolygonMatrixHandle = GLES20.glGetUniformLocation(gPolygonProgram, "u_center");
		gPolygonVertexPositionHandle = GLES20.glGetAttribLocation(gPolygonProgram, "a_position");
		gPolygonColorHandle = GLES20.glGetUniformLocation(gPolygonProgram, "u_color");

		GLES20.glUseProgram(gPolygonProgram);
		GLES20.glEnableVertexAttribArray(gPolygonVertexPositionHandle);

		GLES20.glUseProgram(gLineProgram);
		GLES20.glEnableVertexAttribArray(gLineVertexPositionHandle);
		GLES20.glEnableVertexAttribArray(gLineTexturePositionHandle);

		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glDisable(GLES20.GL_DITHER);
		GLES20.glClearColor(0.96f, 0.96f, 0.95f, 1.0f);
		GLES20.glClearStencil(0);
	}

	@Override
	public boolean processedTile() {
		return true;
	}
}
