package org.osmdroid.overlays;

// TODO composite view as texture overlay and only allow one bubble at a time.

import org.oscim.view.MapView;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

/**
 * View that can be displayed on an OSMDroid map, associated to a GeoPoint.
 * Typical usage: cartoon-like bubbles displayed when clicking an overlay item.
 * It mimics the InfoWindow class of Google Maps JavaScript API V3. Main
 * differences are:
 * <ul>
 * <li>Structure and content of the view is let to the responsibility of the
 * caller.</li>
 * <li>The same InfoWindow can be associated to many items.</li>
 * </ul>
 * Known issues:
 * <ul>
 * <li>It disappears when zooming in/out (osmdroid issue #259 on osmdroid 3.0.8,
 * should be fixed in next version).</li>
 * <li>The window is displayed "above" the marker, so the queue of the bubble
 * can hide the marker.</li>
 * </ul>
 * This is an abstract class.
 * @see DefaultInfoWindow
 * @author M.Kergall
 */
public abstract class InfoWindow {

	protected View mView;
	protected boolean mIsVisible = false;
	protected MapView mMapView;
	protected RelativeLayout mLayout;
	private android.widget.RelativeLayout.LayoutParams mLayoutPos;

	/**
	 * @param layoutResId
	 *            the id of the view resource.
	 * @param mapView
	 *            the mapview on which is hooked the view
	 */
	public InfoWindow(int layoutResId, MapView mapView) {
		mMapView = mapView;

		mIsVisible = false;
		ViewGroup parent = (ViewGroup) mapView.getParent();
		Context context = mapView.getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(layoutResId, parent, false);

		RelativeLayout.LayoutParams rlp =
				new RelativeLayout.LayoutParams(
						android.view.ViewGroup.LayoutParams.MATCH_PARENT,
						android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		mLayout = new RelativeLayout(context);
		mLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
		mLayout.setLayoutParams(rlp);
		mLayoutPos = rlp;

		// not so sure about this. why is just blitting the bitmap on glview so slow?...
		mView.setDrawingCacheEnabled(true);
		//		mLayout.setDrawingCacheEnabled(true);
		//		mLayout.setPersistentDrawingCache(ViewGroup.PERSISTENT_ALL_CACHES);
		//		mLayout.setAlwaysDrawnWithCacheEnabled(true); // call this method 
		mLayout.setWillNotDraw(true);
		mLayout.addView(mView);
	}

	/**
	 * Returns the Android view. This allows to set its content.
	 * @return the Android view
	 */
	public View getView() {
		return (mView);
	}

	/**
	 * open the window at the specified position.
	 * @param item
	 *            the item on which is hooked the view
	 * @param offsetX
	 *            (&offsetY) the offset of the view to the position, in pixels.
	 *            This allows to offset the view from the marker position.
	 * @param offsetY
	 *            ...
	 */
	public void open(ExtendedOverlayItem item, int offsetX, int offsetY) {
		onOpen(item);

		close(); // if it was already opened
		//		mView.requestLayout();
		mView.buildDrawingCache();
		mMapView.addView(mLayout);
		mIsVisible = true;
	}

	public void position(int x, int y) {
		// if this isnt madness...
		RelativeLayout.LayoutParams rlp = mLayoutPos;
		rlp.leftMargin = x;
		rlp.rightMargin = -x;
		rlp.topMargin = -y;
		rlp.bottomMargin = y + mMapView.getHeight() / 2;
		mLayout.setLayoutParams(rlp);

		//mMapView.requestLayout();
		mLayout.requestLayout();

		// using scrollTo the bubble somehow does not appear when it
		// is not already in viewport...
		//		mLayout.scrollTo(-x, y + mMapView.getHeight() / 2);
	}

	public void close() {
		if (mIsVisible) {
			mIsVisible = false;
			((ViewGroup) mLayout.getParent()).removeView(mLayout);
			onClose();
		}
	}

	public boolean isOpen() {
		return mIsVisible;
	}

	// Abstract methods to implement:
	public abstract void onOpen(ExtendedOverlayItem item);

	public abstract void onClose();

}
