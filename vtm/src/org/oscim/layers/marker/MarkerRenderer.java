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

import java.util.Comparator;

import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.elements.SymbolItem;
import org.oscim.renderer.elements.SymbolLayer;
import org.oscim.utils.TimSort;
import org.oscim.utils.geom.GeometryUtils;

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

	private InternalItem[] mItems;

	private final Point mMapPoint = new Point();

	static class InternalItem {
		MarkerItem item;
		boolean visible;
		boolean changes;
		float x, y;
		double px, py;

		float dy;

		@Override
		public String toString() {
			return "\n" + x + ":" + y + " / " + dy + " " + visible;
		}
	}

	public MarkerRenderer(MarkerLayer<MarkerItem> markerLayer, MarkerSymbol defaultSymbol) {
		mSymbolLayer = new SymbolLayer();
		mMarkerLayer = markerLayer;
		mDefaultMarker = defaultSymbol;
	}

	@Override
	public synchronized void update(GLViewport v) {
		if (!v.changed() && !mUpdate)
			return;

		mUpdate = false;

		double mx = v.pos.x;
		double my = v.pos.y;
		double scale = Tile.SIZE * v.pos.scale;

		//int changesInvisible = 0;
		//int changedVisible = 0;
		int numVisible = 0;

		mMarkerLayer.map().viewport().getMapExtents(mBox, mExtents);

		long flip = (long) (Tile.SIZE * v.pos.scale) >> 1;

		if (mItems == null) {
			if (layers.getTextureLayers() != null) {
				layers.clear();
				compile();
			}
			return;
		}

		double angle = Math.toRadians(v.pos.bearing);
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);

		/* check visibility */
		for (InternalItem it : mItems) {
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
					//changesInvisible++;
				}
				continue;
			}

			it.dy = sin * it.x + cos * it.y;

			if (!it.visible) {
				it.visible = true;
				//changedVisible++;
			}
			numVisible++;
		}

		//log.debug(numVisible + " " + changedVisible + " " + changesInvisible);

		/* only update when zoomlevel changed, new items are visible
		 * or more than 10 of the current items became invisible */
		//if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
		//	return;
		layers.clear();

		if (numVisible == 0) {
			compile();
			return;
		}
		/* keep position for current state */
		mMapPosition.copy(v.pos);
		mMapPosition.bearing = -mMapPosition.bearing;

		sort(mItems, 0, mItems.length);
		//log.debug(Arrays.toString(mItems));
		for (InternalItem it : mItems) {
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
			s.set(it.x, it.y, marker.getBitmap(), true);
			s.offset = marker.getHotspot();
			mSymbolLayer.pushSymbol(s);
		}

		mSymbolLayer.prepare();
		layers.setTextureLayers(mSymbolLayer);

		compile();
	}

	protected void populate(int size) {

		InternalItem[] tmp = new InternalItem[size];

		for (int i = 0; i < size; i++) {
			InternalItem it = new InternalItem();
			tmp[i] = it;
			it.item = mMarkerLayer.createItem(i);

			/* pre-project points */
			MercatorProjection.project(it.item.getPoint(), mMapPoint);
			it.px = mMapPoint.x;
			it.py = mMapPoint.y;
		}
		synchronized (this) {
			mUpdate = true;
			mItems = tmp;
		}
	}

	static TimSort<InternalItem> ZSORT = new TimSort<InternalItem>();

	public static void sort(InternalItem[] a, int lo, int hi) {
		int nRemaining = hi - lo;
		if (nRemaining < 2) {
			return;
		}

		ZSORT.doSort(a, zComparator, lo, hi);
	}

	final static Comparator<InternalItem> zComparator = new Comparator<InternalItem>() {
		@Override
		public int compare(InternalItem a, InternalItem b) {
			if (a.visible && b.visible) {
				if (a.dy > b.dy) {
					return -1;
				}
				if (a.dy < b.dy) {
					return 1;
				}
			} else if (a.visible) {
				return -1;
			} else if (b.visible) {
				return 1;
			}

			return 0;
		}
	};

	public void update() {
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
