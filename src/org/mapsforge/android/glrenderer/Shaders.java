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
	final static String gLineVertexShader = ""
			+ "precision highp float; \n"
			+ "uniform mat4 u_center;"
			+ "uniform float u_width;"
			+ "attribute vec4 a_position;"
			+ "attribute vec2 a_st;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  gl_Position = u_center * a_position;"
			+ "  v_st = a_st;" + "}";

	final static String gLineFragmentShader = ""
			+ "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;"
			+ "uniform float u_width;"
			+ "uniform int u_mode;"
			+ "uniform vec4 u_color;"
			+ "const float zero = 0.0;"
			+ "const int standard = 0;"
			+ "const int fixed_width = 2;"
			+ "const vec4 blank = vec4(1.0, 0.0, 0.0, 1.0);"
			+ "const vec4 blank2 = vec4(0.0, 1.0, 0.0, 1.0);"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "   if (u_mode != fixed_width) {"
			// + "       gl_FragColor = u_color;"
			// + "      float fuzz;"
			// + "      float len;"
			+ "      if (v_st.t == zero){ "
			// + "         fuzz = - sqrt(dFdx(v_st.s) * dFdx(v_st.s) + dFdy(v_st.s) * dFdy(v_st.s));"
			+ "         float fuzz = -fwidth(v_st.s) * 1.5;"
			+ "          float len = abs(v_st.s) - u_width;"
			// + "        if (len < fuzz)"
			+ "         gl_FragColor = u_color * smoothstep(zero, fuzz, len);"
			+ "      } else {"
			+ "         float fuzz = -max(fwidth(v_st.s), fwidth(v_st.t)) * 1.5;"
			+ "         float len = length(v_st) - u_width;"
			// + "        if (len < fuzz)"
			+ "         gl_FragColor = u_color * smoothstep(zero, fuzz, len);"
			+ "      } "
			// + "      if (len > zero)"
			// + "         gl_FragColor = blank;"
			// + "         discard;"
			// + "         gl_FragColor = u_color;"
			// + "     else if (len < fuzz)"
			// + "         gl_FragColor = blank2;"
			// + "      else "
			+ "    } else { "
			+ "      float fuzz = fwidth(v_st.s);"
			// + "      gl_FragColor = u_color * smoothstep(fuzz, zero, abs(v_st.s) - u_width + fuzz);"
			// + "         fuzz = - sqrt(dFdx(v_st.s) * dFdx(v_st.s) + dFdy(v_st.s) * dFdy(v_st.s)) * 1.5;"
			+ "         gl_FragColor = u_color * smoothstep(fuzz*0.5, -fuzz, abs(v_st.s) - u_width);"
			+ "    }"
			+ "}";

	// final static String gLineFragmentShader = "" +
	// "#extension GL_OES_standard_derivatives : enable\n" +
	// "precision mediump float;" +
	// "uniform float u_width;" +
	// "uniform int u_mode;" +
	// "uniform vec4 u_color;" +
	// "varying vec2 v_st;" +
	// "void main() {" +
	// "        gl_FragColor = u_color;" +
	// "}";

	final static String gLineFragmentShaderSimple = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_width;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "   vec4 color = u_color;"
			+ "   float len;"
			+ "   if (v_st.t == 0.0) "
			+ "     len = abs(v_st.s);"
			+ "   else "
			+ "      len = length(v_st);"
			+ "   if (len > 0.4) {"
			+ "   color = u_color * (smoothstep(0.2, 1.0, (u_width + 0.3) - len));"
			+ "}"
			+ "    gl_FragColor = color;"
			+ "}";

	final static String gPolygonVertexShader = ""
			+ "precision mediump float; \n"
			+ "uniform mat4 u_center;\n"
			+ "attribute vec4 a_position;"
			+ "void main() {"
			+ "  gl_Position = u_center * a_position;"
			+ "}";

	final static String gPolygonFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "void main() {"
			+ "  gl_FragColor = u_color;"
			+ "}";
}
