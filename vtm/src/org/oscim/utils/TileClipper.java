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
		mLineClipper = new LineClipper((int) minX, (int) minY, (int) maxX, (int) maxY, true);
	}

	public void setRect(float minX, float minY, float maxX, float maxY) {
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		mLineClipper = new LineClipper((int) minX, (int) minY, (int) maxX, (int) maxY, true);
	}

	private LineClipper mLineClipper;

	private final GeometryBuffer mGeomOut = new GeometryBuffer(10, 1);

	public boolean clip(GeometryBuffer geom) {
		if (geom.isPoly()) {

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
		}

		else if (geom.isLine()) {

			GeometryBuffer out = mGeomOut;
			out.clear();

			int numLines = clipLine(geom, out);

			short idx[] = geom.ensureIndexSize(numLines + 1, false);
			System.arraycopy(out.index, 0, idx, 0, numLines);
			geom.index[numLines] = -1;

			float pts[] = geom.ensurePointSize(out.pointPos >> 1, false);
			System.arraycopy(out.points, 0, pts, 0, out.pointPos);
			geom.indexPos = out.indexPos;
			geom.pointPos = out.pointPos;

			if ((geom.indexPos == 0) && (geom.index[0] < 4))
				return false;
		}
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

			//if (out.index[i] < 6) {
			//	out.index[i] = 0;
			//	//if (out.indexPos > 0)
			//	//	out.indexPos--;
			//	// TODO if outer skip holes
			//}

			pointPos += len;

			outer = false;
		}

		return true;
	}

	private int clipLine(GeometryBuffer in, GeometryBuffer out) {

		int pointPos = 0;
		int numLines = 0;
		for (int i = 0, n = in.index.length; i < n; i++) {
			int len = in.index[i];
			if (len < 0)
				break;

			if (len < 4) {
				pointPos += len;
				continue;
			}

			if (len == 0) {
				continue;
			}

			int inPos = pointPos;
			int end = inPos + len;

			float prevX = in.points[inPos + 0];
			float prevY = in.points[inPos + 1];

			boolean inside = mLineClipper.clipStart(prevX, prevY);

			if (inside) {
				out.startLine();
				out.addPoint(prevX, prevY);
				numLines++;
			}

			for (inPos += 2; inPos < end; inPos += 2) {
				// get the current way point coordinates
				float curX = in.points[inPos];
				float curY = in.points[inPos + 1];

				int clip;
				if ((clip = mLineClipper.clipNext(curX, curY)) != 0) {
					//System.out.println(inside + " clip: " + clip + " "
					//        + Arrays.toString(mLineClipper.out));
					if (clip < 0) {
						if (inside) {
							// previous was inside
							out.addPoint(mLineClipper.out[2], mLineClipper.out[3]);
							inside = false;

						} else {
							// previous was outside
							out.startLine();
							numLines++;
							out.addPoint(mLineClipper.out[0], mLineClipper.out[1]);
							out.addPoint(mLineClipper.out[2], mLineClipper.out[3]);

							inside = mLineClipper.clipStart(curX, curY);
						}
					} else {
						out.addPoint(curX, curY);

					}
				} else {
					inside = false;
				}

			}

			pointPos += len;
		}

		return numLines;
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
}
