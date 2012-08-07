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
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_DITHER;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_EQUAL;
import static android.opengl.GLES20.GL_EXTENSIONS;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_INVERT;
import static android.opengl.GLES20.GL_NEVER;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SCISSOR_TEST;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_STENCIL_BUFFER_BIT;
import static android.opengl.GLES20.GL_STENCIL_TEST;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_ZERO;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glClearStencil;
import static android.opengl.GLES20.glColorMask;
import static android.opengl.GLES20.glDepthMask;
import static android.opengl.GLES20.glDisable;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFinish;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetString;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glScissor;
import static android.opengl.GLES20.glStencilFunc;
import static android.opengl.GLES20.glStencilMask;
import static android.opengl.GLES20.glStencilOp;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform2f;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import java.nio.FloatBuffer;
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
public class MapRenderer implements org.mapsforge.android.IMapRenderer {
	private static final String TAG = "MapRenderer";
	private static final int MB = 1024 * 1024;

	private boolean mTriangulate = false;

	private static int CACHE_TILES_MAX = 400;
	private static int CACHE_TILES = CACHE_TILES_MAX;
	private static int LIMIT_BUFFERS = 20 * MB;

	private static final int OES_HALF_FLOAT = 0x8D61;
	private static final int FLOAT_BYTES = 4;
	private static final int SHORT_BYTES = 2;
	private static final int POLYGON_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_TEX_OFFSET = 8;

	private static int STENCIL_BITS = 8;

	private final MapView mMapView;
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

	private FloatBuffer floatBuffer[];
	private ShortBuffer shortBuffer[];

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
	private int gLineModeHandle;
	private int gLineWidthHandle;

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
		int h = (Tile.TILE_SIZE >> 1);
		byte zoom = mMapPosition.zoomLevel;
		long x = mTileX * (Tile.TILE_SIZE) + h;
		long y = mTileY * (Tile.TILE_SIZE) + h;
		int diff;
		long dx, dy;

