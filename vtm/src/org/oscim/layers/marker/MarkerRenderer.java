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

package org.oscim.layers.marker;

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.SymbolLayer;
import org.oscim.utils.GeometryUtils;
import org.oscim.utils.pool.Inlist;

//TODO
//- need to sort items back to front for rendering
//- and to make this work for multiple overlays
//a global scenegraph is probably required.

public class MarkerRenderer extends ElementRenderer {

	protected final MarkerSymbol mDefaultMarker;

	private final SymbolLayer mSymbolLayer;
	private final float[] mBox = new float[8];
	private final MarkerLayer<MarkerItem> mMarkerLayer;
	/** increase view to show items that are partially visible */
	protected int mExtents = 100;
	private boolean mUpdate;
	private Map mMap;
	private InternalItem mItems;
	private final Point mMapPoint = new Point();

	static class InternalItem extends Inlist<InternalItem> {
		MarkerItem item;
		boolean visible;
		boolean changes;
		float x, y;
		double px, py;
	}

	public MarkerRenderer(MarkerLayer<MarkerItem> markerLayer, MarkerSymbol defaultSymbol) {
		mSymbolLayer = new SymbolLayer();
		mMarkerLayer = markerLayer;
		mDefaultMarker = defaultSymbol;
	}

	@Override
	public synchronized void update(MapPosition pos, boolean changed, Matrices m) {

		if (!changed && !mUpdate)
			return;

		mUpdate = false;

		double mx = pos.x;
		double my = pos.y;
		double scale = Tile.SIZE * pos.scale;

		int changesInvisible = 0;
		int changedVisible = 0;
		int numVisible = 0;

		mMarkerLayer.map().getViewport().getMapExtents(mBox, mExtents);

		long flip = (long) (Tile.SIZE * pos.scale) >> 1;

		if (mItems == null) {
			if (layers.getTextureLayers() != null) {
				layers.clear();
				compile();
			}
			return;
		}

		/* check visibility */
		for (InternalItem it = mItems; it != null; it = it.next) {
			it.changes = false;
			it.x = (float) ((it.px - mx) * scale);
			it.y = (float) ((it.py - my) * scale);

			if (it.x > flip)
				it.x -= (flip << 1);
			else if (it.x < -flip)
				it.x += (flip << 1);

			if (!GeometryUtils.pointInPoly(it.x, it.y, mBox, 8, 0)) {
				if (it.visible) {
					it.changes = true;
					changesInvisible++;
				}
				continue;
			}
			if (!it.visible) {
				it.visible = true;
				changedVisible++;
			}
			numVisible++;
		}

		//log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

		/* only update when zoomlevel changed, new items are visible
		 * or more than 10 of the current items became invisible */
		if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
			return;

		/* keep position for current state */
		mMapPosition.copy(pos);

		layers.clear();

		for (InternalItem it = mItems; it != null; it = it.next) {
			if (!it.visible)
				continue;

			if (it.changes) {
				it.visible = false;
				continue;
			}

			MarkerSymbol marker = it.item.getMarker();
			if (marker == null)
				marker = mDefaultMarker;

			SymbolItem s = SymbolItem.pool.get();
			s.bitmap = marker.getBitmap();

			s.x = it.x;
			s.y = it.y;
			s.offset = marker.getHotspot();
			s.billboard = true;

			mSymbolLayer.addSymbol(s);
		}

		mSymbolLayer.prepare();
		layers.setTextureLayers(mSymbolLayer);

		compile();
	}

	@Override
	public synchronized void compile() {
		super.compile();
	}

	protected synchronized void populate(int size) {

		InternalItem pool = mItems;
		mItems = null;

		for (int i = 0; i < size; i++) {
			InternalItem it;
			if (pool != null) {
				it = pool;
				it.visible = false;
				it.changes = false;
				pool = pool.next;
			} else {
				it = new InternalItem();
			}
			it.next = mItems;
			mItems = it;

			it.item = mMarkerLayer.createItem(i);

			/* pre-project points */
			MercatorProjection.project(it.item.getPoint(), mMapPoint);
			it.px = mMapPoint.x;
			it.py = mMapPoint.y;
		}
		mUpdate = true;
	}

	//	/**
	//	 * Returns the Item at the given index.
	//	 * 
	//	 * @param position
	//	 *            the position of the item to return
	//	 * @return the Item of the given index.
	//	 */
	//	public final Item getItem(int position) {
	//
	//		synchronized (lock) {
	//			InternalItem item = mItems;
	//			for (int i = mSize - position - 1; i > 0 && item != null; i--)
	//				item = item.next;
	//
	//			if (item != null)
	//				return item.item;
	//
	//			return null;
	//		}
	//	}
}
