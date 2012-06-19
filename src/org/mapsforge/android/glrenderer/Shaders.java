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
	final static String gLineVertexShader = "" + "precision mediump float; \n" + "uniform mat4 u_center;"
			+ "uniform float u_width;" + "attribute vec4 a_position;" + "attribute vec2 a_st;" + "varying vec2 v_st;"
			+ "void main() {" + "  gl_Position = u_center * a_position;" + "  v_st = a_st;" + "}";

	final static String gLineFragmentShader = "" + "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;" + "uniform float u_width;" + "uniform int u_mode;" + "uniform vec4 u_color;"
			+ "const float zero = 0.0;" + "const int standard = 0;" + "const int fixed_width = 2;"
			+ "varying vec2 v_st;" + "void main() {" + "   gl_FragColor = u_color;" + "   float fuzz = fwidth(v_st.s);"
			+ "   float len = abs(v_st.s) - u_width;" + "   if (u_mode != fixed_width) {"
			+ "      if (v_st.t != zero){ " + "         fuzz = max(fuzz, fwidth(v_st.t));"
			+ "         len = length(v_st) - u_width;" + "      } " +
			// branching is not recommended...
			// "      if (- fuzz > len)  " +
			// "         gl_FragColor = u_color;" +
			// "       if (len < -fuzz)" +
			// "         discard;" +
			// "      else " +
			"      if (len > -fuzz)" + "         gl_FragColor *= smoothstep(fuzz , -fuzz , len);" + "    } else { " +
			// just guesswork.. looks ok for fixed line width >= 0.5
			"      if (len > -fuzz)" + "         gl_FragColor *= smoothstep(fuzz*0.5, -fuzz, len);" + "    }" + "}";

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

	final static String gLineFragmentShaderSimple = "" + "precision mediump float;" + "uniform vec4 u_color;"
			+ "uniform float u_width;" + "varying vec2 v_st;" + "void main() {" + "   vec4 color = u_color;"
			+ "   float len;" + "   if (v_st.t == 0.0) " + "     len = abs(v_st.s);" + "   else "
			+ "      len = length(v_st);" + "   if (len > 0.4) {"
			+ "   color = u_color * (smoothstep(0.2, 1.0, (u_width + 0.3) - len));" + "}" + "    gl_FragColor = color;"
			+ "}";

	final static String gPolygonVertexShader = "" + "precision mediump float; \n" + "uniform mat4 u_center;\n"
			+ "attribute vec4 a_position;" + "void main() {" + "  gl_Position = u_center * a_position;" + "}";

	final static String gPolygonFragmentShader = "" + "precision mediump float;" + "uniform vec4 u_color;"
			+ "void main() {" + "  gl_FragColor = u_color;" + "}";
}
