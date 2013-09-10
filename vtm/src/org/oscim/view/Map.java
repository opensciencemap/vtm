/*
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

import org.oscim.backend.Log;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.MapEventLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.layers.tile.vector.MapTileLoader;
import org.oscim.renderer.GLRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.tilesource.TileSource;
import org.oscim.utils.async.AsyncExecutor;

public abstract class Map {

	private static final String TAG = Map.class.getName();

	private final Layers mLayers;
	private final Viewport mViewport;
	private final MapAnimator mAnimator;

	private final MapPosition mMapPosition;
	private final AsyncExecutor mAsyncExecutor;

	private DebugSettings mDebugSettings;

	protected boolean mClearMap;
	protected final MapEventLayer mEventLayer;

	private MapTileLayer mBaseLayer;
	//private BitmapTileLayer mBackgroundLayer;

	public Map() {

		mViewport = new Viewport(this);
		mAnimator = new MapAnimator(this, mViewport);

		mMapPosition = new MapPosition();
		mLayers = new Layers();
		mAsyncExecutor = new AsyncExecutor(2);

		mDebugSettings = new DebugSettings();
		MapTileLoader.setDebugSettings(mDebugSettings);

		mEventLayer = new MapEventLayer(this);
		mLayers.add(0, mEventLayer);
	}

	public MapEventLayer getEventLayer() {
		return mEventLayer;
	}

	public MapTileLayer setBaseMap(TileSource tileSource) {
		mBaseLayer = new MapTileLayer(this);

		mBaseLayer.setTileSource(tileSource);
		mLayers.add(1, mBaseLayer);

		return mBaseLayer;
	}

	public void setBackgroundMap(BitmapTileLayer tileLayer) {
		mLayers.add(1, tileLayer);
	}

	public MapTileLayer setBaseMap(BitmapTileLayer tileLayer) {
		mLayers.add(1, tileLayer);
		return null;
	}

	private InternalRenderTheme mCurrentTheme;

	public void setTheme(InternalRenderTheme theme) {
		if (mBaseLayer == null) {
			Log.e(TAG, "No base layer set");
			throw new IllegalStateException();
		}

		if (mCurrentTheme == theme){
			Log.d(TAG, "same theme: " + theme);
			return;
		}
		IRenderTheme t = ThemeLoader.load(theme);
		if (t == null) {
			Log.e(TAG, "Invalid theme");
			throw new IllegalStateException();
		}

		mCurrentTheme = theme;
		mBaseLayer.setRenderTheme(t);
		GLRenderer.setBackgroundColor(t.getMapBackground());

		clearMap();
	}

	public void destroy() {
		mLayers.destroy();
		mAsyncExecutor.dispose();
	}

	/**
	 * Request call to onUpdate for all layers. This function can
	 * be called from any thread. Request will be handled on main
	 * thread.
	 *
	 * @param forceRedraw pass true to render next frame
	 */
	public abstract void updateMap(boolean forceRedraw);

	/**
	 * Request to render a frame. Request will be handled on main
	 * thread. Use this for animations in RenderLayers.
	 */
	public abstract void render();

	/**
	 * Post a runnable to be executed on main-thread
	 */
	public abstract boolean post(Runnable action);

	/**
	 * Post a runnable to be executed on main-thread. Execution is delayed for
	 * at least 'delay' milliseconds.
	 */
	public abstract boolean postDelayed(Runnable action, long delay);

	/**
	 * Post a task to run on a shared worker-thread. Only use for
	 * tasks running less than a second!
	 * */
	public void addTask(Runnable task){
		mAsyncExecutor.post(task);
	}

	public abstract int getWidth();

	public abstract int getHeight();


	/**
	 * Request to clear all layers before rendering next frame
	 */
	public void clearMap() {
		mClearMap = true;
	}

	/**
	 * Do not call directly! This function is run on main-loop
	 * before rendering a frame.
	 */
	public void updateLayers() {
		boolean changed = false;

		// get the current MapPosition
		changed |= mViewport.getMapPosition(mMapPosition);

		mLayers.onUpdate(mMapPosition, changed, mClearMap);
		mClearMap = false;
	}

	public void setDebugSettings(DebugSettings debugSettings) {
		mDebugSettings = debugSettings;
		//MapTileLoader.setDebugSettings(debugSettings);
	}

	public DebugSettings getDebugSettings() {
		return mDebugSettings;
	}

	public void setMapPosition(MapPosition mapPosition) {
		mViewport.setMapPosition(mapPosition);
	}

	public void getMapPosition(MapPosition mapPosition) {
		mViewport.getMapPosition(mapPosition);
	}

	/**
	 * Sets the center of the MapView and triggers a redraw.
	 *
	 * @param geoPoint
	 *            the new center point of the map.
	 */
	public void setCenter(GeoPoint geoPoint) {

		mViewport.setMapCenter(geoPoint);
		updateMap(true);
	}

	public Viewport getViewport() {
		return mViewport;
	}

	public Layers getLayers() {
		return mLayers;
	}

	/**
	 * @return estimated visible axis aligned bounding box
	 */
	public BoundingBox getBoundingBox() {
		return mViewport.getViewBox();
	}

	public MapAnimator getAnimator() {
		return mAnimator;
	}
}
