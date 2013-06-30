package org.oscim.gdx.client;

// -draftCompile -localWorkers 2
import org.oscim.core.Tile;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtGraphics;

public class GwtLauncher extends GwtApplication {

	@Override
	public GwtApplicationConfiguration getConfig() {
		GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(GwtGraphics.getWindowWidthJSNI(),
				GwtGraphics.getWindowHeightJSNI() );
		cfg.stencil = true;
		cfg.fps = 25;

		return cfg;
	}

	@Override
	public ApplicationListener getApplicationListener() {
		if (GwtGraphics.getDevicePixelRatioJSNI() > 1)
			Tile.SIZE = 400;
		else
			Tile.SIZE = 360;

		return new GwtGdxMap();
	}
}
