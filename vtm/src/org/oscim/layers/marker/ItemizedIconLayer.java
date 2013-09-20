/*
 * Copyright 2012 osmdroid
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

import java.util.List;

import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.event.MotionEvent;
import org.oscim.event.TouchListener;
import org.oscim.map.Map;
import org.oscim.map.Viewport;

public class ItemizedIconLayer<Item extends MarkerItem> extends ItemizedLayer<Item>
		implements TouchListener {
	//private static final String TAG = ItemizedIconOverlay.class.getName();

	protected final List<Item> mItemList;
	protected OnItemGestureListener<Item> mOnItemGestureListener;
	private int mDrawnItemsLimit = Integer.MAX_VALUE;

	private final Point mTmpPoint = new Point();

	public ItemizedIconLayer(Map map, List<Item> list,
			MarkerSymbol defaultMarker,
			ItemizedIconLayer.OnItemGestureListener<Item> onItemGestureListener) {

		super(map, defaultMarker);

		this.mItemList = list;
		this.mOnItemGestureListener = onItemGestureListener;
		populate();
	}

	@Override
	public boolean onSnapToItem(int x, int y, Point snapPoint) {
		// TODO Implement this!
		return false;
	}

	@Override
	protected Item createItem(int index) {
		return mItemList.get(index);
	}

	@Override
	public int size() {
		return Math.min(mItemList.size(), mDrawnItemsLimit);
	}

	public boolean addItem(Item item) {
		final boolean result = mItemList.add(item);
		populate();
		return result;
	}

	public void addItem(int location, Item item) {
		mItemList.add(location, item);
	}

	public boolean addItems(List<Item> items) {
		final boolean result = mItemList.addAll(items);
		populate();
		return result;
	}

	public void removeAllItems() {
		removeAllItems(true);
	}

	public void removeAllItems(boolean withPopulate) {
		mItemList.clear();
		if (withPopulate) {
			populate();
		}
	}

	public boolean removeItem(Item item) {
		final boolean result = mItemList.remove(item);
		populate();
		return result;
	}

	public Item removeItem(int position) {
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
	public boolean onTap(MotionEvent event, MapPosition pos) {
		return activateSelectedItems(event, mActiveItemSingleTap);
	}

	protected boolean onSingleTapUpHelper(int index, Item item) {
		return this.mOnItemGestureListener.onItemSingleTapUp(index, item);
	}

	private final ActiveItem mActiveItemSingleTap = new ActiveItem() {
		@Override
		public boolean run(int index) {
			final ItemizedIconLayer<Item> that = ItemizedIconLayer.this;
			if (that.mOnItemGestureListener == null) {
				return false;
			}
			return onSingleTapUpHelper(index, that.mItemList.get(index));
		}
	};

	@Override
	public boolean onLongPress(MotionEvent event, MapPosition pos) {
		return activateSelectedItems(event, mActiveItemLongPress);
	}

	protected boolean onLongPressHelper(int index, Item item) {
		return this.mOnItemGestureListener.onItemLongPress(index, item);
	}

	private final ActiveItem mActiveItemLongPress = new ActiveItem() {
		@Override
		public boolean run(final int index) {
			final ItemizedIconLayer<Item> that = ItemizedIconLayer.this;
			if (that.mOnItemGestureListener == null) {
				return false;
			}
			return onLongPressHelper(index, getItem(index));
		}
	};

	@Override
	public boolean onPress(MotionEvent e, MapPosition pos) {
		return false;
	}

	/**
	 * When a content sensitive action is performed the content item needs to be
	 * identified. This method does that and then performs the assigned task on
	 * that item.
	 * @param event
	 *            ...
	 * @param task
	 *            ..
	 * @return true if event is handled false otherwise
	 */
	private boolean activateSelectedItems(MotionEvent event, ActiveItem task) {
		int size = mItemList.size();
		if (size == 0)
			return false;

		int eventX = (int) event.getX() - mMap.getWidth() / 2;
		int eventY = (int) event.getY() - mMap.getHeight() / 2;
		Viewport mapPosition = mMap.getViewport();

		BoundingBox bbox = mapPosition.getViewBox();

		int nearest = -1;
		double dist = Double.MAX_VALUE;

		for (int i = 0; i < size; i++) {
			Item item = getItem(i);

			if (!bbox.contains(item.mGeoPoint))
				continue;

			// TODO use intermediate projection
			mapPosition.toScreenPoint(item.getPoint(), mTmpPoint);

			float dx = (float) (mTmpPoint.x - eventX);
			float dy = (float) (mTmpPoint.y - eventY);

			double d = dx * dx + dy * dy;
			// squared dist: 50*50 pixel
			if (d < 2500) {
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

	public int getDrawnItemsLimit() {
		return this.mDrawnItemsLimit;
	}

	public void setDrawnItemsLimit(final int aLimit) {
		this.mDrawnItemsLimit = aLimit;
	}

	/**
	 * When the item is touched one of these methods may be invoked depending on
	 * the type of touch. Each of them returns true if the event was completely
	 * handled.
	 * @param <T>
	 *            ....
	 */
	public static interface OnItemGestureListener<T> {
		public boolean onItemSingleTapUp(int index, T item);

		public boolean onItemLongPress(int index, T item);
	}

	public static interface ActiveItem {
		public boolean run(int aIndex);
	}
}
