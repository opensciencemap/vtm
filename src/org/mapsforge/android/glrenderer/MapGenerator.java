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
import org.mapsforge.android.rendertheme.renderinstruction.Caption;
import org.mapsforge.android.rendertheme.renderinstruction.Line;
import org.mapsforge.android.rendertheme.renderinstruction.PathText;
import org.mapsforge.android.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.core.WebMercator;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;
import org.mapsforge.database.QueryResult;

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

	private GLMapTile mCurrentTile;

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

	/**
	 * 
	 */
	public MapGenerator() {
		Log.d(TAG, "init DatabaseRenderer");
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
			long x = mCurrentTile.x;
			long y = mCurrentTile.y;
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

		if (lineLayer == null)
			return;

		if (line.outline) {
			lineLayer.addOutline(mCurLineLayer);
			return;
		}

		mCurLineLayer = lineLayer;

		boolean round = line.round;

		for (int i = 0, pos = 0, n = mWays.length; i < n; i++) {
			int length = mWays[i];
			if (length < 0)
				break;

			// save some vertices
			if (round && i > 200) {
				// Log.d(TAG, "WAY TOO MANY LINES!!!");
				round = false;
			}
			// need at least two points
			if (length >= 4) {
				lineLayer.addLine(mWayNodes, pos, length);
				countLines++;
				countNodes += length;
			}
			pos += length;
		}

		// if (line.outline < 0)
		// return;
		//
		// Line outline = MapGenerator.renderTheme.getOutline(line.outline);
		//
		// if (outline == null)
		// return;
		//
		// numLayer = mDrawingLayer + outline.getLevel();
		//
		// l = mLineLayers;
		//
		// if (l == null || l.layer > numLayer) {
		// // insert new layer at start
		// outlineLayer = new LineLayer(numLayer, outline, w, true);
		// // outlineLayer = LineLayers.get(numLayer, outline, w, true);
		// outlineLayer.next = l;
		// mLineLayers = outlineLayer;
		// } else {
		// while (l != null) {
		// if (l.layer == numLayer) {
		// outlineLayer = l;
		// break;
		// }
		// // insert new layer between current and next layer
		// if (l.next == null || l.next.layer > numLayer) {
		// outlineLayer = new LineLayer(numLayer, outline, w, true);
		// // outlineLayer = LineLayers.get(numLayer, outline, w, true);
		// outlineLayer.next = l.next;
		// l.next = outlineLayer;
		// }
		// l = l.next;
		// }
		// }
		//
		// if (outlineLayer != null)
		// outlineLayer.addOutline(lineLayer);

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

		for (int i = 0, pos = 0, n = mWays.length; i < n; i++) {
			int length = mWays[i];
			if (length < 0)
				break;

			// need at least three points
			if (length >= 6)
				layer.addPolygon(mWayNodes, pos, length);

			pos += length;
		}

		// if (area.line != null)
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
	public boolean executeJob(MapGeneratorJob mapGeneratorJob) {
		GLMapTile tile;

		if (mMapDatabase == null)
			return false;

		tile = mCurrentTile = (GLMapTile) mapGeneratorJob.tile;
		mDebugDrawPolygons = !mapGeneratorJob.debugSettings.mDisablePolygons;
		mDebugDrawUnmatched = mapGeneratorJob.debugSettings.mDrawUnmatchted;

		if (tile.isLoading || tile.isReady || tile.isCanceled)
			return false;

		tile.isLoading = true;

		mLevels = MapGenerator.renderTheme.getLevels();

		// limit stroke scale at z=17
		if (tile.zoomLevel < STROKE_MAX_ZOOM_LEVEL)
			setScaleStrokeWidth(tile.zoomLevel);
		else
			setScaleStrokeWidth(STROKE_MAX_ZOOM_LEVEL);

		mLineLayers = null;
		mPolyLayers = null;
		mLabels = null;

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
			tile.isLoading = false;
			return false;
		}

		if (mapGeneratorJob.debugSettings.mDrawTileFrames) {

			final float[] debugBoxCoords = { 0, 0, 0, Tile.TILE_SIZE,
					Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE, 0, 0, 0 };
			final short[] debugBoxIndex = { 10 };

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

		tile.newData = true;
		tile.isLoading = false;

		return true;
	}

	private final Tag[] debugTagBox = { new Tag("debug", "box") };
	private final Tag[] debugTagWay = { new Tag("debug", "way") };
	private final Tag[] debugTagArea = { new Tag("debug", "area") };

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

		long x = mCurrentTile.x;
		long y = mCurrentTile.y;
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
