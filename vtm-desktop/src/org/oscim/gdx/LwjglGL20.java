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

package org.oscim.gdx;

import com.badlogic.gdx.utils.GdxRuntimeException;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.oscim.backend.GL;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * An implementation of the {@link GL20} interface based on LWJGL. Note that LWJGL shaders and OpenGL ES shaders will not be 100%
 * compatible. Some glGetXXX methods are not implemented.
 *
 * @author mzechner
 */
public class LwjglGL20 implements GL {
    private ByteBuffer buffer = null;
    private FloatBuffer floatBuffer = null;
    private IntBuffer intBuffer = null;

    private void ensureBufferCapacity(int numBytes) {
        if (buffer == null || buffer.capacity() < numBytes) {
            buffer = com.badlogic.gdx.utils.BufferUtils.newByteBuffer(numBytes);
            floatBuffer = buffer.asFloatBuffer();
            intBuffer = buffer.asIntBuffer();
        }
    }

    private FloatBuffer toFloatBuffer(float v[], int offset, int count) {
        ensureBufferCapacity(count << 2);
        floatBuffer.clear();
        com.badlogic.gdx.utils.BufferUtils.copy(v, floatBuffer, count, offset);
        return floatBuffer;
    }

    private IntBuffer toIntBuffer(int v[], int offset, int count) {
        ensureBufferCapacity(count << 2);
        intBuffer.clear();
        com.badlogic.gdx.utils.BufferUtils.copy(v, count, offset, intBuffer);
        return intBuffer;
    }

    public void activeTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    public void attachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    public void bindAttribLocation(int program, int index, String name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    public void bindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    public void bindFramebuffer(int target, int framebuffer) {
        EXTFramebufferObject.glBindFramebufferEXT(target, framebuffer);
    }

    public void bindRenderbuffer(int target, int renderbuffer) {
        EXTFramebufferObject.glBindRenderbufferEXT(target, renderbuffer);
    }

    public void bindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
    }

    public void blendColor(float red, float green, float blue, float alpha) {
        GL14.glBlendColor(red, green, blue, alpha);
    }

    public void blendEquation(int mode) {
        GL14.glBlendEquation(mode);
    }

    public void blendEquationSeparate(int modeRGB, int modeAlpha) {
        GL20.glBlendEquationSeparate(modeRGB, modeAlpha);
    }

    public void blendFunc(int sfactor, int dfactor) {
        GL11.glBlendFunc(sfactor, dfactor);
    }

    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    public void bufferData(int target, int size, Buffer data, int usage) {
        if (data == null)
            GL15.glBufferData(target, size, usage);
        else if (data instanceof ByteBuffer)
            GL15.glBufferData(target, (ByteBuffer) data, usage);
        else if (data instanceof IntBuffer)
            GL15.glBufferData(target, (IntBuffer) data, usage);
        else if (data instanceof FloatBuffer)
            GL15.glBufferData(target, (FloatBuffer) data, usage);
        else if (data instanceof DoubleBuffer)
            GL15.glBufferData(target, (DoubleBuffer) data, usage);
        else if (data instanceof ShortBuffer) //
            GL15.glBufferData(target, (ShortBuffer) data, usage);
    }

    public void bufferSubData(int target, int offset, int size, Buffer data) {
        if (data == null)
            throw new GdxRuntimeException("Using null for the data not possible, blame LWJGL");
        else if (data instanceof ByteBuffer)
            GL15.glBufferSubData(target, offset, (ByteBuffer) data);
        else if (data instanceof IntBuffer)
            GL15.glBufferSubData(target, offset, (IntBuffer) data);
        else if (data instanceof FloatBuffer)
            GL15.glBufferSubData(target, offset, (FloatBuffer) data);
        else if (data instanceof DoubleBuffer)
            GL15.glBufferSubData(target, offset, (DoubleBuffer) data);
        else if (data instanceof ShortBuffer) //
            GL15.glBufferSubData(target, offset, (ShortBuffer) data);
    }

    public int checkFramebufferStatus(int target) {
        return EXTFramebufferObject.glCheckFramebufferStatusEXT(target);
    }

    public void clear(int mask) {
        GL11.glClear(mask);
    }

