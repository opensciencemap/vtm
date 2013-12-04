package org.oscim.map;

import java.util.AbstractList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.map.Map.InputListener;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.LayerRenderer;

public final class Layers extends AbstractList<Layer> {

	private final CopyOnWriteArrayList<Layer> mLayerList;
	private final Map mMap;

	private boolean mDirtyLayers;
	private LayerRenderer[] mLayerRenderer;
	private Layer[] mLayers;

	Layers(Map map) {
		mMap = map;
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
	public synchronized void add(int index, Layer layer) {
		if (mLayerList.contains(layer))
			throw new IllegalArgumentException("layer added twice");

		if (layer instanceof UpdateListener)
			mMap.bind((UpdateListener) layer);
		//if (layer instanceof InputListener)
		//	mMap.bind((InputListener) layer);

		mLayerList.add(index, layer);
		mDirtyLayers = true;
	}

	@Override
	public synchronized Layer remove(int index) {
		mDirtyLayers = true;

		Layer remove = mLayerList.remove(index);

		if (remove instanceof UpdateListener)
			mMap.unbind((UpdateListener) remove);
		//if (remove instanceof InputListener)
		//	mMap.unbind((InputListener) remove);

		return remove;
	}

	@Override
	public synchronized Layer set(int index, Layer layer) {
		if (mLayerList.contains(layer))
			throw new IllegalArgumentException("layer added twice");

		mDirtyLayers = true;
		Layer remove = mLayerList.set(index, layer);

		// unbind replaced layer
		if (remove instanceof UpdateListener)
			mMap.unbind((UpdateListener) remove);
		//if (remove instanceof InputListener)
		//	mMap.unbind((InputListener) remove);

		return remove;
	}

	/**
	 * Should only be used by MapRenderer.
	 * 
	 * @return the current LayerRenderer as array.
	 * */
	public LayerRenderer[] getLayerRenderer() {
		if (mDirtyLayers)
			updateLayers();

		return mLayerRenderer;
	}

	void destroy() {
		if (mDirtyLayers)
			updateLayers();

		for (Layer o : mLayers)
			o.onDetach();

		// TODO need to clear lists here?
	}

	boolean handleGesture(Gesture g, MotionEvent e) {
		if (mDirtyLayers)
			updateLayers();

		for (Layer o : mLayers)
			if (o instanceof GestureListener)
				if (((GestureListener) o).onGesture(g, e))
					return true;

		return false;
	}

	void handleMotionEvent(MotionEvent e) {
		if (mDirtyLayers)
			updateLayers();

		for (Layer o : mLayers)
			if (o instanceof InputListener)
				((InputListener) o).onMotionEvent(e);
	}

	private synchronized void updateLayers() {
		mLayers = new Layer[mLayerList.size()];
		int numRenderLayers = 0;

		for (int i = 0, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);

			if (o.getRenderer() != null)
				numRenderLayers++;
			mLayers[i] = o;
		}

		mLayerRenderer = new LayerRenderer[numRenderLayers];

		for (int i = 0, cnt = 0, n = mLayerList.size(); i < n; i++) {
			Layer o = mLayerList.get(i);
			LayerRenderer l = o.getRenderer();
			if (l != null)
				mLayerRenderer[cnt++] = l;
		}

		mDirtyLayers = false;
	}
}
