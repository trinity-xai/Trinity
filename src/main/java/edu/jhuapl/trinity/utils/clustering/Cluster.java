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

import java.util.ArrayList;
import java.util.List;

// At the beginning of each iteration, clusterPoints is empty, and the center is the new mean point
public class Cluster extends Point {

    private List<Integer> clusterPointIndices;
    private double[] sumOfPoints;
    private List<Point> points;

    public Cluster(int dimensions) {
        super(dimensions);
        clusterPointIndices = new ArrayList<>();
        this.sumOfPoints = new double[this.dimensions];
        points = new ArrayList<>();
    }

    public Cluster(Point p) {
        super(p.position);
        clusterPointIndices = new ArrayList<>();
        this.sumOfPoints = new double[this.dimensions];
        points = new ArrayList<>();
    }

    public void addPointToCluster(int index, Point p) {
        //Point p = KMeans.points.get(index);
        clusterPointIndices.add(index);
        points.add(p);
        double[] po = p.getPosition();
        for (int i = 0; i < this.dimensions; ++i) {
            sumOfPoints[i] += po[i];
        }
    }

    public Cluster getNewCenter() {
        double[] centerPosition = new double[this.dimensions];
        for (int i = 0; i < this.dimensions; ++i) {
            centerPosition[i] = sumOfPoints[i] / this.clusterPointIndices.size();
        }
        return new Cluster(new Point(centerPosition));
    }

    public double evaluate(ArrayList<Point> points) {
        double ret = 0.0;
        for (int in : clusterPointIndices) {
            ret += this.dist(points.get(in));
        }
        return ret;
    }

    public ArrayList<Point> getClusterPoints() {
        return new ArrayList<>(points);
    }

    public ArrayList<Integer> belongingPoints() {
        return new ArrayList<>(clusterPointIndices);
    }
}
