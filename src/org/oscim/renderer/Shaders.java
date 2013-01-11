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

	final static String lineVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform float u_width;"
			+ "attribute vec2 a_position;"
			+ "attribute vec2 a_st;"
			+ "varying vec2 v_st;"
			+ "const float dscale = 8.0/2048.0;"
			+ "void main() {"
			// scale extrusion to u_width pixel
			// just ignore the two most insignificant bits of a_st :)
			+ "  vec2 dir = dscale * u_width * a_st;"
			+ "  gl_Position = u_mvp * vec4(a_position + dir, 0.0,1.0);"
			// last two bits of a_st hold the texture coordinates
			+ "  v_st = u_width * (abs(mod(a_st,4.0)) - 1.0);"
			// use bit operations when available (gles 1.3)
			// + "  v_st = u_width * vec2(ivec2(a_st) & 3 - 1);"
			+ "}";

	final static String lineSimpleFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform float u_wscale;"
			+ "uniform float u_width;"
			+ "uniform int u_mode;"
			+ "uniform vec4 u_color;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len;"
			+ "  if (u_mode == 0)"
			+ "    len = u_width - abs(v_st.s);"
			+ "  else "
			+ "    len = u_width - length(v_st);"
			// fade to alpha. u_wscale is the width in pixel which should be
			// faded, u_width - len the position of this fragment on the
			// perpendicular to this line segment, only works with no
			// perspective
			+ "  gl_FragColor = u_color * min(1.0, len / u_wscale);"
			+ "}";

	final static String lineFragmentShader = ""
			+ "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;"
			+ "uniform float u_wscale;"
			+ "uniform float u_width;"
			+ "uniform int u_mode;"
			+ "uniform vec4 u_color;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len;"
			+ "  float fuzz;"
			+ "  if (u_mode == 0){"
			+ "    len = u_width - abs(v_st.s);"
			+ "    fuzz = u_wscale + fwidth(v_st.s);"
			+ "  } else {"
			+ "    len = u_width - length(v_st);"
			+ "    vec2 st_width = fwidth(v_st);"
			+ "    fuzz = u_wscale + max(st_width.s, st_width.t);"
			+ "  }"
			+ "  gl_FragColor = u_color * min(1.0, len / fuzz);"
			+ "}";

	final static String polygonVertexShader = ""
			+ "precision highp float;"
			+ "uniform mat4 u_mvp;"
			+ "attribute vec4 a_position;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_position;"
			+ "}";

	final static String polygonFragmentShader = ""
			+ "precision highp float;"
			+ "uniform vec4 u_color;"
			+ "void main() {"
			+ "  gl_FragColor = u_color;"
			+ "}";

	final static String polygonVertexShaderZ = ""
			+ "precision highp float;"
			+ "uniform mat4 u_mvp;"
			+ "attribute vec4 a_position;"
			+ "varying float z;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_position;"
			+ "  z = gl_Position.z;"
			+ "}";
	final static String polygonFragmentShaderZ = ""
			+ "precision highp float;"
			+ "uniform vec4 u_color;"
			+ "varying float z;"
			+ "void main() {"
			+ "if (z < 0.0)"
			+ "  gl_FragColor = vec4(z * -1.0, 0.0, 0.0, 1.0)*0.8;"
			+ "else if (z < 1.0)"
			+ "  gl_FragColor = vec4(0.0, 0.0, z, 1.0)*0.8;"
			+ "else"
			+ "  gl_FragColor = vec4(0.0, z - 1.0, 0.0, 1.0)*0.8;"
			+ "}";

	final static String textVertexShader = ""
			+ "precision highp float; "
			+ "attribute vec4 vertex;"
			+ "attribute vec2 tex_coord;"
			+ "uniform mat4 u_mv;"
			+ "uniform mat4 u_proj;"
			+ "uniform float u_scale;"
			+ "uniform float u_swidth;"
			+ "varying vec2 tex_c;"
			+ "const vec2 div = vec2(1.0/2048.0,1.0/2048.0);"
			+ "const float coord_scale = 0.125;"
			+ "void main() {"
			+ "  vec4 pos;"
			+ " if (mod(vertex.x, 2.0) == 0.0){"
			+ "       pos = u_proj * (u_mv * vec4(vertex.xy + vertex.zw * u_scale, 0.02, 1.0));"
			+ "  } else {"
			// // place as billboard
			+ "    vec4 dir = u_mv * vec4(vertex.xy, 0.0, 1.0);"
			+ "    pos = u_proj * (dir + vec4(vertex.zw * (coord_scale * u_swidth), 0.02, 0.0));"
			+ "  }"
			+ "  gl_Position = pos;"
			+ "  tex_c = tex_coord * div;"
			+ "}";

	final static String textFragmentShader = ""
			+ "precision highp float;"
			+ "uniform sampler2D tex;"
			+ "varying vec2 tex_c;"
			+ "void main() {"
			+ "   gl_FragColor = texture2D(tex, tex_c.xy);"
			+ "}";

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
