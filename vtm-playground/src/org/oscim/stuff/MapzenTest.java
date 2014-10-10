package org.oscim.stuff;

import java.io.FileNotFoundException;

import org.oscim.backend.CanvasAdapter;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Input;

public class MapzenTest extends GdxMap {

	final Logger log = LoggerFactory.getLogger(MapzenTest.class);

	@Override
	protected boolean onKeyDown(int keycode) {
		if (keycode == Input.Keys.A) {
			loadTheme();
		}
		if (keycode == Input.Keys.NUM_1) {
			mMap.setMapPosition(53, 8, 10);
		}
		if (keycode == Input.Keys.NUM_2) {
			mMap.setMapPosition(53.1, 8.8, 12);
		}

		return super.onKeyDown(keycode);
	}

	@Override
	public void createLayers() {

		UrlTileSource tileSource = OSciMap4TileSource.builder()
		    //.url("http://vector.dev.mapzen.com/osm/all")
		    .url("http://vector.mapzen.com/osm/all")
		    .zoomMax(18)
		    .httpFactory(new OkHttpEngine.OkHttpFactory())
		    .build();

		VectorTileLayer l = mMap.setBaseMap(tileSource);

		loadTheme();

		mMap.layers().add(new BuildingLayer(mMap, l));
		mMap.layers().add(new LabelLayer(mMap, l));

		mMap.setMapPosition(53.08, 8.82, 1 << 17);
	}

	private void loadTheme() {
		try {
			mMap.setTheme(ThemeLoader.load("assets/styles/mapzen.xml"));
		} catch (ThemeException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		CanvasAdapter.dpi = 200;
		GdxMapApp.run(new MapzenTest(), null, 512);
	}
}
