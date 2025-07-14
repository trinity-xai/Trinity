package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;

/**
 *
 * @author Sean Phillips
 */
public class ContinentLightsEffect implements PlanetaryEffect {

    private final Group rootGroup = new Group();
    private Circle planetCircle;
    private double radius;

    private final int numContinents;
    private final int minVertices;
    private final int maxVertices;
    private Double continentScale;
    private Double fillRatio;
    private final int lightsPerContinent;
    private final int clustersPerContinent;
    private final Color outlineColor;
    private final Color fillColor;    
    private final Color lightColor;
    private final double maxLightSize;
    private double innerPadding = 6.0;
    private List<Color> clusterColors;
    private List<Color> lightColors;

    private double cx,cy,effectiveRadius;
    private final Random random = new Random();

    private ContinentLightsEffect(Builder builder) {
        numContinents = builder.numContinents;
        minVertices = builder.minVertices;
        maxVertices = builder.maxVertices;
        continentScale = builder.continentScale;
        lightsPerContinent = builder.lightsPerContinent;
        clustersPerContinent = builder.clustersPerContinent;
        outlineColor = builder.outlineColor;
        lightColor = builder.lightColor;
        maxLightSize = builder.maxLightSize;
        innerPadding = builder.innerPadding;
        fillRatio = builder.fillRatio;
        clusterColors = builder.clusterColors;
        lightColors = builder.lightColors;    
        fillColor = builder.fillColor;       
    }

    @Override
public void attachTo(PlanetaryDisc disc) {
    planetCircle = disc.getPlanetCircle();
    radius = disc.getRadius();
    cx = planetCircle.getCenterX();
    cy = planetCircle.getCenterY();
    effectiveRadius = radius - innerPadding;

    rootGroup.getChildren().clear();

    if(null == fillRatio)
        fillRatio = 0.5;
    double minDistance;
    double scaleNormalized = computeContinentScaleNormalized(numContinents, fillRatio);
    if(null == continentScale) {
        continentScale = scaleNormalized * effectiveRadius;
    } else {
        continentScale *= effectiveRadius;
    }
    minDistance = continentScale * 2.0;

    //Compute valid centers inside the disc
    List<Point2D> centers = generateNonOverlappingCenters(
        numContinents, minDistance, cx, cy, effectiveRadius
    );

    for (Point2D offset : centers) {
        // Build and offset a polygon
        List<Point2D> localPoints = generateContinentPolygon(
            minVertices, maxVertices, continentScale
        );
        List<Point2D> offsetPoints = localPoints.stream()
                .map(p -> p.add(offset))
                .collect(Collectors.toList());
        //Add continent path
        Path continent = buildContinentPath(offsetPoints, cx, cy,
            effectiveRadius, fillColor, outlineColor);
        rootGroup.getChildren().add(continent);
//        //DEBUG: show center dots
//        Circle debugDot = new Circle(offset.getX(), offset.getY(), 2, Color.LIME);
//        rootGroup.getChildren().add(debugDot);
        //Canvas lights
        Canvas canvas = new Canvas(radius * 2, radius * 2);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        //Pick cluster locations and draw them with some attitude
        List<Point2D> clusters = new ArrayList<>();
        for (int i = 0; i < clustersPerContinent; i++) {
            double clusterSize = maxLightSize * 1.5;
            Color clusterColor = pickRandomColor(clusterColors, lightColor).deriveColor(0, 1, 1, 1.0);
            double haloSize = clusterSize * 2.5;
            Point2D cluster = randomPointInPolygon(offsetPoints);
            clusters.add(cluster);
            // Draw glow halo behind cluster center
            gc.setFill(clusterColor.deriveColor(0, 1, 1, 0.15));
            gc.fillOval(cluster.getX() - haloSize / 2, cluster.getY() - haloSize / 2, haloSize, haloSize);
            //draw the cluster center point dot 
            gc.setFill(clusterColor);
            gc.fillOval(cluster.getX() - clusterSize / 2, cluster.getY() - clusterSize / 2, clusterSize, clusterSize);
        }
        //draw points, brighter if close to a cluster center
        for (int i = 0; i < lightsPerContinent; i++) {
            Point2D p = randomPointInPolygon(offsetPoints);
            double brightness = 0.4;
            for (Point2D cluster : clusters) {
                double dist = p.distance(cluster);
                //Steep Gaussian like Falloff for brightness
                brightness += Math.exp(-dist * 10) * 0.6;
            }
            brightness = Math.min(1.0, brightness);
            Color baseLight = pickRandomColor(lightColors, lightColor);
            Color glow = baseLight.deriveColor(0, 1, 1, brightness);
            double size = maxLightSize * (0.4 + brightness * 0.6);
            gc.setFill(glow);
            gc.fillOval(p.getX() - size / 2, p.getY() - size / 2, size, size);
        }

        canvas.setMouseTransparent(true);
        rootGroup.getChildren().add(canvas);
    }
    //apply clipping last
    ClipUtils.applyCircularClip(rootGroup, planetCircle, innerPadding);
}
    private double computeContinentScaleNormalized(int numContinents, double fillRatio) {
        double totalArea = Math.PI;
        double avgArea = (totalArea * fillRatio) / Math.max(1, numContinents);
        double scale = Math.sqrt(avgArea / Math.PI); // This is in normalized radius (1.0 = effectiveRadius)
        return Math.max(scale, 0.08); // Enforce minimum of 8% of disc radius
    }
    private List<Point2D> generateNonOverlappingCenters(int count, double minDistance, double cx, double cy, double effectiveRadius) {
        List<Point2D> centers = new ArrayList<>();

        if (count <= 0) return centers;

        double ringSpacing = minDistance; // distance between rings
        double angleStep;
        double radius;

        // Add first point at center if fits
        if (count > 0) {
            centers.add(new Point2D(cx, cy));
            if (count == 1) return centers;
        }

        int pointsAdded = 1;
        int ringIndex = 1;

        while (pointsAdded < count) {
            radius = ringIndex * ringSpacing;
            if (radius > effectiveRadius) break; // Outside planet disc

            // Calculate how many points can fit evenly on this ring
            double circumference = 2 * Math.PI * radius;
            int pointsInRing = (int) Math.floor(circumference / minDistance);
            if (pointsInRing == 0) pointsInRing = 1;

            angleStep = 2 * Math.PI / pointsInRing;

            for (int i = 0; i < pointsInRing && pointsAdded < count; i++) {
                double angle = i * angleStep;
                double x = cx + radius * Math.cos(angle);
                double y = cy + radius * Math.sin(angle);
                centers.add(new Point2D(x, y));
                pointsAdded++;
            }

            ringIndex++;
        }

        return centers;
    }

