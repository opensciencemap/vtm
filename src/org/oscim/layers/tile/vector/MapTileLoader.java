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
package org.oscim.layers.tile.vector;

import static org.oscim.layers.tile.MapTile.STATE_NONE;

import java.util.Arrays;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.IMapDatabase.QueryResult;
import org.oscim.database.IMapDatabaseCallback;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.renderer.sublayers.ExtrusionLayer;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.renderer.sublayers.LineLayer;
import org.oscim.renderer.sublayers.LineTexLayer;
import org.oscim.renderer.sublayers.PolygonLayer;
import org.oscim.renderer.sublayers.TextItem;
import org.oscim.theme.IRenderCallback;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Circle;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.LineSymbol;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.renderinstruction.Symbol;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.utils.LineClipper;
import org.oscim.view.DebugSettings;

import android.util.Log;

/**
 * @note
 *       1. The MapWorkers call MapTileLoader.execute() to load a tile.
 *       2. The tile data will be loaded from current MapDatabase
 *       3. MapDatabase calls the IMapDatabaseCallback functions
 *       implemented by MapTileLoader for WAY and POI items.
 *       4. these callbacks then call RenderTheme to get the matching style.
 *       5. RenderTheme calls IRenderCallback functions with style information
 *       6. Styled items become added to MapTile.layers... roughly
 */
public class MapTileLoader extends TileLoader implements IRenderCallback, IMapDatabaseCallback  {

	private static final String TAG = MapTileLoader.class.getName();

	private static final double STROKE_INCREASE = Math.sqrt(2.5);
	private static final byte LAYERS = 11;

	public static final byte STROKE_MIN_ZOOM_LEVEL = 12;
	public static final byte STROKE_MAX_ZOOM_LEVEL = 17;

//	private static final Tag[] debugTagWay = { new Tag("debug", "way") };
//	private static final Tag[] debugTagArea = { new Tag("debug", "area") };

	// replacement for variable value tags that should not be matched by RenderTheme
	// FIXME make this general, maybe subclass tags
	private static final Tag mTagEmptyName = new Tag(Tag.TAG_KEY_NAME, null, false);
	private static final Tag mTagEmptyHouseNr = new Tag(Tag.TAG_KEY_HOUSE_NUMBER, null, false);

//	private final MapElement mDebugWay, mDebugPoint;

	private static DebugSettings debug;

	private IRenderTheme renderTheme;
	private int renderLevels;

	// current MapDatabase used by this MapTileLoader
	private IMapDatabase mMapDatabase;

	// currently processed tile
	private MapTile mTile;

	// currently processed MapElement
	private MapElement mElement;

	// current line layer (will be used for following outline layers)
	private LineLayer mCurLineLayer;

	private int mDrawingLayer;

	private float mStrokeScale = 1.0f;
	private float mLatScaleFactor;
	private float mGroundResolution;

	private Tag mTagName;
	private Tag mTagHouseNr;

	private final LineClipper mClipper;

	public void setRenderTheme(IRenderTheme theme) {
		renderTheme = theme;
		renderLevels = theme.getLevels();
	}

	public static void setDebugSettings(DebugSettings debugSettings) {
		debug = debugSettings;
	}

	/**
	 */
	public MapTileLoader(TileManager tileManager) {
		super(tileManager);

		mClipper = new LineClipper(0, 0, Tile.SIZE, Tile.SIZE, true);

//		MapElement m = mDebugWay = new MapElement();
//		m.startLine();
//		int s = Tile.SIZE;
//		m.addPoint(0, 0);
//		m.addPoint(0, s);
//		m.addPoint(s, s);
//		m.addPoint(s, 0);
//		m.addPoint(0, 0);
//		m.tags = new Tag[] { new Tag("debug", "box") };
//		m.type = GeometryType.LINE;
//
//		m = mDebugPoint = new MapElement();
//		m.startPoints();
//		m.addPoint(s >> 1, 10);
//		m.type = GeometryType.POINT;
	}

	/* (non-Javadoc)
	 * @see org.oscim.layers.tile.TileLoader#cleanup()
	 */
	@Override
	public void cleanup() {
		mMapDatabase.close();
	}

