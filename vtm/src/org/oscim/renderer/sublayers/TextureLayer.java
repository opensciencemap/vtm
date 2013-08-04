/*
 * Copyright 2012, 2013 Hannes Janetzek
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
package org.oscim.renderer.sublayers;

import java.nio.ShortBuffer;

public abstract class TextureLayer extends Layer {
	// holds textures and offset in vbo
	public TextureItem textures;

	// scale mode
	public boolean fixed;

	/**
	 * @param sbuf
	 *            buffer to add vertices
	 */
	@Override
	protected void compile(ShortBuffer sbuf) {

		for (TextureItem to = textures; to != null; to = to.next)
			to.upload();

		// add vertices to vbo
		Layers.addPoolItems(this, sbuf);
	}

	abstract public boolean prepare();

	static void putSprite(short buf[], int pos,
			short tx, short ty,
			short x1, short y1,
			short x2, short y2,
			short u1, short v1,
			short u2, short v2) {

		// top-left
		buf[pos + 0] = tx;
		buf[pos + 1] = ty;
		buf[pos + 2] = x1;
		buf[pos + 3] = y1;
		buf[pos + 4] = u1;
		buf[pos + 5] = v2;
		// bot-left
		buf[pos + 6] = tx;
		buf[pos + 7] = ty;
		buf[pos + 8] = x1;
		buf[pos + 9] = y2;
		buf[pos + 10] = u1;
		buf[pos + 11] = v1;
		// top-right
		buf[pos + 12] = tx;
		buf[pos + 13] = ty;
		buf[pos + 14] = x2;
		buf[pos + 15] = y1;
		buf[pos + 16] = u2;
		buf[pos + 17] = v2;
		// bot-right
		buf[pos + 18] = tx;
		buf[pos + 19] = ty;
		buf[pos + 20] = x2;
		buf[pos + 21] = y2;
		buf[pos + 22] = u2;
		buf[pos + 23] = v1;
	}
}
