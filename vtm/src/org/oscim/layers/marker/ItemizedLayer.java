/*
 * Copyright 2012 osmdroid authors
 * Copyright 2013 Hannes Janetzek
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

// TODO
// - need to sort items back to front for rendering
// - and to make this work for multiple overlays
//   a global scenegraph is probably required.

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

/* @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * @author Fred Eisele
 * @author Hannes Janetzek
 * */
/**
 * Draws a list of {@link MarkerItem} as markers to a map. The item with the
 * lowest index is drawn as last and therefore the 'topmost' marker. It also
 * gets checked for onTap first. This class is generic, because you then you get
 * your custom item-class passed back in onTap().
 * 
 * @param <Item>
 *            ...
 */
public abstract class ItemizedLayer<Item extends MarkerItem> extends MarkerLayer implements
        MarkerLayer.Snappable {

	//private final static String TAG = ItemizedOverlay.class.getName();

	protected final MarkerSymbol mDefaultMarker;
	protected boolean mDrawFocusedItem = true;

	class InternalItem {
		InternalItem next;

		Item item;
		boolean visible;
		boolean changes;
		float x, y;
		double px, py;
	}

	/* package */InternalItem mItems;
	/* package */Object lock = new Object();
	/* package */Item mFocusedItem;
	/* package */boolean mUpdate;

	private int mSize;

	class ItemOverlay extends ElementRenderer {

		private final SymbolLayer mSymbolLayer;
		private final float[] mBox = new float[8];

		public ItemOverlay() {
			mSymbolLayer = new SymbolLayer();
		}

		// note: this is called from GL-Thread. so check your syncs!
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

			mMap.getViewport().getMapViewProjection(mBox);

			synchronized (lock) {
				if (mItems == null) {
					if (layers.textureLayers != null) {
						layers.clear();
						compile();
					}
					return;
				}

				// check visibility
				for (InternalItem it = mItems; it != null; it = it.next) {
					it.x = (float) ((it.px - mx) * scale);
					it.y = (float) ((it.py - my) * scale);
					it.changes = false;

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

				//Log.d(TAG, numVisible + " " + changedVisible + " " + changesInvisible);

				// only update when zoomlevel changed, new items are visible
				// or more than 10 of the current items became invisible
				if ((numVisible == 0) && (changedVisible == 0 && changesInvisible < 10))
					return;

				// keep position for current state
				// updateMapPosition();
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

			}
			mSymbolLayer.prepare();
			layers.textureLayers = mSymbolLayer;

			compile();
		}

		@Override
		public synchronized void compile() {
			super.compile();
		}
	}

	/**
	 * Method by which subclasses create the actual Items. This will only be
	 * called from populate() we'll cache them for later use.
	 * 
	 * @param i
	 *            ...
	 * @return ...
	 */
	protected abstract Item createItem(int i);

	/**
	 * The number of items in this overlay.
	 * 
	 * @return ...
	 */
	public abstract int size();

	public ItemizedLayer(Map map, MarkerSymbol defaultSymbol) {
		super(map);

		mDefaultMarker = defaultSymbol;
		mRenderer = new ItemOverlay();
	}

	private final Point mMapPoint = new Point();

	/**
	 * Utility method to perform all processing on a new ItemizedOverlay.
	 * Subclasses provide Items through the createItem(int) method. The subclass
	 * should call this as soon as it has data, before anything else gets
	 * called.
	 */
	protected final void populate() {
		synchronized (lock) {
			final int size = size();
			mSize = size;

			// reuse previous items
			InternalItem pool = mItems;
			mItems = null;

			// flip order to draw in backward cycle, so the items
			// with the least index are on the front.
			for (int a = 0; a < size; a++) {
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

				it.item = createItem(a);

				// pre-project points
				MercatorProjection.project(it.item.mGeoPoint, mMapPoint);
				it.px = mMapPoint.x;
				it.py = mMapPoint.y;
			}
			mUpdate = true;
		}
	}

	/**
	 * Returns the Item at the given index.
	 * 
	 * @param position
	 *            the position of the item to return
	 * @return the Item of the given index.
	 */
	public final Item getItem(final int position) {
		synchronized (lock) {
			InternalItem item = mItems;
			for (int i = mSize - position - 1; i > 0 && item != null; i--)
				item = item.next;

			if (item != null)
				return item.item;

			return null;
		}
	}

	/**
	 * If the given Item is found in the overlay, force it to be the current
	 * focus-bearer. Any registered {@@link
	 * ItemizedOverlay#OnFocusChangeListener} will be notified. This does not
	 * move the map, so if the Item isn't already centered, the user may get
	 * confused. If the Item is not found, this is a no-op. You can also pass
	 * null to remove focus.
	 * 
	 * @param item
	 *            ...
	 */
	public void setFocus(final Item item) {
		mFocusedItem = item;
	}

	/**
	 * @return the currently-focused item, or null if no item is currently
	 *         focused.
	 */
	public Item getFocus() {
		return mFocusedItem;
	}

}
