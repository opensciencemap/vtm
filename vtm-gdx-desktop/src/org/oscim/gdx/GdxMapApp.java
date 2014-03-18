/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.gdx;

import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.oscim.utils.FastMath;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class GdxMapApp extends GdxMap {
	static {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
	}

	// wrap LwjglGL20 to add GL20 interface
	//	static class GdxGL extends GdxGL20 {
	//		@Override
	//		public void glGetShaderSource(int shader, int bufsize, Buffer length, String source) {
	//			throw new IllegalArgumentException("not implemented");
	//		}
	//	}

	public static void init() {
		// load native library
		new SharedLibraryLoader().load("vtm-jni");
		// init globals
		CanvasAdapter.g = AwtGraphics.get();
		GLAdapter.g = new GdxGL20();

		GLAdapter.GDX_DESKTOP_QUIRKS = true;
	}

	public static void main(String[] args) {
		Tile.SIZE = 360;
		init();
		new LwjglApplication(new GdxMapApp(), getConfig());
	}

	public static void run(GdxMap map, LwjglApplicationConfiguration config, int tileSize) {
		Tile.SIZE = FastMath.clamp(tileSize, 128, 512);

		new LwjglApplication(map, (config == null ? getConfig() : config));
	}

	public static void run(LwjglApplicationConfiguration config, int tileSize, GdxMap map) {
		run(map, config, tileSize);
	}

	static protected LwjglApplicationConfiguration getConfig() {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "vtm-gdx";
		//cfg.useGL20 = true;
		cfg.width = 1280;
		cfg.height = 800;
		cfg.stencil = 8;
		cfg.samples = 2;
		cfg.foregroundFPS = 30;
		return cfg;
	}

	@Override
	public void createLayers() {
		TileSource tileSource = new OSciMap4TileSource();

		// TileSource tileSource = new MapFileTileSource();
		// tileSource.setOption("file", "/home/jeff/germany.map");

		initDefaultLayers(tileSource, false, true, true);

		//mMap.getLayers().add(new BitmapTileLayer(mMap, new ImagicoLandcover(), 20));
		//mMap.getLayers().add(new BitmapTileLayer(mMap, new OSMTileSource(), 20));
		//mMap.getLayers().add(new BitmapTileLayer(mMap, new ArcGISWorldShaded(), 20));

		mMap.setMapPosition(0, 0, 1 << 2);
	}
}
