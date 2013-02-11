/*
 * Copyright 2012 OpenScienceMap
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

import static android.opengl.GLES20.GL_SHORT;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glVertexAttribPointer;

import org.oscim.core.MapPosition;
import org.oscim.generator.TileGenerator;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.GlUtils;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */

public final class LineRenderer {
	private final static String TAG = LineRenderer.class.getName();

	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;

	// shader handles
	private static int[] lineProgram = new int[2];
	private static int[] hLineVertexPosition = new int[2];
	private static int[] hLineColor = new int[2];
	private static int[] hLineMatrix = new int[2];
	private static int[] hLineScale = new int[2];
	private static int[] hLineWidth = new int[2];
	private static int[] hLineMode = new int[2];
	private static int mTexID;

	static boolean init() {
		lineProgram[0] = GlUtils.createProgram(lineVertexShader,
				lineFragmentShader);
		if (lineProgram[0] == 0) {
			Log.e(TAG, "Could not create line program.");
			return false;
		}

		lineProgram[1] = GlUtils.createProgram(lineVertexShader,
				lineSimpleFragmentShader);
		if (lineProgram[1] == 0) {
			Log.e(TAG, "Could not create simple line program.");
			return false;
		}

		for (int i = 0; i < 2; i++) {
			hLineMatrix[i] = glGetUniformLocation(lineProgram[i], "u_mvp");
			hLineScale[i] = glGetUniformLocation(lineProgram[i], "u_wscale");
			hLineWidth[i] = glGetUniformLocation(lineProgram[i], "u_width");
			hLineColor[i] = glGetUniformLocation(lineProgram[i], "u_color");
			hLineMode[i] = glGetUniformLocation(lineProgram[i], "u_mode");
			hLineVertexPosition[i] = glGetAttribLocation(lineProgram[i], "a_pos");
		}

		// create lookup table as texture for 'length(0..1,0..1)'
		// using mirrored wrap mode for 'length(-1..1,-1..1)'
		byte[] pixel = new byte[128 * 128];

		for (int x = 0; x < 128; x++) {
			float xx = x * x;
			for (int y = 0; y < 128; y++) {
				float yy = y * y;
				int color = (int) (Math.sqrt(xx + yy) * 2);
				if (color > 255)
					color = 255;
				pixel[x + y * 128] = (byte) color;
			}
		}

		mTexID = GlUtils.loadTexture(pixel, 128, 128, GLES20.GL_ALPHA,
				GLES20.GL_MIRRORED_REPEAT, GLES20.GL_MIRRORED_REPEAT);

		return true;
	}

