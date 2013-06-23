/*
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
package org.oscim.layers.tile;

import static org.oscim.layers.tile.MapTile.STATE_READY;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.sublayers.BitmapRenderer;
import org.oscim.renderer.sublayers.Layer;
import org.oscim.renderer.sublayers.LineRenderer;
import org.oscim.renderer.sublayers.LineTexRenderer;
import org.oscim.renderer.sublayers.PolygonRenderer;
import org.oscim.utils.FastMath;
import org.oscim.utils.Matrix4;
import org.oscim.utils.quadtree.QuadTree;

/**
 * This class is for rendering the Line- and PolygonLayers of visible MapTiles.
 * For visible tiles that do not have data available yet its parent in children
 * tiles are rendered when available.
 *
 */
public class TileRenderer {
	//private final static String TAG = TileRenderer.class.getName();

	private static final GL20 GL = GLAdapter.INSTANCE;

	// Counter increases polygon-offset for each tile drawn.
	private static int mOffsetCnt;

	// Current number of frames drawn, used to not draw a
	// tile twice per frame.
	private static int mDrawSerial = 0;

	private static Matrices mMatrices;
	private static boolean mFaded;

	private static final Matrix4 mProjMatrix = new Matrix4();

	static void draw(MapTile[] tiles, int tileCnt, MapPosition pos, Matrices m, boolean fade) {
		mOffsetCnt = -2048;
		mMatrices = m;
		mFaded = fade;

		mProjMatrix.copy(m.proj);
		// discard depth projection from tilt, we use depth buffer
		// for clipping
		mProjMatrix.setValue(10, 0);
		mProjMatrix.setValue(14, 0);
		mProjMatrix.multiplyRhs(m.view);

		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GL.glDepthFunc(GL20.GL_LESS);

		// load texture for line caps
		LineRenderer.beginLines();

		// Draw visible tiles
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.state == STATE_READY)
				drawTile(t, pos);
		}

		double scale = pos.getZoomScale();

		// Draw parent or children as proxy for visibile tiles that dont
		// have data yet. Proxies are clipped to the region where nothing
		// was drawn to depth buffer.
		// TODO draw proxies for placeholder
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && (t.state != STATE_READY) && (t.holder == null)) {
				boolean preferParent = (scale > 1.5) || (pos.zoomLevel - t.zoomLevel < 0);
				drawProxyTile(t, pos, true, preferParent);
			}
		}

		// Draw grandparents
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && (t.state != STATE_READY) && (t.holder == null))
				drawProxyTile(t, pos, false, false);
		}

		// make sure stencil buffer write is disabled
		GL.glStencilMask(0x00);

		LineRenderer.endLines();

		mDrawSerial++;

		// clear reference
		mMatrices = null;
	}

	private static void drawTile(MapTile tile, MapPosition pos) {
		// draw parents only once
		if (tile.lastDraw == mDrawSerial)
			return;

		tile.lastDraw = mDrawSerial;

		MapTile t = tile;
		if (t.holder != null)
			t = t.holder;

		if (t.layers == null || t.layers.vbo == null) {
			//Log.d(TAG, "missing data " + (t.layers == null) + " " + (t.vbo == null));
			return;
		}

		t.layers.vbo.bindArrayBuffer();

		// place tile relative to map position
		int z = tile.zoomLevel;

		float div = FastMath.pow(z - pos.zoomLevel);

		double curScale = Tile.SIZE * pos.scale;
		double scale = (pos.scale / (1 << z));

		float x = (float) ((tile.x - pos.x) * curScale);
		float y = (float) ((tile.y - pos.y) * curScale);

		Matrices m = mMatrices;
		m.mvp.setTransScale(x, y, (float) (scale / GLRenderer.COORD_SCALE));

		m.mvp.multiplyLhs(mProjMatrix);

		// set depth offset (used for clipping to tile boundaries)
		GL.glPolygonOffset(0, mOffsetCnt++);

		// simple line shader does not take forward shortening into
		// account. only used when tilt is 0.
		int simpleShader = (pos.tilt < 1 ? 1 : 0);

		boolean clipped = false;

		for (Layer l = t.layers.baseLayers; l != null;) {
			switch (l.type) {
				case Layer.POLYGON:
					l = PolygonRenderer.draw(pos, l, m, !clipped, div, true);
					clipped = true;
					break;

				case Layer.LINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(pos, null, m, true, div, true);
						clipped = true;
					}
					l = LineRenderer.draw(t.layers, l, pos, m, div, simpleShader);
					break;

				case Layer.TEXLINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(pos, null, m, true, div, true);
						clipped = true;
					}
					l = LineTexRenderer.draw(t.layers, l, pos, m, div);
					break;

				default:
					// just in case
					l = l.next;
			}
		}

		for (Layer l = t.layers.textureLayers; l != null;) {
			switch (l.type) {
				case Layer.BITMAP:
					l = BitmapRenderer.draw(l, 1, m);
					break;

				default:
					l = l.next;
			}
		}

		// clear clip-region and could also draw 'fade-effect'
		if (mFaded)
			PolygonRenderer.drawOver(m, true, 0x22000000);
		else
			PolygonRenderer.drawOver(m, false, 0);
	}

	private static int drawProxyChild(MapTile tile, MapPosition pos) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if ((tile.proxies & 1 << i) == 0)
				continue;

			MapTile c = tile.rel.get(i);

			if (c.state == STATE_READY) {
				drawTile(c, pos);
				drawn++;
			}
		}
		return drawn;
	}

	// just FIXME!
	private static void drawProxyTile(MapTile tile, MapPosition pos, boolean parent,
			boolean preferParent) {

		QuadTree<MapTile> r = tile.rel;
		MapTile proxy;

		if (!preferParent) {
			// prefer drawing children
			if (drawProxyChild(tile, pos) == 4)
				return;

			if (parent) {
				// draw parent proxy
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy.state == STATE_READY) {
						//Log.d(TAG, "1. draw parent " + proxy);
						drawTile(proxy, pos);
					}
				}
			} else if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
				// check if parent was already drawn
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy.state == STATE_READY)
						return;
				}

				proxy = r.parent.parent.item;
				if (proxy.state == STATE_READY)
					drawTile(proxy, pos);
			}
		} else {
			// prefer drawing parent
			if (parent) {
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy != null && proxy.state == STATE_READY) {
						//Log.d(TAG, "2. draw parent " + proxy);
						drawTile(proxy, pos);
						return;

					}
				}
				drawProxyChild(tile, pos);

			} else if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
				// check if parent was already drawn
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy.state == STATE_READY)
						return;
				}
				// this will do nothing, just to check
				if (drawProxyChild(tile, pos) > 0)
					return;

				proxy = r.parent.parent.item;
				if (proxy.state == STATE_READY)
					drawTile(proxy, pos);
			}
		}
	}
}
