/*
 * Copyright 2012 Hannes Janetzek
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

// TODO
// 1. rewrite. seriously
// 1.1 test if label is actually visible
// 2. compare previous to current state
// 2.1 test for new labels to be placed
// 2.2 handle collisions
// 2.3 try to place labels along a way
// 2.4 use 4 point labeling
// 3 join segments that belong to one feature
// 4 handle zoom-level changes
// 5 QuadTree might be handy
//

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.layers.tile.vector.labeling.LabelLayer.Worker;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.elements.RenderElement;
import org.oscim.renderer.elements.TextureLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TextRenderer extends ElementRenderer {
	static final Logger log = LoggerFactory.getLogger(TextRenderer.class);
	static final boolean dbg = false;

	private final Worker mWorker;

	public TextRenderer(Worker worker) {
		mWorker = worker;

		layers.useVBO = GLAdapter.VBO_TEXTURE_LAYERS;
	}

	long lastDraw = 0;

	@Override
	public synchronized void update(GLViewport v) {

		LabelTask t;
		synchronized (mWorker) {
			t = mWorker.poll();
			if (t == null) {
				if (!mWorker.isRunning()) {
					mWorker.submit(50);
				}
				return;
			}
			layers.clear();
		}

		// set new TextLayer to be uploaded and rendered
		layers.setTextureLayers(t.layers);
		mMapPosition = t.pos;
		compile();
	}

	@Override
	public synchronized void render(GLViewport v) {
		GLState.test(false, false);
		//Debug.draw(pos, layers);

		if (layers.useVBO)
			layers.vbo.bind();
		else
			GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

		float scale = (float) (v.pos.scale / mMapPosition.scale);

		setMatrix(v, false);

		for (RenderElement l = layers.getTextureLayers(); l != null;)
			l = TextureLayer.Renderer.draw(layers, l, v, scale);
	}

}
