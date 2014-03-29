package org.oscim.test;

import org.oscim.core.BoundingBox;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import com.badlogic.gdx.Input;

public class AnimatorTest extends GdxMap {

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff000000);

		TileSource ts = new OSciMap4TileSource();
		initDefaultLayers(ts, false, false, false);

		mMap.setMapPosition(0, 0, 1 << 4);

	}

	@Override
	protected boolean onKeyDown(int keycode) {
		if (keycode == Input.Keys.NUM_1) {
			mMap.animator().animateTo(new BoundingBox(53.1, 8.8, 53.2, 8.9));
			return true;
		}
		return false;
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new AnimatorTest(), null, 256);
	}
}
