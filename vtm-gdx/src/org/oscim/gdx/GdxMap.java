package org.oscim.gdx;

import org.oscim.backend.AssetAdapter;
import org.oscim.backend.Log;
import org.oscim.backend.input.MotionEvent;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap4.OSciMap4TileSource;
import org.oscim.view.MapRenderCallback;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputProcessor;

public class GdxMap implements ApplicationListener, MapRenderCallback {

	private final MapView mMapView;
	private final GLRenderer mMapRenderer;

	public GdxMap() {
		AssetAdapter.g = new GdxAssetAdapter();

		mMapView = new MapView(this);
		mMapRenderer = new GLRenderer(mMapView);
	}

	// Stage ui;
	// Label fps;
	// BitmapFont font;
	MapTileLayer mMapLayer;

	@Override
	public void create() {

		Gdx.graphics.setContinuousRendering(false);

		if (Log.logger == null)
			Log.logger = new GdxLog();

		Gdx.app.log("gdx says", "Hi!");
		Log.d("vtm says", "Hi!");

		int w = Gdx.graphics.getWidth();
		int h = Gdx.graphics.getHeight();
		mWidth = w;
		mHeight = h;

		//TileSource tileSource = new OSciMap2TileSource();
		//tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/map-live");
		TileSource tileSource = new OSciMap4TileSource();
		tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/testing");

		mMapLayer = mMapView.setBaseMap(tileSource);
		mMapLayer.setRenderTheme(InternalRenderTheme.DEFAULT);

		//mMapView.getLayerManager().add(new GenericOverlay(mMapView, new
		//                                                GridRenderLayer(mMapView)));

		mMapView.getMapViewPosition().setViewport(w, h);

		mMapRenderer.onSurfaceCreated();
		mMapRenderer.onSurfaceChanged(w, h);

		Gdx.input.setInputProcessor(new TouchHandler());

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

	//private int fpsCnt = 0;

	@Override
	public void render() {
		// GLState.enableVertexArrays(-1, -1);
		// GLState.blend(false);
		// GLState.test(false, false);

		mMapRenderer.onDrawFrame();

		// Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		// Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		//		int f = Gdx.graphics.getFramesPerSecond();
		//		if (f != fpsCnt) {
		//			Log.d("fps", ">" + f);
		//			fpsCnt = f;
		//		}

		// fps.setText("fps: " + Gdx.graphics.getFramesPerSecond());
		// ui.draw();
	}

	@Override
	public void resize(int w, int h) {
		mWidth = w;
		mHeight = h;

		mMapView.getMapViewPosition().setViewport(w, h);
		mMapRenderer.onSurfaceChanged(w, h);
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
		//if (forceRedraw && !mClearMap)
		//	Gdx.graphics.requestRendering();

		mMapView.updateLayers();

		//if (mClearMap) {
		Gdx.graphics.requestRendering();
		mClearMap = false;
		//}
	}

	private boolean mClearMap;

	public void clearMap() {
		mClearMap = true;
	}

	/* private */boolean mWaitRedraw;
	private final Runnable mRedrawRequest = new Runnable() {
		@Override
		public void run() {
			mWaitRedraw = false;
			redrawMapInternal(false);
		}
	};

	@Override
	public void updateMap(boolean forceRender) {
		if (!mWaitRedraw) {
			mWaitRedraw = true;
			Gdx.app.postRunnable(mRedrawRequest);
		}
	}

	@Override
	public void renderMap() {
		if (mClearMap)
			updateMap(false);
		else
			Gdx.graphics.requestRendering();
	}

	int mHeight, mWidth;

	@Override
	public int getWidth() {
		return mWidth;
	}

	@Override
	public int getHeight() {
		return mHeight;
	}

	class GdxMotionEvent extends MotionEvent {

		@Override
		public int getAction() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getX() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getY() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getX(int idx) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getY(int idx) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getPointerCount() {
			// TODO Auto-generated method stub
			return 0;
		}

	}

	class TouchHandler implements InputProcessor {

		private MapViewPosition mMapPosition;

		public TouchHandler() {
			mMapPosition = mMapView.getMapViewPosition();
		}

		private boolean mActiveScale;
		private boolean mActiveTilt;
		private boolean mActiveRotate;

		private int mPosX, mPosY;

		@Override
		public boolean keyDown(int keycode) {
			switch (keycode) {
			//
			// case Input.Keys.UP:
			// mMapPosition.moveMap(0, -50);
			// mMapView.redrawMap(true);
			// break;
			// case Input.Keys.DOWN:
			// mMapPosition.moveMap(0, 50);
			// mMapView.redrawMap(true);
			// break;
			// case Input.Keys.LEFT:
			// mMapPosition.moveMap(-50, 0);
			// mMapView.redrawMap(true);
			// break;
			// case Input.Keys.RIGHT:
			// mMapPosition.moveMap(50, 0);
			// mMapView.redrawMap(true);
			// break;
			//
			case Input.Keys.R:
				mMapLayer.setRenderTheme(InternalRenderTheme.DEFAULT);
				mMapView.updateMap(false);
				break;

			case Input.Keys.T:
				mMapLayer.setRenderTheme(InternalRenderTheme.TRONRENDER);
				mMapView.updateMap(false);
				break;

			}
			return true;
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
				// mActiveTilt = true;
				mPosY = screenY;
			} else if (button == Buttons.RIGHT) {
				mActiveRotate = true;
				mPosX = screenX;
				mPosY = screenY;
			}
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			// Log.d(TAG, "touch up " + pointer);
			mActiveScale = false;
			mActiveTilt = false;
			mActiveRotate = false;

			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			boolean changed = false;

			if (mActiveScale) {
				// changed = mMapPosition.tilt((screenY - mStartY) / 5f);
				changed = mMapPosition.scaleMap(1 - (screenY - mPosY) / 100f, 0, 0);
				mPosY = screenY;
				return true;
			}

			if (mActiveRotate) {
				mMapPosition.rotateMap((screenX - mPosX) / 500f, 0, 0);
				mPosX = screenX;
				mMapPosition.tiltMap((screenY - mPosY) / 10f);
				mPosY = screenY;
				changed = true;
			}

			if (!(mActiveRotate || mActiveTilt)) {
				int dx = screenX - mPosX;
				int dy = screenY - mPosY;
				if (Math.abs(dx) > 0 || Math.abs(dy) > 0) {
					mMapPosition.moveMap(dx, dy);
					mPosX = screenX;
					mPosY = screenY;
					changed = true;
				}
			}

			if (changed)
				updateMap(true);

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
			float fx = mPosX - mMapView.getWidth() / 2;
			float fy = mPosY - mMapView.getHeight() / 2;

			if (amount > 0)
				mMapPosition.scaleMap(0.9f, fx, fy);
			else
				mMapPosition.scaleMap(1.1f, fx, fy);

			updateMap(false);

			return true;
		}

	}
}
