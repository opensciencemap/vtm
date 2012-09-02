/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android;

import org.mapsforge.android.mapgenerator.IMapGenerator;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ZoomControls;

/**
 * A MapZoomControls instance displays buttons for zooming in and out in a map.
 */
public class MapZoomControls {
	private static class ZoomControlsHideHandler extends Handler {
		private final ZoomControls mZoomControls;

		ZoomControlsHideHandler(ZoomControls zoomControls) {
			super();
			mZoomControls = zoomControls;
		}

		@Override
		public void handleMessage(Message message) {
			mZoomControls.hide();
		}
	}

	private static class ZoomInClickListener implements View.OnClickListener {
		private final MapView mMapView;

		ZoomInClickListener(MapView mapView) {
			mMapView = mapView;
		}

		@Override
		public void onClick(View view) {
			mMapView.zoom((byte) 1);
		}
	}

	private static class ZoomOutClickListener implements View.OnClickListener {
		private final MapView mMapView;

		ZoomOutClickListener(MapView mapView) {
			mMapView = mapView;
		}

		@Override
		public void onClick(View view) {
			mMapView.zoom((byte) -1);
		}
	}

	/**
	 * Default {@link Gravity} of the zoom controls.
	 */
	private static final int DEFAULT_ZOOM_CONTROLS_GRAVITY = Gravity.BOTTOM
			| Gravity.RIGHT;

	/**
	 * Default maximum zoom level.
	 */
	private static final byte DEFAULT_ZOOM_LEVEL_MAX = 22;

	/**
	 * Default minimum zoom level.
	 */
	private static final byte DEFAULT_ZOOM_LEVEL_MIN = 0;

	/**
	 * Message code for the handler to hide the zoom controls.
	 */
	private static final int MSG_ZOOM_CONTROLS_HIDE = 0;

	/**
	 * Horizontal padding for the zoom controls.
	 */
	private static final int ZOOM_CONTROLS_HORIZONTAL_PADDING = 5;

	/**
	 * Delay in milliseconds after which the zoom controls disappear.
	 */
	private static final long ZOOM_CONTROLS_TIMEOUT = ViewConfiguration
			.getZoomControlsTimeout();

	private boolean mGravityChanged;
	private boolean mShowMapZoomControls;
	private final ZoomControls mZoomControls;
	private int mZoomControlsGravity;
	private final Handler mZoomControlsHideHandler;
	private byte mZoomLevelMax;
	private byte mZoomLevelMin;

	MapZoomControls(Context context, final MapView mapView) {
		mZoomControls = new ZoomControls(context);
		mShowMapZoomControls = true;
		mZoomLevelMax = DEFAULT_ZOOM_LEVEL_MAX;
		mZoomLevelMin = DEFAULT_ZOOM_LEVEL_MIN;
		mZoomControls.setVisibility(View.GONE);
		mZoomControlsGravity = DEFAULT_ZOOM_CONTROLS_GRAVITY;

		mZoomControls.setOnZoomInClickListener(new ZoomInClickListener(mapView));
		mZoomControls.setOnZoomOutClickListener(new ZoomOutClickListener(mapView));
		mZoomControlsHideHandler = new ZoomControlsHideHandler(mZoomControls);

		// int wrapContent = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
		// LayoutParams layoutParams = new LayoutParams(wrapContent, wrapContent);
		// mapView.addView(zoomControls, layoutParams);
	}

	/**
	 * @return the current gravity for the placing of the zoom controls.
	 * @see Gravity
	 */
	public int getZoomControlsGravity() {
		return mZoomControlsGravity;
	}

	/**
	 * @return the maximum zoom level of the map.
	 */
	public byte getZoomLevelMax() {
		return mZoomLevelMax;
	}

	/**
	 * @return the minimum zoom level of the map.
	 */
	public byte getZoomLevelMin() {
		return mZoomLevelMin;
	}

	/**
	 * @return true if the zoom controls are visible, false otherwise.
	 */
	public boolean isShowMapZoomControls() {
		return mShowMapZoomControls;
	}

	/**
	 * @param showMapZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setShowMapZoomControls(boolean showMapZoomControls) {
		mShowMapZoomControls = false; // showMapZoomControls;
	}

	/**
	 * Sets the gravity for the placing of the zoom controls. Supported values are {@link Gravity#TOP},
	 * {@link Gravity#CENTER_VERTICAL}, {@link Gravity#BOTTOM}, {@link Gravity#LEFT}, {@link Gravity#CENTER_HORIZONTAL}
	 * and {@link Gravity#RIGHT}.
	 * 
	 * @param zoomControlsGravity
	 *            a combination of {@link Gravity} constants describing the desired placement.
	 */
	public void setZoomControlsGravity(int zoomControlsGravity) {
		if (mZoomControlsGravity != zoomControlsGravity) {
			mZoomControlsGravity = zoomControlsGravity;
			mGravityChanged = true;
		}
	}

