package org.oscim.test;

import java.io.FileNotFoundException;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import com.badlogic.gdx.Input;

public class ExternalRenderThemeTest extends GdxMap {

	VectorTileLayer mapLayer;

	@Override
	protected boolean onKeyDown(int keycode) {
		String name = null;
		if (keycode == Input.Keys.NUM_1)
			name = "themes/freizeitkarte/theme.xml";
		if (keycode == Input.Keys.NUM_2)
			name = "themes/elevate/theme.xml";
		if (keycode == Input.Keys.NUM_3)
			name = "themes/vmap/theme.xml";

		if (name == null)
			return false;

		try {
			IRenderTheme theme = ThemeLoader.load(name);
			mapLayer.setRenderTheme(theme);
			MapRenderer.setBackgroundColor(theme.getMapBackground());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		mMap.clearMap();
		mMap.updateMap(true);
		return true;
	}

	@Override
	public void createLayers() {
		mMap.setMapPosition(53.08, 8.83, 1 << 14);

		// TileSource tileSource = new OSciMap4TileSource();

		MapFileTileSource tileSource = new MapFileTileSource();
		// tileSource.setMapFile("/home/jeff/src/vtm/Freizeitkarte_DEU_NW.map");
		tileSource.setMapFile("/home/jeff/germany.map");

		VectorTileLayer l = mMap.setBaseMap(tileSource);
		mapLayer = l;

		// mMap.getLayers().add(new BuildingLayer(mMap, l.getTileLayer()));
		mMap.layers().add(new LabelLayer(mMap, l));

		try {
			IRenderTheme theme = ThemeLoader
			    .load("themes/freizeitkarte/theme.xml");
			// IRenderTheme theme =
			// ThemeLoader.load("themes/elevate/theme.xml");
			// IRenderTheme theme = ThemeLoader.load("themes/vmap/theme.xml");
			l.setRenderTheme(theme);
			MapRenderer.setBackgroundColor(theme.getMapBackground());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		// mMap.getLayers().add(new GenericLayer(mMap, new MeshRenderer()));
		// mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer()));
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new ExternalRenderThemeTest(), null, 256);
	}
}
