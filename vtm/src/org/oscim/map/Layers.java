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

package org.oscim.map;

import java.util.AbstractList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.oscim.backend.Log;
import org.oscim.backend.input.KeyEvent;
import org.oscim.backend.input.MotionEvent;
import org.oscim.core.MapPosition;
import org.oscim.layers.InputLayer;
import org.oscim.layers.Layer;
import org.oscim.renderer.LayerRenderer;

public class Layers extends AbstractList<Layer> {
	private final static String TAG = Layers.class.getName();
	private final static boolean debugInput = false;

	private final CopyOnWriteArrayList<Layer> mLayerList;

	Layers() {
		mLayerList = new CopyOnWriteArrayList<Layer>();
	}

	@Override
	public synchronized Layer get(int index) {
		return mLayerList.get(index);
	}

	@Override
	public synchronized int size() {
		return mLayerList.size();
	}

	@Override
	public synchronized void add(int index, Layer element) {
		mLayerList.add(index, element);
		mDirtyLayers = true;
	}

	@Override
	public synchronized Layer remove(int index) {
		mDirtyLayers = true;
		return mLayerList.remove(index);
	}

	@Override
	public synchronized Layer set(int index, Layer element) {
		mDirtyLayers = true;
		return mLayerList.set(index, element);
	}

	private boolean mDirtyLayers;
	private LayerRenderer[] mLayerRenderer;

	public LayerRenderer[] getLayerRenderer() {
		if (mDirtyLayers)
			updateLayers();

		return mLayerRenderer;
	}

	public void onUpdate(MapPosition mapPosition, boolean changed, boolean clear) {
		if (mDirtyLayers)
			updateLayers();

		for (Layer l : mLayers)
			l.onUpdate(mapPosition, changed, clear);
	}

	public void destroy() {
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

		for (int i = 0, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);

			if (o.getRenderer() != null)
				numRenderLayers++;

			if (o instanceof InputLayer)
				numInputLayers++;

			mLayers[i] = o;
		}

		mLayerRenderer = new LayerRenderer[numRenderLayers];
		mInputLayer = new InputLayer[numInputLayers];

		for (int i = 0, cntR = 0, cntI = 1, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);
			LayerRenderer l = o.getRenderer();
			if (l != null)
				mLayerRenderer[cntR++] = l;

			if (o instanceof InputLayer) {
				// sort from top to bottom, so that highest layers
				// process event first.
				mInputLayer[numInputLayers - cntI] = (InputLayer) o;
				cntI++;
			}
		}

		mDirtyLayers = false;
	}

	//private boolean mCancelGesture;

	public boolean handleMotionEvent(MotionEvent e) {
		//boolean handleGesture = true;

		//if (mCancelGesture) {
		//	int action = e.getAction();
		//	handleGesture = (action == MotionEvent.ACTION_CANCEL ||
		//			action == MotionEvent.ACTION_UP);
		//}

		//if (handleGesture) {
		//	if (mGestureDetector.onTouchEvent(e))
		//		return true;
		//
		//	mCancelGesture = false;
		//}

		if (onTouchEvent(e))
			return true;

		return false;
	}

	///**
	// * Call this to not foward events to generic GestureDetector until
	// * next ACTION_UP or ACTION_CANCEL event. - Use with care for the
	// * case that an InputLayer recognized the start of its gesture and
	// * does further processing in only onTouch callback.
	// */
	//public void cancelGesture() {
	//	mCancelGesture = true;
	//}

	public boolean onTouchEvent(MotionEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer) {
			if (o.onTouchEvent(event)) {
				if (debugInput)
					Log.d(TAG, "onTouch\t\t" + o.getClass());
				return true;
			}
		}
		return false;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onKeyDown(keyCode, event))
				return true;

		return false;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onKeyUp(keyCode, event))
				return true;

		return false;
	}

	public boolean onTrackballEvent(MotionEvent event) {
		if (mDirtyLayers)
			updateLayers();

		for (InputLayer o : mInputLayer)
			if (o.onTrackballEvent(event))
				return true;

		return false;
	}

	//	/* GestureDetector.OnDoubleTapListener */
	//
	//	public boolean onDoubleTap(MotionEvent e) {
	//
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer) {
	//			if (o.onDoubleTap(e)) {
	//				if (debugInput)
	//					Log.d(TAG, "onDoubleTap\t" + o.getClass());
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//
	//	public boolean onDoubleTapEvent(MotionEvent e) {
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer) {
	//			if (o.onDoubleTapEvent(e)) {
	//				if (debugInput)
	//					Log.d(TAG, "onDoubleTapEvent\t" + o.getClass());
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//
	//	public boolean onSingleTapConfirmed(MotionEvent e) {
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer) {
	//			if (o.onSingleTapConfirmed(e)) {
	//				if (debugInput)
	//					Log.d(TAG, "onSingleTapConfirmed\tt" + o.getClass());
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//
	//	/* OnGestureListener */
	//	public boolean onDown(MotionEvent e) {
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer) {
	//			if (o.onDown(e)) {
	//				if (debugInput)
	//					Log.d(TAG, "onDown\t" + o.getClass());
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//
	//	public void onLongPress(MotionEvent e) {
	//		if (mCancelGesture)
	//			return;
	//
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer)
	//			if (o.onLongPress(e))
	//				return;
	//	}
	//
	//	public boolean onScroll(MotionEvent e1, MotionEvent e2,
	//			float dx, float dy) {
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer) {
	//			if (o.onScroll(e1, e2, dx, dy)) {
	//				if (debugInput)
	//					Log.d(TAG, "onScroll\t" + o.getClass());
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
	//
	//	public void onShowPress(MotionEvent e) {
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer)
	//			o.onShowPress(e);
	//
	//	}
	//
	//	public boolean onSingleTapUp(MotionEvent e) {
	//		if (mDirtyLayers)
	//			updateLayers();
	//
	//		for (InputLayer o : mInputLayer) {
	//			if (o.onSingleTapUp(e)) {
	//				if (debugInput)
	//					Log.d(TAG, "onSingleTapUp\t" + o.getClass());
	//				return true;
	//			}
	//		}
	//		return false;
	//	}
}
