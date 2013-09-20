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
package org.oscim.backend;

public class Log {
	public static Logger logger;

	public static void d(String tag, String msg) {
		logger.d(tag, msg);
	}

	public static void w(String tag, String msg) {
		logger.w(tag, msg);
	}

	public static void e(String tag, String msg) {
		logger.e(tag, msg);
	}

	public static void i(String tag, String msg) {
		logger.i(tag, msg);
	}

	public interface Logger {
		void d(String tag, String msg);

		void w(String tag, String msg);

		void e(String tag, String msg);

		void i(String tag, String msg);
	}
}
