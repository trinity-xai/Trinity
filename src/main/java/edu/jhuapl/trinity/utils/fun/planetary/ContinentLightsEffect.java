package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public class ContinentLightsEffect implements PlanetaryEffect {

    private final Group rootGroup = new Group();
    private Circle planetCircle;
    private double radius;
    private double minDistanceMultiplier;
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
    private Boolean enableTwinkle;
    private double cx, cy, effectiveRadius;

    private ContinentLightsEffect(Builder builder) {
        minDistanceMultiplier = builder.minDistanceMultiplier;
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
        enableTwinkle = builder.enableTwinkle;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        planetCircle = disc.getPlanetCircle();
        radius = disc.getRadius();
        cx = planetCircle.getCenterX();
        cy = planetCircle.getCenterY();
        effectiveRadius = radius - innerPadding;

        rootGroup.getChildren().clear();

        continentScale *= effectiveRadius;
        double minDistance = continentScale * minDistanceMultiplier;

        //Compute valid centers inside the disc
        List<Point2D> centers = GeographyUtils.generateNonOverlappingCenters(
            numContinents, minDistance, cx, cy, effectiveRadius
        );

        for (Point2D offset : centers) {
            // Build and offset a polygon
            List<Point2D> localPoints = GeographyUtils.generateContinentPolygon(
                minVertices, maxVertices, continentScale
            );
            List<Point2D> offsetPoints = localPoints.stream()
                .map(p -> p.add(offset))
                .collect(Collectors.toList());
            //Add continent path
            Path continent = GeographyUtils.buildContinentPath(offsetPoints, cx, cy,
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
            List<Circle> clusterDots = new ArrayList<>();
            for (int i = 0; i < clustersPerContinent; i++) {
                double clusterSize = maxLightSize * 1.5;
                Color clusterColor = GeographyUtils.pickRandomColor(clusterColors, lightColor).deriveColor(0, 1, 1, 1.0);
                double haloSize = clusterSize * 2.5;
                Point2D cluster = GeographyUtils.randomPointInPolygon(offsetPoints);
                clusters.add(cluster);
                // Draw glow halo behind cluster center
                gc.setFill(clusterColor.deriveColor(0, 1, 1, 0.35));
                gc.fillOval(cluster.getX() - haloSize / 2, cluster.getY() - haloSize / 2, haloSize, haloSize);
                //draw the cluster center point dot
                Circle dot = new Circle(cluster.getX(), cluster.getY(), clusterSize / 2);
                dot.setFill(clusterColor);
                dot.setMouseTransparent(true);
                clusterDots.add(dot);
                rootGroup.getChildren().add(dot);
            }
            if (enableTwinkle) {
                Timeline twinkleTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(2), e -> twinkleRandomClusters(clusterDots))
                );
                twinkleTimeline.setCycleCount(Animation.INDEFINITE);
                twinkleTimeline.play();
            }
            //draw points, brighter if close to a cluster center
            for (int i = 0; i < lightsPerContinent; i++) {
                Point2D p = GeographyUtils.randomPointInPolygon(offsetPoints);
                double brightness = 0.4;
                for (Point2D cluster : clusters) {
                    double dist = p.distance(cluster);
                    //Steep Gaussian like Falloff for brightness
                    brightness += Math.exp(-dist * 10) * 0.6;
                }
                brightness = Math.min(1.0, brightness);
                Color baseLight = GeographyUtils.pickRandomColor(lightColors, lightColor);
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

    private void twinkleRandomClusters(List<Circle> clusters) {
        int pulses = Math.min(3, clusters.size());  // Only a few at a time
        Set<Circle> chosen = new HashSet<>();

        while (chosen.size() < pulses) {
            Circle randomDot = clusters.get((int) (Math.random() * clusters.size()));
            if (chosen.add(randomDot)) {
                FadeTransition ft = new FadeTransition(Duration.seconds(1.0 + Math.random()), randomDot);
                ft.setFromValue(0.3);
                ft.setToValue(1.0);
                ft.setAutoReverse(true);
                ft.setCycleCount(2);  // fade in + fade out
                ft.play();
            }
        }
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
        private double minDistanceMultiplier = 1.0;
        private int numContinents = 3;
        private int minVertices = 6;
        private int maxVertices = 12;
        private Double fillRatio = 0.5;
        private Double continentScale = 0.5;
        private int lightsPerContinent = 200;
        private int clustersPerContinent = 3;
        private Color outlineColor = Color.DARKSLATEBLUE;
        private Color lightColor = Color.GOLD;
        private List<Color> clusterColors = null;
        private List<Color> lightColors = null;
        private double maxLightSize = 2.0;
        private double innerPadding = 3.0;
        private Color fillColor = null; // null = no fill
        private Boolean enableTwinkle = false;

        public Builder minDistanceMultiplier(double value) {
            this.minDistanceMultiplier = value;
            return this;
        }

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

        public Builder enableTwinkle(Boolean val) {
            this.enableTwinkle = val;
            return this;
        }

        public ContinentLightsEffect build() {
            return new ContinentLightsEffect(this);
        }
    }
}
