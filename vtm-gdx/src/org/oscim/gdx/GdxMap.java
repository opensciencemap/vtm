package org.oscim.gdx;

import org.oscim.backend.AssetAdapter;
import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.labeling.LabelLayer;
import org.oscim.layers.overlay.BuildingOverlay;
import org.oscim.layers.overlay.GenericOverlay;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.layers.GridRenderLayer;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tilesource.TileSource;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

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

public class GdxMap implements ApplicationListener {

	protected final MapView mMapView;
	private final GLRenderer mMapRenderer;

	boolean mRenderRequest;

	public GdxMap() {
		AssetAdapter.g = new GdxAssetAdapter();

		mMapView = new MapView() {
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
		};

		mMapRenderer = new GLRenderer(mMapView);

	}

	protected void initDefaultMap(TileSource tileSource, boolean tileGrid, boolean labels,
			boolean buildings) {

		if (tileSource != null) {
			mMapLayer = mMapView.setBaseMap(tileSource);
			mMapView.setTheme(InternalRenderTheme.DEFAULT);

			if (buildings)
				mMapView.getLayerManager().add(
						new BuildingOverlay(mMapView, mMapLayer.getTileLayer()));

			if (labels)
				mMapView.getLayerManager().add(new LabelLayer(mMapView,
						mMapLayer.getTileLayer()));
		}

		if (tileGrid)
			mMapView.getLayerManager().add(new GenericOverlay(mMapView,
					new GridRenderLayer()));
	}

	// Stage ui;
	// Label fps;
	// BitmapFont font;

	MapTileLayer mMapLayer;
	GenericOverlay mGridLayer;

	int mHeight, mWidth;

	@Override
	public void create() {

		Gdx.graphics.setContinuousRendering(false);

		Log.logger = new GdxLog();
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		mWidth = w;
		mHeight = h;

		mMapView.getMapViewPosition().setViewport(w, h);
		MapPosition p = new MapPosition();
		p.setZoomLevel(14);
		p.setPosition(53.08, 8.83);
		//p.setPosition(0.0, 0.0);
		mMapView.setMapPosition(p);
		mMapRenderer.onSurfaceCreated();
		mMapRenderer.onSurfaceChanged(w, h);

		InputMultiplexer mux = new InputMultiplexer();
		ViewController controller = new ViewController();
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
	}

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

