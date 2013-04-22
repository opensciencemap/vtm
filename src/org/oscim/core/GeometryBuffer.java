package org.oscim.core;

public class GeometryBuffer {

	// TODO
	// -- check indexPos < Short.Max
	final static boolean CHECK_STATE = true;

	public float[] points;
	public short[] index;
	public int indexPos;
	public int pointPos;

	public int mode;

	public GeometryBuffer(float[] points, short[] index) {
		if (points == null)
			throw new IllegalArgumentException("GeometryBuffer points is null");
		if (index == null)
			throw new IllegalArgumentException("GeometryBuffer index is null");

		this.points = points;
		this.index = index;
		mode = 0;
		indexPos = 0;
		pointPos = 0;
	}

	public GeometryBuffer(int numPoints, int numIndices) {
		this.points = new float[numPoints * 2];
		this.index = new short[numIndices];
	}

	// ---- API ----
	public void clear() {
		index[0] = -1;
		indexPos = 0;
		pointPos = 0;
		mode = 0;
	}

	public void addPoint(float x, float y) {
		if (pointPos >= points.length - 1)
			ensurePointSize((pointPos >> 1) + 1, true);

		points[pointPos++] = x;
		points[pointPos++] = y;

		index[indexPos] += 2;
	}

	public void setPoint(int pos, float x, float y) {
		points[(pos << 1) + 0] = x;
		points[(pos << 1) + 1] = y;
	}

	public void startPoints() {
		if (CHECK_STATE)
			setOrCheckMode(1);
	}

	public void startLine() {
		if (CHECK_STATE)
			setOrCheckMode(2);

		// start next
		if ((index[0] >= 0) && (++indexPos >= index.length))
			ensureIndexSize(indexPos, true);

		// initialize with zero points
		index[indexPos] = 0;
		// set new end marker
		index[indexPos + 1] = -1;
	}

	public void startPolygon() {
		if (CHECK_STATE)
			setOrCheckMode(3);

		if ((indexPos + 4) >= index.length)
			ensureIndexSize(indexPos + 4, true);

		if (indexPos > 0) {

			// end ring
			index[++indexPos] = 0;

			// end polygon
			index[++indexPos] = 0;

			// next polygon start
			indexPos++;
		}

		// initialize with zero points
		index[indexPos] = 0;
		// set new end marker
		index[indexPos + 1] = -1;
	}

	public void startHole() {
		if (CHECK_STATE)
			checkMode(3);

		if ((indexPos + 3) >= index.length)
			ensureIndexSize(indexPos, true);

		// end ring
		index[++indexPos] = 0;

		// initialize with zero points
		index[++indexPos] = 0;

		// set new end marker
		index[indexPos + 1] = -1;
	}



	// ---- internals ----
	public float[] ensurePointSize(int size, boolean copy) {
		if (size * 2 < points.length)
			return points;

		float[] tmp = new float[size * 2 + 1024];
		if (copy)
			System.arraycopy(points, 0, tmp, 0, points.length);

		points = tmp;
		return points;
	}

	public short[] ensureIndexSize(int size, boolean copy) {
		if (size < index.length)
			return index;

		short[] tmp = new short[size + 128];
		if (copy)
			System.arraycopy(index, 0, tmp, 0, index.length);

		index = tmp;

		return index;
	}

	private void setOrCheckMode(int m) {
		if (mode == m)
			return;

		if (mode != 0)
			throw new IllegalArgumentException("GeometryBuffer not cleared " + m + "<>" + mode);

		mode = m;
	}

	private void checkMode(int m) {
		if (mode != m)
			throw new IllegalArgumentException("GeometryBuffer not cleared " + m + "<>" + mode);
	}

}
