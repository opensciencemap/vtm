package org.oscim.layers.tile;

import static org.oscim.layers.tile.MapTile.State.READY;
import static org.oscim.renderer.elements.RenderElement.BITMAP;
import static org.oscim.renderer.elements.RenderElement.LINE;
import static org.oscim.renderer.elements.RenderElement.MESH;
import static org.oscim.renderer.elements.RenderElement.POLYGON;
import static org.oscim.renderer.elements.RenderElement.TEXLINE;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile.TileNode;
import org.oscim.renderer.GLMatrix;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.renderer.elements.RenderElement;
import org.oscim.utils.FastMath;

public class VectorTileRenderer extends TileRenderer {

	static final boolean debugOverdraw = false;

	protected int mClipMode;

	protected GLMatrix mViewProj = new GLMatrix();

	//	public VectorTileRenderer(TileManager tileManager) {
	//		super(tileManager);
	//	}

	@Override
	protected synchronized void update(GLViewport v) {
		super.update(v);

		/* discard depth projection from tilt, depth buffer
		 * is used for clipping */
		mViewProj.copy(v.proj);
		mViewProj.setValue(10, 0);
		mViewProj.setValue(14, 0);
		mViewProj.multiplyRhs(v.view);

		mClipMode = 1;

		/* */
		int tileCnt = mDrawTiles.cnt + mProxyTileCnt;
		MapTile[] tiles = mDrawTiles.tiles;

		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.state != READY) {
				mClipMode = 2;
				break;
			}
		}

		/* draw visible tiles */
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.state == READY)
				drawTile(t, v);

		}

		/* draw parent or children as proxy for visibile tiles that dont
		 * have data yet. Proxies are clipped to the region where nothing
		 * was drawn to depth buffer.
		 * TODO draw proxies for placeholder */
		if (mClipMode > 1) {
			mClipMode = 3;
			//GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);
			GL.glDepthFunc(GL20.GL_LESS);

			/* draw child or parent proxies */
			boolean preferParent = (v.pos.getZoomScale() < 1.5)
			        || (v.pos.zoomLevel < tiles[0].zoomLevel);

			for (int i = 0; i < tileCnt; i++) {
				MapTile t = tiles[i];
				if (t.isVisible
				        && (t.state != READY)
				        && (t.holder == null)) {
					drawProxyTile(t, v, true, preferParent);
				}
			}

			/* draw grandparents */
			for (int i = 0; i < tileCnt; i++) {
				MapTile t = tiles[i];
				if (t.isVisible
				        && (t.state != READY)
				        && (t.holder == null))
					drawProxyTile(t, v, false, false);
			}
			GL.glDepthMask(false);
		}

		/* make sure stencil buffer write is disabled */
		GL.glStencilMask(0x00);

	}

	private void drawTile(MapTile tile, GLViewport v) {
		/* ensure to draw parents only once */
		if (tile.lastDraw == mDrawSerial)
			return;

		tile.lastDraw = mDrawSerial;

		/* use holder proxy when it is set */
		MapTile t = tile.holder == null ? tile : tile.holder;

		if (t.layers == null || t.layers.vbo == null)
			//throw new IllegalStateException(t + "no data " + (t.layers == null));
			return;

		t.layers.vbo.bind();
		MapPosition pos = v.pos;
		/* place tile relative to map position */
		int z = tile.zoomLevel;
		float div = FastMath.pow(z - pos.zoomLevel);
		double tileScale = Tile.SIZE * pos.scale;
		float x = (float) ((tile.x - pos.x) * tileScale);
		float y = (float) ((tile.y - pos.y) * tileScale);

		/* scale relative to zoom-level of this tile */
		float scale = (float) (pos.scale / (1 << z));

		v.mvp.setTransScale(x, y, scale / MapRenderer.COORD_SCALE);
		v.mvp.multiplyLhs(mViewProj);

		boolean clipped = false;
		int mode = mClipMode;
		RenderElement l = t.layers.getBaseLayers();

		while (l != null) {
			if (l.type == POLYGON) {
				l = PolygonLayer.Renderer.draw(l, v, div, !clipped, mode);
				clipped = true;
				continue;
			}
			if (!clipped) {
				/* draw stencil buffer clip region */
				PolygonLayer.Renderer.draw(null, v, div, true, mode);
				clipped = true;
			}
			if (l.type == LINE) {
				l = LineLayer.Renderer.draw(l, v, scale, t.layers);
				continue;
			}
			if (l.type == TEXLINE) {
				l = LineTexLayer.Renderer.draw(l, v, div, t.layers);
				continue;
			}
			if (l.type == MESH) {
				l = MeshLayer.Renderer.draw(l, v);
				continue;
			}
			/* just in case */
			l = l.next;
		}

		l = t.layers.getTextureLayers();
		while (l != null) {
			if (!clipped) {
				PolygonLayer.Renderer.draw(null, v, div, true, mode);
				clipped = true;
			}
			if (l.type == BITMAP) {
				l = BitmapLayer.Renderer.draw(l, v, 1, mRenderAlpha);
				continue;
			}
			l = l.next;
		}

		if (t.fadeTime == 0)
			t.fadeTime = getMinFade(t);

		if (debugOverdraw) {
			if (t.zoomLevel > pos.zoomLevel)
				PolygonLayer.Renderer.drawOver(v, Color.BLUE, 0.5f);
			else if (t.zoomLevel < pos.zoomLevel)
				PolygonLayer.Renderer.drawOver(v, Color.RED, 0.5f);
			else
				PolygonLayer.Renderer.drawOver(v, Color.GREEN, 0.5f);

			return;
		}

		if (mRenderOverdraw != 0 && MapRenderer.frametime - t.fadeTime < FADE_TIME) {
			float fade = 1 - (MapRenderer.frametime - t.fadeTime) / FADE_TIME;
			PolygonLayer.Renderer.drawOver(v, mRenderOverdraw, fade * fade);
			MapRenderer.animate();
		} else {
			PolygonLayer.Renderer.drawOver(v, 0, 1);
		}
	}

	private int drawProxyChild(MapTile tile, GLViewport v) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			if ((tile.proxies & 1 << i) == 0)
				continue;

			MapTile c = tile.node.child(i);

			if (c.state == READY) {
				drawTile(c, v);
				drawn++;
			}
		}
		return drawn;
	}

	protected void drawProxyTile(MapTile tile, GLViewport v,
	        boolean parent, boolean preferParent) {

		TileNode r = tile.node;
		MapTile proxy;

		if (!preferParent) {
			/* prefer drawing children */
			if (drawProxyChild(tile, v) == 4)
				return;

			if (parent) {
				/* draw parent proxy */
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy.state == READY) {
						//log.debug("1. draw parent " + proxy);
						drawTile(proxy, v);
					}
				}
			} else if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
				/* check if parent was already drawn */
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy.state == READY)
						return;
				}

				proxy = r.parent.parent.item;
				if (proxy.state == READY)
					drawTile(proxy, v);
			}
		} else {
			/* prefer drawing parent */
			if (parent) {
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy != null && proxy.state == READY) {
						//log.debug("2. draw parent " + proxy);
						drawTile(proxy, v);
						return;

					}
				}
				drawProxyChild(tile, v);

			} else if ((tile.proxies & MapTile.PROXY_GRAMPA) != 0) {
				/* check if parent was already drawn */
				if ((tile.proxies & MapTile.PROXY_PARENT) != 0) {
					proxy = r.parent.item;
					if (proxy.state == READY)
						return;
				}
				/* this will do nothing, just to check */
				if (drawProxyChild(tile, v) > 0)
					return;

				proxy = r.parent.parent.item;
				if (proxy.state == READY)
					drawTile(proxy, v);
			}
		}
	}
}
