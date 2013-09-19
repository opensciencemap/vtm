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

import org.oscim.backend.Log;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.PointF;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.layers.tile.vector.labeling.WayDecorator;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.TextItem;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.renderinstruction.Area;
import org.oscim.theme.renderinstruction.Circle;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.theme.renderinstruction.LineSymbol;
import org.oscim.theme.renderinstruction.RenderInstruction;
import org.oscim.theme.renderinstruction.Symbol;
import org.oscim.theme.renderinstruction.Text;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileLoader;
import org.oscim.tiling.TileManager;
import org.oscim.tiling.source.ITileDataSink;
import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.ITileDataSource.QueryResult;
import org.oscim.utils.LineClipper;

public class VectorTileLoader extends TileLoader implements IRenderTheme.Callback, ITileDataSink {

	private static final String TAG = VectorTileLoader.class.getName();

	private static final double STROKE_INCREASE = Math.sqrt(2.5);
	private static final byte LAYERS = 11;

	public static final byte STROKE_MIN_ZOOM = 12;
	public static final byte STROKE_MAX_ZOOM = 17;

	//private static final Tag[] debugTagWay = { new Tag("debug", "way") };
	//private static final Tag[] debugTagArea = { new Tag("debug", "area") };

	// replacement for variable value tags that should not be matched by RenderTheme
	// FIXME make this general, maybe subclass tags
	private static final Tag mTagEmptyName = new Tag(Tag.TAG_KEY_NAME, null, false);
	private static final Tag mTagEmptyHouseNr = new Tag(Tag.TAG_KEY_HOUSE_NUMBER, null, false);

	//	private final MapElement mDebugWay, mDebugPoint;

	private IRenderTheme renderTheme;
	private int renderLevels;

	// current TileDataSource used by this MapTileLoader
	private ITileDataSource mTileDataSource;

	// currently processed tile
	private MapTile mTile;

	// currently processed MapElement
	private MapElement mElement;

	// current line layer (will be used for following outline layers)
	private LineLayer mCurLineLayer;

	private int mDrawingLayer;

	private float mLineScale = 1.0f;
	private float mGroundScale;

	private Tag mTagName;
	private Tag mTagHouseNr;

	private final LineClipper mClipper;

	public void setRenderTheme(IRenderTheme theme) {
		renderTheme = theme;
		renderLevels = theme.getLevels();
	}

	public VectorTileLoader(TileManager tileManager) {
		super(tileManager);

		mClipper = new LineClipper(0, 0, Tile.SIZE, Tile.SIZE, true);
	}

	@Override
	public void cleanup() {
		mTileDataSource.destroy();
	}

	@Override
	public boolean executeJob(MapTile tile) {

		if (mTileDataSource == null)
			return false;

		// account for area changes with latitude
		double lat = MercatorProjection.toLatitude(tile.y);

		mLineScale = (float) Math.pow(STROKE_INCREASE, tile.zoomLevel - STROKE_MIN_ZOOM);
		if (mLineScale < 1)
			mLineScale = 1;

		// scale line width relative to latitude + PI * thumb
		mLineScale *= 0.4f + 0.6f * ((float) Math.sin(Math.abs(lat) * (Math.PI / 180)));

		mGroundScale = (float) (Math.cos(lat * (Math.PI / 180))
		                        * MercatorProjection.EARTH_CIRCUMFERENCE
		                        / ((long) Tile.SIZE << tile.zoomLevel));

		mTile = tile;
		mTile.layers = new ElementLayers();

		// query database, which calls 'process' callback
		QueryResult result = mTileDataSource.executeQuery(mTile, this);

		mTile = null;

		clearState();

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

		return (result == QueryResult.SUCCESS);
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

	public void setTileDataSource(ITileDataSource mapDatabase) {
		if (mTileDataSource != null)
			mTileDataSource.destroy();

		mTileDataSource = mapDatabase;
	}

	private boolean mRenderBuildingModel;

	// Replace tags that should only be matched by key in RenderTheme
	// to avoid caching RenderInstructions for each way of the same type
	// only with different name.
	// Maybe this should be done within RenderTheme, also allowing
	// to set these replacement rules in theme file.
	private boolean filterTags(TagSet in) {
		mRenderBuildingModel = false;
		Tag[] tags = in.tags;

		for (int i = 0; i < in.numTags; i++) {
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
			} else if (mTile.zoomLevel > 16) {
				// FIXME, allow overlays to intercept
				// this, or use a theme option for this
				if (key == Tag.TAG_KEY_BUILDING)
					mRenderBuildingModel = true;

				else if (key == Tag.KEY_HEIGHT) {
					try {
						mElement.height = Integer.parseInt(tags[i].value);
					} catch (Exception e) {
					}
				}
				else if (key == Tag.KEY_MIN_HEIGHT) {
					try {
						mElement.minHeight = Integer.parseInt(tags[i].value);
					} catch (Exception e) {
					}
				}
			}
		}
		return true;
	}

