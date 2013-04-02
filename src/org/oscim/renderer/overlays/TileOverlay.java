/*
 * Copyright 2013 ...
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
package org.oscim.renderer.overlays;

import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.LineRenderer;
import org.oscim.renderer.LineTexRenderer;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.PolygonRenderer;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.Layer;
import org.oscim.utils.FastMath;
import org.oscim.utils.Matrix4;
import org.oscim.view.MapView;

import android.opengl.GLES20;

public class TileOverlay extends RenderOverlay {

	private TileSet mTileSet;

	public TileOverlay(MapView mapView) {
		super(mapView);

		this.isReady = true;
	}

	@Override
	public void update(MapPosition curPos, boolean positionChanged, boolean tilesChanged,
			Matrices matrices) {

		// TODO Auto-generated method stub

	}

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(MapPosition pos, Matrices m) {
		GLES20.glDepthMask(true);
		GLES20.glStencilMask(0xFF);

		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);



		// get current tiles

		mTileSet = GLRenderer.getVisibleTiles(mTileSet);

		mDrawCnt = 0;
		GLES20.glDepthFunc(GLES20.GL_LESS);

		// load texture for line caps
		LineRenderer.beginLines();

		for(int i = 0; i < mTileSet.cnt; i++){
			MapTile t = mTileSet.tiles[i];
			drawTile(t, pos, m);
		}

		LineRenderer.endLines();

	}

	private static Matrix4 scaleMatrix  = new Matrix4();

private static int mDrawCnt;
	private static void drawTile(MapTile tile, MapPosition pos, Matrices m) {
		MapTile t = tile;

		//if (t.holder != null)
		//	t = t.holder;

		if (t.layers == null || t.layers.vbo == null) {
			//Log.d(TAG, "missing data " + (t.layers == null) + " " + (t.vbo == null));
			return;
		}

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, t.layers.vbo.id);

		// place tile relative to map position
		float div = FastMath.pow(tile.zoomLevel - pos.zoomLevel);
		float x = (float) (tile.pixelX - pos.x * div);
		float y = (float) (tile.pixelY - pos.y * div);
		float scale = pos.scale / div;

		x += 400 / scale;
		y += 400 / scale;

		m.mvp.setTransScale(x * scale, y * scale, scale / GLRenderer.COORD_SCALE);
		m.mvp.multiplyMM(m.viewproj, m.mvp);

		scaleMatrix.setScale(0.5f, 0.5f, 1);

		m.mvp.multiplyMM(scaleMatrix, m.mvp);

		// set depth offset (used for clipping to tile boundaries)
		GLES20.glPolygonOffset(1, mDrawCnt++);
		if (mDrawCnt > 20)
			mDrawCnt = 0;

		// simple line shader does not take forward shortening into account
		int simpleShader = 0; //= (pos.tilt < 1 ? 1 : 0);

		boolean clipped = false;

		for (Layer l = t.layers.baseLayers; l != null;) {
			switch (l.type) {
				case Layer.POLYGON:
					l = PolygonRenderer.draw(pos, l, m, !clipped, true);
					clipped = true;
					break;

				case Layer.LINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(pos, null, m, true, true);
						clipped = true;
					}
					l = LineRenderer.draw(t.layers, l, pos, m, div, simpleShader);
					break;

				case Layer.TEXLINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(pos, null, m, true, true);
						clipped = true;
					}
					l = LineTexRenderer.draw(t.layers, l, pos, m, div);
					break;

				default:
					// just in case
					l = l.next;
			}
		}

		// clear clip-region and could also draw 'fade-effect'
		PolygonRenderer.drawOver(m);
	}

}
