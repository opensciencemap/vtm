/*
 * Copyright 2012, 2013 Hannes Janetzek
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.theme.IRenderTheme;
import org.oscim.utils.GlUtils;
import org.oscim.utils.Matrix4;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.Pool;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

public class GLRenderer {
	private static final String TAG = GLRenderer.class.getName();

	private static GL20 GL = GLAdapter.get();

	private static final int SHORT_BYTES = 2;
	private static final int CACHE_TILES_MAX = 250;

	public static final float COORD_SCALE = 8.0f;

	static int CACHE_TILES = CACHE_TILES_MAX;

	private static MapView mMapView;
	public static int screenWidth, screenHeight;

	private static MapViewPosition mMapViewPosition;
	private static MapPosition mMapPosition;

	private static short[] mFillCoords;

	public class Matrices {
		// do not modify any of these
		public final Matrix4 viewproj = new Matrix4();
		public final Matrix4 proj = new Matrix4();
		public final Matrix4 view = new Matrix4();
		public final float[] mapPlane = new float[8];

		// for temporary use by callee
		public final Matrix4 mvp = new Matrix4();

		/**
		 * Set MVP so that coordinates are in screen pixel coordinates with 0,0
		 * being center
		 */
		public void useScreenCoordinates(boolean center, float scale) {
			float ratio = (1f / (scale * screenWidth));

			if (center)
				mvp.setScale(ratio, ratio, ratio);
			else
				mvp.setTransScale(
				                  (-screenWidth / 2) * ratio * scale,
				                  (-screenHeight / 2) * ratio * scale,
				                  ratio);

			mvp.multiplyLhs(proj);
		}
	}

	private static Matrices mMatrices;

	// private
	static float[] mClearColor = null;

	public static int mQuadIndicesID;
	public final static int maxQuads = 64;

	private static boolean mUpdateColor = false;

	// drawlock to synchronize Main- and GL-Thread
	// static ReentrantLock tilelock = new ReentrantLock();
	public static Object drawlock = new Object();

	/**
	 * @param mapView
	 *            the MapView
	 */
	public GLRenderer(MapView mapView) {

		mMapView = mapView;
		mMapViewPosition = mapView.getMapViewPosition();
		mMapPosition = new MapPosition();

		mMatrices = new Matrices();

		// tile fill coords
		short min = (short) 0;
		short max = (short) (Tile.SIZE * COORD_SCALE);
		mFillCoords = new short[8];
		mFillCoords[0] = min;
		mFillCoords[1] = max;
		mFillCoords[2] = max;
		mFillCoords[3] = max;
		mFillCoords[4] = min;
		mFillCoords[5] = min;
		mFillCoords[6] = max;
		mFillCoords[7] = min;
	}

	public static void setRenderTheme(IRenderTheme t) {
		mClearColor = GlUtils.colorToFloat(t.getMapBackground());
		mUpdateColor = true;
	}

	static class BufferItem extends Inlist<BufferItem> {

		ShortBuffer shortBuffer;
		FloatBuffer floatBuffer;
		IntBuffer intBuffer;
		int tmpBufferSize;

		void growBuffer(int size) {
			Log.d(TAG, "grow buffer " + size);
			// 32kb min size
			if (size < (1 << 15))
				size = (1 << 15);

			ByteBuffer buf = ByteBuffer
			        .allocateDirect(size)
			        .order(ByteOrder.nativeOrder());

			this.floatBuffer = buf.asFloatBuffer();
			this.shortBuffer = buf.asShortBuffer();
			this.intBuffer = buf.asIntBuffer();
			this.tmpBufferSize = size;
		}
	}
	static class BufferPool extends Pool<BufferItem>{
		private BufferItem mUsedBuffers;

		@Override
        protected BufferItem createItem() {
			// unused;
			return null;
        }

		public BufferItem get(int size){
			BufferItem b = pool;

			if (b == null){
				b = new BufferItem();
			} else {
				pool = b.next;
				b.next = null;
			}
			if (b.tmpBufferSize < size)
				b.growBuffer(size);

			mUsedBuffers = Inlist.push(mUsedBuffers, b);

			return b;
		}

		public void releaseBuffers(){
			mBufferPool.releaseAll(mUsedBuffers);
			mUsedBuffers = null;
		}

	}
	// Do not use the same buffer to upload data within a frame twice
	// - Contrary to what the OpenGL doc says data seems *not* to be
	// *always* copied after glBufferData returns...
	// - Somehow it does always copy when using Android GL bindings
	// but not when using libgdx bindings (LWJGL or AndroidGL20)
	private static BufferPool mBufferPool = new BufferPool();


	/**
	 * Only use on GL Thread! Get a native ShortBuffer for temporary use.
	 */
	public static ShortBuffer getShortBuffer(int size) {
		BufferItem b = mBufferPool.get(size * 2);
		b.shortBuffer.clear();
		return b.shortBuffer;
	}

	/**
	 * Only use on GL Thread! Get a native FloatBuffer for temporary use.
	 */
	public static FloatBuffer getFloatBuffer(int size) {
		BufferItem b = mBufferPool.get(size * 4);
		b.floatBuffer.clear();
		return b.floatBuffer;
	}

	/**
	 * Only use on GL Thread! Get a native IntBuffer for temporary use.
	 */
	public static IntBuffer getIntBuffer(int size) {
		BufferItem b = mBufferPool.get(size * 4);
		b.intBuffer.clear();
		return b.intBuffer;
	}

	public static boolean uploadLayers(Layers layers, int newSize,
	                                   boolean addFill) {
		// add fill coordinates
		if (addFill)
			newSize += 8;

		ShortBuffer sbuf = getShortBuffer(newSize);

		if (addFill)
			sbuf.put(mFillCoords, 0, 8);

		layers.compile(sbuf, addFill);
		sbuf.flip();

		if (newSize != sbuf.remaining()) {
			Log.d(TAG, "wrong size: "
			           + " new size: " + newSize
			           + " buffer pos: " + sbuf.position()
			           + " buffer limit: " + sbuf.limit()
			           + " buffer fill: " + sbuf.remaining());
			return false;
		}
		newSize *= SHORT_BYTES;

		layers.vbo.loadBufferData(sbuf, newSize, GL20.GL_ARRAY_BUFFER);
		return true;
	}

	public void onDrawFrame() {

		// prevent main thread recreating all tiles (updateMap)
		// while rendering is going on.
		synchronized(drawlock){
			draw();
		}

		mBufferPool.releaseBuffers();
	}

	private static void draw() {

		if (mUpdateColor) {
			float cc[] = mClearColor;
			GL.glClearColor(cc[0], cc[1], cc[2], cc[3]);
			mUpdateColor = false;
		}

		GL.glDepthMask(true);
		GL.glStencilMask(0xFF);
		GL.glClear(GL20.GL_COLOR_BUFFER_BIT
		           | GL20.GL_DEPTH_BUFFER_BIT
		           | GL20.GL_STENCIL_BUFFER_BIT);

		boolean changed = false;

		MapPosition pos = mMapPosition;

		synchronized (mMapViewPosition) {
			// update MapPosition
			mMapViewPosition.updateAnimation();

			// get current MapPosition
			changed = mMapViewPosition.getMapPosition(pos);

			if (changed)
				mMapViewPosition.getMapViewProjection(mMatrices.mapPlane);

			mMapViewPosition.getMatrix(mMatrices.view, mMatrices.proj, mMatrices.viewproj);

			if (debugView) {
				mMatrices.mvp.setScale(0.5f, 0.5f, 1);
				mMatrices.viewproj.multiplyLhs(mMatrices.mvp);
			}
		}

		/* update layers */
		RenderLayer[] layers = mMapView.getLayerManager().getRenderLayers();

		for (int i = 0, n = layers.length; i < n; i++)
			layers[i].update(pos, changed, mMatrices);

		/* compile layers */
		for (int i = 0, n = layers.length; i < n; i++) {
			RenderLayer renderLayer = layers[i];

			if (renderLayer.newData) {
				renderLayer.compile();
				renderLayer.newData = false;
			}
		}

		for (int i = 0, n = layers.length; i < n; i++) {
			RenderLayer renderLayer = layers[i];

			if (renderLayer.isReady)
				renderLayer.render(mMapPosition, mMatrices);
		}

		if (GlUtils.checkGlOutOfMemory("finish")) {
			BufferObject.checkBufferUsage(true);
			// FIXME also throw out some textures etc
		}
	}

	public static int depthOffset(MapTile t) {
		return ((t.tileX % 4) + (t.tileY % 4 * 4) + 1);
	}

	public void onSurfaceChanged(int width, int height) {
		Log.d(TAG, "SurfaceChanged:" + mNewSurface + " " + width + "x" + height);

		if (width <= 0 || height <= 0)
			return;

		screenWidth = width;
		screenHeight = height;

		mMapViewPosition.getMatrix(null, mMatrices.proj, null);

		if (debugView) {
			// modify this to scale only the view, to see better which tiles
			// are rendered
			mMatrices.mvp.setScale(0.5f, 0.5f, 1);
			mMatrices.proj.multiplyLhs(mMatrices.mvp);
		}
		GL = GLAdapter.get();
		GL.glViewport(0, 0, width, height);
		GL.glScissor(0, 0, width, height);
		GL.glEnable(GL20.GL_SCISSOR_TEST);

		GL.glClearStencil(0x00);

		GL.glDisable(GL20.GL_CULL_FACE);
		GL.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

		if (!mNewSurface) {
			mMapView.updateMap(false);
			return;
		}

		mNewSurface = false;

		// upload quad indices used by Texture- and LineTexRenderer
		int[] vboIds = GlUtils.glGenBuffers(1);

		mQuadIndicesID = vboIds[0];
		int maxIndices = maxQuads * 6;
		short[] indices = new short[maxIndices];
		for (int i = 0, j = 0; i < maxIndices; i += 6, j += 4) {
			indices[i + 0] = (short) (j + 0);
			indices[i + 1] = (short) (j + 1);
			indices[i + 2] = (short) (j + 2);

			indices[i + 3] = (short) (j + 2);
			indices[i + 4] = (short) (j + 1);
			indices[i + 5] = (short) (j + 3);
		}
		ShortBuffer buf = GLRenderer.getShortBuffer(indices.length);
		buf.put(indices);
		buf.flip();

		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER,
		                mQuadIndicesID);
		GL.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER,
		                indices.length * 2, buf, GL20.GL_STATIC_DRAW);
		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		if (mClearColor != null)
			mUpdateColor = true;

		GLState.init();

		mMapView.updateMap(true);
	}

	public void onSurfaceCreated() {
		
		Log.d(TAG, "surface created");
		
		// Log.d(TAG, GL.glGetString(GL20.GL_EXTENSIONS));

		// classes that require GL context for initialization
		Layers.initRenderer();

		// Set up some vertex buffer objects
		BufferObject.init(CACHE_TILES);

		mNewSurface = true;
	}

	private boolean mNewSurface;

	public static final boolean debugView = false;
}
