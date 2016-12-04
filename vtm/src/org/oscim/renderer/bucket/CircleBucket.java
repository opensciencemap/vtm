/*
 * Copyright 2016 Andrey Novikov
 * Copyright 2016 devemux86
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
package org.oscim.renderer.bucket;

import org.oscim.backend.GL;
import org.oscim.backend.GLAdapter;
import org.oscim.core.GeometryBuffer;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.theme.styles.CircleStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.oscim.backend.GLAdapter.gl;
import static org.oscim.renderer.MapRenderer.COORD_SCALE;

public class CircleBucket extends RenderBucket {

    private static final Logger log = LoggerFactory.getLogger(CircleBucket.class);

    public CircleStyle circle;

    public CircleBucket(int level) {
        super(RenderBucket.CIRCLE, true, false);
        this.level = level;
    }

    public void addCircle(GeometryBuffer geom) {
        if (!geom.isPoint()) {
            log.error("Circle style applied to non-point geometry");
            return;
        }

        float x = geom.getPointX(0);
        float y = geom.getPointY(0);

        vertexItems.add((short) (x * COORD_SCALE), (short) (y * COORD_SCALE));
        indiceItems.add((short) numVertices++);
        numIndices++;
    }

    public static class Renderer {
        static Shader shader;

        static boolean init() {
            shader = new Shader("circle");
            return true;
        }

        public static class Shader extends GLShader {
            int uMVP, uColor, uScale, aPos;

            Shader(String shaderFile) {
                gl.enable(GL.VERTEX_PROGRAM_POINT_SIZE);

                // OpenGL needs GLSL version 120
                String version = null;
                if (GLAdapter.GDX_DESKTOP_QUIRKS)
                    version = "120";

                if (!createVersioned(shaderFile, version))
                    return;

                uMVP = getUniform("u_mvp");
                uColor = getUniform("u_color");
                uScale = getUniform("u_scale");
                aPos = getAttrib("a_pos");
            }

            public void set(GLViewport v) {
                useProgram();
                GLState.enableVertexArrays(aPos, -1);

                v.mvp.setAsUniform(uMVP);
            }
        }

        public static RenderBucket draw(RenderBucket b, GLViewport v) {
            GLState.blend(true);

            Shader s = shader;

            s.set(v);

            for (; b != null && b.type == CIRCLE; b = b.next) {
                CircleBucket cb = (CircleBucket) b;

                GLUtils.setColor(s.uColor, cb.circle.fill, 1);
                gl.uniform1f(s.uScale, cb.circle.radius);

                gl.vertexAttribPointer(s.aPos, 2, GL.SHORT,
                        false, 0, cb.vertexOffset);

                gl.drawElements(GL.POINTS,
                        cb.numIndices,
                        GL.UNSIGNED_SHORT,
                        cb.indiceOffset);
            }

            return b;
        }
    }
}
