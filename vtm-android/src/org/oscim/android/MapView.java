/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
import org.oscim.android.gl.AndroidGL;
import org.oscim.android.input.AndroidMotionEvent;
import org.oscim.backend.AssetAdapter;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class MapView extends RelativeLayout {

	static final Logger log = LoggerFactory.getLogger(MapView.class);

	static {
		System.loadLibrary("vtm-jni");
	}

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;

	public boolean mRotationEnabled = false;
	public boolean mCompassEnabled = false;
	public boolean enablePagedFling = false;

	private final Compass mCompass;
	private final GestureDetector mGestureDetector;

	private int mWidth;
	private int mHeight;

	final AndroidMap mMap;

	boolean mInitialized = false;

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of
	 *             {@link MapActivity} .
	 */
	public MapView(Context context) {
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

	public MapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		CanvasAdapter.g = AndroidGraphics.INSTANCE;
		AssetAdapter.g = new AndroidAssetAdapter(context);
		GLAdapter.g = new AndroidGL();

		this.setWillNotDraw(true);
		this.setClickable(true);
		this.setFocusable(true);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		CanvasAdapter.dpi = (int) Math.max(metrics.xdpi, metrics.ydpi);

		// TODO make this dpi dependent
		Tile.SIZE = 400;

		mMap = new AndroidMap(this);
		mCompass = new Compass(context, mMap);

		mMap.clearMap();
		mMap.updateMap(false);

		mGestureDetector = new GestureDetector(context, new OnGestureListener() {
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return  mMap.handleGesture(Gesture.TAP, mMotionEvent.wrap(e));
			}

			@Override
			public void onShowPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				mMap.handleGesture(Gesture.LONG_PRESS, mMotionEvent.wrap(e));
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return mMap.handleGesture(Gesture.PRESS, mMotionEvent.wrap(e));
			}
		});
	}

	public Map getMap() {
		return mMap;
	}

	public void onStop() {
		log.debug("onStop");
		//mMap.destroy();
	}

	void onPause() {
		mMap.pause(true);

		if (this.mCompassEnabled)
			mCompass.disable();

	}

	void onResume() {
		if (this.mCompassEnabled)
			mCompass.enable();

		mMap.pause(false);
	}

	final AndroidMotionEvent mMotionEvent = new AndroidMotionEvent();

	@Override
	public boolean onTouchEvent(android.view.MotionEvent motionEvent) {

		if (!isClickable())
			return false;

		if (mGestureDetector.onTouchEvent(motionEvent))
			return true;

		mMap.handleMotionEvent(mMotionEvent.wrap(motionEvent));

		return true;
	}

	@Override
	protected void onSizeChanged(int width, int height,
	        int oldWidth, int oldHeight) {
		log.debug("onSizeChanged: " + width + "x" + height);

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		mWidth = width;
		mHeight = height;

		mInitialized = (mWidth > 0 && mHeight > 0);

		if (mInitialized)
			mMap.getViewport().setViewport(width, height);
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
		log.debug("TODO Auto-generated method stub");

	}

}
