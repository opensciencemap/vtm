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
package org.mapsforge.android;

import java.io.File;
import java.io.FileNotFoundException;

import org.mapsforge.android.input.TouchHandler;
import org.mapsforge.android.mapgenerator.IMapGenerator;
import org.mapsforge.android.mapgenerator.JobParameters;
import org.mapsforge.android.mapgenerator.JobQueue;
import org.mapsforge.android.mapgenerator.JobTheme;
import org.mapsforge.android.mapgenerator.MapDatabaseFactory;
import org.mapsforge.android.mapgenerator.MapDatabaseInternal;
import org.mapsforge.android.mapgenerator.MapGeneratorFactory;
import org.mapsforge.android.mapgenerator.MapGeneratorInternal;
import org.mapsforge.android.mapgenerator.MapWorker;
import org.mapsforge.android.rendertheme.ExternalRenderTheme;
import org.mapsforge.android.rendertheme.InternalRenderTheme;
import org.mapsforge.android.utils.GlConfigChooser;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.MapPosition;
import org.mapsforge.database.FileOpenResult;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.mapfile.MapDatabase;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * A MapView shows a map on the display of the device. It handles all user input and touch gestures to move and zoom the
 * map. This MapView also includes a scale bar and zoom controls. The {@link #getController()} method returns a
 * {@link MapController} to programmatically modify the position and zoom level of the map.
 * <p>
 * This implementation supports offline map rendering as well as downloading map images (tiles) over an Internet
 * connection. The operation mode of a MapView can be set in the constructor and changed at runtime with the
 * {@link #setMapGeneratorInternal(IMapGenerator)} method. Some MapView parameters depend on the selected operation
 * mode.
 * <p>
 * In offline rendering mode a special database file is required which contains the map data. Map files can be stored in
 * any folder. The current map file is set by calling {@link #setMapFile(String)}. To retrieve the current
 * {@link MapDatabase}, use the {@link #getMapDatabase()} method.
 * <p>
 */
public class MapView extends GLSurfaceView {

	final static String TAG = "MapView";

	/**
	 * Default render theme of the MapView.
	 */
	public static final InternalRenderTheme DEFAULT_RENDER_THEME = InternalRenderTheme.OSMARENDER;

	private static final float DEFAULT_TEXT_SCALE = 1;

	private final MapController mMapController;
	// private final MapMover mMapMover;
	// private final ZoomAnimator mZoomAnimator;

	private final MapScaleBar mMapScaleBar;
	private final MapViewPosition mMapViewPosition;

	private final MapZoomControls mMapZoomControls;
	private final Projection mProjection;
	private final TouchHandler mTouchEventHandler;

	private IMapDatabase mMapDatabase;
	private IMapGenerator mMapGenerator;
	private MapRenderer mMapRenderer;
	private JobQueue mJobQueue;
	private MapWorker mMapWorker;
	private JobParameters mJobParameters;
	private DebugSettings mDebugSettings;
	private String mMapFile;

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity} .
	 */
	public MapView(Context context) {
		this(context, null,
				MapGeneratorFactory.createMapGenerator(MapGeneratorInternal.GL_RENDERER),
				MapDatabaseFactory.createMapDatabase(MapDatabaseInternal.MAP_READER));
	}

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @param attributeSet
	 *            a set of attributes.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity} .
	 */
	public MapView(Context context, AttributeSet attributeSet) {
		this(context, attributeSet,
				MapGeneratorFactory.createMapGenerator(attributeSet),
				MapDatabaseFactory.createMapDatabase(attributeSet));
	}

	/**
	 * @param context
	 *            the enclosing MapActivity instance.
	 * @param mapGenerator
	 *            the MapGenerator for this MapView.
	 * @throws IllegalArgumentException
	 *             if the context object is not an instance of {@link MapActivity} .
	 */
	public MapView(Context context, IMapGenerator mapGenerator) {
		this(context, null, mapGenerator, MapDatabaseFactory
				.createMapDatabase(MapDatabaseInternal.MAP_READER));
	}

	private MapView(Context context, AttributeSet attributeSet,
			IMapGenerator mapGenerator, IMapDatabase mapDatabase) {

		super(context, attributeSet);

		if (!(context instanceof MapActivity)) {
			throw new IllegalArgumentException(
					"context is not an instance of MapActivity");
		}

		setWillNotDraw(true);
		setWillNotCacheDrawing(true);

		MapActivity mapActivity = (MapActivity) context;

		mDebugSettings = new DebugSettings(false, false, false);

		mJobParameters = new JobParameters(DEFAULT_RENDER_THEME, DEFAULT_TEXT_SCALE);
		mMapController = new MapController(this);

		// mMapDatabase = MapDatabaseFactory.createMapDatabase(MapDatabaseInternal.POSTGIS_READER);
		// mMapDatabase = MapDatabaseFactory.createMapDatabase(MapDatabaseInternal.JSON_READER);
		mMapDatabase = mapDatabase;

		mMapViewPosition = new MapViewPosition(this);
		mMapScaleBar = new MapScaleBar(this);
		mMapZoomControls = new MapZoomControls(mapActivity, this);
		mProjection = new MapViewProjection(this);
		mTouchEventHandler = new TouchHandler(mapActivity, this);

		mJobQueue = new JobQueue(this);
		mMapWorker = new MapWorker(this);
		mMapWorker.start();
		// mMapMover = new MapMover(this);
		// mMapMover.start();
		// mZoomAnimator = new ZoomAnimator(this);
		// mZoomAnimator.start();

		setMapGeneratorInternal(mapGenerator);

		GeoPoint startPoint = mMapGenerator.getStartPoint();
		if (startPoint != null) {
			mMapViewPosition.setMapCenter(startPoint);
		}

		Byte startZoomLevel = mMapGenerator.getStartZoomLevel();
		if (startZoomLevel != null) {
			mMapViewPosition.setZoomLevel(startZoomLevel.byteValue());
		}

		mapActivity.registerMapView(this);

		setEGLConfigChooser(new GlConfigChooser());
		setEGLContextClientVersion(2);

		mMapRenderer = mMapGenerator.getMapRenderer(this);
		setRenderer(mMapRenderer);

		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mMapWorker.setMapRenderer(mMapRenderer);
	}

	/**
	 * @return the MapController for this MapView.
	 */
	public MapController getController() {
		return mMapController;
	}

	/**
	 * @return the debug settings which are used in this MapView.
	 */
	public DebugSettings getDebugSettings() {
		return mDebugSettings;
	}

	/**
	 * @return the job queue which is used in this MapView.
	 */
	public JobQueue getJobQueue() {
		return mJobQueue;
	}

	/**
	 * @return the map database which is used for reading map files.
	 */
	public IMapDatabase getMapDatabase() {
		return mMapDatabase;
	}

	/**
	 * @return the currently used map file.
	 */
	public String getMapFile() {
		return mMapFile;
	}

	/**
	 * @return the currently used MapGenerator (may be null).
	 */
	public IMapGenerator getMapGenerator() {
		return mMapGenerator;
	}

	// /**
	// * @return the MapMover which is used by this MapView.
	// */
	// public MapMover getMapMover() {
	// return mMapMover;
	// }

	/**
	 * @return the current position and zoom level of this MapView.
	 */
	public MapViewPosition getMapPosition() {
		return mMapViewPosition;
	}

	/**
	 * @return the scale bar which is used in this MapView.
	 */
	public MapScaleBar getMapScaleBar() {
		return mMapScaleBar;
	}

	/**
	 * @return the zoom controls instance which is used in this MapView.
	 */
	public MapZoomControls getMapZoomControls() {
		return mMapZoomControls;
	}

	/**
	 * @return the currently used projection of the map. Do not keep this object for a longer time.
	 */
	public Projection getProjection() {
		return mProjection;
	}

	// /**
	// * @return true if the ZoomAnimator is currently running, false otherwise.
	// */
	// public boolean isZoomAnimatorRunning() {
	// return mZoomAnimator.isExecuting();
	// }

	// @Override
	// public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
	// return mMapMover.onKeyDown(keyCode, keyEvent);
	// }
	//
	// @Override
	// public boolean onKeyUp(int keyCode, KeyEvent keyEvent) {
	// return mMapMover.onKeyUp(keyCode, keyEvent);
	// }

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		return mTouchEventHandler.handleMotionEvent(motionEvent);
	}

	// @Override
	// public boolean onTrackballEvent(MotionEvent motionEvent) {
	// return mMapMover.onTrackballEvent(motionEvent);
	// }

	/**
	 * Calculates all necessary tiles and adds jobs accordingly.
	 */
	public synchronized void redrawTiles() {
		if (getWidth() <= 0 || getHeight() <= 0)
			return;

		mMapRenderer.redrawTiles(false);
	}

	void clearAndRedrawMapView() {
		if (getWidth() <= 0 || getHeight() <= 0)
			return;

		mMapRenderer.redrawTiles(true);
	}

	/**
	 * Sets the visibility of the zoom controls.
	 * 
	 * @param showZoomControls
	 *            true if the zoom controls should be visible, false otherwise.
	 */
	public void setBuiltInZoomControls(boolean showZoomControls) {
		mMapZoomControls.setShowMapZoomControls(showZoomControls);

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
		setCenterAndZoom(mapPosition);
	}

	/**
	 * @param debugSettings
	 *            the new DebugSettings for this MapView.
	 */
	public void setDebugSettings(DebugSettings debugSettings) {
		mDebugSettings = debugSettings;

		clearAndRedrawMapView();
	}

	/**
	 * Sets the map file for this MapView.
	 * 
	 * @param mapFile
	 *            the path to the map file.
	 * @return true if the map file was set correctly, false otherwise.
	 */
	public boolean setMapFile(String mapFile) {
		FileOpenResult fileOpenResult = null;

		Log.d(TAG, "set mapfile " + mapFile);

		if (mapFile != null && mapFile.equals(mMapFile)) {
			// same map file as before
			return false;
		}

		// mZoomAnimator.pause();
		// mMapMover.pause();
		mMapWorker.pause();
		// mZoomAnimator.awaitPausing();
		// mMapMover.awaitPausing();
		mMapWorker.awaitPausing();
		// mZoomAnimator.proceed();
		// mMapMover.stopMove();
		// mMapMover.proceed();
		mMapWorker.proceed();

		mMapDatabase.closeFile();

		if (mapFile != null)
			fileOpenResult = mMapDatabase.openFile(new File(mapFile));
		else
			fileOpenResult = mMapDatabase.openFile(null);

		if (fileOpenResult != null && fileOpenResult.isSuccess()) {
			mMapFile = mapFile;

			GeoPoint startPoint = mMapGenerator.getStartPoint();
			if (startPoint != null) {
				Log.d(TAG, "mapfile got startpoint");
				mMapViewPosition.setMapCenter(startPoint);
			}

			Byte startZoomLevel = mMapGenerator.getStartZoomLevel();
			if (startZoomLevel != null) {
				Log.d(TAG, "mapfile got start zoomlevel");
				mMapViewPosition.setZoomLevel(startZoomLevel.byteValue());
			}

			clearAndRedrawMapView();
			Log.d(TAG, "mapfile set");
			return true;
		}

		mMapFile = null;
		Log.d(TAG, "loading mapfile failed");

		return false;
	}

	/**
	 * Sets the MapGenerator for this MapView.
	 * 
	 * @param mapGenerator
	 *            the new MapGenerator.
	 */
	public void setMapGenerator(IMapGenerator mapGenerator) {

		if (mMapGenerator != mapGenerator) {
			setMapGeneratorInternal(mapGenerator);

			clearAndRedrawMapView();
		}
	}

	private void setMapGeneratorInternal(IMapGenerator mapGenerator) {
		if (mapGenerator == null) {
			throw new IllegalArgumentException("mapGenerator must not be null");
		}

		mapGenerator.setMapDatabase(mMapDatabase);

		mMapGenerator = mapGenerator;
		mMapWorker.setMapGenerator(mMapGenerator);

	}

	/**
	 * Sets the MapDatabase for this MapView.
	 * 
	 * @param mapDatabase
	 *            the new MapDatabase.
	 */
	public void setMapDatabase(IMapDatabase mapDatabase) {
		Log.d(TAG, "setMapDatabase " + mapDatabase.getClass());
		if (mMapDatabase != mapDatabase) {

			if (mMapDatabase != null)
				mMapDatabase.closeFile();

			setMapDatabaseInternal(mapDatabase);

			// clearAndRedrawMapView();
		}
	}

	private void setMapDatabaseInternal(IMapDatabase mapDatabase) {
		if (mapDatabase == null) {
			throw new IllegalArgumentException("MapDatabase must not be null");
		}

		if (!mMapWorker.isPausing()) {
			mMapWorker.pause();
			mMapWorker.awaitPausing();
		}

		mJobQueue.clear();
		mMapDatabase = mapDatabase;
		mMapGenerator.setMapDatabase(mMapDatabase);

		Log.d(TAG, "setMapDatabaseInternal " + mapDatabase.getClass());

		String mapFile = mMapFile;
		mMapFile = null;
		setMapFile(mapFile);

		mMapWorker.proceed();

		// mMapWorker.setMapDatabase(mMapDatabase);

	}

	/**
	 * Sets the internal theme which is used for rendering the map.
	 * 
	 * @param internalRenderTheme
	 *            the internal rendering theme.
	 * @throws IllegalArgumentException
	 *             if the supplied internalRenderTheme is null.
	 */
	public void setRenderTheme(InternalRenderTheme internalRenderTheme) {
		if (internalRenderTheme == null) {
			throw new IllegalArgumentException("render theme must not be null");
		}

		Log.d(TAG, "set rendertheme " + internalRenderTheme);
		mJobParameters = new JobParameters(internalRenderTheme, mJobParameters.textScale);

		clearAndRedrawMapView();
	}

	/**
	 * Sets the theme file which is used for rendering the map.
	 * 
	 * @param renderThemePath
	 *            the path to the XML file which defines the rendering theme.
	 * @throws IllegalArgumentException
	 *             if the supplied internalRenderTheme is null.
	 * @throws FileNotFoundException
	 *             if the supplied file does not exist, is a directory or cannot be read.
	 */
	public void setRenderTheme(String renderThemePath) throws FileNotFoundException {
		if (renderThemePath == null) {
			throw new IllegalArgumentException("render theme path must not be null");
		}

		JobTheme jobTheme = new ExternalRenderTheme(renderThemePath);
		mJobParameters = new JobParameters(jobTheme, mJobParameters.textScale);

		clearAndRedrawMapView();
	}

	/**
	 * Sets the text scale for the map rendering. Has no effect in downloading mode.
	 * 
	 * @param textScale
	 *            the new text scale for the map rendering.
	 */
	public void setTextScale(float textScale) {
		mJobParameters = new JobParameters(mJobParameters.jobTheme, textScale);
		clearAndRedrawMapView();
	}

	/**
	 * Zooms in or out by the given amount of zoom levels.
	 * 
	 * @param zoomLevelDiff
	 *            the difference to the current zoom level.
	 * @return true if the zoom level was changed, false otherwise.
	 */
	public boolean zoom(byte zoomLevelDiff) {

		int z = mMapViewPosition.getZoomLevel() + zoomLevelDiff;
		if (zoomLevelDiff > 0) {
			// check if zoom in is possible
			if (z > getMaximumPossibleZoomLevel()) {
				return false;
			}

		} else if (zoomLevelDiff < 0) {
			// check if zoom out is possible
			if (z < mMapZoomControls.getZoomLevelMin()) {
				return false;
			}
		}

		mMapViewPosition.setZoomLevel((byte) z);

		redrawTiles();

		return true;
	}

	// @Override
	// protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
	// super.onLayout(changed, left, top, right, bottom);
	// // mMapZoomControls.onLayout(changed, left, top, right, bottom);
	// }

	// @Override
	// protected final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	// // find out how big the zoom controls should be
	// mMapZoomControls.measure(
	// MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),
	// MeasureSpec.AT_MOST),
	// MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec),
	// MeasureSpec.AT_MOST));
	//
	// // make sure that MapView is big enough to display the zoom controls
	// setMeasuredDimension(
	// Math.max(MeasureSpec.getSize(widthMeasureSpec),
	// mMapZoomControls.getMeasuredWidth()),
	// Math.max(MeasureSpec.getSize(heightMeasureSpec),
	// mMapZoomControls.getMeasuredHeight()));
	// }

	@Override
	protected synchronized void onSizeChanged(int width, int height, int oldWidth,
			int oldHeight) {
		mMapWorker.pause();
		mMapWorker.awaitPausing();
		super.onSizeChanged(width, height, oldWidth, oldHeight);
		mMapWorker.proceed();

		// redrawTiles();
	}

	void destroy() {
		// mMapMover.interrupt();
		mMapWorker.interrupt();
		// mZoomAnimator.interrupt();

		try {
			mMapWorker.join();
		} catch (InterruptedException e) {
			// restore the interrupted status
			Thread.currentThread().interrupt();
		}

		mMapScaleBar.destroy();
		mMapDatabase.closeFile();

	}

	/**
	 * @return the maximum possible zoom level.
	 */
	byte getMaximumPossibleZoomLevel() {
		return (byte) Math.min(mMapZoomControls.getZoomLevelMax(),
				mMapGenerator.getZoomLevelMax());
	}

	/**
	 * @return true if the current center position of this MapView is valid, false otherwise.
	 */
	boolean hasValidCenter() {
		if (!mMapViewPosition.isValid()) {
			return false;
		} else if (!mMapDatabase.hasOpenFile()
				|| !mMapDatabase.getMapFileInfo().boundingBox.contains(getMapPosition()
						.getMapCenter())) {
			return false;
		}

		return true;
	}

	byte limitZoomLevel(byte zoom) {
		return (byte) Math.max(Math.min(zoom, getMaximumPossibleZoomLevel()),
				mMapZoomControls.getZoomLevelMin());
	}

	@Override
	public void onPause() {
		super.onPause();
		mMapWorker.pause();
		// mMapMover.pause();
		// mZoomAnimator.pause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mMapWorker.proceed();
		// mMapMover.proceed();
		// mZoomAnimator.proceed();
	}

	/**
	 * Sets the center and zoom level of this MapView and triggers a redraw.
	 * 
	 * @param mapPosition
	 *            the new map position of this MapView.
	 */
	void setCenterAndZoom(MapPosition mapPosition) {

		// if (hasValidCenter()) {
		// // calculate the distance between previous and current position
		// MapPosition mapPositionOld = mapViewPosition.getMapPosition();

		// GeoPoint geoPointOld = mapPositionOld.geoPoint;
		// GeoPoint geoPointNew = mapPosition.geoPoint;
		// double oldPixelX =
		// MercatorProjection.longitudeToPixelX(geoPointOld.getLongitude(),
		// mapPositionOld.zoomLevel);
		// double newPixelX =
		// MercatorProjection.longitudeToPixelX(geoPointNew.getLongitude(),
		// mapPosition.zoomLevel);
		//
		// double oldPixelY =
		// MercatorProjection.latitudeToPixelY(geoPointOld.getLatitude(),
		// mapPositionOld.zoomLevel);
		// double newPixelY =
		// MercatorProjection.latitudeToPixelY(geoPointNew.getLatitude(),
		// mapPosition.zoomLevel);

		// float matrixTranslateX = (float) (oldPixelX - newPixelX);
		// float matrixTranslateY = (float) (oldPixelY - newPixelY);
		// frameBuffer.matrixPostTranslate(matrixTranslateX,
		// matrixTranslateY);
		// }
		//
		mMapViewPosition.setMapCenterAndZoomLevel(mapPosition);
		// mapZoomControls.onZoomLevelChange(mapViewPosition.getZoomLevel());
		redrawTiles();
	}

	/**
	 * @return MapPosition
	 */
	public MapViewPosition getMapViewPosition() {
		return mMapViewPosition;
	}

	/**
	 * @return current JobParameters
	 */
	public JobParameters getJobParameters() {
		return mJobParameters;
	}

	/**
	 * @return MapWorker
	 */
	public MapWorker getMapWorker() {
		return mMapWorker;
	}
}
