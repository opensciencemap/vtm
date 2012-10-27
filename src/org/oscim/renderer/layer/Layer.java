/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.renderer.layer;

public abstract class Layer {
	public final static byte LINE = 0;
	public final static byte POLYGON = 1;
	public final static byte WAYTEXT = 2;
	public final static byte POITEXT = 3;
	public final static byte SYMBOL = 4;
	public final static byte BITMAP = 5;

	public byte type;

	public Layer next;

	int layer;
	// number of vertices this layer holds
	public int verticesCnt;
	// vertices offset of this layer in VBO
	public int offset;

	VertexPoolItem pool;
	protected VertexPoolItem curItem;

	abstract protected void clear();
}
