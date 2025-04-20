package edu.jhuapl.trinity.data;

import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class Manifold {
    private ArrayList<Point3D> points;
    private String label; //common data label of manifold (from input data)
    private Color color; //color of representative object
    public SimpleBooleanProperty visible;

    public Manifold(ArrayList<Point3D> points, String label, String name, Color color) {
        this.points = new ArrayList<>(points.size());
        this.points.addAll(points);
        this.label = label;
        this.color = color;
        visible = new SimpleBooleanProperty(true);
    }

    /**
     * Provides lookup mechanism to find any object model that is currently
     * anchored in the system.
     */
    private static HashMap<String, Manifold> globalManifoldMap = new HashMap<>();
    public static HashMap<Manifold, Manifold3D> globalManifoldToManifold3DMap = new HashMap<>();

    public static Collection<Manifold> getManifolds() {
        return globalManifoldMap.values();
    }

    public static Manifold getManifold(String label) {
        return globalManifoldMap.get(label);
    }

    public static void addManifold(Manifold manifold) {
        globalManifoldMap.put(manifold.getLabel(), manifold);
    }

    public static void addAllManifolds(List<Manifold> manifolds) {
        manifolds.forEach(d -> {
            globalManifoldMap.put(d.getLabel(), d);
        });
    }

    public static void removeAllManifolds() {
        globalManifoldMap.clear();
        globalManifoldToManifold3DMap.clear();
    }

    public static Manifold removeManifold(String label) {
        Manifold removed = globalManifoldMap.remove(label);
        return removed;
    }

    public static void updateManifold(String label, Manifold manifold) {
        globalManifoldMap.put(label, manifold);
    }

    public static Color getColorByLabel(String label) {
        Manifold fl = Manifold.getManifold(label);
        if (null == fl)
            return Color.ALICEBLUE;
        return fl.getColor();
    }

    public static boolean visibilityByLabel(String label) {
        Manifold fl = Manifold.getManifold(label);
        if (null == fl)
            return true;
        return fl.getVisible();
    }

    public static void setAllVisible(boolean visible) {
        globalManifoldMap.forEach((s, fl) -> {
            fl.setVisible(visible);
        });
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
     * @return the points
     */
    public ArrayList<Point3D> getPoints() {
        return points;
    }

    /**
     * @param points the points to set
     */
    public void setPoints(ArrayList<Point3D> points) {
        this.points = points;
    }
}
