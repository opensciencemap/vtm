/*
 * Copyright 2013 OpenScienceMap
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

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_POLYGON_OFFSET_FILL;
import static org.oscim.generator.JobTile.STATE_READY;

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.Layer;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * @author Hannes Janetzek
 */
public class BaseMap {
	private final static String TAG = BaseMap.class.getName();

	// used to not draw a tile twice per frame.
	private static int mDrawSerial = 0;
	private static float[] mMVPMatrix = new float[16];

	private static float[] mVPMatrix = new float[16];
	private static float[] mfProjMatrix = new float[16];

	static void setProjection(float[] projMatrix) {
		System.arraycopy(projMatrix, 0, mfProjMatrix, 0, 16);
		// set to zero: we modify the z value with polygon-offset for clipping
		mfProjMatrix[10] = 0;
		mfProjMatrix[14] = 0;
	}

	private static int mDrawCnt;

	static void draw(MapTile[] tiles, int tileCnt, MapPosition pos) {
		//long start = SystemClock.uptimeMillis();
		Matrix.multiplyMM(mVPMatrix, 0, mfProjMatrix, 0, pos.viewMatrix, 0);

		mDrawCnt = 0;

		GLES20.glEnable(GL_POLYGON_OFFSET_FILL);
		LineRenderer.beginLines();
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.state == STATE_READY)
				drawTile(t, pos);
		}

		// proxies are clipped to the region where nothing was drawn to depth
		// buffer.
		// TODO draw all parent before grandparent
		// TODO draw proxies for placeholder...
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && (t.state != STATE_READY) && (t.holder == null))
				drawProxyTile(t, pos, true);
		}

		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && (t.state != STATE_READY) && (t.holder == null))
				drawProxyTile(t, pos, false);
		}

		LineRenderer.endLines();

		GLES20.glDisable(GL_POLYGON_OFFSET_FILL);
		//GLES20.glFinish();
		//long end = SystemClock.uptimeMillis();
		//Log.d(TAG, "base took " + (end - start));
		mDrawSerial++;
	}

	private static void drawTile(MapTile tile, MapPosition pos) {
		// draw parents only once
		if (tile.lastDraw == mDrawSerial)
			return;

		tile.lastDraw = mDrawSerial;

		float[] mvp = mMVPMatrix;

		//setMatrix(mvp, tile, div, pos);

		MapTile t = tile;
		if (t.holder != null)
			t = t.holder;

		if (t.layers == null || t.vbo == null)
			return;

		// set Model matrix for tile
		float div = FastMath.pow(tile.zoomLevel - pos.zoomLevel);
		float x = (float) (tile.pixelX - pos.x * div);
		float y = (float) (tile.pixelY - pos.y * div);
		float scale = pos.scale / div;
		GlUtils.setTileMatrix(mvp, x, y, scale);

		// add view-projection matrix
		Matrix.multiplyMM(mvp, 0, mVPMatrix, 0, mvp, 0);

		// set depth offset (used for clipping to tile boundaries)
		//GLES20.glPolygonOffset(-1, -GLRenderer.depthOffset(t));
		GLES20.glPolygonOffset(-1, (mDrawCnt++));

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, t.vbo.id);

		boolean clipped = false;
		// simple line shader does not take forward shortening into account
		int simpleShader = (pos.tilt < 1 ? 1 : 0);

		for (Layer l = t.layers.layers; l != null;) {

			switch (l.type) {
				case Layer.POLYGON:
					GLES20.glDisable(GL_BLEND);
					l = PolygonRenderer.draw(pos, l, mvp, !clipped, true);
					clipped = true;
					break;

				case Layer.LINE:
					if (!clipped) {
						PolygonRenderer.draw(pos, null, mvp, true, true);
						clipped = true;
					}
					// clip lines to quad in depth buffer
					GLState.test(true, false);

					GLES20.glEnable(GL_BLEND);
					l = LineRenderer.draw(pos, l, mvp, div, simpleShader,
							t.layers.lineOffset);
					break;
			}
		}
	}

	private static int drawProxyChild(MapTile tile, MapPosition pos) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if ((tile.proxies & 1 << i) == 0)
				continue;

			MapTile c = tile.rel.child[i].tile;

			if (c.state == STATE_READY) {
				drawTile(c, pos);
				drawn++;
			}
		}
		return drawn;
	}

	private static void drawProxyTile(MapTile tile, MapPosition pos, boolean parent) {
		int diff = pos.zoomLevel - tile.zoomLevel;

		if (pos.scale > 1.5f || diff < 0) {
			// prefer drawing children
			if (drawProxyChild(tile, pos) == 4)
				return;

			if (parent) {
				// draw parent proxy
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile t = tile.rel.parent.tile;
					if (t.state == STATE_READY)
						drawTile(t, pos);
				}
			} else if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
				// check if parent was already drawn
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile t = tile.rel.parent.tile;
					if (t.state == STATE_READY)
						return;
				}

				MapTile t = tile.rel.parent.parent.tile;
				if (t.state == STATE_READY)
					drawTile(t, pos);
			}
		} else {
			// prefer drawing parent
			if (parent) {
				MapTile t = tile.rel.parent.tile;

				if (t != null && t.state == STATE_READY) {
					drawTile(t, pos);
					return;

				}

				drawProxyChild(tile, pos);

			} else if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
				// check if parent was already drawn
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile t = tile.rel.parent.tile;
					if (t.state == STATE_READY)
						return;
				}
				// this will do nothing, just to check
				if (drawProxyChild(tile, pos) > 0)
					return;

				MapTile t = tile.rel.parent.parent.tile;
				if (t.state == STATE_READY)
					drawTile(t, pos);
			}
		}
	}
}
