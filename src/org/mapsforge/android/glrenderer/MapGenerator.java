/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.glrenderer;

import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.rendertheme.IRenderCallback;
import org.mapsforge.android.rendertheme.RenderTheme;
import org.mapsforge.android.rendertheme.renderinstruction.Area;
import org.mapsforge.android.rendertheme.renderinstruction.Line;
import org.mapsforge.android.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.core.WebMercator;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

/**
 * 
 */
public class MapGenerator implements IMapGenerator, IRenderCallback, IMapDatabaseCallback {

	private static String TAG = MapGenerator.class.getName();

	private static final double PI180 = (Math.PI / 180) / 1000000.0;
	private static final double PIx4 = Math.PI * 4;
	private static final double STROKE_INCREASE = Math.sqrt(2);
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	private static final byte LAYERS = 11;
	private static final double f900913 = 20037508.342789244;

	private static RenderTheme renderTheme;

	private IMapDatabase mMapDatabase;

	private GLMapTile mCurrentTile;

	private float[] mWayNodes;
	private int[] mWays;

	private LineLayers mLineLayers;
	private PolygonLayers mPolyLayers;
	private MeshLayers mMeshLayers;

	private int mDrawingLayer;
	private int mLevels;

	private boolean useSphericalMercator = false;
	private float mStrokeScale = 1.0f;

	/**
	 * 
	 */
	public MapGenerator() {
		Log.d(TAG, "init DatabaseRenderer");
		LayerPool.init();
	}

	@Override
	public void renderPointOfInterest(byte layer, int latitude, int longitude, Tag[] tags) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderWaterBackground() {
		// TODO Auto-generated method stub

	}

	private boolean mProjected;
	private boolean mProjectedResult;
	private float mSimplify;

	private boolean projectToTile(boolean area) {
		if (mProjected)
			return mProjectedResult;

		float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, maxx = Float.MIN_VALUE, maxy = Float.MIN_VALUE;

		float[] coords = mWayNodes;

		long x = mCurrentTile.x;
		long y = mCurrentTile.y;
		long z = Tile.TILE_SIZE << mCurrentTile.zoomLevel;
		float min = mSimplify;

		double divx, divy;
		long dx = (x - (z >> 1));
		long dy = (y - (z >> 1));

		if (useSphericalMercator) {
			divx = f900913 / (z >> 1);
			divy = f900913 / (z >> 1);
		} else {
			divx = 180000000.0 / (z >> 1);
			divy = z / PIx4;
		}

		for (int pos = 0, outPos = 0, i = 0, m = mWays.length; i < m; i++) {
			int len = mWays[i];
			if (len == 0)
				continue;
			int cnt = 0;
			float lat, lon, prevLon = 0, prevLat = 0;

			for (int end = pos + len; pos < end; pos += 2) {

				if (useSphericalMercator) {
					lon = (float) (coords[pos] / divx - dx);
					lat = (float) (coords[pos + 1] / divy + dy);
				} else {
					lon = (float) ((coords[pos]) / divx - dx);
					double sinLat = Math.sin(coords[pos + 1] * PI180);
					lat = (float) (Math.log((1.0 + sinLat) / (1.0 - sinLat)) * divy + dy);
				}

				if (area && i == 0) {
					if (lon < minx)
						minx = lon;
					if (lon > maxx)
						maxx = lon;
					if (lat < miny)
						miny = lat;
					if (lat > maxy)
						maxy = lat;
				}

				if (cnt != 0) {
					// drop small distance intermediate nodes

					if (lat == prevLat && lon == prevLon)
						continue;

					if ((pos != end - 2) &&
							!((lat > prevLat + min || lat < prevLat - min) ||
							(lon > prevLon + min || lon < prevLon - min)))
						continue;
				}
				coords[outPos++] = prevLon = lon;
				coords[outPos++] = prevLat = lat;

				cnt += 2;
			}

			if (area) {
				// Log.d(TAG, "area:" + (maxx - minx) * (maxy - miny));
				if ((maxx - minx) * (maxy - miny) < 2000 / mCurrentTile.zoomLevel) {
					mProjected = true;
					mProjectedResult = false;
					return false;
				}
			}

			mWays[i] = cnt;
		}
		mProjected = true;
		mProjectedResult = true;
		return true;
	}

	private boolean firstMatch;
	private boolean prevClosed;

	private RenderInstruction[] mRenderInstructions = null;

	@Override
	public void renderWay(byte layer, Tag[] tags, float[] wayNodes, int[] wayLength,
			boolean changed) {

		mProjected = false;
		mDrawingLayer = getValidLayer(layer) * mLevels;

		int len = wayLength[0];
		boolean closed = (wayNodes[0] == wayNodes[len - 2] &&
				wayNodes[1] == wayNodes[len - 1]);

		mSimplify = 0.5f;

		if (closed) {
			if (mCurrentTile.zoomLevel < 14)
				mSimplify = 0.5f;
			else
				mSimplify = 0.2f;

			if (tags.length == 1 && "water".equals(tags[0].value))
				mSimplify = 0;
		}

		mWayNodes = wayNodes;
		mWays = wayLength;

		if (!firstMatch && prevClosed == closed && !changed) {
			if (mRenderInstructions != null) {
				for (int i = 0, n = mRenderInstructions.length; i < n; i++)
					mRenderInstructions[i].renderWay(this, tags);
			}
			// MapGenerator.renderTheme.matchWay(this, tags,
			// (byte) (mCurrentTile.zoomLevel + 0),
			// closed, false);
		} else {
			prevClosed = closed;
			mRenderInstructions = MapGenerator.renderTheme.matchWay(this, tags,
					(byte) (mCurrentTile.zoomLevel + 0),
					closed, true);
		}

		firstMatch = false;
	}

