package org.oscim.gdx;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGL20;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class MainActivity extends AndroidApplication {

	private final class AndroidGLAdapter extends AndroidGL20 implements GL20 {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		CanvasAdapter.g = AndroidGraphics.INSTANCE;
		GLAdapter.g = new AndroidGLAdapter();

		// TODO make this dpi dependent
		Tile.SIZE = 400;

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useGL20 = true;

		new SharedLibraryLoader().load("vtm-jni");

		initialize(new GdxMapAndroid(), cfg);
	}

	class GdxMapAndroid extends GdxMap {
		@Override
		public void createLayers() {

			TileSource ts = new OSciMap4TileSource();
			ts.setOption("url", "http://opensciencemap.org/tiles/vtm");

			initDefaultLayers(ts, true, true, true);
		}
	}
}
