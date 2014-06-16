/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.layers.tile.buildings;

import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer.TileLoaderThemeHook;
import org.oscim.map.Map;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.OffscreenRenderer;
import org.oscim.renderer.OffscreenRenderer.Mode;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.renderer.elements.ExtrusionLayers;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.RenderStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildingLayer extends Layer implements TileLoaderThemeHook {
	static final Logger log = LoggerFactory.getLogger(BuildingLayer.class);

	private final static int MIN_ZOOM = 17;
	private final static int MAX_ZOOM = 17;
	private final static boolean POST_AA = false;

	private static final Object BUILDING_DATA = BuildingLayer.class.getName();

	private ExtrusionRenderer mExtRenderer;

	public BuildingLayer(Map map, VectorTileLayer tileLayer) {
		this(map, tileLayer, MIN_ZOOM, MAX_ZOOM);

		//		super(map);
		//		tileLayer.addHook(this);
		//
		//		mMinZoom = MIN_ZOOM;
		//
		//		OffscreenRenderer or = new OffscreenRenderer();
		//		or.setRenderer(new ExtrusionRenderer(tileLayer.tileRenderer(), MIN_ZOOM));
		//		mRenderer = or;
	}

	public BuildingLayer(Map map, VectorTileLayer tileLayer, int zoomMin, int zoomMax) {
		super(map);
		tileLayer.addHook(this);

		mExtRenderer = new BuildingRenderer(tileLayer.tileRenderer(),
		                                    zoomMin, zoomMax, false, true);

		if (POST_AA) {
			OffscreenRenderer or = new OffscreenRenderer(Mode.FXAA);
			or.setRenderer(mExtRenderer);
			mRenderer = or;
		} else {
			mRenderer = mExtRenderer;
		}
	}

	/** TileLoaderThemeHook */
	@Override
	public boolean render(MapTile tile, ElementLayers layers, MapElement element,
	        RenderStyle style, int level) {

		if (!(style instanceof ExtrusionStyle))
			return false;

		ExtrusionStyle extrusion = (ExtrusionStyle) style;

		int height = 0;
		int minHeight = 0;

		String v = element.tags.getValue(Tag.KEY_HEIGHT);
		if (v != null)
			height = Integer.parseInt(v);
		v = element.tags.getValue(Tag.KEY_MIN_HEIGHT);
		if (v != null)
			minHeight = Integer.parseInt(v);

		ExtrusionLayers el = get(tile);

		if (el.layers == null) {
			double lat = MercatorProjection.toLatitude(tile.y);
			float groundScale = (float) MercatorProjection
			    .groundResolution(lat, 1 << tile.zoomLevel);

			el.layers = new ExtrusionLayer(0, groundScale, extrusion.colors);
		}

		/* 12m default */
		if (height == 0)
			height = 12 * 100;

		el.layers.add(element, height, minHeight);

		return true;
	}

	public static ExtrusionLayers get(MapTile tile) {
		ExtrusionLayers el = (ExtrusionLayers) tile.getData(BUILDING_DATA);
		if (el == null) {
			el = new ExtrusionLayers(tile);
			tile.addData(BUILDING_DATA, el);
		}
		return el;
	}

	@Override
	public void complete(MapTile tile, boolean success) {
	}

	//	private int multi;
	//	@Override
	//	public void onInputEvent(Event event, MotionEvent e) {
	//		int action = e.getAction() & MotionEvent.ACTION_MASK;
	//		if (action == MotionEvent.ACTION_POINTER_DOWN) {
	//			multi++;
	//		} else if (action == MotionEvent.ACTION_POINTER_UP) {
	//			multi--;
	//			if (!mActive && mAlpha > 0) {
	//				// finish hiding
	//				//log.debug("add multi hide timer " + mAlpha);
	//				addShowTimer(mFadeTime * mAlpha, false);
	//			}
	//		} else if (action == MotionEvent.ACTION_CANCEL) {
	//			multi = 0;
	//			log.debug("cancel " + multi);
	//			if (mTimer != null) {
	//				mTimer.cancel();
	//				mTimer = null;
	//			}
	//		}
	//	}

}
