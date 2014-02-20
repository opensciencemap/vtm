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

import org.oscim.backend.AssetAdapter;
import org.oscim.core.Tile;
import org.oscim.layers.GenericLayer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tiling.TileSource;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public abstract class GdxMap implements ApplicationListener {

	protected final Map mMap;
	private final MapRenderer mMapRenderer;

	boolean mRenderRequest;

	public GdxMap() {
		AssetAdapter.g = new GdxAssetAdapter();

		mMap = new Map() {
			@Override
			public int getWidth() {
				return mWidth;
			}

			@Override
			public int getHeight() {
				return mHeight;
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

		};

		mMapRenderer = new MapRenderer(mMap);

	}

	protected void initDefaultLayers(TileSource tileSource, boolean tileGrid, boolean labels,
	        boolean buildings) {
		Layers layers = mMap.layers();

		if (tileSource != null) {
			mMapLayer = mMap.setBaseMap(tileSource);
			mMap.setTheme(InternalRenderTheme.DEFAULT);

			if (buildings)
				layers.add(new BuildingLayer(mMap, mMapLayer));

			if (labels)
				layers.add(new LabelLayer(mMap, mMapLayer));
		}

		if (tileGrid)
			layers.add(new TileGridLayer(mMap));
	}

	// Stage ui;
	// Label fps;
	// BitmapFont font;

	VectorTileLayer mMapLayer;
	GenericLayer mGridLayer;

	int mHeight, mWidth;

	@Override
	public void create() {

		Gdx.graphics.setContinuousRendering(false);
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		mWidth = w;
		mHeight = h;

		mMap.viewport().setScreenSize(w, h);

		//MapPosition p = new MapPosition();
		//p.setZoomLevel(14);
		//p.setPosition(53.08, 8.83);
		//p.setPosition(0.0, 0.0);
		//mMap.setMapPosition(p);

		mMapRenderer.onSurfaceCreated();
		mMapRenderer.onSurfaceChanged(w, h);

		InputMultiplexer mux = new InputMultiplexer();
		MapController controller = new MapController();
		GestureDetector gestureDetector = new GestureDetector(20, 0.5f, 2, 0.05f, controller);
		mux.addProcessor(new TouchHandler());
		mux.addProcessor(gestureDetector);
		Gdx.input.setInputProcessor(mux);

		// ui = new Stage(w, h, false);
		// font = new BitmapFont(false);
		// fps = new Label("fps: 0", new Label.LabelStyle(font, Color.WHITE));
		// fps.setPosition(10, 30);
		// fps.setColor(0, 1, 0, 1);
		// ui.addActor(fps);
		createLayers();
	}

	protected abstract void createLayers();

	@Override
	public void dispose() {

	}

	@Override
	public void render() {
		// GLState.enableVertexArrays(-1, -1);
		// GLState.blend(false);
		// GLState.test(false, false);

		if (mRenderRequest) {
			mRenderRequest = false;
			mMapRenderer.onDrawFrame();
		}
		// Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		// Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		// fps.setText("fps: " + Gdx.graphics.getFramesPerSecond());
		// ui.draw();
	}

	@Override
	public void resize(int w, int h) {
		mWidth = w;
		mHeight = h;

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

	class TouchHandler implements InputProcessor {

		private ViewController mViewport;

		public TouchHandler() {
			mViewport = mMap.viewport();
		}

		private boolean mActiveScale;
		//private boolean mActiveTilt;
		private boolean mActiveRotate;

		private int mPosX, mPosY;

		@Override
		public boolean keyDown(int keycode) {
			if (onKeyDown(keycode))
				return true;

			switch (keycode) {

				case Input.Keys.UP:
					mViewport.moveMap(0, -50);
					mMap.updateMap(true);
					break;
				case Input.Keys.DOWN:
					mViewport.moveMap(0, 50);
					mMap.updateMap(true);
					break;
				case Input.Keys.LEFT:
					mViewport.moveMap(-50, 0);
					mMap.updateMap(true);
					break;
				case Input.Keys.RIGHT:
					mViewport.moveMap(50, 0);
					mMap.updateMap(true);
					break;
				case Input.Keys.M:
					mViewport.scaleMap(1.05f, 0, 0);
					mMap.updateMap(true);
					break;
				case Input.Keys.N:
					mViewport.scaleMap(0.95f, 0, 0);
					mMap.updateMap(true);
					break;
				case Input.Keys.NUM_1:
					mMap.animator().animateZoom(500, 0.5, 0, 0);
					break;
				case Input.Keys.NUM_2:
					mMap.animator().animateZoom(500, 2, 0, 0);
					break;

				case Input.Keys.D:
					mMap.setTheme(InternalRenderTheme.DEFAULT);
					mMap.updateMap(false);
					break;

				case Input.Keys.T:
					mMap.setTheme(InternalRenderTheme.TRONRENDER);
					mMap.updateMap(false);
					break;

				case Input.Keys.R:
					mMap.setTheme(InternalRenderTheme.OSMARENDER);
					mMap.updateMap(false);
					break;

				case Input.Keys.G:
					if (mGridLayer == null) {
						mGridLayer = new TileGridLayer(mMap);
						mGridLayer.setEnabled(true);
						mMap.layers().add(mGridLayer);
					} else {
						if (mGridLayer.isEnabled()) {
							mGridLayer.setEnabled(false);
							mMap.layers().remove(mGridLayer);
						} else {
							mGridLayer.setEnabled(true);
							mMap.layers().add(mGridLayer);
						}
					}
					mMap.render();
					break;
			}
			return false;
		}

		@Override
		public boolean keyUp(int keycode) {
			return false;
		}

		@Override
		public boolean keyTyped(char character) {
			return false;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			if (button == Buttons.MIDDLE) {
				mActiveScale = true;
				mPosY = screenY;
			} else if (button == Buttons.RIGHT) {
				mActiveRotate = true;
				mPosX = screenX;
				mPosY = screenY;
				return true;
			}
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			mActiveScale = false;
			mActiveRotate = false;

			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			boolean changed = false;

			if (!(mActiveScale || mActiveRotate))
				return false;

			if (mActiveScale) {
				// changed = mMapPosition.tilt((screenY - mStartY) / 5f);
				changed = mViewport.scaleMap(1 - (screenY - mPosY) / 100f, 0, 0);
				mPosY = screenY;
			}

			if (mActiveRotate) {
				mViewport.rotateMap((screenX - mPosX) / 500f, 0, 0);
				mPosX = screenX;
				mViewport.tiltMap((screenY - mPosY) / 10f);
				mPosY = screenY;
				changed = true;
			}

			if (changed) {
				mMap.updateMap(true);
			}
			return true;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			mPosX = screenX;
			mPosY = screenY;
			return false;
		}

		@Override
		public boolean scrolled(int amount) {

			if (amount > 0) {

				mMap.animator().animateZoom(250, 0.75f, 0, 0);
			} else {
				float fx = mPosX - mMap.getWidth() / 2;
				float fy = mPosY - mMap.getHeight() / 2;

				mMap.animator().animateZoom(250, 1.333f, fx, fy);
			}
			mMap.updateMap(false);

			return true;
		}
	}

	class MapController implements GestureListener {

		private boolean mayFling = true;

		private boolean mPinch;

		private boolean mBeginScale;
		private float mSumScale;
		private float mSumRotate;

		private boolean mBeginRotate;
		private boolean mBeginTilt;

		private float mPrevX;
		private float mPrevY;

		private float mPrevX2;
		private float mPrevY2;

		private float mFocusX;
		private float mFocusY;

		private double mAngle;
		protected double mPrevPinchWidth = -1;

		protected static final int JUMP_THRESHOLD = 100;
		protected static final double PINCH_ZOOM_THRESHOLD = 5;
		protected static final double PINCH_ROTATE_THRESHOLD = 0.02;
		protected static final float PINCH_TILT_THRESHOLD = 1f;

		private ViewController mViewport;

		public MapController() {
			mViewport = mMap.viewport();
		}

		@Override
		public boolean touchDown(float x, float y, int pointer, int button) {
			mayFling = true;
			mPinch = false;

			return false;
		}

		@Override
		public boolean tap(float x, float y, int count, int button) {
			return false;
		}

		@Override
		public boolean longPress(float x, float y) {
			return false;
		}

		@Override
		public boolean fling(final float velocityX, final float velocityY,
		        int button) {
			//log.debug("fling " + button + " " + velocityX + "/" + velocityY);
			if (mayFling && button == Buttons.LEFT) {
				int m = Tile.SIZE * 4;
				mMap.animator().animateFling((int) velocityX, (int) velocityY, -m, m, -m, m);
				return true;
			}
			return false;
		}

		@Override
		public boolean pan(float x, float y, float deltaX, float deltaY) {
			if (mPinch)
				return true;

			mViewport.moveMap(deltaX, deltaY);
			mMap.updateMap(true);

			return false;
		}

		@Override
		public boolean zoom(float initialDistance, float distance) {
			return false;
		}

		@Override
		public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2,
		        Vector2 pointer1, Vector2 pointer2) {
			mayFling = false;

			if (!mPinch) {
				mPrevX = pointer1.x;
				mPrevY = pointer1.y;
				mPrevX2 = pointer2.x;
				mPrevY2 = pointer2.y;

				double dx = mPrevX - mPrevX2;
				double dy = mPrevY - mPrevY2;

				mAngle = Math.atan2(dy, dx);
				mPrevPinchWidth = Math.sqrt(dx * dx + dy * dy);

				mPinch = true;

				mBeginTilt = false;
				mBeginRotate = false;
				mBeginScale = false;

				return true;
			}

			float x1 = pointer1.x;
			float y1 = pointer1.y;

			//float mx = x1 - mPrevX;
			float my = y1 - mPrevY;

			float x2 = pointer2.x;
			float y2 = pointer2.y;

			float dx = (x1 - x2);
			float dy = (y1 - y2);
			float slope = 0;

			if (dx != 0)
				slope = dy / dx;

			double pinchWidth = Math.sqrt(dx * dx + dy * dy);

			final double deltaPinchWidth = pinchWidth - mPrevPinchWidth;

			double rad = Math.atan2(dy, dx);
			double r = rad - mAngle;

			boolean startScale = (Math.abs(deltaPinchWidth) > PINCH_ZOOM_THRESHOLD);

			boolean changed = false;

			if (!mBeginTilt && (mBeginScale || startScale)) {
				mBeginScale = true;

				float scale = (float) (pinchWidth / mPrevPinchWidth);

				// decrease change of scale by the change of rotation
				// * 20 is just arbitrary
				if (mBeginRotate)
					scale = 1 + ((scale - 1) * Math.max((1 - (float) Math.abs(r) * 20), 0));

				mSumScale *= scale;

				if ((mSumScale < 0.99 || mSumScale > 1.01)
				        && mSumRotate < Math.abs(0.02))
					mBeginRotate = false;

				float fx = (x2 + x1) / 2 - mWidth / 2;
				float fy = (y2 + y1) / 2 - mHeight / 2;

				// log.debug("zoom " + deltaPinchWidth + " " + scale + " " +
				// mSumScale);
				changed = mViewport.scaleMap(scale, fx, fy);
			}

			if (!mBeginRotate && Math.abs(slope) < 1) {
				float my2 = y2 - mPrevY2;
				float threshold = PINCH_TILT_THRESHOLD;

				// log.debug(r + " " + slope + " m1:" + my + " m2:" + my2);

				if ((my > threshold && my2 > threshold)
				        || (my < -threshold && my2 < -threshold)) {
					mBeginTilt = true;
					changed = mViewport.tiltMap(my / 5);
				}
			}

			if (!mBeginTilt
			        && (mBeginRotate || (Math.abs(slope) > 1 && Math.abs(r) > PINCH_ROTATE_THRESHOLD))) {
				// log.debug("rotate: " + mBeginRotate + " " +
				// Math.toDegrees(rad));
				if (!mBeginRotate) {
					mAngle = rad;

					mSumScale = 1;
					mSumRotate = 0;

					mBeginRotate = true;

					mFocusX = (x1 + x2) / 2 - (mWidth / 2);
					mFocusY = (y1 + y2) / 2 - (mHeight / 2);
				} else {
					double da = rad - mAngle;
					mSumRotate += da;

					if (Math.abs(da) > 0.001) {
						double rsin = Math.sin(r);
						double rcos = Math.cos(r);
						float x = (float) (mFocusX * rcos + mFocusY * -rsin - mFocusX);
						float y = (float) (mFocusX * rsin + mFocusY * rcos - mFocusY);

						mViewport.rotateMap(da, x, y);
						changed = true;
					}
				}
				mAngle = rad;
			}

			if (changed) {
				mMap.updateMap(true);
				mPrevPinchWidth = pinchWidth;
				mPrevY2 = y2;

			}

			mPrevX = x1;
			mPrevY = y1;
			mPrevX2 = x2;

			return true;
		}

		@Override
		public boolean panStop(float x, float y, int pointer, int button) {
			return false;
		}

	}

}
