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
import java.util.ArrayList;
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
import org.oscim.overlay.GenericOverlay;
import org.oscim.overlay.LabelingOverlay;
import org.oscim.overlay.Overlay;
import org.oscim.overlay.OverlayManager;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLView;
import org.oscim.renderer.TileManager;
import org.oscim.renderer.overlays.BuildingOverlay2;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.RenderThemeHandler;
import org.oscim.theme.Theme;
import org.oscim.utils.AndroidUtils;
import org.xml.sax.SAXException;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

/**
 * A MapView shows a map on the display of the device. It handles all user input
 * and touch gestures to move and zoom the map.
 */
public class MapView extends RelativeLayout {

	final static String TAG = "MapView";

	public static final boolean debugFrameTime = false;
	public static final boolean testRegionZoom = false;
	private static final boolean debugDatabase = false;

	public boolean enableRotation = false;
	public boolean enableCompass = false;

	private final MapViewPosition mMapViewPosition;
	private final MapPosition mMapPosition;

	private final MapZoomControls mMapZoomControls;

	private final TouchHandler mTouchEventHandler;
	private final Compass mCompass;

	//private MapDatabases mMapDatabaseType;

	private TileManager mTileManager;
	private final OverlayManager mOverlayManager;

	private final GLView mGLView;
	private final JobQueue mJobQueue;

	// TODO use 1 download and 1 generator thread instead
	private final MapWorker mMapWorkers[];
	private final int mNumMapWorkers = 4;

	private MapOptions mMapOptions;
	private IMapDatabase mMapDatabase;

	private DebugSettings debugSettings;
	private String mRenderTheme;

	private boolean mClearTiles;

	// FIXME: keep until old pbmap reader is removed
	public static boolean enableClosePolygons = false;

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

		// TODO set tilesize, make this dpi dependent
		Tile.TILE_SIZE = 400;

		MapActivity mapActivity = (MapActivity) context;

		debugSettings = new DebugSettings(false, false, false, false);

		mMapViewPosition = new MapViewPosition(this);
		mMapPosition = new MapPosition();

		mOverlayManager = new OverlayManager();

		mTouchEventHandler = new TouchHandler(mapActivity, this);

		mCompass = new Compass(mapActivity, this);

		mJobQueue = new JobQueue();

		mTileManager = TileManager.create(this);

		mGLView = new GLView(context, this);

		mMapWorkers = new MapWorker[mNumMapWorkers];

		for (int i = 0; i < mNumMapWorkers; i++) {
			TileGenerator tileGenerator = new TileGenerator(this);
			mMapWorkers[i] = new MapWorker(i, mJobQueue, tileGenerator, mTileManager);
			mMapWorkers[i].start();
		}

		mapActivity.registerMapView(this);

		if (!mMapViewPosition.isValid()) {
			Log.d(TAG, "set default start position");
			setMapCenter(getStartPosition());
		}

		LayoutParams params = new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		addView(mGLView, params);

		mMapZoomControls = new MapZoomControls(mapActivity, this);
		mMapZoomControls.setShowMapZoomControls(true);
		enableRotation = true;

		//mOverlayManager.add(new GenericOverlay(this, new GridOverlay(this)));
		mOverlayManager.add(new GenericOverlay(this, new BuildingOverlay2(this)));
		mOverlayManager.add(new LabelingOverlay(this));

		//		mOverlayManager.add(new GenericOverlay(this, new TestOverlay(this)));

		//		ArrayList<OverlayItem> pList = new ArrayList<OverlayItem>();
		//		pList.add(new OverlayItem("title", "description", new GeoPoint(53.067221, 8.78767)));
		//		Overlay overlay = new ItemizedIconOverlay<OverlayItem>(this, context, pList, null);
		//		mOverlayManager.add(overlay);

		//		ArrayList<OverlayItem> pList = new ArrayList<OverlayItem>();
		//		pList.add(new ExtendedOverlayItem("Bremen", "description",
		//				new GeoPoint(53.047221, 8.78767), context));
		//		pList.add(new ExtendedOverlayItem("New York", "description",
		//				new GeoPoint(40.4251, -74.021), context));
		//		pList.add(new ExtendedOverlayItem("Tokyo", "description",
		//				new GeoPoint(35.4122, 139.4130), context));
		//		Overlay overlay = new ItemizedOverlayWithBubble<OverlayItem>(this, context, pList, null);
		//		mOverlayManager.add(overlay);

