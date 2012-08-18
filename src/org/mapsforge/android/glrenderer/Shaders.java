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
			+ "precision mediump float; \n"
			+ "uniform mat4 u_center;"
			+ "attribute vec4 a_position;"
			+ "attribute vec2 a_st;"
			+ "varying vec2 v_st;"
			+ "const vec4 scale = vec4(1.0/16.0, 1.0/16.0, 0.0, 1.0);"
			+ "void main() {"
			// + "  gl_Position = u_center * vec4(a_position.x, a_position.y, 0.0, 1.0);"
			// + "  v_st = a_position.zw;"
			+ "  gl_Position = u_center * (scale * a_position);"
			// + "  gl_Position = u_center * a_position;"
			+ "  v_st = a_st;"
			+ "}";

	final static String gLineFragmentShader = ""
			+ "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;"
			+ "uniform lowp vec2 u_mode;"
			+ "uniform vec4 u_color;"
			+ "const lowp float zero = 0.0;"
			+ "const float fuzzf = 1.8;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  lowp vec4 color = u_color;"
			+ "  lowp float len;"
			+ "  lowp float fuzz;"
			+ "  lowp float width = u_mode[1];"
			// + "  if (v_st.t == zero){ "
			// + "     fuzz = fwidth(v_st.s) * fuzzf;"
			// + "     len = width - abs(v_st.s);"
			// + "  } else {"
			+ "     fuzz = max(fwidth(v_st.s), fwidth(v_st.t)) * fuzzf;"
			+ "     len = width - length(v_st);"
			// + "  } "
			// + "  if (len < min_fuzz)"
			// + "    discard;"
			// + "    alpha = zero;"
			+ "  if (len < fuzz) {"
			+ "     lowp float min_fuzz = -fuzz * u_mode[0];"
			+ "     color.a *= smoothstep(min_fuzz, fuzz, len);"
			// + "     if (color.a == 0.0 ) color = vec4(1.0,0.0,0.0,1.0);"
			+ "  }"
			+ "  gl_FragColor = color;"
			+ "}";

	// final static String gLineFragmentShader = ""
	// + "#extension GL_OES_standard_derivatives : enable\n"
	// + "#pragma profilepragma blendoperation(gl_FragColor, GL_FUNC_ADD, GL_ONE, GL_ONE_MINUS_SRC_ALPHA)\n"
	// + "precision mediump float;"
	// + "uniform vec2 u_mode;"
	// + "uniform vec4 u_color;"
	// + "const float zero = 0.0;"
	// + "const vec4 blank = vec4(0.0,0.0,0.0,0.0);"
	// + "varying vec2 v_st;"
	// + "void main() {"
	// + "lowp color = u_color;"
	// + "lowp alpha = 1.0;"
	// + "float width = u_mode[1];"
	// + "      if (v_st.t == zero){ "
	// + "         float fuzz = fwidth(v_st.s) * 1.5;"
	// + "         float min_fuzz = -fuzz * u_mode[0];"
	// + "         float len = width - abs(v_st.s);"
	// // + "         if (len > fuzz)"
	// // + "          gl_FragColor = u_color;"
	// // + "         else if (len < min_fuzz)"
	// // + "          gl_FragColor = blank;"
	// // + "         else"
	// + "         gl_FragColor = u_color * smoothstep(min_fuzz, fuzz, len);"
	// + "      } else {"
	// + "         float fuzz = max(fwidth(v_st.s), fwidth(v_st.t)) * 1.5;"
	// + "         gl_FragColor = u_color * smoothstep(-fuzz * u_mode[0], fuzz, width - length(v_st));"
	// + "      } "
	// + "glFragColor = color"
	// + "}";

	// final static String gLineFragmentShader = ""
	// + "#extension GL_OES_standard_derivatives : enable\n"
	// + "precision mediump float;"
	// + "uniform float u_width;"
	// + "uniform vec2 u_mode;"
	// + "uniform vec4 u_color;"
	// + "const float zero = 0.0;"
	// // + "const vec4 blank = vec4(1.0, 0.0, 0.0, 1.0);"
	// + "varying vec2 v_st;"
	// + "void main() {"
	// + "float width = u_mode[1];"
	// // + "float alpha = 1.0;"
	// + "   if (u_mode[0] == zero) {"
	// // + "       gl_FragColor = u_color;"
	// // + "      float fuzz;"
	// // + "      float len;"
	// + "      if (v_st.t == zero){ "
	// // + "         fuzz = - sqrt(dFdx(v_st.s) * dFdx(v_st.s) + dFdy(v_st.s) * dFdy(v_st.s));"
	// + "         float fuzz = -fwidth(v_st.s) * 1.5;"
	// + "          float len = abs(v_st.s) - width;"
	// // + "        if (len < fuzz)"
	// + "         gl_FragColor = u_color * smoothstep(zero, fuzz, len);"
	// + "      } else {"
	// + "         float fuzz = -max(fwidth(v_st.s), fwidth(v_st.t)) * 1.5;"
	// + "         float len = length(v_st) - width;"
	// // + "        if (len < fuzz)"
	// + "         gl_FragColor = u_color * smoothstep(zero, fuzz, len);"
	// + "      } "
	// // + "      if (len > zero)"
	// // + "         gl_FragColor = blank;"
	// // + "         discard;"
	// // + "         gl_FragColor = u_color;"
	// // + "     else if (len < fuzz)"
	// // + "         gl_FragColor = blank2;"
	// // + "      else "
	// + "    } else { "
	// + "      float fuzz = fwidth(v_st.s);"
	// // + "      gl_FragColor = u_color * smoothstep(fuzz, zero, abs(v_st.s) - u_width + fuzz);"
	// // + "         fuzz = - sqrt(dFdx(v_st.s) * dFdx(v_st.s) + dFdy(v_st.s) * dFdy(v_st.s)) * 1.5;"
	// + "         gl_FragColor = u_color * smoothstep(fuzz*0.5, -fuzz, abs(v_st.s) - width);"
	// + "    }"
	// + "}";
	//
	// final static String gLineFragmentShader = "" +
	// "#extension GL_OES_standard_derivatives : enable\n" +
	// "precision mediump float;" +
	// "uniform int u_mode;" +
	// "uniform vec4 u_color;" +
	// "varying vec2 v_st;" +
	// "void main() {" +
	// "        gl_FragColor = u_color;" +
	// "}";

	final static String gLineFragmentShaderSimple = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "uniform vec2 u_mode;"
			+ "uniform float u_width;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "   gl_FragColor = u_color;"
			+ "   float width = u_width * u_mode[1];"
			+ "   float len;"
			+ "   if (v_st.t == 0.0) "
			+ "     len = abs(v_st.s);"
			+ "   else "
			+ "      len = length(v_st);"
			+ "   gl_FragColor.a = smoothstep(width, width - u_mode[1], width * len);"

			+ "}";

	final static String gPolygonVertexShader = ""
			+ "precision mediump float; \n"
			+ "uniform mat4 u_center;\n"
			+ "attribute vec4 a_position;"
			+ "const vec4 scale = vec4(1.0/16.0, 1.0/16.0, 0.0, 1.0);"
			+ "void main() {"
			+ "  gl_Position = u_center * (scale * a_position);"
			+ "}";

	final static String gPolygonFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform vec4 u_color;"
			+ "void main() {"
			+ "  gl_FragColor = u_color;"
			+ "}";
}
