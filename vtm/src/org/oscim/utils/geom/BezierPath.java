package org.oscim.utils.geom;

import org.oscim.core.Point;

import java.util.ArrayList;
import java.util.List;

public class BezierPath {

    /**
     * from http://paulbourke.net/geometry/bezier/index.html
     * Three control point Bezier interpolation
     * mu ranges from 0 to 1, start to end of the curve
     */
    public static Point bezier3(Point p1, Point p2, Point p3, double mu) {
        double mum1, mum12, mu2;
        Point p = new Point();

        mu2 = mu * mu;
        mum1 = 1 - mu;
        mum12 = mum1 * mum1;
        p.x = p1.x * mum12 + 2 * p2.x * mum1 * mu + p3.x * mu2;
        p.y = p1.y * mum12 + 2 * p2.y * mum1 * mu + p3.y * mu2;
        //p.z = p1.z * mum12 + 2 * p2.z * mum1 * mu + p3.z * mu2;

        return (p);
    }

    /**
     * from http://paulbourke.net/geometry/bezier/index.html
     * Four control point Bezier interpolation
     * mu ranges from 0 to 1, start to end of curve
     */
    public static Point cubicBezier(Point p1, Point p2, Point p3, Point p4, double mu) {
        double mum1, mum13, mu3;
        Point p = new Point();

        mum1 = 1 - mu;
        mum13 = mum1 * mum1 * mum1;
        mu3 = mu * mu * mu;

        p.x = mum13 * p1.x + 3 * mu * mum1 * mum1 * p2.x + 3 * mu * mu * mum1 * p3.x + mu3 * p4.x;
        p.y = mum13 * p1.y + 3 * mu * mum1 * mum1 * p2.y + 3 * mu * mu * mum1 * p3.y + mu3 * p4.y;
        //p.z = mum13*p1.z + 3*mu*mum1*mum1*p2.z + 3*mu*mu*mum1*p3.z + mu3*p4.z;

        return (p);
    }

    /**
     * from geodroid
     * FIXME
     */
    public static List<Point> cubicSplineControlPoints(Point[] coords, float alpha) {

        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be between 0 and 1 inclusive");
        }

        if (coords.length < 2) {
            throw new IllegalArgumentException("number of Points must be >= 2");
        }

        int n = coords.length;

        List<Point> ctrl = new ArrayList<Point>();

        Point curr = new Point(2 * coords[0].x - coords[1].x, 2 * coords[0].y - coords[1].y);
        Point next = coords[0];

        Point mid = new Point();
        mid.x = (curr.x + next.x) / 2.0;
        mid.y = (curr.y + next.y) / 2.0;

        Point midPrev = new Point();

        Point last = new Point(2 * coords[n - 1].x - coords[n - 2].x,
                2 * coords[n - 1].y - coords[n - 2].y);

        Point anchor = new Point();
        double dv = curr.distance(next);

        for (int i = 0; i < n; i++) {
            curr = next;
            next = i < n - 1 ? coords[i + 1] : last;

            midPrev.x = mid.x;
            midPrev.y = mid.y;

            mid.x = (curr.x + next.x) / 2.0;
            mid.y = (curr.y + next.y) / 2.0;

            double dvPrev = dv;
            dv = curr.distance(next);

            double p = dvPrev / (dvPrev + dv);

            anchor.x = midPrev.x + p * (mid.x - midPrev.x);
            anchor.y = midPrev.y + p * (mid.y - midPrev.y);

            double dx = anchor.x - curr.x;
            double dy = anchor.y - curr.y;

            if (i > 0) {
                ctrl.add(new Point(alpha * (curr.x - midPrev.x + dx) + midPrev.x - dx,
                        alpha * (curr.y - midPrev.y + dy) + midPrev.y - dy));
            }
            if (i < n - 1) {
                ctrl.add(new Point(alpha * (curr.x - mid.x + dx) + mid.x - dx,
                        alpha * (curr.y - mid.y + dy) + mid.y - dy));
            }
        }

        return ctrl;
    }
}
