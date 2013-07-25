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
import org.oscim.backend.AssetAdapter;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.core.Tile;
import org.oscim.view.MapView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidGL20;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class AndroidMapView extends RelativeLayout {

	final static String TAG = AndroidMapView.class.getName();

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;

	public boolean mRotationEnabled = false;
	public boolean mCompassEnabled = false;
	public boolean enablePagedFling = false;

	private final Compass mCompass;

	private int mWidth;
	private int mHeight;


	private final MapView mMapView;

	final GLView mGLView;
	boolean mPausing = false;
	boolean mInitialized = false;

	static {
		System.loadLibrary("vtm-jni");
		//System.loadLibrary("tessellate");

		CanvasAdapter.g = AndroidGraphics.INSTANCE;
		GLAdapter.g = new AndroidGL20();
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

		AssetAdapter.g = new AndroidAssetAdapter(context);

		this.setWillNotDraw(true);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		CanvasAdapter.dpi = (int)Math.max(metrics.xdpi, metrics.ydpi);

		// TODO make this dpi dependent
		Tile.SIZE = 400;

		MapActivity mapActivity = (MapActivity) context;

		final AndroidMapView m = this;

		mMapView = new MapView(){

			boolean mWaitRedraw;

			private final Runnable mRedrawRequest = new Runnable() {
				@Override
				public void run() {
					mWaitRedraw = false;
					redrawMapInternal(false);
				}
			};

			@Override
			public int getWidth() {
				return m.getWidth();
			}

			@Override
			public int getHeight() {
				return m.getHeight();
			}

			@Override
			public void updateMap(boolean requestRender) {
				if (requestRender && !mClearMap && !mPausing && mInitialized)
					mGLView.requestRender();

				if (!mWaitRedraw) {
					mWaitRedraw = true;
					post(mRedrawRequest);
				}
			}

			@Override
			public void render() {
				if (mClearMap)
					updateMap(false);
				else
					mGLView.requestRender();
			}

			void redrawMapInternal(boolean forceRedraw) {
				boolean clear = mClearMap;

				if (forceRedraw && !clear)
					mGLView.requestRender();

				updateLayers();

				if (clear) {
					mGLView.requestRender();
					mClearMap = false;
				}
			}
		};

		mGLView = new GLView(context, mMapView);

		mCompass = new Compass(mapActivity, mMapView);

		mapActivity.registerMapView(mMapView);

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		addView(mGLView, params);

		mMapView.clearMap();
		mMapView.updateMap(false);
	}

	public MapView getMap() {
		return mMapView;
	}

	public void onStop() {
		Log.d(TAG, "onStop");
		//mLayerManager.destroy();
	}


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

		return mMapView.getLayerManager().handleMotionEvent(mMotionEvent);
	}

	// synchronized ???
	@Override
	protected void onSizeChanged(int width, int height,
			int oldWidth, int oldHeight) {
		Log.d(TAG, "onSizeChanged: " + width + "x" + height);

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		mWidth = width;
		mHeight = height;

		mInitialized = (mWidth > 0 && mHeight > 0);

		if (mInitialized)
			mMapView.getMapViewPosition().setViewport(width, height);
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
