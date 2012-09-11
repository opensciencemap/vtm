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
package org.oscim.view.glrenderer;

import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.core.WebMercator;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.QueryResult;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Caption;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.PathText;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.view.DebugSettings;
import org.oscim.view.MapView;
import org.oscim.view.mapgenerator.IMapGenerator;
import org.oscim.view.mapgenerator.JobTile;

import android.graphics.Bitmap;
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
	private static final byte LAYERS = 11;
	private static final double f900913 = 20037508.342789244;

	static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	static final byte STROKE_MAX_ZOOM_LEVEL = 17;

	private static RenderTheme renderTheme;

	private IMapDatabase mMapDatabase;

	private MapTile mCurrentTile;

	private float[] mWayNodes;
	private short[] mWays;

	private LineLayer mLineLayers;
	private PolygonLayer mPolyLayers;
	private LineLayer mCurLineLayer;
	private PolygonLayer mCurPolyLayer;

	private TextItem mLabels;

	private int mDrawingLayer;
	private int mLevels;

	private float mStrokeScale = 1.0f;

	private boolean mProjected;
	// private boolean mProjectedResult;
	private float mSimplify;
	// private boolean firstMatch;
	// private boolean prevClosed;

	private RenderInstruction[] mRenderInstructions = null;

	private final String TAG_WATER = "water".intern();
	private final MapView mMapView;

	private final Tag[] debugTagBox = { new Tag("debug", "box") };
	private final Tag[] debugTagWay = { new Tag("debug", "way") };
	private final Tag[] debugTagArea = { new Tag("debug", "area") };
	private final float[] debugBoxCoords = { 0, 0, 0, Tile.TILE_SIZE,
			Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, 0, 0, 0 };
	private final short[] debugBoxIndex = { 10 };

	private float mProjectionScaleFactor;

	/**
	 * @param mapView
	 *            the MapView
	 */
	public MapGenerator(MapView mapView) {
		Log.d(TAG, "init DatabaseRenderer");
		mMapView = mapView;
	}

	private float mPoiX = 256;
	private float mPoiY = 256;

	private Tag mTagEmptyName = new Tag(Tag.TAG_KEY_NAME, null, false);
	private Tag mTagName;

	private void filterTags(Tag[] tags) {
		for (int i = 0; i < tags.length; i++) {
			// Log.d(TAG, "check tag: " + tags[i]);
			if (tags[i].key == Tag.TAG_KEY_NAME && tags[i].value != null) {
				mTagName = tags[i];
				tags[i] = mTagEmptyName;
			}
		}
	}

	// private RenderInstruction[] mNodeRenderInstructions;

	@Override
	public void renderPointOfInterest(byte layer, float latitude, float longitude,
			Tag[] tags) {

		mTagName = null;

		if (mMapProjection != null)
		{
			long x = mCurrentTile.pixelX;
			long y = mCurrentTile.pixelY + Tile.TILE_SIZE;
			long z = Tile.TILE_SIZE << mCurrentTile.zoomLevel;

			double divx, divy;
			long dx = (x - (z >> 1));
			long dy = (y - (z >> 1));

			if (mMapProjection == WebMercator.NAME) {
				double div = f900913 / (z >> 1);
				// divy = f900913 / (z >> 1);
				mPoiX = (float) (longitude / div - dx);
				mPoiY = (float) (latitude / div + dy);
			} else {
				divx = 180000000.0 / (z >> 1);
				divy = z / PIx4;
				mPoiX = (float) (longitude / divx - dx);
				double sinLat = Math.sin(latitude * PI180);
				mPoiY = (float) (Math.log((1.0 + sinLat) / (1.0 - sinLat)) * divy + dy);
				if (mPoiX < -10 || mPoiX > Tile.TILE_SIZE + 10 || mPoiY < -10
						|| mPoiY > Tile.TILE_SIZE + 10)
					return;
			}
		} else {
			mPoiX = longitude;
			mPoiY = latitude;
		}

		// remove tags that should not be cached in Rendertheme
		filterTags(tags);
		// Log.d(TAG, "renderPointOfInterest: " + mTagName);

		// mNodeRenderInstructions =
		MapGenerator.renderTheme.matchNode(this, tags, mCurrentTile.zoomLevel);
	}

	@Override
	public void renderWaterBackground() {
		// TODO Auto-generated method stub

	}

	@Override
	public void renderWay(byte layer, Tag[] tags, float[] wayNodes, short[] wayLength,
			boolean closed) {

		mTagName = null;
		mProjected = false;
		mCurLineLayer = null;

		mDrawingLayer = getValidLayer(layer) * mLevels;
		mSimplify = 0.5f;

		if (closed) {
			if (mCurrentTile.zoomLevel < 14)
				mSimplify = 0.5f;
			else
				mSimplify = 0.2f;

			if (tags.length == 1 && TAG_WATER == (tags[0].value))
				mSimplify = 0;
		}

		mWayNodes = wayNodes;
		mWays = wayLength;

		// remove tags that should not be cached in Rendertheme
		filterTags(tags);

		// if (mRenderInstructions != null) {
		// for (int i = 0, n = mRenderInstructions.length; i < n; i++)
		// mRenderInstructions[i].renderWay(this, tags);
		// }

		mRenderInstructions = MapGenerator.renderTheme.matchWay(this, tags,
				(byte) (mCurrentTile.zoomLevel + 0),
				closed, true);

		if (mRenderInstructions == null && mDebugDrawUnmatched)
			debugUnmatched(closed, tags);
	}

	private void debugUnmatched(boolean closed, Tag[] tags) {

		Log.d(TAG, "way not matched: " + tags[0] + " "
				+ (tags.length > 1 ? tags[1] : "") + " " + closed);

		mTagName = new Tag("name", tags[0].key + ":" + tags[0].value, false);

		if (closed) {
			mRenderInstructions = MapGenerator.renderTheme.matchWay(this, debugTagArea,
					(byte) 0, true, true);
		} else {
			mRenderInstructions = MapGenerator.renderTheme.matchWay(this, debugTagWay,
					(byte) 0, true, true);
		}
	}

	@Override
	public void renderAreaCaption(Caption caption) {
		// Log.d(TAG, "renderAreaCaption: " + mTagName);

		if (mTagName == null)
			return;

		if (caption.textKey == mTagEmptyName.key) {

			TextItem t = new TextItem(mWayNodes[0], mWayNodes[1], mTagName.value, caption);
			t.next = mLabels;
			mLabels = t;
		}
	}

	@Override
	public void renderPointOfInterestCaption(Caption caption) {
		// Log.d(TAG, "renderPointOfInterestCaption: " + mPoiX + " " + mPoiY + " "
		// + mTagName);

		if (mTagName == null)
			return;

		if (caption.textKey == mTagEmptyName.key) {
			TextItem t = new TextItem(mPoiX, mPoiY, mTagName.value, caption);
			t.next = mLabels;
			mLabels = t;
		}
	}

	@Override
	public void renderWayText(PathText pathText) {
		// Log.d(TAG, "renderWayText: " + mTagName);

		if (mTagName == null)
			return;

		if (pathText.textKey == mTagEmptyName.key) {

			mLabels = WayDecorator.renderText(mWayNodes, mTagName.value, pathText, 0,
					mWays[0], mLabels);
		}
	}

	@Override
	public void renderAreaSymbol(Bitmap symbol) {
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

	private int countLines;
	private int countNodes;

	@Override
	public void renderWay(Line line, int level) {

		projectToTile();

		if (line.outline && mCurLineLayer == null)
			return;

		float w = line.width;

		if (!line.fixed) {
			w *= mStrokeScale;
			w *= mProjectionScaleFactor;
		}

		LineLayer lineLayer = null;

		int numLayer = mDrawingLayer + level;

		LineLayer l = mLineLayers;

		if (mCurLineLayer != null && mCurLineLayer.layer == numLayer) {
			lineLayer = mCurLineLayer;
		} else if (l == null || l.layer > numLayer) {
			// insert new layer at start
			lineLayer = new LineLayer(numLayer, line, w, line.outline);
			// lineLayer = LineLayers.get(numLayer, line, w, false);

			lineLayer.next = l;
			mLineLayers = lineLayer;
		} else {
			while (l != null) {
				// found layer
				if (l.layer == numLayer) {
					lineLayer = l;
					break;
				}
				// insert new layer between current and next layer
				if (l.next == null || l.next.layer > numLayer) {
					lineLayer = new LineLayer(numLayer, line, w, line.outline);
					// lineLayer = LineLayers.get(numLayer, line, w, false);
					lineLayer.next = l.next;
					l.next = lineLayer;
				}
				l = l.next;
			}
		}

		if (lineLayer == null) {
			mCurLineLayer = null;
			return;
		}
		if (line.outline) {
			lineLayer.addOutline(mCurLineLayer);
			return;
		}

		mCurLineLayer = lineLayer;

		lineLayer.addLine(mWayNodes, mWays);
	}

	@Override
	public void renderArea(Area area, int level) {
		if (!mDebugDrawPolygons)
			return;

		if (!mProjected && !projectToTile())
			return;

		int numLayer = mDrawingLayer + level;

		PolygonLayer layer = null;
		PolygonLayer l = mPolyLayers;

		if (mCurPolyLayer != null && mCurPolyLayer.layer == numLayer) {
			layer = mCurPolyLayer;
		} else if (l == null || l.layer > numLayer) {
			// insert new layer at start
			layer = new PolygonLayer(numLayer, area);
			layer.next = l;
			mPolyLayers = layer;
		} else {
			while (l != null) {

				if (l.layer == numLayer) {
					layer = l;
					break;
				}

				// insert new layer between current and next layer
				if (l.next == null || l.next.layer > numLayer) {
					layer = new PolygonLayer(numLayer, area);
					layer.next = l.next;
					l.next = layer;
				}
				l = l.next;
			}
		}
		if (layer == null)
			return;

		mCurPolyLayer = layer;

		layer.addPolygon(mWayNodes, mWays);
	}

	@Override
	public void renderWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	private boolean mDebugDrawPolygons;
	boolean mDebugDrawUnmatched;

	@Override
	public boolean executeJob(JobTile jobTile) {
		MapTile tile;

		if (mMapDatabase == null)
			return false;

		tile = mCurrentTile = (MapTile) jobTile;
		DebugSettings debugSettings = mMapView.getDebugSettings();

		mDebugDrawPolygons = !debugSettings.mDisablePolygons;
		mDebugDrawUnmatched = debugSettings.mDrawUnmatchted;

		// fixed now....
		if (tile.newData || tile.isReady) {
			Log.d(TAG, "XXX tile already loaded "
					+ tile + " "
					+ tile.newData + " "
					+ tile.isReady + " ");
			return false;
		}

		mLevels = MapGenerator.renderTheme.getLevels();

		// limit stroke scale at z=17
		if (tile.zoomLevel < STROKE_MAX_ZOOM_LEVEL)
			setScaleStrokeWidth(tile.zoomLevel);
		else
			setScaleStrokeWidth(STROKE_MAX_ZOOM_LEVEL);

		// firstMatch = true;
		countLines = 0;
		countNodes = 0;

		// acount for area changes with latitude
		mProjectionScaleFactor = 0.5f + (float) (0.5 / Math.cos(MercatorProjection
				.pixelYToLatitude(tile.pixelY, tile.zoomLevel)
				* (Math.PI / 180)));

		if (mMapDatabase.executeQuery(tile, this) != QueryResult.SUCCESS) {
			Log.d(TAG, "Failed loading: " + tile);
			LineLayers.clear(mLineLayers);
			PolygonLayers.clear(mPolyLayers);
			mLineLayers = null;
			mPolyLayers = null;
			mLabels = null;
			tile.isLoading = false;
			return false;
		}

		if (debugSettings.mDrawTileFrames) {
			mTagName = new Tag("name", countLines + " " + countNodes + " "
					+ tile.toString(), false);
			mPoiX = Tile.TILE_SIZE >> 1;
			mPoiY = 10;
			MapGenerator.renderTheme.matchNode(this, debugTagWay, (byte) 0);

			mWays = debugBoxIndex;
			mWayNodes = debugBoxCoords;
			mDrawingLayer = 10 * mLevels;
			MapGenerator.renderTheme.matchWay(this, debugTagBox, (byte) 0, false, true);
		}

		tile.lineLayers = mLineLayers;
		tile.polygonLayers = mPolyLayers;
		tile.labels = mLabels;

		mCurPolyLayer = null;
		mCurLineLayer = null;
		mLineLayers = null;
		mPolyLayers = null;
		mLabels = null;

		return true;
	}

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

	private String mMapProjection;

	@Override
	public void setMapDatabase(IMapDatabase mapDatabase) {
		mMapDatabase = mapDatabase;
		mMapProjection = mMapDatabase.getMapProjection();
	}

	@Override
	public IMapDatabase getMapDatabase() {
		return mMapDatabase;
	}

	@Override
	public void setRenderTheme(RenderTheme theme) {
		MapGenerator.renderTheme = theme;
	}

	@Override
	public boolean checkWay(Tag[] tags, boolean closed) {

		mRenderInstructions = MapGenerator.renderTheme.matchWay(this, tags,
				(byte) (mCurrentTile.zoomLevel + 0), closed, false);

		return mRenderInstructions != null;
	}

	private boolean projectToTile() {
		if (mProjected || mMapProjection == null)
			return true;

		boolean useWebMercator = false;

		if (mMapProjection == WebMercator.NAME)
			useWebMercator = true;

		float[] coords = mWayNodes;

		long x = mCurrentTile.pixelX;
		long y = mCurrentTile.pixelY + Tile.TILE_SIZE;
		long z = Tile.TILE_SIZE << mCurrentTile.zoomLevel;
		float min = mSimplify;

		double divx, divy;
		long dx = (x - (z >> 1));
		long dy = (y - (z >> 1));

		if (useWebMercator) {
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
			if (len < 0)
				break;

			int cnt = 0;
			float lat, lon, prevLon = 0, prevLat = 0;

			for (int end = pos + len; pos < end; pos += 2) {

				if (useWebMercator) {
					lon = (float) (coords[pos] / divx - dx);
					lat = (float) (coords[pos + 1] / divy + dy);
				} else {
					lon = (float) ((coords[pos]) / divx - dx);
					double sinLat = Math.sin(coords[pos + 1] * PI180);
					lat = (float) (Math.log((1.0 + sinLat) / (1.0 - sinLat)) * divy + dy);
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

			mWays[i] = (short) cnt;
		}
		mProjected = true;
		// mProjectedResult = true;
		return true;
	}
}
