package org.oscim.gdx;

import org.oscim.backend.input.MotionEvent;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

public class GdxMotionEvent extends MotionEvent implements InputProcessor{

	@Override
	public int getAction() {
		return 0;
	}

	@Override
	public float getX() {
		return 0;
	}

	@Override
	public float getY() {
		return 0;
	}

	@Override
	public float getX(int idx) {
		return 0;
	}

	@Override
	public float getY(int idx) {
		return 0;
	}

	@Override
	public int getPointerCount() {
		return 0;
	}

	@Override
	public long getTime() {
		return 0;
	}

	// -------- InputProcessor ----------
	@Override
	public boolean keyDown(int keycode) {
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
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}

}
