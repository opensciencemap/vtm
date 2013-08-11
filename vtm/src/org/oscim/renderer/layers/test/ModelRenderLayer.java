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


import org.oscim.core.MapPosition;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.RenderLayer;
public class ModelRenderLayer extends RenderLayer{

	@Override
	public void update(MapPosition pos, boolean changed, Matrices m) {
	}

	@Override
	public void compile() {
	}

	@Override
	public void render(MapPosition pos, Matrices m) {
	}



//	private final static String vertexShader = ""
//			+ "uniform mat4 uMVPMatrix;"
//			+ "uniform vec4 uLightPos;"
//			+ "attribute vec4 vPosition;"
//			+ "attribute vec4 vNormal;"
//			+ "attribute vec2 aTextureCoord;"
//			+ "varying vec2 vTextureCoord;"
//			+ "varying vec4 color;"
//			+
//
//			"void main() {"
//			+ "  vec3 light = normalize (uLightPos.xyz);"
//			+ "  vec3 normal = normalize (vNormal.xyz);"
//			+ "  vTextureCoord = aTextureCoord;"
//			+ "  color = vec4 (0.6, 0.8, 0.1, 1.0)*max(0.2, dot(normal, light));"
//			+ "  gl_Position = uMVPMatrix * vPosition;"
//			+ "}";
//
//	private final static String fragmentShader = ""
//			+ "precision mediump float;"
//			+ "varying vec4 color;"
//			+ "varying vec2 vTextureCoord;"
//			+ "uniform sampler2D sTexture;"
//			+ "void main() {"
//			+ "  gl_FragColor = color + texture2D(sTexture, vTextureCoord);"
//			+ "}";

}
