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
package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MercatorProjection;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.utils.ColorUtil;
import org.oscim.utils.Tessellator;
import org.oscim.utils.pool.Inlist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshLayer extends RenderElement {
	static final Logger log = LoggerFactory.getLogger(MeshLayer.class);
	static final boolean dbgRender = false;

	BufferObject indicesVbo;
	int numIndices;

	VertexItem indiceItems;
	public AreaStyle area;
	public float heightOffset;

	public MeshLayer(int level) {
		super(RenderElement.MESH);
		this.level = level;
	}

	public void addMesh(GeometryBuffer geom) {
		if (geom.index[0] < 6)
			return;

		if (vertexItems == null) {
			vertexItems = VertexItem.pool.get();
			indiceItems = VertexItem.pool.get();
		}

		numIndices += Tessellator.tessellate(geom, MapRenderer.COORD_SCALE,
		                                     Inlist.last(vertexItems),
		                                     Inlist.last(indiceItems),
		                                     numVertices);

		numVertices = vertexItems.getSize() / 2;

		if (numIndices <= 0) {
			log.debug("empty " + geom.index);
			vertexItems = VertexItem.pool.releaseAll(vertexItems);
			indiceItems = VertexItem.pool.releaseAll(indiceItems);
		}
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
		if (indiceItems == null) {
			indicesVbo = BufferObject.release(indicesVbo);
			return;
		}

		// add vertices to shared VBO
		ElementLayers.addPoolItems(this, sbuf);

		// add indices to indicesVbo
		sbuf = MapRenderer.getShortBuffer(numIndices);

		for (VertexItem it = indiceItems; it != null; it = it.next)
			sbuf.put(it.vertices, 0, it.used);

		indiceItems = VertexItem.pool.releaseAll(indiceItems);

		if (indicesVbo == null)
			indicesVbo = BufferObject.get(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		indicesVbo.loadBufferData(sbuf.flip(), sbuf.limit() * 2);
	}

	@Override
	protected void clear() {
		indicesVbo = BufferObject.release(indicesVbo);
		indiceItems = VertexItem.pool.releaseAll(indiceItems);
		vertexItems = VertexItem.pool.releaseAll(vertexItems);
	}

	public static class Renderer {
		static Shader shader;

		static boolean init() {
			shader = new Shader("mesh_layer_2D");
			return true;
		}

		static class Shader extends GLShader {
			int uMVP, uColor, uHeight, aPos;

			Shader(String shaderFile) {
				if (!create(shaderFile))
					return;

				uMVP = getUniform("u_mvp");
				uColor = getUniform("u_color");
				uHeight = getUniform("u_height");
				aPos = getAttrib("a_pos");
			}
		}

		public static RenderElement draw(RenderElement l, GLViewport v) {
			GLState.blend(true);

			Shader s = shader;
			s.useProgram();
			GLState.enableVertexArrays(s.aPos, -1);

			v.mvp.setAsUniform(s.uMVP);

			float heightOffset = 0;
			GL.glUniform1f(s.uHeight, heightOffset);

			for (; l != null && l.type == RenderElement.MESH; l = l.next) {
				MeshLayer ml = (MeshLayer) l;

				if (ml.indicesVbo == null)
					continue;

				if (ml.heightOffset != heightOffset) {
					heightOffset = ml.heightOffset;

					GL.glUniform1f(s.uHeight, heightOffset /
					        MercatorProjection.groundResolution(v.pos));
				}

				ml.indicesVbo.bind();

				if (ml.area == null)
					GLUtils.setColor(s.uColor, Color.BLUE, 0.4f);
				else
					GLUtils.setColor(s.uColor, ml.area.color, 1);

				GL.glVertexAttribPointer(s.aPos, 2, GL20.GL_SHORT,
				                         false, 0, ml.offset);

				GL.glDrawElements(GL20.GL_TRIANGLES, ml.numIndices,
				                  GL20.GL_UNSIGNED_SHORT, 0);

				if (dbgRender) {
					GLUtils.setColor(s.uColor, ColorUtil.shiftHue(ml.area.color, 0.5), 0.8f);
					GL.glDrawElements(GL20.GL_LINES, ml.numIndices,
					                  GL20.GL_UNSIGNED_SHORT, 0);
				}
			}

			GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

			return l;
		}
	}
}
