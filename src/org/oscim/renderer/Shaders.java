/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.oscim.renderer;

public final class Shaders {

	// final static String lineVertexZigZagShader = ""
	// + "precision mediump float;"
	// + "uniform mat4 mvp;"
	// + "attribute vec4 a_pos1;"
	// + "attribute vec2 a_st1;"
	// + "attribute vec4 a_pos2;"
	// + "attribute vec2 a_st2;"
	// + "varying vec2 v_st;"
	// + "uniform vec2 u_mode;"
	// + "const float dscale = 1.0/1000.0;"
	// + "void main() {"
	// + "if (gl_VertexID & 1 == 0) {"
	// + "  vec2 dir = dscale * u_mode[1] * a_pos1.zw;"
	// + "  gl_Position = mvp * vec4(a_pos1.xy + dir, 0.0,1.0);"
	// + "  v_st = u_mode[1] * a_st1;"
	// + "} else {"
	// + "  vec2 dir = dscale * u_mode[1] * a_pos2.zw;"
	// + "  gl_Position = mvp * vec4( a_pos1.xy, dir, 0.0,1.0);"
	// + "  v_st = u_mode[1] * vec2(-a_st2.s , a_st2.t);"
	// + "}";

	// final static String lineVertexShader = ""
	// + "precision mediump float;"
	// + "uniform mat4 mvp;"
	// + "attribute vec4 a_position;"
	// + "attribute vec2 a_st;"
	// + "varying vec2 v_st;"
	// + "uniform float u_width;"
	// + "const float dscale = 8.0/1000.0;"
	// + "void main() {"
	// + "  vec2 dir = dscale * u_width * a_position.zw;"
	// + "  gl_Position = mvp * vec4(a_position.xy + dir, 0.0,1.0);"
	// + "  v_st = u_width * a_st;"
	// + "}";
	// final static String lineFragmentShader = ""
	// + "#extension GL_OES_standard_derivatives : enable\n"
	// + "precision mediump float;"
	// + "uniform float u_wscale;"
	// + "uniform float u_width;"
	// + "uniform int u_mode;"
	// + "uniform vec4 u_color;"
	// + "varying vec2 v_st;"
	// + "void main() {"
	// + "  gl_FragColor = u_color * 0.5;"
	// + "}";

	//	final static String buildingVertexShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform mat4 u_mvp;"
	//			+ "uniform vec4 u_color;"
	//			+ "uniform int u_mode;"
	//			+ "uniform float u_scale;"
	//			+ "attribute vec4 a_position;"
	//			+ "attribute float a_light;"
	//			+ "varying vec4 color;"
	//			+ "const float ff = 256.0;"
	//			+ "const float ffff = 65536.0;"
	//			+ "void main() {"
	//			+ "  gl_Position = u_mvp * vec4(a_position.xy, a_position.z/u_scale, 1.0);"
	//			+ "  if (u_mode == 0)"
	//			//     roof / depth pass
	//			+ "    color = u_color;"
	//			+ "  else if (u_mode == 1)"
	//			//     sides 1 - use 0xff00
	//			+ "    color = vec4(u_color.rgb * (a_light / ffff), 0.9);"
	//			+ "  else"
	//			//     sides 2 - use 0x00ff
	//			+ "    color = vec4(u_color.rgb * fract(a_light/ff), 0.9);"
	//			+ "}";

}
