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
package org.oscim.layers.tile.vector;

import org.oscim.core.MapElement;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tag;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.vector.VectorTileLayer.TileLoaderHook;
import org.oscim.map.Map;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.utils.FastMath;

public class BuildingLayer extends Layer implements TileLoaderHook {
	//static final Logger log = LoggerFactory.getLogger(BuildingOverlay.class);

	final ExtrusionRenderer mExtLayer;

	public BuildingLayer(Map map, VectorTileLayer tileLayer) {
		super(map);
		tileLayer.addHook(this);

		mExtLayer = new ExtrusionRenderer(tileLayer.tileRenderer()) {
			private long mStartTime;

			@Override
			public void update(GLViewport v) {

				boolean show = v.pos.scale >= (1 << MIN_ZOOM);

				if (show) {
					if (mAlpha < 1) {
						long now = System.currentTimeMillis();

						if (mStartTime == 0) {
							mStartTime = now;
						}
						float a = (now - mStartTime) / mFadeTime;
						mAlpha = FastMath.clamp(a, 0, 1);
						mMap.render();
					} else
						mStartTime = 0;
				} else {
					if (mAlpha > 0) {
						long now = System.currentTimeMillis();
						if (mStartTime == 0) {
							mStartTime = now + 100;
						}
						long diff = (now - mStartTime);
						if (diff > 0) {
							float a = 1 - diff / mFadeTime;
							mAlpha = FastMath.clamp(a, 0, 1);
						}
						mMap.render();
					} else
						mStartTime = 0;
				}
				//log.debug(show + " > " + mAlpha);
				super.update(v);
			}
		};

		//mExtLayer.setColors(Color.LTGRAY, Color.GRAY, Color.DKGRAY);
		mRenderer = mExtLayer;
	}

	//private int multi;

	private final float mFadeTime = 500;

	private final static int MIN_ZOOM = 17;

	@Override
	public void render(MapTile tile, ElementLayers layers, MapElement element,
	        RenderStyle style, int level) {

		if (!(style instanceof ExtrusionStyle))
			return;

		ExtrusionStyle extrusion = (ExtrusionStyle) style;

		int height = 0;
		int minHeight = 0;

		String v = element.tags.getValue(Tag.KEY_HEIGHT);
		if (v != null)
			height = Integer.parseInt(v);
		v = element.tags.getValue(Tag.KEY_MIN_HEIGHT);
		if (v != null)
			minHeight = Integer.parseInt(v);

		ExtrusionLayer l = layers.getExtrusionLayers();

		if (l == null) {
			double lat = MercatorProjection.toLatitude(tile.y);
			float groundScale = (float) MercatorProjection
			    .groundResolution(lat, 1 << tile.zoomLevel);

			l = new ExtrusionLayer(0, groundScale, extrusion.colors);
			layers.setExtrusionLayers(l);
		}

		/* 12m default */
		if (height == 0)
			height = 12 * 100;

		l.add(element, height, minHeight);
	}

	//@Override
	//public boolean onTouchEvent(MotionEvent e) {
	//	int action = e.getAction() & MotionEvent.ACTION_MASK;
	//	if (action == MotionEvent.ACTION_POINTER_DOWN) {
	//		multi++;
	//	} else if (action == MotionEvent.ACTION_POINTER_UP) {
	//		multi--;
	//		if (!mActive && mAlpha > 0) {
	//			// finish hiding
	//			//log.debug("add multi hide timer " + mAlpha);
	//			addShowTimer(mFadeTime * mAlpha, false);
	//		}
	//	} else if (action == MotionEvent.ACTION_CANCEL) {
	//		multi = 0;
	//		log.debug("cancel " + multi);
	//		if (mTimer != null) {
	//			mTimer.cancel();
	//			mTimer = null;
	//		}
	//	}
	//
	//	return false;
	//}

}
