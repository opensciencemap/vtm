/*
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
import org.oscim.event.Gesture;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class MapView extends RelativeLayout {

	static {
		System.loadLibrary("vtm-jni");
	}

	static final Logger log = LoggerFactory.getLogger(MapView.class);

	protected final AndroidMap mMap;
	protected final GestureDetector mGestureDetector;
	protected final AndroidMotionEvent mMotionEvent;

	protected int mWidth;
	protected int mHeight;

	public MapView(Context context) {
		this(context, null);
	}

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

		mMap = new AndroidMap(this);

		if (context instanceof MapActivity)
			((MapActivity) context).registerMapView(this);

		mMap.clearMap();
		mMap.updateMap(false);

		mMotionEvent = new AndroidMotionEvent();
		mGestureDetector = new GestureDetector(context, new OnGestureListener() {
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return mMap.handleGesture(Gesture.TAP, mMotionEvent.wrap(e));
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
		mGestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				return mMap.handleGesture(Gesture.DOUBLE_TAP, mMotionEvent.wrap(e));
			}
		});
	}

	public Map getMap() {
		return mMap;
	}

	public void onStop() {
		log.debug("onStop");
	}

	void onPause() {
		mMap.pause(true);
	}

	void onResume() {
		mMap.pause(false);
	}

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

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		mWidth = width;
		mHeight = height;

		if (mWidth > 0 && mHeight > 0)
			mMap.viewport().setScreenSize(width, height);
	}
}
