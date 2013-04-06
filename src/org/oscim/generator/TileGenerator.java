/*
 * Copyright 2012, 2013 Hannes Janetzek
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
package org.oscim.generator;

import static org.oscim.generator.JobTile.STATE_NONE;

import java.util.Arrays;

import org.oscim.core.GeometryBuffer;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.database.QueryResult;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.layer.ExtrusionLayer;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.Layers;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.layer.LineTexLayer;
import org.oscim.renderer.layer.PolygonLayer;
import org.oscim.renderer.layer.TextItem;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.utils.LineClipper;
import org.oscim.view.DebugSettings;
import org.oscim.view.MapView;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.Log;

/**
 * @note
 *       1. The MapWorkers call TileGenerator.execute() to load a tile.
 *       2. The tile data will be loaded from current MapDatabase
 *       3. MapDatabase calls the IMapDatabaseCallback functions
 *       implemented by TileGenerator for WAY and POI items.
 *       4. these callbacks then call RenderTheme to get the matching style.
 *       5. RenderTheme calls IRenderCallback functions with style information
 *       6. Styled items become added to MapTile.layers... roughly
 */
public class TileGenerator implements IRenderCallback, IMapDatabaseCallback {

	private static final String TAG = TileGenerator.class.getName();

	private static final double STROKE_INCREASE = Math.sqrt(2.2);
	private static final byte LAYERS = 11;

	public static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	public static final byte STROKE_MAX_ZOOM_LEVEL = 17;

	//private static final Tag[] debugTagBox = ;
	private static final Tag[] debugTagWay = { new Tag("debug", "way") };
	private static final Tag[] debugTagArea = { new Tag("debug", "area") };

	private final GeometryBuffer mDebugPoint = new GeometryBuffer(
			new float[] { Tile.TILE_SIZE >> 1, 10 },
			new short[] { 2 });

	private final WayData mDebugWay = new WayData(
			new GeometryBuffer(
					new float[] { 0, 0, 0, Tile.TILE_SIZE,
							Tile.TILE_SIZE, Tile.TILE_SIZE,
							Tile.TILE_SIZE, 0, 0, 0 },
					new short[1]),
			new Tag[] { new Tag("debug", "box") }
			);

	private static RenderTheme renderTheme;
	private static int renderLevels;
	private static DebugSettings debug;

	// current MapDatabase used by this TileGenerator
	private IMapDatabase mMapDatabase;

	// currently processed tile
	private MapTile mTile;

	// currently processed geometry
	private GeometryBuffer mGeom;

	private WayData mWay;

	// current line layer, will be added to outline layers
	private LineLayer mCurLineLayer;

	private int mDrawingLayer;

	private float mStrokeScale = 1.0f;
	private float mLatScaleFactor;
	private float mGroundResolution;

	// replacement for variable value tags that should not be matched by RenderTheme
	private final static Tag mTagEmptyName = new Tag(Tag.TAG_KEY_NAME, null, false);
	private final static Tag mTagEmptyHouseNr = new Tag(Tag.TAG_KEY_HOUSE_NUMBER, null, false);
	private Tag mTagName;
	private Tag mTagHouseNr;

	private final LineClipper mClipper;

	public static void setRenderTheme(RenderTheme theme) {
		renderTheme = theme;
		renderLevels = theme.getLevels();
	}

	public static void setDebugSettings(DebugSettings debugSettings) {
		debug = debugSettings;
	}

	/**
	 */
	public TileGenerator() {
		mClipper = new LineClipper(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE, true);
	}

	public void cleanup() {
	}

	public boolean executeJob(JobTile jobTile) {

		if (mMapDatabase == null)
			return false;

		mTile = (MapTile) jobTile;

		if (mTile.layers != null) {
			// should be fixed now.
			Log.d(TAG, "BUG tile already loaded " + mTile + " " + mTile.state);
			mTile = null;
			return false;
		}

		setScaleStrokeWidth(mTile.zoomLevel);

		// account for area changes with latitude
		double latitude = MercatorProjection.pixelYToLatitude(mTile.pixelY + (Tile.TILE_SIZE >> 1),
				mTile.zoomLevel);
		mLatScaleFactor = 0.5f + 0.5f * ((float) Math.sin(Math.abs(latitude) * (Math.PI / 180)));

		mGroundResolution = (float) (Math.cos(latitude * (Math.PI / 180))
				* MercatorProjection.EARTH_CIRCUMFERENCE
				/ ((long) Tile.TILE_SIZE << mTile.zoomLevel));

		Log.d(TAG, mTile + " " + mGroundResolution);

		mTile.layers = new Layers();

		// query database, which calls renderWay and renderPOI
		// callbacks while processing map tile data.
		if (mMapDatabase.executeQuery(mTile, this) != QueryResult.SUCCESS) {

			//Log.d(TAG, "Failed loading: " + tile);
			mTile.layers.clear();
			mTile.layers = null;
			TextItem.release(mTile.labels);
			mTile.labels = null;
			// FIXME add STATE_FAILED?
			mTile.state = STATE_NONE;
			mTile = null;
			return false;
		}

		if (debug.drawTileFrames) {
			// draw tile coordinate
			mTagName = new Tag("name", mTile.toString(), false);
			mGeom = mDebugPoint;
			RenderInstruction[] ri;
			ri = renderTheme.matchNode(debugTagWay, (byte) 0);
			renderNode(ri, debugTagWay);

			// draw tile box
			mWay = mDebugWay;
			mWay.geom.index[0] = (short) (MapView.enableClosePolygons ? 8 : 10);
			mDrawingLayer = 100 * renderLevels;
			ri = renderTheme.matchWay(mDebugWay.tags, (byte) 0, false);
			renderWay(ri, mDebugWay.tags);
		}

		mTile = null;
		return true;
	}

