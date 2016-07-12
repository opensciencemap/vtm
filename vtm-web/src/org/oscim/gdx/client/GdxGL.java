package org.oscim.gdx.client;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import com.badlogic.gdx.backends.gwt.GwtGL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.webgl.client.WebGLRenderingContext;

import org.oscim.backend.GL;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class GdxGL extends GwtGL20 implements GL {

    protected final WebGLRenderingContext gl;

    public GdxGL(WebGLRenderingContext gl) {
        super(gl);
        gl.pixelStorei(WebGLRenderingContext.UNPACK_PREMULTIPLY_ALPHA_WEBGL, 1);
        this.gl = gl;
    }

    //    @Override
    //    public void glGetShaderSource(int shader, int bufsize, Buffer length, String source) {
    //
    //    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height,
                             int border, int format, int type, Buffer pixels) {

        if (pixels == null) {
            gl.texImage2D(target, level, internalformat,
                    width, height, border, format,
                    type, null);
            return;
        }

        Pixmap pixmap = Pixmap.pixmaps.get(((IntBuffer) pixels).get(0));
        if (pixmap != null) {
            gl.texImage2D(target, level, internalformat, format, type, pixmap.getCanvasElement());
        } else if (format == GL.ALPHA) {
            int tmp[] = new int[(width * height) >> 2];
            ((IntBuffer) pixels).get(tmp);

            Uint8ArrayNative v = com.google.gwt.typedarrays.client.Uint8ArrayNative.create(width
                    * height);

            for (int i = 0, n = (width * height) >> 2; i < n; i++) {
                v.set(i * 4 + 3, (tmp[i] >> 24) & 0xff);
                v.set(i * 4 + 2, (tmp[i] >> 16) & 0xff);
                v.set(i * 4 + 1, (tmp[i] >> 8) & 0xff);
                v.set(i * 4 + 0, (tmp[i]) & 0xff);
            }
            gl.texImage2D(target, level, internalformat, width, height, 0, format, type, v);
        }
    }

    public void activeTexture(int texture) {
        glActiveTexture(texture);
    }

    public void bindTexture(int target, int texture) {
        glBindTexture(target, texture);
    }

    public void blendFunc(int sfactor, int dfactor) {
        glBlendFunc(sfactor, dfactor);
    }

    public void clear(int mask) {
        glClear(mask);
    }

    public void clearColor(float red, float green, float blue, float alpha) {
        glClearColor(red, green, blue, alpha);
    }

    public void clearDepthf(float depth) {
        glClearDepthf(depth);
    }

    public void clearStencil(int s) {
        glClearStencil(s);
    }

    public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        glColorMask(red, green, blue, alpha);
    }

    public void compressedTexImage2D(int target, int level, int internalformat, int width,
                                     int height, int border,
                                     int imageSize, Buffer data) {
        glCompressedTexImage2D(
                target,
                level,
                internalformat,
                width,
                height,
                border,
                imageSize,
                data);
    }

    public void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset,
                                        int width, int height, int format,
                                        int imageSize, Buffer data) {
        glCompressedTexSubImage2D(target,
                level,
                xoffset,
                yoffset,
                width,
                height,
                format,
                imageSize,
                data);
    }

    public void copyTexImage2D(int target, int level, int internalformat, int x, int y,
                               int width, int height, int border) {
        glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    public void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y,
                                  int width, int height) {
        glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    public void cullFace(int mode) {
        glCullFace(mode);
    }

    public void deleteTextures(int n, IntBuffer textures) {
        glDeleteTextures(n, textures);
    }

    public void depthFunc(int func) {
        glDepthFunc(func);
    }

    public void depthMask(boolean flag) {
        glDepthMask(flag);
    }

    public void depthRangef(float zNear, float zFar) {
        glDepthRangef(zNear, zFar);
    }

    public void disable(int cap) {
        glDisable(cap);
    }

    public void drawArrays(int mode, int first, int count) {
        glDrawArrays(mode, first, count);
    }

    public void drawElements(int mode, int count, int type, Buffer indices) {
        glDrawElements(mode, count, type, indices);
    }

    public void enable(int cap) {
        glEnable(cap);
    }

    public void finish() {
        glFinish();
    }

    public void flush() {
        glFlush();
    }

    public void frontFace(int mode) {
        glFrontFace(mode);
    }

    public void genTextures(int n, IntBuffer textures) {
        glGenTextures(n, textures);
    }

    public int getError() {
        return glGetError();
    }

    public void getIntegerv(int pname, IntBuffer params) {
        glGetIntegerv(pname, params);
    }

    public String getString(int name) {
        return glGetString(name);
    }

    public void hint(int target, int mode) {
        glHint(target, mode);
    }

    public void lineWidth(float width) {
        glLineWidth(width);
    }

    public void pixelStorei(int pname, int param) {
        glPixelStorei(pname, param);
    }

    public void polygonOffset(float factor, float units) {
        glPolygonOffset(factor, units);
    }

    public void readPixels(int x, int y, int width, int height, int format, int type,
                           Buffer pixels) {
        glReadPixels(x, y, width, height, format, type, pixels);
    }

    public void scissor(int x, int y, int width, int height) {
        glScissor(x, y, width, height);
    }

    public void stencilFunc(int func, int ref, int mask) {
        glStencilFunc(func, ref, mask);
    }

    public void stencilMask(int mask) {
        glStencilMask(mask);
    }

    public void stencilOp(int fail, int zfail, int zpass) {
        glStencilOp(fail, zfail, zpass);
    }

    public void texImage2D(int target, int level, int internalFormat, int width, int height,
                           int border, int format, int type,
                           Buffer pixels) {
        glTexImage2D(target,
                level,
                internalFormat,
                width,
                height,
                border,
                format,
                type,
                pixels);
    }

    public void texParameterf(int target, int pname, float param) {
        glTexParameterf(target, pname, param);
    }

    public void texSubImage2D(int target, int level, int xoffset, int yoffset, int width,
                              int height, int format, int type,
                              Buffer pixels) {
        glTexSubImage2D(target,
                level,
                xoffset,
                yoffset,
                width,
                height,
                format,
                type,
                pixels);
    }

    public void viewport(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
    }

    public void getFloatv(int pname, FloatBuffer params) {
        glGetFloatv(pname, params);
    }

    public void getTexParameterfv(int target, int pname, FloatBuffer params) {
        glGetTexParameterfv(target, pname, params);
    }

    public void texParameterfv(int target, int pname, FloatBuffer params) {
        glTexParameterfv(target, pname, params);
    }

    public void bindBuffer(int target, int buffer) {
        glBindBuffer(target, buffer);
    }

    public void bufferData(int target, int size, Buffer data, int usage) {
        glBufferData(target, size, data, usage);
    }

    public void bufferSubData(int target, int offset, int size, Buffer data) {
        glBufferSubData(target, offset, size, data);
    }

    public void deleteBuffers(int n, IntBuffer buffers) {
        glDeleteBuffers(n, buffers);
    }

    public void getBufferParameteriv(int target, int pname, IntBuffer params) {
        glGetBufferParameteriv(target, pname, params);
    }

    public void genBuffers(int n, IntBuffer buffers) {
        glGenBuffers(n, buffers);
    }

    public void getTexParameteriv(int target, int pname, IntBuffer params) {
        glGetTexParameteriv(target, pname, params);
    }

    public boolean isBuffer(int buffer) {
        return glIsBuffer(buffer);
    }

    public boolean isEnabled(int cap) {
        return glIsEnabled(cap);
    }

    public boolean isTexture(int texture) {
        return glIsTexture(texture);
    }

    public void texParameteri(int target, int pname, int param) {
        glTexParameteri(target, pname, param);
    }

    public void texParameteriv(int target, int pname, IntBuffer params) {
        glTexParameteriv(target, pname, params);
    }

    public void drawElements(int mode, int count, int type, int indices) {
        glDrawElements(mode, count, type, indices);
    }

    public void attachShader(int program, int shader) {
        glAttachShader(program, shader);
    }

    public void bindAttribLocation(int program, int index, String name) {
        glBindAttribLocation(program, index, name);
    }

    public void bindFramebuffer(int target, int framebuffer) {
        glBindFramebuffer(target, framebuffer);
    }

    public void bindRenderbuffer(int target, int renderbuffer) {
        glBindRenderbuffer(target, renderbuffer);
    }

    public void blendColor(float red, float green, float blue, float alpha) {
        glBlendColor(red, green, blue, alpha);
    }

    public void blendEquation(int mode) {
        glBlendEquation(mode);
    }

    public void blendEquationSeparate(int modeRGB, int modeAlpha) {
        glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    public int checkFramebufferStatus(int target) {
        return glCheckFramebufferStatus(target);
    }

    public void compileShader(int shader) {
        glCompileShader(shader);
    }

    public int createProgram() {
        return glCreateProgram();
    }

    public int createShader(int type) {
        return glCreateShader(type);
    }

    public void deleteFramebuffers(int n, IntBuffer framebuffers) {
        glDeleteFramebuffers(n, framebuffers);
    }

    public void deleteProgram(int program) {
        glDeleteProgram(program);
    }

    public void deleteRenderbuffers(int n, IntBuffer renderbuffers) {
        glDeleteRenderbuffers(n, renderbuffers);
    }

    public void deleteShader(int shader) {
        glDeleteShader(shader);
    }

    public void detachShader(int program, int shader) {
        glDetachShader(program, shader);
    }

    public void disableVertexAttribArray(int index) {
        glDisableVertexAttribArray(index);
    }

    public void enableVertexAttribArray(int index) {
        glEnableVertexAttribArray(index);
    }

    public void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget,
                                        int renderbuffer) {
        glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    public void framebufferTexture2D(int target, int attachment, int textarget, int texture,
                                     int level) {
        glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    public void generateMipmap(int target) {
        glGenerateMipmap(target);
    }

    public void genFramebuffers(int n, IntBuffer framebuffers) {
        glGenFramebuffers(n, framebuffers);
    }

    public void genRenderbuffers(int n, IntBuffer renderbuffers) {
        glGenRenderbuffers(n, renderbuffers);
    }

    public String getActiveAttrib(int program, int index, IntBuffer size, Buffer type) {
        return glGetActiveAttrib(program,
                index,
                size,
                type);
    }

    public String getActiveUniform(int program, int index, IntBuffer size, Buffer type) {
        return glGetActiveUniform(program,
                index,
                size,
                type);
    }

    public void getAttachedShaders(int program, int maxcount, Buffer count, IntBuffer shaders) {
        glGetAttachedShaders(program,
                maxcount,
                count,
                shaders);
    }

    public int getAttribLocation(int program, String name) {
        return glGetAttribLocation(program, name);
    }

    public void getBooleanv(int pname, Buffer params) {
        glGetBooleanv(pname, params);
    }

    public void getFramebufferAttachmentParameteriv(int target, int attachment, int pname,
                                                    IntBuffer params) {
        glGetFramebufferAttachmentParameteriv(target,
                attachment,
                pname,
                params);
    }

    public void getProgramiv(int program, int pname, IntBuffer params) {
        glGetProgramiv(program, pname, params);
    }

    public String getProgramInfoLog(int program) {
        return glGetProgramInfoLog(program);
    }

    public void getRenderbufferParameteriv(int target, int pname, IntBuffer params) {
        glGetRenderbufferParameteriv(target, pname, params);
    }

    public void getShaderiv(int shader, int pname, IntBuffer params) {
        glGetShaderiv(shader, pname, params);
    }

    public String getShaderInfoLog(int shader) {
        return glGetShaderInfoLog(shader);
    }

    public void getShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range,
                                         IntBuffer precision) {
        glGetShaderPrecisionFormat(shadertype,
                precisiontype,
                range,
                precision);
    }

    public void getShaderSource(int shader, int bufsize, Buffer length, String source) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void getUniformfv(int program, int location, FloatBuffer params) {
        glGetUniformfv(program, location, params);
    }

    public void getUniformiv(int program, int location, IntBuffer params) {
        glGetUniformiv(program, location, params);
    }

    public int getUniformLocation(int program, String name) {
        return glGetUniformLocation(program, name);
    }

    public void getVertexAttribfv(int index, int pname, FloatBuffer params) {
        glGetVertexAttribfv(index, pname, params);
    }

    public void getVertexAttribiv(int index, int pname, IntBuffer params) {
        glGetVertexAttribiv(index, pname, params);
    }

    public void getVertexAttribPointerv(int index, int pname, Buffer pointer) {
        glGetVertexAttribPointerv(index, pname, pointer);
    }

    public boolean isFramebuffer(int framebuffer) {
        return glIsFramebuffer(framebuffer);
    }

    public boolean isProgram(int program) {
        return glIsProgram(program);
    }

    public boolean isRenderbuffer(int renderbuffer) {
        return glIsRenderbuffer(renderbuffer);
    }

    public boolean isShader(int shader) {
        return glIsShader(shader);
    }

    public void linkProgram(int program) {
        glLinkProgram(program);
    }

    public void releaseShaderCompiler() {
        glReleaseShaderCompiler();
    }

    public void renderbufferStorage(int target, int internalformat, int width, int height) {
        glRenderbufferStorage(target, internalformat, width, height);
    }

    public void sampleCoverage(float value, boolean invert) {
        glSampleCoverage(value, invert);
    }

    public void shaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
        glShaderBinary(n,
                shaders,
                binaryformat,
                binary,
                length);
    }

    public void shaderSource(int shader, String string) {
        glShaderSource(shader, string);
    }

    public void stencilFuncSeparate(int face, int func, int ref, int mask) {
        glStencilFuncSeparate(face, func, ref, mask);
    }

    public void stencilMaskSeparate(int face, int mask) {
        glStencilMaskSeparate(face, mask);
    }

    public void stencilOpSeparate(int face, int fail, int zfail, int zpass) {
        glStencilOpSeparate(face, fail, zfail, zpass);
    }

    public void uniform1f(int location, float x) {
        glUniform1f(location, x);
    }

    public void uniform1fv(int location, int count, FloatBuffer v) {
        glUniform1fv(location, count, v);
    }

    public void uniform1i(int location, int x) {
        glUniform1i(location, x);
    }

    public void uniform1iv(int location, int count, IntBuffer v) {
        glUniform1iv(location, count, v);
    }

    public void uniform2f(int location, float x, float y) {
        glUniform2f(location, x, y);
    }

    public void uniform2fv(int location, int count, FloatBuffer v) {
        glUniform2fv(location, count, v);
    }

    public void uniform2i(int location, int x, int y) {
        glUniform2i(location, x, y);
    }

    public void uniform2iv(int location, int count, IntBuffer v) {
        glUniform2iv(location, count, v);
    }

    public void uniform3f(int location, float x, float y, float z) {
        glUniform3f(location, x, y, z);
    }

    public void uniform3fv(int location, int count, FloatBuffer v) {
        glUniform3fv(location, count, v);
    }

    public void uniform3i(int location, int x, int y, int z) {
        glUniform3i(location, x, y, z);
    }

    public void uniform3iv(int location, int count, IntBuffer v) {
        glUniform3iv(location, count, v);
    }

    public void uniform4f(int location, float x, float y, float z, float w) {
        glUniform4f(location, x, y, z, w);
    }

    public void uniform4fv(int location, int count, FloatBuffer v) {
        glUniform4fv(location, count, v);
    }

    public void uniform4i(int location, int x, int y, int z, int w) {
        glUniform4i(location, x, y, z, w);
    }

    public void uniform4iv(int location, int count, IntBuffer v) {
        glUniform4iv(location, count, v);
    }

    public void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value) {
        glUniformMatrix2fv(location, count, transpose, value);
    }

    public void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value) {
        glUniformMatrix3fv(location, count, transpose, value);
    }

    public void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value) {
        glUniformMatrix4fv(location, count, transpose, value);
    }

    public void useProgram(int program) {
        glUseProgram(program);
    }

    public void validateProgram(int program) {
        glValidateProgram(program);
    }

    public void vertexAttrib1f(int indx, float x) {
        glVertexAttrib1f(indx, x);
    }

    public void vertexAttrib1fv(int indx, FloatBuffer values) {
        glVertexAttrib1fv(indx, values);
    }

    public void vertexAttrib2f(int indx, float x, float y) {
        glVertexAttrib2f(indx, x, y);
    }

    public void vertexAttrib2fv(int indx, FloatBuffer values) {
        glVertexAttrib2fv(indx, values);
    }

    public void vertexAttrib3f(int indx, float x, float y, float z) {
        glVertexAttrib3f(indx, x, y, z);
    }

    public void vertexAttrib3fv(int indx, FloatBuffer values) {
        glVertexAttrib3fv(indx, values);
    }

    public void vertexAttrib4f(int indx, float x, float y, float z, float w) {
        glVertexAttrib4f(indx, x, y, z, w);
    }

    public void vertexAttrib4fv(int indx, FloatBuffer values) {
        glVertexAttrib4fv(indx, values);
    }

    public void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
                                    Buffer ptr) {
        glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    public void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
                                    int ptr) {
        glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }
}
