/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.utils;

public class Matrix4 {

	static {
		System.loadLibrary("glutils");
	}

	private final static String TAG = Matrix4.class.getName();
	private final static boolean dbg = false;

	private native static long alloc();

	private native static void delete(long self);

	private native static void set(long self, float[] m);

	private native static void copy(long self, long other);

	private native static void identity(long self);

	private native static void get(long self, float[] m);

	private native static void mul(long self, long lhs_ptr);

	private native static void smul(long self, long rhs_ptr, long lhs_ptr);

	private native static void smulrhs(long self, long rhs_ptr);

	private native static void smullhs(long self, long lhs_ptr);

	private native static void strans(long self, long rhs_ptr);

	private native static void prj(long self, float[] vec3);

	private native static void setRotation(long self, float a, float x, float y, float z);

	private native static void setScale(long self, float x, float y, float z);

	private native static void setTranslation(long self, float x, float y, float z);

	private native static void setTransScale(long self, float tx, float ty, float scale);

	private native static void setAsUniform(long self, int handle);

	private native void setValueAt(long self, int pos, float value);

	private native void addDepthOffset(long self, int delta);

	private final long pointer;

	private final static String INVALID_INPUT = "Bad Array!";

	public Matrix4() {
		pointer = alloc();
	}

	/**
	 * Set the Matrix from float array
	 *
	 * @param m float array to copy
	 */
	public void set(float[] m) {
		if (m == null || m.length != 16)
			throw new IllegalArgumentException(INVALID_INPUT);

		set(pointer, m);
	}

	/**
	 * Get the Matrix as float array
	 *
	 * @param m float array to store Matrix
	 */
	public void get(float[] m) {
		if (m == null || m.length != 16)
			throw new IllegalArgumentException(INVALID_INPUT);

		get(pointer, m);
	}

	/**
	 * Copy values from mat
	 *
	 * @param mat Matrix to copy
	 */
	public void copy(Matrix4 mat) {
		copy(pointer, mat.pointer);
	}

	/**
	 * Project Vector with Matrix
	 *
	 * @param vec3 Vector to project
	 */
	public void prj(float[] vec3) {
		if (vec3 == null || vec3.length < 3)
			throw new IllegalArgumentException(INVALID_INPUT);

		prj(pointer, vec3);
	}

	/**
	 * Multiply rhs onto Matrix.
	 *
	 * @param rhs right hand side
	 */
	public void multiplyRhs(Matrix4 rhs) {
		smulrhs(pointer, rhs.pointer);
	}

	/**
	 * Use this matrix as rhs, multiply it on lhs and store result.
	 *
	 * @param lhs right hand side
	 */
	public void multiplyLhs(Matrix4 lhs) {
		smullhs(pointer, lhs.pointer);
	}

	/**
	 * Multiply rhs onto lhs and store result in Matrix.
	 *
	 * This matrix MUST be different from lhs and rhs!
	 *
	 * As you know, when combining matrices for vector projection
	 * this has the same effect first as applying rhs then lhs.
	 *
	 * @param lhs left hand side
	 * @param rhs right hand side
	 */
	public void multiplyMM(Matrix4 lhs, Matrix4 rhs) {
		smul(pointer, lhs.pointer, rhs.pointer);
	}

	/**
	 * Transpose mat and store result in Matrix
	 *
	 * @param mat to transpose
	 */
	public void transposeM(Matrix4 mat) {
		strans(pointer, mat.pointer);
	}

	/**
	 * Set rotation
	 *
	 * @param a angle in degree
	 * @param x around x-axis
	 * @param y around y-axis
	 * @param z around z-axis
	 */
	public void setRotation(float a, float x, float y, float z) {
		setRotation(pointer, a, x, y, z);
	}

	/**
	 * Set translation
	 *
	 * @param x along x-axis
	 * @param y along y-axis
	 * @param z along z-axis
	 */
	public void setTranslation(float x, float y, float z) {
		setTranslation(pointer, x, y, z);
	}

	/**
	 * Set scale factor
	 *
	 * @param x axis
	 * @param y axis
	 * @param z axis
	 */
	public void setScale(float x, float y, float z) {
		setScale(pointer, x, y, z);
	}

	/**
	 * Set translation and x,y scale
	 *
	 * @param tx translate x
	 * @param ty translate y
	 * @param scale factor x,y
	 */
	public void setTransScale(float tx, float ty, float scale) {
		setTransScale(pointer, tx, ty, scale);
	}

	/**
	 * Set Matrix with glUniformMatrix
	 *
	 * @param location GL location id
	 */
	public void setAsUniform(int location) {
		setAsUniform(pointer, location);
	}

	/**
	 * Set single value
	 *
	 * @param pos at position
	 * @param value value to set
	 */
	public void setValue(int pos, float value) {
		setValueAt(pointer, pos, value);
	}

	/**
	 * add some offset (similar to glDepthOffset)
	 *
	 * @param delta offset
	 */
	public void addDepthOffset(int delta) {
		addDepthOffset(pointer, delta);
	}

	/**
	 * Set identity matrix
	 */
	public void setIdentity() {
		identity(pointer);
	}

	@Override
	public void finalize() {
		if (pointer != 0)
			delete(pointer);
	}
}