	Tag[] mFilterTags = new Tag[1];

	private static int getValidLayer(int layer) {
		if (layer < 0) {
			return 0;
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
	 *            the zoom level for which the scale stroke factor should be
	 *            set.
	 */
	private void setScaleStrokeWidth(byte zoomLevel) {
		mStrokeScale = (float) Math.pow(STROKE_INCREASE, zoomLevel - STROKE_MIN_ZOOM_LEVEL);
		if (mStrokeScale < 1)
			mStrokeScale = 1;
	}

	public void setMapDatabase(IMapDatabase mapDatabase) {
		if (mMapDatabase != null)
			mMapDatabase.close();

		mMapDatabase = mapDatabase;
	}

	public IMapDatabase getMapDatabase() {
		return mMapDatabase;
	}

	private boolean mRenderBuildingModel;

	// Replace tags that should only be matched by key in RenderTheme
	// to avoid caching RenderInstructions for each way of the same type
	// only with different name.
	// Maybe this should be done within RenderTheme, also allowing
	// to set these replacement rules in theme file.
	private boolean filterTags(Tag[] tags) {
		mRenderBuildingModel = false;

		for (int i = 0; i < tags.length; i++) {
			String key = tags[i].key;
			if (tags[i].key == Tag.TAG_KEY_NAME) {
				if (tags[i].value != null) {
					mTagName = tags[i];
					tags[i] = mTagEmptyName;
				}
			} else if (tags[i].key == Tag.TAG_KEY_HOUSE_NUMBER) {
				if (tags[i].value != null) {
					mTagHouseNr = tags[i];
					tags[i] = mTagEmptyHouseNr;
				}
			} else if (mTile.zoomLevel >= 17 &&
					// FIXME, allow overlays to intercept
					// this, or use a theme option for this
					key == Tag.TAG_KEY_BUILDING) {
				mRenderBuildingModel = true;
			}
		}
		return true;
	}

	// ---------------- MapDatabaseCallback -----------------

	@Override
	public void renderPOI(byte layer, Tag[] tags, GeometryBuffer geom) {
		clearState();

		// remove tags that should not be cached in Rendertheme
		filterTags(tags);

		// get render instructions
		RenderInstruction[] ri = renderTheme.matchNode(tags, mTile.zoomLevel);

		if (ri == null)
			return;

		mGeom = geom;
		renderNode(ri, tags);
	}

	@Override
	public void renderWay(WayData way) {
		mWay = way;

		clearState();

		// replace tags that should not be cached in Rendertheme (e.g. name)
		if (!filterTags(way.tags))
			return;

		mDrawingLayer = getValidLayer(way.layer) * renderLevels;

		// get render instructions
		RenderInstruction[] ri = renderTheme.matchWay(way.tags,
				(byte) (mTile.zoomLevel + 0), way.closed);

		renderWay(ri, way.tags);

		if (debug.debugTheme && ri == null)
			debugUnmatched(way.closed, way.tags);

		mCurLineLayer = null;
		mWay = null;
	}

	private void debugUnmatched(boolean closed, Tag[] tags) {
		Log.d(TAG, "DBG way not matched: " + closed + " "
				+ Arrays.deepToString(tags));

		mTagName = new Tag("name", tags[0].key + ":"
				+ tags[0].value, false);

		RenderInstruction[] ri;
		ri = renderTheme.matchWay(closed ? debugTagArea : debugTagWay,
				(byte) 0, true);

		renderWay(ri, tags);
	}

	private void renderWay(RenderInstruction[] ri, Tag[] tags) {
		if (ri == null)
			return;

		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderWay(this, tags);
	}

	private void renderNode(RenderInstruction[] ri, Tag[] tags) {
		if (ri == null)
			return;

		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderNode(this, tags);
	}

	private void clearState() {
		mTagName = null;
		mTagHouseNr = null;
		mCurLineLayer = null;
	}

	@Override
	public void renderWaterBackground() {
	}

	// ----------------- RenderThemeCallback -----------------
	@Override
	public void renderWay(Line line, int level) {
		int numLayer = (mDrawingLayer * 2) + level;

		if (line.stipple == 0) {
			if (line.outline && mCurLineLayer == null) {
				Log.e(TAG, "BUG in theme: line must come before outline!");
				return;
			}

			LineLayer lineLayer = (LineLayer)
					mTile.layers.getLayer(numLayer, Layer.LINE);

			if (lineLayer == null)
				return;

			if (lineLayer.line == null) {
				lineLayer.line = line;

				float w = line.width;
				if (!line.fixed) {
					w *= mStrokeScale;
					w *= mLatScaleFactor;
				}
				lineLayer.width = w;
			}

			if (line.outline) {
				lineLayer.addOutline(mCurLineLayer);
				return;
			}

			lineLayer.addLine(mWay.geom.points, mWay.geom.index, mWay.closed);

			// keep reference for outline layer
			mCurLineLayer = lineLayer;

		} else {
			LineTexLayer lineLayer = (LineTexLayer)
					mTile.layers.getLayer(numLayer, Layer.TEXLINE);

			if (lineLayer == null)
				return;

			if (lineLayer.line == null) {
				lineLayer.line = line;

				float w = line.width;
				if (!line.fixed) {
					w *= mStrokeScale;
					w *= mLatScaleFactor;
				}
				lineLayer.width = w;
			}

			lineLayer.addLine(mWay.geom.points, mWay.geom.index);
		}
	}

	@Override
	public void renderArea(Area area, int level) {
		int numLayer = mDrawingLayer + level;

		if (mRenderBuildingModel) {
			//Log.d(TAG, "add buildings: " + mTile + " " + mPriority);
			if (mTile.layers.extrusionLayers == null)
				mTile.layers.extrusionLayers = new ExtrusionLayer(0, mGroundResolution);

			((ExtrusionLayer) mTile.layers.extrusionLayers).addBuildings(mWay);

			return;
		}

		if (debug.disablePolygons)
			return;

		PolygonLayer layer = (PolygonLayer)
				mTile.layers.getLayer(numLayer, Layer.POLYGON);

		if (layer == null)
			return;

		if (layer.area == null)
			layer.area = area;

		layer.addPolygon(mWay.geom.points, mWay.geom.index);
	}

	private String textValueForKey(Text text) {
		String value = null;

		if (text.textKey == Tag.TAG_KEY_NAME) {
			if (mTagName != null)
				value = mTagName.value;
		} else if (text.textKey == Tag.TAG_KEY_HOUSE_NUMBER) {
			if (mTagHouseNr != null)
				value = mTagHouseNr.value;
		}
		return value;
	}

	@Override
	public void renderAreaCaption(Text text) {
		// TODO place somewhere on polygon

		String value = textValueForKey(text);
		if (value == null)
			return;

		float x = mWay.geom.points[0];
		float y = mWay.geom.points[1];

		mTile.addLabel(TextItem.get().set(x, y, value, text));
	}

	@Override
	public void renderPointOfInterestCaption(Text text) {
		String value = textValueForKey(text);
		if (value == null)
			return;

		for (int i = 0, n = mGeom.index[0]; i < n; i += 2) {
			float x = mGeom.points[i];
			float y = mGeom.points[i + 1];
			mTile.addLabel(TextItem.get().set(x, y, value, text));
		}
	}

	@Override
	public void renderWayText(Text text) {
		String value = textValueForKey(text);
		if (value == null)
			return;

		int offset = 0;
		for (int i = 0, n = mWay.geom.index.length; i < n; i++) {
			int length = mWay.geom.index[i];
			if (length < 4)
				break;

			WayDecorator.renderText(mClipper, mWay.geom.points, value, text,
					offset, length, mTile);
			offset += length;
		}
	}

	@Override
	public void renderPointOfInterestCircle(float radius, Paint fill, int level) {
	}

	@Override
	public void renderPointOfInterestSymbol(Bitmap bitmap) {
		// Log.d(TAG, "add symbol");

		//		if (mLayers.textureLayers == null)
		//			mLayers.textureLayers = new SymbolLayer();
		//
		//		SymbolLayer sl = (SymbolLayer) mLayers.textureLayers;
		//
		//		SymbolItem it = SymbolItem.get();
		//		it.x = mPoiX;
		//		it.y = mPoiY;
		//		it.bitmap = bitmap;
		//		it.billboard = true;
		//
		//		sl.addSymbol(it);
	}

	@Override
	public void renderAreaSymbol(Bitmap symbol) {
	}

	@Override
	public void renderWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat) {

	}
}
