/*
 * Copyright 2012 osmdroid authors
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

package org.oscim.map;

import java.util.AbstractList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.oscim.layers.Layer;
import org.oscim.renderer.LayerRenderer;

public class Layers extends AbstractList<Layer> {
	//private final static String TAG = Layers.class.getName();

	private final CopyOnWriteArrayList<Layer> mLayerList;

	Layers() {
		mLayerList = new CopyOnWriteArrayList<Layer>();
	}

	@Override
	public synchronized Layer get(int index) {
		return mLayerList.get(index);
	}

	@Override
	public synchronized int size() {
		return mLayerList.size();
	}

	@Override
	public synchronized void add(int index, Layer element) {
		mLayerList.add(index, element);
		mDirtyLayers = true;
	}

	@Override
	public synchronized Layer remove(int index) {
		mDirtyLayers = true;
		return mLayerList.remove(index);
	}

	@Override
	public synchronized Layer set(int index, Layer element) {
		mDirtyLayers = true;
		return mLayerList.set(index, element);
	}

	private boolean mDirtyLayers;
	private LayerRenderer[] mLayerRenderer;

	public LayerRenderer[] getLayerRenderer() {
		if (mDirtyLayers)
			updateLayers();

		return mLayerRenderer;
	}

	public void destroy() {
		if (mDirtyLayers)
			updateLayers();

		for (Layer o : mLayers)
			o.onDetach();
	}

	Layer[] mLayers;

	private synchronized void updateLayers() {
		if (!mDirtyLayers)
			return;

		mLayers = new Layer[mLayerList.size()];

		int numRenderLayers = 0;

		for (int i = 0, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);

			if (o.getRenderer() != null)
				numRenderLayers++;
			mLayers[i] = o;
		}

		mLayerRenderer = new LayerRenderer[numRenderLayers];

		for (int i = 0, cntR = 0, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);
			LayerRenderer l = o.getRenderer();
			if (l != null)
				mLayerRenderer[cntR++] = l;
		}

		mDirtyLayers = false;
	}
}
