package org.oscim.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jeo.carto.Carto;
import org.jeo.data.Dataset;
import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.data.mem.MemVector;
import org.jeo.data.mem.MemWorkspace;
import org.jeo.feature.Feature;
import org.jeo.feature.Features;
import org.jeo.feature.Schema;
import org.jeo.feature.SchemaBuilder;
import org.jeo.geojson.GeoJSONDataset;
import org.jeo.geojson.GeoJSONReader;
import org.jeo.geom.GeomBuilder;
import org.jeo.map.Style;
import org.oscim.layers.OSMIndoorLayer;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Map;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import com.vividsolutions.jts.geom.Geometry;

public class JeoTest {

	public static void indoorSketch(Map map, String file) {
		MapRenderer.setBackgroundColor(0xff909090);
		VectorTileLayer baseLayer = map.setBaseMap(new OSciMap4TileSource());
		map.layers().add(new BuildingLayer(map, baseLayer));

		VectorDataset data = null;
		try {
			data = JeoTest.readGeoJson(new FileInputStream(new File(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Style style = JeoTest.getStyle();
		map.layers().add(new OSMIndoorLayer(map, data, style));
	}

	public static Style getStyle() {
		Style style = null;

		try {
			style = Carto.parse("" +
			        "#way {" +
			        "  line-width: 2;" +
			        "  line-color: #c80;" +
			        "  polygon-fill: #44111111;" +
			        "  " +
			        "}" +
			        "#states {" +
			        "  line-width: 2.2;" +
			        "  line-color: #c80;" +
			        "  polygon-fill: #44111111;" +
			        "  " +
			        "}"
			    );

			return style;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static VectorDataset readGeoJson(InputStream is) {
		GeoJSONReader r = new GeoJSONReader();

		@SuppressWarnings("resource")
		MemWorkspace mem = new MemWorkspace();

		//mem.put("layer", data);
		try {
			Schema s = new SchemaBuilder("way").schema();

			MemVector memData = mem.create(s);

			for (Feature f : r.features(is)) {
				//System.out.println("loaded: " + f);
				memData.add(f);
			}
			return memData;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Dataset getJsonData(String file, boolean memory) {
		GeoJSONDataset data = null;

		try {
			data = new GeoJSONDataset(new File(file));
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (memory) {
			@SuppressWarnings("resource")
			MemWorkspace mem = new MemWorkspace();

			//mem.put("layer", data);
			try {

				Schema s = data.schema();
				Query q = new Query();

				MemVector memData = mem.create(s);

				for (Feature f : data.cursor(q)) {
					memData.add(f);
				}

				//return mem.get("layer");
				return memData;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	public static Dataset getMemWorkspace(String layer) {
		GeomBuilder gb = new GeomBuilder(4326);

		@SuppressWarnings("resource")
		MemWorkspace mem = new MemWorkspace();
		Schema schema = new SchemaBuilder(layer)
		    .field("geometry", Geometry.class)
		    .field("id", Integer.class)
		    .field("name", String.class)
		    .field("cost", Double.class).schema();

		MemVector data;
		try {
			data = mem.create(schema);
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		Geometry g = gb.point(0, 0).toPoint();
		//g.setSRID(4326);

		data.add(Features.create(null, data.schema(),
		                         g, 1, "anvil",
		                         10.99));

		data.add(Features.create(null, data.schema(),
		                         gb.points(10, 10, 20, 20).toLineString(),
		                         2, "bomb", 11.99));

		data.add(Features.create(null, data.schema(),
		                         gb.point(100, 10).toPoint().buffer(10),
		                         3, "dynamite", 12.99));

		//Dataset jsonData = new GeoJSONDataset(new File("states.json"));
		//mem.put("states", jsonData);

		try {
			return mem.get(layer);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
