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

package com.badlogic.gdx.backends.android;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.oscim.backend.GL20;

public class AndroidGL20 implements GL20 {
	static {
		System.loadLibrary("androidgl20");
		init();
	}

	private static native void init ();

	@Override
	public native void glActiveTexture (int texture);

	@Override
	public native void glAttachShader (int program, int shader);

	@Override
	public native void glBindAttribLocation (int program, int index, String name);

	@Override
	public native void glBindBuffer (int target, int buffer);

	@Override
	public native void glBindFramebuffer (int target, int framebuffer);

	@Override
	public native void glBindRenderbuffer (int target, int renderbuffer);

	@Override
	public native void glBindTexture (int target, int texture);

	@Override
	public native void glBlendColor (float red, float green, float blue, float alpha);

	@Override
	public native void glBlendEquation (int mode);

	@Override
	public native void glBlendEquationSeparate (int modeRGB, int modeAlpha);

	@Override
	public native void glBlendFunc (int sfactor, int dfactor);

	@Override
	public native void glBlendFuncSeparate (int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

	@Override
	public native void glBufferData (int target, int size, Buffer data, int usage);

	@Override
	public native void glBufferSubData (int target, int offset, int size, Buffer data);

	@Override
	public native int glCheckFramebufferStatus (int target);

	@Override
	public native void glClear (int mask);

	@Override
	public native void glClearColor (float red, float green, float blue, float alpha);

	@Override
	public native void glClearDepthf (float depth);

	@Override
	public native void glClearStencil (int s);

	@Override
	public native void glColorMask (boolean red, boolean green, boolean blue, boolean alpha);

	@Override
	public native void glCompileShader (int shader);

	@Override
	public native void glCompressedTexImage2D (int target, int level, int internalformat, int width, int height, int border,
		int imageSize, Buffer data);

	@Override
	public native void glCompressedTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height,
		int format, int imageSize, Buffer data);

	@Override
	public native void glCopyTexImage2D (int target, int level, int internalformat, int x, int y, int width, int height, int border);

	@Override
	public native void glCopyTexSubImage2D (int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);

	@Override
	public native int glCreateProgram ();

	@Override
	public native int glCreateShader (int type);

	@Override
	public native void glCullFace (int mode);

	@Override
	public native void glDeleteBuffers (int n, IntBuffer buffers);

	@Override
	public native void glDeleteFramebuffers (int n, IntBuffer framebuffers);

	@Override
	public native void glDeleteProgram (int program);

	@Override
	public native void glDeleteRenderbuffers (int n, IntBuffer renderbuffers);

	@Override
	public native void glDeleteShader (int shader);

	@Override
	public native void glDeleteTextures (int n, IntBuffer textures);

	@Override
	public native void glDepthFunc (int func);

	@Override
	public native void glDepthMask (boolean flag);

	@Override
	public native void glDepthRangef (float zNear, float zFar);

	@Override
	public native void glDetachShader (int program, int shader);

	@Override
	public native void glDisable (int cap);

	@Override
	public native void glDisableVertexAttribArray (int index);

	@Override
	public native void glDrawArrays (int mode, int first, int count);

	@Override
	public native void glDrawElements (int mode, int count, int type, Buffer indices);

	@Override
	public native void glDrawElements (int mode, int count, int type, int indices);

	@Override
	public native void glEnable (int cap);

	@Override
	public native void glEnableVertexAttribArray (int index);

	@Override
	public native void glFinish ();

	@Override
	public native void glFlush ();

	@Override
	public native void glFramebufferRenderbuffer (int target, int attachment, int renderbuffertarget, int renderbuffer);

	@Override
	public native void glFramebufferTexture2D (int target, int attachment, int textarget, int texture, int level);

	@Override
	public native void glFrontFace (int mode);

	@Override
	public native void glGenBuffers (int n, IntBuffer buffers);

	@Override
	public native void glGenerateMipmap (int target);

	@Override
	public native void glGenFramebuffers (int n, IntBuffer framebuffers);

	@Override
	public native void glGenRenderbuffers (int n, IntBuffer renderbuffers);

	@Override
	public native void glGenTextures (int n, IntBuffer textures);

	@Override
	public native String glGetActiveAttrib (int program, int index, IntBuffer size, Buffer type);

	@Override
	public native String glGetActiveUniform (int program, int index, IntBuffer size, Buffer type);

	@Override
	public native void glGetAttachedShaders (int program, int maxcount, Buffer count, IntBuffer shaders);

	@Override
	public native int glGetAttribLocation (int program, String name);

	@Override
	public native void glGetBooleanv (int pname, Buffer params);

	@Override
	public native void glGetBufferParameteriv (int target, int pname, IntBuffer params);

	@Override
	public native int glGetError ();

	@Override
	public native void glGetFloatv (int pname, FloatBuffer params);

	@Override
	public native void glGetFramebufferAttachmentParameteriv (int target, int attachment, int pname, IntBuffer params);

	@Override
	public native void glGetIntegerv (int pname, IntBuffer params);

	@Override
	public native void glGetProgramiv (int program, int pname, IntBuffer params);

	@Override
	public native String glGetProgramInfoLog (int program);

	@Override
	public native void glGetRenderbufferParameteriv (int target, int pname, IntBuffer params);

	@Override
	public native void glGetShaderiv (int shader, int pname, IntBuffer params);

	@Override
	public native String glGetShaderInfoLog (int shader);

	@Override
	public native void glGetShaderPrecisionFormat (int shadertype, int precisiontype, IntBuffer range, IntBuffer precision);

	@Override
	public native void glGetShaderSource (int shader, int bufsize, Buffer length, String source);

	@Override
	public native String glGetString (int name);

