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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.oscim.backend.GLAdapter;
import org.oscim.map.Map;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GLView extends GLSurfaceView {

	class GLRenderer extends org.oscim.renderer.MapRenderer implements GLSurfaceView.Renderer {

		public GLRenderer(Map map) {
			super(map);
		}

		@Override
		public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			super.onSurfaceCreated();
		}

		@Override
		public void onSurfaceChanged(GL10 gl, int width, int height) {
			super.onSurfaceChanged(width, height);

		}

		@Override
		public void onDrawFrame(GL10 gl) {
			super.onDrawFrame();
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
