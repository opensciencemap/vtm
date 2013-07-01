package org.oscim.gdx;

import org.oscim.android.AndroidLog;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.core.Tile;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap4.OSciMap4TileSource;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidGL20;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class MainActivity extends AndroidApplication {

	private final class AndroidGLAdapter extends AndroidGL20 implements GL20{
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set our globals
        CanvasAdapter.g = AndroidGraphics.INSTANCE;
        GLAdapter.g = new AndroidGLAdapter();
		Log.logger = new AndroidLog();
		// TODO make this dpi dependent
		Tile.SIZE = 400;

        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useGL20 = true;

        new SharedLibraryLoader().load("vtm-jni");

        TileSource tileSource = new OSciMap4TileSource();
		tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/testing");

        initialize(new GdxMap(tileSource), cfg);
    }
}