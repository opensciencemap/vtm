package org.oscim.test.jeo;

import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.carto.RenderTheme;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class ThemeTest extends GdxMapApp {

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new ThemeTest(), null, 256);
	}

	@Override
	public void createLayers() {
		UrlTileSource ts = new OSciMap4TileSource();

		VectorTileLayer l = mMap.setBaseMap(ts);

		l.setRenderTheme(new RenderTheme());

		MapRenderer.setBackgroundColor(0xffcccccc);

		// mMap.getLayers().add(new LabelLayer(mMap,
		// mMapLayer.getTileLayer()));
		// mMap.getLayers().add(new JeoMapLayer(mMap));

		mMap.layers().add(new TileGridLayer(mMap));
	}
}
