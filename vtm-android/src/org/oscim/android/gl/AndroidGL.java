/*
 * Copyright 2013
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

import android.opengl.GLES20;

public class AndroidGL implements GL20{

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
		return GLES20.glGetActiveAttrib(program, index, size, (IntBuffer)type);
	}

	@Override
	public String glGetActiveUniform(int program, int index, IntBuffer size, Buffer type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void glGetAttachedShaders(int program, int maxcount, Buffer count, IntBuffer shaders) {
		// TODO Auto-generated method stub

	}

	@Override
	public int glGetAttribLocation(int program, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void glGetBooleanv(int pname, Buffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetBufferParameteriv(int target, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetFloatv(int pname, FloatBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetFramebufferAttachmentParameteriv(int target, int attachment, int pname,
			IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetProgramiv(int program, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public String glGetProgramInfoLog(int program) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void glGetRenderbufferParameteriv(int target, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetShaderiv(int shader, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public String glGetShaderInfoLog(int shader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void glGetShaderPrecisionFormat(int shadertype, int precisiontype, IntBuffer range,
			IntBuffer precision) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetShaderSource(int shader, int bufsize, Buffer length, String source) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetTexParameteriv(int target, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetUniformfv(int program, int location, FloatBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetUniformiv(int program, int location, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public int glGetUniformLocation(int program, String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void glGetVertexAttribfv(int index, int pname, FloatBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetVertexAttribiv(int index, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGetVertexAttribPointerv(int index, int pname, Buffer pointer) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean glIsBuffer(int buffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean glIsEnabled(int cap) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean glIsFramebuffer(int framebuffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean glIsProgram(int program) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean glIsRenderbuffer(int renderbuffer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean glIsShader(int shader) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean glIsTexture(int texture) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void glLinkProgram(int program) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glReleaseShaderCompiler() {
		// TODO Auto-generated method stub

	}

	@Override
	public void glRenderbufferStorage(int target, int internalformat, int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glSampleCoverage(float value, boolean invert) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glShaderBinary(int n, IntBuffer shaders, int binaryformat, Buffer binary, int length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glShaderSource(int shader, String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glStencilMaskSeparate(int face, int mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glStencilOpSeparate(int face, int fail, int zfail, int zpass) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glTexParameterfv(int target, int pname, FloatBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glTexParameteri(int target, int pname, int param) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glTexParameteriv(int target, int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform1f(int location, float x) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform1fv(int location, int count, FloatBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform1i(int location, int x) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform1iv(int location, int count, IntBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform2f(int location, float x, float y) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform2fv(int location, int count, FloatBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform2i(int location, int x, int y) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform2iv(int location, int count, IntBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform3f(int location, float x, float y, float z) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform3fv(int location, int count, FloatBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform3i(int location, int x, int y, int z) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform3iv(int location, int count, IntBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform4f(int location, float x, float y, float z, float w) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform4fv(int location, int count, FloatBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform4i(int location, int x, int y, int z, int w) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniform4iv(int location, int count, IntBuffer v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniformMatrix2fv(int location, int count, boolean transpose, FloatBuffer value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniformMatrix3fv(int location, int count, boolean transpose, FloatBuffer value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUniformMatrix4fv(int location, int count, boolean transpose, FloatBuffer value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glUseProgram(int program) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glValidateProgram(int program) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib1f(int indx, float x) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib1fv(int indx, FloatBuffer values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib2f(int indx, float x, float y) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib2fv(int indx, FloatBuffer values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib3f(int indx, float x, float y, float z) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib3fv(int indx, FloatBuffer values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib4f(int indx, float x, float y, float z, float w) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttrib4fv(int indx, FloatBuffer values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
			Buffer ptr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glVertexAttribPointer(int indx, int size, int type, boolean normalized, int stride,
			int ptr) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glActiveTexture(int texture) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glBindTexture(int target, int texture) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glBlendFunc(int sfactor, int dfactor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glClear(int mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glClearColor(float red, float green, float blue, float alpha) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glClearDepthf(float depth) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glClearStencil(int s) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glCompressedTexImage2D(int target, int level, int internalformat, int width,
			int height, int border, int imageSize, Buffer data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset,
			int width, int height, int format, int imageSize, Buffer data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glCopyTexImage2D(int target, int level, int internalformat, int x, int y,
			int width, int height, int border) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y,
			int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glCullFace(int mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDeleteTextures(int n, IntBuffer textures) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDepthFunc(int func) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDepthMask(boolean flag) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDepthRangef(float zNear, float zFar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDisable(int cap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDrawArrays(int mode, int first, int count) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glDrawElements(int mode, int count, int type, Buffer indices) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glEnable(int cap) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glFinish() {
		// TODO Auto-generated method stub

	}

	@Override
	public void glFlush() {
		// TODO Auto-generated method stub

	}

	@Override
	public void glFrontFace(int mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glGenTextures(int n, IntBuffer textures) {
		// TODO Auto-generated method stub

	}

	@Override
	public int glGetError() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void glGetIntegerv(int pname, IntBuffer params) {
		// TODO Auto-generated method stub

	}

	@Override
	public String glGetString(int name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void glHint(int target, int mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glLineWidth(float width) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glPixelStorei(int pname, int param) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glPolygonOffset(float factor, float units) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glReadPixels(int x, int y, int width, int height, int format, int type,
			Buffer pixels) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glScissor(int x, int y, int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glStencilFunc(int func, int ref, int mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glStencilMask(int mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glStencilOp(int fail, int zfail, int zpass) {
		// TODO Auto-generated method stub

	}

	@Override
	public void glTexImage2D(int target, int level, int internalformat, int width, int height,
			int border, int format, int type, Buffer pixels) {
		GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
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
