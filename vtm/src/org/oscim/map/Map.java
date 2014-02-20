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

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.EventDispatcher;
import org.oscim.event.EventListener;
import org.oscim.event.Gesture;
import org.oscim.event.GestureDetector;
import org.oscim.event.MotionEvent;
import org.oscim.layers.MapEventLayer;
import org.oscim.layers.tile.BitmapTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.TileSource;
import org.oscim.utils.async.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Map {

	static final Logger log = LoggerFactory.getLogger(Map.class);

	/**
	 * Listener interface for map update notifications.
	 * Layers implementing this interface they will be automatically
	 * register when the layer is added to the map and unregistered when
	 * the layer is removed.
	 */
	public interface UpdateListener extends EventListener {
		void onMapEvent(Event e, MapPosition mapPosition);
	}

	/**
	 * Listener interface for input events.
	 * Layers implementing this interface they will be automatically
	 * register when the layer is added to the map and unregistered when
	 * the layer is removed.
	 */

	public interface InputListener extends EventListener {
		void onInputEvent(Event e, MotionEvent motionEvent);
	}

	/***/
	public static Event UPDATE_EVENT = new Event();
	/**
	 * Map state has changed in a way that all layers should clear their state
	 * e.g. the theme or the TilesSource has changed
	 */
	public static Event CLEAR_EVENT = new Event();
	public static Event POSITION_EVENT = new Event();

	public final EventDispatcher<InputListener, MotionEvent> input;
	public final EventDispatcher<UpdateListener, MapPosition> events;

	private final Layers mLayers;
	private final ViewController mViewport;
	private final Animator mAnimator;
	private final MapPosition mMapPosition;
	private final AsyncExecutor mAsyncExecutor;

	protected final MapEventLayer mEventLayer;
	protected GestureDetector mGestureDetector;
	/**
	 * Listener interface for map update notifications.
	 * Layers implementing this interface they will be automatically
	 * register when the layer is added to the map and unregistered when
	 * the layer is removed.
	 */
	private VectorTileLayer mBaseLayer;

	protected boolean mClearMap = true;

	public Map() {
		mViewport = new ViewController();
		mAnimator = new Animator(this);
		mLayers = new Layers(this);

		input = new EventDispatcher<InputListener, MotionEvent>() {

			@Override
			public void tell(InputListener l, Event e, MotionEvent d) {
				l.onInputEvent(e, d);
			}
		};
		events = new EventDispatcher<UpdateListener, MapPosition>() {

			@Override
			public void tell(UpdateListener l, Event e, MapPosition d) {
				l.onMapEvent(e, d);
			}
		};

		mAsyncExecutor = new AsyncExecutor(2);
		mMapPosition = new MapPosition();

		mEventLayer = new MapEventLayer(this);
		mLayers.add(0, mEventLayer);

	}

	public MapEventLayer getEventLayer() {
		return mEventLayer;
	}

	public VectorTileLayer setBaseMap(TileSource tileSource) {
		// TODO cleanup previous baseLayer here?

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

	/**
	 * Utility function to set theme of base vector-layer and
	 * use map background color from theme.
	 */
	public void setTheme(InternalRenderTheme theme) {
		if (mBaseLayer == null) {
			log.error("No base layer set");
			throw new IllegalStateException();
		}

		IRenderTheme t = ThemeLoader.load(theme);
		if (t == null) {
			log.error("Invalid theme");
			return;
		}

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
	 * @param redraw pass true to render next frame afterwards
	 */
	public abstract void updateMap(boolean redraw);

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
		updateMap(true);
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
	 * @return true when MapPosition was updated (has changed)
	 */
	public boolean getMapPosition(MapPosition mapPosition) {
		return mViewport.getMapPosition(mapPosition);
	}

	/**
	 * Get current {@link MapPosition}. Consider using
	 * getViewport.getMapPosition(pos) instead to reuse
	 * MapPosition instance.
	 */
	public MapPosition getMapPosition() {
		MapPosition pos = new MapPosition();
		mViewport.getMapPosition(pos);
		return pos;
	}

	/**
	 * @return Viewport instance
	 */
	public ViewController viewport() {
		return mViewport;
	}

	/**
	 * @return Layers instance
	 */
	public Layers layers() {
		return mLayers;
	}

	/**
	 * @return MapAnimator instance
	 */
	public Animator animator() {
		return mAnimator;
	}

	/**
	 * This function is run on main-loop before rendering a frame.
	 * Caution: Do not call directly!
	 */
	protected void updateLayers() {
		boolean changed = false;
		MapPosition pos = mMapPosition;

		changed |= mViewport.getMapPosition(pos);

		if (mClearMap)
			events.fire(CLEAR_EVENT, pos);
		else if (changed)
			events.fire(POSITION_EVENT, pos);
		else
			events.fire(UPDATE_EVENT, pos);

		mClearMap = false;
	}

	public boolean handleGesture(Gesture g, MotionEvent e) {
		return mLayers.handleGesture(g, e);
	}
}
