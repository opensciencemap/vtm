package org.oscim.gdx.client;
// -draftCompile -localWorkers 2
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;

public class GwtLauncher extends GwtApplication {


	@Override
	public GwtApplicationConfiguration getConfig () {
		GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(1400, 800);
		cfg.stencil = true;
		cfg.fps = 25;

		return cfg;
	}

	@Override
	public ApplicationListener getApplicationListener () {

		return new GwtGdxMap();
	}
}