    private List<Point2D> generateContinentPolygon(int minPts, int maxPts, double scale) {
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

    private Path buildContinentPath(List<Point2D> globalPoints, double cx, double cy, double scale, Paint fill, Paint stroke) {
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

    private Point2D randomPointInPolygon(List<Point2D> polygon) {
    if (polygon.isEmpty()) return new Point2D(0, 0);

    // Compute bounding box of polygon
    double minX = polygon.stream().mapToDouble(Point2D::getX).min().orElse(0);
    double maxX = polygon.stream().mapToDouble(Point2D::getX).max().orElse(0);
    double minY = polygon.stream().mapToDouble(Point2D::getY).min().orElse(0);
    double maxY = polygon.stream().mapToDouble(Point2D::getY).max().orElse(0);

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

    private boolean pointInPolygon(Point2D p, List<Point2D> polygon) {
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
    private Color pickRandomColor(List<Color> colors, Color fallback) {
        if (colors == null || colors.isEmpty()) {
            return fallback;
        }
        return colors.get(random.nextInt(colors.size()));
    }
    @Override
    public void update(double occlusion) {
        rootGroup.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return rootGroup;
    }

    public static class Builder {

        private int numContinents = 3;
        private int minVertices = 6;
        private int maxVertices = 12;
        private Double fillRatio = null;
        private Double continentScale = null;
        private int lightsPerContinent = 200;
        private int clustersPerContinent = 3;
        private Color outlineColor = Color.DARKSLATEBLUE;
        private Color lightColor = Color.GOLD;
        private List<Color> clusterColors = null;
        private List<Color> lightColors = null;        
        private double maxLightSize = 2.0;
        private double innerPadding = 6.0;
        private Color fillColor = null; // null = no fill

        public Builder clusterColors(List<Color> colors) {
            this.clusterColors = colors;
            return this;
        }

        public Builder lightColors(List<Color> colors) {
            this.lightColors = colors;
            return this;
        }        
        public Builder fillColor(Color color) {
            this.fillColor = color;
            return this;
        }
        public Builder innerPadding(double value) {
            this.innerPadding = value;
            return this;
        }
        public Builder numContinents(int val) {
            this.numContinents = val;
            return this;
        }

        public Builder vertexRange(int min, int max) {
            this.minVertices = min;
            this.maxVertices = max;
            return this;
        }

        public Builder fillRatio(double val) {
            this.fillRatio = val;
            return this;
        }

        public Builder continentScale(double val) {
            this.continentScale = val;
            return this;
        }

        public Builder lightsPerContinent(int val) {
            this.lightsPerContinent = val;
            return this;
        }

        public Builder clustersPerContinent(int val) {
            this.clustersPerContinent = val;
            return this;
        }

        public Builder outlineColor(Color val) {
            this.outlineColor = val;
            return this;
        }

        public Builder lightColor(Color val) {
            this.lightColor = val;
            return this;
        }

        public Builder maxLightSize(double val) {
            this.maxLightSize = val;
            return this;
        }

        public ContinentLightsEffect build() {
            return new ContinentLightsEffect(this);
        }
    }
}
