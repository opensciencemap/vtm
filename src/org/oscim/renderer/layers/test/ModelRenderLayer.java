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
package org.oscim.renderer.layers.test;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.view.MapView;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.RenderLayer;
public class ModelRenderLayer extends RenderLayer{

	public ModelRenderLayer(MapView mapView) {
		super(mapView);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void update(MapPosition pos, boolean changed, Matrices m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render(MapPosition pos, Matrices m) {
		// TODO Auto-generated method stub

	}

	// based on edu.spsu.logo.SimpleRenderer (c) Jeff Chastine
	private FloatBuffer vertexBuffer;				// A buffer to hold the geometry/vertices
    private FloatBuffer normalBuffer;				// A buffer to hold the normals of each vertex
    private FloatBuffer texCoordBuffer;				// A buffer to hold the texture coordinates for each vertex
    //private FloatBuffer lightBuffer;				// A buffer to hold the position of a light

	 private void initShapes() {

			float sin30 = (float)Math.sin(Math.PI/6.0);
			float cos30 = (float)Math.cos(Math.PI/6.0);

			float hexagonCoords[] = {
					0.0f, 0.0f, 0.0f,			// Hexagon face of SPSU logo
		            cos30, sin30, 0.0f,
		            0.0f, 1.0f, 0.0f,
		            -cos30, sin30, 0.0f,
		            -cos30, -sin30, 0.0f,
		            0.0f, -1.0f, 0.0f,
		            cos30, -sin30, 0.0f,
		            cos30, sin30, 0.0f
		    };

			float hexagonNormals[] = {
					 0.0f, 0.0f, 1.0f,			// Normals for each vertex
			         0.0f, 0.0f, 1.0f,
			         0.0f, 0.0f, 1.0f,
			         0.0f, 0.0f, 1.0f,
			         0.0f, 0.0f, 1.0f,
			         0.0f, 0.0f, 1.0f,
			         0.0f, 0.0f, 1.0f,
			         0.0f, 0.0f, 1.0f
			};

			float hexagonTexCoords[] = {
					0.5f, 0.5f,					// Texture coordinates for each vertex
		            -cos30/2.0f+0.5f, -sin30/2.0f+0.5f,
		            0.5f, 0.0f,
		            cos30/2.0f+0.5f, -sin30/2.0f+0.5f,
		            cos30/2.0f+0.5f, sin30/2.0f+0.5f,
		            0.5f, 1.0f,
		            -cos30/2.0f+0.5f, sin30/2.0f+0.5f,
		            -cos30/2.0f+0.5f, -sin30/2.0f+0.5f,
		    };

			// Load all of that info into 3 buffers
			ByteBuffer vbb = ByteBuffer.allocateDirect(hexagonCoords.length*4);
			vbb.order (ByteOrder.nativeOrder());
			vertexBuffer = vbb.asFloatBuffer();	// make a buffer from a buffer
			vertexBuffer.put(hexagonCoords);	// add the coords to the float buffer
			vertexBuffer.position(0);			// set the reading pointer back to 0

			ByteBuffer vbb2 = ByteBuffer.allocateDirect(hexagonNormals.length*4);
			vbb2.order (ByteOrder.nativeOrder());
			normalBuffer = vbb2.asFloatBuffer();	// make a buffer from a buffer
			normalBuffer.put(hexagonNormals);	// add the coords to the float buffer
			normalBuffer.position(0);			// set the reading pointer back to 0

			ByteBuffer vbb3 = ByteBuffer.allocateDirect(hexagonTexCoords.length*4);
			vbb3.order (ByteOrder.nativeOrder());
			texCoordBuffer = vbb3.asFloatBuffer();	// make a buffer from a buffer
			texCoordBuffer.put(hexagonTexCoords);	// add the coords to the float buffer
			texCoordBuffer.position(0);			// set the reading pointer back to 0

		}

	private final static String vertexShader = ""
			+ "uniform mat4 uMVPMatrix;"
			+ "uniform vec4 uLightPos;"
			+ "attribute vec4 vPosition;"
			+ "attribute vec4 vNormal;"
			+ "attribute vec2 aTextureCoord;"
			+ "varying vec2 vTextureCoord;"
			+ "varying vec4 color;"
			+

			"void main() {"
			//"  vec4 normal = vNormal*uMVPMatrix;" +
			+ "  vec3 light = normalize (uLightPos.xyz);"
			+ "  vec3 normal = normalize (vNormal.xyz);"
			+ "  vTextureCoord = aTextureCoord;"
			+ "  color = vec4 (0.6, 0.8, 0.1, 1.0)*max(0.2, dot(normal, light));"
			+ "  gl_Position = uMVPMatrix * vPosition;"
			+ "}";

	private final static String fragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "varying vec2 vTextureCoord;"
			+ "uniform sampler2D sTexture;"
			+ "void main() {"
			+ "  gl_FragColor = color + texture2D(sTexture, vTextureCoord);"
			+ "}";

}
