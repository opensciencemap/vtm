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
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.TileSource.OpenResult;
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
		                          tileSource.getZoomLevelMin(),
		                          tileSource.getZoomLevelMax(),
		                          100),
		     new VectorTileRenderer());

		setTileSource(tileSource);
	}

	public VectorTileLayer(Map map, int minZoom, int maxZoom, int cacheLimit) {
		this(map, new TileManager(map, minZoom, maxZoom, cacheLimit),
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

	public interface TileLoaderProcessHook {
		public boolean process(MapTile tile, ElementLayers layers, MapElement element);
	}

	public interface TileLoaderThemeHook {
		public boolean render(MapTile tile, ElementLayers layers,
		        MapElement element, RenderStyle style, int level);
	}

	private TileLoaderProcessHook[] mLoaderProcessHooks = new TileLoaderProcessHook[0];
	private TileLoaderThemeHook[] mLoaderThemeHooks = new TileLoaderThemeHook[0];

	public TileLoaderProcessHook[] loaderProcessHooks() {
		return mLoaderProcessHooks;
	}

	public TileLoaderThemeHook[] loaderThemeHooks() {
		return mLoaderThemeHooks;
	}

	public void addHook(TileLoaderProcessHook h) {
		TileLoaderProcessHook[] tmp = mLoaderProcessHooks;
		mLoaderProcessHooks = new TileLoaderProcessHook[tmp.length + 1];
		System.arraycopy(tmp, 0, mLoaderProcessHooks, 0, tmp.length);
		mLoaderProcessHooks[tmp.length] = h;
	}

	public void addHook(TileLoaderThemeHook h) {
		TileLoaderThemeHook[] tmp = mLoaderThemeHooks;
		mLoaderThemeHooks = new TileLoaderThemeHook[tmp.length + 1];
		System.arraycopy(tmp, 0, mLoaderThemeHooks, 0, tmp.length);
		mLoaderThemeHooks[tmp.length] = h;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mTileSource.close();
	}
}
