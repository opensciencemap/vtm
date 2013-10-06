package org.oscim.utils;

import org.oscim.core.GeometryBuffer;

/**
 * Clip polygon to a rectangle. Output cannot expected to be valid
 * Simple-Feature geometry, i.e. all rings are clipped independently
 * so that inner and outer rings might touch, etc.
 * 
 * based on http://www.cs.rit.edu/~icss571/clipTrans/PolyClipBack.html
 * */
public class TileClipper {
	private float minX;
	private float maxX;
	private float minY;
	private float maxY;

	public TileClipper(float minX, float minY, float maxX, float maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public void setRect(float minX, float minY, float maxX, float maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	private final GeometryBuffer mGeomOut = new GeometryBuffer(10, 1);

	public boolean clip(GeometryBuffer geom) {
		GeometryBuffer out = mGeomOut;

		out.clear();

		clipEdge(geom, out, LineClipper.LEFT);
		geom.clear();

		clipEdge(out, geom, LineClipper.TOP);
		out.clear();

		clipEdge(geom, out, LineClipper.RIGHT);
		geom.clear();

		clipEdge(out, geom, LineClipper.BOTTOM);

		if ((geom.indexPos == 0) && (geom.index[0] < 6))
			return false;

		return true;
	}

	private boolean clipEdge(GeometryBuffer in, GeometryBuffer out, int edge) {

		out.startPolygon();
		boolean outer = true;

		int pointPos = 0;

		for (int i = 0, n = in.index.length; i < n; i++) {
			int len = in.index[i];
			if (len < 0)
				break;

			if (len < 6) {
				pointPos += len;
				continue;
			}

			if (len == 0) {
				out.startPolygon();
				outer = true;
				continue;
			}

			if (!outer)
				out.startHole();

			clipRing(i, pointPos, in, out, edge);

			//			if (out.index[i] < 6) {
			//				out.index[i] = 0;
			//				//if (out.indexPos > 0)
			//				//	out.indexPos--;
			//				// TODO if outer skip holes 
			//			}

			pointPos += len;

			outer = false;
		}

		return true;
	}

	private boolean clipRing(int indexPos, int pointPos, GeometryBuffer in, GeometryBuffer out,
	        int edge) {

		int len = in.index[indexPos];
		if (len < 6)
			return false;

		len += pointPos;

		float px = in.points[len - 2];
		float py = in.points[len - 1];

		for (int i = pointPos; i < len;) {
			float cx = in.points[i++];
			float cy = in.points[i++];

			switch (edge) {
				case LineClipper.LEFT:
					if (cx > minX) {
						// current is inside
						if (px > minX) {
							// previous was inside
							out.addPoint(cx, cy);
						} else {
							// previous was outside, add edge point
							out.addPoint(minX, py + (cy - py) * (minX - px) / (cx - px));
							out.addPoint(cx, cy);
						}
					} else {
						if (px > minX) {
							// previous was inside, add edge point
							out.addPoint(minX, py + (cy - py) * (minX - px) / (cx - px));
						}
					}
					break;

				case LineClipper.RIGHT:
					if (cx < maxX) {
						if (px < maxX) {
							out.addPoint(cx, cy);
						} else {
							out.addPoint(maxX, py + (cy - py) * (maxX - px) / (cx - px));
							out.addPoint(cx, cy);
						}
					} else {
						if (px < maxX) {
							out.addPoint(maxX, py + (cy - py) * (maxX - px) / (cx - px));
						}
					}
					break;

				case LineClipper.BOTTOM:
					if (cy > minY) {
						if (py > minY) {
							out.addPoint(cx, cy);
						} else {
							out.addPoint(px + (cx - px) * (minY - py) / (cy - py), minY);
							out.addPoint(cx, cy);
						}
					} else {
						if (py > minY) {
							out.addPoint(px + (cx - px) * (minY - py) / (cy - py), minY);
						}
					}
					break;

				case LineClipper.TOP:
					if (cy < maxY) {
						if (py < maxY) {
							out.addPoint(cx, cy);
						} else {
							out.addPoint(px + (cx - px) * (maxY - py) / (cy - py), maxY);
							out.addPoint(cx, cy);
						}
					} else {
						if (py < maxY) {
							out.addPoint(px + (cx - px) * (maxY - py) / (cy - py), maxY);
						}
					}
					break;
			}

			px = cx;
			py = cy;
		}
		return true;
	}

	//	public static void main(String[] args) {
	//		TileClipper clipper = new TileClipper(0, 0, 100, 100);
	//		GeometryBuffer geom = new GeometryBuffer(10, 1);
	//
	//		geom.startPolygon();
	//		geom.addPoint(-10, -10);
	//		geom.addPoint(110, -10);
	//		geom.addPoint(110, 110);
	//		geom.addPoint(-10, 110);
	//
	//		geom.startHole();
	//		geom.addPoint(10, 10);
	//		geom.addPoint(110, 10);
	//		geom.addPoint(110, 90);
	//		geom.addPoint(10, 90);
	//
	//		System.out.println("" + Arrays.toString(geom.points));
	//		clipper.clip(geom);
	//		System.out.println("" + Arrays.toString(geom.points) + " " + geom.index[0]);
	//
	//	}
}
