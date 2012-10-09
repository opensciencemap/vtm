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
package org.oscim.renderer;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureObject {
	private static TextureObject pool;

	public static synchronized TextureObject get() {
		TextureObject to;

		if (pool == null) {
			init(10);
		}

		to = pool;
		pool = pool.next;
		to.next = null;
		return to;
	}

	public static synchronized void release(TextureObject to) {
		to.next = pool;
		pool = to;
	}

	public static void uploadTexture(TextureObject to, Bitmap bitmap,
			int format, int type, int w, int h) {

		if (to == null) {
			Log.d("...", "no fckn texture!");
			return;
		}
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, to.id);
		if (to.width == w && to.height == h)
			GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);
		else {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, format, bitmap, type, 0);
			to.width = w;
			to.height = h;
		}
	}

	static void init(int num) {
		TextureObject to;

		int[] textureIds = new int[num];
		GLES20.glGenTextures(num, textureIds, 0);

		for (int i = 1; i < num; i++) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i]);

			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
					GLES20.GL_CLAMP_TO_EDGE); // Set U Wrapping
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
					GLES20.GL_CLAMP_TO_EDGE); // Set V Wrapping

			to = new TextureObject(textureIds[i]);
			to.next = pool;
			pool = to;
		}
	}

	public TextureObject next;

	int id;
	int width;
	int height;

	// vertex offset from which this texture is referenced
	// or store texture id with vertex?
	int offset;

	TextureObject(int id) {
		this.id = id;
	}
}
