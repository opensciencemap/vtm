package org.oscim.test;

import static java.lang.System.out;

import java.util.Arrays;

import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tile;
import org.oscim.utils.Tessellator;

import com.badlogic.gdx.utils.SharedLibraryLoader;

public class TessellatorTest extends Tessellator{

	public static void main(String[] args) {
		new SharedLibraryLoader().load("vtm-jni");

		GeometryBuffer e = new GeometryBuffer(128, 3);

		int size = Tile.SIZE;

		float x1 = -1;
		float y1 = -1;
		float x2 = size + 1;
		float y2 = size + 1;

		// always clear geometry before starting
		// a different type.
		e.clear();
		e.startPolygon();
		e.addPoint(x1, y1);
		e.addPoint(x2, y1);
		e.addPoint(x2, y2);
		e.addPoint(x1, y2);

		y1 = 5;
		y2 = size - 5;
		x1 = 5;
		x2 = size - 5;

		e.startHole();
		e.addPoint(x1, y1);
		e.addPoint(x2, y1);
		e.addPoint(x2, y2);
		e.addPoint(x1, y2);

		addMesh(e);
	}

	static void addMesh(GeometryBuffer geom) {
		int numRings = 2;
		int[] result = new int[2];

		long ctx = Tessellator.tessellate(geom.points, 0, geom.index, 0, numRings, result);
		out.println("ok" + Arrays.toString(result));

		short[] coordinates = new short[100];

		while (Tessellator.tessGetVertices(ctx, coordinates, 2) > 0) {
			out.println(Arrays.toString(coordinates));
		}

		while (Tessellator.tessGetIndices(ctx, coordinates) > 0) {
			out.println(Arrays.toString(coordinates));
		}

		Tessellator.tessFinish(ctx);
	}

}
