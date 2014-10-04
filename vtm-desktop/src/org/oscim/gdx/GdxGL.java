package org.oscim.gdx;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import static com.badlogic.jglfw.utils.Memory.getPosition;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.oscim.backend.GL;

public class GdxGL implements GL {
	public void activeTexture(int texture) {
		com.badlogic.jglfw.gl.GL.glActiveTexture(texture);
	}

	public void bindTexture(int target, int texture) {
		com.badlogic.jglfw.gl.GL.glBindTexture(target, texture);
	}

	public void blendFunc(int sfactor, int dfactor) {
		com.badlogic.jglfw.gl.GL.glBlendFunc(sfactor, dfactor);
	}

	public void clear(int mask) {
		com.badlogic.jglfw.gl.GL.glClear(mask);
	}

	public void clearColor(float red, float green, float blue, float alpha) {
		com.badlogic.jglfw.gl.GL.glClearColor(red, green, blue, alpha);
	}

	public void clearDepthf(float depth) {
		com.badlogic.jglfw.gl.GL.glClearDepthf(depth);
	}

	public void clearStencil(int s) {
		com.badlogic.jglfw.gl.GL.glClearStencil(s);
	}

	public void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		com.badlogic.jglfw.gl.GL.glColorMask(red, green, blue, alpha);
	}

	public void compressedTexImage2D(int target, int level, int internalformat, int width,
	        int height, int border,
	        int imageSize, Buffer data) {
		com.badlogic.jglfw.gl.GL.glCompressedTexImage2D(
		                                                target,
		                                                level,
		                                                internalformat,
		                                                width,
		                                                height,
		                                                border,
		                                                imageSize,
		                                                data,
		                                                getPosition(data));
	}

	public void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset,
	        int width, int height, int format,
	        int imageSize, Buffer data) {
		com.badlogic.jglfw.gl.GL.glCompressedTexSubImage2D(
		                                                   target,
		                                                   level,
		                                                   xoffset,
		                                                   yoffset,
		                                                   width,
		                                                   height,
		                                                   format,
		                                                   imageSize,
		                                                   data,
		                                                   getPosition(data));
	}

	public void copyTexImage2D(int target, int level, int internalformat, int x, int y,
	        int width, int height, int border) {
		com.badlogic.jglfw.gl.GL.glCopyTexImage2D(
		                                          target,
		                                          level,
		                                          internalformat,
		                                          x,
		                                          y,
		                                          width,
		                                          height,
		                                          border);
	}

	public void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y,
	        int width, int height) {
		com.badlogic.jglfw.gl.GL.glCopyTexSubImage2D(
		                                             target,
		                                             level,
		                                             xoffset,
		                                             yoffset,
		                                             x,
		                                             y,
		                                             width,
		                                             height);
	}

	public void cullFace(int mode) {
		com.badlogic.jglfw.gl.GL.glCullFace(mode);
	}

	public void deleteTextures(int n, IntBuffer textures) {
		com.badlogic.jglfw.gl.GL.glDeleteTextures(n, textures, getPosition(textures));
	}

	public void depthFunc(int func) {
		com.badlogic.jglfw.gl.GL.glDepthFunc(func);
	}

	public void depthMask(boolean flag) {
		com.badlogic.jglfw.gl.GL.glDepthMask(flag);
	}

	public void depthRangef(float zNear, float zFar) {
		com.badlogic.jglfw.gl.GL.glDepthRangef(zNear, zFar);
	}

	public void disable(int cap) {
		com.badlogic.jglfw.gl.GL.glDisable(cap);
	}

	public void drawArrays(int mode, int first, int count) {
		com.badlogic.jglfw.gl.GL.glDrawArrays(mode, first, count);
	}

	public void drawElements(int mode, int count, int type, Buffer indices) {
		com.badlogic.jglfw.gl.GL.glDrawElements(mode, count, type, indices, getPosition(indices));
	}

	public void enable(int cap) {
		com.badlogic.jglfw.gl.GL.glEnable(cap);
	}

	public void finish() {
		com.badlogic.jglfw.gl.GL.glFinish();
	}

	public void flush() {
		com.badlogic.jglfw.gl.GL.glFlush();
	}

	public void frontFace(int mode) {
		com.badlogic.jglfw.gl.GL.glFrontFace(mode);
	}

	public void genTextures(int n, IntBuffer textures) {
		com.badlogic.jglfw.gl.GL.glGenTextures(n, textures, getPosition(textures));
	}

	public int getError() {
		return com.badlogic.jglfw.gl.GL.glGetError();
	}

	public void getIntegerv(int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetIntegerv(pname, params, getPosition(params));
	}

	public String getString(int name) {
		return com.badlogic.jglfw.gl.GL.glGetString(name);
	}

	public void hint(int target, int mode) {
		com.badlogic.jglfw.gl.GL.glHint(target, mode);
	}

	public void lineWidth(float width) {
		com.badlogic.jglfw.gl.GL.glLineWidth(width);
	}

	public void pixelStorei(int pname, int param) {
		com.badlogic.jglfw.gl.GL.glPixelStorei(pname, param);
	}

	public void polygonOffset(float factor, float units) {
		com.badlogic.jglfw.gl.GL.glPolygonOffset(factor, units);
	}

	public void readPixels(int x, int y, int width, int height, int format, int type,
	        Buffer pixels) {
		com.badlogic.jglfw.gl.GL.glReadPixels(
		                                      x,
		                                      y,
		                                      width,
		                                      height,
		                                      format,
		                                      type,
		                                      pixels,
		                                      getPosition(pixels));
	}

	public void scissor(int x, int y, int width, int height) {
		com.badlogic.jglfw.gl.GL.glScissor(x, y, width, height);
	}

	public void stencilFunc(int func, int ref, int mask) {
		com.badlogic.jglfw.gl.GL.glStencilFunc(func, ref, mask);
	}

	public void stencilMask(int mask) {
		com.badlogic.jglfw.gl.GL.glStencilMask(mask);
	}

	public void stencilOp(int fail, int zfail, int zpass) {
		com.badlogic.jglfw.gl.GL.glStencilOp(fail, zfail, zpass);
	}

	public void texImage2D(int target, int level, int internalFormat, int width, int height,
	        int border, int format, int type,
	        Buffer pixels) {
		com.badlogic.jglfw.gl.GL.glTexImage2D(
		                                      target,
		                                      level,
		                                      internalFormat,
		                                      width,
		                                      height,
		                                      border,
		                                      format,
		                                      type,
		                                      pixels,
		                                      getPosition(pixels));
	}

	public void texParameterf(int target, int pname, float param) {
		com.badlogic.jglfw.gl.GL.glTexParameterf(target, pname, param);
	}

	public void texSubImage2D(int target, int level, int xoffset, int yoffset, int width,
	        int height, int format, int type,
	        Buffer pixels) {
		com.badlogic.jglfw.gl.GL.glTexSubImage2D(
		                                         target,
		                                         level,
		                                         xoffset,
		                                         yoffset,
		                                         width,
		                                         height,
		                                         format,
		                                         type,
		                                         pixels,
		                                         getPosition(pixels));
	}

	public void viewport(int x, int y, int width, int height) {
		com.badlogic.jglfw.gl.GL.glViewport(x, y, width, height);
	}

	public void getFloatv(int pname, FloatBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetFloatv(pname, params, getPosition(params));
	}

	public void getTexParameterfv(int target, int pname, FloatBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetTexParameterfv(target, pname, params, getPosition(params));
	}

	public void texParameterfv(int target, int pname, FloatBuffer params) {
		com.badlogic.jglfw.gl.GL.glTexParameterfv(target, pname, params, getPosition(params));
	}

	public void bindBuffer(int target, int buffer) {
		com.badlogic.jglfw.gl.GL.glBindBuffer(target, buffer);
	}

	public void bufferData(int target, int size, Buffer data, int usage) {
		com.badlogic.jglfw.gl.GL.glBufferData(target, size, data, getPosition(data), usage);
	}

	public void bufferSubData(int target, int offset, int size, Buffer data) {
		com.badlogic.jglfw.gl.GL.glBufferSubData(target, offset, size, data, getPosition(data));
	}

	public void deleteBuffers(int n, IntBuffer buffers) {
		com.badlogic.jglfw.gl.GL.glDeleteBuffers(n, buffers, getPosition(buffers));
	}

	public void getBufferParameteriv(int target, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetBufferParameteriv(target, pname, params, getPosition(params));
	}

	public void genBuffers(int n, IntBuffer buffers) {
		com.badlogic.jglfw.gl.GL.glGenBuffers(n, buffers, getPosition(buffers));
	}

	public void getTexParameteriv(int target, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetTexParameteriv(target, pname, params, getPosition(params));
	}

	public boolean isBuffer(int buffer) {
		return com.badlogic.jglfw.gl.GL.glIsBuffer(buffer);
	}

	public boolean isEnabled(int cap) {
		return com.badlogic.jglfw.gl.GL.glIsEnabled(cap);
	}

	public boolean isTexture(int texture) {
		return com.badlogic.jglfw.gl.GL.glIsTexture(texture);
	}

	public void texParameteri(int target, int pname, int param) {
		com.badlogic.jglfw.gl.GL.glTexParameteri(target, pname, param);
	}

	public void texParameteriv(int target, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glTexParameteriv(target, pname, params, getPosition(params));
	}

	public void drawElements(int mode, int count, int type, int indices) {
		com.badlogic.jglfw.gl.GL.glDrawElements(mode, count, type, indices);
	}

	public void attachShader(int program, int shader) {
		com.badlogic.jglfw.gl.GL.glAttachShader(program, shader);
	}

	public void bindAttribLocation(int program, int index, String name) {
		com.badlogic.jglfw.gl.GL.glBindAttribLocation(program, index, name);
	}

	public void bindFramebuffer(int target, int framebuffer) {
		com.badlogic.jglfw.gl.GL.glBindFramebufferEXT(target, framebuffer);
	}

	public void bindRenderbuffer(int target, int renderbuffer) {
		com.badlogic.jglfw.gl.GL.glBindRenderbufferEXT(target, renderbuffer);
	}

	public void blendColor(float red, float green, float blue, float alpha) {
		com.badlogic.jglfw.gl.GL.glBlendColor(red, green, blue, alpha);
	}

	public void blendEquation(int mode) {
		com.badlogic.jglfw.gl.GL.glBlendEquation(mode);
	}

	public void blendEquationSeparate(int modeRGB, int modeAlpha) {
		com.badlogic.jglfw.gl.GL.glBlendEquationSeparate(modeRGB, modeAlpha);
	}

	public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
		com.badlogic.jglfw.gl.GL.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
	}

	public int checkFramebufferStatus(int target) {
		return com.badlogic.jglfw.gl.GL.glCheckFramebufferStatusEXT(target);
	}

	public void compileShader(int shader) {
		com.badlogic.jglfw.gl.GL.glCompileShader(shader);
	}

	public int createProgram() {
		return com.badlogic.jglfw.gl.GL.glCreateProgram();
	}

	public int createShader(int type) {
		return com.badlogic.jglfw.gl.GL.glCreateShader(type);
	}

	public void deleteFramebuffers(int n, IntBuffer framebuffers) {
		com.badlogic.jglfw.gl.GL
		    .glDeleteFramebuffersEXT(n, framebuffers, getPosition(framebuffers));
	}

	public void deleteProgram(int program) {
		com.badlogic.jglfw.gl.GL.glDeleteProgram(program);
	}

	public void deleteRenderbuffers(int n, IntBuffer renderbuffers) {
		com.badlogic.jglfw.gl.GL.glDeleteRenderbuffersEXT(
		                                                  n,
		                                                  renderbuffers,
		                                                  getPosition(renderbuffers));
	}

	public void deleteShader(int shader) {
		com.badlogic.jglfw.gl.GL.glDeleteShader(shader);
	}

	public void detachShader(int program, int shader) {
		com.badlogic.jglfw.gl.GL.glDetachShader(program, shader);
	}

	public void disableVertexAttribArray(int index) {
		com.badlogic.jglfw.gl.GL.glDisableVertexAttribArray(index);
	}

	public void enableVertexAttribArray(int index) {
		com.badlogic.jglfw.gl.GL.glEnableVertexAttribArray(index);
	}

	public void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget,
	        int renderbuffer) {
		com.badlogic.jglfw.gl.GL.glFramebufferRenderbufferEXT(
		                                                      target,
		                                                      attachment,
		                                                      renderbuffertarget,
		                                                      renderbuffer);
	}

	public void framebufferTexture2D(int target, int attachment, int textarget, int texture,
	        int level) {
		com.badlogic.jglfw.gl.GL.glFramebufferTexture2DEXT(
		                                                   target,
		                                                   attachment,
		                                                   textarget,
		                                                   texture,
		                                                   level);
	}

	public void generateMipmap(int target) {
		com.badlogic.jglfw.gl.GL.glGenerateMipmapEXT(target);
	}

	public void genFramebuffers(int n, IntBuffer framebuffers) {
		com.badlogic.jglfw.gl.GL.glGenFramebuffersEXT(n, framebuffers, getPosition(framebuffers));
	}

	public void genRenderbuffers(int n, IntBuffer renderbuffers) {
		com.badlogic.jglfw.gl.GL
		    .glGenRenderbuffersEXT(n, renderbuffers, getPosition(renderbuffers));
	}

	public String getActiveAttrib(int program, int index, IntBuffer size, Buffer type) {
		return com.badlogic.jglfw.gl.GL.glGetActiveAttrib(
		                                                  program,
		                                                  index,
		                                                  size,
		                                                  getPosition(size),
		                                                  type,
		                                                  getPosition(type));
	}

	public String getActiveUniform(int program, int index, IntBuffer size, Buffer type) {
		return com.badlogic.jglfw.gl.GL.glGetActiveUniform(
		                                                   program,
		                                                   index,
		                                                   size,
		                                                   getPosition(size),
		                                                   type,
		                                                   getPosition(type));
	}

	public void getAttachedShaders(int program, int maxcount, Buffer count, IntBuffer shaders) {
		com.badlogic.jglfw.gl.GL.glGetAttachedShaders(
		                                              program,
		                                              maxcount,
		                                              count,
		                                              getPosition(count),
		                                              shaders,
		                                              getPosition(shaders));
	}

	public int getAttribLocation(int program, String name) {
		return com.badlogic.jglfw.gl.GL.glGetAttribLocation(program, name);
	}

	public void getBooleanv(int pname, Buffer params) {
		com.badlogic.jglfw.gl.GL.glGetBooleanv(pname, params, getPosition(params));
	}

	public void getFramebufferAttachmentParameteriv(int target, int attachment, int pname,
	        IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetFramebufferAttachmentParameterivEXT(
		                                                                  target,
		                                                                  attachment,
		                                                                  pname,
		                                                                  params,
		                                                                  getPosition(params));
	}

	public void getProgramiv(int program, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetProgramiv(program, pname, params, getPosition(params));
	}

	public String getProgramInfoLog(int program) {
		return com.badlogic.jglfw.gl.GL.glGetProgramInfoLog(program);
	}

	public void getRenderbufferParameteriv(int target, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetRenderbufferParameterivEXT(
		                                                         target,
		                                                         pname,
		                                                         params,
		                                                         getPosition(params));
	}

	public void getShaderiv(int shader, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetShaderiv(shader, pname, params, getPosition(params));
	}

	public String getShaderInfoLog(int shader) {
		return com.badlogic.jglfw.gl.GL.glGetShaderInfoLog(shader);
	}

	public void getShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range,
	        IntBuffer precision) {
		com.badlogic.jglfw.gl.GL.glGetShaderPrecisionFormat(
		                                                    shadertype,
		                                                    precisiontype,
		                                                    range,
		                                                    getPosition(range),
		                                                    precision,
		                                                    getPosition(precision));
	}

	public void getShaderSource(int shader, int bufsize, Buffer length, String source) {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void getUniformfv(int program, int location, FloatBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetUniformfv(program, location, params, getPosition(params));
	}

	public void getUniformiv(int program, int location, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetUniformiv(program, location, params, getPosition(params));
	}

	public int getUniformLocation(int program, String name) {
		return com.badlogic.jglfw.gl.GL.glGetUniformLocation(program, name);
	}

	public void getVertexAttribfv(int index, int pname, FloatBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetVertexAttribfv(index, pname, params, getPosition(params));
	}

	public void getVertexAttribiv(int index, int pname, IntBuffer params) {
		com.badlogic.jglfw.gl.GL.glGetVertexAttribiv(index, pname, params, getPosition(params));
	}

	public void getVertexAttribPointerv(int index, int pname, Buffer pointer) {
		com.badlogic.jglfw.gl.GL.glGetVertexAttribPointerv(
		                                                   index,
		                                                   pname,
		                                                   pointer,
		                                                   getPosition(pointer));
	}

	public boolean isFramebuffer(int framebuffer) {
		return com.badlogic.jglfw.gl.GL.glIsFramebufferEXT(framebuffer);
	}

	public boolean isProgram(int program) {
		return com.badlogic.jglfw.gl.GL.glIsProgram(program);
	}

	public boolean isRenderbuffer(int renderbuffer) {
		return com.badlogic.jglfw.gl.GL.glIsRenderbufferEXT(renderbuffer);
	}

	public boolean isShader(int shader) {
		return com.badlogic.jglfw.gl.GL.glIsShader(shader);
	}

	public void linkProgram(int program) {
		com.badlogic.jglfw.gl.GL.glLinkProgram(program);
	}

	public void releaseShaderCompiler() {
		com.badlogic.jglfw.gl.GL.glReleaseShaderCompiler();
	}

	public void renderbufferStorage(int target, int internalformat, int width, int height) {
		com.badlogic.jglfw.gl.GL.glRenderbufferStorageEXT(target, internalformat, width, height);
	}

	public void sampleCoverage(float value, boolean invert) {
		com.badlogic.jglfw.gl.GL.glSampleCoverage(value, invert);
	}

	public void shaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
		com.badlogic.jglfw.gl.GL.glShaderBinary(
		                                        n,
		                                        shaders,
		                                        getPosition(shaders),
		                                        binaryformat,
		                                        binary,
		                                        getPosition(binary),
		                                        length);
	}

	public void shaderSource(int shader, String string) {
		com.badlogic.jglfw.gl.GL.glShaderSource(shader, string);
	}

	public void stencilFuncSeparate(int face, int func, int ref, int mask) {
		com.badlogic.jglfw.gl.GL.glStencilFuncSeparate(face, func, ref, mask);
	}

	public void stencilMaskSeparate(int face, int mask) {
		com.badlogic.jglfw.gl.GL.glStencilMaskSeparate(face, mask);
	}

	public void stencilOpSeparate(int face, int fail, int zfail, int zpass) {
		com.badlogic.jglfw.gl.GL.glStencilOpSeparate(face, fail, zfail, zpass);
	}

	public void uniform1f(int location, float x) {
		com.badlogic.jglfw.gl.GL.glUniform1f(location, x);
	}

	public void uniform1fv(int location, int count, FloatBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform1fv(location, count, v, getPosition(v));
	}

	public void uniform1i(int location, int x) {
		com.badlogic.jglfw.gl.GL.glUniform1i(location, x);
	}

	public void uniform1iv(int location, int count, IntBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform1iv(location, count, v, getPosition(v));
	}

	public void uniform2f(int location, float x, float y) {
		com.badlogic.jglfw.gl.GL.glUniform2f(location, x, y);
	}

	public void uniform2fv(int location, int count, FloatBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform2fv(location, count, v, getPosition(v));
	}

	public void uniform2i(int location, int x, int y) {
		com.badlogic.jglfw.gl.GL.glUniform2i(location, x, y);
	}

	public void uniform2iv(int location, int count, IntBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform2iv(location, count, v, getPosition(v));
	}

	public void uniform3f(int location, float x, float y, float z) {
		com.badlogic.jglfw.gl.GL.glUniform3f(location, x, y, z);
	}

	public void uniform3fv(int location, int count, FloatBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform3fv(location, count, v, getPosition(v));
	}

	public void uniform3i(int location, int x, int y, int z) {
		com.badlogic.jglfw.gl.GL.glUniform3i(location, x, y, z);
	}

	public void uniform3iv(int location, int count, IntBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform3iv(location, count, v, getPosition(v));
	}

	public void uniform4f(int location, float x, float y, float z, float w) {
		com.badlogic.jglfw.gl.GL.glUniform4f(location, x, y, z, w);
	}

	public void uniform4fv(int location, int count, FloatBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform4fv(location, count, v, getPosition(v));
	}

	public void uniform4i(int location, int x, int y, int z, int w) {
		com.badlogic.jglfw.gl.GL.glUniform4i(location, x, y, z, w);
	}

	public void uniform4iv(int location, int count, IntBuffer v) {
		com.badlogic.jglfw.gl.GL.glUniform4iv(location, count, v, getPosition(v));
	}

	public void uniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value) {
		com.badlogic.jglfw.gl.GL.glUniformMatrix2fv(
		                                            location,
		                                            count,
		                                            transpose,
		                                            value,
		                                            getPosition(value));
	}

	public void uniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value) {
		com.badlogic.jglfw.gl.GL.glUniformMatrix3fv(
		                                            location,
		                                            count,
		                                            transpose,
		                                            value,
		                                            getPosition(value));
	}

	public void uniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value) {
		com.badlogic.jglfw.gl.GL.glUniformMatrix4fv(
		                                            location,
		                                            count,
		                                            transpose,
		                                            value,
		                                            getPosition(value));
	}

	public void useProgram(int program) {
		com.badlogic.jglfw.gl.GL.glUseProgram(program);
	}

	public void validateProgram(int program) {
		com.badlogic.jglfw.gl.GL.glValidateProgram(program);
	}

	public void vertexAttrib1f(int indx, float x) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib1f(indx, x);
	}

	public void vertexAttrib1fv(int indx, FloatBuffer values) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib1fv(indx, values, getPosition(values));
	}

	public void vertexAttrib2f(int indx, float x, float y) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib2f(indx, x, y);
	}

	public void vertexAttrib2fv(int indx, FloatBuffer values) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib2fv(indx, values, getPosition(values));
	}

	public void vertexAttrib3f(int indx, float x, float y, float z) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib3f(indx, x, y, z);
	}

	public void vertexAttrib3fv(int indx, FloatBuffer values) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib3fv(indx, values, getPosition(values));
	}

	public void vertexAttrib4f(int indx, float x, float y, float z, float w) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib4f(indx, x, y, z, w);
	}

	public void vertexAttrib4fv(int indx, FloatBuffer values) {
		com.badlogic.jglfw.gl.GL.glVertexAttrib4fv(indx, values, getPosition(values));
	}

	public void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
	        Buffer ptr) {
		com.badlogic.jglfw.gl.GL.glVertexAttribPointer(
		                                               indx,
		                                               size,
		                                               type,
		                                               normalized,
		                                               stride,
		                                               ptr,
		                                               getPosition(ptr));
	}

	public void vertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
	        int ptr) {
		com.badlogic.jglfw.gl.GL.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
	}
}
