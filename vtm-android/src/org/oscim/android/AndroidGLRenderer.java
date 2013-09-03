/*
 * Copyright 2013 Hannes Janetzek
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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.oscim.renderer.GLRenderer;
import org.oscim.view.Map;

import android.opengl.GLSurfaceView;

public class AndroidGLRenderer extends GLRenderer implements GLSurfaceView.Renderer{

	public AndroidGLRenderer(Map map) {
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