	public static void beginLines() {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexID);
	}

	public static void endLines() {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
	}

	public static Layer draw(MapPosition pos, Layer layer, float[] matrix, float div,
			int mode, int bufferOffset) {

		int zoom = pos.zoomLevel;
		float scale = pos.scale;

		if (layer == null)
			return null;

		GLState.blend(true);

		GLState.useProgram(lineProgram[mode]);

		int uLineScale = hLineScale[mode];
		int uLineMode = hLineMode[mode];
		int uLineColor = hLineColor[mode];
		int uLineWidth = hLineWidth[mode];

		GLState.enableVertexArrays(hLineVertexPosition[mode], -1);

		glVertexAttribPointer(hLineVertexPosition[mode], 4, GL_SHORT,
				false, 0, bufferOffset + LINE_VERTICES_DATA_POS_OFFSET);

		glUniformMatrix4fv(hLineMatrix[mode], 1, false, matrix, 0);

		// line scale factor for non fixed lines: within a zoom-
		// level lines would be scaled by the factor 2 via projection.
		// though lines should only scale by sqrt(2). this is achieved
		// by inverting scaling of extrusion vector with: width/sqrt(s).
		// within one zoom-level: 1 <= s <= 2
		float s = scale / div;
		float lineScale = (float) Math.sqrt(s * 2 / 2.2);

		// scale factor to map one pixel on tile to one pixel on screen:
		// only works with orthographic projection
		float pixel = 0;

		if (mode == 1)
			pixel = 1.5f / s;

		glUniform1f(uLineScale, pixel);
		int lineMode = 0;
		glUniform1f(uLineMode, lineMode);

		boolean blur = false;
		// dont increase scale when max is reached
		boolean strokeMaxZoom = zoom > TileGenerator.STROKE_MAX_ZOOM_LEVEL;

		Layer l = layer;
		for (; l != null && l.type == Layer.LINE; l = l.next) {
			LineLayer ll = (LineLayer) l;
			Line line = ll.line;
			float width;

			if (line.fade < zoom) {
				glUniform4fv(uLineColor, 1, line.color, 0);
			} else if (line.fade > zoom) {
				continue;
			} else {
				float alpha = (scale > 1.2f ? scale : 1.2f) - 1f;
				GlUtils.setColor(uLineColor, line.color, alpha);
			}

			if (mode == 0 && blur && line.blur == 0) {
				glUniform1f(uLineScale, 0);
				blur = false;
			}

			if (line.outline) {
				// draw linelayers references by this outline
				for (LineLayer o = ll.outlines; o != null; o = o.outlines) {

					if (o.line.fixed || strokeMaxZoom) {
						width = (ll.width + o.width) / s;
					} else {
						width = ll.width / s + o.width / lineScale;

						// check min size for outline
						if (o.line.min > 0 && o.width * lineScale < o.line.min * 2)
							continue;
					}

					glUniform1f(uLineWidth, width);

					if (line.blur != 0) {
						glUniform1f(uLineScale, 1f - (line.blur / s));
						blur = true;
					} else if (mode == 1) {
						glUniform1f(uLineScale, pixel / width);
					}

					if (o.roundCap) {
						if (lineMode != 1) {
							lineMode = 1;
							glUniform1f(uLineMode, lineMode);
						}
					} else if (lineMode != 0) {
						lineMode = 0;
						glUniform1f(uLineMode, lineMode);
					}
					glDrawArrays(GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			} else {

				if (line.fixed || strokeMaxZoom) {
					// invert scaling of extrusion vectors so that line
					// width stays the same.
					width = ll.width / s;
				} else {
					// reduce linear scaling of extrusion vectors so that
					// line width increases by sqrt(2.2).
					width = ll.width / lineScale;

					if (ll.line.min > 0 && ll.width * lineScale < ll.line.min * 2)
						width = (ll.width - 0.2f) / lineScale;
				}

				glUniform1f(uLineWidth, width);

				if (line.blur != 0) {
					glUniform1f(uLineScale, line.blur);
					blur = true;
				} else if (mode == 1) {
					glUniform1f(uLineScale, pixel / width);
				}

				if (ll.roundCap) {
					if (lineMode != 1) {
						lineMode = 1;
						glUniform1f(uLineMode, lineMode);
					}
				} else if (lineMode != 0) {
					lineMode = 0;
					glUniform1f(uLineMode, lineMode);
				}

				glDrawArrays(GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}

		return l;
	}

	private final static String lineVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform float u_width;"
			+ "attribute vec4 a_pos;"
			+ "uniform float u_mode;"
			+ "varying vec2 v_st;"
			+ "varying vec2 v_mode;"
			+ "const float dscale = 8.0/2048.0;"
			+ "void main() {"
			// scale extrusion to u_width pixel
			// just ignore the two most insignificant bits of a_st :)
			+ "  vec2 dir = a_pos.zw;"
			+ "  gl_Position = u_mvp * vec4(a_pos.xy + (dscale * u_width * dir), 0.0, 1.0);"
			// last two bits of a_st hold the texture coordinates
			// ..maybe one could wrap texture so that `abs` is not required
			+ "  v_st = abs(mod(dir, 4.0)) - 1.0;"
			+ "  v_mode = vec2(1.0 - u_mode, u_mode);"
			+ "}";

	private final static String lineSimpleFragmentShader = ""
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ "uniform float u_width;"
			+ "uniform float u_wscale;"
			+ "uniform vec4 u_color;"
			+ "varying vec2 v_st;"
			+ "varying vec2 v_mode;"
			+ "void main() {"
			//+ "  float len;"
			// some say one should not use conditionals
			// (FIXME currently required as overlay line renderers dont load the texture)
			//+ "  if (u_mode == 0)"
			//+ "    len = abs(v_st.s);"
			//+ "  else"
			//+ "    len = texture2D(tex, v_st).a;"
			// one trick to avoid branching, need to check performance
			+ " float len = max(v_mode[0] * abs(v_st.s), v_mode[1] * texture2D(tex, v_st).a);"
			// interpolate alpha between: 0.0 < 1.0 - len < u_wscale
			// where wscale is 'filter width' / 'line width' and 0 <= len <= sqrt(2)
			+ "  gl_FragColor = u_color * smoothstep(0.0, u_wscale, 1.0 - len);"
			//+ "  gl_FragColor = u_color * min(1.0, (1.0 - len) / u_wscale);"
			+ "}";

	private final static String lineFragmentShader = ""
			+ "#extension GL_OES_standard_derivatives : enable\n"
			+ "precision mediump float;"
			+ "uniform sampler2D tex;"
			+ "uniform float u_mode;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_width;"
			+ "uniform float u_wscale;"
			+ "varying vec2 v_st;"
			+ "void main() {"
			+ "  float len;"
			+ "  float fuzz;"
			+ "  if (u_mode == 0.0){"
			+ "    len = abs(v_st.s);"
			+ "    fuzz = fwidth(v_st.s);"
			+ "  } else {"
			+ "    len = texture2D(tex, v_st).a;"
			//+ "  len = length(v_st);"
			+ "    vec2 st_width = fwidth(v_st);"
			+ "    fuzz = max(st_width.s, st_width.t);"
			+ "  }"
			//+ "  gl_FragColor = u_color * smoothstep(0.0, fuzz + u_wscale, 1.0 - len);"
			// smoothstep is too sharp, guess one could increase extrusion with z..
			// this looks ok:
			//+ "  gl_FragColor = u_color * min(1.0, (1.0 - len) / (u_wscale + fuzz));"
			// can be faster according to nvidia docs 'Optimize OpenGL ES 2.0 Performace'
			+ "  gl_FragColor = u_color * clamp((1.0 - len) / (u_wscale + fuzz), 0.0, 1.0);"
			+ "}";

	//	private final static String lineVertexShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform mat4 u_mvp;"
	//			+ "uniform float u_width;"
	//			+ "attribute vec4 a_pos;"
	//			+ "uniform int u_mode;"
	//			//+ "attribute vec2 a_st;"
	//			+ "varying vec2 v_st;"
	//			+ "const float dscale = 8.0/2048.0;"
	//			+ "void main() {"
	//			// scale extrusion to u_width pixel
	//			// just ignore the two most insignificant bits of a_st :)
	//			+ "  vec2 dir = a_pos.zw;"
	//			+ "  gl_Position = u_mvp * vec4(a_pos.xy + (dscale * u_width * dir), 0.0, 1.0);"
	//			// last two bits of a_st hold the texture coordinates
	//			+ "  v_st = u_width * (abs(mod(dir, 4.0)) - 1.0);"
	//			// use bit operations when available (gles 1.3)
	//			// + "  v_st = u_width * vec2(a_st.x & 3 - 1, a_st.y & 3 - 1);"
	//			+ "}";
	//
	//	private final static String lineSimpleFragmentShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform float u_wscale;"
	//			+ "uniform float u_width;"
	//			+ "uniform int u_mode;"
	//			+ "uniform vec4 u_color;"
	//			+ "varying vec2 v_st;"
	//			+ "void main() {"
	//			+ "  float len;"
	//			+ "  if (u_mode == 0)"
	//			+ "    len = abs(v_st.s);"
	//			+ "  else "
	//			+ "    len = length(v_st);"
	//			// fade to alpha. u_wscale is the width in pixel which should be
	//			// faded, u_width - len the position of this fragment on the
	//			// perpendicular to this line segment. this only works with no
	//			// perspective
	//			//+ "  gl_FragColor = min(1.0, (u_width - len) / u_wscale) * u_color;"
	//			+ "  gl_FragColor = u_color * smoothstep(0.0, u_wscale, (u_width - len));"
	//			+ "}";
	//
	//	private final static String lineFragmentShader = ""
	//			+ "#extension GL_OES_standard_derivatives : enable\n"
	//			+ "precision mediump float;"
	//			+ "uniform float u_wscale;"
	//			+ "uniform float u_width;"
	//			+ "uniform int u_mode;"
	//			+ "uniform vec4 u_color;"
	//			+ "varying vec2 v_st;"
	//			+ "void main() {"
	//			+ "  float len;"
	//			+ "  float fuzz;"
	//			+ "  if (u_mode == 0){"
	//			+ "    len = abs(v_st.s);"
	//			+ "    fuzz = u_wscale + fwidth(v_st.s);"
	//			+ "  } else {"
	//			+ "    len = length(v_st);"
	//			+ "    vec2 st_width = fwidth(v_st);"
	//			+ "    fuzz = u_wscale + max(st_width.s, st_width.t);"
	//			+ "  }"
	//			+ "  gl_FragColor = u_color * min(1.0, (u_width - len) / fuzz);"
	//			+ "}";
}
