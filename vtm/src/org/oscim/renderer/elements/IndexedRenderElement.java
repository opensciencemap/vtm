package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.MapRenderer;

public abstract class IndexedRenderElement extends RenderElement {
	BufferObject indicesVbo;
	int numIndices;

	VertexData indiceItems = new VertexData();

	protected IndexedRenderElement(int type) {
		super(type);
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
		if (numIndices <= 0) {
			indicesVbo = BufferObject.release(indicesVbo);
			return;
		}

		/* add vertices to shared VBO */
		compileVertexItems(sbuf);

		/* add indices to indicesVbo */
		ShortBuffer ibuf = MapRenderer.getShortBuffer(numIndices);
		indiceItems.compile(ibuf);

		if (indicesVbo == null)
			indicesVbo = BufferObject.get(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		indicesVbo.loadBufferData(ibuf.flip(), ibuf.limit() * 2);
	}

	@Override
	protected void clear() {
		indicesVbo = BufferObject.release(indicesVbo);
		vertexItems.dispose();
		indiceItems.dispose();
		numIndices = 0;
		numVertices = 0;
	}

}
