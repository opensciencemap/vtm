/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.gdx.client;

import java.nio.Buffer;
import java.nio.IntBuffer;

import org.oscim.backend.GL20;

import com.badlogic.gdx.backends.gwt.GwtGL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.webgl.client.WebGLRenderingContext;

public class GwtGLAdapter extends GwtGL20 implements GL20 {

	protected final WebGLRenderingContext gl;

	public GwtGLAdapter(WebGLRenderingContext gl) {
		super(gl);
		gl.pixelStorei(WebGLRenderingContext.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 1);
		this.gl = gl;
	}

	@Override
	public void glGetShaderSource(int shader, int bufsize, Buffer length, String source) {

	}

	@Override
	public void glTexImage2D(int target, int level, int internalformat, int width, int height,
	        int border, int format, int type, Buffer pixels) {

		if (pixels == null) {
			gl.texImage2D(target, level, internalformat,
			              width, height, border, format,
			              type, null);
			return;
		}

		Pixmap pixmap = Pixmap.pixmaps.get(((IntBuffer) pixels).get(0));
		if (pixmap != null) {
			gl.texImage2D(target, level, internalformat, format, type, pixmap.getCanvasElement());
		} else if (format == GL20.GL_ALPHA) {
			int tmp[] = new int[(width * height) >> 2];
			((IntBuffer) pixels).get(tmp);

			Uint8ArrayNative v = com.google.gwt.typedarrays.client.Uint8ArrayNative.create(width
			        * height);

			for (int i = 0, n = (width * height) >> 2; i < n; i++) {
				v.set(i * 4 + 3, (tmp[i] >> 24) & 0xff);
				v.set(i * 4 + 2, (tmp[i] >> 16) & 0xff);
				v.set(i * 4 + 1, (tmp[i] >> 8) & 0xff);
				v.set(i * 4 + 0, (tmp[i]) & 0xff);
			}
			gl.texImage2D(target, level, internalformat, width, height, 0, format, type, v);
		}
	}
}
