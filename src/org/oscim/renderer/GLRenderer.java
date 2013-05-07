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

import static android.opengl.GLES20.GL_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_DYNAMIC_DRAW;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.renderer.sublayers.Layers;
import org.oscim.theme.IRenderTheme;
import org.oscim.utils.GlUtils;
import org.oscim.utils.Matrix4;
import org.oscim.view.MapView;
import org.oscim.view.MapViewPosition;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class GLRenderer implements GLSurfaceView.Renderer {

	private static final String TAG = GLRenderer.class.getName();

	private static final int MB = 1024 * 1024;
	private static final int SHORT_BYTES = 2;
	private static final int CACHE_TILES_MAX = 250;
	private static final int LIMIT_BUFFERS = 16 * MB;

	public static final float COORD_SCALE = 8.0f;

	static int CACHE_TILES = CACHE_TILES_MAX;

	private static MapView mMapView;
	public static int screenWidth, screenHeight;

	private static MapViewPosition mMapViewPosition;
	private static MapPosition mMapPosition;

	private static ShortBuffer shortBuffer;
	private static FloatBuffer floatBuffer;
	private static int tmpBufferSize;

	private static short[] mFillCoords;

	// bytes currently loaded in VBOs
	private static int mBufferMemoryUsage;

	public class Matrices {
		// do not modify any of these
		public final Matrix4 viewproj = new Matrix4();
		public final Matrix4 proj = new Matrix4();
		public final Matrix4 view = new Matrix4();
		public final float[] mapPlane = new float[8];

		// for temporary use by callee
		public final Matrix4 mvp = new Matrix4();

		/**
		 * Set MVP so that coordinates are in screen pixel coordinates
		 * with 0,0 being center
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
			//
			//				mvp.setTransScale(-screenWidth / ratio,
			//						screenHeight / ratio, ratio);

			mvp.multiplyMM(proj, mvp);
		}
	}

	private static Matrices mMatrices;

	//private
	static float[] mClearColor = null;

	public static int mQuadIndicesID;
	public final static int maxQuads = 64;

	private static boolean mUpdateColor = false;

	// drawlock to synchronize Main- and GL-Thread
	// static ReentrantLock tilelock = new ReentrantLock();
	public static ReentrantLock drawlock = new ReentrantLock();

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
		short min = 0;
		short max = (short) ((Tile.SIZE * COORD_SCALE));
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

	/**
	 * Only use on GL Thread!
	 * Get a native ShortBuffer for temporary use.
	 */
	public static ShortBuffer getShortBuffer(int size) {
		if (tmpBufferSize < size * 2)
			growBuffer(size * 2);
		else
			shortBuffer.clear();

		return shortBuffer;
	}

	/**
	 * Only use on GL Thread!
	 * Get a native FloatBuffer for temporary use.
	 */
	public static FloatBuffer getFloatBuffer(int size) {
		if (tmpBufferSize < size * 4)
			growBuffer(size * 4);
		else
			floatBuffer.clear();

		return floatBuffer;
	}

	private static void growBuffer(int size) {
		Log.d(TAG, "grow buffer " + size);
		ByteBuffer buf = ByteBuffer
				.allocateDirect(size)
				.order(ByteOrder.nativeOrder());

		floatBuffer = buf.asFloatBuffer();
		shortBuffer = buf.asShortBuffer();
		tmpBufferSize = size;
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

		GLES20.glBindBuffer(GL_ARRAY_BUFFER, layers.vbo.id);

		// reuse memory allocated for vbo when possible and allocated
		// memory is less then four times the new data
		if (layers.vbo.size > newSize && layers.vbo.size < newSize * 4
				&& mBufferMemoryUsage < LIMIT_BUFFERS) {
			GLES20.glBufferSubData(GL_ARRAY_BUFFER, 0, newSize, sbuf);
		} else {
			mBufferMemoryUsage += newSize - layers.vbo.size;
			layers.vbo.size = newSize;
			GLES20.glBufferData(GL_ARRAY_BUFFER, layers.vbo.size, sbuf, GL_DYNAMIC_DRAW);
		}

		return true;
	}

	public static void checkBufferUsage(boolean force) {
		// try to clear some unused vbo when exceding limit

		if (!force && mBufferMemoryUsage < LIMIT_BUFFERS) {
			if (CACHE_TILES < CACHE_TILES_MAX)
				CACHE_TILES += 50;
			return;
		}

		Log.d(TAG, "buffer object usage: " + mBufferMemoryUsage / MB + "MB");

		mBufferMemoryUsage -= BufferObject.limitUsage(2 * MB);

		Log.d(TAG, "now: " + mBufferMemoryUsage / MB + "MB");

		if (mBufferMemoryUsage > LIMIT_BUFFERS && CACHE_TILES > 100)
			CACHE_TILES -= 50;
	}

	private long lastDraw = 0;

	@Override
	public void onDrawFrame(GL10 glUnused) {
		long start = SystemClock.uptimeMillis();
		long wait = 30 - (start - lastDraw);
		if (wait > 5) {
			//Log.d(TAG, "wait " + wait);
			SystemClock.sleep(wait);
			lastDraw = start + wait;
		} else
			lastDraw = start;

		// prevent main thread recreating all tiles (updateMap)
		// while rendering is going on.
		drawlock.lock();
		try {
			draw();
		} finally {
			drawlock.unlock();
		}
	}

	private static void draw() {
		long start = 0;

		if (MapView.debugFrameTime)
			start = SystemClock.uptimeMillis();

		if (mUpdateColor) {
			float cc[] = mClearColor;
			GLES20.glClearColor(cc[0], cc[1], cc[2], cc[3]);
			mUpdateColor = false;
		}

		GLES20.glDepthMask(true);
		GLES20.glStencilMask(0xFF);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT
				| GLES20.GL_DEPTH_BUFFER_BIT
				| GLES20.GL_STENCIL_BUFFER_BIT);

		boolean changed = false;

		// get current MapPosition, set mBoxCoords (mapping of screen to model
		// coordinates)
		MapPosition pos = mMapPosition;

		synchronized (mMapViewPosition) {
			mMapViewPosition.updateAnimation();

			changed = mMapViewPosition.getMapPosition(pos);

			if (changed)
				mMapViewPosition.getMapViewProjection(mMatrices.mapPlane);

			mMapViewPosition.getMatrix(mMatrices.view, null, mMatrices.viewproj);

			if (debugView) {
				mMatrices.mvp.setScale(0.5f, 0.5f, 1);
				mMatrices.viewproj.multiplyMM(mMatrices.mvp, mMatrices.viewproj);
			}
		}

		/* update layers */
		RenderLayer[] overlays = mMapView.getLayerManager().getRenderLayers();

		for (int i = 0, n = overlays.length; i < n; i++)
			overlays[i].update(pos, changed, mMatrices);

		/* draw layers */
		for (int i = 0, n = overlays.length; i < n; i++) {
			RenderLayer renderLayer = overlays[i];

			if (renderLayer.newData) {
				renderLayer.compile();
				renderLayer.newData = false;
			}

			if (renderLayer.isReady)
				renderLayer.render(mMapPosition, mMatrices);
		}

		if (MapView.debugFrameTime) {
			GLES20.glFinish();
			Log.d(TAG, "draw took " + (SystemClock.uptimeMillis() - start));
		}

		if (GlUtils.checkGlOutOfMemory("finish")) {
			checkBufferUsage(true);
			// TODO also throw out some textures etc
		}
	}

	public static int depthOffset(MapTile t) {
		return ((t.tileX % 4) + (t.tileY % 4 * 4) + 1);
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
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
			mMatrices.proj.multiplyMM(mMatrices.mvp, mMatrices.proj);
		}

		GLES20.glViewport(0, 0, width, height);
		GLES20.glScissor(0, 0, width, height);
		GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

		GLES20.glClearStencil(0x00);

		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

		if (!mNewSurface) {
			mMapView.redrawMap(false);
			return;
		}
		mNewSurface = false;

		// set initial temp buffer size
		growBuffer(MB >> 2);

		// upload quad indices used by Texture- and LineTexRenderer
		int[] vboIds = new int[1];
		GLES20.glGenBuffers(1, vboIds, 0);
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

		shortBuffer.put(indices);
		shortBuffer.flip();

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mQuadIndicesID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				indices.length * 2, shortBuffer, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		mBufferMemoryUsage = 0;
		//mDrawTiles = null;

		int numTiles = (screenWidth / (Tile.SIZE / 2) + 2)
				* (screenHeight / (Tile.SIZE / 2) + 2);

		// Set up vertex buffer objects
		int numVBO = (CACHE_TILES + (numTiles * 2));
		BufferObject.init(numVBO);

		if (mClearColor != null)
			mUpdateColor = true;

		GLState.init();

		mMapView.redrawMap(true);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Log.d(TAG, GLES20.glGetString(GLES20.GL_EXTENSIONS));

		// classes that require GL context for initialization
		Layers.initRenderer();

		mNewSurface = true;
	}

	private boolean mNewSurface;

	public static final boolean debugView = false;

	void clearBuffer() {
		mNewSurface = true;
	}
}
