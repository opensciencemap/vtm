/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.view.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.utils.GlUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.FloatMath;
import android.util.Log;

public class TextRenderer {
	private final static int TEXTURE_WIDTH = 512;
	private final static int TEXTURE_HEIGHT = 256;
	private final static float SCALE = 8.0f;
	private final static int LBIT_MASK = 0xfffffffe;
	// private final static int L2BIT_MASK = 0xfffffffc;

	final static int INDICES_PER_SPRITE = 6;
	final static int VERTICES_PER_SPRITE = 4;
	final static int SHORTS_PER_VERTICE = 6;
	final static int MAX_LABELS = 35;

	private static Bitmap mBitmap;
	private static Canvas mCanvas;
	private static int mFontPadX = 1;
	private static int mFontPadY = 1;
	private static int mBitmapFormat;
	private static int mBitmapType;
	private static ShortBuffer mShortBuffer;
	private static TextTexture[] mTextures;

	private static int mIndicesVBO;
	private static int mVerticesVBO;

	private static int mTextProgram;
	private static int hTextUVPMatrix;
	private static int hTextRotationMatrix;
	private static int hTextVertex;
	private static int hTextScale;
	private static int hTextTextureCoord;

	private static Paint mPaint = new Paint(Color.BLACK);

	private static boolean debug = false;
	private static short[] debugVertices = {

			0, 0,
			0, TEXTURE_HEIGHT * 4,

			0, TEXTURE_HEIGHT - 1,
			0, 0,

			TEXTURE_WIDTH - 1, 0,
			TEXTURE_WIDTH * 4, TEXTURE_HEIGHT * 4,

			TEXTURE_WIDTH - 1, TEXTURE_HEIGHT - 1,
			TEXTURE_WIDTH * 4, 0,

	};

	static void init() {
		mTextProgram = GlUtils.createProgram(Shaders.textVertexShader,
				Shaders.textFragmentShader);

		hTextUVPMatrix = GLES20.glGetUniformLocation(mTextProgram, "mvp");
		hTextRotationMatrix = GLES20.glGetUniformLocation(mTextProgram, "rotation");

		hTextVertex = GLES20.glGetAttribLocation(mTextProgram, "vertex");
		hTextScale = GLES20.glGetUniformLocation(mTextProgram, "scale");
		hTextTextureCoord = GLES20.glGetAttribLocation(mTextProgram, "tex_coord");

	}

	static boolean setup(int numTextures) {
		int bufferSize = numTextures
				* MAX_LABELS * VERTICES_PER_SPRITE
				* SHORTS_PER_VERTICE * (Short.SIZE / 8);

		// if (mBitmap == null) {
		mBitmap = Bitmap.createBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT,
				Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);

