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
import org.oscim.backend.GLAdapter;
import org.oscim.core.Tile;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.backends.jglfw.JglfwApplication;
import com.badlogic.gdx.backends.jglfw.JglfwApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class GdxMapApp extends GdxMap {

	public static final Logger log = LoggerFactory.getLogger(GdxMapApp.class);

	public static void init() {
		// load native library
		new SharedLibraryLoader().load("vtm-jni");
		// init globals
		AwtGraphics.init();
		GdxAssets.init("assets/");
		GLAdapter.init(new GdxGL());
		GLAdapter.GDX_DESKTOP_QUIRKS = true;
	}

	public static void main(String[] args) {
		Tile.SIZE = 360;
		init();
		new JglfwApplication(new GdxMapApp(), getConfig());
	}

	public static void run(GdxMap map, JglfwApplicationConfiguration config, int tileSize) {
		Tile.SIZE = FastMath.clamp(tileSize, 128, 512);

		new JglfwApplication(map, (config == null ? getConfig() : config));
	}

	public static void run(JglfwApplicationConfiguration config, int tileSize, GdxMap map) {
		run(map, config, tileSize);
	}

	static protected JglfwApplicationConfiguration getConfig() {
		JglfwApplicationConfiguration cfg = new JglfwApplicationConfiguration();
		cfg.title = "vtm-gdx";
		cfg.width = 1280;
		cfg.height = 800;
		cfg.stencil = 8;
		//cfg.samples = 2;
		cfg.foregroundFPS = 60;
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
