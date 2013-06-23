/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.android;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.android.input.AndroidMotionEvent;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.Log;
import org.oscim.core.Tile;
import org.oscim.view.MapRenderCallback;
import org.oscim.view.MapView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.RelativeLayout;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class AndroidMapView extends RelativeLayout implements MapRenderCallback {

	final static String TAG = AndroidMapView.class.getName();

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;

	public boolean mRotationEnabled = false;
	public boolean mCompassEnabled = false;
	public boolean enablePagedFling = false;

	private final GLView mGLView;
	private final Compass mCompass;

	private int mWidth;
	private int mHeight;
	private boolean mInitialized;


	private final MapView mMapView;

	static {
		CanvasAdapter.g = AndroidGraphics.INSTANCE;
		Log.logger = new AndroidLog();
	}

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of
	 *             {@link MapActivity} .
	 */
	public AndroidMapView(Context context) {
		this(context, null);
	}

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @param attributeSet
	 *            a set of attributes.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of
	 *             {@link MapActivity} .
	 */

	public AndroidMapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException(
					"context is not an instance of MapActivity");
		}
		//CanvasAdapter.g = AndroidCanvas;

		this.setWillNotDraw(true);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		CanvasAdapter.dpi = (int)Math.max(metrics.xdpi, metrics.ydpi);

		// TODO make this dpi dependent
		Tile.SIZE = 400;

		MapActivity mapActivity = (MapActivity) context;

		mMapView = new MapView(this);

		mGLView = new GLView(context, mMapView);

		mCompass = new Compass(mapActivity, mMapView);

		mapActivity.registerMapView(mMapView);

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		addView(mGLView, params);

		clearMap();
		updateMap(false);
	}

	public MapView getMap() {
		return mMapView;
	}

	public void onStop() {
		Log.d(TAG, "onStop");
		//mLayerManager.destroy();
	}

	private boolean mPausing = false;

	void onPause() {
		mPausing = true;

		if (this.mCompassEnabled)
			mCompass.disable();

	}

	void onResume() {
		if (this.mCompassEnabled)
			mCompass.enable();

		mPausing = false;
	}

	AndroidMotionEvent mMotionEvent = new AndroidMotionEvent();

	@Override
	public boolean onTouchEvent(android.view.MotionEvent motionEvent) {

		if (!isClickable())
			return false;

		mMotionEvent.wrap(motionEvent);

		//return mMapView.handleMotionEvent(mMotionEvent);

		return mMapView.getLayerManager().handleMotionEvent(mMotionEvent);
	}

	@Override
	protected synchronized void onSizeChanged(int width, int height,
			int oldWidth, int oldHeight) {
		Log.d(TAG, "onSizeChanged: " + width + "x" + height);

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		mWidth = width;
		mHeight = height;

		mInitialized = (mWidth > 0 && mHeight > 0);

		if (mInitialized)
			mMapView.getMapViewPosition().setViewport(width, height);
	}

	/* private */boolean mWaitRedraw;

	private final Runnable mRedrawRequest = new Runnable() {
		@Override
		public void run() {
			mWaitRedraw = false;
			redrawMapInternal(false);
		}
	};

	/**
	 * Request to redraw the map when a global state like position,
	 * datasource or theme has changed. This will trigger a call
	 * to onUpdate() for all Layers.
	 *
	 * @param requestRender
	 *            also request to draw a frame
	 */
	@Override
	public void updateMap(boolean requestRender) {
		if (requestRender && !mClearMap && !mPausing && mInitialized)
			mGLView.requestRender();

		if (!mWaitRedraw) {
			mWaitRedraw = true;
			post(mRedrawRequest);
		}
	}

	private boolean mClearMap;

	public void clearMap() {
		mClearMap = true;
	}

	/**
	 * Request to render a frame. Use this for animations.
	 */
	@Override
	public void renderMap() {
		if (mClearMap)
			updateMap(false);
		else
			mGLView.requestRender();
	}

	/**
	 * Update all Layers on Main thread.
	 *
	 * @param forceRedraw also render frame
	 *            FIXME (does nothing atm)
	 */
	void redrawMapInternal(boolean forceRedraw) {

		if (forceRedraw && !mClearMap)
			mGLView.requestRender();

		mMapView.updateLayers();

		if (mClearMap) {
			mGLView.requestRender();
			mClearMap = false;
		}
	}

	public void enableRotation(boolean enable) {
		mRotationEnabled = enable;

		if (enable) {
			enableCompass(false);
		}
	}

	public void enableCompass(boolean enable) {
		if (enable == mCompassEnabled)
			return;

		mCompassEnabled = enable;

		if (enable)
			enableRotation(false);

		if (enable)
			mCompass.enable();
		else
			mCompass.disable();
	}

	public boolean getCompassEnabled() {
		return mCompassEnabled;
	}

	public boolean getRotationEnabled() {
		return mRotationEnabled;
	}

	public void destroy() {
		Log.d(TAG, "TODO Auto-generated method stub");

	}

}
