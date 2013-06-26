package org.oscim.gdx;

import org.oscim.backend.Log.Logger;

public class GdxLog implements Logger {

	@Override
	public void d(String tag, String msg) {
		//Gdx.app.log(tag, msg);
		System.err.println(msg);
	}

	@Override
	public void w(String tag, String msg) {
		//Gdx.app.log(tag, msg);
		System.err.println(msg);

	}

	@Override
	public void e(String tag, String msg) {
		//Gdx.app.log(tag, msg);
		System.err.println(msg);

	}

	@Override
	public void i(String tag, String msg) {
		//Gdx.app.log(tag, msg);
		System.err.println(msg);

	}

}
