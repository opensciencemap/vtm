package org.oscim.overlay;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.oscim.core.MapPosition;
import org.oscim.overlay.Overlay.Snappable;
import org.oscim.renderer.overlays.RenderOverlay;
import org.oscim.view.MapView;

import android.graphics.Point;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class OverlayManager extends AbstractList<Overlay> {

	private final CopyOnWriteArrayList<Overlay> mOverlayList;

	public OverlayManager() {
		mOverlayList = new CopyOnWriteArrayList<Overlay>();
	}

	@Override
	public synchronized Overlay get(final int pIndex) {
		return mOverlayList.get(pIndex);
	}

	@Override
	public synchronized int size() {
		return mOverlayList.size();
	}

	@Override
	public synchronized void add(final int pIndex, final Overlay pElement) {
		mOverlayList.add(pIndex, pElement);
		mDirtyOverlays = true;
	}

	@Override
	public synchronized Overlay remove(final int pIndex) {
		mDirtyOverlays = true;
		return mOverlayList.remove(pIndex);
	}

	@Override
	public synchronized Overlay set(final int pIndex, final Overlay pElement) {
		mDirtyOverlays = true;
		return mOverlayList.set(pIndex, pElement);
	}

	private boolean mDirtyOverlays;
	private List<RenderOverlay> mDrawLayers = new ArrayList<RenderOverlay>();

	public List<RenderOverlay> getRenderLayers() {
		if (mDirtyOverlays)
			updateOverlays();

		return mDrawLayers;
	}

	public void onDetach(final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			o.onDetach(pMapView);
	}

	Overlay[] mOverlays;

	private synchronized void updateOverlays() {
		if (!mDirtyOverlays)
			return;

		mOverlays = new Overlay[mOverlayList.size()];

		mDrawLayers.clear();
		for (int i = 0, n = mOverlayList.size(); i < n; i++) {
			Overlay o = mOverlayList.get(i);
			RenderOverlay l = o.getLayer();
			if (l != null)
				mDrawLayers.add(l);

			mOverlays[n - i - 1] = o;
		}

		mDirtyOverlays = false;
	}

	public boolean onKeyDown(final int keyCode, final KeyEvent event, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onKeyDown(keyCode, event, pMapView))
				return true;

		return false;
	}

	public boolean onKeyUp(final int keyCode, final KeyEvent event, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onKeyUp(keyCode, event, pMapView))
				return true;

		return false;
	}

	public boolean onTouchEvent(final MotionEvent event, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onTouchEvent(event, pMapView))
				return true;

		return false;
	}

	public boolean onTrackballEvent(final MotionEvent event, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onTrackballEvent(event, pMapView))
				return true;

		return false;
	}

	public boolean onSnapToItem(final int x, final int y, final Point snapPoint,
			final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o instanceof Snappable)
				if (((Snappable) o).onSnapToItem(x, y, snapPoint, pMapView))
					return true;

		return false;
	}

	/* GestureDetector.OnDoubleTapListener */

	public boolean onDoubleTap(final MotionEvent e, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onDoubleTap(e, pMapView))
				return true;

		return false;
	}

	public boolean onDoubleTapEvent(final MotionEvent e, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onDoubleTapEvent(e, pMapView))
				return true;

		return false;
	}

	public boolean onSingleTapConfirmed(final MotionEvent e, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onSingleTapConfirmed(e, pMapView))
				return true;

		return false;
	}

	/* OnGestureListener */

	public boolean onDown(final MotionEvent pEvent, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onDown(pEvent, pMapView))
				return true;

		return false;
	}

	public boolean onFling(final MotionEvent pEvent1, final MotionEvent pEvent2,
			final float pVelocityX, final float pVelocityY, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY, pMapView))
				return true;

		return false;
	}

	public boolean onLongPress(final MotionEvent pEvent, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onLongPress(pEvent, pMapView))
				return true;

		return false;
	}

	public boolean onScroll(final MotionEvent pEvent1, final MotionEvent pEvent2,
			final float pDistanceX, final float pDistanceY, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY, pMapView))
				return true;

		return false;
	}

	public void onShowPress(final MotionEvent pEvent, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			o.onShowPress(pEvent, pMapView);

	}

	public boolean onSingleTapUp(final MotionEvent pEvent, final MapView pMapView) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onSingleTapUp(pEvent, pMapView))
				return true;

		return false;
	}

	public void onUpdate(MapPosition mapPosition, boolean changed) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			o.onUpdate(mapPosition, changed);
	}

	// /**
	// * Gets the optional TilesOverlay class.
	// *
	// * @return the tilesOverlay
	// */
	// public TilesOverlay getTilesOverlay() {
	// return mTilesOverlay;
	// }
	//
	// /**
	// * Sets the optional TilesOverlay class. If set, this overlay will be
	// drawn before all other
	// * overlays and will not be included in the editable list of overlays and
	// can't be cleared
	// * except by a subsequent call to setTilesOverlay().
	// *
	// * @param tilesOverlay
	// * the tilesOverlay to set
	// */
	// public void setTilesOverlay(final TilesOverlay tilesOverlay) {
	// mTilesOverlay = tilesOverlay;
	// }

	//	public void onDraw(final Canvas c, final MapView pMapView) {
	//		// if ((mTilesOverlay != null) && mTilesOverlay.isEnabled()) {
	//		// mTilesOverlay.draw(c, pMapView, true);
	//		// }
	//		//
	//		// if ((mTilesOverlay != null) && mTilesOverlay.isEnabled()) {
	//		// mTilesOverlay.draw(c, pMapView, false);
	//		// }
	//
	//		for (final Overlay overlay : mOverlayList) {
	//			if (overlay.isEnabled()) {
	//				overlay.draw(c, pMapView, true);
	//			}
	//		}
	//
	//		for (final Overlay overlay : mOverlayList) {
	//			if (overlay.isEnabled()) {
	//				overlay.draw(c, pMapView, false);
	//			}
	//		}
	//
	//	}

	// ** Options Menu **//

	// public void setOptionsMenusEnabled(final boolean pEnabled) {
	// for (final Overlay overlay : mOverlayList) {
	// if ((overlay instanceof IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()) {
	// ((IOverlayMenuProvider) overlay).setOptionsMenuEnabled(pEnabled);
	// }
	// }
	// }
	//
	// public boolean onCreateOptionsMenu(final Menu pMenu, final int
	// menuIdOffset,
	// final MapView mapView) {
	// boolean result = true;
	// for (final Overlay overlay : this.overlaysReversed()) {
	// if ((overlay instanceof IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()) {
	// result &= ((IOverlayMenuProvider) overlay).onCreateOptionsMenu(pMenu,
	// menuIdOffset,
	// mapView);
	// }
	// }
	//
	// if ((mTilesOverlay != null) && (mTilesOverlay instanceof
	// IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) mTilesOverlay).isOptionsMenuEnabled()) {
	// result &= mTilesOverlay.onCreateOptionsMenu(pMenu, menuIdOffset,
	// mapView);
	// }
	//
	// return result;
	// }
	//
	// public boolean onPrepareOptionsMenu(final Menu pMenu, final int
	// menuIdOffset,
	// final MapView mapView) {
	// for (final Overlay overlay : this.overlaysReversed()) {
	// if ((overlay instanceof IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()) {
	// ((IOverlayMenuProvider) overlay).onPrepareOptionsMenu(pMenu,
	// menuIdOffset, mapView);
	// }
	// }
	//
	// if ((mTilesOverlay != null) && (mTilesOverlay instanceof
	// IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) mTilesOverlay).isOptionsMenuEnabled()) {
	// mTilesOverlay.onPrepareOptionsMenu(pMenu, menuIdOffset, mapView);
	// }
	//
	// return true;
	// }
	//
	// public boolean onOptionsItemSelected(final MenuItem item, final int
	// menuIdOffset,
	// final MapView mapView) {
	// for (final Overlay overlay : this.overlaysReversed()) {
	// if ((overlay instanceof IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) overlay).isOptionsMenuEnabled()
	// && ((IOverlayMenuProvider) overlay).onOptionsItemSelected(item,
	// menuIdOffset,
	// mapView)) {
	// return true;
	// }
	// }
	//
	// if ((mTilesOverlay != null)
	// && (mTilesOverlay instanceof IOverlayMenuProvider)
	// && ((IOverlayMenuProvider) mTilesOverlay).isOptionsMenuEnabled()
	// && ((IOverlayMenuProvider) mTilesOverlay).onOptionsItemSelected(item,
	// menuIdOffset,
	// mapView)) {
	// return true;
	// }
	//
	// return false;
	// }
}
