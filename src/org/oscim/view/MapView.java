/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.database.IMapDatabase;
import org.oscim.database.MapDatabaseFactory;
import org.oscim.database.MapDatabases;
import org.oscim.database.MapInfo;
import org.oscim.database.MapOptions;
import org.oscim.database.OpenResult;
import org.oscim.generator.JobQueue;
import org.oscim.generator.JobTile;
import org.oscim.generator.MapWorker;
import org.oscim.generator.TileGenerator;
import org.oscim.overlay.BuildingOverlay;
import org.oscim.overlay.LabelingOverlay;
import org.oscim.overlay.Overlay;
import org.oscim.overlay.OverlayManager;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLView;
import org.oscim.renderer.TileManager;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.theme.Theme;
import org.oscim.utils.AndroidUtils;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class MapView extends RelativeLayout {

	final static String TAG = MapView.class.getName();

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;
	private static final boolean debugDatabase = false;

	public boolean mRotationEnabled = false;
	public boolean mCompassEnabled = false;
	public boolean enablePagedFling = false;

	private final MapViewPosition mMapViewPosition;
	private final MapPosition mMapPosition;

	//private final MapZoomControls mMapZoomControls;

	private final TouchHandler mTouchEventHandler;
	private final Compass mCompass;

	private final TileManager mTileManager;
	private final OverlayManager mOverlayManager;

	final GLView mGLView;
	private final JobQueue mJobQueue;

	// TODO use 1 download and 1 generator thread instead
	private final MapWorker mMapWorkers[];
	private final int mNumMapWorkers = 4;

	private MapOptions mMapOptions;
	private IMapDatabase mMapDatabase;
	private String mRenderTheme;
	private DebugSettings mDebugSettings;

	private boolean mClearMap;

	private int mWidth;
	private int mHeight;

	// FIXME: keep until old pbmap reader is removed
	public static boolean enableClosePolygons = false;

	public final float dpi;

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of
	 *             {@link MapActivity} .
	 */
	public MapView(Context context) {
		this(context, null);
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
		super(context, attributeSet);

		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException(
					"context is not an instance of MapActivity");
		}

		this.setWillNotDraw(true);

		DisplayMetrics metrics = getResources().getDisplayMetrics();
		dpi = Math.max(metrics.xdpi, metrics.ydpi);

		// TODO make this dpi dependent
		Tile.TILE_SIZE = 400;

		MapActivity mapActivity = (MapActivity) context;

		mMapViewPosition = new MapViewPosition(this);
		mMapPosition = new MapPosition();

		mOverlayManager = new OverlayManager();

		mTouchEventHandler = new TouchHandler(mapActivity, this);

		mCompass = new Compass(mapActivity, this);

		mJobQueue = new JobQueue();

		mTileManager = new TileManager(this);

		mGLView = new GLView(context, this);
		mMapWorkers = new MapWorker[mNumMapWorkers];

		mDebugSettings = new DebugSettings();
		TileGenerator.setDebugSettings(mDebugSettings);

		for (int i = 0; i < mNumMapWorkers; i++) {
			TileGenerator tileGenerator = new TileGenerator(this);
			mMapWorkers[i] = new MapWorker(i, mJobQueue, tileGenerator, mTileManager);
			mMapWorkers[i].start();
		}

		mapActivity.registerMapView(this);

		if (!mMapViewPosition.isValid()) {
			Log.d(TAG, "set default start position");
			setMapCenter(new MapPosition(new GeoPoint(0, 0), (byte) 2, 1));
		}

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		addView(mGLView, params);

		//mMapZoomControls = new MapZoomControls(mapActivity, this);
		//mMapZoomControls.setShowMapZoomControls(true);
		mRotationEnabled = true;

		//mOverlayManager.add(new GenericOverlay(this, new GridOverlay(this)));
		mOverlayManager.add(new BuildingOverlay(this));
		mOverlayManager.add(new LabelingOverlay(this));

		//mOverlayManager.add(new GenericOverlay(this, new TestLineOverlay(this)));
		//mOverlayManager.add(new GenericOverlay(this, new TestOverlay(this)));

		//		if (testRegionZoom)
		//			mRegionLookup = new RegionLookup(this);
		clearMap();

	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		// mMapZoomControlsjonMapViewTouchEvent(motionEvent.getAction()
		// & MotionEvent.ACTION_MASK);

		if (this.isClickable())
			return mTouchEventHandler.handleMotionEvent(motionEvent);

		return false;
	}

	@Override
	protected synchronized void onSizeChanged(int width, int height,
			int oldWidth, int oldHeight) {
		Log.d(TAG, "onSizeChanged: " + width + "x" + height);

		mJobQueue.clear();
		mapWorkersPause(true);

		super.onSizeChanged(width, height, oldWidth, oldHeight);

		mWidth = width;
		mHeight = height;

		if (width != 0 && height != 0)
			mMapViewPosition.setViewport(width, height);

		clearMap();
		mapWorkersProceed();
	}

	public void render() {
		if (!MapView.debugFrameTime)
			mGLView.requestRender();
	}

	/**
	 * @return the current position and zoom level of this MapView.
	 */
	public MapViewPosition getMapPosition() {
		return mMapViewPosition;
	}

	public void enableRotation(boolean enable) {
		mRotationEnabled = enable;

		if (enable) {
			enableCompass(false);
		}
	}

	public void enableCompass(boolean enable) {
		if (enable == mCompassEnabled)
			return;

		mCompassEnabled = enable;

		if (enable)
			enableRotation(false);

		if (enable)
			mCompass.enable();
		else
			mCompass.disable();
	}

	public boolean getCompassEnabled() {
		return mCompassEnabled;
	}

	public boolean getRotationEnabled() {
		return mRotationEnabled;
	}

	/**
	 * Calculates all necessary tiles and adds jobs accordingly.
	 *
	 * @param forceRedraw TODO
	 */
	public void redrawMap(boolean forceRedraw) {
		if (mPausing || mWidth == 0 || mHeight == 0)
			return;

		if (forceRedraw)
			render();

		if (mClearMap) {
			mTileManager.init(mWidth, mHeight);
			mClearMap = false;

			// make sure mMapPosition will be updated
			mMapPosition.zoomLevel = -1;

			// TODO clear overlays
		}

		boolean changed = mMapViewPosition.getMapPosition(mMapPosition);

		// required when not changed?
		if (AndroidUtils.currentThreadIsUiThread())
			mOverlayManager.onUpdate(mMapPosition, changed);

		if (changed) {
			mTileManager.updateMap(mMapPosition);
		}
	}

	private void clearMap() {
		// clear tile and overlay data before next draw
		mClearMap = true;
	}

	/**
	 * @param debugSettings
	 *            the new DebugSettings for this MapView.
	 */
	public void setDebugSettings(DebugSettings debugSettings) {
		mDebugSettings = debugSettings;
		TileGenerator.setDebugSettings(debugSettings);
		clearMap();
	}

	/**
	 * @return the debug settings which are used in this MapView.
	 */
	public DebugSettings getDebugSettings() {
		return mDebugSettings;
	}

	public Map<String, String> getMapOptions() {
		return mMapOptions;
	}

	public MapPosition getMapFileCenter() {
		if (mMapDatabase == null)
			return null;

		MapInfo mapInfo = mMapDatabase.getMapInfo();
		if (mapInfo == null)
			return null;

		GeoPoint startPos = mapInfo.startPosition;

		if (startPos == null)
			startPos = mapInfo.mapCenter;

		if (startPos == null)
			startPos = new GeoPoint(0, 0);

		if (mapInfo.startZoomLevel != null)
			return new MapPosition(startPos, (mapInfo.startZoomLevel).byteValue(), 1);

		return new MapPosition(startPos, (byte) 12, 1);
	}

	/**
	 * Sets the MapDatabase for this MapView.
	 *
	 * @param options
	 *            the new MapDatabase options.
	 * @return true if MapDatabase changed
	 */
	public boolean setMapDatabase(MapOptions options) {
		if (debugDatabase)
			return false;

		Log.i(TAG, "setMapDatabase: " + options.db.name());

		if (mMapOptions != null && mMapOptions.equals(options))
			return true;

		mapWorkersPause(true);

		mJobQueue.clear();
		mMapOptions = options;

		mMapDatabase = null;

		for (int i = 0; i < mNumMapWorkers; i++) {
			MapWorker mapWorker = mMapWorkers[i];

			IMapDatabase mapDatabase = MapDatabaseFactory
					.createMapDatabase(options.db);

			OpenResult result = mapDatabase.open(options);

			if (result != OpenResult.SUCCESS) {
				Log.d(TAG, "failed open db: " + result.getErrorMessage());
			}

			TileGenerator tileGenerator = mapWorker.getTileGenerator();
			tileGenerator.setMapDatabase(mapDatabase);

			// TODO this could be done in a cleaner way..
			if (mMapDatabase == null)
				mMapDatabase = mapDatabase;
		}

		if (options.db == MapDatabases.OSCIMAP_READER ||
				options.db == MapDatabases.MAP_READER)
			MapView.enableClosePolygons = true;
		else
			MapView.enableClosePolygons = false;

		clearMap();

		mapWorkersProceed();

		return true;
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

		clearMap();

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

		clearMap();
	}

	private boolean setRenderTheme(Theme theme) {

		mapWorkersPause(true);

		InputStream inputStream = null;
		try {
			inputStream = theme.getRenderThemeAsStream();
			RenderTheme t = RenderThemeHandler.getRenderTheme(inputStream);
			t.scaleTextSize(1 + (dpi / 240 - 1) * 0.5f);
			// FIXME
			GLRenderer.setRenderTheme(t);
			TileGenerator.setRenderTheme(t);
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

	void destroy() {
		for (MapWorker mapWorker : mMapWorkers) {
			mapWorker.pause();
			mapWorker.interrupt();

			mapWorker.getTileGenerator().getMapDatabase().close();

			try {
				mapWorker.join(10000);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}

		}
	}

	private boolean mPausing = false;

	void onPause() {
		mPausing = true;

		Log.d(TAG, "onPause");
		mJobQueue.clear();
		mapWorkersPause(true);

		if (this.mCompassEnabled)
			mCompass.disable();

	}

	void onResume() {
		Log.d(TAG, "onResume");
		mapWorkersProceed();

		if (this.mCompassEnabled)
			mCompass.enable();

		mPausing = false;
	}

	public void onStop() {
		Log.d(TAG, "onStop");
		mTileManager.destroy();
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
		redrawMap(true);
	}

	/**
	 * Sets the center of the MapView and triggers a redraw.
	 *
	 * @param geoPoint
	 *            the new center point of the map.
	 */
	public void setCenter(GeoPoint geoPoint) {

		mMapViewPosition.setMapCenter(geoPoint);
		redrawMap(true);
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
	public void addJobs(JobTile[] jobs) {
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

	/**
	 * You can add/remove/reorder your Overlays using the List of
	 * {@link Overlay}. The first (index 0) Overlay gets drawn first, the one
	 * with the highest as the last one.
	 *
	 * @return ...
	 */
	public List<Overlay> getOverlays() {
		return this.getOverlayManager();
	}

	public OverlayManager getOverlayManager() {
		return mOverlayManager;
	}

	public TileManager getTileManager() {
		return mTileManager;
	}

	public BoundingBox getBoundingBox() {
		return mMapViewPosition.getViewBox();
	}

	public GeoPoint getCenter() {
		return new GeoPoint(mMapPosition.lat, mMapPosition.lon);
	}
}
