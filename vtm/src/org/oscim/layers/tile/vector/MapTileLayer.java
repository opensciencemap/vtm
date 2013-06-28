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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.Log;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.TileManager;
import org.oscim.renderer.GLRenderer;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.theme.Theme;
import org.oscim.tilesource.ITileDataSource;
import org.oscim.tilesource.MapInfo;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.TileSource.OpenResult;
import org.oscim.view.MapView;

public class MapTileLayer extends TileLayer<MapTileLoader> {
	private final static String TAG = MapTileLayer.class.getName();

	public MapTileLayer(MapView mapView) {
		super(mapView);
	}

	@Override
	protected MapTileLoader createLoader(TileManager tm) {
		return new MapTileLoader(tm);
	}

	private TileSource mTileSource;
	private String mRenderTheme;

	/**
	 * Sets the TileSource for this MapView.
	 *
	 * @param tileSource
	 *            the new TileSource.
	 * @return true if TileSource changed
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

		mMapView.clearMap();

		resumeLoaders();

		return true;
	}

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
			mapPosition.setZoomLevel(12);
		else
			mapPosition.setZoomLevel((mapInfo.startZoomLevel).byteValue());

		return mapPosition;
	}

	public String getRenderTheme() {
		return mRenderTheme;
	}

	/**
	 * Sets the internal theme which is used for rendering the map.
	 *
	 * @param internalRenderTheme
	 *            the internal rendering theme.
	 * @return ...
	 * @throws IllegalArgumentException
	 *             if the supplied internalRenderTheme is null.
	 */
	public boolean setRenderTheme(InternalRenderTheme internalRenderTheme) {
		if (internalRenderTheme == null) {
			throw new IllegalArgumentException("render theme must not be null");
		}

		if (internalRenderTheme.name() == mRenderTheme)
			return true;

		boolean ret = setRenderTheme((Theme) internalRenderTheme);
		if (ret) {
			mRenderTheme = internalRenderTheme.name();
		}

		mMapView.clearMap();

		return ret;
	}

	/**
	 * Sets the theme file which is used for rendering the map.
	 *
	 * @param renderThemePath
	 *            the path to the XML file which defines the rendering theme.
	 * @throws IllegalArgumentException
	 *             if the supplied internalRenderTheme is null.
	 * @throws FileNotFoundException
	 *             if the supplied file does not exist, is a directory or cannot
	 *             be read.
	 */
	public void setRenderTheme(String renderThemePath) throws FileNotFoundException {
		if (renderThemePath == null) {
			throw new IllegalArgumentException("render theme path must not be null");
		}

		boolean ret = setRenderTheme(new ExternalRenderTheme(renderThemePath));
		if (ret) {
			mRenderTheme = renderThemePath;
		}

		mMapView.clearMap();
	}

	private boolean setRenderTheme(Theme theme) {

		pauseLoaders(true);

		InputStream inputStream = null;
		try {
			inputStream = theme.getRenderThemeAsStream();
			IRenderTheme t = RenderThemeHandler.getRenderTheme(inputStream);
			t.scaleTextSize(1 + (CanvasAdapter.dpi / 240 - 1) * 0.5f);

			// FIXME !!!
			GLRenderer.setRenderTheme(t);

			for (MapTileLoader g : mTileLoader)
				g.setRenderTheme(t);

			return true;
//		} catch (ParserConfigurationException e) {
//			Log.e(TAG, e.getMessage());
//		} catch (SAXException e) {
//			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			//Log.e(TAG, e.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			resumeLoaders();
		}
		return false;
	}

}
