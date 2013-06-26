package org.oscim.gdx.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;

public class GwtLauncher extends GwtApplication {


	@Override
	public GwtApplicationConfiguration getConfig () {
		GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(1400, 800);
		cfg.stencil = true;
		cfg.fps = 20;

		return cfg;
	}

	@Override
	public ApplicationListener getApplicationListener () {

		return new GwtGdxMap();
	}
}