package org.oscim.test;

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.RenderTheme;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThemeBuilderTest extends GdxMap {

	final Logger log = LoggerFactory.getLogger(MeshTest.class);

	static class MyTheme extends ThemeBuilder {
		public MyTheme() {
			rules(
			      matchKeyValue("natural", "water")
			          .style(area(Color.BLUE)),

			      matchKeyValue("landuse", "forest")
			          .style(area(Color.GREEN)),

			      matchKeyValue("landuse", "residential")
			          .style(area(Color.LTGRAY)),

			      matchKey("highway")
			          .rules(matchValue("residential")
			              .style(line(Color.DKGRAY, 1.2f),
			                     line(Color.WHITE, 1.1f)
			                         .cap(Cap.ROUND)))

			          .style(line(Color.BLACK, 1)
			              .blur(0.5f)));
		}
	}

	@Override
	public void createLayers() {

		VectorTileLayer l = mMap.setBaseMap(new OSciMap4TileSource());

		RenderTheme t = new MyTheme().build();

		mMap.setTheme(t);
		//mMap.setTheme(VtmThemes.DEFAULT);

		mMap.layers().add(new LabelLayer(mMap, l));

		mMap.setMapPosition(53.08, 8.82, 1 << 17);

	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new ThemeBuilderTest(), null, 400);
	}
}
