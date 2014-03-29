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
package org.oscim.gdx;

import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public abstract class GdxMap implements ApplicationListener {
	final static Logger log = LoggerFactory.getLogger(GdxMap.class);

	protected final Map mMap;
	private final MapAdapter mMapAdapter;

	VectorTileLayer mMapLayer;
	private final MapRenderer mMapRenderer;

	public GdxMap() {
		mMap = mMapAdapter = new MapAdapter();
		mMapRenderer = new MapRenderer(mMap);
	}

	protected void initDefaultLayers(TileSource tileSource, boolean tileGrid, boolean labels,
	        boolean buildings) {
		Layers layers = mMap.layers();

		if (tileSource != null) {
			mMapLayer = mMap.setBaseMap(tileSource);
			mMap.setTheme(VtmThemes.DEFAULT);

			if (buildings)
				layers.add(new BuildingLayer(mMap, mMapLayer));

			if (labels)
				layers.add(new LabelLayer(mMap, mMapLayer));
		}

		if (tileGrid)
			layers.add(new TileGridLayer(mMap));
	}

	@Override
	public void create() {

		Gdx.graphics.setContinuousRendering(false);
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();

		mMap.viewport().setScreenSize(w, h);
		mMapRenderer.onSurfaceCreated();
		mMapRenderer.onSurfaceChanged(w, h);

		InputMultiplexer mux = new InputMultiplexer();
		mux.addProcessor(new InputHandler(this));
		//mux.addProcessor(new GestureDetector(20, 0.5f, 2, 0.05f,
		//                                     new MapController(mMap)));
		mux.addProcessor(new MotionHandler(mMap));

		Gdx.input.setInputProcessor(mux);

		createLayers();
	}

	protected void createLayers() {
		mMap.layers().add(new TileGridLayer(mMap));
	}

	@Override
	public void dispose() {

	}

	@Override
	public void render() {
		if (!mMapAdapter.needsRedraw())
			return;

		mMapRenderer.onDrawFrame();
	}

	@Override
	public void resize(int w, int h) {
		mMap.viewport().setScreenSize(w, h);
		mMapRenderer.onSurfaceChanged(w, h);
		mMap.render();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	protected boolean onKeyDown(int keycode) {
		return false;
	}

	public Map getMap() {
		return mMap;
	}

	static class MapAdapter extends Map {
		boolean mRenderRequest;

		@Override
		public int getWidth() {
			return Gdx.graphics.getWidth();
		}

		@Override
		public int getHeight() {
			return Gdx.graphics.getHeight();
		}

		@Override
		public void updateMap(boolean forceRender) {
			if (!mWaitRedraw) {
				mWaitRedraw = true;
				Gdx.app.postRunnable(mRedrawRequest);
			}
		}

		@Override
		public void render() {
			mRenderRequest = true;
			if (mClearMap)
				updateMap(false);
			else
				Gdx.graphics.requestRendering();
		}

		@Override
		public boolean post(Runnable runnable) {
			Gdx.app.postRunnable(runnable);
			return true;
		}

		@Override
		public boolean postDelayed(final Runnable action, long delay) {
			Timer.schedule(new Task() {
				@Override
				public void run() {
					action.run();
				}
			}, delay / 1000f);
			return true;
		}

		/**
		 * Update all Layers on Main thread.
		 * 
		 * @param forceRedraw
		 *            also render frame FIXME (does nothing atm)
		 */
		private void redrawMapInternal(boolean forceRedraw) {
			updateLayers();

			mRenderRequest = true;
			Gdx.graphics.requestRendering();
		}

		/* private */boolean mWaitRedraw;
		private final Runnable mRedrawRequest = new Runnable() {
			@Override
			public void run() {
				mWaitRedraw = false;
				redrawMapInternal(false);
			}
		};

		public boolean needsRedraw() {
			if (!mRenderRequest)
				return false;

			mRenderRequest = false;
			return true;
		}

	}
}
