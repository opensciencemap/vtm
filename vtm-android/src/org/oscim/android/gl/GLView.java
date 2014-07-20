/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.android.gl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Calendar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.oscim.backend.GLAdapter;
import org.oscim.map.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;

public class GLView extends GLSurfaceView {

	class GLRenderer extends org.oscim.renderer.MapRenderer implements GLSurfaceView.Renderer {

		public GLRenderer(Map map) {
			super(map);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			super.onSurfaceCreated();
		}

		int frame = 0;

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			super.onSurfaceChanged(width, height);

			width_surface = width;
			height_surface = height;

		}

		int width_surface, height_surface;
		public boolean printOptionEnable;
		File dir_image;

		@Override
		public void onDrawFrame(GL10 gl) {
			super.onDrawFrame();

			//if (frame++ == 100) {
			//	printOptionEnable = true;
			//	frame = 0;
			//}
			try {

				if (printOptionEnable) {
					printOptionEnable = false;
					Log.i("hari", "printOptionEnable if condition:" + printOptionEnable);
					int w = width_surface;
					int h = height_surface;

					Log.i("hari", "w:" + w + "-----h:" + h);

					int b[] = new int[(int) (w * h)];
					int bt[] = new int[(int) (w * h)];
					IntBuffer buffer = IntBuffer.wrap(b);
					buffer.position(0);
					GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
					for (int i = 0; i < h; i++) {
						//remember, that OpenGL bitmap is incompatible with Android bitmap
						//and so, some correction need.       
						for (int j = 0; j < w; j++) {
							int pix = b[i * w + j];
							int pb = (pix >> 16) & 0xff;
							int pr = (pix << 16) & 0x00ff0000;
							int pix1 = (pix & 0xff00ff00) | pr | pb;
							bt[(h - i - 1) * w + j] = pix1;
						}
					}
					Bitmap inBitmap = null;
					if (inBitmap == null || !inBitmap.isMutable()
					        || inBitmap.getWidth() != w || inBitmap.getHeight() != h) {
						inBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
					}
					//Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
					inBitmap.copyPixelsFromBuffer(buffer);
					//return inBitmap ;
					// return Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);
					inBitmap = Bitmap.createBitmap(bt, w, h, Bitmap.Config.ARGB_8888);

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					inBitmap.compress(CompressFormat.JPEG, 90, bos);
					byte[] bitmapdata = bos.toByteArray();
					ByteArrayInputStream fis = new ByteArrayInputStream(bitmapdata);

					final Calendar c = Calendar.getInstance();
					long mytimestamp = c.getTimeInMillis();
					String timeStamp = String.valueOf(mytimestamp);
					String myfile = "hari" + timeStamp + ".jpeg";

					dir_image = new File(Environment.getExternalStorageDirectory()
					        + File.separator
					        + "printerscreenshots"
					        + File.separator + "image");
					dir_image.mkdirs();

					try {
						File tmpFile = new File(dir_image, myfile);
						FileOutputStream fos = new FileOutputStream(tmpFile);

						byte[] buf = new byte[1024];
						int len;
						while ((len = fis.read(buf)) > 0) {
							fos.write(buf, 0, len);
						}
						fis.close();
						fos.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

					Log.v("hari", "screenshots:" + dir_image.toString());

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	public GLView(Context context, Map map) {
		super(context);
		setEGLConfigChooser(new GlConfigChooser());
		setEGLContextClientVersion(2);

		if (GLAdapter.debug)
			setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);

		setRenderer(new GLRenderer(map));

		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
}
