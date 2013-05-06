package org.oscim.core;

public class GeometryBuffer {
	public enum GeometryType {

		NONE(0),
		POINT(1),
		LINE(2),
		POLY(3);

		private GeometryType(int type) {
			nativeInt = type;
		}

		public final int nativeInt;

	}

	// TODO
	// -- check indexPos < Short.Max
	final static boolean CHECK_STATE = true;

	public float[] points;
	public short[] index;
	public int indexPos;
	public int pointPos;

	//public int type;
	// GEOM_*
	public GeometryType type;

	public GeometryBuffer(float[] points, short[] index) {
		if (points == null)
			throw new IllegalArgumentException("GeometryBuffer points is null");
		if (index == null)
			throw new IllegalArgumentException("GeometryBuffer index is null");

		this.points = points;
		this.index = index;
		this.type = GeometryType.NONE;
		this.indexPos = 0;
		this.pointPos = 0;
	}

	public GeometryBuffer(int numPoints, int numIndices) {
		this.points = new float[numPoints * 2];
		this.index = new short[numIndices];
		this.type = GeometryType.NONE;
		this.indexPos = 0;
		this.pointPos = 0;
	}

	// ---- API ----
	public void clear() {
		index[0] = -1;
		indexPos = 0;
		pointPos = 0;
		type = GeometryType.NONE;
	}

	public void addPoint(float x, float y) {
		if (pointPos >= points.length - 1)
			ensurePointSize((pointPos >> 1) + 1, true);

		points[pointPos++] = x;
		points[pointPos++] = y;

		index[indexPos] += 2;
	}
	public boolean isPoly(){
		return type == GeometryType.POLY;
	}
	public boolean isLine(){
		return type == GeometryType.LINE;
	}
	public boolean isPoint(){
		return type == GeometryType.POINT;
	}

	public void setPoint(int pos, float x, float y) {
		points[(pos << 1) + 0] = x;
		points[(pos << 1) + 1] = y;
	}

	public void startPoints() {
		if (CHECK_STATE)
			setOrCheckMode(GeometryType.POINT);
	}

	public void startLine() {
		if (CHECK_STATE)
			setOrCheckMode(GeometryType.LINE);

		// start next
		if ((index[0] >= 0) && (++indexPos >= index.length))
			ensureIndexSize(indexPos, true);

		// initialize with zero points
		index[indexPos] = 0;

		// set new end marker
		if (index.length > indexPos + 1)
			index[indexPos + 1] = -1;
	}

	public void startPolygon() {
		if (CHECK_STATE)
			setOrCheckMode(GeometryType.POLY);

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
		if (index.length > indexPos + 1)
			index[indexPos + 1] = -1;
	}

	public void startHole() {
		if (CHECK_STATE)
			checkMode(GeometryType.POLY);

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

	private void setOrCheckMode(GeometryType m) {
		if (type == m)
			return;

		if (type != GeometryType.NONE)
			throw new IllegalArgumentException("GeometryBuffer not cleared " + m + "<>" + type);

		type = m;
	}

	private void checkMode(GeometryType m) {
		if (type != m)
			throw new IllegalArgumentException("GeometryBuffer not cleared " + m + "<>" + type);
	}

}
