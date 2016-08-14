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
package org.oscim.layers.tile.vector;

import org.oscim.core.MapElement;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.VectorTileRenderer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.TileSource.OpenResult;
import org.oscim.utils.pool.Inlist.List;
import org.oscim.utils.pool.LList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The vector-tile-map layer. This class manages instances of
 * {@link VectorTileLoader} that load and assemble vector tiles
 * for rendering.
 */
public class VectorTileLayer extends TileLayer {
	static final Logger log = LoggerFactory.getLogger(VectorTileLayer.class);

	protected TileSource mTileSource;

	public VectorTileLayer(Map map, TileSource tileSource) {
		this(map, new TileManager(map,
		                          100),
		     new VectorTileRenderer());

		setTileSource(tileSource);
	}

	public VectorTileLayer(Map map, int cacheLimit) {
		this(map, new TileManager(map, cacheLimit),
		     new VectorTileRenderer());
	}

	public VectorTileLayer(Map map, TileManager tileManager,
	        VectorTileRenderer renderer) {
		super(map, tileManager, renderer);

		initLoader(getNumLoaders());
	}

	@Override
	protected TileLoader createLoader() {
		return new VectorTileLoader(this);
	}

	/**
	 * Sets the {@link TileSource} used by {@link TileLoader}.
	 * 
	 * @return true when new TileSource was set (has changed)
	 */
	public boolean setTileSource(TileSource tileSource) {
		pauseLoaders(true);
		mTileManager.clearJobs();
		mTileManager.setZoomLevel(tileSource.getZoomLevelMin(),
		                          tileSource.getZoomLevelMax());

		if (mTileSource != null) {
			mTileSource.close();
			mTileSource = null;
		}

		OpenResult msg = tileSource.open();

		if (msg != OpenResult.SUCCESS) {
			log.debug(msg.getErrorMessage());
			return false;
		}

		mTileSource = tileSource;

		for (TileLoader l : mTileLoader)
			((VectorTileLoader) l).setDataSource(tileSource.getDataSource());

		mMap.clearMap();
		resumeLoaders();

		return true;
	}

	/**
	 * Set {@link IRenderTheme} used by {@link TileLoader}
	 */
	public void setRenderTheme(IRenderTheme theme) {
		/* wait for loaders to finish all current jobs to
		 * not change theme instance hold by loader instance
		 * while running */
		pauseLoaders(true);
		mTileManager.clearJobs();

		mTheme = theme;
		//	for (TileLoader l : mTileLoader)
		//	((VectorTileLoader) l).setRenderTheme(theme);

		tileRenderer().setOverdrawColor(theme.getMapBackground());

		resumeLoaders();
	}

	private IRenderTheme mTheme;

	public IRenderTheme getTheme() {
		return mTheme;
	}

	/**
	 * Hook to intercept tile data processing. Called concurently by tile
	 * loader threads, so dont keep tile specific state.
	 */
	public interface TileLoaderProcessHook {
		public boolean process(MapTile tile, RenderBuckets layers, MapElement element);

		/** Called on loader thread when tile loading is completed */
		public void complete(MapTile tile, boolean success);
	}

	/**
	 * Hook to intercept tile data processing after theme style lookup. Called
	 * concurently by tile loader threads, so dont keep tile specific state. See
	 * e.g. LabelTileLoaderHook.
	 */
	public interface TileLoaderThemeHook {
		/** Called for each RenderStyle found for a MapElement. */
		public boolean process(MapTile tile, RenderBuckets buckets,
		        MapElement element, RenderStyle style, int level);

		/** Called on loader thread when tile loading is completed */
		public void complete(MapTile tile, boolean success);
	}

	private List<LList<TileLoaderProcessHook>> mLoaderProcessHooks =
	        new List<LList<TileLoaderProcessHook>>();

	private List<LList<TileLoaderThemeHook>> mLoaderThemeHooks =
	        new List<LList<TileLoaderThemeHook>>();

	public void addHook(TileLoaderProcessHook h) {
		mLoaderProcessHooks.append(new LList<TileLoaderProcessHook>(h));
	}

	public void addHook(TileLoaderThemeHook h) {
		mLoaderThemeHooks.append(new LList<TileLoaderThemeHook>(h));
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mTileSource.close();
	}

	public void callThemeHooks(MapTile tile, RenderBuckets layers, MapElement element,
	        RenderStyle style, int level) {

		LList<TileLoaderThemeHook> th = mLoaderThemeHooks.head();
		while (th != null) {
			if (th.data.process(tile, layers, element, style, level))
				return;

			th = th.next;
		}
	}

	public boolean callProcessHooks(MapTile tile, RenderBuckets layers, MapElement element) {

		LList<TileLoaderProcessHook> ph = mLoaderProcessHooks.head();
		while (ph != null) {
			if (ph.data.process(tile, layers, element))
				return true;
			ph = ph.next;
		}

		return false;
	}

	public void callHooksComplete(MapTile tile, boolean success) {
		/* NB: cannot use internal iterater as this function
		 * is called concurently by TileLoaders */

		LList<TileLoaderThemeHook> th = mLoaderThemeHooks.head();
		while (th != null) {
			th.data.complete(tile, success);
			th = th.next;
		}

		LList<TileLoaderProcessHook> ph = mLoaderProcessHooks.head();
		while (ph != null) {
			ph.data.complete(tile, success);
			ph = ph.next;
		}
	}
}
