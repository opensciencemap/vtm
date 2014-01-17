/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.map;

import java.util.LinkedHashSet;
import java.util.Set;

import org.oscim.core.MapPosition;
import org.oscim.event.Gesture;
import org.oscim.event.GestureDetector;
import org.oscim.event.MotionEvent;
import org.oscim.layers.MapEventLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.source.TileSource;
import org.oscim.utils.async.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Map {

	static final Logger log = LoggerFactory.getLogger(Map.class);

	/**
	 * Listener interface for map update notifications.
	 * Layers implementing this interface they will be automatically
	 * regiseter when the layer is added to the map and unregistered when
	 * the layer is removed.
	 */
	public interface UpdateListener {
		void onMapUpdate(MapPosition mapPosition, boolean positionChanged, boolean clear);
	}

	/**
	 * Listener interface for input events.
	 * Layers implementing this interface they will be automatically
	 * regiseter when the layer is added to the map and unregistered when
	 * the layer is removed.
	 */
	public interface InputListener {
		void onMotionEvent(MotionEvent e);
	}

	public static final boolean debugTheme = false;

	private final Layers mLayers;
	private final Viewport mViewport;
	private final MapAnimator mAnimator;

	private final MapPosition mMapPosition;
	private final AsyncExecutor mAsyncExecutor;

	protected boolean mClearMap;
	protected final MapEventLayer mEventLayer;
	protected GestureDetector mGestureDetector;

	private VectorTileLayer mBaseLayer;

	private Set<InputListener> mInputListenerSet = new LinkedHashSet<InputListener>();
	private InputListener[] mInputListeners;

	private Set<UpdateListener> mUpdateListenerSet = new LinkedHashSet<UpdateListener>();
	private UpdateListener[] mUpdateListeners;

	public Map() {
		mViewport = new Viewport(this);
		mAnimator = new MapAnimator(this, mViewport);

		mMapPosition = new MapPosition();
		mLayers = new Layers(this);
		mAsyncExecutor = new AsyncExecutor(2);

		mEventLayer = new MapEventLayer(this);
		mLayers.add(0, mEventLayer);
	}

	public MapEventLayer getEventLayer() {
		return mEventLayer;
	}

	public VectorTileLayer setBaseMap(TileSource tileSource) {
		mBaseLayer = new VectorTileLayer(this);

		mBaseLayer.setTileSource(tileSource);
		mLayers.add(1, mBaseLayer);

		return mBaseLayer;
	}

	public void setBackgroundMap(BitmapTileLayer tileLayer) {
		mLayers.add(1, tileLayer);
	}

	public VectorTileLayer setBaseMap(BitmapTileLayer tileLayer) {
		mLayers.add(1, tileLayer);
		return null;
	}

	private InternalRenderTheme mCurrentTheme;

	/**
	 * Utility function to set theme of base vector-layer and
	 * use map background color from theme.
	 */
	public void setTheme(InternalRenderTheme theme) {
		if (mBaseLayer == null) {
			log.error("No base layer set");
			throw new IllegalStateException();
		}

		if (mCurrentTheme == theme) {
			log.debug("same theme: " + theme);
			return;
		}

		IRenderTheme t = ThemeLoader.load(theme);
		if (t == null) {
			log.error("Invalid theme");
			return;
		}

		mCurrentTheme = theme;
		mBaseLayer.setRenderTheme(t);
		MapRenderer.setBackgroundColor(t.getMapBackground());

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
	 */
	public void addTask(Runnable task) {
		mAsyncExecutor.post(task);
	}

	/**
	 * Return screen width in pixel.
	 */
	public abstract int getWidth();

	/**
	 * Return screen height in pixel.
	 */
	public abstract int getHeight();

	/**
	 * Request to clear all layers before rendering next frame
	 */
	public void clearMap() {
		mClearMap = true;
	}

	/**
	 * Set {@link MapPosition} of {@link Viewport} and trigger a redraw.
	 */
	public void setMapPosition(MapPosition mapPosition) {
		mViewport.setMapPosition(mapPosition);
		updateMap(true);
	}

	public void setMapPosition(double latitude, double longitude, double scale) {
		mViewport.setMapPosition(new MapPosition(latitude, longitude, scale));
		updateMap(true);
	}

	/**
	 * Get current {@link MapPosition}.
	 * 
	 * @param mapPosition
	 */
	public boolean getMapPosition(MapPosition mapPosition) {
		return mViewport.getMapPosition(mapPosition);
	}

	/**
	 * Get current {@link MapPosition}.
	 */
	public MapPosition getMapPostion() {
		MapPosition pos = new MapPosition();
		mViewport.getMapPosition(pos);
		return pos;
	}

	public Viewport getViewport() {
		return mViewport;
	}

	public Layers getLayers() {
		return mLayers;
	}

	/**
	 * @return MapAnimator instance
	 */
	public MapAnimator getAnimator() {
		return mAnimator;
	}

	/**
	 * Register UpdateListener
	 */
	public void bind(UpdateListener l) {
		if (mUpdateListenerSet.add(l))
			mUpdateListeners = null;
	}

	/**
	 * Unregister UpdateListener
	 */
	public void unbind(UpdateListener l) {
		if (mUpdateListenerSet.remove(l))
			mUpdateListeners = null;
	}

	/**
	 * This function is run on main-loop before rendering a frame.
	 * Caution: Do not call directly!
	 */
	protected void updateLayers() {
		boolean changed = false;

		// get the current MapPosition
		changed |= mViewport.getMapPosition(mMapPosition);

		if (mUpdateListeners == null) {
			mUpdateListeners = new UpdateListener[mUpdateListenerSet.size()];
			mUpdateListenerSet.toArray(mUpdateListeners);
		}

		for (UpdateListener l : mUpdateListeners)
			l.onMapUpdate(mMapPosition, changed, mClearMap);

		mClearMap = false;
	}

	/**
	 * Register InputListener
	 */
	public void bind(InputListener listener) {
		if (mInputListenerSet.add(listener))
			mInputListeners = null;
	}

	/**
	 * Unregister InputListener
	 */
	public void unbind(InputListener listener) {
		if (mInputListenerSet.remove(listener))
			mInputListeners = null;
	}

	public void handleMotionEvent(MotionEvent e) {

		if (mInputListeners == null) {
			mInputListeners = new InputListener[mInputListenerSet.size()];
			mInputListenerSet.toArray(mInputListeners);
		}

		for (InputListener l : mInputListeners)
			l.onMotionEvent(e);
	}

	public boolean handleGesture(Gesture g, MotionEvent e) {
		return mLayers.handleGesture(g, e);
	}

}
