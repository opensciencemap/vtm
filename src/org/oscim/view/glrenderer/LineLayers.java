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
package org.oscim.view.glrenderer;

import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniform4fv;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

import java.nio.ShortBuffer;

import org.oscim.theme.renderinstruction.Line;
import org.oscim.view.utils.GlUtils;

import android.opengl.GLES20;
import android.util.FloatMath;

class LineLayers {
	private static int NUM_VERTEX_SHORTS = 4;

	private static final int LINE_VERTICES_DATA_POS_OFFSET = 0;
	private static final int LINE_VERTICES_DATA_TEX_OFFSET = 4;

	// shader handles
	private static int lineProgram;
	private static int hLineVertexPosition;
	private static int hLineTexturePosition;
	private static int hLineColor;
	private static int hLineMatrix;
	private static int hLineScale;
	private static int hLineWidth;

	static boolean init() {
		lineProgram = GlUtils.createProgram(Shaders.lineVertexShader,
				Shaders.lineFragmentShader);
		if (lineProgram == 0) {
			// Log.e(TAG, "Could not create line program.");
			return false;
		}

		hLineMatrix = glGetUniformLocation(lineProgram, "mvp");
		hLineScale = glGetUniformLocation(lineProgram, "u_wscale");
		hLineWidth = glGetUniformLocation(lineProgram, "u_width");
		hLineColor = glGetUniformLocation(lineProgram, "u_color");
		hLineVertexPosition = GLES20.glGetAttribLocation(lineProgram, "a_position");
		hLineTexturePosition = glGetAttribLocation(lineProgram, "a_st");

		return true;
	}

	static final boolean mFast = true;

	static LineLayer drawLines(MapTile tile, LineLayer layer, int next, float[] matrix,
			float div, double zoom, float scale) {

		float z = 1 / div;

		if (layer == null)
			return null;

		glUseProgram(lineProgram);

		glEnableVertexAttribArray(hLineVertexPosition);
		glEnableVertexAttribArray(hLineTexturePosition);

		glVertexAttribPointer(hLineVertexPosition, 2, GLES20.GL_SHORT,
				false, 8, tile.lineOffset + LINE_VERTICES_DATA_POS_OFFSET);

		glVertexAttribPointer(hLineTexturePosition, 2, GLES20.GL_SHORT,
				false, 8, tile.lineOffset + LINE_VERTICES_DATA_TEX_OFFSET);

		glUniformMatrix4fv(hLineMatrix, 1, false, matrix, 0);

		// scale factor to map one pixel on tile to one pixel on screen:
		float pixel = 2.0f / (scale * z);

		if (mFast)
			GLES20.glUniform1f(hLineScale, pixel);
		else
			GLES20.glUniform1f(hLineScale, 0);

		// line scale factor (for non fixed lines)
		float s = FloatMath.sqrt(scale * z);
		boolean blur = false;

		LineLayer l = layer;
		for (; l != null && l.layer < next; l = l.next) {
			Line line = l.line;
			if (line.fade != -1 && line.fade > zoom)
				continue;

			if (line.fade >= zoom) {
				float alpha = 1.0f;

				alpha = (scale > 1.2f ? scale : 1.2f) - alpha;
				if (alpha > 1.0f)
					alpha = 1.0f;
				glUniform4f(hLineColor,
						line.color[0] * alpha,
						line.color[1] * alpha,
						line.color[2] * alpha,
						alpha);
			} else {
				glUniform4fv(hLineColor, 1, line.color, 0);
			}

			if (blur) {
				if (mFast)
					GLES20.glUniform1f(hLineScale, pixel);
				else
					GLES20.glUniform1f(hLineScale, 0);
				blur = false;
			}

			if (l.isOutline) {
				for (LineLayer o = l.outlines; o != null; o = o.outlines) {

					if (line.blur != 0) {
						GLES20.glUniform1f(hLineScale, (l.width + o.width) / (scale * z)
								- (line.blur / (scale * z)));
						blur = true;
					}

					if (zoom > MapGenerator.STROKE_MAX_ZOOM_LEVEL)
						GLES20.glUniform1f(hLineWidth,
								(l.width + o.width) / (scale * z));
					else
						GLES20.glUniform1f(hLineWidth, l.width / (scale * z)
								+ o.width / s);

					glDrawArrays(GL_TRIANGLE_STRIP, o.offset, o.verticesCnt);
				}
			}
			else {
				if (line.blur != 0) {
					GLES20.glUniform1f(hLineScale, (l.width / s) * line.blur);
					blur = true;
				}

				if (line.fixed || zoom > MapGenerator.STROKE_MAX_ZOOM_LEVEL) {
					// invert scaling of extrusion vectors so that line width stays the same
					GLES20.glUniform1f(hLineWidth, (l.width / (scale * z)));
				} else {
					GLES20.glUniform1f(hLineWidth, (l.width / s));
				}

				glDrawArrays(GL_TRIANGLE_STRIP, l.offset, l.verticesCnt);
			}
		}

		glDisableVertexAttribArray(hLineVertexPosition);
		glDisableVertexAttribArray(hLineTexturePosition);

		return l;
	}

