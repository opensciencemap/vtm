/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

import java.util.concurrent.CancellationException;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.PointF;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.layers.tile.vector.labeling.WayDecorator;
import org.oscim.map.Map;
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
import org.oscim.theme.renderinstruction.Extrusion;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorTileLoader extends TileLoader implements IRenderTheme.Callback, ITileDataSink {

	static final Logger log = LoggerFactory.getLogger(VectorTileLoader.class);

	private static final double STROKE_INCREASE = Math.sqrt(2.5);
	private static final byte LAYERS = 11;

	public static final byte STROKE_MIN_ZOOM = 12;
	public static final byte STROKE_MAX_ZOOM = 17;

	private IRenderTheme renderTheme;
	private int renderLevels;

	/** current TileDataSource used by this MapTileLoader */
	private ITileDataSource mTileDataSource;

	/** currently processed tile */
	private MapTile mTile;

	/** currently processed MapElement */
	private MapElement mElement;

	/** current line layer (will be used for outline layers) */
	private LineLayer mCurLineLayer;

	/** Current layer for adding elements */
	private int mCurLayer;

	/** Line-scale-factor depending on zoom and latitude */
	private float mLineScale = 1.0f;

	private final LineClipper mClipper;

	private final TagSet mFilteredTags;

	public void setRenderTheme(IRenderTheme theme) {
		renderTheme = theme;
		renderLevels = theme.getLevels();
	}

	public VectorTileLoader(TileManager tileManager) {
		super(tileManager);

		mClipper = new LineClipper(0, 0, Tile.SIZE, Tile.SIZE, true);
		mFilteredTags = new TagSet();
	}

	@Override
	public void cleanup() {
		mTileDataSource.destroy();
	}

	@Override
	public boolean executeJob(MapTile tile) {

		if (mTileDataSource == null) {
			log.debug("no tile source is set");
			return false;
		}

		if (renderTheme == null) {
			log.debug("no theme is set");
			return false;
		}

		if (Map.debugTheme)
			log.debug(tile.toString());

		// account for area changes with latitude
		double lat = MercatorProjection.toLatitude(tile.y);

		mLineScale = (float) Math.pow(STROKE_INCREASE, tile.zoomLevel - STROKE_MIN_ZOOM);
		if (mLineScale < 1)
			mLineScale = 1;

		// scale line width relative to latitude + PI * thumb
		mLineScale *= 0.4f + 0.6f * ((float) Math.sin(Math.abs(lat) * (Math.PI / 180)));

		mTile = tile;
		mTile.layers = new ElementLayers();
		QueryResult result = null;
		try {
			// query database, which calls 'process' callback
			result = mTileDataSource.executeQuery(mTile, this);
		} catch (CancellationException e) {
			log.debug("canceled {}", mTile);
		} catch (Exception e) {
			log.debug("{}", e);
		} finally {
			mTile = null;
			clearState();
		}
		return (result == QueryResult.SUCCESS);
	}

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

	static class TagReplacement {
		public TagReplacement(String key) {
			this.key = key;
			this.tag = new Tag(key, null);
		}

		String key;
		Tag tag;
	}

	// Replace tags that should only be matched by key in RenderTheme
	// to avoid caching RenderInstructions for each way of the same type
	// only with different name.
	// Maybe this should be done within RenderTheme, also allowing
	// to set these replacement rules in theme file.
	private static final TagReplacement[] mTagReplacement = {
	        new TagReplacement(Tag.KEY_NAME),
	        new TagReplacement(Tag.KEY_HOUSE_NUMBER),
	        new TagReplacement(Tag.KEY_REF),
	        new TagReplacement(Tag.KEY_HEIGHT),
	        new TagReplacement(Tag.KEY_MIN_HEIGHT)
	};

	private boolean filterTags(TagSet tagSet) {
		Tag[] tags = tagSet.tags;

		mFilteredTags.clear();

		O: for (int i = 0, n = tagSet.numTags; i < n; i++) {
			Tag t = tags[i];

			for (TagReplacement replacement : mTagReplacement) {
				if (t.key == replacement.key) {
					mFilteredTags.add(replacement.tag);
					continue O;
				}
			}

			mFilteredTags.add(t);
		}

		return true;
	}

	@Override
	public void process(MapElement element) {

		clearState();

		if (isCanceled())
			throw new CancellationException();

		mElement = element;

		if (element.type == GeometryType.POINT) {
			// remove tags that should not be cached in Rendertheme
			filterTags(element.tags);

			// get and apply render instructions
			renderNode(renderTheme.matchElement(element.type, mFilteredTags, mTile.zoomLevel));
		} else {

			// replace tags that should not be cached in Rendertheme (e.g. name)
			if (!filterTags(element.tags))
				return;

			mCurLayer = getValidLayer(element.layer) * renderLevels;

			// get and apply render instructions
			renderWay(renderTheme.matchElement(element.type, mFilteredTags, mTile.zoomLevel));

			//boolean closed = element.type == GeometryType.POLY;

			mCurLineLayer = null;
		}

		mElement = null;
	}

	//private void debugUnmatched(boolean closed, TagSet tags) {
	//		log.debug("DBG way not matched: " + closed + " "
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
		if (ri == null) {
			if (Map.debugTheme)
				log.debug("no rule for way: " + mElement.tags);
			return;
		}
		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderWay(this);
	}

	private void renderNode(RenderInstruction[] ri) {
		if (ri == null) {
			if (Map.debugTheme)
				log.debug("no rule for node: " + mElement.tags);
			return;
		}

		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderNode(this);
	}

	private void clearState() {
		mCurLineLayer = null;
	}

	/*** RenderThemeCallback ***/
	@Override
	public void renderWay(Line line, int level) {
		int numLayer = mCurLayer + level;

		if (line.stipple == 0) {
			if (line.outline && mCurLineLayer == null) {
				log.debug("missing line for outline! " + mElement.tags
				        + " lvl:" + level + " layer:" + mElement.layer);
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
		int numLayer = mCurLayer + level;

		PolygonLayer layer = mTile.layers.getPolygonLayer(numLayer);

		if (layer == null)
			return;

		layer.area = area;
		layer.addPolygon(mElement.points, mElement.index);
	}

	@Override
	public void renderAreaText(Text text) {
		// TODO place somewhere on polygon
		String value = mElement.tags.getValue(text.textKey);
		if (value == null || value.length() == 0)
			return;

		float x = 0;
		float y = 0;
		int n = mElement.index[0];

		for (int i = 0; i < n;) {
			x += mElement.points[i++];
			y += mElement.points[i++];
		}
		x /= (n / 2);
		y /= (n / 2);

		mTile.addLabel(TextItem.pool.get().set(x, y, value, text));
	}

	@Override
	public void renderPointText(Text text) {
		String value = mElement.tags.getValue(text.textKey);
		if (value == null || value.length() == 0)
			return;

		for (int i = 0, n = mElement.getNumPoints(); i < n; i++) {
			PointF p = mElement.getPoint(i);
			mTile.addLabel(TextItem.pool.get().set(p.x, p.y, value, text));
		}
	}

	@Override
	public void renderWayText(Text text) {
		String value = mElement.tags.getValue(text.textKey);
		if (value == null || value.length() == 0)
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
			if (Map.debugTheme)
				log.debug("missing symbol for " + mElement.tags.toString());
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

	@Override
	public void renderExtrusion(Extrusion extrusion, int level) {

		int height = 0;
		int minHeight = 0;

		String v = mElement.tags.getValue(Tag.KEY_HEIGHT);
		if (v != null)
			height = Integer.parseInt(v);
		v = mElement.tags.getValue(Tag.KEY_MIN_HEIGHT);
		if (v != null)
			minHeight = Integer.parseInt(v);

		ExtrusionLayer l = (ExtrusionLayer) mTile.layers.extrusionLayers;

		if (l == null) {
			double lat = MercatorProjection.toLatitude(mTile.y);
			float groundScale = (float) (Math.cos(lat * (Math.PI / 180))
			        * MercatorProjection.EARTH_CIRCUMFERENCE
			        / ((long) Tile.SIZE << mTile.zoomLevel));

			mTile.layers.extrusionLayers = l = new ExtrusionLayer(0, groundScale, extrusion.colors);
		}
		l.add(mElement, height, minHeight);
	}

	/**
	 * used for event-driven loading by html backend
	 */
	@Override
	public void completed(boolean success) {
	}

	@Override
	public void setTileImage(Bitmap bitmap) {

	}
}