	@Override
	public native void glGetTexParameterfv (int target, int pname, FloatBuffer params);

	@Override
	public native void glGetTexParameteriv (int target, int pname, IntBuffer params);

	@Override
	public native void glGetUniformfv (int program, int location, FloatBuffer params);

	@Override
	public native void glGetUniformiv (int program, int location, IntBuffer params);

	@Override
	public native int glGetUniformLocation (int program, String name);

	@Override
	public native void glGetVertexAttribfv (int index, int pname, FloatBuffer params);

	@Override
	public native void glGetVertexAttribiv (int index, int pname, IntBuffer params);

	@Override
	public native void glGetVertexAttribPointerv (int index, int pname, Buffer pointer);

	@Override
	public native void glHint (int target, int mode);

	@Override
	public native boolean glIsBuffer (int buffer);

	@Override
	public native boolean glIsEnabled (int cap);

	@Override
	public native boolean glIsFramebuffer (int framebuffer);

	@Override
	public native boolean glIsProgram (int program);

	@Override
	public native boolean glIsRenderbuffer (int renderbuffer);

	@Override
	public native boolean glIsShader (int shader);

	@Override
	public native boolean glIsTexture (int texture);

	@Override
	public native void glLineWidth (float width);

	@Override
	public native void glLinkProgram (int program);

	@Override
	public native void glPixelStorei (int pname, int param);

	@Override
	public native void glPolygonOffset (float factor, float units);

	@Override
	public native void glReadPixels (int x, int y, int width, int height, int format, int type, Buffer pixels);

	@Override
	public native void glReleaseShaderCompiler ();

	@Override
	public native void glRenderbufferStorage (int target, int internalformat, int width, int height);

	@Override
	public native void glSampleCoverage (float value, boolean invert);

	@Override
	public native void glScissor (int x, int y, int width, int height);

	@Override
	public native void glShaderBinary (int n, IntBuffer shaders, int binaryformat, Buffer binary, int length);

	@Override
	public native void glShaderSource (int shader, String string);

	@Override
	public native void glStencilFunc (int func, int ref, int mask);

	@Override
	public native void glStencilFuncSeparate (int face, int func, int ref, int mask);

	@Override
	public native void glStencilMask (int mask);

	@Override
	public native void glStencilMaskSeparate (int face, int mask);

	@Override
	public native void glStencilOp (int fail, int zfail, int zpass);

	@Override
	public native void glStencilOpSeparate (int face, int fail, int zfail, int zpass);

	@Override
	public native void glTexImage2D (int target, int level, int internalformat, int width, int height, int border, int format,
		int type, Buffer pixels);

	@Override
	public native void glTexParameterf (int target, int pname, float param);

	@Override
	public native void glTexParameterfv (int target, int pname, FloatBuffer params);

	@Override
	public native void glTexParameteri (int target, int pname, int param);

	@Override
	public native void glTexParameteriv (int target, int pname, IntBuffer params);

	@Override
	public native void glTexSubImage2D (int target, int level, int xoffset, int yoffset, int width, int height, int format,
		int type, Buffer pixels);

	@Override
	public native void glUniform1f (int location, float x);

	@Override
	public native void glUniform1fv (int location, int count, FloatBuffer v);

	@Override
	public native void glUniform1i (int location, int x);

	@Override
	public native void glUniform1iv (int location, int count, IntBuffer v);

	@Override
	public native void glUniform2f (int location, float x, float y);

	@Override
	public native void glUniform2fv (int location, int count, FloatBuffer v);

	@Override
	public native void glUniform2i (int location, int x, int y);

	@Override
	public native void glUniform2iv (int location, int count, IntBuffer v);

	@Override
	public native void glUniform3f (int location, float x, float y, float z);

	@Override
	public native void glUniform3fv (int location, int count, FloatBuffer v);

	@Override
	public native void glUniform3i (int location, int x, int y, int z);

	@Override
	public native void glUniform3iv (int location, int count, IntBuffer v);

	@Override
	public native void glUniform4f (int location, float x, float y, float z, float w);

	@Override
	public native void glUniform4fv (int location, int count, FloatBuffer v);

	@Override
	public native void glUniform4i (int location, int x, int y, int z, int w);

	@Override
	public native void glUniform4iv (int location, int count, IntBuffer v);

	@Override
	public native void glUniformMatrix2fv (int location, int count, boolean transpose, FloatBuffer value);

	@Override
	public native void glUniformMatrix3fv (int location, int count, boolean transpose, FloatBuffer value);

	@Override
	public native void glUniformMatrix4fv (int location, int count, boolean transpose, FloatBuffer value);

	@Override
	public native void glUseProgram (int program);

	@Override
	public native void glValidateProgram (int program);

	@Override
	public native void glVertexAttrib1f (int indx, float x);

	@Override
	public native void glVertexAttrib1fv (int indx, FloatBuffer values);

	@Override
	public native void glVertexAttrib2f (int indx, float x, float y);

	@Override
	public native void glVertexAttrib2fv (int indx, FloatBuffer values);

	@Override
	public native void glVertexAttrib3f (int indx, float x, float y, float z);

	@Override
	public native void glVertexAttrib3fv (int indx, FloatBuffer values);

	@Override
	public native void glVertexAttrib4f (int indx, float x, float y, float z, float w);

	@Override
	public native void glVertexAttrib4fv (int indx, FloatBuffer values);

	@Override
	public native void glVertexAttribPointer (int indx, int size, int type, boolean normalized, int stride, Buffer ptr);

	@Override
	public native void glVertexAttribPointer (int indx, int size, int type, boolean normalized, int stride, int ptr);

	@Override
	public native void glViewport (int x, int y, int width, int height);
}
