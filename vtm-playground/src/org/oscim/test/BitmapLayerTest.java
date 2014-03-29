package org.oscim.test;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.bitmap.DefaultSources.HillShadeHD;
import org.oscim.tiling.source.bitmap.DefaultSources.OpenStreetMap;
import org.oscim.tiling.source.bitmap.DefaultSources.StamenToner;
import org.oscim.tiling.source.bitmap.DefaultSources.StamenWatercolor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Input;

public class BitmapLayerTest extends GdxMap {

	final Logger log = LoggerFactory.getLogger(BitmapTileLayer.class);

	BitmapTileLayer mLayer = null;

	@Override
	protected boolean onKeyDown(int keycode) {
		if (keycode == Input.Keys.NUM_1) {
			mMap.layers().remove(mLayer);
			mLayer = new BitmapTileLayer(mMap, new OpenStreetMap());
			mMap.layers().set(2, mLayer);
			return true;
		} else if (keycode == Input.Keys.NUM_2) {
			mMap.layers().remove(mLayer);
			mLayer = new BitmapTileLayer(mMap, new StamenWatercolor());
			mMap.layers().set(2, mLayer);
			return true;
		} else if (keycode == Input.Keys.NUM_3) {
			mMap.layers().remove(mLayer);
			mLayer = new BitmapTileLayer(mMap, new HillShadeHD());
			mMap.layers().set(2, mLayer);
			return true;
		}

		return false;
	}

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff888888);

		mLayer = new BitmapTileLayer(mMap, new StamenToner());
		mMap.layers().add(mLayer);

	}

	public static void main(String[] args) {

		GdxMapApp.init();
		GdxMapApp.run(new BitmapLayerTest(), null, 256);
	}
}
