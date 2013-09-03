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
package org.oscim.layers.tile.vector;

import org.oscim.backend.Log;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileLoader;
import org.oscim.layers.tile.TileManager;
import org.oscim.theme.IRenderTheme;
import org.oscim.tilesource.ITileDataSource;
import org.oscim.tilesource.MapInfo;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.TileSource.OpenResult;
import org.oscim.view.Map;

/**
 * The vector-tile-map layer. This class manages instances of
 * {@link MapTileLoader} that load and assemble vector tiles
 * for rendering.
 */
public class MapTileLayer extends TileLayer<MapTileLoader> {
	private final static String TAG = MapTileLayer.class.getName();

	private TileSource mTileSource;

	public MapTileLayer(Map map) {
		super(map);
	}

	@Override
	protected MapTileLoader createLoader(TileManager tm) {
		return new MapTileLoader(tm);
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
			Log.d(TAG, msg.getErrorMessage());
			return false;
		}

		mTileSource = tileSource;

		for (int i = 0; i < mNumTileLoader; i++) {
			ITileDataSource tileDataSource = tileSource.getDataSource();
			mTileLoader.get(i).setTileDataSource(tileDataSource);
		}

		mTileManager.setZoomTable(mTileSource.getMapInfo().zoomLevel);

		mMap.clearMap();

		resumeLoaders();

		return true;
	}

	/**
	 * Set {@link IRenderTheme} used by {@link TileLoader}
	 */
	public void setRenderTheme(IRenderTheme theme) {
		pauseLoaders(true);

		for (MapTileLoader g : mTileLoader)
			g.setRenderTheme(theme);

		resumeLoaders();
	}

	/**
	 * @deprecated
	 */
	public MapPosition getMapFileCenter() {
		if (mTileSource == null)
			return null;

		MapInfo mapInfo = mTileSource.getMapInfo();
		if (mapInfo == null)
			return null;

		GeoPoint startPos = mapInfo.startPosition;

		if (startPos == null)
			startPos = mapInfo.mapCenter;

		if (startPos == null)
			startPos = new GeoPoint(0, 0);

		MapPosition mapPosition = new MapPosition();
		mapPosition.setPosition(startPos);

		if (mapInfo.startZoomLevel == null)
			mapPosition.setZoomLevel(2);
		else
			mapPosition.setZoomLevel((mapInfo.startZoomLevel).byteValue());

		return mapPosition;
	}

}
