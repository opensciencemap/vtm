/*
 * Copyright 2012-2014 Hannes Janetzek
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

import static org.oscim.layers.tile.MapTile.State.CANCEL;

import org.oscim.core.GeometryBuffer.GeometryType;
import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.core.TagSet;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLoader;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.CircleStyle;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.theme.styles.SymbolStyle;
import org.oscim.theme.styles.TextStyle;
import org.oscim.tiling.ITileDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VectorTileLoader extends TileLoader implements IRenderTheme.Callback {

	static final Logger log = LoggerFactory.getLogger(VectorTileLoader.class);

	protected static final double STROKE_INCREASE = Math.sqrt(2.5);
	protected static final byte LAYERS = 11;

	public static final byte STROKE_MIN_ZOOM = 12;
	public static final byte STROKE_MAX_ZOOM = 17;

	protected IRenderTheme renderTheme;

	/** current TileDataSource used by this MapTileLoader */
	protected ITileDataSource mTileDataSource;

	/** currently processed MapElement */
	protected MapElement mElement;

	/** current line layer (will be used for outline layers) */
	protected LineLayer mCurLineLayer;

	/** Current layer for adding elements */
	protected int mCurLayer;

	/** Line-scale-factor depending on zoom and latitude */
	protected float mLineScale = 1.0f;

	protected ElementLayers mLayers;

	private final VectorTileLayer mTileLayer;

	public VectorTileLoader(VectorTileLayer tileLayer) {
		super(tileLayer.getManager());
		mTileLayer = tileLayer;
	}

	@Override
	public void cleanup() {
		if (mTileDataSource != null)
			mTileDataSource.destroy();
	}

	@Override
	public boolean loadTile(MapTile tile) {

		if (mTileDataSource == null) {
			log.error("no tile source is set");
			return false;
		}
		renderTheme = mTileLayer.getTheme();
		if (renderTheme == null) {
			log.error("no theme is set");
			return false;
		}

		//mTileLayer.getLoaderHooks();

		/* account for area changes with latitude */
		double lat = MercatorProjection.toLatitude(tile.y);
		mLineScale = (float) Math.pow(STROKE_INCREASE, tile.zoomLevel - STROKE_MIN_ZOOM);
		if (mLineScale < 1)
			mLineScale = 1;

		/* scale line width relative to latitude + PI * thumb */
		mLineScale *= 0.4f + 0.6f * ((float) Math.sin(Math.abs(lat) * (Math.PI / 180)));
		mLayers = new ElementLayers();
		tile.data = mLayers;

		try {
			/* query data source, which calls process() callback */
			mTileDataSource.query(tile, this);
		} catch (NullPointerException e) {
			log.debug("NPE {} {}", tile, e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			log.debug("{} {}", tile, e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void completed(QueryResult result) {
		mTileLayer.callHooksComplete(mTile, result == QueryResult.SUCCESS);

		super.completed(result);
		clearState();
	}

	protected static int getValidLayer(int layer) {
		if (layer < 0) {
			return 0;
		} else if (layer >= LAYERS) {
			return LAYERS - 1;
		} else {
			return layer;
		}
	}

	public void setDataSource(ITileDataSource dataSource) {
		cleanup();
		mTileDataSource = dataSource;
	}

	static class TagReplacement {
		public TagReplacement(String key) {
			this.key = key;
			this.tag = new Tag(key, null);
		}

		String key;
		Tag tag;
	}

	/**
	 * Override this method to change tags that should be passed
	 * to {@link RenderTheme} matching.
	 * E.g. to replace tags that should not be cached in Rendertheme
	 */
	protected TagSet filterTags(TagSet tagSet) {
		return tagSet;
	}

	@Override
	public void process(MapElement element) {
		if (isCanceled() || mTile.state(CANCEL))
			return;

		if (mTileLayer.callProcessHooks(mTile, mLayers, element))
			return;

		TagSet tags = filterTags(element.tags);
		if (tags == null)
			return;

		mElement = element;

		/* get and apply render instructions */
		if (element.type == GeometryType.POINT) {
			renderNode(renderTheme.matchElement(element.type, tags, mTile.zoomLevel));
		} else {
			mCurLayer = getValidLayer(element.layer) * renderTheme.getLevels();
			renderWay(renderTheme.matchElement(element.type, tags, mTile.zoomLevel));
		}
		clearState();
	}

	//	private final static LineStyle DEBUG_LINE =
	//	        new LineStyle(Integer.MAX_VALUE / 12, Color.MAGENTA, 1.2f);
	//
	//	private final static TextStyle DEBUG_TEXT = new TextBuilder()
	//	    .setFontSize(12)
	//	    .setColor(Color.RED)
	//	    .setCaption(true)
	//	    .setTextKey(Tag.KEY_NAME)
	//	    .build();

	protected void renderWay(RenderStyle[] style) {
		if (style == null) {
			//	DEBUG_LINE.renderWay(this);
			//	String t = mElement.tags.toString();
			//	mElement.tags.clear();
			//	mElement.tags.add(new Tag(Tag.KEY_NAME, t));
			//	DEBUG_TEXT.renderWay(this);
			return;
		}
		for (int i = 0, n = style.length; i < n; i++)
			style[i].renderWay(this);
	}

	protected void renderNode(RenderStyle[] style) {
		if (style == null)
			return;

		for (int i = 0, n = style.length; i < n; i++)
			style[i].renderNode(this);
	}

	protected void clearState() {
		mCurLineLayer = null;
		mElement = null;
	}

	/*** RenderThemeCallback ***/
	@Override
	public void renderWay(LineStyle line, int level) {
		int numLayer = mCurLayer + level;

		if (line.stipple == 0) {
			if (line.outline && mCurLineLayer == null) {
				log.debug("missing line for outline! " + mElement.tags
				        + " lvl:" + level + " layer:" + mElement.layer);
				return;
			}

			LineLayer ll = mLayers.getLineLayer(numLayer);

			if (ll.line == null) {
				ll.line = line;
				ll.scale = line.fixed ? 1 : mLineScale;
				ll.setExtents(-4, Tile.SIZE + 4);
			}

			if (line.outline) {
				ll.addOutline(mCurLineLayer);
				return;
			}

			ll.addLine(mElement);

			/* keep reference for outline layer(s) */
			mCurLineLayer = ll;

		} else {
			LineTexLayer ll = mLayers.getLineTexLayer(numLayer);

			if (ll.line == null) {
				ll.line = line;

				float w = line.width;
				if (!line.fixed)
					w *= mLineScale;

				ll.width = w;
			}

			ll.addLine(mElement);
		}
	}

	/* slower to load (requires tesselation) and uses
	 * more memory but should be faster to render */
	protected final static boolean USE_MESH_POLY = false;

	@Override
	public void renderArea(AreaStyle area, int level) {
		int numLayer = mCurLayer + level;
		if (USE_MESH_POLY) {
			MeshLayer l = mLayers.getMeshLayer(numLayer);
			l.area = area;
			l.addMesh(mElement);
		} else {
			PolygonLayer l = mLayers.getPolygonLayer(numLayer);
			l.area = area;
			l.addPolygon(mElement.points, mElement.index);
		}
	}

	@Override
	public void renderSymbol(SymbolStyle symbol) {
		mTileLayer.callThemeHooks(mTile, mLayers, mElement, symbol, 0);
	}

	@Override
	public void renderExtrusion(ExtrusionStyle extrusion, int level) {
		mTileLayer.callThemeHooks(mTile, mLayers, mElement, extrusion, level);
	}

	@Override
	public void renderCircle(CircleStyle circle, int level) {
	}

	@Override
	public void renderText(TextStyle text) {
		mTileLayer.callThemeHooks(mTile, mLayers, mElement, text, 0);
	}
}