    public void clearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    public void clearDepthf(float depth) {
        GL11.glClearDepth(depth);
    }

    public void clearStencil(int s) {
        GL11.glClearStencil(s);
    }

    public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    public void compileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    public void compressedTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                                     int imageSize, Buffer data) {
        if (data instanceof ByteBuffer) {
            GL13.glCompressedTexImage2D(target, level, internalformat, width, height, border, (ByteBuffer) data);
        } else {
            throw new GdxRuntimeException("Can't use " + data.getClass().getName() + " with this method. Use ByteBuffer instead.");
        }
    }

    public void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format,
                                        int imageSize, Buffer data) {
        throw new GdxRuntimeException("not implemented");
    }

    public void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border) {
        GL11.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }

    public void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    public int createProgram() {
        return GL20.glCreateProgram();
    }

    public int createShader(int type) {
        return GL20.glCreateShader(type);
    }

    public void cullFace(int mode) {
        GL11.glCullFace(mode);
    }

    public void deleteBuffers(int n, IntBuffer buffers) {
        GL15.glDeleteBuffers(buffers);
    }

    public void deleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    public void deleteFramebuffers(int n, IntBuffer framebuffers) {
        EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffers);
    }

    public void deleteFramebuffer(int framebuffer) {
        EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
    }

    public void deleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    public void deleteRenderbuffers(int n, IntBuffer renderbuffers) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(renderbuffers);
    }

    public void deleteRenderbuffer(int renderbuffer) {
        EXTFramebufferObject.glDeleteRenderbuffersEXT(renderbuffer);
    }

    public void deleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    public void deleteTextures(int n, IntBuffer textures) {
        GL11.glDeleteTextures(textures);
    }

    public void deleteTexture(int texture) {
        GL11.glDeleteTextures(texture);
    }

    public void depthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    public void depthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    public void depthRangef(float zNear, float zFar) {
        GL11.glDepthRange(zNear, zFar);
    }

    public void detachShader(int program, int shader) {
        GL20.glDetachShader(program, shader);
    }

    public void disable(int cap) {
        GL11.glDisable(cap);
    }

    public void disableVertexAttribArray(int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    public void drawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    public void drawElements(int mode, int count, int type, Buffer indices) {
        if (indices instanceof ShortBuffer && type == com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_SHORT)
            GL11.glDrawElements(mode, (ShortBuffer) indices);
        else if (indices instanceof ByteBuffer && type == com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_SHORT)
            GL11.glDrawElements(mode, ((ByteBuffer) indices).asShortBuffer()); // FIXME yay...
        else if (indices instanceof ByteBuffer && type == com.badlogic.gdx.graphics.GL20.GL_UNSIGNED_BYTE)
            GL11.glDrawElements(mode, (ByteBuffer) indices);
        else
            throw new GdxRuntimeException("Can't use " + indices.getClass().getName()
                    + " with this method. Use ShortBuffer or ByteBuffer instead. Blame LWJGL");
    }

    public void enable(int cap) {
        GL11.glEnable(cap);
    }

    public void enableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    public void finish() {
        GL11.glFinish();
    }

    public void flush() {
        GL11.glFlush();
    }

    public void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        EXTFramebufferObject.glFramebufferRenderbufferEXT(target, attachment, renderbuffertarget, renderbuffer);
    }

    public void framebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
    }

    public void frontFace(int mode) {
        GL11.glFrontFace(mode);
    }

    public void genBuffers(int n, IntBuffer buffers) {
        GL15.glGenBuffers(buffers);
    }

    public int genBuffer() {
        return GL15.glGenBuffers();
    }

    public void genFramebuffers(int n, IntBuffer framebuffers) {
        EXTFramebufferObject.glGenFramebuffersEXT(framebuffers);
    }

    public int genFramebuffer() {
        return EXTFramebufferObject.glGenFramebuffersEXT();
    }

    public void genRenderbuffers(int n, IntBuffer renderbuffers) {
        EXTFramebufferObject.glGenRenderbuffersEXT(renderbuffers);
    }

    public int genRenderbuffer() {
        return EXTFramebufferObject.glGenRenderbuffersEXT();
    }

    public void genTextures(int n, IntBuffer textures) {
        GL11.glGenTextures(textures);
    }

    public int genTexture() {
        return GL11.glGenTextures();
    }

    public void generateMipmap(int target) {
        EXTFramebufferObject.glGenerateMipmapEXT(target);
    }

    public String getActiveAttrib(int program, int index, IntBuffer size, Buffer type) {
        // FIXME this is less than ideal of course...
        IntBuffer typeTmp = BufferUtils.createIntBuffer(2);
        String name = GL20.glGetActiveAttrib(program, index, 256, typeTmp);
        size.put(typeTmp.get(0));
        if (type instanceof IntBuffer) ((IntBuffer) type).put(typeTmp.get(1));
        return name;
    }

    public String getActiveUniform(int program, int index, IntBuffer size, Buffer type) {
        // FIXME this is less than ideal of course...
        IntBuffer typeTmp = BufferUtils.createIntBuffer(2);
        String name = GL20.glGetActiveUniform(program, index, 256, typeTmp);
        size.put(typeTmp.get(0));
        if (type instanceof IntBuffer) ((IntBuffer) type).put(typeTmp.get(1));
        return name;
    }

    public void getAttachedShaders(int program, int maxcount, Buffer count, IntBuffer shaders) {
        GL20.glGetAttachedShaders(program, (IntBuffer) count, shaders);
    }

    public int getAttribLocation(int program, String name) {
        return GL20.glGetAttribLocation(program, name);
    }

    public void getBooleanv(int pname, Buffer params) {
        GL11.glGetBoolean(pname, (ByteBuffer) params);
    }

    public void getBufferParameteriv(int target, int pname, IntBuffer params) {
        GL15.glGetBufferParameter(target, pname, params);
    }

    public int getError() {
        return GL11.glGetError();
    }

    public void getFloatv(int pname, FloatBuffer params) {
        GL11.glGetFloat(pname, params);
    }

    public void getFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntBuffer params) {
        EXTFramebufferObject.glGetFramebufferAttachmentParameterEXT(target, attachment, pname, params);
    }

    public void getIntegerv(int pname, IntBuffer params) {
        GL11.glGetInteger(pname, params);
    }

    public String getProgramInfoLog(int program) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        GL20.glGetProgramInfoLog(program, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }

    public void getProgramiv(int program, int pname, IntBuffer params) {
        GL20.glGetProgram(program, pname, params);
    }

    public void getRenderbufferParameteriv(int target, int pname, IntBuffer params) {
        EXTFramebufferObject.glGetRenderbufferParameterEXT(target, pname, params);
    }

    public String getShaderInfoLog(int shader) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 10);
        buffer.order(ByteOrder.nativeOrder());
        ByteBuffer tmp = ByteBuffer.allocateDirect(4);
        tmp.order(ByteOrder.nativeOrder());
        IntBuffer intBuffer = tmp.asIntBuffer();

        GL20.glGetShaderInfoLog(shader, intBuffer, buffer);
        int numBytes = intBuffer.get(0);
        byte[] bytes = new byte[numBytes];
        buffer.get(bytes);
        return new String(bytes);
    }

    public void getShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range, IntBuffer precision) {
        throw new UnsupportedOperationException("unsupported, won't implement");
    }

    public void getShaderiv(int shader, int pname, IntBuffer params) {
        GL20.glGetShader(shader, pname, params);
    }

    public String getString(int name) {
        return GL11.glGetString(name);
    }

    public void getTexParameterfv(int target, int pname, FloatBuffer params) {
        GL11.glGetTexParameter(target, pname, params);
    }

    public void getTexParameteriv(int target, int pname, IntBuffer params) {
        GL11.glGetTexParameter(target, pname, params);
    }

    public int getUniformLocation(int program, String name) {
        return GL20.glGetUniformLocation(program, name);
    }

    public void getUniformfv(int program, int location, FloatBuffer params) {
        GL20.glGetUniform(program, location, params);
    }

    public void getUniformiv(int program, int location, IntBuffer params) {
        GL20.glGetUniform(program, location, params);
    }

    public void getVertexAttribPointerv(int index, int pname, Buffer pointer) {
        throw new UnsupportedOperationException("unsupported, won't implement");
    }

    public void getVertexAttribfv(int index, int pname, FloatBuffer params) {
        GL20.glGetVertexAttrib(index, pname, params);
    }

    public void getVertexAttribiv(int index, int pname, IntBuffer params) {
        GL20.glGetVertexAttrib(index, pname, params);
    }

    public void hint(int target, int mode) {
        GL11.glHint(target, mode);
    }

    public boolean isBuffer(int buffer) {
        return GL15.glIsBuffer(buffer);
    }

    public boolean isEnabled(int cap) {
        return GL11.glIsEnabled(cap);
    }

    public boolean isFramebuffer(int framebuffer) {
        return EXTFramebufferObject.glIsFramebufferEXT(framebuffer);
    }

    public boolean isProgram(int program) {
        return GL20.glIsProgram(program);
    }

    public boolean isRenderbuffer(int renderbuffer) {
        return EXTFramebufferObject.glIsRenderbufferEXT(renderbuffer);
    }

    public boolean isShader(int shader) {
        return GL20.glIsShader(shader);
    }

    public boolean isTexture(int texture) {
        return GL11.glIsTexture(texture);
    }

    public void lineWidth(float width) {
        GL11.glLineWidth(width);
    }

    public void linkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    public void pixelStorei(int pname, int param) {
        GL11.glPixelStorei(pname, param);
    }

    public void polygonOffset(float factor, float units) {
        GL11.glPolygonOffset(factor, units);
    }

    public void readPixels(int x, int y, int width, int height, int format, int type, Buffer pixels) {
        if (pixels instanceof ByteBuffer)
            GL11.glReadPixels(x, y, width, height, format, type, (ByteBuffer) pixels);
        else if (pixels instanceof ShortBuffer)
            GL11.glReadPixels(x, y, width, height, format, type, (ShortBuffer) pixels);
        else if (pixels instanceof IntBuffer)
            GL11.glReadPixels(x, y, width, height, format, type, (IntBuffer) pixels);
        else if (pixels instanceof FloatBuffer)
            GL11.glReadPixels(x, y, width, height, format, type, (FloatBuffer) pixels);
        else
            throw new GdxRuntimeException("Can't use " + pixels.getClass().getName()
                    + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer or FloatBuffer instead. Blame LWJGL");
    }

    public void releaseShaderCompiler() {
        // nothing to do here
    }

    public void renderbufferStorage(int target, int internalformat, int width, int height) {
        EXTFramebufferObject.glRenderbufferStorageEXT(target, internalformat, width, height);
    }

    public void sampleCoverage(float value, boolean invert) {
        GL13.glSampleCoverage(value, invert);
    }

    public void scissor(int x, int y, int width, int height) {
        GL11.glScissor(x, y, width, height);
    }

    public void shaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
        throw new UnsupportedOperationException("unsupported, won't implement");
    }

    public void shaderSource(int shader, String string) {
        GL20.glShaderSource(shader, string);
    }

    public void stencilFunc(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    public void stencilFuncSeparate(int face, int func, int ref, int mask) {
        GL20.glStencilFuncSeparate(face, func, ref, mask);
    }

    public void stencilMask(int mask) {
        GL11.glStencilMask(mask);
    }

    public void stencilMaskSeparate(int face, int mask) {
        GL20.glStencilMaskSeparate(face, mask);
    }

    public void stencilOp(int fail, int zfail, int zpass) {
        GL11.glStencilOp(fail, zfail, zpass);
    }

    public void stencilOpSeparate(int face, int fail, int zfail, int zpass) {
        GL20.glStencilOpSeparate(face, fail, zfail, zpass);
    }

    public void texImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type,
                           Buffer pixels) {
        if (pixels == null)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer) null);
        else if (pixels instanceof ByteBuffer)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ByteBuffer) pixels);
        else if (pixels instanceof ShortBuffer)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (ShortBuffer) pixels);
        else if (pixels instanceof IntBuffer)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (IntBuffer) pixels);
        else if (pixels instanceof FloatBuffer)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (FloatBuffer) pixels);
        else if (pixels instanceof DoubleBuffer)
            GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, (DoubleBuffer) pixels);
        else
            throw new GdxRuntimeException("Can't use " + pixels.getClass().getName()
                    + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer or DoubleBuffer instead. Blame LWJGL");
    }

    public void texParameterf(int target, int pname, float param) {
        GL11.glTexParameterf(target, pname, param);
    }

    public void texParameterfv(int target, int pname, FloatBuffer params) {
        GL11.glTexParameter(target, pname, params);
    }

    public void texParameteri(int target, int pname, int param) {
        GL11.glTexParameteri(target, pname, param);
    }

    public void texParameteriv(int target, int pname, IntBuffer params) {
        GL11.glTexParameter(target, pname, params);
    }

    public void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type,
                              Buffer pixels) {
        if (pixels instanceof ByteBuffer)
            GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (ByteBuffer) pixels);
        else if (pixels instanceof ShortBuffer)
            GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (ShortBuffer) pixels);
        else if (pixels instanceof IntBuffer)
            GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (IntBuffer) pixels);
        else if (pixels instanceof FloatBuffer)
            GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (FloatBuffer) pixels);
        else if (pixels instanceof DoubleBuffer)
            GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, (DoubleBuffer) pixels);
        else
            throw new GdxRuntimeException("Can't use " + pixels.getClass().getName()
                    + " with this method. Use ByteBuffer, ShortBuffer, IntBuffer, FloatBuffer or DoubleBuffer instead. Blame LWJGL");
    }

    public void uniform1f(int location, float x) {
        GL20.glUniform1f(location, x);
    }

    public void uniform1fv(int location, int count, FloatBuffer v) {
        GL20.glUniform1(location, v);
    }

    public void uniform1fv(int location, int count, float[] v, int offset) {
        GL20.glUniform1(location, toFloatBuffer(v, offset, count));
    }

    public void uniform1i(int location, int x) {
        GL20.glUniform1i(location, x);
    }

    public void uniform1iv(int location, int count, IntBuffer v) {
        GL20.glUniform1(location, v);
    }

    public void uniform1iv(int location, int count, int[] v, int offset) {
        GL20.glUniform1(location, toIntBuffer(v, offset, count));
    }

    public void uniform2f(int location, float x, float y) {
        GL20.glUniform2f(location, x, y);
    }

    public void uniform2fv(int location, int count, FloatBuffer v) {
        GL20.glUniform2(location, v);
    }

    public void uniform2fv(int location, int count, float[] v, int offset) {
        GL20.glUniform2(location, toFloatBuffer(v, offset, count << 1));
    }

    public void uniform2i(int location, int x, int y) {
        GL20.glUniform2i(location, x, y);
    }

    public void uniform2iv(int location, int count, IntBuffer v) {
        GL20.glUniform2(location, v);
    }

    public void uniform2iv(int location, int count, int[] v, int offset) {
        GL20.glUniform2(location, toIntBuffer(v, offset, count << 1));
    }

    public void uniform3f(int location, float x, float y, float z) {
        GL20.glUniform3f(location, x, y, z);
    }

    public void uniform3fv(int location, int count, FloatBuffer v) {
        GL20.glUniform3(location, v);
    }

    public void uniform3fv(int location, int count, float[] v, int offset) {
        GL20.glUniform3(location, toFloatBuffer(v, offset, count * 3));
    }

    public void uniform3i(int location, int x, int y, int z) {
        GL20.glUniform3i(location, x, y, z);
    }

    public void uniform3iv(int location, int count, IntBuffer v) {
        GL20.glUniform3(location, v);
    }

    public void uniform3iv(int location, int count, int[] v, int offset) {
        GL20.glUniform3(location, toIntBuffer(v, offset, count * 3));
    }

    public void uniform4f(int location, float x, float y, float z, float w) {
        GL20.glUniform4f(location, x, y, z, w);
    }

    public void uniform4fv(int location, int count, FloatBuffer v) {
        GL20.glUniform4(location, v);
    }

    public void uniform4fv(int location, int count, float[] v, int offset) {
        GL20.glUniform4(location, toFloatBuffer(v, offset, count << 2));
    }

    public void uniform4i(int location, int x, int y, int z, int w) {
        GL20.glUniform4i(location, x, y, z, w);
    }

    public void uniform4iv(int location, int count, IntBuffer v) {
        GL20.glUniform4(location, v);
    }

    public void uniform4iv(int location, int count, int[] v, int offset) {
        GL20.glUniform4(location, toIntBuffer(v, offset, count << 2));
    }

    public void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix2(location, transpose, value);
    }

    public void uniformMatrix2fv(int location, int count, boolean transpose, float[] value, int offset) {
        GL20.glUniformMatrix2(location, transpose, toFloatBuffer(value, offset, count << 2));
    }

    public void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix3(location, transpose, value);
    }

    public void uniformMatrix3fv(int location, int count, boolean transpose, float[] value, int offset) {
        GL20.glUniformMatrix3(location, transpose, toFloatBuffer(value, offset, count * 9));
    }

    public void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix4(location, transpose, value);
    }

    public void uniformMatrix4fv(int location, int count, boolean transpose, float[] value, int offset) {
        GL20.glUniformMatrix4(location, transpose, toFloatBuffer(value, offset, count << 4));
    }

    public void useProgram(int program) {
        GL20.glUseProgram(program);
    }

    public void validateProgram(int program) {
        GL20.glValidateProgram(program);
    }

    public void vertexAttrib1f(int indx, float x) {
        GL20.glVertexAttrib1f(indx, x);
    }

    public void vertexAttrib1fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib1f(indx, values.get());
    }

    public void vertexAttrib2f(int indx, float x, float y) {
        GL20.glVertexAttrib2f(indx, x, y);
    }

    public void vertexAttrib2fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib2f(indx, values.get(), values.get());
    }

    public void vertexAttrib3f(int indx, float x, float y, float z) {
        GL20.glVertexAttrib3f(indx, x, y, z);
    }

    public void vertexAttrib3fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib3f(indx, values.get(), values.get(), values.get());
    }

    public void vertexAttrib4f(int indx, float x, float y, float z, float w) {
        GL20.glVertexAttrib4f(indx, x, y, z, w);
    }

    public void vertexAttrib4fv(int indx, FloatBuffer values) {
        GL20.glVertexAttrib4f(indx, values.get(), values.get(), values.get(), values.get());
    }

    public void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            if (type == BYTE)
                GL20.glVertexAttribPointer(indx, size, false, normalized, stride, (ByteBuffer) buffer);
            else if (type == UNSIGNED_BYTE)
                GL20.glVertexAttribPointer(indx, size, true, normalized, stride, (ByteBuffer) buffer);
            else if (type == SHORT)
                GL20.glVertexAttribPointer(indx, size, false, normalized, stride, ((ByteBuffer) buffer).asShortBuffer());
            else if (type == UNSIGNED_SHORT)
                GL20.glVertexAttribPointer(indx, size, true, normalized, stride, ((ByteBuffer) buffer).asShortBuffer());
            else if (type == FLOAT)
                GL20.glVertexAttribPointer(indx, size, normalized, stride, ((ByteBuffer) buffer).asFloatBuffer());
            else
                throw new GdxRuntimeException(
                        "Can't use "
                                + buffer.getClass().getName()
                                + " with type "
                                + type
                                + " with this method. Use ByteBuffer and one of GL_BYTE, GL_UNSIGNED_BYTE, GL_SHORT, GL_UNSIGNED_SHORT or GL_FLOAT for type. Blame LWJGL");
        } else if (buffer instanceof FloatBuffer) {
            if (type == FLOAT)
                GL20.glVertexAttribPointer(indx, size, normalized, stride, (FloatBuffer) buffer);
            else
                throw new GdxRuntimeException("Can't use " + buffer.getClass().getName() + " with type " + type
                        + " with this method.");
        } else
            throw new GdxRuntimeException("Can't use " + buffer.getClass().getName()
                    + " with this method. Use ByteBuffer instead. Blame LWJGL");
    }

    public void viewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    public void drawElements(int mode, int count, int type, int indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }

    public void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride, int ptr) {
        GL20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
    }

    public void getShaderSource(int shader, int bufsize, Buffer length, String source) {
    }
}