		//		PathOverlay pathOverlay = new PathOverlay(this, Color.BLUE, context);
		//		pathOverlay.addGreatCircle(
		//				new GeoPoint(53.047221, 8.78767),
		//				new GeoPoint(40.4251, -74.021));
		//		//		pathOverlay.addPoint(new GeoPoint(53.047221, 8.78767));
		//		//		pathOverlay.addPoint(new GeoPoint(53.067221, 8.78767));
		//		mOverlayManager.add(pathOverlay);

		//		mMapViewPosition.animateTo(new GeoPoint(53.067221, 8.78767));

		//		if (testRegionZoom)
		//			mRegionLookup = new RegionLookup(this);

	}

	public void render() {
		if (!MapView.debugFrameTime)
			mGLView.requestRender();
	}

	//	/**
	//	 * @return the map database which is used for reading map files.
	//	 */
	//	public IMapDatabase getMapDatabase() {
	//		return mMapDatabase;
	//	}

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

		if (AndroidUtils.currentThreadIsUiThread()) {
			boolean changed = mMapViewPosition.getMapPosition(mMapPosition, null);

			mOverlayManager.onUpdate(mMapPosition, changed);
		}
		mTileManager.updateMap(mClearTiles);
		mClearTiles = false;
	}

	public void clearAndRedrawMap() {
		if (mPausing || this.getWidth() == 0 || this.getHeight() == 0)
			return;

		//if (AndroidUtils.currentThreadIsUiThread())
		mTileManager.updateMap(true);
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
	 * @param options
	 *            the new MapDatabase options.
	 * @return ...
	 */
	public boolean setMapDatabase(MapOptions options) {
		if (debugDatabase)
			return false;

		Log.i(TAG, "setMapDatabase " + options.db.name());

		if (mMapOptions != null && mMapOptions.equals(options))
			return true;

		mapWorkersPause(true);

		mJobQueue.clear();
		mClearTiles = true;
		mMapOptions = options;

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
		}

		if (options.db == MapDatabases.OSCIMAP_READER)
			MapView.enableClosePolygons = true;
		else
			MapView.enableClosePolygons = false;

		mapWorkersProceed();

		return true;
	}

	public String getRenderTheme() {
		return mRenderTheme;
	}

	/**
	 * Sets the internal theme which is used for rendering the map.
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
		clearAndRedrawMap();
		return ret;
	}

	/**
	 * Sets the theme file which is used for rendering the map.
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
			// FIXME somehow...
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

	@Override
	protected synchronized void onSizeChanged(int width, int height,
			int oldWidth, int oldHeight) {

		mJobQueue.clear();
		mapWorkersPause(true);
		Log.d(TAG, "onSizeChanged" + width + " " + height);
		super.onSizeChanged(width, height, oldWidth, oldHeight);

		if (width != 0 && height != 0)
			mMapViewPosition.setViewport(width, height);

		TileManager.onSizeChanged(width, height);

		mapWorkersProceed();
	}

	//	@Override
	//	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	//		//		super.onLayout(changed, left, top, right, bottom);
	//		mMapZoomControls.onLayout(changed, left, top, right, bottom);
	//	}

	void destroy() {
		for (MapWorker mapWorker : mMapWorkers) {
			mapWorker.pause();
			mapWorker.interrupt();

			try {
				// FIXME this hangs badly sometimes,
				// just let it crash...
				mapWorker.join(10000);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
			TileGenerator tileGenerator = mapWorker.getTileGenerator();
			tileGenerator.getMapDatabase().close();
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

	//	byte limitZoomLevel(byte zoom) {
	//		if (mMapZoomControls == null)
	//			return zoom;
	//
	//		return (byte) Math.max(Math.min(zoom, getMaximumPossibleZoomLevel()),
	//				mMapZoomControls.getZoomLevelMin());
	//	}

	/**
	 * Sets the center and zoom level of this MapView and triggers a redraw.
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

	/**
	 * You can add/remove/reorder your Overlays using the List of
	 * {@link Overlay}. The first (index 0) Overlay gets drawn first, the one
	 * with the highest as the last one.
	 * @return ...
	 */
	public List<Overlay> getOverlays() {
		return this.getOverlayManager();
	}

	public OverlayManager getOverlayManager() {
		return mOverlayManager;
	}

	public BoundingBox getBoundingBox() {
		return mMapViewPosition.getViewBox();
	}

	public GeoPoint getCenter() {
		return new GeoPoint(mMapPosition.lat, mMapPosition.lon);
	}

	//	@Override
	//	protected void onLayout(boolean changed, int l, int t, int r, int b) {
	//		// TODO Auto-generated method stub
	//
	//	}
	//	
	//	@Override
	//	protected void onMeasure()) {
	//		// TODO Auto-generated method stub
	//
	//	}
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
