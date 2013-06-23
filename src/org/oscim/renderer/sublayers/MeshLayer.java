/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.renderer.sublayers;

import java.nio.ShortBuffer;
import java.util.Arrays;

import org.oscim.backend.Log;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.Tile;


public class MeshLayer extends Layer {
	GeometryBuffer mGeom = new GeometryBuffer(10,10);

	public MeshLayer() {
		GeometryBuffer e = mGeom;

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

	public void addMesh(GeometryBuffer geom){
		int numRings = 2;

		long ctx = tessellate(geom.points, 0, geom.index, 0, numRings);

		short[] coordinates = new short[100];

		while (tessGetCoordinates(ctx, coordinates, 2) > 0){
			Log.d("..", Arrays.toString(coordinates));
		}

		while (tessGetIndices(ctx, coordinates) > 0){
			Log.d("..", Arrays.toString(coordinates));
		}

		tessFinish(ctx);
	}

	@Override
	protected void compile(ShortBuffer sbuf) {

	}

	@Override
	protected void clear() {

	}

	/**
	 * @param points an array of x,y coordinates
	 * @param pos position in points array
	 * @param index geom indices
	 * @param ipos position in index array
	 * @param numRings number of rings in polygon == outer(1) + inner rings
	 * @return number of triangles in io buffer
	 */
	public static native int tessellate(float[] points, int pos,
			short[] index, int ipos, int numRings);

	public static native void tessFinish(long ctx);

	public static native int tessGetCoordinates(long ctx, short[] coordinates, float scale);

	public static native int tessGetIndices(long ctx, short[] indices);


	static {
		System.loadLibrary("tessellate");
	}
}