	static int sizeOf(LineLayer layers) {
		int size = 0;
		for (LineLayer l = layers; l != null; l = l.next)
			size += l.verticesCnt;

		size *= NUM_VERTEX_SHORTS;
		return size;
	}

	static void compileLayerData(LineLayer layers, ShortBuffer sbuf) {
		int pos = 0;
		ShortItem last = null, items = null;

		for (LineLayer l = layers; l != null; l = l.next) {
			if (l.isOutline)
				continue;

			for (ShortItem item = l.pool; item != null; item = item.next) {

				if (item.next == null) {
					sbuf.put(item.vertices, 0, item.used);
				} else {
					// item.used = ShortItem.SIZE;
					sbuf.put(item.vertices);
				}

				last = item;
			}

			l.offset = pos;
			pos += l.verticesCnt;

			if (last != null) {
				last.next = items;
				items = l.pool;
			}

			l.pool = null;
			l.curItem = null;
		}

		ShortPool.add(items);
	}

	// @SuppressLint("UseValueOf")
	// private static final Boolean lock = new Boolean(true);
	// private static final int POOL_LIMIT = 1500;
	//
	// static private LineLayer pool = null;
	// static private int count = 0;
	// static private int countAll = 0;
	//
	// static void finish() {
	// synchronized (lock) {
	// count = 0;
	// countAll = 0;
	// pool = null;
	// }
	// }
	//
	// static LineLayer get(int layer, Line line, float width, boolean outline) {
	// synchronized (lock) {
	//
	// if (count == 0 && pool == null) {
	// countAll++;
	// return new LineLayer(layer, line, width, outline);
	// }
	// if (count > 0) {
	// count--;
	// } else {
	// int c = 0;
	// LineLayer tmp = pool;
	//
	// while (tmp != null) {
	// c++;
	// tmp = tmp.next;
	// }
	//
	// Log.d("LineLayersl", "eek wrong count: " + c + " left");
	// }
	//
	// LineLayer it = pool;
	// pool = pool.next;
	// it.next = null;
	// it.layer = layer;
	// it.line = line;
	// it.isOutline = outline;
	// it.width = width;
	// return it;
	// }
	// }
	//
	// static void add(LineLayer layers) {
	// if (layers == null)
	// return;
	//
	// synchronized (lock) {
	//
	// // limit pool items
	// if (countAll < POOL_LIMIT) {
	// LineLayer last = layers;
	//
	// while (true) {
	// count++;
	//
	// if (last.next == null)
	// break;
	//
	// last = last.next;
	// }
	//
	// last.next = pool;
	// pool = layers;
	//
	// } else {
	// int cleared = 0;
	// LineLayer prev, tmp = layers;
	// while (tmp != null) {
	// prev = tmp;
	// tmp = tmp.next;
	//
	// countAll--;
	// cleared++;
	//
	// prev.next = null;
	//
	// }
	// Log.d("LineLayers", "sum: " + countAll + " free: " + count + " freed "
	// + cleared);
	// }
	//
	// }
	// }
	//
	static void clear(LineLayer layer) {
		for (LineLayer l = layer; l != null; l = l.next) {
			if (l.pool != null) {
				ShortPool.add(l.pool);
				l.pool = null;
				l.curItem = null;
			}
		}
		// LineLayers.add(layer);
	}
}
