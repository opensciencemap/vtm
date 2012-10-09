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

import org.oscim.core.MapPosition;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.GlUtils;

import android.graphics.Paint.Cap;
import android.opengl.GLES20;
import android.util.FloatMath;
import android.util.Log;

class LineRenderer {
	private final static String TAG = "LineRenderer";

	// private static int NUM_VERTEX_SHORTS = 4;

	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_TEX_OFFSET = 4;

	// shader handles
	private static int[] lineProgram = new int[2];
	private static int[] hLineVertexPosition = new int[2];
	private static int[] hLineTexturePosition = new int[2];
	private static int[] hLineColor = new int[2];
	private static int[] hLineMatrix = new int[2];
	private static int[] hLineScale = new int[2];
	private static int[] hLineWidth = new int[2];
	private static int[] hLineMode = new int[2];

	static boolean init() {
		lineProgram[0] = GlUtils.createProgram(Shaders.lineVertexShader,
				Shaders.lineFragmentShader);
		if (lineProgram[0] == 0) {
			Log.e(TAG, "Could not create line program.");
			return false;
		}

		hLineMatrix[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_mvp");
		hLineScale[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_wscale");
		hLineWidth[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_width");
		hLineColor[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_color");
		hLineMode[0] = GLES20.glGetUniformLocation(lineProgram[0], "u_mode");

		hLineVertexPosition[0] = GLES20.glGetAttribLocation(lineProgram[0], "a_position");
		hLineTexturePosition[0] = GLES20.glGetAttribLocation(lineProgram[0], "a_st");

		lineProgram[1] = GlUtils.createProgram(Shaders.lineVertexShader,
				Shaders.lineSimpleFragmentShader);
		if (lineProgram[1] == 0) {
			Log.e(TAG, "Could not create simple line program.");
			return false;
		}

		hLineMatrix[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_mvp");
		hLineScale[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_wscale");
		hLineWidth[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_width");
		hLineColor[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_color");
		hLineMode[1] = GLES20.glGetUniformLocation(lineProgram[1], "u_mode");

		hLineVertexPosition[1] = GLES20.glGetAttribLocation(lineProgram[1], "a_position");
		hLineTexturePosition[1] = GLES20.glGetAttribLocation(lineProgram[1], "a_st");

		return true;
	}

	static Layer draw(MapPosition pos, Layer layer, float[] matrix, float div,
			int mode, int bufferOffset) {

		int zoom = pos.zoomLevel;
		float scale = pos.scale;

		if (layer == null)
			return null;

		GLES20.glUseProgram(lineProgram[mode]);

		int va = hLineVertexPosition[mode];
		if (!GLRenderer.vertexArray[va]) {
			GLES20.glEnableVertexAttribArray(va);
			GLRenderer.vertexArray[va] = true;
		}

		va = hLineTexturePosition[mode];
		if (!GLRenderer.vertexArray[va]) {
			GLES20.glEnableVertexAttribArray(va);
			GLRenderer.vertexArray[va] = true;
		}

		// GLES20.glEnableVertexAttribArray(hLineVertexPosition[mode]);
		// GLES20.glEnableVertexAttribArray(hLineTexturePosition[mode]);

		GLES20.glVertexAttribPointer(hLineVertexPosition[mode], 2, GLES20.GL_SHORT,
				false, 8, bufferOffset + LINE_VERTICES_DATA_POS_OFFSET);

		GLES20.glVertexAttribPointer(hLineTexturePosition[mode], 2, GLES20.GL_SHORT,
				false, 8, bufferOffset + LINE_VERTICES_DATA_TEX_OFFSET);

		GLES20.glUniformMatrix4fv(hLineMatrix[mode], 1, false, matrix, 0);

		// scale factor to map one pixel on tile to one pixel on screen:
		// only works with orthographic projection
		float s = scale / div;
		float pixel = 0;

		if (mode == 1)
			pixel = 1.5f / s;

		GLES20.glUniform1f(hLineScale[mode], pixel);
		int lineMode = 0;
		GLES20.glUniform1i(hLineMode[mode], lineMode);

		// line scale factor (for non fixed lines)
		float lineScale = FloatMath.sqrt(s);
		float blurScale = pixel;
		boolean blur = false;
		// dont increase scale when max is reached
		boolean strokeMaxZoom = zoom > TileGenerator.STROKE_MAX_ZOOM_LEVEL;
		float width = 1;

		Layer l = layer;
		for (; l != null && l.type == Layer.LINE; l = l.next) {
			LineLayer ll = (LineLayer) l;
			Line line = ll.line;

			if (line.fade != -1 && line.fade > zoom)
				continue;

			float alpha = 1.0f;

			if (line.fade >= zoom)
				alpha = (scale > 1.2f ? scale : 1.2f) - alpha;

			GlUtils.setColor(hLineColor[mode], line.color, alpha);

			if (blur && line.blur == 0) {
				GLES20.glUniform1f(hLineScale[mode], pixel);
				blur = false;
			}

			if (line.outline) {
				for (LineLayer o = ll.outlines; o != null; o = o.outlines) {

					if (o.line.fixed || strokeMaxZoom) {
						width = (ll.width + o.width) / s;
					} else {
						width = ll.width / s + o.width / lineScale;
					}

					GLES20.glUniform1f(hLineWidth[mode], width);

					if (line.blur != 0) {
						blurScale = (ll.width + o.width) / s - (line.blur / s);
						GLES20.glUniform1f(hLineScale[mode], blurScale);
						blur = true;
					}

					if (line.cap == Cap.ROUND) {
						if (lineMode != 1) {
							lineMode = 1;
							GLES20.glUniform1i(hLineMode[mode], lineMode);
						}
					} else if (lineMode != 0) {
						lineMode = 0;
						GLES20.glUniform1i(hLineMode[mode], lineMode);
					}

					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			} else {

				if (line.fixed || strokeMaxZoom) {
					// invert scaling of extrusion vectors so that line width
					// stays the same.
					width = ll.width / s;
				} else {
					width = ll.width / lineScale;
				}

				GLES20.glUniform1f(hLineWidth[mode], width);

				if (line.blur != 0) {
					blurScale = (ll.width / lineScale) * line.blur;
					GLES20.glUniform1f(hLineScale[mode], blurScale);
					blur = true;
				}

				if (line.cap == Cap.ROUND) {
					if (lineMode != 1) {
						lineMode = 1;
						GLES20.glUniform1i(hLineMode[mode], lineMode);
					}
				} else if (lineMode != 0) {
					lineMode = 0;
					GLES20.glUniform1i(hLineMode[mode], lineMode);
				}

				GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}

		}

		// GLES20.glDisableVertexAttribArray(hLineVertexPosition[mode]);
		// GLES20.glDisableVertexAttribArray(hLineTexturePosition[mode]);

		return l;
	}
}
