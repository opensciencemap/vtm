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
package org.oscim.renderer.layer;

import java.nio.ShortBuffer;

import org.oscim.renderer.TextureObject;

/**
 * @author Hannes Janetzek
 */
public abstract class TextureLayer extends Layer {
	// holds textures and offset in vbo
	public TextureObject textures;

	// scale mode
	public boolean fixed;

	/**
	 * @param sbuf
	 *            buffer to add vertices
	 */
	@Override
	protected void compile(ShortBuffer sbuf) {

		for (TextureObject to = textures; to != null; to = to.next)
			TextureObject.uploadTexture(to);

		// add vertices to vbo
		Layers.addPoolItems(this, sbuf);
	}

	abstract public boolean prepare();
}
