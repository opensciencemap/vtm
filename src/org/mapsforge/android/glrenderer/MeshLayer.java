/*
 * Copyright 2010, 2011, 2012 Hannes Janetzek
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
package org.mapsforge.android.glrenderer;

import java.util.LinkedList;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

public class MeshLayer extends Layer {

	MeshLayer(int l, int color) {
		super(l, color);

		curItem = LayerPool.get();
		pool = new LinkedList<PoolItem>();
		pool.add(curItem);
	}

	void addPolygon(float[] points, int position, int length) {
		float[] curVertices = curItem.vertices;
		int outPos = curItem.used;

		int len = length / 2 - 1;
		int pos = position;

		PolygonPoint[] pp = new PolygonPoint[len];

		for (int i = 0; i < len; i++) {
			pp[i] = new PolygonPoint(points[pos++], points[pos++]);
		}

		Polygon poly = new Polygon(pp);

		Poly2Tri.triangulate(poly);

		for (DelaunayTriangle tri : poly.getTriangles()) {

			for (int i = 0; i < 3; i++) {

				if (outPos == PoolItem.SIZE) {
					curVertices = getNextPoolItem();
					outPos = 0;
				}

				curVertices[outPos++] = (float) tri.points[i].getX();
				curVertices[outPos++] = (float) tri.points[i].getY();

			}
			// System.out.println("" +
			// (float) tri.points[0].getX() + "/" + (float) tri.points[0].getY()
			// + ", " +
			// (float) tri.points[1].getX() + "/" + (float) tri.points[1].getY()
			// + ", " +
			// (float) tri.points[2].getX() + "/" + (float) tri.points[2].getY());
		}
		// System.out.println("---");
		curItem.used = outPos;
		verticesCnt += poly.getTriangles().size() * 3;
	}
}
