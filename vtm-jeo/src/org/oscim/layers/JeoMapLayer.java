package org.oscim.layers;

import org.jeo.data.Dataset;
import org.jeo.map.Style;
import org.oscim.core.MapPosition;
import org.oscim.layers.JeoMapLoader.Task;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JeoMapLayer extends Layer implements UpdateListener {

	public static final Logger log = LoggerFactory.getLogger(JeoMapLayer.class);

	final org.jeo.map.View view;
	private final org.jeo.map.Map mJeoMap;

	private final JeoMapLoader mWorker;

	public JeoMapLayer(Map map, Dataset data, Style style) {
		super(map);

		mJeoMap = org.jeo.map.Map.build().layer(data).style(style).map();
		view = mJeoMap.getView();

		mRenderer = new ElementRenderer() {
			@Override
			protected synchronized void update(MapPosition position, boolean changed,
			        Matrices matrices) {

				if (mNewLayers != null) {
					mMapPosition.copy(mNewLayers);

					this.layers.clear();
					this.layers.baseLayers = mNewLayers.layers;
					mNewLayers = null;

					compile();
					log.debug("is ready " + isReady() + " " + layers.getSize());
				}
			}
		};

		mWorker = new JeoMapLoader(this);
		mWorker.start();
	}

	@Override
	public void onDetach() {
		super.onDetach();

		mWorker.awaitPausing();
		try {
			mWorker.join();
		} catch (Exception e) {
			log.error(e.toString());
		}
	}

	@Override
	public void onMapUpdate(MapPosition pos, boolean changed, boolean clear) {
		if (changed) {
			log.debug("go");
			mWorker.go();
		}
	}

	Task mNewLayers;

	void setLayers(Task newLayers) {
		synchronized (mRenderer) {
			mNewLayers = newLayers;
		}
		mMap.render();
	}

}
