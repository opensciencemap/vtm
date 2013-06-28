package org.oscim.gdx;

import org.oscim.backend.Log.Logger;

import com.badlogic.gdx.Gdx;

public class GdxLog implements Logger {

	@Override
	public void d(String tag, String msg) {
		Gdx.app.debug(tag, msg);
	}

	@Override
	public void w(String tag, String msg) {
		Gdx.app.log(tag, msg);
	}

	@Override
	public void e(String tag, String msg) {
		Gdx.app.error(tag, msg);
	}

	@Override
	public void i(String tag, String msg) {
		Gdx.app.log(tag, msg);
	}
}
