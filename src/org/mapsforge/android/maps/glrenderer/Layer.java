package org.mapsforge.android.maps.glrenderer;

import java.util.LinkedList;

class Layer {
	LinkedList<PoolItem> pool;
	protected PoolItem curItem;

	int verticesCnt;
	int offset;

	final int layer;
	final int color;

	Layer(int l, int c) {
		color = c;
		layer = l;
		verticesCnt = 0;
	}

	float[] getNextItem() {
		curItem.used = PoolItem.SIZE;
		curItem = LayerPool.get();
		pool.add(curItem);
		return curItem.vertices;
	}
}
