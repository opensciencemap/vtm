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
	        int border, int format, int type,
	        Buffer pixels) {
		Pixmap pixmap = Pixmap.pixmaps.get(((IntBuffer) pixels).get(0));
		if (pixmap != null) {
			// Gdx.app.log("GwtGL20", "load texture "+ target + " "+ width + " " + height + " " + type + " " + format);
			gl.texImage2D(target, level, internalformat, format, type, pixmap.getCanvasElement());
		} else if (format == GL20.GL_ALPHA) {
			// Gdx.app.log("GwtGL20", "load byte texture " + width + " " + height + " " + type + " " + format);
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
