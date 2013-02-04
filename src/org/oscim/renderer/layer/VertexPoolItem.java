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

public class VertexPoolItem {
	public final short[] vertices = new short[SIZE];

	public int used;
	public VertexPoolItem next;

	// must be multiple of
	// 4 (LineLayer/PolygonLayer),
	// 6 (TexLineLayer)
	// 24 (TextureLayer)
	public static final int SIZE = 360;
}
