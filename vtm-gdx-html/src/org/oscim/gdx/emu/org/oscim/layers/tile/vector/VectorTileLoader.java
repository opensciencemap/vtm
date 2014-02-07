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

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.PointF;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.TextItem;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Circle;
import org.oscim.theme.styles.Extrusion;
import org.oscim.theme.styles.Line;
import org.oscim.theme.styles.LineSymbol;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.Symbol;
import org.oscim.theme.styles.Text;
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

		if (mTileDataSource == null)
			return false;

		// account for area changes with latitude
		double lat = MercatorProjection.toLatitude(tile.y);

		mLineScale = (float) Math.pow(STROKE_INCREASE, tile.zoomLevel - STROKE_MIN_ZOOM);
		if (mLineScale < 1)
			mLineScale = 1;

		// scale line width relative to latitude + PI * thumb
		mLineScale *= 0.4f + 0.6f * ((float) Math.sin(Math.abs(lat) * (Math.PI / 180)));

		mTile = tile;
		mTile.layers = new ElementLayers();

		// query database, which calls renderWay and renderPOI
		// callbacks while processing map tile data.
		if (mTileDataSource.executeQuery(mTile, this) != QueryResult.SUCCESS) {
			return false;
		}

		return true;
	}

	public void completed(boolean success) {
		if (success) {
			mTile.loader.jobCompleted(mTile, true);
			mTile = null;
			return;
		}
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
			//if (debug.debugTheme && ri == null)
			//	debugUnmatched(closed, element.tags);

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

	private void renderWay(RenderStyle[] ri) {
		if (ri == null)
			return;

		for (int i = 0, n = ri.length; i < n; i++)
			ri[i].renderWay(this);
	}

	private void renderNode(RenderStyle[] ri) {
		if (ri == null)
			return;

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
				log.error("BUG in theme: line must come before outline!");
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

	private final static boolean USE_MESH_POLY = false;

	@Override
	public void renderArea(Area area, int level) {
		int numLayer = mCurLayer + level;
		if (USE_MESH_POLY) {
			MeshLayer l = mTile.layers.getMeshLayer(numLayer);
			l.area = area;
			l.addMesh(mElement);
		} else {
			PolygonLayer l = mTile.layers.getPolygonLayer(numLayer);
			l.area = area;
			l.addPolygon(mElement.points, mElement.index);
		}
	}

	@Override
	public void renderAreaText(Text text) {
		// TODO place somewhere on polygon
		String value = mElement.tags.getValue(text.textKey);
		if (value == null)
			return;

		PointF p = mElement.getPoint(0);
		mTile.labels.push(TextItem.pool.get().set(p.x, p.y, value, text));
	}

	@Override
	public void renderPointText(Text text) {
		String value = mElement.tags.getValue(text.textKey);
		if (value == null)
			return;

		for (int i = 0, n = mElement.getNumPoints(); i < n; i++) {
			PointF p = mElement.getPoint(i);
			mTile.labels.push(TextItem.pool.get().set(p.x, p.y, value, text));
		}
	}

	@Override
	public void renderWayText(Text text) {
		String value = mElement.tags.getValue(text.textKey);
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
			log.debug("missing symbol for " + mElement.tags.toString());
			return;
		}
		for (int i = 0, n = mElement.getNumPoints(); i < n; i++) {
			PointF p = mElement.getPoint(i);

			SymbolItem it = SymbolItem.pool.get();
			it.set(p.x, p.y, symbol.texture, true);
			mTile.symbols.push(it);
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

		ExtrusionLayer l = mTile.layers.getExtrusionLayers();

		if (l == null) {
			double lat = MercatorProjection.toLatitude(mTile.y);
			float groundScale = (float) (Math.cos(lat * (Math.PI / 180))
			        * MercatorProjection.EARTH_CIRCUMFERENCE
			        / ((long) Tile.SIZE << mTile.zoomLevel));

			l = new ExtrusionLayer(0, groundScale, extrusion.colors);
			mTile.layers.setExtrusionLayers(l);
		}
		l.add(mElement, height, minHeight);
	}

	@Override
	public void setTileImage(Bitmap bitmap) {
		// TODO Auto-generated method stub

	}
}
