/*
 * Copyright 2012 osmdroid
 * Copyright 2013 OpenScienceMap
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
package org.oscim.overlay;

import java.util.List;

import org.oscim.core.MercatorProjection;
import org.oscim.overlay.ResourceProxy.bitmap;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

public class ItemizedIconOverlay<Item extends OverlayItem> extends ItemizedOverlay<Item> {
	private static final String TAG = ItemizedIconOverlay.class.getSimpleName();

	protected final List<Item> mItemList;
	protected OnItemGestureListener<Item> mOnItemGestureListener;
	private int mDrawnItemsLimit = Integer.MAX_VALUE;
	private final Point mTouchScreenPoint = new Point();

	private final Point mItemPoint = new Point();

	public ItemizedIconOverlay(
			final MapView mapView,
			final List<Item> pList,
			final Drawable pDefaultMarker,
			final ItemizedIconOverlay.OnItemGestureListener<Item> pOnItemGestureListener,
			final ResourceProxy pResourceProxy) {

		super(mapView, pDefaultMarker, pResourceProxy);

		this.mItemList = pList;
		this.mOnItemGestureListener = pOnItemGestureListener;
		populate();
	}

	public ItemizedIconOverlay(
			final MapView mapView,
			final List<Item> pList,
			final ItemizedIconOverlay.OnItemGestureListener<Item> pOnItemGestureListener,
			final ResourceProxy pResourceProxy) {

		this(mapView, pList, pResourceProxy.getDrawable(bitmap.marker_default),
				pOnItemGestureListener,
				pResourceProxy);
	}

	public ItemizedIconOverlay(
			final MapView mapView,
			final Context pContext,
			final List<Item> pList,
			final ItemizedIconOverlay.OnItemGestureListener<Item> pOnItemGestureListener) {
		this(mapView, pList, new DefaultResourceProxyImpl(pContext)
				.getDrawable(bitmap.marker_default),
				pOnItemGestureListener, new DefaultResourceProxyImpl(pContext));
	}

	@Override
	public boolean onSnapToItem(final int pX, final int pY, final Point pSnapPoint,
			final MapView pMapView) {
		// TODO Implement this!
		return false;
	}

	@Override
	protected Item createItem(final int index) {
		return mItemList.get(index);
	}

	@Override
	public int size() {
		return Math.min(mItemList.size(), mDrawnItemsLimit);
	}

	public boolean addItem(final Item item) {
		final boolean result = mItemList.add(item);
		populate();
		return result;
	}

	public void addItem(final int location, final Item item) {
		mItemList.add(location, item);
	}

	public boolean addItems(final List<Item> items) {
		final boolean result = mItemList.addAll(items);
		populate();
		return result;
	}

	public void removeAllItems() {
		removeAllItems(true);
	}

	public void removeAllItems(final boolean withPopulate) {
		mItemList.clear();
		if (withPopulate) {
			populate();
		}
	}

	public boolean removeItem(final Item item) {
		final boolean result = mItemList.remove(item);
		populate();
		return result;
	}

	public Item removeItem(final int position) {
		final Item result = mItemList.remove(position);
		populate();
		return result;
	}

	/**
	 * Each of these methods performs a item sensitive check. If the item is
	 * located its corresponding method is called. The result of the call is
	 * returned. Helper methods are provided so that child classes may more
	 * easily override behavior without resorting to overriding the
	 * ItemGestureListener methods.
	 */
	@Override
	public boolean onSingleTapUp(final MotionEvent event, final MapView mapView) {
		return (activateSelectedItems(event, mapView, new ActiveItem() {
			@Override
			public boolean run(final int index) {
				final ItemizedIconOverlay<Item> that = ItemizedIconOverlay.this;
				if (that.mOnItemGestureListener == null) {
					return false;
				}
				return onSingleTapUpHelper(index, that.mItemList.get(index), mapView);
			}
		})) || super.onSingleTapUp(event, mapView);
	}

	/**
	 * @param index
	 *            ...
	 * @param item
	 *            ...
	 * @param mapView
	 *            ...
	 * @return ...
	 */
	protected boolean onSingleTapUpHelper(final int index, final Item item, final MapView mapView) {
		return this.mOnItemGestureListener.onItemSingleTapUp(index, item);
	}

	@Override
	public boolean onLongPress(final MotionEvent event, final MapView mapView) {

		Log.d(TAG, "onLongPress");

		return (activateSelectedItems(event, mapView, new ActiveItem() {
			@Override
			public boolean run(final int index) {
				final ItemizedIconOverlay<Item> that = ItemizedIconOverlay.this;
				if (that.mOnItemGestureListener == null) {
					return false;
				}
				return onLongPressHelper(index, getItem(index));
			}
		})) || super.onLongPress(event, mapView);
	}

	protected boolean onLongPressHelper(final int index, final Item item) {
		return this.mOnItemGestureListener.onItemLongPress(index, item);
	}

	/**
	 * When a content sensitive action is performed the content item needs to be
	 * identified. This method does that and then performs the assigned task on
	 * that item.
	 *
	 * @param event
	 *            ...
	 * @param mapView
	 *            ...
	 * @param task
	 *            ..
	 * @return true if event is handled false otherwise
	 */
	private boolean activateSelectedItems(final MotionEvent event, final MapView mapView,
			final ActiveItem task) {
		final int eventX = (int) event.getX();
		final int eventY = (int) event.getY();
		MapViewPosition mapViewPosition = mMapView.getMapViewPosition();

		byte z = mapViewPosition.getMapPosition().zoomLevel;

		mapViewPosition.getScreenPointOnMap(eventX, eventY, mTouchScreenPoint);

		int nearest = -1;
		double dist = Double.MAX_VALUE;

		// TODO use intermediate projection and bounding box test
		for (int i = 0; i < this.mItemList.size(); ++i) {
			final Item item = getItem(i);

			//	final Drawable marker = (item.getMarker(0) == null) ? this.mDefaultMarker : item
			//		.getMarker(0);
			MercatorProjection.projectPoint(item.getPoint(), z, mItemPoint);

			float dx = mItemPoint.x - mTouchScreenPoint.x;
			float dy = mItemPoint.y - mTouchScreenPoint.y;
			double d = Math.sqrt(dx * dx + dy * dy);

			if (d < 50) {
				if (d < dist) {
					dist = d;
					nearest = i;
				}
			}
		}

		if (nearest >= 0 && task.run(nearest)) {
			return true;
		}

		return false;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public int getDrawnItemsLimit() {
		return this.mDrawnItemsLimit;
	}

	public void setDrawnItemsLimit(final int aLimit) {
		this.mDrawnItemsLimit = aLimit;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * When the item is touched one of these methods may be invoked depending on
	 * the type of touch. Each of them returns true if the event was completely
	 * handled.
	 *
	 * @param <T>
	 *            ....
	 */
	public static interface OnItemGestureListener<T> {
		public boolean onItemSingleTapUp(final int index, final T item);

		public boolean onItemLongPress(final int index, final T item);
	}

	public static interface ActiveItem {
		public boolean run(final int aIndex);
	}
}