	/* (non-Javadoc)
	 * @see org.oscim.layers.tile.TileLoader#executeJob(org.oscim.layers.tile.MapTile)
	 */
	@Override
	public boolean executeJob(MapTile mapTile) {

		if (mMapDatabase == null)
			return false;

		mTile = mapTile;

		if (mTile.layers != null) {
			// should be fixed now.
			Log.d(TAG, "BUG tile already loaded " + mTile + " " + mTile.state);
			mTile = null;
			return false;
		}

		setScaleStrokeWidth(mTile.zoomLevel);

		// account for area changes with latitude
		double latitude = MercatorProjection.toLatitude(mTile.y);

		mLatScaleFactor = 0.4f + 0.6f * ((float) Math.sin(Math.abs(latitude) * (Math.PI / 180)));

		mGroundResolution = (float) (Math.cos(latitude * (Math.PI / 180))
				* MercatorProjection.EARTH_CIRCUMFERENCE
				/ ((long) Tile.SIZE << mTile.zoomLevel));

		mTile.layers = new Layers();

		// query database, which calls renderWay and renderPOI
		// callbacks while processing map tile data.
		if (mMapDatabase.executeQuery(mTile, this) != QueryResult.SUCCESS) {

			//Log.d(TAG, "Failed loading: " + tile);
			mTile.layers.clear();
			mTile.layers = null;
			TextItem.pool.releaseAll(mTile.labels);
			mTile.labels = null;
			// FIXME add STATE_FAILED?
			// in passTile everything but STATE_LOADING is considered failed.
			mTile.state = STATE_NONE;
			mTile = null;
			return false;
		}

//		if (debug.drawTileFrames) {
//			// draw tile coordinate
//			mTagName = new Tag("name", mTile.toString(), false);
//			mElement = mDebugPoint;
//			RenderInstruction[] ri;
//			ri = renderTheme.matchNode(debugTagWay, (byte) 0);
//			renderNode(ri);
//
//			// draw tile box
//			mElement = mDebugWay;
//			mDrawingLayer = 100 * renderLevels;
//			ri = renderTheme.matchWay(mDebugWay.tags, (byte) 0, false);
//			renderWay(ri);
//		}

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
	public void renderElement(MapElement element) {
		clearState();

		mElement = element;

		if (element.type == GeometryType.POINT) {
			// remove tags that should not be cached in Rendertheme
			filterTags(element.tags);

			// get render instructions
			//RenderInstruction[] ri = renderTheme.matchNode(element.tags, mTile.zoomLevel);
			RenderInstruction[] ri = renderTheme.matchElement(element, mTile.zoomLevel);

			if (ri != null)
				renderNode(ri);
		}
		else {

			// replace tags that should not be cached in Rendertheme (e.g. name)
			if (!filterTags(element.tags))
				return;

			boolean closed = element.type == GeometryType.POLY;

			mDrawingLayer = getValidLayer(element.layer) * renderLevels;

			// get render instructions
//			RenderInstruction[] ri = renderTheme.matchWay(element.tags,
//					(byte) (mTile.zoomLevel + 0), closed);

			RenderInstruction[] ri = renderTheme.matchElement(element, mTile.zoomLevel);

			renderWay(ri);

			if (debug.debugTheme && ri == null)
				debugUnmatched(closed, element.tags);

			mCurLineLayer = null;
		}

		mElement = null;
	}

	private void debugUnmatched(boolean closed, Tag[] tags) {
		Log.d(TAG, "DBG way not matched: " + closed + " "
				+ Arrays.deepToString(tags));
		mElement = null;
//		mTagName = new Tag("name", tags[0].key + ":"
//				+ tags[0].value, false);
//
//		RenderInstruction[] ri;
//		ri = renderTheme.matchWay(closed ? debugTagArea : debugTagWay,
//				(byte) 0, true);
//
//		renderWay(ri);
	}

	private void renderWay(RenderInstruction[] ri) {
		if (ri == null)
			return;

		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderWay(this);
	}

	private void renderNode(RenderInstruction[] ri) {
		if (ri == null)
			return;

		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderNode(this);
	}

	private void clearState() {
		mTagName = null;
		mTagHouseNr = null;
		mCurLineLayer = null;
	}

	//	@Override
	//	public void renderWaterBackground() {
	//	}

	// ----------------- RenderThemeCallback -----------------
	@Override
	public void renderWay(Line line, int level) {
		int numLayer = (mDrawingLayer * 2) + level;

		if (line.stipple == 0) {
			if (line.outline && mCurLineLayer == null) {
				Log.e(TAG, "BUG in theme: line must come before outline!");
				return;
			}

			LineLayer lineLayer = mTile.layers.getLineLayer(numLayer);

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

			lineLayer.addLine(mElement.points, mElement.index,
					mElement.type == GeometryType.POLY);

			// keep reference for outline layer
			mCurLineLayer = lineLayer;

		} else {
			LineTexLayer lineLayer = mTile.layers.getLineTexLayer(numLayer);

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

			lineLayer.addLine(mElement.points, mElement.index);
		}
	}

	@Override
	public void renderArea(Area area, int level) {
		int numLayer = mDrawingLayer + level;

		if (mRenderBuildingModel) {
			//Log.d(TAG, "add buildings: " + mTile + " " + mPriority);
			if (mTile.layers.extrusionLayers == null)
				mTile.layers.extrusionLayers = new ExtrusionLayer(0, mGroundResolution);

			((ExtrusionLayer) mTile.layers.extrusionLayers).addBuildings(mElement);

			return;
		}

		if (debug.disablePolygons)
			return;

		PolygonLayer layer = mTile.layers.getPolygonLayer(numLayer);

		if (layer == null)
			return;

		if (layer.area == null)
			layer.area = area;

		layer.addPolygon(mElement.points, mElement.index);
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

		float x = mElement.points[0];
		float y = mElement.points[1];

		mTile.addLabel(TextItem.pool.get().set(x, y, value, text));
	}

	@Override
	public void renderPointOfInterestCaption(Text text) {
		String value = textValueForKey(text);
		if (value == null)
			return;

		for (int i = 0, n = mElement.index[0]; i < n; i += 2) {
			float x = mElement.points[i];
			float y = mElement.points[i + 1];
			mTile.addLabel(TextItem.pool.get().set(x, y, value, text));
		}
	}

	@Override
	public void renderWayText(Text text) {
		String value = textValueForKey(text);
		if (value == null)
			return;

		int offset = 0;
		for (int i = 0, n = mElement.index.length; i < n; i++) {
			int length = mElement.index[i];
			if (length < 4)
				break;

			WayDecorator.renderText(mClipper, mElement.points, value, text,
					offset, length, mTile);
			offset += length;
		}
	}

	@Override
	public void renderPointOfInterestCircle(Circle circle, int level) {
	}

	@Override
	public void renderPointOfInterestSymbol(Symbol symbol) {
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
	public void renderAreaSymbol(Symbol symbol) {
	}

	@Override
	public void renderWaySymbol(LineSymbol symbol) {

	}
}