		mMapView.getMapViewPosition().setViewport(w, h);
		mMapRenderer.onSurfaceChanged(w, h);
		mMapView.render();
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	/**
	 * Update all Layers on Main thread.
	 *
	 * @param forceRedraw
	 *            also render frame FIXME (does nothing atm)
	 */
	void redrawMapInternal(boolean forceRedraw) {
		GLState.blend(false);
		GLState.test(false, false);

		mMapView.updateLayers();

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

	class TouchHandler implements InputProcessor {

		private MapViewPosition mMapPosition;

		public TouchHandler() {
			mMapPosition = mMapView.getMapViewPosition();
		}

		private boolean mActiveScale;
		//private boolean mActiveTilt;
		private boolean mActiveRotate;

		private int mPosX, mPosY;

		@Override
		public boolean keyDown(int keycode) {
			switch (keycode) {

				case Input.Keys.UP:
					mMapPosition.moveMap(0, -50);
					mMapView.updateMap(true);
					break;
				case Input.Keys.DOWN:
					mMapPosition.moveMap(0, 50);
					mMapView.updateMap(true);
					break;
				case Input.Keys.LEFT:
					mMapPosition.moveMap(-50, 0);
					mMapView.updateMap(true);
					break;
				case Input.Keys.RIGHT:
					mMapPosition.moveMap(50, 0);
					mMapView.updateMap(true);
					break;
				case Input.Keys.M:
					mMapPosition.scaleMap(1.05f, 0, 0);
					mMapView.updateMap(true);
					break;
				case Input.Keys.N:
					mMapPosition.scaleMap(0.95f, 0, 0);
					mMapView.updateMap(true);
					break;

				case Input.Keys.D:
					mMapView.setTheme(InternalRenderTheme.DEFAULT);
					mMapView.updateMap(false);
					break;

				case Input.Keys.T:
					mMapView.setTheme(InternalRenderTheme.TRONRENDER);
					mMapView.updateMap(false);
					break;

				case Input.Keys.G:
					if (mGridLayer == null) {
						mGridLayer = new GenericOverlay(mMapView, new GridRenderLayer());
						mGridLayer.setEnabled(true);
						mMapView.getLayerManager().add(mGridLayer);
					} else {
						if (mGridLayer.isEnabled()) {
							mGridLayer.setEnabled(false);
							mMapView.getLayerManager().remove(mGridLayer);
						} else {
							mGridLayer.setEnabled(true);
							mMapView.getLayerManager().add(mGridLayer);
						}
					}
					mMapView.render();
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
				changed = mMapPosition.scaleMap(1 - (screenY - mPosY) / 100f, 0, 0);
				mPosY = screenY;
			}

			if (mActiveRotate) {
				mMapPosition.rotateMap((screenX - mPosX) / 500f, 0, 0);
				mPosX = screenX;
				mMapPosition.tiltMap((screenY - mPosY) / 10f);
				mPosY = screenY;
				changed = true;
			}

			if (changed) {
				mMapView.updateMap(true);
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

				mMapPosition.animateZoom(150, 0.8f, 0, 0);
			} else {
				float fx = mPosX - mMapView.getWidth() / 2;
				float fy = mPosY - mMapView.getHeight() / 2;

				mMapPosition.animateZoom(150, 1.25f, fx, fy);
			}
			mMapView.updateMap(false);

			return true;
		}
	}

	class ViewController implements GestureListener {

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

		private MapViewPosition mMapPosition;

		public ViewController() {
			mMapPosition = mMapView.getMapViewPosition();
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
			//Log.d("", "fling " + button + " " + velocityX + "/" + velocityY);
			if (mayFling && button == Buttons.LEFT) {
				int m = Tile.SIZE * 4;
				mMapPosition.animateFling((int) velocityX, (int) velocityY, -m, m, -m, m);
				return true;
			}
			return false;
		}

		@Override
		public boolean pan(float x, float y, float deltaX, float deltaY) {
			if (mPinch)
				return true;

			mMapPosition.moveMap(deltaX, deltaY);
			mMapView.updateMap(true);

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
					scale = 1 + ((scale - 1) * Math.max(
							(1 - (float) Math.abs(r) * 20), 0));

				mSumScale *= scale;

				if ((mSumScale < 0.99 || mSumScale > 1.01)
						&& mSumRotate < Math.abs(0.02))
					mBeginRotate = false;

				float fx = (x2 + x1) / 2 - mWidth / 2;
				float fy = (y2 + y1) / 2 - mHeight / 2;

				// Log.d(TAG, "zoom " + deltaPinchWidth + " " + scale + " " +
				// mSumScale);
				changed = mMapPosition.scaleMap(scale, fx, fy);
			}

			if (!mBeginRotate && Math.abs(slope) < 1) {
				float my2 = y2 - mPrevY2;
				float threshold = PINCH_TILT_THRESHOLD;

				// Log.d(TAG, r + " " + slope + " m1:" + my + " m2:" + my2);

				if ((my > threshold && my2 > threshold)
						|| (my < -threshold && my2 < -threshold)) {
					mBeginTilt = true;
					changed = mMapPosition.tiltMap(my / 5);
				}
			}

			if (!mBeginTilt
					&& (mBeginRotate || (Math.abs(slope) > 1 && Math.abs(r) > PINCH_ROTATE_THRESHOLD))) {
				// Log.d(TAG, "rotate: " + mBeginRotate + " " +
				// Math.toDegrees(rad));
				if (!mBeginRotate) {
					mAngle = rad;

					mSumScale = 1;
					mSumRotate = 0;

					mBeginRotate = true;

					mFocusX = (mWidth / 2) - (x1 + x2) / 2;
					mFocusY = (mHeight / 2) - (y1 + y2) / 2;
				} else {
					double da = rad - mAngle;
					mSumRotate += da;

					if (Math.abs(da) > 0.001) {
						double rsin = Math.sin(r);
						double rcos = Math.cos(r);
						float x = (float) (mFocusX * rcos + mFocusY * -rsin - mFocusX);
						float y = (float) (mFocusX * rsin + mFocusY * rcos - mFocusY);

						mMapPosition.rotateMap(da, x, y);
						changed = true;
					}
				}
				mAngle = rad;
			}

			if (changed) {
				mMapView.updateMap(true);
				mPrevPinchWidth = pinchWidth;
				mPrevY2 = y2;

			}

			mPrevX = x1;
			mPrevY = y1;
			mPrevX2 = x2;

			return true;
		}

	}

}
