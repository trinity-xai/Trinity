package edu.jhuapl.trinity.data;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class Distance {
    private String metric; //Used to lookup into Metric enum later
    private double value; //the computed value
    private int width; //width of the connector
    private Point3D point1;
    private Point3D point2;
    private String label;
    private Color color;
    public SimpleBooleanProperty visible;

    public Distance(String label, Color color, String metric, Integer width) {
        this.label = label;
        this.color = color;
        this.width = width;
        if (null == metric)
            metric = "euclidean";
        else
            this.metric = metric;
        visible = new SimpleBooleanProperty(true);
    }

    /**
     * Provides lookup mechanism to find any object model that is currently
     * anchored in the system.
     */
    private static HashMap<String, Distance> globalDistanceMap = new HashMap<>();

    public static Collection<Distance> getDistances() {
        return globalDistanceMap.values();
    }

    public static Distance getDistance(String label) {
        return globalDistanceMap.get(label);
    }

    public static void addDistance(Distance distance) {
        globalDistanceMap.put(distance.getLabel(), distance);
    }

    public static void addAllDistances(List<Distance> distances) {
        distances.forEach(d -> {
            globalDistanceMap.put(d.getLabel(), d);
        });
    }

    public static void removeAllDistances() {
        globalDistanceMap.clear();
    }

    public static Distance removeDistance(String label) {
        Distance removed = globalDistanceMap.remove(label);
        return removed;
    }

    public static void updateDistance(String label, Distance distance) {
        globalDistanceMap.put(label, distance);
    }

    public static Color getColorByLabel(String label) {
        Distance fl = Distance.getDistance(label);
        if (null == fl)
            return Color.ALICEBLUE;
        return fl.getColor();
    }

    public static boolean visibilityByLabel(String label) {
        Distance fl = Distance.getDistance(label);
        if (null == fl)
            return true;
        return fl.getVisible();
    }

    public static void setAllVisible(boolean visible) {
        globalDistanceMap.forEach((s, fl) -> {
            fl.setVisible(visible);
        });
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * @return the point1
     */
    public Point3D getPoint1() {
        return point1;
    }

    /**
     * @param point1 the point1 to set
     */
    public void setPoint1(Point3D point1) {
        this.point1 = point1;
    }

    /**
     * @return the point2
     */
    public Point3D getPoint2() {
        return point2;
    }

    /**
     * @param point2 the point2 to set
     */
    public void setPoint2(Point3D point2) {
        this.point2 = point2;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    public SimpleBooleanProperty visibleProperty() {
        return this.visible;
    }

    public java.lang.Boolean getVisible() {
        return this.visibleProperty().get();
    }

    public void setVisible(final java.lang.Boolean visible) {
        this.visibleProperty().set(visible);
    }

    /**
     * @return the metric
     */
    public String getMetric() {
        return metric;
    }

    /**
     * @param metric the metric to set
     */
    public void setMetric(String metric) {
        this.metric = metric;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }
}
