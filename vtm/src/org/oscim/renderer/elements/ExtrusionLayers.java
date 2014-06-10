package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtrusionLayers extends TileData {
	static final Logger log = LoggerFactory.getLogger(ExtrusionLayers.class);

	public ExtrusionLayer layers;

	public boolean compiled;

	public final int zoomLevel;

	public final double x;
	public final double y;

	public BufferObject vboIndices;
	public BufferObject vboVertices;

	public ExtrusionLayers(MapTile tile) {
		zoomLevel = tile.zoomLevel;
		x = tile.x;
		y = tile.y;
	}

	/**
	 * Set new ExtrusionLayers and clear previous.
	 */
	public void setLayers(ExtrusionLayer el) {
		for (RenderElement l = layers; l != null; l = l.next)
			l.clear();

		layers = el;
	}

	public ExtrusionLayer getLayers() {
		return (ExtrusionLayer) layers;
	}

	@Override
	protected void dispose() {
		setLayers(null);

		if (compiled) {
			vboIndices = BufferObject.release(vboIndices);
			vboVertices = BufferObject.release(vboVertices);
		}

	}

	public boolean compileLayers() {

		if (layers == null)
			return false;

		int sumIndices = 0;
		int sumVertices = 0;

		for (ExtrusionLayer l = layers; l != null; l = l.next()) {
			sumIndices += l.sumIndices;
			sumVertices += l.sumVertices;
		}
		if (sumIndices == 0)
			return false;

		ShortBuffer vbuf = MapRenderer.getShortBuffer(sumVertices * 4);
		ShortBuffer ibuf = MapRenderer.getShortBuffer(sumIndices);

		for (ExtrusionLayer l = layers; l != null; l = l.next()) {
			l.compile(vbuf, ibuf);
		}
		int size = sumIndices * 2;
		if (ibuf.position() != sumIndices) {
			int pos = ibuf.position();
			log.error("invalid indice size: {} {}", sumIndices, pos);
			size = pos * 2;
		}
		vboIndices = BufferObject.get(GL20.GL_ELEMENT_ARRAY_BUFFER, size);
		vboIndices.loadBufferData(ibuf.flip(), size);
		vboIndices.unbind();

		size = sumVertices * 4 * 2;
		if (vbuf.position() != sumVertices * 4) {
			int pos = vbuf.position();
			log.error("invalid vertex size: {} {}", sumVertices, pos);
			size = pos * 2;
		}

		vboVertices = BufferObject.get(GL20.GL_ARRAY_BUFFER, size);
		vboVertices.loadBufferData(vbuf.flip(), size);
		vboVertices.unbind();

		compiled = true;

		return true;
	}

}
