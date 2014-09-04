/*
 * Copyright 2012, 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.canvas.Color;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.RenderBuckets;
import org.oscim.renderer.bucket.TextureBucket;
import org.oscim.renderer.bucket.TextureItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapRenderer {
	static final Logger log = LoggerFactory.getLogger(MapRenderer.class);

	static GL20 GL;

	/** scale factor used for short vertices */
	public static final float COORD_SCALE = 8.0f;

	private final Map mMap;
	private final GLViewport mViewport;

	private static float[] mClearColor;

	private static int mQuadIndicesID;
	private static int mQuadVerticesID;
	public final static int maxQuads = 512;

	public static long frametime;
	private static boolean rerender;

	private static NativeBufferPool mBufferPool;

	public MapRenderer(Map map) {
		mMap = map;
		mViewport = new GLViewport();
		mBufferPool = new NativeBufferPool();

		/* FIXME should be done in 'destroy' method
		 * clear all previous vbo refs */
		BufferObject.clear();
		setBackgroundColor(Color.DKGRAY);
	}

	public static void setBackgroundColor(int color) {
		mClearColor = GLUtils.colorToFloat(color);
	}

	public void onDrawFrame() {
		frametime = System.currentTimeMillis();
		draw();

		mBufferPool.releaseBuffers();
		TextureItem.disposeTextures();
	}

	private void draw() {
		GLState.setClearColor(mClearColor);

		GL.glDepthMask(true);
		GL.glStencilMask(0xFF);

		GL.glClear(GL20.GL_COLOR_BUFFER_BIT
		        | GL20.GL_DEPTH_BUFFER_BIT
		        | GL20.GL_STENCIL_BUFFER_BIT);

		GL.glDepthMask(false);
		GL.glStencilMask(0);

		GLState.blend(false);
		GLState.bindTex2D(-1);
		GLState.useProgram(-1);

		mMap.animator().updateAnimation();
		mViewport.setFrom(mMap.viewport());

		if (GLAdapter.debugView) {
			/* modify this to scale only the view, to see
			 * which tiles are rendered */
			mViewport.mvp.setScale(0.5f, 0.5f, 1);
			mViewport.viewproj.multiplyLhs(mViewport.mvp);
			mViewport.proj.multiplyLhs(mViewport.mvp);
		}

		/* update layers */
		LayerRenderer[] layers = mMap.layers().getLayerRenderer();

		for (int i = 0, n = layers.length; i < n; i++) {
			LayerRenderer renderer = layers[i];

			if (!renderer.isInitialized) {
				renderer.setup();
				renderer.isInitialized = true;
			}

			renderer.update(mViewport);

			if (renderer.isReady)
				renderer.render(mViewport);

			if (GLAdapter.debug)
				GLUtils.checkGlError(renderer.getClass().getName());
		}

		if (GLUtils.checkGlOutOfMemory("finish")) {
			BufferObject.checkBufferUsage(true);
			// FIXME also throw out some textures etc
		}
		if (rerender) {
			mMap.render();
			rerender = false;
		}
	}

	public void onSurfaceChanged(int width, int height) {
		//log.debug("onSurfaceChanged: new={}, {}x{}", mNewSurface, width, height);

		if (width <= 0 || height <= 0)
			return;

		//mMap.viewport().getMatrix(null, mMatrices.proj, null);
		mViewport.initFrom(mMap.viewport());
		GL.glViewport(0, 0, width, height);

		//GL.glScissor(0, 0, width, height);
		//GL.glEnable(GL20.GL_SCISSOR_TEST);

		GL.glClearStencil(0x00);

		GL.glDisable(GL20.GL_CULL_FACE);
		GL.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

		GL.glFrontFace(GL20.GL_CW);
		GL.glCullFace(GL20.GL_BACK);

		if (!mNewSurface) {
			mMap.updateMap(false);
			return;
		}

		mNewSurface = false;

		/** initialize quad indices used by Texture- and LineTexRenderer */
		int[] vboIds = GLUtils.glGenBuffers(2);

		mQuadIndicesID = vboIds[0];
		int maxIndices = maxQuads * TextureBucket.INDICES_PER_SPRITE;
		short[] indices = new short[maxIndices];
		for (int i = 0, j = 0; i < maxIndices; i += 6, j += 4) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);

			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 1);
			indices[i + 5] = (short) (j + 3);
		}
		ShortBuffer buf = MapRenderer.getShortBuffer(indices.length);
		buf.put(indices);
		buf.flip();

		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER,
		                mQuadIndicesID);
		GL.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER,
		                indices.length * 2, buf, GL20.GL_STATIC_DRAW);
		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		/** initialize default quad */
		FloatBuffer floatBuffer = MapRenderer.getFloatBuffer(8);
		float[] quad = new float[] { -1, -1, -1, 1, 1, -1, 1, 1 };
		floatBuffer.put(quad);
		floatBuffer.flip();
		mQuadVerticesID = vboIds[1];

		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, mQuadVerticesID);
		GL.glBufferData(GL20.GL_ARRAY_BUFFER,
		                quad.length * 4, floatBuffer, GL20.GL_STATIC_DRAW);
		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

		GLState.init(GL);

		mMap.updateMap(true);
	}

	public void onSurfaceCreated() {
		GL = GLAdapter.get();
		// log.debug(GL.glGetString(GL20.GL_EXTENSIONS));
		String vendor = GL.glGetString(GL20.GL_VENDOR);
		String renderer = GL.glGetString(GL20.GL_RENDERER);
		String version = GL.glGetString(GL20.GL_VERSION);
		log.debug("{}/{}/{}", vendor, renderer, version);

		if ("Adreno (TM) 330".equals(renderer) || "Adreno (TM) 320".equals(renderer)) {
			log.debug("==> not using glBufferSubData");
			GLAdapter.NO_BUFFER_SUB_DATA = true;
		}

		GLState.init(GL);
		GLUtils.init(GL);
		GLShader.init(GL);
		OffscreenRenderer.init(GL);

		// Set up some vertex buffer objects
		BufferObject.init(GL, 200);

		// classes that require GL context for initialization
		RenderBuckets.initRenderer(GL);
		LayerRenderer.init(GL);

		mNewSurface = true;
	}

	private boolean mNewSurface;

	/**
	 * Bind VBO for a simple quad. Handy for simple custom RenderLayers
	 * Vertices: float[]{ -1, -1, -1, 1, 1, -1, 1, 1 }
	 * 
	 * GL.glDrawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);
	 * 
	 * @param bind - true to activate, false to unbind
	 */
	public static void bindQuadVertexVBO(int location, boolean bind) {
		GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, mQuadVerticesID);
		if (location >= 0)
			GL.glVertexAttribPointer(location, 2, GL20.GL_FLOAT, false, 0, 0);
	}

	/**
	 * Bind indices for rendering up to MapRenderer.maxQuads (512) in
	 * one draw call. Vertex order is 0-1-2 2-1-3
	 * 
	 * @param bind - true to activate, false to unbind (dont forget!)
	 * */
	public static void bindQuadIndicesVBO(boolean bind) {
		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bind ? mQuadIndicesID : 0);
	}

	/**
	 * Trigger next redraw from GL-Thread. This should be used to animate
	 * LayerRenderers instead of calling Map.render().
	 */
	public static void animate() {
		rerender = true;
	}

	public static FloatBuffer getFloatBuffer(int size) {
		return mBufferPool.getFloatBuffer(size);
	}

	public static ShortBuffer getShortBuffer(int size) {
		return mBufferPool.getShortBuffer(size);
	}

	public static IntBuffer getIntBuffer(int size) {
		return mBufferPool.getIntBuffer(size);
	}

}
