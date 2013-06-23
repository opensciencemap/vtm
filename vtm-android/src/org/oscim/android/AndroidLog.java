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
package org.oscim.android;

import android.util.Log;


public class AndroidLog  implements org.oscim.backend.Log.Logger{

	@Override
	public void d(String tag, String msg) {
		Log.d(tag, msg);
	}

	@Override
	public void w(String tag, String msg) {
		Log.w(tag, msg);
	}

	@Override
	public void e(String tag, String msg) {
		Log.e(tag, msg);
	}

	@Override
	public void i(String tag, String msg) {
		Log.i(tag, msg);
	}

}