	@Override
	public void renderAreaCaption(String caption, float verticalOffset, Paint paint,
			Paint stroke) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderAreaSymbol(Bitmap symbol) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderPointOfInterestCaption(String caption, float verticalOffset,
			Paint paint, Paint stroke) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderPointOfInterestCircle(float radius, Paint fill, int level) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderPointOfInterestSymbol(Bitmap symbol) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderWay(Line line) {

		projectToTile(false);

		LineLayer outlineLayer = null;
		LineLayer l = mLineLayers.getLayer(mDrawingLayer + line.level, line.color, false,
				line.fixed);

		float w = line.strokeWidth;

		if (!line.fixed) {
			w *= mStrokeScale;
			w *= mProjectionScaleFactor;
		}
		if (line.outline != -1) {
			Line outline = MapGenerator.renderTheme.getOutline(line.outline);
			if (outline != null) {
				outlineLayer = mLineLayers.getLayer(mDrawingLayer + outline.level,
						outline.color, true, false);
				outlineLayer.addOutline(l);
			}
		}

		for (int i = 0, pos = 0, n = mWays.length; i < n; i++) {
			int length = mWays[i];

			// need at least two points
			if (length >= 4)
				l.addLine(mWayNodes, pos, length, w, line.round);

			pos += length;
		}
	}

	@Override
	public void renderArea(Area area) {
		if (!mDebugDrawPolygons)
			return;

		// if (!projectToTile(mCurrentTile.zoomLevel < 14))
		if (!projectToTile(false))
			return;

		PolygonLayer l = mPolyLayers.getLayer(mDrawingLayer + area.level, area.color,
				area.fade);

		// MeshLayer l = mMeshLayers.getLayer(mDrawingLayer + area.level, area.color,
		// area.fade);

		for (int i = 0, pos = 0, n = mWays.length; i < n; i++) {
			int length = mWays[i];
			// need at least three points
			if (length >= 6)
				l.addPolygon(mWayNodes, pos, length);

			pos += length;
		}

	}

	@Override
	public void renderWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat) {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderWayText(String text, Paint paint, Paint stroke) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	private boolean mDebugDrawPolygons;

	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob) {
		// Log.d(TAG, "load " + mCurrentTile);

		if (!(mapGeneratorJob.tile instanceof GLMapTile))
			return false;

		if (mMapDatabase == null)
			return false;

		useSphericalMercator = WebMercator.NAME.equals(mMapDatabase.getMapProjection());

		mCurrentTile = (GLMapTile) mapGeneratorJob.tile;
		mDebugDrawPolygons = !mapGeneratorJob.debugSettings.mDisablePolygons;

		if (mCurrentTile.isLoading || mCurrentTile.isDrawn)
			return false;

		mCurrentTile.isLoading = true;

		mLevels = MapGenerator.renderTheme.getLevels();

		setScaleStrokeWidth(mCurrentTile.zoomLevel);

		mLineLayers = new LineLayers();
		mPolyLayers = new PolygonLayers();
		mMeshLayers = new MeshLayers();
		mCurrentTile.lineLayers = mLineLayers;
		mCurrentTile.polygonLayers = mPolyLayers;
		mCurrentTile.meshLayers = mMeshLayers;

		firstMatch = true;

		mProjectionScaleFactor = (float) (1.0 / Math.cos(MercatorProjection
				.pixelYToLatitude(mCurrentTile.pixelY, mCurrentTile.zoomLevel)
				* (Math.PI / 180))) / 1.5f;
		mMapDatabase.executeQuery(mCurrentTile, this);

		if (mapGeneratorJob.debugSettings.mDrawTileFrames) {
			float[] coords = { 0, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE,
					Tile.TILE_SIZE, 0, 0, 0 };
			LineLayer ll = mLineLayers.getLayer(Integer.MAX_VALUE, Color.BLACK, false,
					true);
			ll.addLine(coords, 0, coords.length, 2.0f, false);
		}

		mCurrentTile.newData = true;
		return true;
	}

	private float mProjectionScaleFactor;

	private static byte getValidLayer(byte layer) {
		if (layer < 0) {
			return 0;
			/**
			 * return instances of MapRenderer
			 */
		} else if (layer >= LAYERS) {
			return LAYERS - 1;
		} else {
			return layer;
		}
	}

	/**
	 * Sets the scale stroke factor for the given zoom level.
	 * 
	 * @param zoomLevel
	 *            the zoom level for which the scale stroke factor should be set.
	 */
	private void setScaleStrokeWidth(byte zoomLevel) {
		int zoomLevelDiff = Math.max(zoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
		mStrokeScale = (float) Math.pow(STROKE_INCREASE, zoomLevelDiff);
		if (mStrokeScale < 1)
			mStrokeScale = 1;
	}

	@Override
	public void setMapDatabase(IMapDatabase mapDatabase) {
		mMapDatabase = mapDatabase;
	}

	@Override
	public IMapDatabase getMapDatabase() {
		return mMapDatabase;
	}

	@Override
	public void setRenderTheme(RenderTheme theme) {
		MapGenerator.renderTheme = theme;
	}
}
