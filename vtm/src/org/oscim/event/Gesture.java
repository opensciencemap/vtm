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
package org.oscim.event;

public interface Gesture {

    static final class Press implements Gesture {
    }

    static final class LongPress implements Gesture {
    }

    static final class Tap implements Gesture {
    }

    static final class DoubleTap implements Gesture {
    }

    public static Gesture PRESS = new Press();
    public static Gesture LONG_PRESS = new LongPress();
    public static Gesture TAP = new Tap();
    public static Gesture DOUBLE_TAP = new DoubleTap();
}
