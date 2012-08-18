/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.glrenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.mapsforge.android.utils.GlUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextRenderer {
	private final static int TEXTURE_WIDTH = 512;
	private final static int TEXTURE_HEIGHT = 256;

	final static int MAX_LABELS = 30;

	private final Bitmap mBitmap;
	private final Canvas mCanvas;
	private int mFontPadX = 1;
	private int mFontPadY = 1;
	private int mBitmapFormat;
	private int mBitmapType;
	private ByteBuffer mByteBuffer;
	private ShortBuffer mShortBuffer;
	private TextTexture[] mTextures;

	private int mIndicesVBO;
	private int mVerticesVBO;

	final static int INDICES_PER_SPRITE = 6; // Indices Per Sprite
	final static int VERTICES_PER_SPRITE = 4; // Vertices Per Sprite
	final static int SHORTS_PER_VERTICE = 6;

	private static int mTextProgram;
	static int hTextUVPMatrix;
	static int hTextVertex;
	static int hTextScale;
	static int hTextTextureCoord;
	static int hTextUColor;

	static Paint mPaint = new Paint(Color.BLACK);

	boolean debug = false;
	short[] debugVertices = {

			0, 0,
			0, TEXTURE_HEIGHT,

			0, TEXTURE_HEIGHT - 1,
			0, 0,

			TEXTURE_WIDTH - 1, 0,
			TEXTURE_WIDTH, TEXTURE_HEIGHT,

			TEXTURE_WIDTH - 1, TEXTURE_HEIGHT - 1,
			TEXTURE_WIDTH, 0,

	};

	TextRenderer(int numTextures) {
		mBitmap = Bitmap
				.createBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT, Bitmap.Config.ARGB_8888);
		mCanvas = new Canvas(mBitmap);

		mBitmapFormat = GLUtils.getInternalFormat(mBitmap);
		mBitmapType = GLUtils.getType(mBitmap);

		mTextProgram = GlUtils.createProgram(textVertexShader, textFragmentShader);

		hTextUVPMatrix = GLES20.glGetUniformLocation(mTextProgram, "mvp");
		hTextUColor = GLES20.glGetUniformLocation(mTextProgram, "col");
		hTextVertex = GLES20.glGetAttribLocation(mTextProgram, "vertex");
		hTextScale = GLES20.glGetUniformLocation(mTextProgram, "scale");
		hTextTextureCoord = GLES20.glGetAttribLocation(mTextProgram, "tex_coord");

		// mVertexBuffer = new float[];
		int bufferSize = numTextures
				* MAX_LABELS * VERTICES_PER_SPRITE
				* SHORTS_PER_VERTICE * (Short.SIZE / 8);

		mByteBuffer = ByteBuffer.allocateDirect(bufferSize)
				.order(ByteOrder.nativeOrder());

		mShortBuffer = mByteBuffer.asShortBuffer();

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
			// GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
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
		}

		ShortBuffer tmpIndices = mByteBuffer.asShortBuffer();

		tmpIndices.put(indices, 0, len);
		tmpIndices.flip();

		int[] mVboIds = new int[2];
		GLES20.glGenBuffers(2, mVboIds, 0);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mVboIds[0]);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, len * (Short.SIZE / 8),
				tmpIndices,
				GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		mIndicesVBO = mVboIds[0];
		mVerticesVBO = mVboIds[1];
	}

	boolean drawToTexture(GLMapTile tile) {
		TextTexture tex = null;

		if (tile.labels.size() == 0)
			return false;

		for (int i = 0; i < mTextures.length; i++) {
			tex = mTextures[i];
			if (tex.tile == null)
				break;
			if (!tex.tile.isActive)
				break;

			tex = null;
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

		int max = tile.labels.size();
		if (max > MAX_LABELS)
			max = MAX_LABELS;

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

		for (int i = 0; i < max; i++) {
			TextItem t = tile.labels.get(i);

			height = (int) (t.caption.fontHeight) + 2 * mFontPadY;
			width = t.width + 2 * mFontPadX;

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = mFontPadX;
				y += advanceY;
				advanceY = (int) height;
			}

			float yy = y + (height - 1) - t.caption.fontDescent - mFontPadY;

			if (yy > TEXTURE_HEIGHT) {
				Log.d(TAG, "reached max labels");
				continue;
			}

			if (t.caption.stroke != null)
				mCanvas.drawText(t.text, x + t.width / 2, yy, t.caption.stroke);

			mCanvas.drawText(t.text, x + t.width / 2, yy, t.caption.paint);

			// Log.d(TAG, "draw: " + t.text + " at:" + (xx + t.width / 2) + " " + yy + " w:"
			// + t.width + " " + cellHeight);

			if (width > TEXTURE_WIDTH)
				width = TEXTURE_WIDTH;

			float halfWidth = width / 2.0f;
			float halfHeight = height / 2.0f;

			// short x1 = (short) (2.0f * (t.x - halfWidth));
			// short y1 = (short) (2.0f * (t.y - halfHeight));
			// short x2 = (short) (2.0f * (t.x + halfWidth));
			// short y2 = (short) (2.0f * (t.y + halfHeight));

			short x1 = (short) (2.0f * (-halfWidth));
			short y1 = (short) (2.0f * (-halfHeight));
			short x2 = (short) (2.0f * (halfWidth));
			short y2 = (short) (2.0f * (halfHeight));

			short u1 = (short) (2.0f * x);
			short v1 = (short) (2.0f * y);
			short u2 = (short) (2.0f * (x + width));
			short v2 = (short) (2.0f * (y + height));

			short tx = (short) (2.0f * t.x);
			short ty = (short) (2.0f * t.y);

			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x1;
			buf[pos++] = y1;
			buf[pos++] = u1;
			buf[pos++] = v2;

			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x2;
			buf[pos++] = y1;
			buf[pos++] = u2;
			buf[pos++] = v2;

			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x2;
			buf[pos++] = y2;
			buf[pos++] = u2;
			buf[pos++] = v1;

			buf[pos++] = tx;
			buf[pos++] = ty;
			buf[pos++] = x1;
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
		// GlUtils.checkGlError("0");
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex.id);
		// GlUtils.checkGlError("1");
		GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, mBitmap, mBitmapFormat,
				mBitmapType);
		// GlUtils.checkGlError("2");

		return true;
	}

	private static String TAG = "TextRenderer";

	void compileTextures() {
		int offset = 0;
		TextTexture tex;

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVerticesVBO);

		mShortBuffer.clear();

		for (int i = 0; i < mTextures.length; i++) {
			tex = mTextures[i];
			if (tex.tile == null || !tex.tile.isActive)
				continue;

			mShortBuffer.put(tex.vertices, 0, tex.length);
			tex.offset = offset;
			offset += tex.length;
		}

		mShortBuffer.flip();
		// Log.d(TAG, "compileTextures" + mFloatBuffer.remaining() + " " + offset);

		// TODO use sub-bufferdata function
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, offset * (Short.SIZE / 8),
				mShortBuffer, GLES20.GL_DYNAMIC_DRAW);
	}

	void beginDraw(float scale) {
		GLES20.glUseProgram(mTextProgram);

		GLES20.glEnableVertexAttribArray(hTextTextureCoord);
		GLES20.glEnableVertexAttribArray(hTextVertex);
		GLES20.glUniform1f(hTextScale, scale);
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

	void endDraw() {

		GLES20.glDisableVertexAttribArray(hTextTextureCoord);
		GLES20.glDisableVertexAttribArray(hTextVertex);
	}

	void drawTile(GLMapTile tile, float[] matrix) {

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tile.texture.id);

		GLES20.glUniformMatrix4fv(hTextUVPMatrix, 1, false, matrix, 0);

		if (debug) {
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		} else {

			GLES20.glVertexAttribPointer(hTextVertex, 4,
					GLES20.GL_SHORT, false, 12, tile.texture.offset * (Short.SIZE / 8));

			// GLES20.glVertexAttribPointer(hTextVertexOffset, 2,
			// GLES20.GL_SHORT, false, 8, tile.texture.offset * (Short.SIZE / 8) + 4);
			//
			GLES20.glVertexAttribPointer(hTextTextureCoord, 2,
					GLES20.GL_SHORT, false, 12, tile.texture.offset * (Short.SIZE / 8)
							+ 8);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES, (tile.texture.length / 24) *
					INDICES_PER_SPRITE, GLES20.GL_UNSIGNED_SHORT, 0);
		}

	}

	private static String textVertexShader = ""
			+ "precision highp float; "
			+ "attribute vec4 vertex;"
			// + "attribute vec4 offset;"
			+ "attribute vec2 tex_coord;"
			+ "uniform mat4 mvp;"
			+ "uniform float scale;"
			+ "varying vec2 tex_c;"
			+ "const vec2 div = vec2(1.0/1024.0,1.0/512.0);"
			+ "const vec4 dp = vec4(0.5,0.5,0.5,0.5);"
			+ "void main() {"
			+ "   vec4  s = dp * vertex;"
			+ "   gl_Position = mvp * vec4(s.x + (s.z / scale), s.y + (s.w / scale), 0.0, 1.0);"
			// + "   gl_Position = vec4(pos.x + s.z, pos.y + s.w, 0.0, 1.0);"
			+ "   tex_c = tex_coord * div;"
			+ "}";

	private static String textFragmentShader = ""
			+ "precision highp float;"
			+ "uniform sampler2D tex;"
			+ "uniform vec4 col;"
			+ "varying vec2 tex_c;"
			+ "void main() {"
			+ "   gl_FragColor = texture2D(tex, tex_c.xy);"
			+ "}";

}
