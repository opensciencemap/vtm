/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.layers.tile.vector.labeling;

import org.oscim.core.MapPosition;
import org.oscim.event.MotionEvent;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.tiling.TileRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabelLayer extends Layer implements Map.InputListener, Map.UpdateListener {
	static final Logger log = LoggerFactory.getLogger(LabelLayer.class);
	private final TextRenderer mTextRenderer;

	//private int multi;

	public LabelLayer(Map map, TileRenderer tileRenderLayer) {
		super(map);

		//mTextLayer = new org.oscim.renderer.layers.TextRenderLayer(map, tileRenderLayer);
		mTextRenderer = new TextRenderer(map, tileRenderLayer);
		mRenderer = mTextRenderer;
	}

	@Override
	public void onDetach() {
		// TODO stop and clear labeling thread
		log.debug("DETACH");
		mTextRenderer.clearLabels();

		super.onDetach();
	}

	@Override
	public void onMotionEvent(MotionEvent e) {
		//	int action = e.getAction() & MotionEvent.ACTION_MASK;
		//	if (action == MotionEvent.ACTION_POINTER_DOWN) {
		//		multi++;
		//		mTextRenderer.hold(true);
		//	} else if (action == MotionEvent.ACTION_POINTER_UP) {
		//		multi--;
		//		if (multi == 0)
		//			mTextRenderer.hold(false);
		//	} else if (action == MotionEvent.ACTION_CANCEL) {
		//		multi = 0;
		//		log.debug("cancel " + multi);
		//		mTextRenderer.hold(false);
		//	}
	}

	@Override
	public void onMapUpdate(MapPosition mapPosition, boolean changed, boolean clear) {
		if (clear)
			mTextRenderer.clearLabels();

		mTextRenderer.update();
	}

	//	@Override
	//	public boolean onTouchEvent(MotionEvent e) {
	//		int action = e.getAction() & MotionEvent.ACTION_MASK;
	//		if (action == MotionEvent.ACTION_POINTER_DOWN) {
	//			multi++;
	//			mTextRenderer.hold(true);
	//		} else if (action == MotionEvent.ACTION_POINTER_UP) {
	//			multi--;
	//			if (multi == 0)
	//				mTextRenderer.hold(false);
	//		} else if (action == MotionEvent.ACTION_CANCEL) {
	//			multi = 0;
	//			log.debug("cancel " + multi);
	//			mTextRenderer.hold(false);
	//		}
	//
	//		return false;
	//	}

}