	/**
	 * Sets the maximum zoom level of the map.
	 * <p>
	 * The maximum possible zoom level of the MapView depends also on the current {@link IMapGenerator}. For example,
	 * downloading map tiles may only be possible up to a certain zoom level. Setting a higher maximum zoom level has no
	 * effect in this case.
	 * 
	 * @param zoomLevelMax
	 *            the maximum zoom level.
	 * @throws IllegalArgumentException
	 *             if the maximum zoom level is smaller than the current minimum zoom level.
	 */
	public void setZoomLevelMax(byte zoomLevelMax) {
		if (zoomLevelMax < mZoomLevelMin) {
			throw new IllegalArgumentException();
		}
		mZoomLevelMax = zoomLevelMax;
	}

	/**
	 * Sets the minimum zoom level of the map.
	 * 
	 * @param zoomLevelMin
	 *            the minimum zoom level.
	 * @throws IllegalArgumentException
	 *             if the minimum zoom level is larger than the current maximum zoom level.
	 */
	public void setZoomLevelMin(byte zoomLevelMin) {
		if (zoomLevelMin > mZoomLevelMax) {
			throw new IllegalArgumentException();
		}
		mZoomLevelMin = zoomLevelMin;
	}

	private int calculatePositionLeft(int left, int right, int zoomControlsWidth) {
		int gravity = mZoomControlsGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
		switch (gravity) {
			case Gravity.LEFT:
				return ZOOM_CONTROLS_HORIZONTAL_PADDING;

			case Gravity.CENTER_HORIZONTAL:
				return (right - left - zoomControlsWidth) / 2;

			case Gravity.RIGHT:
				return right - left - zoomControlsWidth
						- ZOOM_CONTROLS_HORIZONTAL_PADDING;
		}

		throw new IllegalArgumentException("unknown horizontal gravity: " + gravity);
	}

	private int calculatePositionTop(int top, int bottom, int zoomControlsHeight) {
		int gravity = mZoomControlsGravity & Gravity.VERTICAL_GRAVITY_MASK;
		switch (gravity) {
			case Gravity.TOP:
				return 0;

			case Gravity.CENTER_VERTICAL:
				return (bottom - top - zoomControlsHeight) / 2;

			case Gravity.BOTTOM:
				return bottom - top - zoomControlsHeight;
		}

		throw new IllegalArgumentException("unknown vertical gravity: " + gravity);
	}

	private void showZoomControls() {
		mZoomControlsHideHandler.removeMessages(MSG_ZOOM_CONTROLS_HIDE);
		if (mZoomControls.getVisibility() != View.VISIBLE) {
			mZoomControls.show();
		}
	}

	private void showZoomControlsWithTimeout() {
		showZoomControls();
		mZoomControlsHideHandler.sendEmptyMessageDelayed(MSG_ZOOM_CONTROLS_HIDE,
				ZOOM_CONTROLS_TIMEOUT);
	}

	int getMeasuredHeight() {
		return mZoomControls.getMeasuredHeight();
	}

	int getMeasuredWidth() {
		return mZoomControls.getMeasuredWidth();
	}

	void measure(int widthMeasureSpec, int heightMeasureSpec) {
		mZoomControls.measure(widthMeasureSpec, heightMeasureSpec);
	}

	void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (!changed && !mGravityChanged) {
			return;
		}

		int zoomControlsWidth = mZoomControls.getMeasuredWidth();
		int zoomControlsHeight = mZoomControls.getMeasuredHeight();

		int positionLeft = calculatePositionLeft(left, right, zoomControlsWidth);
		int positionTop = calculatePositionTop(top, bottom, zoomControlsHeight);
		int positionRight = positionLeft + zoomControlsWidth;
		int positionBottom = positionTop + zoomControlsHeight;

		mZoomControls.layout(positionLeft, positionTop, positionRight, positionBottom);
		mGravityChanged = false;
	}

	void onMapViewTouchEvent(int action) {
		if (mShowMapZoomControls) {
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					showZoomControls();
					break;
				case MotionEvent.ACTION_CANCEL:
					showZoomControlsWithTimeout();
					break;
				case MotionEvent.ACTION_UP:
					showZoomControlsWithTimeout();
					break;
			}
		}
	}

	void onZoomLevelChange(int zoomLevel) {
		boolean zoomInEnabled = zoomLevel < mZoomLevelMax;
		boolean zoomOutEnabled = zoomLevel > mZoomLevelMin;

		mZoomControls.setIsZoomInEnabled(zoomInEnabled);
		mZoomControls.setIsZoomOutEnabled(zoomOutEnabled);
	}
}
