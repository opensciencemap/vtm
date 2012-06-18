package org.mapsforge.android.maps.glrenderer;

import java.util.LinkedList;

class PolygonLayer extends Layer {
	int fadeLevel;
	private boolean first = true;
	private float originX;
	private float originY;

	PolygonLayer(int layer, int color, int fade) {
		super(layer, color);
		fadeLevel = fade;
		curItem = LayerPool.get();
		pool = new LinkedList<PoolItem>();
		pool.add(curItem);
	}

	void addPolygon(float[] points, int pos, int length) {

		verticesCnt += length / 2 + 2;

		if (first) {
			first = false;
			originX = points[pos];
			originY = points[pos + 1];
		}

		float[] curVertices = curItem.vertices;
		int outPos = curItem.used;

		if (outPos == PoolItem.SIZE) {
			curVertices = getNextItem();
			outPos = 0;
		}

		curVertices[outPos++] = originX;
		curVertices[outPos++] = originY;

		int remaining = length;
		int inPos = pos;
		while (remaining > 0) {

			if (outPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				outPos = 0;
			}

			int len = remaining;
			if (len > (PoolItem.SIZE) - outPos)
				len = (PoolItem.SIZE) - outPos;

			System.arraycopy(points, inPos, curVertices, outPos, len);
			outPos += len;
			inPos += len;
			remaining -= len;
		}

		if (outPos == PoolItem.SIZE) {
			curVertices = getNextItem();
			outPos = 0;
		}

		curVertices[outPos++] = points[pos + 0];
		curVertices[outPos++] = points[pos + 1];

		curItem.used = outPos;
	}
}
