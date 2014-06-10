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

	public final static int LINE = 0;
	public final static int TEXLINE = 1;
	public final static int POLYGON = 2;
	public final static int MESH = 3;
	public final static int EXTRUSION = 4;
	public final static int SYMBOL = 5;
	public final static int BITMAP = 6;

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

	/** compile vertex data to vbo. */
	protected void compile(ShortBuffer sbuf) {

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
}