	@Override
	public void process(MapElement element) {
		clearState();

		mElement = element;

		if (element.type == GeometryType.POINT) {
			// remove tags that should not be cached in Rendertheme
			filterTags(element.tags);

			// get and apply render instructions
			renderNode(renderTheme.matchElement(element, mTile.zoomLevel));
		} else {

			// replace tags that should not be cached in Rendertheme (e.g. name)
			if (!filterTags(element.tags))
				return;

			mDrawingLayer = getValidLayer(element.layer) * renderLevels;

			// get and apply render instructions
			renderWay(renderTheme.matchElement(element, mTile.zoomLevel));

			//boolean closed = element.type == GeometryType.POLY;
			//if (debug.debugTheme && ri == null)
			//	debugUnmatched(closed, element.tags);

			mCurLineLayer = null;
		}

		mElement = null;
	}

	//private void debugUnmatched(boolean closed, TagSet tags) {
	//		Log.d(TAG, "DBG way not matched: " + closed + " "
	//				+ Arrays.deepToString(tags));
	//
	//		mTagName = new Tag("name", tags[0].key + ":"
	//				+ tags[0].value, false);
	//
	//		mElement.tags = closed ? debugTagArea : debugTagWay;
	//		RenderInstruction[] ri = renderTheme.matchElement(mElement, mTile.zoomLevel);
	//		renderWay(ri);
	//}

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

	/*** RenderThemeCallback ***/
	@Override
	public void renderWay(Line line, int level) {
		int numLayer = mDrawingLayer + level;

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
				if (!line.fixed)
					w *= mLineScale;

				lineLayer.width = w;
			}

			if (line.outline) {
				lineLayer.addOutline(mCurLineLayer);
				return;
			}

			lineLayer.addLine(mElement);

			// keep reference for outline layer
			mCurLineLayer = lineLayer;

		} else {
			LineTexLayer lineLayer = mTile.layers.getLineTexLayer(numLayer);

			if (lineLayer == null)
				return;

			if (lineLayer.line == null) {
				lineLayer.line = line;

				float w = line.width;
				if (!line.fixed)
					w *= mLineScale;

				lineLayer.width = w;
			}

			lineLayer.addLine(mElement);
		}
	}

	@Override
	public void renderArea(Area area, int level) {
		int numLayer = mDrawingLayer + level;

		if (mRenderBuildingModel) {
			//Log.d(TAG, "add buildings: " + mTile + " " + mPriority);
			if (mTile.layers.extrusionLayers == null)
				mTile.layers.extrusionLayers = new ExtrusionLayer(0, mGroundScale);

			((ExtrusionLayer) mTile.layers.extrusionLayers).addBuildings(mElement);

			return;
		}

		PolygonLayer layer = mTile.layers.getPolygonLayer(numLayer);

		if (layer == null)
			return;

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
	public void renderAreaText(Text text) {
		// TODO place somewhere on polygon

		String value = textValueForKey(text);
		if (value == null)
			return;

		PointF p = mElement.getPoint(0);
		mTile.addLabel(TextItem.pool.get().set(p.x, p.y, value, text));
	}

	@Override
	public void renderPointText(Text text) {
		String value = textValueForKey(text);
		if (value == null)
			return;

		for (int i = 0, n = mElement.getNumPoints(); i < n; i++) {
			PointF p = mElement.getPoint(i);
			mTile.addLabel(TextItem.pool.get().set(p.x, p.y, value, text));
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
	public void renderPointCircle(Circle circle, int level) {
	}

	@Override
	public void renderPointSymbol(Symbol symbol) {
		if (symbol.texture == null) {
			Log.d(TAG, "missing symbol for " + mElement.tags.asString());
			return;
		}
		for (int i = 0, n = mElement.getNumPoints(); i < n; i++) {
			PointF p = mElement.getPoint(i);

			SymbolItem it = SymbolItem.pool.get();
			it.set(p.x, p.y, symbol.texture, true);
			mTile.addSymbol(it);
		}
	}

	@Override
	public void renderAreaSymbol(Symbol symbol) {
	}

	@Override
	public void renderWaySymbol(LineSymbol symbol) {

	}

	/**
	 * used for event-driven loading by html backend
	 */
	@Override
	public void completed(boolean success) {
	}
}
