/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.renderer;

import static org.oscim.backend.GL20.GL_CULL_FACE;
import static org.oscim.backend.GL20.GL_DEPTH_BUFFER_BIT;
import static org.oscim.backend.GL20.GL_EQUAL;
import static org.oscim.backend.GL20.GL_LEQUAL;
import static org.oscim.backend.GL20.GL_LESS;
import static org.oscim.backend.GL20.GL_LINES;
import static org.oscim.backend.GL20.GL_SHORT;
import static org.oscim.backend.GL20.GL_TRIANGLES;
import static org.oscim.backend.GL20.GL_UNSIGNED_BYTE;
import static org.oscim.backend.GL20.GL_UNSIGNED_SHORT;

import org.oscim.backend.GL20;
import org.oscim.core.Tile;
import org.oscim.renderer.bucket.ExtrusionBucket;
import org.oscim.renderer.bucket.ExtrusionBuckets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExtrusionRenderer extends LayerRenderer {
	static final Logger log = LoggerFactory.getLogger(ExtrusionRenderer.class);

	private final boolean mTranslucent;
	private final int mMode;
	private Shader mShader;

	protected ExtrusionBuckets[] mExtrusionBucketSet = {};
	protected int mBucketsCnt;
	protected float mAlpha = 1;

	public ExtrusionRenderer(boolean mesh, boolean alpha) {
		mMode = mesh ? 1 : 0;
		mTranslucent = alpha;
	}

	public static class Shader extends GLShader {
		int uMVP, uColor, uAlpha, uMode, aPos, aLight;

		public Shader(String shader) {
			if (!create(shader))
				return;

			uMVP = getUniform("u_mvp");
			uColor = getUniform("u_color");
			uAlpha = getUniform("u_alpha");
			uMode = getUniform("u_mode");
			aPos = getAttrib("a_pos");
			aLight = getAttrib("a_light");
		}
	}

	@Override
	public boolean setup() {
		if (mMode == 0)
			mShader = new Shader("extrusion_layer_ext");
		else
			mShader = new Shader("extrusion_layer_mesh");

		return true;
	}

	private void renderCombined(int vertexPointer, ExtrusionBuckets ebs) {

		for (ExtrusionBucket eb = ebs.buckets(); eb != null; eb = eb.next()) {

			GL.glVertexAttribPointer(vertexPointer, 3,
			                         GL_SHORT, false, 8,
			                         eb.getVertexOffset());

			int sumIndices = eb.idx[0] + eb.idx[1] + eb.idx[2];

			/* extrusion */
			if (sumIndices > 0)
				GL.glDrawElements(GL_TRIANGLES, sumIndices,
				                  GL_UNSIGNED_SHORT, eb.off[0]);

			/* mesh */
			if (eb.idx[4] > 0) {
				GL.glDrawElements(GL_TRIANGLES, eb.idx[4],
				                  GL_UNSIGNED_SHORT, eb.off[4]);
			}
		}
	}

	@Override
	public void render(GLViewport v) {

		GL.glDepthMask(true);
		GL.glClear(GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);

		Shader s = mShader;
		s.useProgram();
		GLState.enableVertexArrays(s.aPos, -1);

		/* only use face-culling when it's unlikely
		 * that one'moves through the building' */
		if (v.pos.zoomLevel < 18)
			GL.glEnable(GL_CULL_FACE);

		GL.glDepthFunc(GL_LESS);
		GL.glUniform1f(s.uAlpha, mAlpha);

		ExtrusionBuckets[] ebs = mExtrusionBucketSet;

		if (mTranslucent) {
			/* only draw to depth buffer */
			GLState.blend(false);
			GL.glColorMask(false, false, false, false);
			GL.glUniform1i(s.uMode, -1);

			for (int i = 0; i < mBucketsCnt; i++) {
				if (ebs[i].ibo == null)
					return;

				ebs[i].ibo.bind();
				ebs[i].vbo.bind();

				setMatrix(v, ebs[i], true);
				v.mvp.setAsUniform(s.uMVP);

				renderCombined(s.aPos, ebs[i]);
			}

			/* only draw to color buffer */
			GL.glColorMask(true, true, true, true);
			GL.glDepthMask(false);

			GL.glDepthFunc(GL_EQUAL);
		}

		GLState.blend(true);

		GLState.enableVertexArrays(s.aPos, s.aLight);
		float[] currentColor = null;

		for (int i = 0; i < mBucketsCnt; i++) {
			if (ebs[i].ibo == null)
				continue;

			ebs[i].ibo.bind();
			ebs[i].vbo.bind();

			if (!mTranslucent) {
				setMatrix(v, ebs[i], false);
				v.mvp.setAsUniform(s.uMVP);
			}

			ExtrusionBucket eb = ebs[i].buckets();

			for (; eb != null; eb = eb.next()) {

				if (eb.colors != currentColor) {
					currentColor = eb.colors;
					GLUtils.glUniform4fv(s.uColor,
					                     mMode == 0 ? 4 : 1,
					                     eb.colors);
				}
				GL.glVertexAttribPointer(s.aPos, 3, GL_SHORT,
				                         false, 8, eb.getVertexOffset());

				GL.glVertexAttribPointer(s.aLight, 2, GL_UNSIGNED_BYTE,
				                         false, 8, eb.getVertexOffset() + 6);

				/* draw extruded outlines */
				if (eb.idx[0] > 0) {
					if (mTranslucent) {
						GL.glDepthFunc(GL20.GL_EQUAL);
						setMatrix(v, ebs[i], true);
						v.mvp.setAsUniform(s.uMVP);
					}

					/* draw roof */
					GL.glUniform1i(s.uMode, 0);
					GL.glDrawElements(GL_TRIANGLES, eb.idx[2], GL_UNSIGNED_SHORT, eb.off[2]);

					/* draw sides 1 */
					GL.glUniform1i(s.uMode, 1);
					GL.glDrawElements(GL_TRIANGLES, eb.idx[0], GL_UNSIGNED_SHORT, eb.off[0]);

					/* draw sides 2 */
					GL.glUniform1i(s.uMode, 2);
					GL.glDrawElements(GL_TRIANGLES, eb.idx[1], GL_UNSIGNED_SHORT, eb.off[1]);

					if (mTranslucent) {
						/* drawing gl_lines with the same coordinates does not
						 * result in same depth values as polygons, so add
						 * offset and draw gl_lequal: */
						GL.glDepthFunc(GL_LEQUAL);
						v.mvp.addDepthOffset(100);
						v.mvp.setAsUniform(s.uMVP);
					}

					GL.glUniform1i(s.uMode, 3);

					GL.glDrawElements(GL_LINES, eb.idx[3], GL_UNSIGNED_SHORT, eb.off[3]);
				}

				/* draw triangle meshes */
				if (eb.idx[4] > 0) {
					GL.glDrawElements(GL_TRIANGLES, eb.idx[4], GL_UNSIGNED_SHORT, eb.off[4]);
				}
			}

			/* just a temporary reference! */
			ebs[i] = null;
		}

		if (!mTranslucent)
			GL.glDepthMask(false);

		if (v.pos.zoomLevel < 18)
			GL.glDisable(GL_CULL_FACE);
	}

	private static void setMatrix(GLViewport v, ExtrusionBuckets l, boolean offset) {

		int z = l.zoomLevel;
		double curScale = Tile.SIZE * v.pos.scale;
		float scale = (float) (v.pos.scale / (1 << z));

		float x = (float) ((l.x - v.pos.x) * curScale);
		float y = (float) ((l.y - v.pos.y) * curScale);

		v.mvp.setTransScale(x, y, scale / MapRenderer.COORD_SCALE);
		v.mvp.setValue(10, scale / 10);
		v.mvp.multiplyLhs(v.viewproj);

		if (offset) {
			int delta = (int) (l.x * (1 << z)) % 4 + (int) (l.y * (1 << z)) % 4 * 4;
			v.mvp.addDepthOffset(delta);
		}
	}
}
