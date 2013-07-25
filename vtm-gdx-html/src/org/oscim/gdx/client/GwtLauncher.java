package org.oscim.gdx.client;

// -draftCompile -localWorkers 2
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.gdx.GdxMap;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap2.OSciMap2TileSource;
import org.oscim.tilesource.oscimap4.OSciMap4TileSource;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
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

		TileSource tileSource;
		if ("oscimap4".equals(getMapConfig("tilesource")))
			tileSource = new OSciMap4TileSource();
		else
			tileSource = new OSciMap2TileSource();

		tileSource.setOption("url", url);
		return new GwtGdxMap(tileSource);
	}

	private static native String getMapConfig(String key)/*-{
		return $wnd.mapconfig && $wnd.mapconfig[key] || null;
	}-*/;

	class GwtGdxMap extends GdxMap {

		public GwtGdxMap(TileSource tileSource) {
			super(tileSource);
		}

		@Override
		public void create() {
			CanvasAdapter.g = GwtCanvasAdapter.INSTANCE;
			GLAdapter.g = (GL20)Gdx.graphics.getGL20();
			GLAdapter.GDX_WEBGL_QUIRKS = true;

			//GLAdapter.NON_PREMUL_CANVAS = true;
			//Gdx.app.setLogLevel(Application.LOG_DEBUG);

			super.create();

			double lat =  Double.parseDouble(getMapConfig("latitude"));
			double lon =  Double.parseDouble(getMapConfig("longitude"));
			int zoom =  Integer.parseInt(getMapConfig("zoom"));

			MapPosition p = new MapPosition();
			p.setZoomLevel(zoom);
			p.setPosition(lat, lon);
			mMapView.setMapPosition(p);
		}
	}
}
