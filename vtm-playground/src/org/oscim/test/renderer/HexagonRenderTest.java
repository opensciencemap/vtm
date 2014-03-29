package org.oscim.test.renderer;

import java.nio.FloatBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Color;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.GenericLayer;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.utils.FastMath;

public class HexagonRenderTest extends GdxMap {

	@Override
	protected void createLayers() {
		mMap.setMapPosition(0, 0, 1 << 4);
		mMap.layers().add(new GenericLayer(mMap, new HexagonRenderer()));
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new HexagonRenderTest(), null, 400);
	}

	/* This is an example how to integrate custom OpenGL drawing routines as map
	 * overlay
	 * 
	 * based on chapter 2 from:
	 * https://github.com/dalinaum/opengl-es-book-samples/tree/master/Android */

	static class HexagonRenderer extends ElementRenderer {

		private int mProgramObject;
		private int hVertexPosition;
		private int hMatrixPosition;
		private int hColorPosition;
		private int hCenterPosition;

		//private FloatBuffer mVertices;
		private boolean mInitialized;
		private BufferObject mVBO;

		int mZoom = -1;
		float mCellScale = 60 * MapRenderer.COORD_SCALE;

		@Override
		protected void update(GLViewport v) {
			if (!mInitialized) {
				if (!init()) {
					return;
				}
				mInitialized = true;

				compile();
				mMapPosition.copy(v.pos);
			}

			//if (mZoom != v.pos.zoomLevel) {
			//	mMapPosition.copy(v.pos);
			//	mZoom = v.pos.zoomLevel;
			//}
		}

		@Override
		protected void compile() {

			float[] vertices = new float[12];

			for (int i = 0; i < 6; i++) {
				vertices[i * 2 + 0] = (float) Math.cos(Math.PI * 2 * i / 6) * mCellScale;
				vertices[i * 2 + 1] = (float) Math.sin(Math.PI * 2 * i / 6) * mCellScale;
			}
			FloatBuffer buf = MapRenderer.getFloatBuffer(12);
			buf.put(vertices);

			mVBO = BufferObject.get(GL20.GL_ARRAY_BUFFER, 0);
			mVBO.loadBufferData(buf.flip(), 12 * 4);

			setReady(true);
		}

		@Override
		protected void render(GLViewport v) {

			// Use the program object
			GLState.useProgram(mProgramObject);

			GLState.blend(true);
			GLState.test(false, false);

			// bind VBO data
			mVBO.bind();

			// set VBO vertex layout
			GL.glVertexAttribPointer(hVertexPosition, 2, GL20.GL_FLOAT, false, 0, 0);

			GLState.enableVertexArrays(hVertexPosition, -1);

			/* apply view and projection matrices */
			// set mvp (tmp) matrix relative to mMapPosition
			// i.e. fixed on the map
			setMatrix(v);
			v.mvp.setAsUniform(hMatrixPosition);

			final int offset_x = 4;
			final int offset_y = 16;

			float h = (float) (Math.sqrt(3) / 2);
			for (int y = -offset_y; y < offset_y; y++) {
				for (int x = -offset_x; x < offset_x; x++) {
					float xx = x * 2 + (y % 2 == 0 ? 1 : 0);
					float yy = y * h + h / 2;

					GL.glUniform2f(hCenterPosition, xx * (mCellScale * 1.5f), yy * mCellScale);

					//float alpha = 1 + (float) Math.log10(FastMath.clamp(
					//		(float) Math.sqrt(xx * xx + yy * yy) / offset_y, 0.0f, 1.0f)) * 2;

					float alpha = (float) Math.sqrt(xx * xx + yy * yy) / offset_y;

					float fy = (float) (y + offset_y) / (offset_y * 2);
					float fx = (float) (x + offset_x) / (offset_x * 2);
					float fz = FastMath.clamp(
					                          (float) (x < 0 || y < 0 ? 1 - Math.sqrt(fx * fx + fy
					                                  * fy)
					                                  : 0),
					                          0,
					                          1);

					int c = 0xff << 24
					        | (int) (0xff * fy) << 16
					        | (int) (0xff * fx) << 8
					        | (int) (0xff * fz);

					GLUtils.setColor(hColorPosition, c, alpha);

					GL.glDrawArrays(GL20.GL_TRIANGLE_FAN, 0, 6);
				}
			}

			GLUtils.setColor(hColorPosition, Color.DKGRAY, 0.3f);

			for (int y = -offset_y; y < offset_y; y++) {
				for (int x = -offset_x; x < offset_x; x++) {
					float xx = x * 2 + (y % 2 == 0 ? 1 : 0);
					float yy = y * h + h / 2;

					GL.glUniform2f(hCenterPosition, xx * (mCellScale * 1.5f), yy * mCellScale);
					GL.glDrawArrays(GL20.GL_LINE_LOOP, 0, 6);
				}
			}

			GLUtils.checkGlError("...");
		}

		private boolean init() {
			// Load the vertex/fragment shaders
			int programObject = GLShader.createProgram(vShaderStr, fShaderStr);

			if (programObject == 0)
				return false;

			// Handle for vertex position in shader
			hVertexPosition = GL.glGetAttribLocation(programObject, "a_pos");

			hMatrixPosition = GL.glGetUniformLocation(programObject, "u_mvp");

			hColorPosition = GL.glGetUniformLocation(programObject, "u_color");

			hCenterPosition = GL.glGetUniformLocation(programObject, "u_center");

			// Store the program object
			mProgramObject = programObject;

			return true;
		}

		private final static String vShaderStr = ""
		        + "#ifdef GLES\n"
		        + "precision mediump float;\n"
		        + "#endif\n"
		        + "uniform mat4 u_mvp;"
		        + "uniform vec2 u_center;"
		        + "attribute vec2 a_pos;"
		        + "void main()"
		        + "{"
		        + "   gl_Position = u_mvp * vec4(u_center + a_pos, 0.0, 1.0);"
		        + "}";

		private final static String fShaderStr = ""
		        + "#ifdef GLES\n"
		        + "precision mediump float;\n"
		        + "#endif\n"
		        + "varying float alpha;"
		        + "uniform vec4 u_color;"
		        + "void main()"
		        + "{"
		        + "  gl_FragColor = u_color;"
		        + "}";

	}

}
