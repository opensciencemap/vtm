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

import org.oscim.core.BoundingBox;
import org.oscim.core.Box;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.event.EventDispatcher;
import org.oscim.event.EventListener;
import org.oscim.event.Gesture;
import org.oscim.event.GestureDetector;
import org.oscim.event.MotionEvent;
import org.oscim.layers.MapEventLayer;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.TileSource;
import org.oscim.utils.ThreadUtils;
import org.oscim.utils.async.AsyncExecutor;
import org.oscim.utils.async.TaskQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Map implements TaskQueue {

	static final Logger log = LoggerFactory.getLogger(Map.class);

	/**
	 * Listener interface for map update notifications.
	 * Layers implementing this interface they will be automatically register
	 * when the layer is added to the map and unregistered when the layer is
	 * removed. Otherwise use map.events.bind(UpdateListener).
	 */
	public interface UpdateListener extends EventListener {
		void onMapEvent(Event e, MapPosition mapPosition);
	}

	/**
	 * Listener interface for input events.
	 * Layers implementing this interface they will be automatically register
	 * when the layer is added to the map and unregistered when the layer is
	 * removed.
	 */

	public interface InputListener extends EventListener {
		void onInputEvent(Event e, MotionEvent motionEvent);
	}

	/**
	 * UpdateListener event. Map position has changed.
	 */
	public static final Event POSITION_EVENT = new Event();

	/**
	 * UpdateLister event. Delivered on main-thread when updateMap() was called
	 * and no CLEAR_EVENT or POSITION_EVENT was triggered.
	 */
	public static final Event UPDATE_EVENT = new Event();

	/**
	 * UpdateListerner event. Map state has changed in a way that all layers
	 * should clear their state e.g. the theme or the TilesSource has changed.
	 * TODO should have an event-source to only clear affected layers.
	 */
	public static final Event CLEAR_EVENT = new Event();

	public static final Event ANIM_END = new Event();

	public final EventDispatcher<InputListener, MotionEvent> input;
	public final EventDispatcher<UpdateListener, MapPosition> events;

	private final Layers mLayers;
	private final ViewController mViewport;
	private final AsyncExecutor mAsyncExecutor;

	protected final Animator mAnimator;
	protected final MapPosition mMapPosition;

	protected final MapEventLayer mEventLayer;
	protected GestureDetector mGestureDetector;

	private TileLayer mBaseLayer;

	protected boolean mClearMap = true;

	public Map() {
		ThreadUtils.init();

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

		mAsyncExecutor = new AsyncExecutor(4, this);
		mMapPosition = new MapPosition();

		mEventLayer = new MapEventLayer(this);
		mLayers.add(0, mEventLayer);

	}

	public MapEventLayer getEventLayer() {
		return mEventLayer;
	}

	/**
	 * Create OsmTileLayer with given TileSource and
	 * set as base map (layer 1)
	 * 
	 * TODO deprecate
	 */
	public VectorTileLayer setBaseMap(TileSource tileSource) {
		VectorTileLayer l = new OsmTileLayer(this);
		l.setTileSource(tileSource);
		setBaseMap(l);
		return l;
	}

	public TileLayer setBaseMap(TileLayer tileLayer) {
		mLayers.add(1, tileLayer);
		mBaseLayer = tileLayer;
		return tileLayer;
	}

	/**
	 * Utility function to set theme of base vector-layer and
	 * use map background color from theme.
	 */
	public void setTheme(ThemeFile theme) {
		if (mBaseLayer == null) {
			log.error("No base layer set");
			throw new IllegalStateException();
		}
		setTheme(ThemeLoader.load(theme));
	}

	public void setTheme(IRenderTheme theme) {
		if (theme == null) {
			throw new IllegalArgumentException("Theme cannot be null.");
		}

		if (mBaseLayer == null) {
			log.warn("No base layer set.");
		} else if (mBaseLayer instanceof VectorTileLayer) {
			((VectorTileLayer) mBaseLayer).setRenderTheme(theme);
		}

		MapRenderer.setBackgroundColor(theme.getMapBackground());

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
	@Override
	public abstract boolean post(Runnable action);

	/**
	 * Post a runnable to be executed on main-thread. Execution is delayed for
	 * at least 'delay' milliseconds.
	 */
	public abstract boolean postDelayed(Runnable action, long delay);

	/**
	 * Post a task to run on a shared worker-thread. Shoul only use for
	 * tasks running less than a second.
	 */
	@Override
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
	public void setMapPosition(final MapPosition mapPosition) {
		if (!ThreadUtils.isMainThread())
			post(new Runnable() {
				@Override
				public void run() {
					mViewport.setMapPosition(mapPosition);
					updateMap(true);
				}
			});
		else {
			mViewport.setMapPosition(mapPosition);
			updateMap(true);
		}
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
		if (!ThreadUtils.isMainThread()) {
			return mViewport.getSyncMapPosition(mapPosition);
		}

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

	public BoundingBox getBoundingBox(int expand) {
		Box box = new Box();
		mViewport.getBBox(box, expand);
		box.map2mercator();
		return new BoundingBox(box.ymin, box.xmin, box.ymax, box.xmax);
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
	 * This function is run on main-thread before rendering a frame.
	 * 
	 * For internal use only. Do not call!
	 */
	protected void prepareFrame() {
		ThreadUtils.assertMainThread();

		MapPosition pos = mMapPosition;

		mAnimator.updateAnimation();

		boolean changed = mViewport.getMapPosition(pos);

		if (mClearMap)
			events.fire(CLEAR_EVENT, pos);
		else if (changed)
			events.fire(POSITION_EVENT, pos);
		else
			events.fire(UPDATE_EVENT, pos);

		mClearMap = false;

		mAnimator.updateAnimation();

		mViewport.syncViewport();
	}

	public boolean handleGesture(Gesture g, MotionEvent e) {
		return mLayers.handleGesture(g, e);
	}

	/** Called on render thread, use synchronized! */
	public abstract void beginFrame();

	/** Called on render thread, use synchronized! */
	public abstract void doneFrame(boolean needsRedraw);
}
