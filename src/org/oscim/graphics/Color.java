/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oscim.graphics;

public  class Color {
	 public static final int BLACK       = 0xFF000000;
	    public static final int DKGRAY      = 0xFF444444;
	    public static final int GRAY        = 0xFF888888;
	    public static final int LTGRAY      = 0xFFCCCCCC;
	    public static final int WHITE       = 0xFFFFFFFF;
	    public static final int RED         = 0xFFFF0000;
	    public static final int GREEN       = 0xFF00FF00;
	    public static final int BLUE        = 0xFF0000FF;
	    public static final int YELLOW      = 0xFFFFFF00;
	    public static final int CYAN        = 0xFF00FFFF;
	    public static final int MAGENTA     = 0xFFFF00FF;
	    public static final int TRANSPARENT = 0;


	    /**
	     * Parse the color string, and return the corresponding color-int.
	     * If the string cannot be parsed, throws an IllegalArgumentException
	     * exception. Supported formats are:
	     * #RRGGBB
	     * #AARRGGBB
	     * 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta',
	     * 'yellow', 'lightgray', 'darkgray'
	     */
	    public static int parseColor(String colorString) {
	        if (colorString.charAt(0) == '#') {
	            // Use a long to avoid rollovers on #ffXXXXXX
	            long color = Long.parseLong(colorString.substring(1), 16);
	            if (colorString.length() == 7) {
	                // Set the alpha value
	                color |= 0x00000000ff000000;
	            } else if (colorString.length() != 9) {
	                throw new IllegalArgumentException("Unknown color");
	            }
	            return (int)color;
	        }
	        throw new IllegalArgumentException("Unknown color");
	    }
}
