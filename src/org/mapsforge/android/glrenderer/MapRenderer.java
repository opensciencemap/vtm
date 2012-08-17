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
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SCISSOR_TEST;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_STENCIL_BUFFER_BIT;
import static android.opengl.GLES20.GL_STENCIL_TEST;
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
import org.mapsforge.android.rendertheme.renderinstruction.Line;
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

	// private boolean mTriangulate = false;

	private static int CACHE_TILES_MAX = 250;
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

	private static int mWidth, mHeight;
	private static float mAspect;

	// draw position is updated from current position in onDrawFrame
	// keeping the position consistent while drawing
	private static double mDrawX, mDrawY, mDrawZ, mCurX, mCurY, mCurZ;
	private static float mDrawScale, mCurScale;

	// current center tile
	private static long mTileX, mTileY;

	private static int rotateBuffers = 2;
	private static FloatBuffer floatBuffer[];
	private static ShortBuffer shortBuffer[];

	static boolean useHalfFloat = false;

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
	// private float[] mMMatrix = new float[16];
	// private float[] mRMatrix = new float[16];

	// newTiles is set in updateVisibleList and synchronized swapped
	// with nextTiles on main thread.
	// nextTiles is swapped with curTiles in onDrawFrame in GL thread.
	private static TilesData newTiles, nextTiles, curTiles;

	private boolean mInitial;

	// shader handles
	private static int lineProgram;
	private static int hLineVertexPosition;
	private static int hLineTexturePosition;
	private static int hLineColor;
	private static int hLineMatrix;
	private static int hLineMode;
	private static int hLineWidth;

	private static int polygonProgram;
	private static int hPolygonVertexPosition;
	private static int hPolygonMatrix;
	private static int hPolygonColor;

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
				Log.d(TAG, "EEEK removing active tile");
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

		// scramble tile draw order: might help to make gl
		// pipelines more independent... just a guess :)
		for (int i = 1; i < tiles / 2; i += 2) {
			GLMapTile tmp = newTiles.tiles[i];
			newTiles.tiles[i] = newTiles.tiles[tiles - i];
			newTiles.tiles[tiles - i] = tmp;
		}

		// pass new tile list to glThread
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

		int removes = mTiles.size() - CACHE_TILES;

		if (removes > 0) {
			Collections.sort(mTileList, mTileDistanceSort);
			limitCache(removes);
		}

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
	public void redrawTiles(boolean clear) {
		boolean changedPos = false;
		boolean changedZoom = false;
		mMapPosition = mMapView.getMapPosition().getMapPosition();

		if (mMapPosition == null) {
			Log.d(TAG, ">>> no map position");
			return;
		}

		if (clear) {
			mInitial = true;
			synchronized (this) {
				for (GLMapTile t : mTileList) {
					if (t.lineVBO != null) {
						mVBOs.add(t.lineVBO);
					}
					if (t.polygonVBO != null)
						mVBOs.add(t.polygonVBO);
				}
				mTileList.clear();
				mTiles.clear();
				nextTiles.cnt = 0;
				mBufferMemoryUsage = 0;
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

		// pass new position to glThread
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

			if (l.area.fade >= mDrawZ) {

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

			glUniform4f(hPolygonColor,
					l.area.color[0], l.area.color[1], l.area.color[2], alpha);

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

		if (tile.polygonLayers == null)
			return true;

		glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		if (mLastBoundVBO != tile.polygonVBO.id) {
			mLastBoundVBO = tile.polygonVBO.id;
			glBindBuffer(GL_ARRAY_BUFFER, tile.polygonVBO.id);

			if (useHalfFloat) {
				glVertexAttribPointer(hPolygonVertexPosition, 2,
						OES_HALF_FLOAT, false, 0,
						POLYGON_VERTICES_DATA_POS_OFFSET);
			} else {
				glVertexAttribPointer(hPolygonVertexPosition, 2,
						GL_FLOAT, false, 0,
						POLYGON_VERTICES_DATA_POS_OFFSET);
			}
		}
		setMatrix(tile, diff);
		glUniformMatrix4fv(hPolygonMatrix, 1, false, mMVPMatrix, 0);

		boolean firstPass = true;

		for (PolygonLayer l = tile.polygonLayers; l != null; l = l.next) {
			// fade out polygon layers (set in RederTheme)
			if (l.area.fade > 0 && l.area.fade > mDrawZ)
				continue;

			if (cnt == 0) {
				// disable drawing to framebuffer
				glColorMask(false, false, false, false);

				// never pass the test, i.e. always apply first stencil op (sfail)
				glStencilFunc(GL_NEVER, 0, 0xff);

				if (firstPass)
					firstPass = false;
				else {
					GLES20.glFlush();

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
		GLES20.glFlush();
		return true;
	}

	private static int mLastBoundVBO;

	private boolean drawLines(GLMapTile tile, int diff) {
		float z = 1;

		if (tile.lineLayers == null)
			return false;

		glScissor(tile.sx, tile.sy, tile.sw, tile.sh);

		if (mLastBoundVBO != tile.lineVBO.id) {
			mLastBoundVBO = tile.lineVBO.id;
			glBindBuffer(GL_ARRAY_BUFFER, tile.lineVBO.id);

			if (useHalfFloat) {
				glVertexAttribPointer(hLineVertexPosition, 2, OES_HALF_FLOAT,
						false, 8, LINE_VERTICES_DATA_POS_OFFSET);

				glVertexAttribPointer(hLineTexturePosition, 2, OES_HALF_FLOAT,
						false, 8, LINE_VERTICES_DATA_TEX_OFFSET >> 1);
			} else {
				glVertexAttribPointer(hLineVertexPosition, 2, GL_FLOAT,
						false, 16, LINE_VERTICES_DATA_POS_OFFSET);

				glVertexAttribPointer(hLineTexturePosition, 2, GL_FLOAT,
						false, 16, LINE_VERTICES_DATA_TEX_OFFSET);
			}
		}
		if (diff != 0)
			z = (diff > 0) ? (1 << diff) : 1.0f / (1 << -diff);

		setMatrix(tile, diff);
		glUniformMatrix4fv(hLineMatrix, 1, false, mMVPMatrix, 0);

		boolean drawOutlines = false;
		boolean drawFixed = false;

		// stroke scale factor
		float wdiv = 1.0f / FloatMath.sqrt(mDrawScale / z);
		// linear scale for fixed lines
		float fdiv = 0.9f / (mDrawScale / z);

		boolean first = true;

		for (LineLayer l = tile.lineLayers; l != null; l = l.next) {
			Line line = l.line;
			if (line.fade > 0 && line.fade > mDrawZ)
				continue;

			// set line width and mode
			if (first || (l.isOutline != drawOutlines)
					|| (line.fixed != drawFixed)) {
				first = false;
				drawOutlines = l.isOutline;
				drawFixed = line.fixed;

				if (drawOutlines) {
					glUniform2f(hLineMode, 0, wdiv);
				} else if (!drawFixed) {
					glUniform2f(hLineMode, 0, wdiv * 0.98f);
				}
			}

			if (drawFixed) {
				if (l.width < 1.0)
					glUniform2f(hLineMode, 0.4f, fdiv);
				else
					glUniform2f(hLineMode, 0, fdiv);
			}

			if (line.fade >= mDrawZ) {
				float alpha = 1.0f;

				alpha = (mDrawScale > 1.3f ? mDrawScale : 1.3f) - alpha;
				if (alpha > 1.0f)
					alpha = 1.0f;
				glUniform4f(hLineColor,
						line.color[0], line.color[1], line.color[2], alpha);
			} else {
				glUniform4fv(hLineColor, 1, line.color, 0);
			}

			if (drawOutlines) {
				for (LineLayer o = l.outlines; o != null; o = o.outlines) {

					if (mSimpleLines)
						glUniform1f(hLineWidth, o.width);

					glDrawArrays(GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			}
			else {
				if (mSimpleLines)
					glUniform1f(hLineWidth, l.width);

				glDrawArrays(GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}
		GLES20.glFlush();
		return true;
	}

	private static void setMatrix(GLMapTile tile, int diff) {
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

		int sw = (int) ((dx + size) * scale) - sx;
		int sh = (int) ((dy + size) * scale) - sy;

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

	private int uploadCnt = 0;

	private boolean uploadTileData(GLMapTile tile) {
		ShortBuffer sbuf = null;
		FloatBuffer fbuf = null;

		// double start = SystemClock.uptimeMillis();

		// use multiple buffers to avoid overwriting buffer while current
		// data is uploaded (or rather the blocking which is required to avoid this)
		if (uploadCnt >= rotateBuffers) {
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
		int size = 0;

		if (useHalfFloat) {
			sbuf = LineLayers.compileLayerData(tile.lineLayers,
					shortBuffer[uploadCnt * 2]);
			size = sbuf.remaining();
			shortBuffer[uploadCnt * 2] = sbuf;
		} else {
			fbuf = LineLayers.compileLayerData(tile.lineLayers,
					floatBuffer[uploadCnt * 2]);
			size = fbuf.remaining();
			floatBuffer[uploadCnt * 2] = fbuf;
		}

		if (size > 0) {
			mBufferMemoryUsage -= tile.lineVBO.size;

			glBindBuffer(GL_ARRAY_BUFFER, tile.lineVBO.id);
			// glBufferData(GL_ARRAY_BUFFER, 0, null, GL_DYNAMIC_DRAW);

			if (useHalfFloat) {
				tile.lineVBO.size = size * SHORT_BYTES;
				glBufferData(GL_ARRAY_BUFFER, tile.lineVBO.size,
						sbuf, GL_DYNAMIC_DRAW);
			} else {
				tile.lineVBO.size = size * FLOAT_BYTES;
				glBufferData(GL_ARRAY_BUFFER, tile.lineVBO.size,
						fbuf, GL_DYNAMIC_DRAW);
			}

			mBufferMemoryUsage += tile.lineVBO.size;

		} else {
			tile.lineLayers = null;
		}

		if (useHalfFloat) {
			sbuf = PolygonLayers.compileLayerData(tile.polygonLayers,
					shortBuffer[uploadCnt * 2 + 1]);
			size = sbuf.remaining();
			shortBuffer[uploadCnt * 2 + 1] = sbuf;
		} else {
			fbuf = PolygonLayers.compileLayerData(tile.polygonLayers,
					floatBuffer[uploadCnt * 2 + 1]);
			size = fbuf.remaining();
			floatBuffer[uploadCnt * 2 + 1] = fbuf;
		}
		// Upload polygon data to vertex buffer object
		if (size > 0) {
			mBufferMemoryUsage -= tile.polygonVBO.size;

			glBindBuffer(GL_ARRAY_BUFFER, tile.polygonVBO.id);
			// glBufferData(GL_ARRAY_BUFFER, 0, null,
			// GL_DYNAMIC_DRAW);

			if (useHalfFloat) {
				tile.polygonVBO.size = size * SHORT_BYTES;
				glBufferData(GL_ARRAY_BUFFER, tile.polygonVBO.size,
						sbuf, GL_DYNAMIC_DRAW);
			} else {
				tile.polygonVBO.size = size * FLOAT_BYTES;
				glBufferData(GL_ARRAY_BUFFER, tile.polygonVBO.size,
						fbuf, GL_DYNAMIC_DRAW);
			}
			mBufferMemoryUsage += tile.polygonVBO.size;

		} else {
			tile.polygonLayers = null;
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

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = 0, poly_time = 0, clear_time = 0;

		if (mMapPosition == null)
			return;

		if (timing)
			start = SystemClock.uptimeMillis();

		glStencilMask(0xFF);
		glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

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

		int updateTextures = 0;

		// check visible tiles, set tile clip scissors, upload new vertex data

		for (int i = 0; i < tileCnt; i++) {
			GLMapTile tile = tiles[i];

			if (!setTileScissor(tile, 1))
				continue;

			if (tile.texture == null && tile.labels != null &&
					mTextRenderer.drawToTexture(tile))
				updateTextures++;

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

		if (updateTextures > 0)
			mTextRenderer.compileTextures();

		// if (GlUtils.checkGlOutOfMemory("upload: " + mBufferMemoryUsage)
		// && LIMIT_BUFFERS > MB)
		// LIMIT_BUFFERS -= MB;

		if (timing)
			clear_time = (SystemClock.uptimeMillis() - start);

		glEnable(GL_SCISSOR_TEST);

		glUseProgram(polygonProgram);
		glEnableVertexAttribArray(hPolygonVertexPosition);

		glDisable(GL_BLEND);
		// Draw Polygons
		glEnable(GL_STENCIL_TEST);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible) {
				GLMapTile tile = tiles[i];

				if (tile.isDrawn)
					drawPolygons(tile, 0);
				else
					drawProxyPolygons(tile);
			}
		}

		glDisable(GL_STENCIL_TEST);

		// required on GalaxyII, Android 2.3.3 (cant just VAA enable once...)
		glDisableVertexAttribArray(hPolygonVertexPosition);

		if (timing) {
			glFinish();
			poly_time = (SystemClock.uptimeMillis() - start);
		}

		// Draw lines
		glEnable(GL_BLEND);
		glUseProgram(lineProgram);

		glEnableVertexAttribArray(hLineVertexPosition);
		glEnableVertexAttribArray(hLineTexturePosition);

		for (int i = 0; i < tileCnt; i++) {
			if (tiles[i].isVisible) {
				GLMapTile tile = tiles[i];

				if (tile.isDrawn)
					drawLines(tile, 0);
				else
					drawProxyLines(tile);
			}
		}

		glDisableVertexAttribArray(hLineVertexPosition);
		glDisableVertexAttribArray(hLineTexturePosition);

		glDisable(GL_SCISSOR_TEST);

		glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
		mTextRenderer.beginDraw();
		for (int i = 0; i < tileCnt; i++) {
			if (!tiles[i].isVisible || tiles[i].texture == null)
				continue;

			setMatrix(tiles[i], 0);

			mTextRenderer.drawTile(tiles[i], mMVPMatrix);
		}
		mTextRenderer.endDraw();

		if (timing) {
			glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start) + " "
					+ clear_time + " " + poly_time);
		}
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	private static TextRenderer mTextRenderer;

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GlUtils.checkGlError("onSurfaceChanged");

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
		GlUtils.checkGlError("glViewport");

		int numTiles = (mWidth / (Tile.TILE_SIZE / 2) + 2)
				* (mHeight / (Tile.TILE_SIZE / 2) + 2);

		curTiles = new TilesData(numTiles);
		newTiles = new TilesData(numTiles);
		nextTiles = new TilesData(numTiles);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + numTiles) * 2;
		int[] mVboIds = new int[numVBO];
		glGenBuffers(numVBO, mVboIds, 0);
		GlUtils.checkGlError("glGenBuffers");

		for (int i = 0; i < numVBO; i++)
			mVBOs.add(new VertexBufferObject(mVboIds[i]));

		// Set up textures
		mTextRenderer = new TextRenderer(numTiles * 2);

		mDebugSettings = mMapView.getDebugSettings();
		mJobParameter = mMapView.getJobParameters();

		mInitial = true;
		mMapView.redrawTiles();
	}

	private boolean mSimpleLines = false;

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Set up the program for rendering lines

		lineProgram = GlUtils.createProgram(Shaders.gLineVertexShader,
				Shaders.gLineFragmentShader);
		if (lineProgram == 0) {
			mSimpleLines = true;
			Log.e(TAG, "trying simple line program.");
			lineProgram = GlUtils.createProgram(Shaders.gLineVertexShader,
					Shaders.gLineFragmentShaderSimple);
			if (lineProgram == 0) {
				Log.e(TAG, "Could not create line program.");
				return;
			}
		}

		String ext = glGetString(GL_EXTENSIONS);

		if (ext.indexOf("GL_OES_vertex_half_float") >= 0) {
			useHalfFloat = true;
			shortBuffer = new ShortBuffer[rotateBuffers * 2];
		}
		else {
			floatBuffer = new FloatBuffer[rotateBuffers * 2];
		}
		Log.d(TAG, "Extensions: " + ext);

		hLineMatrix = glGetUniformLocation(lineProgram, "u_center");
		hLineMode = glGetUniformLocation(lineProgram, "u_mode");
		hLineColor = glGetUniformLocation(lineProgram, "u_color");
		hLineVertexPosition = GLES20.glGetAttribLocation(lineProgram, "a_position");
		hLineTexturePosition = glGetAttribLocation(lineProgram, "a_st");
		if (mSimpleLines)
			hLineWidth = glGetUniformLocation(lineProgram, "u_width");

		// Set up the program for rendering polygons
		polygonProgram = GlUtils.createProgram(Shaders.gPolygonVertexShader,
				Shaders.gPolygonFragmentShader);
		if (polygonProgram == 0) {
			Log.e(TAG, "Could not create polygon program.");
			return;
		}
		hPolygonMatrix = glGetUniformLocation(polygonProgram, "u_center");
		hPolygonVertexPosition = glGetAttribLocation(polygonProgram, "a_position");
		hPolygonColor = glGetUniformLocation(polygonProgram, "u_color");

		// glUseProgram(polygonProgram);
		// glEnableVertexAttribArray(hPolygonVertexPosition);
		//
		// glUseProgram(lineProgram);
		// glEnableVertexAttribArray(hLineVertexPosition);
		// glEnableVertexAttribArray(hLineTexturePosition);

		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		// glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_DITHER);
		glClearColor(0.98f, 0.98f, 0.97f, 1.0f);
		glClearStencil(0);

		GlUtils.checkGlError("onSurfaceCreated");

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
