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
package org.oscim.android.gl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.oscim.backend.GL20;

import android.annotation.SuppressLint;
import android.opengl.GLES20;

@SuppressLint("NewApi")
public class AndroidGL implements GL20 {

	@Override
	public void glAttachShader(int program, int shader) {
		GLES20.glAttachShader(program, shader);
	}

	@Override
	public void glBindAttribLocation(int program, int index, String name) {
		GLES20.glBindAttribLocation(program, index, name);
	}

	@Override
	public void glBindBuffer(int target, int buffer) {
		GLES20.glBindBuffer(target, buffer);
	}

	@Override
	public void glBindFramebuffer(int target, int framebuffer) {
		GLES20.glBindFramebuffer(target, framebuffer);
	}

	@Override
	public void glBindRenderbuffer(int target, int renderbuffer) {
		GLES20.glBindRenderbuffer(target, renderbuffer);
	}

	@Override
	public void glBlendColor(float red, float green, float blue, float alpha) {
		GLES20.glBlendColor(red, green, blue, alpha);
	}

	@Override
	public void glBlendEquation(int mode) {
		GLES20.glBlendEquation(mode);
	}

	@Override
	public void glBlendEquationSeparate(int modeRGB, int modeAlpha) {
		GLES20.glBlendEquationSeparate(modeRGB, modeAlpha);
	}

