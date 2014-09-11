package org.oscim.test;

import static org.oscim.tiling.source.bitmap.DefaultSources.HD_HILLSHADE;
import static org.oscim.tiling.source.bitmap.DefaultSources.OPENSTREETMAP;
import static org.oscim.tiling.source.bitmap.DefaultSources.STAMEN_TONER;
import static org.oscim.tiling.source.bitmap.DefaultSources.STAMEN_WATERCOLOR;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Input;

public class BitmapLayerTest extends GdxMap {

	final Logger log = LoggerFactory.getLogger(BitmapTileLayer.class);

	BitmapTileLayer mLayer = null;
	BitmapTileLayer mShaded = null;

	@Override
	protected boolean onKeyDown(int keycode) {
		if (keycode == Input.Keys.NUM_1) {
			mMap.layers().remove(mLayer);
			mLayer = new BitmapTileLayer(mMap, OPENSTREETMAP.build());
			mMap.layers().add(mLayer);
			return true;
		} else if (keycode == Input.Keys.NUM_2) {
			mMap.layers().remove(mLayer);
			mLayer = new BitmapTileLayer(mMap, STAMEN_WATERCOLOR.build());
			mMap.layers().add(mLayer);
			return true;
		} else if (keycode == Input.Keys.NUM_3) {
			if (mShaded != null) {
				mMap.layers().remove(mShaded);
				mShaded = null;
			} else {
				mShaded = new BitmapTileLayer(mMap, HD_HILLSHADE.build());
				mMap.layers().add(mShaded);
			}
			return true;
		}

		return false;
	}

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff888888);

		mLayer = new BitmapTileLayer(mMap, STAMEN_TONER.build());
		mMap.layers().add(mLayer);

	}

	public static void main(String[] args) {

		GdxMapApp.init();
		GdxMapApp.run(new BitmapLayerTest(), null, 256);
	}
}
