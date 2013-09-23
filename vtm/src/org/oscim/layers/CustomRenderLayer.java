package org.oscim.layers;

import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer.Matrices;

public class CustomRenderLayer extends Layer implements UpdateListener {

	private static final String TAG = CustomRenderLayer.class.getName();

	class CustomRenderer extends LayerRenderer {

		// functions running on MapRender Thread
		@Override
		protected void update(MapPosition pos, boolean changed, Matrices matrices) {
			int currentState;

			synchronized (this) {
				currentState = someConccurentVariable;
				compile();
			}
			Log.d(TAG, "state " + currentState);

		}

		protected void compile() {
			setReady(true);
		}

		@Override
		protected void render(MapPosition pos, Matrices m) {
		}
	}

	public CustomRenderLayer(Map map, LayerRenderer renderer) {

		super(map);
		mRenderer = new CustomRenderer();
	}

	private int someConccurentVariable;

	@Override
	public void onMapUpdate(MapPosition mapPosition, boolean changed, boolean clear) {

		synchronized (mRenderer) {
			someConccurentVariable++;
		}
	}
}
