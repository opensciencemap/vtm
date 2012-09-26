/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.oscim.core.GeoPoint;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.MapDatabaseFactory;
import org.oscim.database.MapDatabases;
import org.oscim.database.MapInfo;
import org.oscim.database.OpenResult;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.theme.Theme;
import org.oscim.view.generator.JobQueue;
import org.oscim.view.generator.JobTile;
import org.oscim.view.generator.MapWorker;
import org.oscim.view.renderer.MapRenderer;
import org.oscim.view.renderer.TileGenerator;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class MapView extends FrameLayout {

	final static String TAG = "MapView";

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;
	private static final boolean debugDatabase = false;

	RegionLookup mRegionLookup;

	public boolean enableRotation = false;
	public boolean enableCompass = false;

	private final MapViewPosition mMapViewPosition;

	private final MapZoomControls mMapZoomControls;

	private final TouchHandler mTouchEventHandler;
	private final Compass mCompass;

	private IMapDatabase mMapDatabase;
	private MapDatabases mMapDatabaseType;

	private MapRenderer mMapRenderer;
	private JobQueue mJobQueue;
	private MapWorker mMapWorkers[];
	private int mNumMapWorkers = 4;
	private DebugSettings debugSettings;
	private String mRenderTheme;
	private Map<String, String> mMapOptions;

	// private final Handler mHandler;

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of
	 *             {@link MapActivity} .
	 */
	public MapView(Context context) {
		this(context, null, MapDatabases.MAP_READER);
	}

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @param attributeSet
	 *            a set of attributes.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of
	 *             {@link MapActivity} .
	 */
	public MapView(Context context, AttributeSet attributeSet) {
		this(context, attributeSet, MapDatabaseFactory.getMapDatabase(attributeSet));
	}

	private MapView(Context context, AttributeSet attributeSet,
			MapDatabases mapDatabaseType) {

		super(context, attributeSet);

		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException(
					"context is not an instance of MapActivity");
		}

		Log.d(TAG, "create MapView: " + mapDatabaseType.name());

		// TODO make this dpi dependent
		Tile.TILE_SIZE = 400;

		MapActivity mapActivity = (MapActivity) context;

		// mHandler = new DelayedTaskHandler();

		debugSettings = new DebugSettings(false, false, false, false);

		mMapDatabaseType = mapDatabaseType;

		mMapViewPosition = new MapViewPosition(this);

		mTouchEventHandler = new TouchHandler(mapActivity, this);

		mCompass = new Compass(mapActivity, this);

		mJobQueue = new JobQueue();

		mMapRenderer = new MapRenderer(context, this);

		mMapWorkers = new MapWorker[mNumMapWorkers];

		for (int i = 0; i < mNumMapWorkers; i++) {
			IMapDatabase mapDatabase;
			if (debugDatabase) {
				// mapDatabase = MapDatabaseFactory
				// .createMapDatabase(MapDatabases.TEST_READER);
				mapDatabase = MapDatabaseFactory
						.createMapDatabase(MapDatabases.MAP_READER);
				mNumMapWorkers = 1;
			} else {
				mapDatabase = MapDatabaseFactory.createMapDatabase(mapDatabaseType);
			}

			TileGenerator tileGenerator = new TileGenerator(this);
			tileGenerator.setMapDatabase(mapDatabase);

			if (i == 0)
				mMapDatabase = mapDatabase;

			mMapWorkers[i] = new MapWorker(i, mJobQueue, tileGenerator, mMapRenderer);
		}

		mapActivity.registerMapView(this);

		if (!mMapDatabase.isOpen()) {
			Log.d(TAG, "open database with defaults");
			setMapOptions(null);
		}
		if (!mMapViewPosition.isValid()) {
			Log.d(TAG, "set default start position");
			setMapCenter(getStartPosition());
		}

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		addView(mMapRenderer, params);

		if (testRegionZoom)
			mRegionLookup = new RegionLookup(this);

		mMapZoomControls = new MapZoomControls(mapActivity, this);
		mMapZoomControls.setShowMapZoomControls(true);

		enableRotation = true;

		for (MapWorker worker : mMapWorkers)
			worker.start();

	}

	/**
	 * @return the map database which is used for reading map files.
	 */
	public IMapDatabase getMapDatabase() {
		return mMapDatabase;
	}

	/**
	 * @return the current position and zoom level of this MapView.
	 */
	public MapViewPosition getMapPosition() {
		return mMapViewPosition;
	}

	public void enableRotation(boolean enable) {
		enableRotation = enable;

		if (enable) {
			enableCompass(false);
		}
	}

	public void enableCompass(boolean enable) {
		if (enable == this.enableCompass)
			return;

		this.enableCompass = enable;

		if (enable)
			enableRotation(false);

		if (enable)
			mCompass.enable();
		else
			mCompass.disable();
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		// mMapZoomControls.onMapViewTouchEvent(motionEvent.getAction()
		// & MotionEvent.ACTION_MASK);

		if (this.isClickable())
			return mTouchEventHandler.handleMotionEvent(motionEvent);

		return false;
	}

	/**
	 * Calculates all necessary tiles and adds jobs accordingly.
	 */
	public void redrawMap() {
		if (mPausing || this.getWidth() == 0 || this.getHeight() == 0)
			return;

		mMapRenderer.updateMap(false);
	}

	public void clearAndRedrawMap() {
		if (mPausing || this.getWidth() == 0 || this.getHeight() == 0)
			return;

		mMapRenderer.updateMap(true);
	}

	/**
	 * @param debugSettings
	 *            the new DebugSettings for this MapView.
	 */
	public void setDebugSettings(DebugSettings debugSettings) {
		this.debugSettings = debugSettings;
		clearAndRedrawMap();
	}

	/**
	 * @return the debug settings which are used in this MapView.
	 */
	public DebugSettings getDebugSettings() {
		return debugSettings;
	}

	public Map<String, String> getMapOptions() {
		return mMapOptions;
	}

	/**
	 * Sets the map file for this MapView.
	 * 
	 * @param mapOptions
	 *            ...
	 * @return true if the map file was set correctly, false otherwise.
	 */
	public boolean setMapOptions(Map<String, String> mapOptions) {
		OpenResult openResult = null;

		boolean initialized = false;

		mJobQueue.clear();
		mapWorkersPause(true);

		for (MapWorker mapWorker : mMapWorkers) {

			TileGenerator tileGenerator = mapWorker.getMapGenerator();
			IMapDatabase mapDatabase = tileGenerator.getMapDatabase();

			mapDatabase.close();
			openResult = mapDatabase.open(null);

			if (openResult.isSuccess())
				initialized = true;
		}

		mapWorkersProceed();

		if (initialized) {
			mMapOptions = mapOptions;
			clearAndRedrawMap();

			Log.i(TAG, "MapDatabase ready");
			return true;
		}

		mMapOptions = null;
		Log.i(TAG, "Opening MapDatabase failed");

		return false;
	}

	private MapPosition getStartPosition() {
		if (mMapDatabase == null)
			return new MapPosition();

		MapInfo mapInfo = mMapDatabase.getMapInfo();
		if (mapInfo == null)
			return new MapPosition();

		GeoPoint startPos = mapInfo.startPosition;

		if (startPos == null)
			startPos = mapInfo.mapCenter;

		if (startPos == null)
			startPos = new GeoPoint(0, 0);

		if (mapInfo.startZoomLevel != null)
			return new MapPosition(startPos, (mapInfo.startZoomLevel).byteValue(), 1);

		return new MapPosition(startPos, (byte) 1, 1);
	}

	/**
	 * Sets the MapDatabase for this MapView.
	 * 
	 * @param mapDatabaseType
	 *            the new MapDatabase.
	 */

	public void setMapDatabase(MapDatabases mapDatabaseType) {
		if (debugDatabase)
			return;

		TileGenerator tileGenerator;

		Log.i(TAG, "setMapDatabase " + mapDatabaseType.name());

		if (mMapDatabaseType == mapDatabaseType)
			return;

		mMapDatabaseType = mapDatabaseType;

		mapWorkersPause(true);

		for (MapWorker mapWorker : mMapWorkers) {
			tileGenerator = mapWorker.getMapGenerator();

			tileGenerator.setMapDatabase(MapDatabaseFactory
					.createMapDatabase(mapDatabaseType));
		}

		mJobQueue.clear();

		setMapOptions(null);

		mapWorkersProceed();
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

		boolean ret = setRenderTheme((Theme) internalRenderTheme);
		if (ret) {
			mRenderTheme = internalRenderTheme.name();
		}
		clearAndRedrawMap();
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
		clearAndRedrawMap();
	}

	private boolean setRenderTheme(Theme theme) {

		mapWorkersPause(true);

		InputStream inputStream = null;
		try {
			inputStream = theme.getRenderThemeAsStream();
			RenderTheme t = RenderThemeHandler.getRenderTheme(inputStream);
			mMapRenderer.setRenderTheme(t);
			mMapWorkers[0].getMapGenerator().setRenderTheme(t);
			return true;
		} catch (ParserConfigurationException e) {
			Log.e(TAG, e.getMessage());
		} catch (SAXException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			mapWorkersProceed();
		}

		return false;
	}

	@Override
	protected synchronized void onSizeChanged(int width, int height,
			int oldWidth, int oldHeight) {

		mJobQueue.clear();
		mapWorkersPause(true);
		Log.d(TAG, "onSizeChanged" + width + " " + height);
		super.onSizeChanged(width, height, oldWidth, oldHeight);

		if (width != 0 && height != 0)
			mMapViewPosition.setViewport(width, height);

		mapWorkersProceed();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mMapZoomControls.onLayout(changed, left, top, right, bottom);
	}

	void destroy() {
		for (MapWorker mapWorker : mMapWorkers) {
			mapWorker.pause();
			mapWorker.interrupt();

			try {
				mapWorker.join();
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			IMapDatabase mapDatabase = mapWorker.getMapGenerator().getMapDatabase();
			mapDatabase.close();
		}
	}

	private boolean mPausing = false;

	void onPause() {
		mPausing = true;

		Log.d(TAG, "onPause");
		mJobQueue.clear();
		mapWorkersPause(true);

		if (this.enableCompass)
			mCompass.disable();

	}

	void onResume() {
		Log.d(TAG, "onResume");
		mapWorkersProceed();

		if (this.enableCompass)
			mCompass.enable();

		mPausing = false;
	}

	/**
	 * @return the maximum possible zoom level.
	 */
	byte getMaximumPossibleZoomLevel() {
		return (byte) MapViewPosition.MAX_ZOOMLEVEL;
		// Math.min(mMapZoomControls.getZoomLevelMax(),
		// mMapGenerator.getZoomLevelMax());
	}

	/**
	 * @return true if the current center position of this MapView is valid,
	 *         false otherwise.
	 */
	boolean hasValidCenter() {
		MapInfo mapInfo;

		if (!mMapViewPosition.isValid())
			return false;

		if ((mapInfo = mMapDatabase.getMapInfo()) == null)
			return false;

		if (!mapInfo.boundingBox.contains(getMapPosition().getMapCenter()))
			return false;

		return true;
	}

	byte limitZoomLevel(byte zoom) {
		if (mMapZoomControls == null)
			return zoom;

		return (byte) Math.max(Math.min(zoom, getMaximumPossibleZoomLevel()),
				mMapZoomControls.getZoomLevelMin());
	}

	/**
	 * Sets the center and zoom level of this MapView and triggers a redraw.
	 * 
	 * @param mapPosition
	 *            the new map position of this MapView.
	 */
	public void setMapCenter(MapPosition mapPosition) {
		Log.d(TAG, "setMapCenter "
				+ " lat: " + mapPosition.lat
				+ " lon: " + mapPosition.lon);
		mMapViewPosition.setMapCenter(mapPosition);
		redrawMap();
	}

	/**
	 * Sets the center of the MapView and triggers a redraw.
	 * 
	 * @param geoPoint
	 *            the new center point of the map.
	 */
	public void setCenter(GeoPoint geoPoint) {
		MapPosition mapPosition = new MapPosition(geoPoint,
				mMapViewPosition.getZoomLevel(), 1);

		setMapCenter(mapPosition);
	}

	/**
	 * @return MapPosition
	 */
	public MapViewPosition getMapViewPosition() {
		return mMapViewPosition;
	}

	/**
	 * add jobs and remember MapWorkers that stuff needs to be done
	 * 
	 * @param jobs
	 *            tile jobs
	 */
	public void addJobs(ArrayList<JobTile> jobs) {
		if (jobs == null) {
			mJobQueue.clear();
			return;
		}
		mJobQueue.setJobs(jobs);

		for (int i = 0; i < mNumMapWorkers; i++) {
			MapWorker m = mMapWorkers[i];
			synchronized (m) {
				m.notify();
			}
		}
	}

	private void mapWorkersPause(boolean wait) {
		for (MapWorker mapWorker : mMapWorkers) {
			if (!mapWorker.isPausing())
				mapWorker.pause();
		}
		if (wait) {
			for (MapWorker mapWorker : mMapWorkers) {
				if (!mapWorker.isPausing())
					mapWorker.awaitPausing();
			}
		}
	}

	private void mapWorkersProceed() {
		for (MapWorker mapWorker : mMapWorkers)
			mapWorker.proceed();
	}

	// /**
	// * Sets the visibility of the zoom controls.
	// *
	// * @param showZoomControls
	// * true if the zoom controls should be visible, false otherwise.
	// */
	// public void setBuiltInZoomControls(boolean showZoomControls) {
	// mMapZoomControls.setShowMapZoomControls(showZoomControls);
	//
	// }

	// /**
	// * Sets the text scale for the map rendering. Has no effect in downloading
	// mode.
	// *
	// * @param textScale
	// * the new text scale for the map rendering.
	// */
	// public void setTextScale(float textScale) {
	// mJobParameters = new JobParameters(mJobParameters.theme, textScale);
	// clearAndRedrawMapView();
	// }
}
