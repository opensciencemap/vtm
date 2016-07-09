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
package org.oscim.utils.async;

public abstract class Task implements Runnable {

    public static final int ERROR = -1;
    public static final int CANCEL = 0;
    public static final int GO = 1;
    public static final int DONE = 2;

    protected int state = GO;

    boolean isCanceled;

    @Override
    public void run() {
        go(state == CANCEL);
    }

    /**
     * @return ignored
     */
    public abstract int go(boolean canceled);

    public void cancel() {
        state = CANCEL;
    }
}
