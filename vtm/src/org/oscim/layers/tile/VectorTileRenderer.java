package org.oscim.layers.tile;

import static org.oscim.layers.tile.MapTile.PROXY_GRAMPA;
import static org.oscim.layers.tile.MapTile.PROXY_PARENT;
import static org.oscim.layers.tile.MapTile.State.READY;
import static org.oscim.renderer.elements.RenderElement.BITMAP;
import static org.oscim.renderer.elements.RenderElement.HAIRLINE;
import static org.oscim.renderer.elements.RenderElement.LINE;
import static org.oscim.renderer.elements.RenderElement.MESH;
import static org.oscim.renderer.elements.RenderElement.POLYGON;
import static org.oscim.renderer.elements.RenderElement.TEXLINE;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Color;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.HairLineLayer;
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

	/**
	 * Current number of frames drawn, used to not draw a
	 * tile twice per frame.
	 */
	protected int mDrawSerial;

	@Override
	protected synchronized void update(GLViewport v) {
		super.update(v);

		/* discard depth projection from tilt, depth buffer
		 * is used for clipping */
		mViewProj.copy(v.proj);
		mViewProj.setValue(10, 0);
		mViewProj.setValue(14, 0);
		mViewProj.multiplyRhs(v.view);

		mClipMode = PolygonLayer.CLIP_STENCIL;

		int tileCnt = mDrawTiles.cnt + mProxyTileCnt;

		MapTile[] tiles = mDrawTiles.tiles;

		boolean drawProxies = false;

		mDrawSerial++;

		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.state != READY) {
				GL.glDepthMask(true);
				GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

				/* always write depth for non-proxy tiles */
				GL.glDepthFunc(GL20.GL_ALWAYS);

				mClipMode = PolygonLayer.CLIP_DEPTH;
				drawProxies = true;

				break;
			}
		}

		/* draw visible tiles */
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if (t.isVisible && t.state == READY)
				drawTile(t, v, 0);
		}

		/* draw parent or children as proxy for visibile tiles that dont
		 * have data yet. Proxies are clipped to the region where nothing
		 * was drawn to depth buffer.
		 * TODO draw proxies for placeholder */
		if (!drawProxies)
			return;

		/* only draw where no other tile is drawn */
		GL.glDepthFunc(GL20.GL_LESS);

		/* draw child or parent proxies */
		boolean preferParent = (v.pos.getZoomScale() < 1.5)
		        || (v.pos.zoomLevel < tiles[0].zoomLevel);

		if (preferParent) {
			for (int i = 0; i < tileCnt; i++) {
				MapTile t = tiles[i];
				if ((!t.isVisible) || (t.lastDraw == mDrawSerial))
					continue;
				if (!drawParent(t, v))
					drawChildren(t, v);
			}
		} else {
			for (int i = 0; i < tileCnt; i++) {
				MapTile t = tiles[i];
				if ((!t.isVisible) || (t.lastDraw == mDrawSerial))
					continue;
				drawChildren(t, v);
			}
			for (int i = 0; i < tileCnt; i++) {
				MapTile t = tiles[i];
				if ((!t.isVisible) || (t.lastDraw == mDrawSerial))
					continue;
				drawParent(t, v);
			}
		}

		/* draw grandparents */
		for (int i = 0; i < tileCnt; i++) {
			MapTile t = tiles[i];
			if ((!t.isVisible) || (t.lastDraw == mDrawSerial))
				continue;
			drawGrandParent(t, v);
		}
		GL.glDepthMask(false);

		/* make sure stencil buffer write is disabled */
		//GL.glStencilMask(0x00);
	}

	private void drawTile(MapTile tile, GLViewport v, int proxyLevel) {

		/* ensure to draw parents only once */
		if (tile.lastDraw == mDrawSerial)
			return;

		tile.lastDraw = mDrawSerial;

		/* use holder proxy when it is set */
		ElementLayers layers = (tile.holder == null)
		        ? tile.getLayers()
		        : tile.holder.getLayers();

		if (layers == null || layers.vbo == null)
			return;

		layers.vbo.bind();
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

		RenderElement l = layers.getBaseLayers();

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
				l = LineLayer.Renderer.draw(l, v, scale, layers);
				continue;
			}
			if (l.type == TEXLINE) {
				l = LineTexLayer.Renderer.draw(l, v, div, layers);
				continue;
			}
			if (l.type == MESH) {
				l = MeshLayer.Renderer.draw(l, v);
				continue;
			}
			if (l.type == HAIRLINE) {
				l = HairLineLayer.Renderer.draw(l, v);
				continue;
			}

			/* just in case */
			log.error("unknown layer {}", l.type);
			l = l.next;
		}

		l = layers.getTextureLayers();
		while (l != null) {
			if (!clipped) {
				PolygonLayer.Renderer.draw(null, v, div, true, mode);
				clipped = true;
			}
			if (l.type == BITMAP) {
				l = BitmapLayer.Renderer.draw(l, v, 1, mLayerAlpha);
				continue;
			}
			log.error("unknown layer {}", l.type);
			l = l.next;
		}

		if (debugOverdraw) {
			if (tile.zoomLevel > pos.zoomLevel)
				PolygonLayer.Renderer.drawOver(v, Color.BLUE, 0.5f);
			else if (tile.zoomLevel < pos.zoomLevel)
				PolygonLayer.Renderer.drawOver(v, Color.RED, 0.5f);
			else
				PolygonLayer.Renderer.drawOver(v, Color.GREEN, 0.5f);

			return;
		}

		if (tile.fadeTime == 0) {
			/* need to use original tile to get the fade */
			MapTile t = (tile.holder == null) ? tile : tile.holder;
			tile.fadeTime = getMinFade(t, proxyLevel);
		}

		long dTime = MapRenderer.frametime - tile.fadeTime;

		if (mOverdrawColor == 0 || dTime > FADE_TIME) {
			PolygonLayer.Renderer.drawOver(v, 0, 1);
			return;
		}

		float fade = 1 - dTime / FADE_TIME;
		PolygonLayer.Renderer.drawOver(v, mOverdrawColor, fade * fade);

		MapRenderer.animate();
	}

	protected boolean drawChildren(MapTile t, GLViewport v) {
		int drawn = 0;
		for (int i = 0; i < 4; i++) {
			MapTile c = t.getProxyChild(i, READY);
			if (c == null)
				continue;

			drawTile(c, v, 1);
			drawn++;
		}
		if (drawn == 4) {
			t.lastDraw = mDrawSerial;
			return true;
		}
		return false;
	}

	protected boolean drawParent(MapTile t, GLViewport v) {
		MapTile proxy = t.getProxy(PROXY_PARENT, READY);
		if (proxy != null) {
			drawTile(proxy, v, -1);
			t.lastDraw = mDrawSerial;
			return true;
		}
		return false;
	}

	protected void drawGrandParent(MapTile t, GLViewport v) {
		MapTile proxy = t.getProxy(PROXY_GRAMPA, READY);
		if (proxy != null) {
			drawTile(proxy, v, -2);
			t.lastDraw = mDrawSerial;
		}
	}
}
