package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;

/**
 *
 * @author Sean Phillips
 */
public class GeographyUtils {
    public static double computeContinentScaleNormalized(int numContinents, double fillRatio) {
        double totalArea = Math.PI;
        double avgArea = (totalArea * fillRatio) / Math.max(1, numContinents);
        double scale = Math.sqrt(avgArea / Math.PI); // This is in normalized radius (1.0 = effectiveRadius)
        return Math.max(scale, 0.08); // Enforce minimum of 8% of disc radius
    }

    public static Color pickRandomColor(List<Color> colors, Color fallback) {
        if (colors == null || colors.isEmpty()) {
            return fallback;
        }
        Random random = new Random();
        return colors.get(random.nextInt(colors.size()));
    }    
//    public static  List<Point2D> generateNonOverlappingCenters(int count, double minDistance, double cx, double cy, double effectiveRadius) {
//        List<Point2D> centers = new ArrayList<>();
//
//        if (count <= 0) return centers;
//
//        double ringSpacing = minDistance; // distance between rings
//        double angleStep;
//        double radius;
//
//        // Add first point at center if fits
//        if (count > 0) {
//            centers.add(new Point2D(cx, cy));
//            if (count == 1) return centers;
//        }
//
//        int pointsAdded = 1;
//        int ringIndex = 1;
//
//        while (pointsAdded < count) {
//            radius = ringIndex * ringSpacing;
//            if (radius > effectiveRadius) break; // Outside planet disc
//
//            // Calculate how many points can fit evenly on this ring
//            double circumference = 2 * Math.PI * radius;
//            int pointsInRing = (int) Math.floor(circumference / minDistance);
//            if (pointsInRing == 0) pointsInRing = 1;
//
//            angleStep = 2 * Math.PI / pointsInRing;
//
//            for (int i = 0; i < pointsInRing && pointsAdded < count; i++) {
//                double angle = i * angleStep;
//                double x = cx + radius * Math.cos(angle);
//                double y = cy + radius * Math.sin(angle);
//                centers.add(new Point2D(x, y));
//                pointsAdded++;
//            }
//
//            ringIndex++;
//        }
//
//        return centers;
//    }
public static List<Point2D> generateNonOverlappingCenters(
        int count, double minDistance, double cx, double cy, double effectiveRadius
) {
    List<Point2D> centers = new ArrayList<>();
    int attempts = 0;
    int maxAttempts = count * 30;

//    double placementRadius = effectiveRadius - minDistance * 0.8;
    double placementRadius = effectiveRadius * 0.95; // - minDistance * 0.8;

    while (centers.size() < count && attempts < maxAttempts) {
        attempts++;

        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * placementRadius;
        double x = cx + distance * Math.cos(angle);
        double y = cy + distance * Math.sin(angle);
        Point2D candidate = new Point2D(x, y);

        boolean tooClose = centers.stream().anyMatch(existing ->
                candidate.distance(existing) < minDistance);

        if (!tooClose) {
            centers.add(candidate);
        }
    }

    // Fallback: if not enough were added, relax the spacing
    while (centers.size() < count) {
        double angle = Math.random() * 2 * Math.PI;
//        double distance = Math.random() * (effectiveRadius - minDistance * 0.4);
        double distance = Math.random() * (effectiveRadius * 0.95);
        double x = cx + distance * Math.cos(angle);
        double y = cy + distance * Math.sin(angle);
        centers.add(new Point2D(x, y));
    }

    return centers;
}

    public static  List<Point2D> generateContinentPolygon(int minPts, int maxPts, double scale) {
        Random random = new Random();
        int pts = minPts + random.nextInt(maxPts - minPts + 1);
        List<Point2D> result = new ArrayList<>();

        double angleStep = 2 * Math.PI / pts;
        for (int i = 0; i < pts; i++) {
            double angle = i * angleStep + random.nextDouble() * angleStep * 0.4;
            double r = scale * (0.5 + random.nextDouble() * 0.5);
            result.add(new Point2D(Math.cos(angle) * r, Math.sin(angle) * r));
        }
        return result;
    }

    public static  Path buildContinentPath(List<Point2D> globalPoints, double cx, double cy, double scale, Paint fill, Paint stroke) {
    Path path = new Path();
    if (globalPoints.size() < 3) return path;

    // Start with first point
    Point2D start = globalPoints.get(0);
    path.getElements().add(new MoveTo(start.getX(), start.getY()));

    for (int i = 1; i < globalPoints.size() - 1; i++) {
        Point2D p1 = globalPoints.get(i);
        Point2D p2 = globalPoints.get(i + 1);

        // Middle control point
        Point2D control = p1;
        Point2D end = p1.midpoint(p2);

        path.getElements().add(new QuadCurveTo(
                control.getX(), control.getY(),
                end.getX(), end.getY()));
    }

    // Final curve from second-to-last to start
    Point2D last = globalPoints.get(globalPoints.size() - 1);
    Point2D control = last;
    Point2D end = last.midpoint(start);

    path.getElements().add(new QuadCurveTo(
            control.getX(), control.getY(),
            end.getX(), end.getY()));

    path.getElements().add(new ClosePath());

    path.setFill(fill != null ? fill : Color.DARKBLUE);
    path.setStroke(stroke != null ? stroke : Color.CYAN);
    path.setMouseTransparent(true);

    return path;
}

    public static Point2D randomPointInPolygon(List<Point2D> polygon) {
    if (polygon.isEmpty()) return new Point2D(0, 0);

    // Compute bounding box of polygon
    double minX = polygon.stream().mapToDouble(Point2D::getX).min().orElse(0);
    double maxX = polygon.stream().mapToDouble(Point2D::getX).max().orElse(0);
    double minY = polygon.stream().mapToDouble(Point2D::getY).min().orElse(0);
    double maxY = polygon.stream().mapToDouble(Point2D::getY).max().orElse(0);
    Random random = new Random();
    // Try to find a random point inside the bounding box
    for (int tries = 0; tries < 50; tries++) {
        double x = minX + random.nextDouble() * (maxX - minX);
        double y = minY + random.nextDouble() * (maxY - minY);
        Point2D candidate = new Point2D(x, y);
        if (pointInPolygon(candidate, polygon)) {
            return candidate;
        }
    }

    // Fallback to first vertex
    return polygon.get(0);
}

    public static boolean pointInPolygon(Point2D p, List<Point2D> polygon) {
        int crossings = 0;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            Point2D pi = polygon.get(i);
            Point2D pj = polygon.get(j);
            if (((pi.getY() > p.getY()) != (pj.getY() > p.getY()))
                    && (p.getX() < (pj.getX() - pi.getX()) * (p.getY() - pi.getY()) / (pj.getY() - pi.getY()) + pi.getX())) {
                crossings++;
            }
        }
        return (crossings % 2 == 1);
    }    
}
