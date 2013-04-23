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

package org.oscim.view;

import java.util.AbstractList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.oscim.core.MapPosition;
import org.oscim.core.PointF;
import org.oscim.layers.InputLayer;
import org.oscim.layers.Layer;
import org.oscim.overlay.Overlay.Snappable;
import org.oscim.renderer.overlays.RenderOverlay;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class LayerManager extends AbstractList<Layer> implements OnGestureListener,
		OnDoubleTapListener {
	private final static String TAG = LayerManager.class.getName();

	private final GestureDetector mGestureDetector;

	private final CopyOnWriteArrayList<Layer> mLayerList;

	LayerManager(Context context) {
		mLayerList = new CopyOnWriteArrayList<Layer>();
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setOnDoubleTapListener(this);
	}

	@Override
	public synchronized Layer get(final int pIndex) {
		return mLayerList.get(pIndex);
	}

	@Override
	public synchronized int size() {
		return mLayerList.size();
	}

	@Override
	public synchronized void add(final int pIndex, final Layer pElement) {
		mLayerList.add(pIndex, pElement);
		mDirtyLayers = true;
	}

	@Override
	public synchronized Layer remove(final int pIndex) {
		mDirtyLayers = true;
		return mLayerList.remove(pIndex);
	}

	@Override
	public synchronized Layer set(final int pIndex, final Layer pElement) {
		mDirtyLayers = true;
		return mLayerList.set(pIndex, pElement);
	}

	public boolean handleMotionEvent(MotionEvent e) {

		if (mGestureDetector.onTouchEvent(e))
			return true;

		if (onTouchEvent(e))
			return true;

		return false;
	}

	private boolean mDirtyLayers;
	private RenderOverlay[] mDrawLayers;

	public RenderOverlay[] getRenderLayers() {
		if (mDirtyLayers)
			updateLayers();

		return mDrawLayers;
	}

	public void onDetach() {
		if (mDirtyLayers)
			updateLayers();

		for (Layer o : mLayers)
			o.onDetach();
	}

	Layer[] mLayers;
	InputLayer[] mInputLayer;

	private synchronized void updateLayers() {
		if (!mDirtyLayers)
			return;

		mLayers = new Layer[mLayerList.size()];

		int numRenderLayers = 0;
		int numInputLayers = 0;

		Log.d(TAG, "update layers:");

		for (int i = 0, n = mLayerList.size(); i < n; i++) {

			Layer o = mLayerList.get(i);

			Log.d(TAG, "\t" + o.getClass().getName());

			if (o.getLayer() != null)
				numRenderLayers++;

			if (o instanceof InputLayer)
				numInputLayers++;

			mLayers[i] = o;
		}

		mDrawLayers = new RenderOverlay[numRenderLayers];
		mInputLayer = new InputLayer[numInputLayers];

		for (int i = 0, cntR = 0, cntI = 1, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);
			RenderOverlay l = o.getLayer();
			if (l != null)
				mDrawLayers[cntR++] = l;

			if (o instanceof InputLayer) {
				// sort from top to bottom, so that highest layers
				// process event first.
				mInputLayer[numInputLayers - cntI] = (InputLayer) o;
				cntI++;
			}
		}

		mDirtyLayers = false;
	}

	public boolean onTouchEvent(final MotionEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onTouchEvent(event))
				return true;

		return false;
	}

	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onKeyDown(keyCode, event))
				return true;

		return false;
	}

	public boolean onKeyUp(final int keyCode, final KeyEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onKeyUp(keyCode, event))
				return true;

		return false;
	}

	public boolean onTrackballEvent(final MotionEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onTrackballEvent(event))
				return true;

		return false;
	}

	public boolean onSnapToItem(final int x, final int y, final PointF snapPoint) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o instanceof Snappable)
				if (((Snappable) o).onSnapToItem(x, y, snapPoint))
					return true;

		return false;
	}

	/* GestureDetector.OnDoubleTapListener */

	@Override
	public boolean onDoubleTap(final MotionEvent e) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onDoubleTap(e))
				return true;

		return false;
	}

	@Override
	public boolean onDoubleTapEvent(final MotionEvent e) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onDoubleTapEvent(e))
				return true;

		return false;
	}

	@Override
	public boolean onSingleTapConfirmed(final MotionEvent e) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onSingleTapConfirmed(e))
				return true;

		return false;
	}

	/* OnGestureListener */

	@Override
	public boolean onDown(final MotionEvent pEvent) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onDown(pEvent))
				return true;

		return false;
	}

	@Override
	public boolean onFling(final MotionEvent pEvent1, final MotionEvent pEvent2,
			final float pVelocityX, final float pVelocityY) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onFling(pEvent1, pEvent2, pVelocityX, pVelocityY))
				return true;

		return false;
	}

	@Override
	public void onLongPress(final MotionEvent pEvent) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onLongPress(pEvent))
				return;
	}

	@Override
	public boolean onScroll(final MotionEvent pEvent1, final MotionEvent pEvent2,
			final float pDistanceX, final float pDistanceY) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onScroll(pEvent1, pEvent2, pDistanceX, pDistanceY))
				return true;

		return false;
	}

	@Override
	public void onShowPress(final MotionEvent pEvent) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			o.onShowPress(pEvent);

	}

	@Override
	public boolean onSingleTapUp(final MotionEvent pEvent) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onSingleTapUp(pEvent))
				return true;

		return false;
	}

	public void onUpdate(MapPosition mapPosition, boolean changed) {
		if (mDirtyLayers)
			updateLayers();

		for (Layer l : mLayers)
			l.onUpdate(mapPosition, changed);
	}

	public void destroy() {
		if (mDirtyLayers)
			updateLayers();

		for (Layer l : mLayers) {
			l.destroy();
		}
	}

	// /**
	// * Gets the optional TilesLayer class.
	// *
	// * @return the tilesLayer
	// */
	// public TilesLayer getTilesLayer() {
	// return mTilesLayer;
	// }
	//
	// /**
	// * Sets the optional TilesLayer class. If set, this overlay will be
	// drawn before all other
	// * overlays and will not be included in the editable list of overlays and
	// can't be cleared
	// * except by a subsequent call to setTilesLayer().
	// *
	// * @param tilesLayer
	// * the tilesLayer to set
	// */
	// public void setTilesLayer(final TilesLayer tilesLayer) {
	// mTilesLayer = tilesLayer;
	// }

	//	public void onDraw(final Canvas c, final MapView pMapView) {
	//		// if ((mTilesLayer != null) && mTilesLayer.isEnabled()) {
	//		// mTilesLayer.draw(c, pMapView, true);
	//		// }
	//		//
	//		// if ((mTilesLayer != null) && mTilesLayer.isEnabled()) {
	//		// mTilesLayer.draw(c, pMapView, false);
	//		// }
	//
	//		for (final Layer overlay : mLayerList) {
	//			if (overlay.isEnabled()) {
	//				overlay.draw(c, pMapView, true);
	//			}
	//		}
	//
	//		for (final Layer overlay : mLayerList) {
	//			if (overlay.isEnabled()) {
	//				overlay.draw(c, pMapView, false);
	//			}
	//		}
	//
	//	}

	// ** Options Menu **//

	// public void setOptionsMenusEnabled(final boolean pEnabled) {
	// for (final Layer overlay : mLayerList) {
	// if ((overlay instanceof ILayerMenuProvider)
	// && ((ILayerMenuProvider) overlay).isOptionsMenuEnabled()) {
	// ((ILayerMenuProvider) overlay).setOptionsMenuEnabled(pEnabled);
	// }
	// }
	// }
	//
	// public boolean onCreateOptionsMenu(final Menu pMenu, final int
	// menuIdOffset,
	// final MapView mapView) {
	// boolean result = true;
	// for (final Layer overlay : this.overlaysReversed()) {
	// if ((overlay instanceof ILayerMenuProvider)
	// && ((ILayerMenuProvider) overlay).isOptionsMenuEnabled()) {
	// result &= ((ILayerMenuProvider) overlay).onCreateOptionsMenu(pMenu,
	// menuIdOffset,
	// mapView);
	// }
	// }
	//
	// if ((mTilesLayer != null) && (mTilesLayer instanceof
	// ILayerMenuProvider)
	// && ((ILayerMenuProvider) mTilesLayer).isOptionsMenuEnabled()) {
	// result &= mTilesLayer.onCreateOptionsMenu(pMenu, menuIdOffset,
	// mapView);
	// }
	//
	// return result;
	// }
	//
	// public boolean onPrepareOptionsMenu(final Menu pMenu, final int
	// menuIdOffset,
	// final MapView mapView) {
	// for (final Layer overlay : this.overlaysReversed()) {
	// if ((overlay instanceof ILayerMenuProvider)
	// && ((ILayerMenuProvider) overlay).isOptionsMenuEnabled()) {
	// ((ILayerMenuProvider) overlay).onPrepareOptionsMenu(pMenu,
	// menuIdOffset, mapView);
	// }
	// }
	//
	// if ((mTilesLayer != null) && (mTilesLayer instanceof
	// ILayerMenuProvider)
	// && ((ILayerMenuProvider) mTilesLayer).isOptionsMenuEnabled()) {
	// mTilesLayer.onPrepareOptionsMenu(pMenu, menuIdOffset, mapView);
	// }
	//
	// return true;
	// }
	//
	// public boolean onOptionsItemSelected(final MenuItem item, final int
	// menuIdOffset,
	// final MapView mapView) {
	// for (final Layer overlay : this.overlaysReversed()) {
	// if ((overlay instanceof ILayerMenuProvider)
	// && ((ILayerMenuProvider) overlay).isOptionsMenuEnabled()
	// && ((ILayerMenuProvider) overlay).onOptionsItemSelected(item,
	// menuIdOffset,
	// mapView)) {
	// return true;
	// }
	// }
	//
	// if ((mTilesLayer != null)
	// && (mTilesLayer instanceof ILayerMenuProvider)
	// && ((ILayerMenuProvider) mTilesLayer).isOptionsMenuEnabled()
	// && ((ILayerMenuProvider) mTilesLayer).onOptionsItemSelected(item,
	// menuIdOffset,
	// mapView)) {
	// return true;
	// }
	//
	// return false;
	// }
}
