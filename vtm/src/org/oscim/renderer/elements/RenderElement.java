/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.utils.pool.Inlist;

public abstract class RenderElement extends Inlist<RenderElement> {
	protected static GL20 GL;

	public static final int LINE = 0;
	public static final int TEXLINE = 1;
	public static final int POLYGON = 2;
	public static final int MESH = 3;
	public static final int EXTRUSION = 4;
	public static final int HAIRLINE = 5;
	public static final int SYMBOL = 6;
	public static final int BITMAP = 7;

	public final int type;

	/** drawing order from bottom to top. */
	int level;

	/** number of vertices for this layer. */
	protected int numVertices;

	/** temporary list of vertex data. */
	protected final VertexData vertexItems = new VertexData();

	protected RenderElement(int type) {
		this.type = type;
	}

	/** clear all resources. */
	protected void clear() {
		vertexItems.dispose();
		numVertices = 0;
	}

	/**
	 * Final preparation of content before compilation
	 * for stuff that should not be done on render-thread.
	 */
	protected void prepare() {

	}

	/** Compile vertex data to vbo. */
	protected void compile(ShortBuffer sbuf) {
		compileVertexItems(sbuf);
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	/**
	 * For line- and polygon-layers this is the offset
	 * of VERTICES in its layers.vbo.
	 * For all other types it is the byte offset in vbo.
	 */
	protected int offset;

	protected void compile(ShortBuffer vertexBuffer, ShortBuffer indexBuffer) {

	}

	protected void compileVertexItems(ShortBuffer sbuf) {
		/* keep offset of layer data in vbo */
		offset = sbuf.position() * 2;
		vertexItems.compile(sbuf);
	}

}
