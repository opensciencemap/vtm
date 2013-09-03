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
package org.oscim.android;

import org.oscim.view.Map;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GLView extends GLSurfaceView {

	Map mMap;
	private final AndroidGLRenderer mRenderer;

	public GLView(Context context, Map map) {
		super(context);
		mMap = map;
		// Log.d(TAG, "init GLSurfaceLayer");
		setEGLConfigChooser(new GlConfigChooser());
		setEGLContextClientVersion(2);

		setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);
		mRenderer = new AndroidGLRenderer(mMap);
		setRenderer(mRenderer);

		//if (!MapView.debugFrameTime)
			setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
}
