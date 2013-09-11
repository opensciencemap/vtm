package org.oscim.gdx;

import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class Main {

	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "vtm-gdx";
		cfg.useGL20 = true;
		cfg.width = 1280;
		cfg.height = 800;
		cfg.stencil = 8;
		cfg.foregroundFPS = 20;
		// cfg.samples = 4;

		// set our globals
		CanvasAdapter.g = AwtGraphics.INSTANCE;
		GLAdapter.g = new GdxGLAdapter();
		GLAdapter.GDX_DESKTOP_QUIRKS = true;

		Tile.SIZE = 360;

		new SharedLibraryLoader().load("vtm-jni");

		new LwjglApplication(new GdxMapDesktop(), cfg);
	}

	static class GdxMapDesktop extends GdxMap {

		public GdxMapDesktop() {
			super();
		}

		@Override
		public void create() {
			super.create();

			TileSource tileSource = new OSciMap4TileSource();
			tileSource.setOption("url", "http://city.informatik.uni-bremen.de/tiles/vtm");

			initDefaultMap(tileSource, false, true, true);
		}
	}
}
