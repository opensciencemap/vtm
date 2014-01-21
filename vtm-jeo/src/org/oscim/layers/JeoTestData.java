package org.oscim.layers;

import java.io.File;
import java.io.IOException;

import org.jeo.carto.Carto;
import org.jeo.data.Dataset;
import org.jeo.data.Query;
import org.jeo.data.mem.MemVector;
import org.jeo.data.mem.MemWorkspace;
import org.jeo.feature.Feature;
import org.jeo.feature.Features;
import org.jeo.feature.Schema;
import org.jeo.feature.SchemaBuilder;
import org.jeo.geojson.GeoJSONDataset;
import org.jeo.geom.GeomBuilder;
import org.jeo.map.Style;

import com.vividsolutions.jts.geom.Geometry;

public class JeoTestData {

	public static Style getStyle() {
		Style style = null;

		try {
			style = Carto.parse("" +
			        "#things {" +
			        "  line-color: #c80;" +
			        "  polygon-fill: #00a;" +
			        "}" +
			        "#states {" +
			        "  polygon-fill: #0dc;" +
			        "}"
			    );

			return style;
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