	@Override
	public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
		GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
	}

	@Override
	public void glBufferData(int target, int size, Buffer data, int usage) {
		GLES20.glBufferData(target, size, data, usage);
	}

	@Override
	public void glBufferSubData(int target, int offset, int size, Buffer data) {
		GLES20.glBufferSubData(target, offset, size, data);
	}

	@Override
	public int glCheckFramebufferStatus(int target) {
		return GLES20.glCheckFramebufferStatus(target);
	}

	@Override
	public void glCompileShader(int shader) {
		GLES20.glCompileShader(shader);
	}

	@Override
	public int glCreateProgram() {
		return GLES20.glCreateProgram();
	}

	@Override
	public int glCreateShader(int type) {
		return GLES20.glCreateShader(type);
	}

	@Override
	public void glDeleteBuffers(int n, IntBuffer buffers) {
		GLES20.glDeleteBuffers(n, buffers);
	}

	@Override
	public void glDeleteFramebuffers(int n, IntBuffer framebuffers) {
		GLES20.glDeleteFramebuffers(n, framebuffers);
	}

	@Override
	public void glDeleteProgram(int program) {
		GLES20.glDeleteProgram(program);
	}

	@Override
	public void glDeleteRenderbuffers(int n, IntBuffer renderbuffers) {
		GLES20.glDeleteRenderbuffers(n, renderbuffers);
	}

	@Override
	public void glDeleteShader(int shader) {
		GLES20.glDeleteShader(shader);
	}

	@Override
	public void glDetachShader(int program, int shader) {
		GLES20.glDetachShader(program, shader);
	}

	@Override
	public void glDisableVertexAttribArray(int index) {
		GLES20.glDisableVertexAttribArray(index);
	}

	@Override
	public void glDrawElements(int mode, int count, int type, int indices) {
		GLES20.glDrawElements(mode, count, type, indices);
	}

	@Override
	public void glEnableVertexAttribArray(int index) {
		GLES20.glEnableVertexAttribArray(index);
	}

	@Override
	public void glFramebufferRenderbuffer(int target, int attachment, int renderbuffertarget,
	        int renderbuffer) {
		GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
	}

	@Override
	public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture,
	        int level) {
		GLES20.glFramebufferTexture2D(target, attachment, textarget, texture, level);
	}

	@Override
	public void glGenBuffers(int n, IntBuffer buffers) {
		GLES20.glGenBuffers(n, buffers);
	}

	@Override
	public void glGenerateMipmap(int target) {
		GLES20.glGenerateMipmap(target);
	}

	@Override
	public void glGenFramebuffers(int n, IntBuffer framebuffers) {
		GLES20.glGenFramebuffers(n, framebuffers);
	}

	@Override
	public void glGenRenderbuffers(int n, IntBuffer renderbuffers) {
		GLES20.glGenRenderbuffers(n, renderbuffers);
	}

	@Override
	public String glGetActiveAttrib(int program, int index, IntBuffer size, Buffer type) {
		return GLES20.glGetActiveAttrib(program, index, size, (IntBuffer) type);
	}

	@Override
	public String glGetActiveUniform(int program, int index, IntBuffer size, Buffer type) {
		//return GLES20.glGetActiveUniform(program, index, bufsize, length, size, type, name);
		throw new UnsupportedOperationException("missing implementation");
	}

	@Override
	public void glGetAttachedShaders(int program, int maxcount, Buffer count, IntBuffer shaders) {
		throw new UnsupportedOperationException("missing implementation");
		//GLES20.glGetAttachedShaders(program, maxcount, count, shaders);
	}

	@Override
	public int glGetAttribLocation(int program, String name) {
		return GLES20.glGetAttribLocation(program, name);
	}

	@Override
	public void glGetBooleanv(int pname, Buffer params) {
		throw new UnsupportedOperationException("missing implementation");
		//GLES20.glGetBooleanv(pname, params);
	}

	@Override
	public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
		GLES20.glGetBufferParameteriv(target, pname, params);

	}

	@Override
	public void glGetFloatv(int pname, FloatBuffer params) {
		GLES20.glGetFloatv(pname, params);

	}

	@Override
	public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname,
	        IntBuffer params) {
		GLES20.glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
	}

	@Override
	public void glGetProgramiv(int program, int pname, IntBuffer params) {
		GLES20.glGetProgramiv(program, pname, params);

	}

	@Override
	public String glGetProgramInfoLog(int program) {
		return GLES20.glGetProgramInfoLog(program);
	}

	@Override
	public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params) {
		GLES20.glGetRenderbufferParameteriv(target, pname, params);

	}

	@Override
	public void glGetShaderiv(int shader, int pname, IntBuffer params) {
		GLES20.glGetShaderiv(shader, pname, params);

	}

	@Override
	public String glGetShaderInfoLog(int shader) {
		return GLES20.glGetShaderInfoLog(shader);
	}

	@Override
	public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range,
	        IntBuffer precision) {
		GLES20.glGetShaderPrecisionFormat(shadertype, precisiontype, range, precision);
	}

	@Override
	public void glGetShaderSource(int shader, int bufsize, Buffer length, String source) {
		throw new UnsupportedOperationException("missing implementation");
	}

	@Override
	public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
		GLES20.glGetTexParameterfv(target, pname, params);

	}

	@Override
	public void glGetTexParameteriv(int target, int pname, IntBuffer params) {
		GLES20.glGetTexParameteriv(target, pname, params);

	}

	@Override
	public void glGetUniformfv(int program, int location, FloatBuffer params) {
		GLES20.glGetUniformfv(program, location, params);

	}

	@Override
	public void glGetUniformiv(int program, int location, IntBuffer params) {
		GLES20.glGetUniformiv(program, location, params);

	}

	@Override
	public int glGetUniformLocation(int program, String name) {
		return GLES20.glGetUniformLocation(program, name);
	}

	@Override
	public void glGetVertexAttribfv(int index, int pname, FloatBuffer params) {
		GLES20.glGetVertexAttribfv(index, pname, params);

	}

	@Override
	public void glGetVertexAttribiv(int index, int pname, IntBuffer params) {
		GLES20.glGetVertexAttribiv(index, pname, params);

	}

	@Override
	public void glGetVertexAttribPointerv(int index, int pname, Buffer pointer) {
		//GLES20.glGetVertexAttribPointerv(index, pname, pointer);
		throw new UnsupportedOperationException("missing implementation");
	}

	@Override
	public boolean glIsBuffer(int buffer) {
		return GLES20.glIsBuffer(buffer);
	}

	@Override
	public boolean glIsEnabled(int cap) {
		return GLES20.glIsEnabled(cap);
	}

	@Override
	public boolean glIsFramebuffer(int framebuffer) {
		return GLES20.glIsFramebuffer(framebuffer);
	}

	@Override
	public boolean glIsProgram(int program) {
		return GLES20.glIsProgram(program);
	}

	@Override
	public boolean glIsRenderbuffer(int renderbuffer) {
		return GLES20.glIsRenderbuffer(renderbuffer);
	}

	@Override
	public boolean glIsShader(int shader) {
		return GLES20.glIsShader(shader);
	}

	@Override
	public boolean glIsTexture(int texture) {
		return GLES20.glIsTexture(texture);
	}

	@Override
	public void glLinkProgram(int program) {
		GLES20.glLinkProgram(program);

	}

	@Override
	public void glReleaseShaderCompiler() {
		GLES20.glReleaseShaderCompiler();

	}

	@Override
	public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
		GLES20.glRenderbufferStorage(target, internalformat, width, height);

	}

	@Override
	public void glSampleCoverage(float value, boolean invert) {
		GLES20.glSampleCoverage(value, invert);

	}

	@Override
	public void glShaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
		GLES20.glShaderBinary(n, shaders, binaryformat, binary, length);

	}

	@Override
	public void glShaderSource(int shader, String string) {
		GLES20.glShaderSource(shader, string);

	}

	@Override
	public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
		GLES20.glStencilFuncSeparate(face, func, ref, mask);

	}

	@Override
	public void glStencilMaskSeparate(int face, int mask) {
		GLES20.glStencilMaskSeparate(face, mask);

	}

	@Override
	public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
		GLES20.glStencilOpSeparate(face, fail, zfail, zpass);

	}

	@Override
	public void glTexParameterfv(int target, int pname, FloatBuffer params) {
		GLES20.glTexParameterfv(target, pname, params);

	}

	@Override
	public void glTexParameteri(int target, int pname, int param) {
		GLES20.glTexParameteri(target, pname, param);

	}

	@Override
	public void glTexParameteriv(int target, int pname, IntBuffer params) {
		GLES20.glTexParameteriv(target, pname, params);

	}

	@Override
	public void glUniform1f(int location, float x) {
		GLES20.glUniform1f(location, x);

	}

	@Override
	public void glUniform1fv(int location, int count, FloatBuffer v) {
		GLES20.glUniform1fv(location, count, v);

	}

	@Override
	public void glUniform1i(int location, int x) {
		GLES20.glUniform1i(location, x);

	}

	@Override
	public void glUniform1iv(int location, int count, IntBuffer v) {
		GLES20.glUniform1iv(location, count, v);

	}

	@Override
	public void glUniform2f(int location, float x, float y) {
		GLES20.glUniform2f(location, x, y);

	}

	@Override
	public void glUniform2fv(int location, int count, FloatBuffer v) {
		GLES20.glUniform2fv(location, count, v);

	}

	@Override
	public void glUniform2i(int location, int x, int y) {
		GLES20.glUniform2i(location, x, y);

	}

	@Override
	public void glUniform2iv(int location, int count, IntBuffer v) {
		GLES20.glUniform2iv(location, count, v);

	}

	@Override
	public void glUniform3f(int location, float x, float y, float z) {
		GLES20.glUniform3f(location, x, y, z);

	}

	@Override
	public void glUniform3fv(int location, int count, FloatBuffer v) {
		GLES20.glUniform3fv(location, count, v);

	}

	@Override
	public void glUniform3i(int location, int x, int y, int z) {
		GLES20.glUniform3i(location, x, y, z);

	}

	@Override
	public void glUniform3iv(int location, int count, IntBuffer v) {
		GLES20.glUniform3iv(location, count, v);

	}

	@Override
	public void glUniform4f(int location, float x, float y, float z, float w) {
		GLES20.glUniform4f(location, x, y, z, w);
	}

	@Override
	public void glUniform4fv(int location, int count, FloatBuffer v) {
		GLES20.glUniform4fv(location, count, v);
	}

	@Override
	public void glUniform4i(int location, int x, int y, int z, int w) {
		GLES20.glUniform4i(location, x, y, z, w);

	}

	@Override
	public void glUniform4iv(int location, int count, IntBuffer v) {
		GLES20.glUniform4iv(location, count, v);

	}

	@Override
	public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value) {
		GLES20.glUniformMatrix2fv(location, count, transpose, value);

	}

	@Override
	public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value) {
		GLES20.glUniformMatrix3fv(location, count, transpose, value);

	}

	@Override
	public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value) {
		GLES20.glUniformMatrix4fv(location, count, transpose, value);

	}

	@Override
	public void glUseProgram(int program) {
		GLES20.glUseProgram(program);

	}

	@Override
	public void glValidateProgram(int program) {
		GLES20.glValidateProgram(program);

	}

	@Override
	public void glVertexAttrib1f(int indx, float x) {
		GLES20.glVertexAttrib1f(indx, x);

	}

	@Override
	public void glVertexAttrib1fv(int indx, FloatBuffer values) {
		GLES20.glVertexAttrib1fv(indx, values);

	}

	@Override
	public void glVertexAttrib2f(int indx, float x, float y) {
		GLES20.glVertexAttrib2f(indx, x, y);

	}

	@Override
	public void glVertexAttrib2fv(int indx, FloatBuffer values) {
		GLES20.glVertexAttrib2fv(indx, values);

	}

	@Override
	public void glVertexAttrib3f(int indx, float x, float y, float z) {
		GLES20.glVertexAttrib3f(indx, x, y, z);

	}

	@Override
	public void glVertexAttrib3fv(int indx, FloatBuffer values) {
		GLES20.glVertexAttrib3fv(indx, values);

	}

	@Override
	public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
		GLES20.glVertexAttrib4f(indx, x, y, z, w);

	}

	@Override
	public void glVertexAttrib4fv(int indx, FloatBuffer values) {
		GLES20.glVertexAttrib4fv(indx, values);

	}

	@Override
	public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
	        Buffer ptr) {
		GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
	}

	@Override
	public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
	        int offset) {
		// FIXME check implementation!
		GLES20.glVertexAttribPointer(indx, size, type, normalized, stride, offset);
		//throw new UnsupportedOperationException("missing implementation");
	}

	@Override
	public void glActiveTexture(int texture) {
		GLES20.glActiveTexture(texture);

	}

	@Override
	public void glBindTexture(int target, int texture) {
		GLES20.glBindTexture(target, texture);

	}

	@Override
	public void glBlendFunc(int sfactor, int dfactor) {
		GLES20.glBlendFunc(sfactor, dfactor);

	}

	@Override
	public void glClear(int mask) {
		GLES20.glClear(mask);

	}

	@Override
	public void glClearColor(float red, float green, float blue, float alpha) {
		GLES20.glClearColor(red, green, blue, alpha);

	}

	@Override
	public void glClearDepthf(float depth) {
		GLES20.glClearDepthf(depth);

	}

	@Override
	public void glClearStencil(int s) {
		GLES20.glClearStencil(s);

	}

	@Override
	public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		GLES20.glColorMask(red, green, blue, alpha);

	}

	@Override
	public void glCompressedTexImage2D(int target, int level, int internalformat, int width,
	        int height, int border, int imageSize, Buffer data) {
		throw new UnsupportedOperationException("missing implementation");

	}

	@Override
	public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset,
	        int width, int height, int format, int imageSize, Buffer data) {
		throw new UnsupportedOperationException("missing implementation");

	}

	@Override
	public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y,
	        int width, int height, int border) {
		GLES20.glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
	}

	@Override
	public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y,
	        int width, int height) {
		GLES20.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
	}

	@Override
	public void glCullFace(int mode) {
		GLES20.glCullFace(mode);

	}

	@Override
	public void glDeleteTextures(int n, IntBuffer textures) {
		GLES20.glDeleteTextures(n, textures);

	}

	@Override
	public void glDepthFunc(int func) {
		GLES20.glDepthFunc(func);

	}

	@Override
	public void glDepthMask(boolean flag) {
		GLES20.glDepthMask(flag);

	}

	@Override
	public void glDepthRangef(float zNear, float zFar) {
		GLES20.glDepthRangef(zNear, zFar);

	}

	@Override
	public void glDisable(int cap) {
		GLES20.glDisable(cap);

	}

	@Override
	public void glDrawArrays(int mode, int first, int count) {
		GLES20.glDrawArrays(mode, first, count);

	}

	@Override
	public void glDrawElements(int mode, int count, int type, Buffer indices) {
		GLES20.glDrawElements(mode, count, type, indices);

	}

	@Override
	public void glEnable(int cap) {
		GLES20.glEnable(cap);

	}

	@Override
	public void glFinish() {
		GLES20.glFinish();

	}

	@Override
	public void glFlush() {
		GLES20.glFlush();

	}

	@Override
	public void glFrontFace(int mode) {
		GLES20.glFrontFace(mode);

	}

	@Override
	public void glGenTextures(int n, IntBuffer textures) {
		GLES20.glGenTextures(n, textures);

	}

	@Override
	public int glGetError() {
		return GLES20.glGetError();
	}

	@Override
	public void glGetIntegerv(int pname, IntBuffer params) {
		GLES20.glGetIntegerv(pname, params);

	}

	@Override
	public String glGetString(int name) {
		return GLES20.glGetString(name);
	}

	@Override
	public void glHint(int target, int mode) {
		GLES20.glHint(target, mode);
	}

	@Override
	public void glLineWidth(float width) {
		GLES20.glLineWidth(width);

	}

	@Override
	public void glPixelStorei(int pname, int param) {
		GLES20.glPixelStorei(pname, param);

	}

	@Override
	public void glPolygonOffset(float factor, float units) {
		GLES20.glPolygonOffset(factor, units);

	}

	@Override
	public void glReadPixels(int x, int y, int width, int height, int format, int type,
	        Buffer pixels) {
		GLES20.glReadPixels(x, y, width, height, format, type, pixels);
	}

	@Override
	public void glScissor(int x, int y, int width, int height) {
		GLES20.glScissor(x, y, width, height);
	}

	@Override
	public void glStencilFunc(int func, int ref, int mask) {
		GLES20.glStencilFunc(func, ref, mask);
	}

	@Override
	public void glStencilMask(int mask) {
		GLES20.glStencilMask(mask);
	}

	@Override
	public void glStencilOp(int fail, int zfail, int zpass) {
		GLES20.glStencilOp(fail, zfail, zpass);
	}

	@Override
	public void glTexImage2D(int target, int level, int internalformat, int width, int height,
	        int border, int format, int type, Buffer pixels) {
		GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type,
		                    pixels);
	}

	@Override
	public void glTexParameterf(int target, int pname, float param) {
		GLES20.glTexParameterf(target, pname, param);
	}

	@Override
	public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width,
	        int height, int format, int type, Buffer pixels) {
		GLES20.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);

	}

	@Override
	public void glViewport(int x, int y, int width, int height) {
		GLES20.glViewport(x, y, width, height);
	}

}
