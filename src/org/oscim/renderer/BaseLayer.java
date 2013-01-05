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
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_POLYGON_OFFSET_FILL;
import static org.oscim.generator.JobTile.STATE_READY;

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.Layer;
import org.oscim.utils.FastMath;

import android.opengl.GLES20;
import android.opengl.Matrix;

/**
 * @author Hannes Janetzek
 */
public class BaseLayer {

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

	static void draw(MapTile[] tiles, int tileCnt, MapPosition pos) {

		Matrix.multiplyMM(mVPMatrix, 0, mfProjMatrix, 0, pos.viewMatrix, 0);

		/* draw base layer */
		GLES20.glEnable(GL_DEPTH_TEST);
		GLES20.glEnable(GL_POLYGON_OFFSET_FILL);
		//	mDrawCount = 0;

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
				drawProxyTile(t, pos);
		}

		GLES20.glDisable(GL_POLYGON_OFFSET_FILL);
		GLES20.glDisable(GL_DEPTH_TEST);
		mDrawSerial++;
	}

	private static void drawTile(MapTile tile, MapPosition pos) {
		// draw parents only once
		if (tile.lastDraw == mDrawSerial)
			return;

		float div = FastMath.pow(tile.zoomLevel - pos.zoomLevel);

		tile.lastDraw = mDrawSerial;

		float[] mvp = mMVPMatrix;
		setMatrix(mvp, tile, div, pos);

		if (tile.holder != null)
			tile = tile.holder;

		if (tile.layers == null)
			return;

		GLES20.glPolygonOffset(0, GLRenderer.depthOffset(tile));

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, tile.vbo.id);

		boolean clipped = false;
		int simpleShader = 0; // mRotate ? 0 : 1;

		for (Layer l = tile.layers.layers; l != null;) {

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

					GLES20.glEnable(GL_BLEND);
					l = LineRenderer.draw(pos, l, mvp, div, simpleShader,
							tile.layers.lineOffset);
					break;
			}
		}

		//		if (tile.layers.textureLayers != null) {
		//			setMatrix(mvp, tile, div, false);
		//
		//			for (Layer l = tile.layers.textureLayers; l != null;) {
		//				l = TextureRenderer.draw(l, 1, mProjMatrix, mvp,
		//						tile.layers.texOffset);
		//			}
		//		}
	}

	private static void setMatrix(float[] matrix, MapTile tile,
			float div, MapPosition pos) {

		float x = (float) (tile.pixelX - pos.x * div);
		float y = (float) (tile.pixelY - pos.y * div);
		float scale = pos.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;

		// scale to tile to world coordinates
		scale /= GLRenderer.COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;

		//	Matrix.multiplyMM(matrix, 0, pos.viewMatrix, 0, matrix, 0);
		//	Matrix.multiplyMM(matrix, 0, mfProjMatrix, 0, matrix, 0);
		Matrix.multiplyMM(matrix, 0, mVPMatrix, 0, matrix, 0);
	}

	private static boolean drawProxyChild(MapTile tile, MapPosition pos) {
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
		return drawn == 4;
	}

	private static void drawProxyTile(MapTile tile, MapPosition pos) {
		int diff = pos.zoomLevel - tile.zoomLevel;

		boolean drawn = false;
		if (pos.scale > 1.5f || diff < 0) {
			// prefer drawing children
			if (!drawProxyChild(tile, pos)) {
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					MapTile t = tile.rel.parent.tile;
					if (t.state == STATE_READY) {
						drawTile(t, pos);
						drawn = true;
					}
				}

				if (!drawn && (tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
					MapTile t = tile.rel.parent.parent.tile;
					if (t.state == STATE_READY)
						drawTile(t, pos);

				}
			}
		} else {
			// prefer drawing parent
			MapTile t = tile.rel.parent.tile;

			if (t != null && t.state == STATE_READY) {
				drawTile(t, pos);

			} else if (!drawProxyChild(tile, pos)) {

				if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
					t = tile.rel.parent.parent.tile;
					if (t.state == STATE_READY)
						drawTile(t, pos);
				}
			}
		}
	}
}
