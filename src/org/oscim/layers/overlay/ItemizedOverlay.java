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

package org.oscim.layers.overlay;

// TODO
// - need to sort items back to front for rendering
// - and to make this work for multiple overlays
//   a global scenegraph is probably required.

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.PointD;
import org.oscim.core.Tile;
import org.oscim.layers.overlay.OverlayItem.HotspotPlace;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.layers.BasicRenderLayer;
import org.oscim.renderer.sublayers.SymbolLayer;
import org.oscim.utils.GeometryUtils;
import org.oscim.view.MapView;

import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/* @author Marc Kurtz
 * @author Nicolas Gramlich
 * @author Theodore Hong
 * @author Fred Eisele
 * @author Hannes Janetzek
 * */
/**
 * Draws a list of {@link OverlayItem} as markers to a map. The item with the
 * lowest index is drawn as last and therefore the 'topmost' marker. It also
 * gets checked for onTap first. This class is generic, because you then you get
 * your custom item-class passed back in onTap().
 *
 * @param <Item>
 *            ...
 */
public abstract class ItemizedOverlay<Item extends OverlayItem> extends Overlay implements
		Overlay.Snappable {

	//private final static String TAG = ItemizedOverlay.class.getName();

	protected final Drawable mDefaultMarker;
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

	class ItemOverlay extends BasicRenderLayer {

		private final SymbolLayer mSymbolLayer;
		private final float[] mBox = new float[8];

		public ItemOverlay(MapView mapView) {
			super(mapView);
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

			mMapView.getMapViewPosition().getMapViewProjection(mBox);

			synchronized (lock) {
				if (mItems == null) {
					if (layers.textureLayers != null) {
						layers.clear();
						newData = true;
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

					int state = 0;
					if (mDrawFocusedItem && (mFocusedItem == it.item))
						state = OverlayItem.ITEM_STATE_FOCUSED_MASK;

					Drawable marker = it.item.getDrawable();
					if (marker == null)
						marker = mDefaultMarker;

					//	if (item.getMarker(state) == null) {
					//		OverlayItem.setState(mDefaultMarker, state);
					//		marker = mDefaultMarker;
					//	} else
					//		marker = item.getMarker(state);

					mSymbolLayer.addDrawable(marker, state, it.x, it.y);
				}

			}
			mSymbolLayer.prepare();
			layers.textureLayers = mSymbolLayer;
			newData = true;
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

	public ItemizedOverlay(MapView mapView, final Drawable pDefaultMarker) {
		super(mapView);

		if (pDefaultMarker == null) {
			throw new IllegalArgumentException("You must pass a default marker to ItemizedOverlay.");
		}

		this.mDefaultMarker = pDefaultMarker;
		mLayer = new ItemOverlay(mapView);
	}

	private final PointD mMapPoint = new PointD();

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
		//			return mInternalItemList.get(position);
	}

	//	private Drawable getDefaultMarker(final int state) {
	//		OverlayItem.setState(mDefaultMarker, state);
	//		return mDefaultMarker;
	//	}

	/**
	 * See if a given hit point is within the bounds of an item's marker.
	 * Override to modify the way an item is hit tested. The hit point is
	 * relative to the marker's bounds. The default implementation just checks
	 * to see if the hit point is within the touchable bounds of the marker.
	 *
	 * @param item
	 *            the item to hit test
	 * @param marker
	 *            the item's marker
	 * @param hitX
	 *            x coordinate of point to check
	 * @param hitY
	 *            y coordinate of point to check
	 * @return true if the hit point is within the marker
	 */
	protected boolean hitTest(final Item item, final android.graphics.drawable.Drawable marker,
			final int hitX,
			final int hitY) {
		return marker.getBounds().contains(hitX, hitY);
	}

	/**
	 * Set whether or not to draw the focused item. The default is to draw it,
	 * but some clients may prefer to draw the focused item themselves.
	 *
	 * @param drawFocusedItem
	 *            ...
	 */
	public void setDrawFocusedItem(final boolean drawFocusedItem) {
		mDrawFocusedItem = drawFocusedItem;
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

	private static final Rect mRect = new Rect();

	/**
	 * Adjusts a drawable's bounds so that (0,0) is a pixel in the location
	 * described by the hotspot parameter. Useful for "pin"-like graphics. For
	 * convenience, returns the same drawable that was passed in.
	 *
	 * @param marker
	 *            the drawable to adjust
	 * @param hotspot
	 *            the hotspot for the drawable
	 * @return the same drawable that was passed in.
	 */
	public static synchronized Drawable boundToHotspot(final Drawable marker, HotspotPlace hotspot) {
		final int markerWidth = marker.getIntrinsicWidth();
		final int markerHeight = marker.getIntrinsicHeight();

		mRect.set(0, 0, 0 + markerWidth, 0 + markerHeight);

		if (hotspot == null) {
			hotspot = HotspotPlace.BOTTOM_CENTER;
		}

		switch (hotspot) {
			default:
			case NONE:
				break;
			case CENTER:
				mRect.offset(-markerWidth / 2, -markerHeight / 2);
				break;
			case BOTTOM_CENTER:
				mRect.offset(-markerWidth / 2, -markerHeight);
				break;
			case TOP_CENTER:
				mRect.offset(-markerWidth / 2, 0);
				break;
			case RIGHT_CENTER:
				mRect.offset(-markerWidth, -markerHeight / 2);
				break;
			case LEFT_CENTER:
				mRect.offset(0, -markerHeight / 2);
				break;
			case UPPER_RIGHT_CORNER:
				mRect.offset(-markerWidth, 0);
				break;
			case LOWER_RIGHT_CORNER:
				mRect.offset(-markerWidth, -markerHeight);
				break;
			case UPPER_LEFT_CORNER:
				mRect.offset(0, 0);
				break;
			case LOWER_LEFT_CORNER:
				mRect.offset(0, -markerHeight);
				break;
		}
		marker.setBounds(mRect);
		return marker;
	}

	public static Drawable makeMarker(Resources res, int id, HotspotPlace place) {
		Drawable marker = res.getDrawable(id);
		if (place == null)
			boundToHotspot(marker, HotspotPlace.CENTER);
		else
			boundToHotspot(marker, place);
		return marker;
	}
	//	/**
	//	 * Draw a marker on each of our items. populate() must have been called
	//	 * first.<br/>
	//	 * <br/>
	//	 * The marker will be drawn twice for each Item in the Overlay--once in the
	//	 * shadow phase, skewed and darkened, then again in the non-shadow phase.
	//	 * The bottom-center of the marker will be aligned with the geographical
	//	 * coordinates of the Item.<br/>
	//	 * <br/>
	//	 * The order of drawing may be changed by overriding the getIndexToDraw(int)
	//	 * method. An item may provide an alternate marker via its
	//	 * OverlayItem.getMarker(int) method. If that method returns null, the
	//	 * default marker is used.<br/>
	//	 * <br/>
	//	 * The focused item is always drawn last, which puts it visually on top of
	//	 * the other items.<br/>
	//	 *
	//	 * @param canvas
	//	 *            the Canvas upon which to draw. Note that this may already have
	//	 *            a transformation applied, so be sure to leave it the way you
	//	 *            found it
	//	 * @param mapView
	//	 *            the MapView that requested the draw. Use
	//	 *            MapView.getProjection() to convert between on-screen pixels
	//	 *            and latitude/longitude pairs
	//	 * @param shadow
	//	 *            if true, draw the shadow layer. If false, draw the overlay
	//	 *            contents.
	//	 */
	//	@Override
	//	public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
	//
	//		if (shadow) {
	//			return;
	//		}
	//
	//		final Projection pj = mapView.getProjection();
	//		final int size = this.mInternalItemList.size() - 1;
	//
	//		/*
	//		 * Draw in backward cycle, so the items with the least index are on the
	//		 * front.
	//		 */
	//		for (int i = size; i >= 0; i--) {
	//			final Item item = getItem(i);
	//			pj.toMapPixels(item.mGeoPoint, mCurScreenCoords);
	//
	//			onDrawItem(canvas, item, mCurScreenCoords);
	//		}
	//	}

	//	/**
	//	 * Draws an item located at the provided screen coordinates to the canvas.
	//	 *
	//	 * @param canvas
	//	 *            what the item is drawn upon
	//	 * @param item
	//	 *            the item to be drawn
	//	 * @param curScreenCoords
	//	 *            the screen coordinates of the item
	//	 */
	//	protected void onDrawItem(final Canvas canvas, final Item item, final Point curScreenCoords) {
	//		int state = 0;
	//
	//		if (mDrawFocusedItem && (mFocusedItem == item))
	//			state = OverlayItem.ITEM_STATE_FOCUSED_MASK;
	//
	//		Drawable marker;
	//
	//		if (item.getMarker(state) == null)
	//			marker = getDefaultMarker(state);
	//		else
	//			marker = item.getMarker(state);
	//
	//		boundToHotspot(marker, item.getMarkerHotspot());
	//
	//		// draw it
	//		Overlay.drawAt(canvas, marker, curScreenCoords.x, curScreenCoords.y, false);
	//	}
}
