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

package org.mapsforge.android.glrenderer;

class Shaders {

	final static String lineVertexShader = ""
			// + "#version 130\n"
			+ "precision mediump float;"
			+ "uniform mat4 mvp;"
			+ "attribute vec2 a_position;"
			+ "attribute vec2 a_st;"
			+ "varying vec2 v_st;"
			+ "uniform float u_width;"
			+ "const float dscale = 8.0/2048.0;"
			+ "void main() {"
			// scale extrusion to line u_width pixel
			// just ignore the two most insignificant bits of a_st :)
			+ "  vec2 dir = dscale * u_width * a_st;"
			+ "  gl_Position = mvp * vec4(a_position + dir, 0.0,1.0);"
			// last two bits of a_st hold the texture coordinates
			// TODO use bit operations when available!
			+ "  v_st = u_width * (abs(mod(a_st,4.0)) - 1.0);"
			// + "  v_st = u_width * vec2(ivec2(a_st) & 3 - 1);"
			+ "}";

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

	final static String lineFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform float u_wscale;"
			+ "uniform float u_width;"
			+ "uniform vec4 u_color;"
			+ "varying vec2 v_st;"
			+ "const float zero = 0.0;"
			+ "void main() {"
			+ "  float len;"
			// + "  if (v_st.t == zero)"
			// + "    len = abs(v_st.s);"
			// + "  else "
			+ "    len = length(v_st);"
			// fade to alpha. u_wscale is the width in pixel which should be faded,
			// u_width - len the position of this fragment on the perpendicular to the line
			+ "  gl_FragColor = smoothstep(zero, u_wscale, u_width - len) * u_color;"
			+ "}";

	// final static String lineFragmentShader = ""
	// + "#extension GL_OES_standard_derivatives : enable\n"
	// + "precision mediump float;\n"
	// + "uniform float u_wscale;"
	// + "uniform float u_width;"
	// + "uniform vec4 u_color;"
	// + "varying vec2 v_st;"
	// + "const float zero = 0.0;"
	// + "void main() {"
	// + "  vec4 color = u_color;"
	// + "  float width = u_width;"
	// + "  float len;"
	// + "  if (v_st.t == zero)"
	// + "    len = abs(v_st.s);"
	// + "  else "
	// + "    len = length(v_st);"
	// + "    vec2 st_width = fwidth(v_st);"
	// + "    float fuzz = max(st_width.s, st_width.t) * 1.5;"
	// + "    color.a *= smoothstep(zero, fuzz + u_wscale, u_width - len);"
	// + "  gl_FragColor = color;"
	// + "}";

	final static String polygonVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 mvp;"
			+ "attribute vec4 a_position;"
			+ "void main() {"
			+ "  gl_Position = mvp * a_position;"
			+ "}";

	final static String polygonFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "void main() {"
			+ "  gl_FragColor = u_color;"
			+ "}";

	final static String textVertexShader = ""
			+ "precision highp float; "
			+ "attribute vec4 vertex;"
			+ "attribute vec2 tex_coord;"
			+ "uniform mat4 mvp;"
			+ "uniform mat4 rotation;"
			+ "uniform float scale;"
			+ "varying vec2 tex_c;"
			+ "const vec2 div = vec2(1.0/4096.0,1.0/2048.0);"
			+ "void main() {"
			+ " if (mod(vertex.x, 2.0) == 0.0){"
			+ "       gl_Position = mvp * vec4(vertex.xy + vertex.zw / scale, 0.0, 1.0);"
			+ "  } else {"
			+ "    vec4 dir = rotation * vec4(vertex.zw / scale, 0.0, 1.0);"
			+ "    gl_Position = mvp * vec4(vertex.xy + dir.xy, 0.0, 1.0);"
			+ "  }"
			+ "  tex_c = tex_coord * div;"
			+ "}";

	// final static String textVertexShader = ""
	// + "precision highp float; "
	// + "attribute vec4 vertex;"
	// + "attribute vec2 tex_coord;"
	// + "uniform mat4 mvp;"
	// + "uniform mat4 rotation;"
	// + "uniform float scale;"
	// + "varying vec2 tex_c;"
	// + "const vec2 div = vec2(1.0/4096.0,1.0/2048.0);"
	// + "void main() {"
	// + " if (mod(vertex.x, 2.0) == 0.0){"
	// + "       gl_Position = mvp * vec4(vertex.xy + vertex.zw / scale, 0.0, 1.0);"
	// + "  } else {"
	// + "    vec4 dir = rotation * vec4(vertex.zw / scale, 0.0, 1.0);"
	// + "    gl_Position = mvp * vec4(vertex.xy + dir.xy, 0.0, 1.0);"
	// + "  }"
	// + "  tex_c = tex_coord * div;"
	// + "}";

	final static String textFragmentShader = ""
			+ "precision highp float;"
			+ "uniform sampler2D tex;"
			+ "uniform vec4 col;"
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

	// final static String lineFragmentShader = ""
	// + "#extension GL_OES_standard_derivatives : enable\n"
	// + "precision mediump float;"
	// + "uniform vec2 u_mode;"
	// + "uniform vec4 u_color;"
	// + "varying vec2 v_st;"
	// + "const float zero = 0.0;"
	// + "void main() {"
	// + "  vec4 color = u_color;"
	// + "  float width = u_mode[1];"
	// + "  float len;"
	// + "  if (v_st.t == zero)"
	// + "    len = abs(v_st.s);"
	// + "  else "
	// + "    len = length(v_st);"
	// + "  vec2 st_width = fwidth(v_st);"
	// + "  float fuzz = max(st_width.s, st_width.t);"
	// + "  color.a *= smoothstep(-pixel, fuzz*pixel, width - (len * width));"
	// + "  gl_FragColor = color;"
	// + "}";
}
