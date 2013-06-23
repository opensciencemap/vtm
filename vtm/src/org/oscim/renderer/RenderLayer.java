/*
 * Copyright 2012, Hannes Janetzek
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
package org.oscim.renderer;

import org.oscim.view.MapView;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer.Matrices;

public abstract class RenderLayer {

	protected final MapView mMapView;
	/**
	 * Use mMapPosition.copy(position) to keep the position for which
	 * the Overlay is _compiled_. NOTE: required by setMatrix utility
	 * functions to draw this layer fixed to the map
	 */
	protected MapPosition mMapPosition;

	/** flag to set when data is ready for (re)compilation. */
	public boolean newData;

	/** flag to set when layer is ready for rendering */
	public boolean isReady;

	public RenderLayer(MapView mapView) {
		mMapView = mapView;
		mMapPosition = new MapPosition();
	}

	/**
	 * ////////////////////// GLRender Thread ///////////////////////////
	 * 1. Called first by GLRenderer:
	 * Update the layer state here. Set 'this.newData = true' when
	 * 'compile()' should be called before next call to 'render()'
	 *
	 * @param position current MapPosition
	 * @param changed
	 *            true when MapPosition has changed since last frame
	 * @param matrices contains the current view- and projection-matrices
	 *            and 'mvp' matrix for temporary use.
	 */
	public abstract void update(MapPosition position, boolean changed,
			Matrices matrices);

	/**
	 * 2. Compile vertex buffers and upload textures for drawing:
	 * Set 'this.isReady = true' when things are ready for 'render()'
	 */
	public abstract void compile();

	/**
	 * 3. Draw layer:
	 *
	 * @param position current MapPosition
	 * @param matrices contains the current view- and projection-matrices.
	 *            'matrices.mvp' is for temporary use to build the model-
	 *            view-projection to set as uniform.
	 */
	public abstract void render(MapPosition position, Matrices matrices);

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition.
	 * Use this to 'stick' your layer to the map.
	 *
	 * @param position
	 *            current MapPosition
	 * @param matrices
	 *            current Matrices
	 * @param project
	 *            if true apply view- and projection, or just view otherwise.
	 */
	protected void setMatrix(MapPosition position, Matrices matrices, boolean project) {
		MapPosition oPos = mMapPosition;

		double tileScale = Tile.SIZE * position.scale;

		double x = oPos.x - position.x;
		double y = oPos.y - position.y;

		// wrap around date-line
		//	while (x < -1)
		//		x += 1.0;
		//	while (x > 2)
		//		x -= 1.0;

		matrices.mvp.setTransScale((float) (x * tileScale), (float) (y * tileScale),
				(float) ((position.scale / oPos.scale) / GLRenderer.COORD_SCALE));

		matrices.mvp.multiplyLhs(project ? matrices.viewproj : matrices.view);
	}

	/**
	 * Utility: Set matrices.mvp matrix relative to the difference of current
	 * MapPosition and the last updated Overlay MapPosition and add
	 * matrices.viewproj
	 *
	 * @param position ...
	 * @param matrices ...
	 */
	protected void setMatrix(MapPosition position, Matrices matrices) {
		setMatrix(position, matrices, true);
	}
}
