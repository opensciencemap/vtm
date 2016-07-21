package org.osmdroid.overlays;

// TODO composite view as texture overlay and only allow one bubble at a time.

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.oscim.android.MapView;

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
 *
 * @author M.Kergall
 * @see DefaultInfoWindow
 */
public abstract class InfoWindow {

    protected View mView;
    protected boolean mIsVisible = false;
    protected RelativeLayout mLayout;
    private android.widget.RelativeLayout.LayoutParams mLayoutPos;

    private MapView mMap;

    /**
     * @param layoutResId the id of the view resource.
     * @param mapView     the mapview on which is hooked the view
     */
    public InfoWindow(int layoutResId, MapView mapView) {
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
        mLayout.setWillNotDraw(true);
        mLayout.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        mLayout.setLayoutParams(rlp);
        mLayoutPos = rlp;
        mView.setDrawingCacheEnabled(true);
        mLayout.addView(mView);

        mIsVisible = false;
        mLayout.setVisibility(View.GONE);
        mMap = mapView;

        parent.addView(mLayout);
    }

    /**
     * Returns the Android view. This allows to set its content.
     *
     * @return the Android view
     */
    public View getView() {
        return (mView);
    }

    private int mHeight;

    /**
     * open the window at the specified position.
     *
     * @param item    the item on which is hooked the view
     * @param offsetX (&offsetY) the offset of the view to the position, in pixels.
     *                This allows to offset the view from the marker position.
     * @param offsetY ...
     */
    public void open(ExtendedMarkerItem item, int offsetX, int offsetY) {

        onOpen(item);
        close();

        mView.buildDrawingCache();

        mHeight = mMap.getHeight();
        mLayout.setVisibility(View.VISIBLE);
        mIsVisible = true;

    }

    public void position(int x, int y) {
        RelativeLayout.LayoutParams rlp = mLayoutPos;
        rlp.leftMargin = x;
        rlp.rightMargin = -x;
        rlp.topMargin = y;
        rlp.bottomMargin = mHeight / 2 - y;
        mLayout.setLayoutParams(rlp);
        mLayout.requestLayout();
    }

    public void close() {

        if (mIsVisible) {
            mIsVisible = false;
            mLayout.setVisibility(View.GONE);
            onClose();
        }
    }

    public boolean isOpen() {
        return mIsVisible;
    }

    // Abstract methods to implement:
    public abstract void onOpen(ExtendedMarkerItem item);

    public abstract void onClose();

}
