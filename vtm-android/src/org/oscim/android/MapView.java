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
import org.oscim.android.input.GestureHandler;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.widget.RelativeLayout;

public class MapView extends RelativeLayout {

	static {
		System.loadLibrary("vtm-jni");
	}

	static final Logger log = LoggerFactory.getLogger(MapView.class);

	protected final AndroidMap mMap;
	protected final GestureDetector mGestureDetector;
	protected final AndroidMotionEvent mMotionEvent;

	public MapView(Context context) {
		this(context, null);
	}

	public MapView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		this.setWillNotDraw(true);
		this.setClickable(true);
		this.setFocusable(true);

		AndroidGraphics.init();
		AndroidAssets.init(context);
		GLAdapter.init(new AndroidGL());

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		CanvasAdapter.dpi = (int) Math.max(metrics.xdpi, metrics.ydpi);

		mMap = new AndroidMap(this);

		if (context instanceof MapActivity)
			((MapActivity) context).registerMapView(this);

		mMap.clearMap();
		mMap.updateMap(false);

		GestureHandler gestureHandler = new GestureHandler(mMap);
		mGestureDetector = new GestureDetector(context, gestureHandler);
		mGestureDetector.setOnDoubleTapListener(gestureHandler);

		mMotionEvent = new AndroidMotionEvent();
	}

	public void onStop() {

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

		mMap.input.fire(null, mMotionEvent.wrap(motionEvent));
		return true;
	}

	@Override
	protected void onSizeChanged(int width, int height,
	        int oldWidth, int oldHeight) {

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		if (width > 0 && height > 0)
			mMap.viewport().setScreenSize(width, height);
	}

	public Map map() {
		return mMap;
	}
}
