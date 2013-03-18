package org.oscim.core;

public class GeometryBuffer {
	public float[] points;
	public short[] index;
	public int indexPos;
	public int pointPos;

	public GeometryBuffer(float[] points, short[] index){
		this.points = points;
		this.index = index;
	}
	public GeometryBuffer(int numPoints, int numIndices) {
		this.points = new float[numPoints * 2];
		this.index = new short[numIndices];
	}

	public float[] ensurePointSize(int size, boolean copy){
		if (size * 2 < points.length)
			return points;

		float[] tmp = new float[size * 2 + 1024];
		if (copy)
			System.arraycopy(tmp, 0, points, 0, points.length);

		points = tmp;
		return points;
	}

	public short[] ensureIndexSize(int size, boolean copy){
		if (size < index.length)
			return index;

		short[] tmp = new short[size + 128];
		if (copy)
			System.arraycopy(tmp, 0, index, 0, index.length);

		index = tmp;

		return index;
	}
}
