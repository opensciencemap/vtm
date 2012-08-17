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

import java.util.ArrayList;

import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.rendertheme.IRenderCallback;
import org.mapsforge.android.rendertheme.RenderTheme;
import org.mapsforge.android.rendertheme.renderinstruction.Area;
import org.mapsforge.android.rendertheme.renderinstruction.Caption;
import org.mapsforge.android.rendertheme.renderinstruction.Line;
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
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	private static final byte LAYERS = 11;
	private static final double f900913 = 20037508.342789244;

	private static RenderTheme renderTheme;

	private IMapDatabase mMapDatabase;

	private GLMapTile mCurrentTile;

	private float[] mWayNodes;
	private short[] mWays;

	private LineLayer mLineLayers;
	private PolygonLayer mPolyLayers;
	private LineLayer mCurLineLayer;
	private PolygonLayer mCurPolyLayer;

	private ArrayList<TextItem> mLabels;

	private int mDrawingLayer;
	private int mLevels;

	private boolean useSphericalMercator = false;
	private float mStrokeScale = 1.0f;

	/**
	 * 
	 */
	public MapGenerator() {
		Log.d(TAG, "init DatabaseRenderer");
		VertexPool.init();
	}

	private float mPoiX = 256;
	private float mPoiY = 256;

	private Tag mTagEmptyName = new Tag("name", "");
	private Tag mTagName;

	private void filterTags(Tag[] tags) {
		for (int i = 0; i < tags.length; i++) {
			// Log.d(TAG, "check tag: " + tags[i]);
			if (tags[i].key == mTagEmptyName.key && tags[i].value != null) {
				mTagName = tags[i];
				tags[i] = mTagEmptyName;
			}
		}
	}

	@Override
	public void renderPointOfInterest(byte layer, float latitude, float longitude,
			Tag[] tags) {

		mTagName = null;

		long x = mCurrentTile.x;
		long y = mCurrentTile.y;
		long z = Tile.TILE_SIZE << mCurrentTile.zoomLevel;

		double divx, divy;
		long dx = (x - (z >> 1));
		long dy = (y - (z >> 1));

		if (useSphericalMercator) {
			divx = f900913 / (z >> 1);
			divy = f900913 / (z >> 1);
			mPoiX = (float) (longitude / divx - dx);
			mPoiY = (float) (latitude / divy + dy);
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

		// remove tags that should not be cached in Rendertheme
		filterTags(tags);
		// Log.d(TAG, "renderPointOfInterest: " + mTagName);

		MapGenerator.renderTheme.matchNode(this, tags, mCurrentTile.zoomLevel);
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

		// float minx = Float.MAX_VALUE, miny = Float.MAX_VALUE, maxx = Float.MIN_VALUE, maxy = Float.MIN_VALUE;

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

				// if (area && i == 0) {
				// if (lon < minx)
				// minx = lon;
				// if (lon > maxx)
				// maxx = lon;
				// if (lat < miny)
				// miny = lat;
				// if (lat > maxy)
				// maxy = lat;
				// }

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

			// if (area) {
			// // Log.d(TAG, "area:" + (maxx - minx) * (maxy - miny));
			// if ((maxx - minx) * (maxy - miny) < 2000 / mCurrentTile.zoomLevel) {
			// mProjected = true;
			// mProjectedResult = false;
			// return false;
			// }
			// }

			mWays[i] = (short) cnt;
		}
		mProjected = true;
		mProjectedResult = true;
		return true;
	}

	// private boolean firstMatch;
	// private boolean prevClosed;

	private RenderInstruction[] mRenderInstructions = null;

	private final String TAG_WATER = "water".intern();

	@Override
	public void renderWay(byte layer, Tag[] tags, float[] wayNodes, short[] wayLength,
			boolean changed) {

		// Log.d(TAG, "render way: " + layer);
		mTagName = null;

		mProjected = false;
		mDrawingLayer = getValidLayer(layer) * mLevels;

		// int len = wayLength[0];
		// boolean closed = (wayNodes[0] == wayNodes[len - 2] &&
		// wayNodes[1] == wayNodes[len - 1]);

		boolean closed = changed;
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

		// if (mRenderInstructions != null) {
		// for (int i = 0, n = mRenderInstructions.length; i < n; i++)
		// mRenderInstructions[i].renderWay(this, tags);
		// }

		// prevClosed = closed;
		mRenderInstructions = MapGenerator.renderTheme.matchWay(this, tags,
				(byte) (mCurrentTile.zoomLevel + 0),
				closed, true);

		if (mRenderInstructions == null && mDebugDrawUnmatched)
			debugUnmatched(closed, tags);

		// firstMatch = false;
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
			if (mLabels == null)
				mLabels = new ArrayList<TextItem>();
			mLabels.add(new TextItem(mWayNodes[0], mWayNodes[1], mTagName.value, caption));
		}

		// if (caption.textKey == mTagEmptyName.key)
		// mLabels.add(new TextItem(mPoiX, mPoiY, mTagName.value, caption));
	}

	@Override
	public void renderPointOfInterestCaption(Caption caption) {
		// Log.d(TAG, "renderPointOfInterestCaption: " + mPoiX + " " + mPoiY + " "
		// + mTagName);

		if (mTagName == null)
			return;

		if (caption.textKey == mTagEmptyName.key) {
			if (mLabels == null)
				mLabels = new ArrayList<TextItem>();
			mLabels.add(new TextItem(mPoiX, mPoiY, mTagName.value, caption));
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

	@Override
	public void renderWay(Line line) {
		projectToTile(false);

		LineLayer outlineLayer = null;
		LineLayer lineLayer = null;

		int numLayer = mDrawingLayer + line.level;

		// LineLayer l = mLineLayers;
		//
		// for (; l != null; l = l.next)
		// if (l.next == null || l.next.layer > numLayer)
		// break;
		//
		// if (l == null || l == mLineLayers) {
		// // insert at start
		// lineLayer = new LineLayer(numLayer, line, false);
		// lineLayer.next = mLineLayers;
		// mLineLayers = lineLayer;
		// } else if (l.layer == numLayer) {
		// lineLayer = l;
		// } else {
		// // insert between current and next layer
		// lineLayer = new LineLayer(numLayer, line, false);
		// lineLayer.next = l.next;
		// l.next = lineLayer;
		// }

		// FIXME simplify this...
		if (mCurLineLayer != null && mCurLineLayer.layer == numLayer) {
			lineLayer = mCurLineLayer;
		} else if (mLineLayers == null || mLineLayers.layer > numLayer) {
			// insert new layer at start
			lineLayer = new LineLayer(numLayer, line, false);
			lineLayer.next = mLineLayers;
			mLineLayers = lineLayer;
		} else {
			for (LineLayer l = mLineLayers; l != null; l = l.next) {
				if (l.layer == numLayer) {
					lineLayer = l;
					break;
				}
				if (l.next == null || l.next.layer > numLayer) {
					lineLayer = new LineLayer(numLayer, line, false);
					// insert new layer between current and next layer
					lineLayer.next = l.next;
					l.next = lineLayer;
				}
			}
		}

		if (lineLayer == null)
			return;

		mCurLineLayer = lineLayer;

		float w = line.strokeWidth;

		if (!line.fixed) {
			w *= mStrokeScale;
			w *= mProjectionScaleFactor;
		}
		else {
			w *= 1.2; // TODO make this dependent on dpi
		}

		for (int i = 0, pos = 0, n = mWays.length; i < n; i++) {
			int length = mWays[i];

			// need at least two points
			if (length >= 4)
				lineLayer.addLine(mWayNodes, pos, length, w, line.round);

			pos += length;
		}

		if (line.outline < 0)
			return;

		Line outline = MapGenerator.renderTheme.getOutline(line.outline);

		if (outline == null)
			return;

		numLayer = mDrawingLayer + outline.level;

		if (mLineLayers == null || mLineLayers.layer > numLayer) {
			// insert new layer at start
			outlineLayer = new LineLayer(numLayer, outline, true);
			outlineLayer.next = mLineLayers;
			mLineLayers = outlineLayer;
		} else {
			for (LineLayer l = mLineLayers; l != null; l = l.next) {
				if (l.layer == numLayer) {
					outlineLayer = l;
					break;
				}
				if (l.next == null || l.next.layer > numLayer) {
					outlineLayer = new LineLayer(numLayer, outline, true);
					// insert new layer between current and next layer
					outlineLayer.next = l.next;
					l.next = outlineLayer;
				}
			}
		}

		if (outlineLayer != null)
			outlineLayer.addOutline(lineLayer);

	}

	@Override
	public void renderArea(Area area) {
		if (!mDebugDrawPolygons)
			return;

		// if (!projectToTile(mCurrentTile.zoomLevel < 14))
		if (!projectToTile(false))
			return;

		int numLayer = mDrawingLayer + area.level;
		PolygonLayer layer = null;

		if (mCurPolyLayer != null && mCurPolyLayer.layer == numLayer) {
			layer = mCurPolyLayer;
		} else if (mPolyLayers == null || mPolyLayers.layer > numLayer) {
			// insert new layer at start
			layer = new PolygonLayer(numLayer, area);
			layer.next = mPolyLayers;
			mPolyLayers = layer;
		} else {
			for (PolygonLayer l = mPolyLayers; l != null; l = l.next) {
				if (l.layer >= numLayer) {
					layer = l;
					break;
				}

				if (l.next == null || l.next.layer > numLayer) {
					layer = new PolygonLayer(numLayer, area);
					// insert new layer between current and next layer
					layer.next = l.next;
					l.next = layer;
				}
			}
		}
		if (layer == null)
			return;

		mCurPolyLayer = layer;

		for (int i = 0, pos = 0, n = mWays.length; i < n; i++) {
			int length = mWays[i];
			// need at least three points
			if (length >= 6)
				layer.addPolygon(mWayNodes, pos, length);

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
	boolean mDebugDrawUnmatched;

	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob) {

		if (!(mapGeneratorJob.tile instanceof GLMapTile))
			return false;

		if (mMapDatabase == null)
			return false;

		useSphericalMercator = WebMercator.NAME.equals(mMapDatabase.getMapProjection());

		mCurrentTile = (GLMapTile) mapGeneratorJob.tile;
		mDebugDrawPolygons = !mapGeneratorJob.debugSettings.mDisablePolygons;
		mDebugDrawUnmatched = mapGeneratorJob.debugSettings.mDrawUnmatchted;
		if (mCurrentTile.isLoading || mCurrentTile.isDrawn)
			return false;

		mCurrentTile.isLoading = true;

		mLevels = MapGenerator.renderTheme.getLevels();

		setScaleStrokeWidth(mCurrentTile.zoomLevel);

		mLineLayers = null;
		mPolyLayers = null;
		mLabels = null;

		// firstMatch = true;

		mProjectionScaleFactor = (float) (1.0 / Math.cos(MercatorProjection
				.pixelYToLatitude(mCurrentTile.pixelY, mCurrentTile.zoomLevel)
				* (Math.PI / 180))); // / 1.5f;

		if (mMapDatabase.executeQuery(mCurrentTile, this) != QueryResult.SUCCESS) {
			LineLayers.clear(mLineLayers);
			PolygonLayers.clear(mPolyLayers);
			mLineLayers = null;
			mPolyLayers = null;
			mCurrentTile.isLoading = false;
			return false;
		}

		if (mapGeneratorJob.debugSettings.mDrawTileFrames) {
			mTagName = new Tag("name", mCurrentTile.toString(), false);
			mPoiX = 10;
			mPoiY = 10;
			MapGenerator.renderTheme.matchNode(this, debugTagWay, (byte) 0);
			// float[] coords = { 0, 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE, Tile.TILE_SIZE,
			// Tile.TILE_SIZE, 0, 0, 0 };
			// LineLayer ll = mLineLayers.getLayer(Integer.MAX_VALUE, Color.BLACK, false,
			// true, -1);
			// ll.addLine(coords, 0, coords.length, 1.5f, false);
		}
		mCurrentTile.lineLayers = mLineLayers;
		mCurrentTile.polygonLayers = mPolyLayers;
		mCurrentTile.labels = mLabels;
		mCurPolyLayer = null;
		mCurLineLayer = null;

		mCurrentTile.newData = true;
		return true;
	}

	private Tag[] debugTagWay = { new Tag("debug", "way") };
	private Tag[] debugTagArea = { new Tag("debug", "area") };

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

	@Override
	public boolean checkWay(Tag[] tags, boolean closed) {

		mRenderInstructions = MapGenerator.renderTheme.matchWay(this, tags,
				(byte) (mCurrentTile.zoomLevel + 0), closed, false);

		return mRenderInstructions != null;
	}
}