		// TODO this could be optimized to consider move/zoom direction
		for (int i = 0, n = mTileList.size(); i < n; i++) {
			GLMapTile t = mTileList.get(i);
			diff = (t.zoomLevel - zoom);

			if (diff != 0) {
				if (diff > 0) {
					// tile zoom level is child of current
					dx = ((t.pixelX + h) >> diff) - x;
					dy = ((t.pixelY + h) >> diff) - y;
				} else {
					// tile zoom level is parent of current
					dx = ((t.pixelX + h) << -diff) - x;
					dy = ((t.pixelY + h) << -diff) - y;
				}

				if (diff == -1) {
					dy *= mAspect;
					t.distance = 2 + ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy));
				} else {
					// load parent before current layer (kind of progressive transmission :)
					t.distance = ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy));
					// prefer lower zoom level, i.e. it covers a larger area
					t.distance *= (1 + (diff > 0 ? diff * 4 : -diff * 2));
				}
			} else {
				dx = (t.pixelX + h) - x;
				dy = (t.pixelY + h) - y;
				dy *= mAspect;
				t.distance = (1 + ((dx > 0 ? dx : -dx) + (dy > 0 ? dy : -dy)));// * 2;
			}
			// Log.d(TAG, t + " " + t.distance);
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
					if (t.parent != null && t.parent.isActive
							&& !(t.parent.isDrawn || t.parent.newData)) {
						mTileList.add(t);
						Log.d(TAG, "EEEK removing active proxy child");
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
						Log.d(TAG, "EEEK removing active proxy parent");
						mTileList.add(t);
						continue;
					}
				} else if (t.zoomLevel == z - 2) {
					GLMapTile c = null, c2 = null;
					for (int i = 0; i < 4; i++) {
						c = t.child[i];
						if (c != null) {
							for (int k = 0; k < 4; k++) {
								c2 = c.child[k];
								if (c2 != null && c2.isActive
										&& !(c2.isDrawn || c2.newData))
									break;

								c2 = null;
							}
							if (c2 != null)
								break;
						}
						c = null;
					}

					if (c != null) {
						Log.d(TAG, "EEEK removing active second level proxy parent");
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
		long limit = (long) Math.pow(2, zoomLevel) - 1;

		if (tileTop < 0)
			tileTop = 0;
		if (tileLeft < 0)
			tileLeft = 0;
		if (tileBottom >= limit)
			tileBottom = limit;
		if (tileRight >= limit)
			tileRight = limit;

		for (long tileY = tileTop; tileY <= tileBottom; tileY++) {
			for (long tileX = tileLeft; tileX <= tileRight; tileX++) {
				// FIXME
				if (tiles == max)
					break;

				GLMapTile tile = mTiles.get(mTileCacheKey.set(tileX, tileY, zoomLevel));

				if (tile == null) {
					tile = new GLMapTile(tileX, tileY, zoomLevel);
					TileCacheKey key = new TileCacheKey(mTileCacheKey);

					// FIXME use sparse matrix or sth.
					if (mTiles.put(key, tile) != null)
						Log.d(TAG, "eeek collision");

					mTileList.add(tile);

					mTileCacheKey.set((tileX >> 1), (tileY >> 1), (byte) (zoomLevel - 1));
					tile.parent = mTiles.get(mTileCacheKey);
					int idx = (int) ((tileX & 0x01) + 2 * (tileY & 0x01));

					// set this tile to be child of its parent
					if (tile.parent != null) {
						tile.parent.child[idx] = tile;
					}
					else if (zoomLevel > 0) {
						tile.parent = new GLMapTile(tileX >> 1, tileY >> 1,
								(byte) (zoomLevel - 1));
						key = new TileCacheKey(mTileCacheKey);
						if (mTiles.put(key, tile.parent) != null)
							Log.d(TAG, "eeek collision");
						mTileList.add(tile.parent);
						setChildren(tile.parent);
					}

					setChildren(tile);

				}

				newTiles.tiles[tiles++] = tile;

				if (!tile.isDrawn && !tile.newData && !tile.isLoading) {
					MapGeneratorJob job = new MapGeneratorJob(tile, mJobParameter,
							mDebugSettings);
					mJobList.add(job);
				}

				// prefetch parent
				// if (tile.parent != null && !tile.parent.isDrawn && !tile.parent.newData
				// && !tile.parent.isLoading) {
				// MapGeneratorJob job = new MapGeneratorJob(tile.parent, mJobParameter,
				// mDebugSettings);
				// if (!mJobList.contains(job))
				// mJobList.add(job);
				// }
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

		if (mJobList.size() > 0)
			mMapView.addJobs(mJobList);

		return true;
	}

	private void setChildren(GLMapTile tile) {

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

		double x = MercatorProjection.longitudeToPixelX(
				mMapPosition.geoPoint.getLongitude(), zoomLevel);
		double y = MercatorProjection.latitudeToPixelY(
				mMapPosition.geoPoint.getLatitude(), zoomLevel);

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
		if (!timing && mapGeneratorJob.tile.isVisible)
			mMapView.requestRender();
		return true;
	}

	private PolygonLayer[] mFillPolys;

	private void fillPolygons(int count) {
		boolean blend = false;

		// draw to framebuffer
		glColorMask(true, true, true, true);

		// do not modify stencil buffer
		glStencilMask(0);

		for (int c = 0; c < count; c++) {
			PolygonLayer l = mFillPolys[c];

			float alpha = 1.0f;

			if (l.fadeLevel >= mDrawZ) {

				alpha = (mDrawScale > 1.3f ? mDrawScale : 1.3f) - alpha;
				if (alpha > 1.0f)
					alpha = 1.0f;

				if (!blend) {
					glEnable(GL_BLEND);
					blend = true;
				}
			} else if (blend) {
				glDisable(GL_BLEND);
				blend = false;
			}

			glUniform4f(gPolygonColorHandle,
					l.colors[0], l.colors[1], l.colors[2], alpha);

			// set stencil buffer mask used to draw this layer
			glStencilFunc(GL_EQUAL, 0xff, 1 << c);

			// draw tile fill coordinates
			glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		}

		if (blend)
			glDisable(GL_BLEND);
	}

	private boolean drawPolygons(GLMapTile tile, int diff) {
		int cnt = 0;

		if (tile.polygonLayers == null || tile.polygonLayers.array == null)
			return true;

		glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		if (mLastBoundVBO != tile.polygonVBO.id) {
			mLastBoundVBO = tile.polygonVBO.id;
			glBindBuffer(GL_ARRAY_BUFFER, tile.polygonVBO.id);

			if (useHalfFloat) {
				glVertexAttribPointer(gPolygonVertexPositionHandle, 2,
						OES_HALF_FLOAT, false, 0,
						POLYGON_VERTICES_DATA_POS_OFFSET);
			} else {
				glVertexAttribPointer(gPolygonVertexPositionHandle, 2,
						GL_FLOAT, false, 0,
						POLYGON_VERTICES_DATA_POS_OFFSET);
			}

			// glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		setMatrix(tile, diff);
		glUniformMatrix4fv(gPolygonMatrixHandle, 1, false, mMVPMatrix, 0);

		boolean firstPass = true;

		for (int i = 0, n = tile.polygonLayers.array.length; i < n; i++) {
			PolygonLayer l = tile.polygonLayers.array[i];

			// fade out polygon layers (set in RederTheme)
			if (l.fadeLevel > 0 && l.fadeLevel > mDrawZ)
				continue;

			if (cnt == 0) {
				// disable drawing to framebuffer
				glColorMask(false, false, false, false);

				// never pass the test, i.e. always apply first stencil op (sfail)
				glStencilFunc(GL_NEVER, 0, 0xff);

				if (firstPass)
					firstPass = false;
				else {
					// eeek, nexus! - cant do old-school polygons
					// glFinish();

					// clear stencilbuffer
					glStencilMask(0xFF);
					// glClear(GL_STENCIL_BUFFER_BIT);

					// clear stencilbuffer (tile region)
					glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);
					glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
				}

				// stencil op for stencil method polygon drawing
				glStencilOp(GL_INVERT, GL_INVERT, GL_INVERT);
			}

			mFillPolys[cnt] = l;

			// set stencil mask to draw to
			glStencilMask(1 << cnt++);

			glDrawArrays(GL_TRIANGLE_FAN, l.offset, l.verticesCnt);

			// draw up to 8 layers into stencil buffer
			if (cnt == STENCIL_BITS) {
				fillPolygons(cnt);
				cnt = 0;
			}
		}

		if (cnt > 0) {
			fillPolygons(cnt);
			// eeek, nexus! - cant do old-school polygons
			// glFinish();
		}
		return true;
	}

	private int mLastBoundVBO;

	private boolean drawTriangles(GLMapTile tile, int diff) {

		if (tile.meshLayers == null || tile.meshLayers.array == null)
			return true;

		glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		if (mLastBoundVBO != tile.polygonVBO.id) {
			mLastBoundVBO = tile.polygonVBO.id;
			glBindBuffer(GL_ARRAY_BUFFER, tile.polygonVBO.id);

			if (useHalfFloat) {
				glVertexAttribPointer(gPolygonVertexPositionHandle, 2,
						OES_HALF_FLOAT, false, 0,
						POLYGON_VERTICES_DATA_POS_OFFSET);
			} else {
				glVertexAttribPointer(gPolygonVertexPositionHandle, 2,
						GL_FLOAT, false, 0,
						POLYGON_VERTICES_DATA_POS_OFFSET);
			}

			// glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		setMatrix(tile, diff);
		glUniformMatrix4fv(gPolygonMatrixHandle, 1, false, mMVPMatrix, 0);

		MeshLayer[] layers = tile.meshLayers.array;

		for (int i = 0, n = layers.length; i < n; i++) {
			MeshLayer l = layers[i];
			glUniform4fv(gPolygonColorHandle, 1, l.colors, 0);

			// glUniform4f(gPolygonColorHandle, 1, 0, 0, 1);

			// System.out.println("draw: " + l.offset + " " + l.verticesCnt);
			glDrawArrays(GL_TRIANGLES, l.offset, l.verticesCnt);
		}

		return true;
	}

	private boolean drawLines(GLMapTile tile, int diff) {
		float z = 1;

		if (tile.lineLayers == null || tile.lineLayers.array == null)
			return false;

		glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		if (mLastBoundVBO != tile.lineVBO.id) {
			mLastBoundVBO = tile.lineVBO.id;
			glBindBuffer(GL_ARRAY_BUFFER, tile.lineVBO.id);

			if (useHalfFloat) {
				glVertexAttribPointer(gLineVertexPositionHandle, 2, OES_HALF_FLOAT,
						false, 8, LINE_VERTICES_DATA_POS_OFFSET);

				glVertexAttribPointer(gLineTexturePositionHandle, 2, OES_HALF_FLOAT,
						false, 8, LINE_VERTICES_DATA_TEX_OFFSET >> 1);
			} else {
				glVertexAttribPointer(gLineVertexPositionHandle, 2, GL_FLOAT,
						false, 16, LINE_VERTICES_DATA_POS_OFFSET);

				glVertexAttribPointer(gLineTexturePositionHandle, 2, GL_FLOAT,
						false, 16, LINE_VERTICES_DATA_TEX_OFFSET);
			}
			// glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		if (diff != 0)
			z = (diff > 0) ? (1 << diff) : 1.0f / (1 << -diff);

		setMatrix(tile, diff);
		glUniformMatrix4fv(gLineMatrixHandle, 1, false, mMVPMatrix, 0);

		LineLayer[] layers = tile.lineLayers.array;

		boolean drawOutlines = false;
		boolean drawFixed = false;

		// stroke scale factor
		float wdiv = 1.0f / FloatMath.sqrt(mDrawScale / z);
		// linear scale for fixed lines
		float fdiv = 0.9f / (mDrawScale / z);

		// int cnt = 0;
		for (int i = 0, n = layers.length; i < n; i++) {
			LineLayer l = layers[i];

			// set line width and mode
			if ((i == 0) || (l.isOutline != drawOutlines) || (l.isFixed != drawFixed)) {
				drawOutlines = l.isOutline;
				drawFixed = l.isFixed;

				if (drawOutlines) {
					glUniform2f(gLineModeHandle, 0, wdiv);
				} else if (!drawFixed) {
					glUniform2f(gLineModeHandle, 0, wdiv * 0.98f);
				}
			}

			if (drawFixed) {
				if (l.width < 1.0)
					glUniform2f(gLineModeHandle, 0.4f, fdiv);
				else
					glUniform2f(gLineModeHandle, 0, fdiv);
			}
			glUniform4fv(gLineColorHandle, 1, l.colors, 0);

			if (drawOutlines) {
				for (int j = 0, m = l.outlines.size(); j < m; j++) {
					LineLayer o = l.outlines.get(j);

					if (mSimpleLines)
						glUniform1f(gLineWidthHandle, o.width);

					glDrawArrays(GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			}
			else {
				if (mSimpleLines)
					glUniform1f(gLineWidthHandle, l.width);

				glDrawArrays(GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}

		return true;
	}

	private void setMatrix(GLMapTile tile, int diff) {
		float x, y, scale;
		float z = 1;

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

		if (sh + (sy - sh) > mHeight)
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
			int drawn = 0;
			// scissor coordinates already set for polygons
			for (int i = 0; i < 4; i++) {
				GLMapTile c = tile.child[i];
				if (c != null && c.isDrawn && c.isVisible) {
					drawLines(c, 1);
					drawn++;
				}

			}
			if (drawn < 4 && tile.parent != null) {
				GLMapTile p = tile.parent.parent;
				if (p != null && p.isDrawn) {
					p.sx = tile.sx;
					p.sy = tile.sy;
					p.sw = tile.sw;
					p.sh = tile.sh;
					drawLines(p, -2);
				}
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
			int drawn = 0;

			for (int i = 0; i < 4; i++) {
				GLMapTile c = tile.child[i];

				if (c != null && c.isDrawn && setTileScissor(c, 2)) {
					drawPolygons(c, 1);
					drawn++;
				}
			}

			if (drawn < 4 && tile.parent != null) {
				GLMapTile p = tile.parent.parent;
				if (p != null && p.isDrawn) {
					p.sx = tile.sx;
					p.sy = tile.sy;
					p.sw = tile.sw;
					p.sh = tile.sh;
					drawPolygons(p, -2);
				}
			}
		}
	}

	private void drawProxyTriangles(GLMapTile tile) {
		if (tile.parent != null && tile.parent.isDrawn) {
			tile.parent.sx = tile.sx;
			tile.parent.sy = tile.sy;
			tile.parent.sw = tile.sw;
			tile.parent.sh = tile.sh;
			drawTriangles(tile.parent, -1);
		} else {
			int drawn = 0;

			for (int i = 0; i < 4; i++) {
				GLMapTile c = tile.child[i];

				if (c != null && c.isDrawn && setTileScissor(c, 2)) {
					drawTriangles(c, 1);
					drawn++;
				}
			}

			if (drawn < 4 && tile.parent != null) {
				GLMapTile p = tile.parent.parent;
				if (p != null && p.isDrawn) {
					p.sx = tile.sx;
					p.sy = tile.sy;
					p.sw = tile.sw;
					p.sh = tile.sh;
					drawTriangles(p, -2);
				}
			}
		}
	}

	private int uploadCnt = 0;

	private boolean uploadTileData(GLMapTile tile) {

		// double start = SystemClock.uptimeMillis();

		// use multiple buffers to avoid overwriting buffer while current data is uploaded
		// (or rather the blocking which is required to avoid this)
		if (uploadCnt >= 10) {
			// mMapView.requestRender();
			// return false;
			uploadCnt = 0;
			glFinish();
		}

		// Upload line data to vertex buffer object
		if (tile.lineVBO == null) {
			synchronized (mVBOs) {
				if (mVBOs.size() < 2) {
					Log.d(TAG, "uploadTileData, no VBOs left");
					return false;
				}
				tile.lineVBO = mVBOs.remove(mVBOs.size() - 1);
				tile.polygonVBO = mVBOs.remove(mVBOs.size() - 1);
			}
		}
		if (useHalfFloat)
			shortBuffer[uploadCnt * 2] = tile.lineLayers
					.compileLayerData(shortBuffer[uploadCnt * 2]);
		else
			floatBuffer[uploadCnt * 2] = tile.lineLayers
					.compileLayerData(floatBuffer[uploadCnt * 2]);

		if (tile.lineLayers.size > 0) {
			mBufferMemoryUsage -= tile.lineVBO.size;

			glBindBuffer(GL_ARRAY_BUFFER, tile.lineVBO.id);
			// glBufferData(GL_ARRAY_BUFFER, 0, null, GL_DYNAMIC_DRAW);

			if (useHalfFloat) {
				tile.lineVBO.size = tile.lineLayers.size * SHORT_BYTES;
				glBufferData(GL_ARRAY_BUFFER, tile.lineVBO.size,
						shortBuffer[uploadCnt * 2], GL_DYNAMIC_DRAW);
			} else {
				tile.lineVBO.size = tile.lineLayers.size * FLOAT_BYTES;
				glBufferData(GL_ARRAY_BUFFER, tile.lineVBO.size,
						floatBuffer[uploadCnt * 2], GL_DYNAMIC_DRAW);
			}

			mBufferMemoryUsage += tile.lineVBO.size;

		} else {
			tile.lineLayers = null;
		}

		if (!mTriangulate) {
			if (useHalfFloat)
				shortBuffer[uploadCnt * 2 + 1] = tile.polygonLayers
						.compileLayerData(shortBuffer[uploadCnt * 2 + 1]);
			else
				floatBuffer[uploadCnt * 2 + 1] = tile.polygonLayers
						.compileLayerData(floatBuffer[uploadCnt * 2 + 1]);

			// Upload polygon data to vertex buffer object
			if (tile.polygonLayers.size > 0) {
				mBufferMemoryUsage -= tile.polygonVBO.size;

				glBindBuffer(GL_ARRAY_BUFFER, tile.polygonVBO.id);
				// glBufferData(GL_ARRAY_BUFFER, 0, null,
				// GL_DYNAMIC_DRAW);

				if (useHalfFloat) {
					tile.polygonVBO.size = tile.polygonLayers.size * SHORT_BYTES;
					glBufferData(GL_ARRAY_BUFFER, tile.polygonVBO.size,
							shortBuffer[uploadCnt * 2 + 1], GL_DYNAMIC_DRAW);
				} else {
					tile.polygonVBO.size = tile.polygonLayers.size * FLOAT_BYTES;
					glBufferData(GL_ARRAY_BUFFER, tile.polygonVBO.size,
							floatBuffer[uploadCnt * 2 + 1], GL_DYNAMIC_DRAW);
				}
				mBufferMemoryUsage += tile.polygonVBO.size;

			} else {
				tile.polygonLayers = null;
			}
		}
		else {
			if (useHalfFloat)
				shortBuffer[uploadCnt * 2 + 1] = tile.meshLayers
						.compileLayerData(shortBuffer[uploadCnt * 2 + 1]);
			else
				floatBuffer[uploadCnt * 2 + 1] = tile.meshLayers
						.compileLayerData(floatBuffer[uploadCnt * 2 + 1]);

			// Upload triangle data to vertex buffer object
			if (tile.meshLayers.size > 0) {
				mBufferMemoryUsage -= tile.polygonVBO.size;

				glBindBuffer(GL_ARRAY_BUFFER, tile.polygonVBO.id);
				// glBufferData(GL_ARRAY_BUFFER, 0, null,
				// GL_DYNAMIC_DRAW);

				if (useHalfFloat) {
					tile.polygonVBO.size = tile.meshLayers.size * SHORT_BYTES;
					glBufferData(GL_ARRAY_BUFFER, tile.polygonVBO.size,
							shortBuffer[uploadCnt * 2 + 1], GL_DYNAMIC_DRAW);
				} else {
					tile.polygonVBO.size = tile.meshLayers.size * FLOAT_BYTES;
					glBufferData(GL_ARRAY_BUFFER, tile.polygonVBO.size,
							floatBuffer[uploadCnt * 2 + 1], GL_DYNAMIC_DRAW);
				}
				mBufferMemoryUsage += tile.polygonVBO.size;

			} else {
				tile.meshLayers = null;
			}
		}
		tile.newData = false;
		tile.isDrawn = true;
		tile.isLoading = false;
		// double compile = SystemClock.uptimeMillis();
		// glFinish();
		// double now = SystemClock.uptimeMillis();
		// Log.d(TAG, tile + " - upload took: " + (now - start) + " "
		// + (mBufferMemoryUsage / 1024) + "kb");

		uploadCnt++;

		return true;
	}

	// private long startTime = SystemClock.uptimeMillis();

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0, poly_time = 0, clear_time = 0;

		if (mMapPosition == null)
			return;

		if (timing)
			start = SystemClock.uptimeMillis();

		glStencilMask(0xFF);
		glDisable(GL_SCISSOR_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

		// long endTime = SystemClock.uptimeMillis();
		// long dt = endTime - startTime;
		// if (dt < 33)
		// try {
		// Thread.sleep(33 - dt);
		// } catch (InterruptedException e) {
		// Log.d(TAG, "interrupt");
		// return;
		// }
		// startTime = SystemClock.uptimeMillis();

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
			Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / MB + "MB");
			synchronized (mVBOs) {
				for (VertexBufferObject vbo : mVBOs) {
					if (vbo.size == 0)
						continue;
					mBufferMemoryUsage -= vbo.size;
					glBindBuffer(GL_ARRAY_BUFFER, vbo.id);
					glBufferData(GL_ARRAY_BUFFER, 0, null,
							GL_DYNAMIC_DRAW);
					vbo.size = 0;

				}
				glBindBuffer(GL_ARRAY_BUFFER, 0);
			}
			Log.d(TAG, " > " + mBufferMemoryUsage / MB + "MB");

			if (CACHE_TILES > 50)
				CACHE_TILES -= 50;
		} else if (CACHE_TILES < CACHE_TILES_MAX) {
			CACHE_TILES += 50;
		}

		uploadCnt = 0;
		mLastBoundVBO = -1;

		// check visible tiles, set tile clip scissors, upload new vertex data
		for (int i = 0; i < tileCnt; i++) {
			GLMapTile tile = tiles[i];

			if (!setTileScissor(tile, 1))
				continue;

			if (tile.newData) {
				uploadTileData(tile);
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

		if (GlUtils.checkGlOutOfMemory("upload: " + mBufferMemoryUsage)
				&& LIMIT_BUFFERS > MB)
			LIMIT_BUFFERS -= MB;

		if (timing)
			clear_time = (SystemClock.uptimeMillis() - start);

		glEnable(GL_SCISSOR_TEST);

		glUseProgram(gPolygonProgram);
		glEnableVertexAttribArray(gPolygonVertexPositionHandle);

		if (!mTriangulate) {
			glDisable(GL_BLEND);
			// Draw Polygons
			glEnable(GL_STENCIL_TEST);

			// glEnableVertexAttribArray(gPolygonVertexPositionHandle);

			for (int i = 0; i < tileCnt; i++) {
				if (tiles[i].isVisible) {
					GLMapTile tile = tiles[i];

					if (tile.isDrawn)
						drawPolygons(tile, 0);
					else
						drawProxyPolygons(tile);
				}
			}
			// GlUtils.checkGlError("polygons");
			glDisable(GL_STENCIL_TEST);
		} else {
			// Draw Triangles
			for (int i = 0; i < tileCnt; i++) {
				if (tiles[i].isVisible) {
					GLMapTile tile = tiles[i];

					if (tile.isDrawn)
						drawTriangles(tile, 0);
					else
						drawProxyTriangles(tile);
				}
			}
			// GlUtils.checkGlError("triangles");
		}
		// required on GalaxyII, Android 2.3.3 (cant just VAA enable once...)
		glDisableVertexAttribArray(gPolygonVertexPositionHandle);

		if (timing) {
			glFinish();
			poly_time = (SystemClock.uptimeMillis() - start);
		}

		// Draw lines
		glEnable(GL_BLEND);
		glUseProgram(gLineProgram);

		glEnableVertexAttribArray(gLineVertexPositionHandle);
		glEnableVertexAttribArray(gLineTexturePositionHandle);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible) {
				GLMapTile tile = tiles[i];

				if (tile.isDrawn)
					drawLines(tile, 0);
				else
					drawProxyLines(tile);
			}
		}

		if (timing) {
			glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start) + " "
					+ clear_time + " " + poly_time);
		}
		glDisableVertexAttribArray(gLineVertexPositionHandle);
		glDisableVertexAttribArray(gLineTexturePositionHandle);
		// GlUtils.checkGlError("lines");
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
		mFillPolys = new PolygonLayer[STENCIL_BITS];

		mWidth = width;
		mHeight = height;
		mAspect = (float) height / width;

		glViewport(0, 0, width, height);

		int tiles = (mWidth / Tile.TILE_SIZE + 4) * (mHeight / Tile.TILE_SIZE + 4);
		curTiles = new TilesData(tiles);
		newTiles = new TilesData(tiles);
		nextTiles = new TilesData(tiles);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + tiles) * 2;
		mVboIds = new int[numVBO];
		glGenBuffers(numVBO, mVboIds, 0);

		for (int i = 0; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		mDebugSettings = mMapView.getDebugSettings();
		mJobParameter = mMapView.getJobParameters();

		mInitial = true;
		mMapView.redrawTiles();
	}

	private boolean mSimpleLines = false;

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set up the program for rendering lines

		gLineProgram = GlUtils.createProgram(Shaders.gLineVertexShader,
				Shaders.gLineFragmentShader);
		if (gLineProgram == 0) {
			mSimpleLines = true;
			Log.e(TAG, "trying simple line program.");
			gLineProgram = GlUtils.createProgram(Shaders.gLineVertexShader,
					Shaders.gLineFragmentShaderSimple);
			if (gLineProgram == 0) {
				Log.e(TAG, "Could not create line program.");
				return;
			}
		}

		String ext = glGetString(GL_EXTENSIONS);

		if (ext.indexOf("GL_OES_vertex_half_float") >= 0) {
			useHalfFloat = true;
			shortBuffer = new ShortBuffer[20];
		}
		else {
			floatBuffer = new FloatBuffer[20];
		}
		Log.d(TAG, "Extensions: " + ext);

		gLineMatrixHandle = glGetUniformLocation(gLineProgram, "u_center");
		gLineModeHandle = glGetUniformLocation(gLineProgram, "u_mode");
		gLineColorHandle = glGetUniformLocation(gLineProgram, "u_color");
		gLineVertexPositionHandle = GLES20
				.glGetAttribLocation(gLineProgram, "a_position");
		gLineTexturePositionHandle = glGetAttribLocation(gLineProgram, "a_st");
		if (mSimpleLines)
			gLineWidthHandle = glGetUniformLocation(gLineProgram, "u_width");

		// Set up the program for rendering polygons
		gPolygonProgram = GlUtils.createProgram(Shaders.gPolygonVertexShader,
				Shaders.gPolygonFragmentShader);
		if (gPolygonProgram == 0) {
			Log.e(TAG, "Could not create polygon program.");
			return;
		}
		gPolygonMatrixHandle = glGetUniformLocation(gPolygonProgram, "u_center");
		gPolygonVertexPositionHandle = glGetAttribLocation(gPolygonProgram,
				"a_position");
		gPolygonColorHandle = glGetUniformLocation(gPolygonProgram, "u_color");

		// glUseProgram(gPolygonProgram);
		// glEnableVertexAttribArray(gPolygonVertexPositionHandle);
		//
		// glUseProgram(gLineProgram);
		// glEnableVertexAttribArray(gLineVertexPositionHandle);
		// glEnableVertexAttribArray(gLineTexturePositionHandle);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_DITHER);
		glClearColor(0.98f, 0.98f, 0.97f, 1.0f);
		glClearStencil(0);
	}

	@Override
	public boolean processedTile() {
		return true;
	}

	public IMapGenerator getMapGenerator() {
		return new MapGenerator();
	}

	@Override
	public IMapGenerator createMapGenerator() {
		return new MapGenerator();

	}
}
