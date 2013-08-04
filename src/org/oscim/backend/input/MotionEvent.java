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
package org.oscim.backend.input;

public class MotionEvent {

	public static final int ACTION_DOWN = 0;
	public static final int ACTION_UP = 1;
	public static final int ACTION_MOVE = 2;
	public static final int ACTION_CANCEL = 3;
	public static final int ACTION_POINTER_DOWN = 5;
	public static final int ACTION_POINTER_UP = 6;

	public static final int ACTION_MASK = 0xff;

	public static final int ACTION_POINTER_INDEX_MASK = 0xff00;
	public static final int ACTION_POINTER_INDEX_SHIFT = 8;

	public int getAction() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getX() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getY() {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getX(int idx) {
		// TODO Auto-generated method stub
		return 0;
	}

	public float getY(int idx) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getPointerCount() {
		// TODO Auto-generated method stub
		return 0;
	}

}