		mBitmapFormat = GLUtils.getInternalFormat(mBitmap);
		mBitmapType = GLUtils.getType(mBitmap);

		ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize)
				.order(ByteOrder.nativeOrder());

		mShortBuffer = buf.asShortBuffer();
		// }

		int[] textureIds = new int[numTextures];
		TextTexture[] textures = new TextTexture[numTextures];
		GLES20.glGenTextures(numTextures, textureIds, 0);

		for (int i = 0; i < numTextures; i++) {
			// setup filters for texture
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i]);

			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
					GLES20.GL_CLAMP_TO_EDGE); // Set U Wrapping
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
					GLES20.GL_CLAMP_TO_EDGE); // Set V Wrapping

			// load the generated bitmap onto the texture
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmapFormat, mBitmap,
					mBitmapType, 0);

			textures[i] = new TextTexture(textureIds[i]);
		}

		GlUtils.checkGlError("init textures");

		mTextures = textures;

		// Setup triangle indices
		short[] indices = new short[MAX_LABELS * INDICES_PER_SPRITE];
		int len = indices.length;
		short j = 0;
		for (int i = 0; i < len; i += INDICES_PER_SPRITE, j += VERTICES_PER_SPRITE) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);
			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 3);
			indices[i + 5] = (short) (j + 0);
			// indices[i + 0] = (short) (j + 0);
			// indices[i + 1] = (short) (j + 0);
			// indices[i + 2] = (short) (j + 1);
			// indices[i + 3] = (short) (j + 3);
			// indices[i + 4] = (short) (j + 2);
			// indices[i + 5] = (short) (j + 2);
		}

		mShortBuffer.clear();
		mShortBuffer.put(indices, 0, len);
		mShortBuffer.flip();

		int[] mVboIds = new int[2];
		GLES20.glGenBuffers(2, mVboIds, 0);
		mIndicesVBO = mVboIds[0];
		mVerticesVBO = mVboIds[1];

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesVBO);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, len * (Short.SIZE / 8),
				mShortBuffer, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		mShortBuffer.clear();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesVBO);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, bufferSize,
				mShortBuffer, GLES20.GL_DYNAMIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		return true;
	}

	static boolean drawToTexture(MapTile tile) {
		TextTexture tex = null;

		if (tile.labels == null)
			return false;

		for (int i = 0; i < mTextures.length; i++) {
			tex = mTextures[i];
			if (tex.tile == null)
				break;

			if (!tex.tile.isLocked)
				break;

			tex = null;
		}

		if (tex == null) {
			for (int i = 0; i < mTextures.length; i++) {
				tex = mTextures[i];
				if (!tex.tile.isVisible)
					break;

				tex = null;
			}
		}

		if (tex == null) {
			Log.d(TAG, "no textures left");
			return false;
		}
		if (tex.tile != null)
			tex.tile.texture = null;

		mBitmap.eraseColor(Color.TRANSPARENT);

		int pos = 0;
		short[] buf = tex.vertices;

		float y = 0;
		float x = mFontPadX;
		float width, height;

		int max = MAX_LABELS;

		if (debug) {
			mCanvas.drawLine(debugVertices[0], debugVertices[1], debugVertices[4],
					debugVertices[5], mPaint);
			mCanvas.drawLine(debugVertices[0], debugVertices[1], debugVertices[8],
					debugVertices[9], mPaint);

			mCanvas.drawLine(debugVertices[12], debugVertices[13], debugVertices[4],
					debugVertices[5], mPaint);
			mCanvas.drawLine(debugVertices[12], debugVertices[13], debugVertices[8],
					debugVertices[9], mPaint);
		}

		int advanceY = 0;

		TextItem t = tile.labels;
		float yy;
		short x1, x2, x3, x4, y1, y2, y3, y4;

		for (int i = 0; t != null && i < max; t = t.next, i++) {

			if (t.caption != null) {
				height = (int) (t.caption.fontHeight) + 2 * mFontPadY;
			} else {
				height = (int) (t.path.fontHeight) + 2 * mFontPadY;
			}

			width = t.width + 2 * mFontPadX;

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = mFontPadX;
				y += advanceY;
				advanceY = (int) height;
			}

			if (t.caption != null) {
				yy = y + (height - 1) - t.caption.fontDescent - mFontPadY;
			} else {
				yy = y + (height - 1) - t.path.fontDescent - mFontPadY;
			}

			if (yy > TEXTURE_HEIGHT) {
				Log.d(TAG, "reached max labels");
				continue;
			}

			if (t.caption != null) {
				if (t.caption.stroke != null)
					mCanvas.drawText(t.text, x + t.width / 2, yy, t.caption.stroke);

				mCanvas.drawText(t.text, x + t.width / 2, yy, t.caption.paint);
			} else {
				if (t.path.stroke != null)
					mCanvas.drawText(t.text, x + t.width / 2, yy, t.path.stroke);

				mCanvas.drawText(t.text, x + t.width / 2, yy, t.path.paint);
			}
			if (width > TEXTURE_WIDTH)
				width = TEXTURE_WIDTH;

			float hw = width / 2.0f;
			float hh = height / 2.0f;

			if (t.caption != null) {
				x1 = x3 = (short) (SCALE * (-hw));
				y1 = y3 = (short) (SCALE * (-hh));
				x2 = x4 = (short) (SCALE * (hw));
				y2 = y4 = (short) (SCALE * (hh));
			}
			else {
				float vx = t.x1 - t.x2;
				float vy = t.y1 - t.y2;
				float a = FloatMath.sqrt(vx * vx + vy * vy);
				vx = vx / a;
				vy = vy / a;

				float ux = -vy;
				float uy = vx;

				// int dx = (int) (vx * SCALE) & L2BIT_MASK;
				// int dy = (int) (vy * SCALE) & L2BIT_MASK;
				//
				// x1 = (short) dx;
				// y1 = (short) dy;
				//
				// x2 = (short) (dx | 1);
				// y3 = (short) (dy | 1);
				//
				// x4 = (short) (dx | 3);
				// y4 = (short) (dy | 3);
				//
				// x3 = (short) (dx | 2);
				// y2 = (short) (dy | 2);

				x1 = (short) (SCALE * (vx * hw + ux * hh));
				y1 = (short) (SCALE * (vy * hw + uy * hh));
				x2 = (short) (SCALE * (-vx * hw + ux * hh));
				y3 = (short) (SCALE * (-vy * hw + uy * hh));
				x4 = (short) (SCALE * (-vx * hw - ux * hh));
				y4 = (short) (SCALE * (-vy * hw - uy * hh));
				x3 = (short) (SCALE * (vx * hw - ux * hh));
				y2 = (short) (SCALE * (vy * hw - uy * hh));

			}
			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// pack caption/way-text info in lowest bit
			short tx;
			if (t.caption == null)
				tx = (short) ((int) (SCALE * t.x) & LBIT_MASK | 0);
			else
				tx = (short) ((int) (SCALE * t.x) & LBIT_MASK | 1);

			short ty = (short) (SCALE * t.y);

			// top-left
			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x1;
			buf[pos++] = y1;
			buf[pos++] = u1;
			buf[pos++] = v2;

			// top-right
			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x2;
			buf[pos++] = y3;
			buf[pos++] = u2;
			buf[pos++] = v2;

			// bot-right
			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x4;
			buf[pos++] = y4;
			buf[pos++] = u2;
			buf[pos++] = v1;

			// bot-left
			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x3;
			buf[pos++] = y2;
			buf[pos++] = u1;
			buf[pos++] = v1;

			x += width;

			if (y > TEXTURE_HEIGHT) {
				Log.d(TAG, "reached max labels: texture is full");
				break;
			}
		}

		tex.length = pos;
		tile.texture = tex;
		tex.tile = tile;

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.id);
		GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mBitmap,
				mBitmapFormat, mBitmapType);

		// FIXME shouldnt be needed here, still looking for sometimes corrupted labels..
		GLES20.glFlush();

		return true;
	}

	private static String TAG = "TextRenderer";

	static void compileTextures() {
		int offset = 0;
		TextTexture tex;

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesVBO);

		mShortBuffer.clear();

		for (int i = 0; i < mTextures.length; i++) {
			tex = mTextures[i];
			if (tex.tile == null || !tex.tile.isLocked)
				continue;

			mShortBuffer.put(tex.vertices, 0, tex.length);
			tex.offset = offset;
			offset += tex.length;
		}

		mShortBuffer.flip();

		GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, offset * (Short.SIZE / 8),
				mShortBuffer);
	}

	static void beginDraw(float scale, float[] rotation) {
		GLES20.glUseProgram(mTextProgram);

		GLES20.glEnableVertexAttribArray(hTextTextureCoord);
		GLES20.glEnableVertexAttribArray(hTextVertex);

		GLES20.glUniform1f(hTextScale, scale);
		GLES20.glUniformMatrix4fv(hTextRotationMatrix, 1, false, rotation, 0);

		if (debug) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			mShortBuffer.clear();
			mShortBuffer.put(debugVertices, 0, 16);
			mShortBuffer.flip();
			GLES20.glVertexAttribPointer(hTextVertex, 2,
					GLES20.GL_SHORT, false, 8, mShortBuffer);
			mShortBuffer.position(2);
			GLES20.glVertexAttribPointer(hTextTextureCoord, 2,
					GLES20.GL_SHORT, false, 8, mShortBuffer);
		} else {
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesVBO);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesVBO);
		}
	}

	static void endDraw() {

		GLES20.glDisableVertexAttribArray(hTextTextureCoord);
		GLES20.glDisableVertexAttribArray(hTextVertex);
	}

	static void drawTile(MapTile tile, float[] matrix) {

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.texture.id);

		GLES20.glUniformMatrix4fv(hTextUVPMatrix, 1, false, matrix, 0);

		if (debug) {
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		} else {

			GLES20.glVertexAttribPointer(hTextVertex, 4,
					GLES20.GL_SHORT, false, 12, tile.texture.offset * (Short.SIZE / 8));

			GLES20.glVertexAttribPointer(hTextTextureCoord, 2,
					GLES20.GL_SHORT, false, 12, tile.texture.offset * (Short.SIZE / 8)
							+ 8);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES, (tile.texture.length / 24) *
					INDICES_PER_SPRITE, GLES20.GL_UNSIGNED_SHORT, 0);
		}
	}
}
