package org.oscim.utils.geom;

import org.oscim.backend.Log;
import org.oscim.renderer.sublayers.VertexItem;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayUtils;
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Int32Array;

public class Triangulator {

	public static synchronized int triangulate(float[] points, int ppos, int plen, short[] index,
			int ipos, int rings, int vertexOffset, VertexItem outTris) {

		JavaScriptObject o;

		try{
		o = tessellate(JsArrayUtils.readOnlyJsArray(points), ppos, plen,
				JsArrayUtils.readOnlyJsArray(index), ipos, rings);
		} catch(JavaScriptException e){
			e.printStackTrace();
			return 0;
		}

		Float32Array vo = getPoints(o);
		Int32Array io = getIndices(o);


		if (vo.length() != plen) {
			// TODO handle different output points
			Log.d("", "other points out" + plen + ":" + vo.length() + ", " + io.length());

			//for (int i = 0; i < vo.length(); i += 2)
			//	Log.d("<", vo.get(i) + " " + vo.get(i + 1));
			//for (int i = ppos; i < ppos + plen; i += 2)
			//	Log.d(">",  points[i]+ " " + points[i + 1]);

			return 0;
		}

		int numIndices = io.length();

		for (int k = 0, cnt = 0; k < numIndices; k += cnt) {

			if (outTris.used == VertexItem.SIZE) {
				outTris.next = VertexItem.pool.get();
				outTris = outTris.next;
			}

			cnt = VertexItem.SIZE - outTris.used;

			if (k + cnt > numIndices)
				cnt = numIndices - k;

			for (int i = 0; i < cnt; i++){
				int idx = (vertexOffset + io.get(k + i));
				outTris.vertices[outTris.used + i] = (short) idx;
			}
			outTris.used += cnt;
		}

		return numIndices;
	}

	static native JavaScriptObject tessellate(JsArrayNumber points, int pOffset, int pLength,
			JsArrayInteger bounds, int bOffset, int bLength)/*-{

		return $wnd.tessellate(points, pOffset, pOffset + pLength, bounds,
				bOffset, bOffset + bLength);
	}-*/;

	static native Float32Array getPoints(JavaScriptObject result)/*-{
		return result.vertices;
	}-*/;

	static native Int32Array getIndices(JavaScriptObject result)/*-{
		return result.triangles;
	}-*/;

}
