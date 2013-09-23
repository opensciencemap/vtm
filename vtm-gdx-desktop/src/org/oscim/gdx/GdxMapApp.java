package org.oscim.gdx;

import java.nio.Buffer;

import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglGL20;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class GdxMapApp extends GdxMap {

	// wrap LwjglGL20 to add GL20 interface
	static class GdxGL extends LwjglGL20 implements org.oscim.backend.GL20 {
		@Override
		public void glGetShaderSource(int shader, int bufsize, Buffer length, String source) {
			throw new IllegalArgumentException("not implemented");
		}
	}

	static {
		// load native library
		new SharedLibraryLoader().load("vtm-jni");
		// init globals
		CanvasAdapter.g = AwtGraphics.get();
		GLAdapter.g = new GdxGL();

		GLAdapter.GDX_DESKTOP_QUIRKS = true;
	}

	public static void main(String[] args) {
		Tile.SIZE = 360;
		new LwjglApplication(new GdxMapApp(), getConfig());
	}

	static protected LwjglApplicationConfiguration getConfig() {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "vtm-gdx";
		cfg.useGL20 = true;
		cfg.width = 1280;
		cfg.height = 800;
		cfg.stencil = 8;
		cfg.foregroundFPS = 20;
		return cfg;
	}

	@Override
	public void createLayers() {
		TileSource tileSource = new OSciMap4TileSource();
		tileSource.setOption("url", "http://opensciencemap.org/tiles/vtm");

		// TileSource tileSource = new MapFileTileSource();
		// tileSource.setOption("file", "/home/jeff/germany.map");

		initDefaultLayers(tileSource, false, true, true);

		MapPosition p = new MapPosition();
		p.setZoomLevel(14);
		p.setPosition(53.08, 8.83);
		mMap.setMapPosition(p);
	}
}
