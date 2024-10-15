/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.starfield;

/*
 Copyright (c) 2019 Goksel KUCUKSAHIN
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private static Random random = new Random(); //static random generator //one generator for everything!

    static int getRandomInt(int limit) {
        return random.nextInt(limit);
    }

    static double getRandom(double min, double max) {
        return map(Math.random(), 0, 1, min, max);
    }

    static double getRandom(double limit) {
        return map(Math.random(), 0, 1, 0, limit);
    }

    static int getRandomInt(int start, int end) {
        return random.nextInt(end - start) + start;
    }

    static Point2D endPoint(Point2D point, double angle, double size) {
        return endPoint(point.getX(), point.getY(), angle, size);
    }

    static Point2D endPoint(double x, double y, double angle, double size) {
        x += (size * Math.cos(angleToRadian(angle)));
        y -= (size * Math.sin(angleToRadian(angle)));
        return new Point2D(x, y);
    }

    static double calculateAngle(double p1X, double p1Y, double p2X, double p2Y) {
        double dx = Math.abs(p1X - p2X);
        double dy = Math.abs(p1Y - p2Y);
        //System.out.println(dx+": "+dy);
        double a = radianToAngle(Math.atan(dy / dx));
        if (p1X - p2X >= 0) {
            //II - III
            if (p1Y - p2Y >= 0) {
                //II Region
                return 180 - a;
            } else {
                //III Region
                return 180 + a;
            }
        } else {
            // I - IV
            if (p1Y - p2Y >= 0) {
                //I Region
                return a;
            } else {
                //IV Region
                return 360 - a;
            }
        }
    }

    static double calculateAngle(Point2D p1, Point2D p2) {
        return calculateAngle(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    static double angleToRadian(double angle) {
        return (Math.PI / 180.0) * angle;
    }

    static double radianToAngle(double radian) {
        return radian * (180.0 / Math.PI);
    }

    static double map(double x, double in_min, double in_max, double out_min, double out_max) {
        //double map
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    static double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    static double distance(Point2D p1, Point2D p2) {
        return distance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    static double span(Point2D vector) {
        return Math.sqrt(Math.pow(vector.getX(), 2) + Math.pow(vector.getY(), 2));
    }

    static boolean isHit(Point2D obj1, Point2D obj2, double lenght) {
        double difX = obj2.getX() - obj1.getX();
        double difY = obj2.getY() - obj1.getY();
        double ltp = lenght * lenght;
        return Math.pow(difX, 2) + Math.pow(difY, 2) <= ltp;
    }

    static Point2D lineXlineIntersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        double t = (((x1 - x3) * (y3 - y4)) - (y1 - y3) * (x3 - x4))
            / (((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4)));
        return new Point2D(x1 + t * (x2 - x1), y1 + t * (y2 - y1));
    }

    static Point2D lineXlineIntersection(Line l1, Line l2) {
        return lineXlineIntersection(l1.getStartX(), l1.getStartY(), l1.getEndX(), l1.getEndY(),
            l2.getStartX(), l2.getStartY(), l2.getEndX(), l2.getEndY());
    }

    static double pisagor(double edge1, double edge2) {
        return Math.sqrt(Math.pow(edge1, 2) + Math.pow(edge2, 2));
    }

    static boolean isIntersecting(Line line, Point2D p) {
        double x1 = Math.min(line.getStartX(), line.getEndX());
        double x2 = Math.max(line.getStartX(), line.getEndX());
        double y1 = Math.min(line.getStartY(), line.getEndY());
        double y2 = Math.max(line.getStartY(), line.getEndY());
        return (p.getX() >= x1 && p.getX() <= x2) && ((p.getY() >= y1) && (p.getY() <= y2));
    }

    public static String hashFunctionGenerator(String stringForEncode) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Exception", e); //it means somethings went really wrong.
        }
        byte[] hash = digest.digest(stringForEncode.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
/*A GOKSEL KUCUKSAHIN SOFTWARE*/
