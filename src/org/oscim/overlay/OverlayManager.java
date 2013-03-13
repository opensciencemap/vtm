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

package org.oscim.overlay;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.oscim.core.MapPosition;
import org.oscim.core.PointF;
import org.oscim.overlay.Overlay.Snappable;
import org.oscim.renderer.overlays.RenderOverlay;

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
	private final List<RenderOverlay> mDrawLayers = new ArrayList<RenderOverlay>();

	public List<RenderOverlay> getRenderLayers() {
		if (mDirtyOverlays)
			updateOverlays();

		return mDrawLayers;
	}

	public void onDetach() {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			o.onDetach();
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

	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onKeyDown(keyCode, event))
				return true;

		return false;
	}

	public boolean onKeyUp(final int keyCode, final KeyEvent event) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onKeyUp(keyCode, event))
				return true;

		return false;
	}

	public boolean onTouchEvent(final MotionEvent event) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onTouchEvent(event))
				return true;

		return false;
	}

	public boolean onTrackballEvent(final MotionEvent event) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onTrackballEvent(event))
				return true;

		return false;
	}

	public boolean onSnapToItem(final int x, final int y, final PointF snapPoint) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o instanceof Snappable)
				if (((Snappable) o).onSnapToItem(x, y, snapPoint))
					return true;

		return false;
	}

	/* GestureDetector.OnDoubleTapListener */

	public boolean onDoubleTap(final MotionEvent e) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onDoubleTap(e))
				return true;

		return false;
	}

	public boolean onDoubleTapEvent(final MotionEvent e) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onDoubleTapEvent(e))
				return true;

		return false;
	}

	public boolean onSingleTapConfirmed(final MotionEvent e) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onSingleTapConfirmed(e))
				return true;

		return false;
	}

	/* OnGestureListener */

	public boolean onDown(final MotionEvent pEvent) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onDown(pEvent))
				return true;

		return false;
	}

	public boolean onFling(final MotionEvent pEvent1, final MotionEvent pEvent2,
			final float pVelocityX, final float pVelocityY) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY))
				return true;

		return false;
	}

	public boolean onLongPress(final MotionEvent pEvent) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onLongPress(pEvent))
				return true;

		return false;
	}

	public boolean onScroll(final MotionEvent pEvent1, final MotionEvent pEvent2,
			final float pDistanceX, final float pDistanceY) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY))
				return true;

		return false;
	}

	public void onShowPress(final MotionEvent pEvent) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			o.onShowPress(pEvent);

	}

	public boolean onSingleTapUp(final MotionEvent pEvent) {
		if (mDirtyOverlays)
			updateOverlays();

		for (Overlay o : mOverlays)
			if (o.onSingleTapUp(pEvent))
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
