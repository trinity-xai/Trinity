package edu.jhuapl.trinity.utils.clustering;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
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
 * #L%
 */

/**
 * An N-dimensional point.
 */
public class Point {

    protected double[] position;
    protected int dimensions;

    public Point(int size) {
        position = new double[size];
        this.dimensions = size;
    }

    public Point(double... p) {
        this.position = p;
        this.dimensions = position.length;
    }


    public int getDimensions() {
        return this.dimensions;
    }

    public double[] getPosition() {
        return position;
    }

    public double average() {
        double average = 0;
        for (int i = 0; i < getPosition().length; i++) {
            average += getPosition()[i];
        }
        return average / getPosition().length;
    }

    /**
     * Calculates the euclidean distance between this point and the other point.
     * Assumes the dimensions of each point are the same (this is not checked).
     *
     * @param o other point
     * @return
     */
    double dist(Point o) {
        double sum = 0.0;
        for (int i = 0; i < position.length; ++i) {
            final double d = position[i] - o.position[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    double distSquared(Point o) {
        double sum = 0.0;
        for (int i = 0; i < position.length; ++i) {
            final double d = position[i] - o.position[i];
            sum += d * d;
        }
        return sum;
    }

    @Override
    public boolean equals(Object o) {
        Point p = (Point) o;
        if (this.dimensions != p.dimensions) {
            return false;
        }
        for (int i = 0; i < this.dimensions; i++) {
            if (this.position[i] != p.position[i]) {
                return false;
            }
        }
        return true;
    }
}
