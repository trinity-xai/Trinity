package edu.jhuapl.trinity.utils.clustering;

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
