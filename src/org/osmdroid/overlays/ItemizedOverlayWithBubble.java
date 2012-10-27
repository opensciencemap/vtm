package org.osmdroid.overlays;

import java.util.List;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.overlay.ItemizedIconOverlay;
import org.oscim.overlay.OverlayItem;
import org.oscim.view.MapView;
import org.osmdroid.utils.BonusPackHelper;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;

/**
 * An itemized overlay with an InfoWindow or "bubble" which opens when the user
 * taps on an overlay item, and displays item attributes. <br>
 * Items must be ExtendedOverlayItem. <br>
 * @see ExtendedOverlayItem
 * @see InfoWindow
 * @author M.Kergall
 * @param <Item>
 *            ...
 */
public class ItemizedOverlayWithBubble<Item extends OverlayItem> extends ItemizedIconOverlay<Item>
		implements ItemizedIconOverlay.OnItemGestureListener<Item>
{

	protected List<Item> mItemsList;

	// only one for all items of this overlay => one at a time
	protected InfoWindow mBubble;

	// the item currently showing the bubble. Null if none.
	protected OverlayItem mItemWithBubble;

	static int layoutResId = 0;

	@Override
	public boolean onItemLongPress(final int index, final OverlayItem item) {
		if (mBubble.isOpen())
			hideBubble();
		else
			showBubble(index);
		return false;
	}

	@Override
	public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
		showBubble(index);
		return false;
	}

	private Point mTmpPoint = new Point();

	@Override
	public void onUpdate(MapPosition mapPosition) {
		if (mBubble.isOpen()) {
			GeoPoint gp = mItemWithBubble.getPoint();
			Point p = mTmpPoint;

			mMapView.getMapViewPosition().project(gp, p);

			mBubble.position(p.x, p.y);
		}
	}

	public ItemizedOverlayWithBubble(final MapView mapView, final Context context,
			final List<Item> aList, final InfoWindow bubble) {

		super(mapView, context, aList, null);

		mItemsList = aList;
		if (bubble != null) {
			mBubble = bubble;
		} else {
			// build default bubble:
			String packageName = context.getPackageName();
			if (layoutResId == 0) {
				layoutResId = context.getResources().getIdentifier(
						"layout/bonuspack_bubble", null,
						packageName);
				if (layoutResId == 0)
					Log.e(BonusPackHelper.LOG_TAG,
							"ItemizedOverlayWithBubble: layout/bonuspack_bubble not found in "
									+ packageName);
			}
			mBubble = new DefaultInfoWindow(layoutResId, mapView);
		}
		mItemWithBubble = null;

		mOnItemGestureListener = this;
	}

	public ItemizedOverlayWithBubble(final Context context, final List<Item> aList,
			final MapView mapView) {
		this(mapView, context, aList, null);
	}

	void showBubble(int index) {
		showBubbleOnItem(index, mMapView);
	}

	/**
	 * Opens the bubble on the item. For each ItemizedOverlay, only one bubble
	 * is opened at a time. If you want more bubbles opened simultaneously, use
	 * many ItemizedOverlays.
	 * @param index
	 *            of the overlay item to show
	 * @param mapView
	 *            ...
	 */
	public void showBubbleOnItem(final int index, final MapView mapView) {
		ExtendedOverlayItem eItem = (ExtendedOverlayItem) (getItem(index));
		mItemWithBubble = eItem;
		if (eItem != null) {
			eItem.showBubble(mBubble, mapView);
			//			mMapView.getMapViewPosition().animateTo(eItem.mGeoPoint, 0);

			mapView.redrawMap();
			setFocus((Item) eItem);
		}
	}

	/** Close the bubble (if it's opened). */
	public void hideBubble() {
		mBubble.close();
		mItemWithBubble = null;
	}

	@Override
	protected boolean onSingleTapUpHelper(final int index, final Item item, final MapView mapView) {
		showBubbleOnItem(index, mapView);
		return true;
	}

	/** @return the item currenty showing the bubble, or null if none. */
	public OverlayItem getBubbledItem() {
		if (mBubble.isOpen())
			return mItemWithBubble;

		return null;
	}

	/** @return the index of the item currenty showing the bubble, or -1 if none. */
	public int getBubbledItemId() {
		OverlayItem item = getBubbledItem();
		if (item == null)
			return -1;

		return mItemsList.indexOf(item);
	}

	@Override
	public boolean removeItem(final Item item) {
		boolean result = super.removeItem(item);
		if (mItemWithBubble == item) {
			hideBubble();
		}
		return result;
	}

	@Override
	public void removeAllItems() {
		super.removeAllItems();
		hideBubble();
	}

	//	FIXME @Override
	//	public void draw(final Canvas canvas, final MapView mapView, final boolean shadow) {
	//		// 1. Fixing drawing focused item on top in ItemizedOverlay (osmdroid
	//		// issue 354):
	//		if (shadow) {
	//			return;
	//		}
	//		final Projection pj = mapView.getProjection();
	//		final int size = mItemsList.size() - 1;
	//		final Point mCurScreenCoords = new Point();
	//
	//		/*
	//		 * Draw in backward cycle, so the items with the least index are on the
	//		 * front.
	//		 */
	//		for (int i = size; i >= 0; i--) {
	//			final Item item = getItem(i);
	//			if (item != mItemWithBubble) {
	//				pj.toMapPixels(item.mGeoPoint, mCurScreenCoords);
	//				onDrawItem(canvas, item, mCurScreenCoords);
	//			}
	//		}
	//		// draw focused item last:
	//		if (mItemWithBubble != null) {
	//			pj.toMapPixels(mItemWithBubble.mGeoPoint, mCurScreenCoords);
	//			onDrawItem(canvas, (Item) mItemWithBubble, mCurScreenCoords);
	//		}
	//	}

}
