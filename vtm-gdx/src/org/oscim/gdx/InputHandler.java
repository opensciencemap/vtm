package org.oscim.gdx;

import org.oscim.layers.GenericLayer;
import org.oscim.layers.TileGridLayer;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.theme.VtmThemes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputProcessor;

public class InputHandler implements InputProcessor {

	private ViewController mViewport;
	private final Map mMap;
	private GenericLayer mGridLayer;
	private final GdxMap mGdxApp;

	public InputHandler(GdxMap map) {
		mMap = map.getMap();
		mViewport = mMap.viewport();
		mGdxApp = map;
	}

	private boolean mActiveScale;
	//private boolean mActiveTilt;
	private boolean mActiveRotate;

	private int mPosX, mPosY;

	@Override
	public boolean keyDown(int keycode) {
		if (mGdxApp.onKeyDown(keycode))
			return true;

		switch (keycode) {
			case Input.Keys.ESCAPE:
				Gdx.app.exit();
				break;

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
				mMap.setTheme(VtmThemes.DEFAULT);
				mMap.updateMap(false);
				break;

			case Input.Keys.T:
				mMap.setTheme(VtmThemes.TRONRENDER);
				mMap.updateMap(false);
				break;

			case Input.Keys.R:
				mMap.setTheme(VtmThemes.OSMARENDER);
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
