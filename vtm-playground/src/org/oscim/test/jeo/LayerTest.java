package org.oscim.test.jeo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jeo.data.VectorDataset;
import org.jeo.map.Style;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.JeoVectorLayer;
import org.oscim.layers.OSMIndoorLayer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.test.JeoTest;
import org.oscim.tiling.source.bitmap.DefaultSources.StamenToner;

public class LayerTest extends GdxMap {

	String PATH = "https://gist.github.com/anonymous/8960337/raw/overpass.geojson";

	OSMIndoorLayer mIndoorLayer;

	@Override
	public void createLayers() {
		mMap.setBackgroundMap(new BitmapTileLayer(mMap, new StamenToner()));
		mMap.layers().add(new TileGridLayer(mMap));

		mMap.addTask(new Runnable() {
			@Override
			public void run() {
				try {
					URL url = new URL(PATH);
					URLConnection conn = url.openConnection();
					InputStream is = conn.getInputStream();

					VectorDataset data = JeoTest.readGeoJson(is);
					Style style = JeoTest.getStyle();
					mIndoorLayer = new OSMIndoorLayer(mMap, data, style);
					mIndoorLayer.activeLevels[0] = true;
					mIndoorLayer.activeLevels[1] = true;
					mIndoorLayer.activeLevels[2] = true;
					mIndoorLayer.activeLevels[3] = true;

					mMap.layers().add(new JeoVectorLayer(mMap, data, style));
					mMap.layers().add(mIndoorLayer);

					mMap.updateMap(true);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		mMap.setMapPosition(53.5620092, 9.9866457, 1 << 16);

		//VectorDataset data = (VectorDataset) JeoTest.getJsonData("states.json", true);
		//Style style = JeoTest.getStyle();
		//mMap.layers().add(new JeoVectorLayer(mMap, data, style));

	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new LayerTest(), null, 256);
	}
}
