package org.oscim.gdx.client;

// -draftCompile -localWorkers 2
import org.oscim.core.Tile;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap4.OSciMap4TileSource;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.GwtGraphics;

public class GwtLauncher extends GwtApplication {

	@Override
	public GwtApplicationConfiguration getConfig() {
		GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(
				GwtGraphics.getWindowWidthJSNI(),
				GwtGraphics.getWindowHeightJSNI());
		cfg.stencil = true;
		cfg.fps = 30;

		return cfg;
	}

	@Override
	public ApplicationListener getApplicationListener() {
		if (GwtGraphics.getDevicePixelRatioJSNI() > 1)
			Tile.SIZE = 400;
		else
			Tile.SIZE = 360;

		String url = getMapConfig("tileurl");

		TileSource tileSource = new OSciMap4TileSource();
		tileSource.setOption("url", url);
		return new GwtGdxMap(tileSource);
	}

	private static native String getMapConfig(String key)/*-{
		return $wnd.mapconfig && $wnd.mapconfig[key] || null;
	}-*/;
}
