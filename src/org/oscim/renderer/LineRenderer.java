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
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import org.oscim.core.MapPosition;
import org.oscim.generator.TileGenerator;
import org.oscim.renderer.layer.Layer;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.GlUtils;

import android.graphics.Paint.Cap;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */

public final class LineRenderer {
	private final static String TAG = "LineRenderer";

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

		lineProgram[1] = GlUtils.createProgram(Shaders.lineVertexShader,
				Shaders.lineSimpleFragmentShader);
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
			hLineVertexPosition[i] = glGetAttribLocation(lineProgram[i], "a_position");
			hLineTexturePosition[i] = glGetAttribLocation(lineProgram[i], "a_st");
		}
		return true;
	}

	public static Layer draw(MapPosition pos, Layer layer, float[] matrix, float div,
			int mode, int bufferOffset) {

		int zoom = pos.zoomLevel;
		float scale = pos.scale;

		if (layer == null)
			return null;

		glUseProgram(lineProgram[mode]);

		int uLineScale = hLineScale[mode];
		int uLineMode = hLineMode[mode];
		int uLineColor = hLineColor[mode];
		int uLineWidth = hLineWidth[mode];

		GLRenderer.enableVertexArrays(hLineVertexPosition[mode], hLineTexturePosition[mode]);

		glVertexAttribPointer(hLineVertexPosition[mode], 2, GL_SHORT,
				false, 8, bufferOffset + LINE_VERTICES_DATA_POS_OFFSET);

		glVertexAttribPointer(hLineTexturePosition[mode], 2, GL_SHORT,
				false, 8, bufferOffset + LINE_VERTICES_DATA_TEX_OFFSET);

		glUniformMatrix4fv(hLineMatrix[mode], 1, false, matrix, 0);

		// scale factor to map one pixel on tile to one pixel on screen:
		// only works with orthographic projection
		float s = scale / div;
		float pixel = 0;

		if (mode == 1)
			pixel = 1.5f / s;

		glUniform1f(uLineScale, pixel);
		int lineMode = 0;
		glUniform1i(uLineMode, lineMode);

		// line scale factor (for non fixed lines)
		float lineScale = (float) Math.sqrt(s);
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

			GlUtils.setColor(uLineColor, line.color, alpha);

			if (blur && line.blur == 0) {
				glUniform1f(uLineScale, pixel);
				blur = false;
			}

			if (line.outline) {
				// draw outline for linelayers references by this outline
				for (LineLayer o = ll.outlines; o != null; o = o.outlines) {

					if (o.line.fixed || strokeMaxZoom) {
						width = (ll.width + o.width) / s;
					} else {
						width = ll.width / s + o.width / lineScale;
					}

					glUniform1f(uLineWidth, width);

					if (line.blur != 0) {
						blurScale = (ll.width + o.width) / s - (line.blur / s);
						glUniform1f(uLineScale, blurScale);
						blur = true;
					}

					if (o.line.cap == Cap.ROUND) {
						if (lineMode != 1) {
							lineMode = 1;
							glUniform1i(uLineMode, lineMode);
						}
					} else if (lineMode != 0) {
						lineMode = 0;
						glUniform1i(uLineMode, lineMode);
					}

					glDrawArrays(GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			} else {

				if (line.fixed || strokeMaxZoom) {
					// invert scaling of extrusion vectors so that line width
					// stays the same.
					width = ll.width / s;
				} else {
					width = ll.width / lineScale;
				}

				glUniform1f(uLineWidth, width);

				if (line.blur != 0) {
					blurScale = (ll.width / lineScale) * line.blur;
					glUniform1f(uLineScale, blurScale);
					blur = true;
				}

				if (line.cap == Cap.ROUND) {
					if (lineMode != 1) {
						lineMode = 1;
						glUniform1i(uLineMode, lineMode);
					}
				} else if (lineMode != 0) {
					lineMode = 0;
					glUniform1i(uLineMode, lineMode);
				}

				glDrawArrays(GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}

		return l;
	}
}
