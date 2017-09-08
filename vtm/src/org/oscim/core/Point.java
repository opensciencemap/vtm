/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2017 devemux86
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
package org.oscim.core;

public class Point {
    public double x;
    public double y;

    public Point() {
    }

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double distance(Point other) {
        return Math.sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y));
    }

    public double distanceSq(Point other) {
        return (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y);
    }

    @Override
    public String toString() {
        return x + " " + y;
    }

    public void setPerpendicular(Point other) {
        x = -other.y;
        y = other.x;
    }

    public void setPerpendicular(Point p1, Point p2) {
        x = p1.x + p2.x;
        y = p1.y + p2.y;

        double a = p2.x * y - p2.y * x;

        if (a < 0.01 && a > -0.01) {
            /* Almost straight */
            x = -p2.y;
            y = p2.x;
        } else {
            x /= a;
            y /= a;
        }
    }
}
