package org.oscim.renderer.bucket;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.MapTile.TileData;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtrusionBuckets extends TileData {
	static final Logger log = LoggerFactory.getLogger(ExtrusionBuckets.class);

	public ExtrusionBucket layers;

	public boolean compiled;

	public final int zoomLevel;

	public final double x;
	public final double y;

	public BufferObject ibo;
	public BufferObject vbo;

	public ExtrusionBuckets(MapTile tile) {
		zoomLevel = tile.zoomLevel;
		x = tile.x;
		y = tile.y;
	}

	/**
	 * Set new ExtrusionLayers and clear previous.
	 */
	public void setLayers(ExtrusionBucket el) {
		for (RenderBucket l = layers; l != null; l = l.next)
			l.clear();

		layers = el;
	}

	public ExtrusionBucket getLayers() {
		return layers;
	}

	@Override
	protected void dispose() {
		setLayers(null);

		if (compiled) {
			ibo = BufferObject.release(ibo);
			vbo = BufferObject.release(vbo);
		}

	}

	public void prepare() {
		for (RenderBucket l = layers; l != null; l = l.next)
			l.prepare();
	}

	public boolean compileLayers() {

		if (layers == null)
			return false;

		int sumIndices = 0;
		int sumVertices = 0;

		for (ExtrusionBucket l = layers; l != null; l = l.next()) {
			sumIndices += l.numIndices;
			sumVertices += l.numVertices;
		}
		if (sumIndices == 0)
			return false;

		ShortBuffer vboData = MapRenderer.getShortBuffer(sumVertices * 4);
		ShortBuffer iboData = MapRenderer.getShortBuffer(sumIndices);

		for (ExtrusionBucket l = layers; l != null; l = l.next()) {
			l.compile(vboData, iboData);
		}
		int size = sumIndices * 2;
		if (iboData.position() != sumIndices) {
			int pos = iboData.position();
			log.error("invalid indice size: {} {}", sumIndices, pos);
			size = pos * 2;
		}
		ibo = BufferObject.get(GL20.GL_ELEMENT_ARRAY_BUFFER, size);
		ibo.loadBufferData(iboData.flip(), size);
		ibo.unbind();

		size = sumVertices * 4 * 2;
		if (vboData.position() != sumVertices * 4) {
			int pos = vboData.position();
			log.error("invalid vertex size: {} {}", sumVertices, pos);
			size = pos * 2;
		}

		vbo = BufferObject.get(GL20.GL_ARRAY_BUFFER, size);
		vbo.loadBufferData(vboData.flip(), size);
		vbo.unbind();

		compiled = true;

		return true;
	}

}
