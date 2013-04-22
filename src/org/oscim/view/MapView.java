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
package org.oscim.view;

import java.util.List;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.database.MapOptions;
import org.oscim.layers.Layer;
import org.oscim.layers.tile.MapTileLayer;
import org.oscim.layers.tile.MapTileLoader;
import org.oscim.overlay.BuildingOverlay;
import org.oscim.overlay.LabelingOverlay;
import org.oscim.overlay.Overlay;
import org.oscim.renderer.GLView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class MapView extends RelativeLayout {

	final static String TAG = MapView.class.getName();

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;

	public boolean mRotationEnabled = false;
	public boolean mCompassEnabled = false;
	public boolean enablePagedFling = false;

	private final GLView mGLView;

	private final LayerManager mLayerManager;
	private final MapViewPosition mMapViewPosition;
	private final MapPosition mMapPosition;

	private final Compass mCompass;

	private DebugSettings mDebugSettings;

	private int mWidth;
	private int mHeight;

	// FIXME: keep until old pbmap reader is removed
	public static boolean enableClosePolygons = false;

	public static float dpi;

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

		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException(
					"context is not an instance of MapActivity");
		}

		this.setWillNotDraw(true);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		dpi = Math.max(metrics.xdpi, metrics.ydpi);

		// TODO make this dpi dependent
		Tile.SIZE = 400;

		MapActivity mapActivity = (MapActivity) context;

		mMapViewPosition = new MapViewPosition(this);
		mMapPosition = new MapPosition();

		mLayerManager = new LayerManager(context);

		mCompass = new Compass(mapActivity, this);

		mGLView = new GLView(context, this);

		mDebugSettings = new DebugSettings();

		// FIXME
		MapTileLoader.setDebugSettings(mDebugSettings);

		mapActivity.registerMapView(this);

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		addView(mGLView, params);

		redrawMap(false);
	}

	public MapTileLayer setBaseMap(MapOptions options) {
		MapTileLayer baseLayer = new MapTileLayer(this);

		baseLayer.setMapDatabase(options);

		mLayerManager.add(0, new MapEventLayer(this));

		mLayerManager.add(1, baseLayer);

		mRotationEnabled = true;

		//mLayerManager.add(new GenericOverlay(this, new GridOverlay(this)));
		mLayerManager.add(new BuildingOverlay(this, baseLayer.getTileLayer()));
		mLayerManager.add(new LabelingOverlay(this, baseLayer.getTileLayer()));

		return baseLayer;
	}

	void destroy() {
		mLayerManager.destroy();
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

	public void onStop() {
		Log.d(TAG, "onStop");
		//mLayerManager.destroy();
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {

		if (!isClickable())
			return false;

		return mLayerManager.handleMotionEvent(motionEvent);
	}

	@Override
	protected synchronized void onSizeChanged(int width, int height,
			int oldWidth, int oldHeight) {
		Log.d(TAG, "onSizeChanged: " + width + "x" + height);

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		mWidth = width;
		mHeight = height;

		if (width != 0 && height != 0)
			mMapViewPosition.setViewport(width, height);
	}

	boolean mWaitRedraw;

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
	 * to onUpdate() to all Layers.
	 *
	 * @param requestRender
	 *            also request to draw a frame
	 */
	public void redrawMap(boolean requestRender) {
		if (requestRender) {
			if (!(mPausing || mWidth == 0 || mHeight == 0))
				mGLView.requestRender();
		}
		if (!mWaitRedraw) {
			mWaitRedraw = true;
			post(mRedrawRequest);
		}
	}

	/**
	 * Request to render a frame. Use this for animations.
	 */
	public void render() {
		mGLView.requestRender();
	}

	/**
	 * Calculates all necessary tiles and adds jobs accordingly.
	 *
	 * @param forceRedraw TODO
	 */
	void redrawMapInternal(boolean forceRedraw) {
		boolean changed = false;

		if (mPausing || mWidth == 0 || mHeight == 0)
			return;

		if (forceRedraw) {
			mGLView.requestRender();
			changed = true;
		}

		// get the current MapPosition
		changed |= mMapViewPosition.getMapPosition(mMapPosition);

		mLayerManager.onUpdate(mMapPosition, changed);
	}

	/**
	 * @param debugSettings
	 *            the new DebugSettings for this MapView.
	 */
	public void setDebugSettings(DebugSettings debugSettings) {
		mDebugSettings = debugSettings;
		MapTileLoader.setDebugSettings(debugSettings);
	}

	/**
	 * @return the debug settings which are used in this MapView.
	 */
	public DebugSettings getDebugSettings() {
		return mDebugSettings;
	}

	public void setMapPosition(MapPosition mapPosition) {
		mMapViewPosition.setMapPosition(mapPosition);
	}

	/**
	 * Sets the center of the MapView and triggers a redraw.
	 *
	 * @param geoPoint
	 *            the new center point of the map.
	 */
	public void setCenter(GeoPoint geoPoint) {

		mMapViewPosition.setMapCenter(geoPoint);
		redrawMap(true);
	}

	/**
	 * @return MapViewPosition
	 */
	public MapViewPosition getMapViewPosition() {
		return mMapViewPosition;
	}

	/**
	 * You can add/remove/reorder your Overlays using the List of
	 * {@link Overlay}. The first (index 0) Overlay gets drawn first, the one
	 * with the highest as the last one.
	 *
	 * @return ...
	 */
	public List<Layer> getOverlays() {
		return this.getOverlayManager();
	}

	public LayerManager getOverlayManager() {
		return mLayerManager;
	}

	/**
	 * @return estimated visible axis aligned bounding box
	 */
	public BoundingBox getBoundingBox() {
		return mMapViewPosition.getViewBox();
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

}
