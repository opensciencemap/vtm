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

import java.util.List;

import org.oscim.backend.Log;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.Layer;
import org.oscim.layers.MapEventLayer;
import org.oscim.layers.overlay.Overlay;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.layers.tile.vector.MapTileLoader;
import org.oscim.renderer.GLRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.tilesource.TileSource;

public abstract class MapView {

	private static final String TAG = MapView.class.getName();

	//public static boolean enableClosePolygons;
	private final LayerManager mLayerManager;
	private final MapViewPosition mMapViewPosition;
	private final MapPosition mMapPosition;

	private DebugSettings mDebugSettings;

	public MapView() {

		mMapViewPosition = new MapViewPosition(this);
		mMapPosition = new MapPosition();
		mLayerManager = new LayerManager();

		// FIXME
		mDebugSettings = new DebugSettings();
		MapTileLoader.setDebugSettings(mDebugSettings);

		mLayerManager.add(0, new MapEventLayer(this));
	}
	private MapTileLayer mBaseLayer;

	public MapTileLayer setBaseMap(TileSource tileSource) {
		mBaseLayer = new MapTileLayer(this);

		mBaseLayer.setTileSource(tileSource);
		//mLayerManager.add(0, new MapEventLayer(this));

		mLayerManager.add(1, mBaseLayer);

		//mRotationEnabled = true;

		//mLayerManager.add(new GenericOverlay(this, new GridRenderLayer(this)));

		return mBaseLayer;
	}

	public void setBackgroundMap(BitmapTileLayer tileLayer) {
		mLayerManager.add(1, tileLayer);
	}

	public MapTileLayer setBaseMap(BitmapTileLayer tileLayer) {
		//mLayerManager.add(0, new MapEventLayer(this));
		mLayerManager.add(1, tileLayer);
		return null;
	}

	public void setTheme(InternalRenderTheme theme) {
		if (mBaseLayer == null){
			Log.e(TAG, "No base layer set");
			throw new IllegalStateException();
		}

		IRenderTheme t = ThemeLoader.load(theme);
		if (t == null){
			Log.e(TAG, "Invalid theme");
			throw new IllegalStateException();
		}
		mBaseLayer.setRenderTheme(t);
		GLRenderer.setBackgroundColor(t.getMapBackground());
	}

	public void destroy() {
		mLayerManager.destroy();
	}

	/**
	 * Request call to onUpdate for all layers. This function can
	 * be called from any thread. Request will be handled on main
	 * thread.
	 *
	 * @param forceRedraw pass true to render next frame
	 */
	public abstract void updateMap(boolean forceRedraw);


	public abstract int getWidth();
	public abstract int getHeight();
	public abstract boolean postRunnable(Runnable runnable);

	protected boolean mClearMap;

	/**
	 * Request to clear all layers before rendering next frame
	 */
	public void clearMap() {
		mClearMap = true;
	}

	/**
	 * Request to render a frame. Request will be handled on main
	 * thread. Use this for animations.
	 */
	public abstract void render();

	/**
	 * Do not call directly! This function is run on main-loop
	 * before rendering a frame.
	 */
	public void updateLayers() {
		boolean changed = false;

		// get the current MapPosition
		changed |= mMapViewPosition.getMapPosition(mMapPosition);

		mLayerManager.onUpdate(mMapPosition, changed, mClearMap);
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
		updateMap(true);
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
		return this.getLayerManager();
	}

	public LayerManager getLayerManager() {
		return mLayerManager;
	}

	/**
	 * @return estimated visible axis aligned bounding box
	 */
	public BoundingBox getBoundingBox() {
		return mMapViewPosition.getViewBox();
	}




}
