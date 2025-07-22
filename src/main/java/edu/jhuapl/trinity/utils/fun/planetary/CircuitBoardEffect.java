package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Sean Phillips
 */
public class CircuitBoardEffect implements PlanetaryEffect {

    private final Group circuitGroup = new Group();
    private final Map<Node, Glow> glowMap = new HashMap<>();
    private final Random random = new Random();

    private Circle planetCircle;
    private double radius;
    private double innerPadding;

    private int horizontalLines;
    private int verticalLines;
    private Color lineColor;
    private double lineThickness;
    private double padRadius;
    private boolean drawPads;
    private boolean drawDots;
    private boolean drawJumpers;
    private boolean glowEnabled;
    private boolean animateGlow;

    private Timeline glowAnimator;

    private CircuitBoardEffect(Builder builder) {
        this.horizontalLines = builder.horizontalLines;
        this.verticalLines = builder.verticalLines;
        this.lineColor = builder.lineColor;
        this.lineThickness = builder.lineThickness;
        this.innerPadding = builder.innerPadding;
        this.padRadius = builder.padRadius;
        this.drawPads = builder.drawPads;
        this.drawDots = builder.drawDots;
        this.drawJumpers = builder.drawJumpers;
        this.glowEnabled = builder.glowEnabled;
        this.animateGlow = builder.animateGlow;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        this.planetCircle = disc.getPlanetCircle();
        this.radius = disc.getRadius();

        circuitGroup.getChildren().clear();
        glowMap.clear();
        if (glowAnimator != null) {
            glowAnimator.stop();
        }

        double cx = planetCircle.getCenterX();
        double cy = planetCircle.getCenterY();
        double effectiveRadius = radius - innerPadding;
        double diameter = effectiveRadius * 2;

        double hSpacing = diameter / (horizontalLines - 1);
        double vSpacing = diameter / (verticalLines - 1);

        Path horizontalPath = new Path();
        horizontalPath.setStroke(lineColor);
        horizontalPath.setStrokeWidth(lineThickness);
        horizontalPath.setMouseTransparent(true);
        horizontalPath.setStrokeType(StrokeType.CENTERED);

        Path verticalPath = new Path();
        verticalPath.setStroke(lineColor);
        verticalPath.setStrokeWidth(lineThickness);
        verticalPath.setMouseTransparent(true);
        verticalPath.setStrokeType(StrokeType.CENTERED);

        Path jumperPath = new Path();
        jumperPath.setStroke(lineColor);
        jumperPath.setStrokeWidth(lineThickness);
        jumperPath.setMouseTransparent(true);
        jumperPath.setStrokeType(StrokeType.CENTERED);
        jumperPath.setFill(null);

        List<Point2D> padPoints = new ArrayList<>();

        // Horizontal lines + dots
        for (int i = 0; i < horizontalLines; i++) {
            double y = cy - effectiveRadius + i * hSpacing;
            double xStart = cx - effectiveRadius;
            while (xStart < cx + effectiveRadius) {
                double segmentLength = 10 + random.nextDouble() * 20;
                double gapLength = 5 + random.nextDouble() * 10;
                double xEnd = Math.min(xStart + segmentLength, cx + effectiveRadius);

                horizontalPath.getElements().addAll(
                    new MoveTo(xStart, y),
                    new LineTo(xEnd, y)
                );

                if (drawDots && random.nextDouble() < 0.2) {
                    double dotX = xStart + segmentLength / 2;
                    Circle dot = new Circle(dotX, y, 1.5, lineColor);
                    decorate(dot);
                }

                xStart = xEnd + gapLength;
            }
        }

        // Vertical lines + dots + pad points
        for (int j = 0; j < verticalLines; j++) {
            double x = cx - effectiveRadius + j * vSpacing;
            double yStart = cy - effectiveRadius;
            while (yStart < cy + effectiveRadius) {
                double segmentLength = 10 + random.nextDouble() * 20;
                double gapLength = 5 + random.nextDouble() * 10;
                double yEnd = Math.min(yStart + segmentLength, cy + effectiveRadius);

                verticalPath.getElements().addAll(
                    new MoveTo(x, yStart),
                    new LineTo(x, yEnd)
                );

                for (int i = 0; i < horizontalLines; i++) {
                    double y = cy - effectiveRadius + i * hSpacing;
                    if (y >= yStart && y <= yEnd) {
                        padPoints.add(new Point2D(x, y));
                    }
                }

                if (drawDots && random.nextDouble() < 0.2) {
                    double dotY = yStart + segmentLength / 2;
                    Circle dot = new Circle(x, dotY, 1.5, lineColor);
                    decorate(dot);
                }

                yStart = yEnd + gapLength;
            }
        }

        // Jumpers as QuadCurveTo paths
        if (drawJumpers) {
            for (int j = 0; j < verticalLines - 2; j++) {
                if (random.nextDouble() < 0.3) {
                    double x1 = cx - effectiveRadius + j * vSpacing;
                    double x2 = cx - effectiveRadius + (j + 2) * vSpacing;
                    double y = cy + effectiveRadius - (random.nextDouble() * diameter);
                    double controlY = y - 6 - random.nextDouble() * 4;

                    jumperPath.getElements().addAll(
                        new MoveTo(x1, y),
                        new QuadCurveTo((x1 + x2) / 2, controlY, x2, y)
                    );
                }
            }
        }

        circuitGroup.getChildren().addAll(horizontalPath, verticalPath);
        if (drawJumpers) {
            circuitGroup.getChildren().add(jumperPath);
        }

        if (drawPads) {
            for (Point2D pt : padPoints) {
                Circle pad = new Circle(pt.getX(), pt.getY(), padRadius, lineColor);
                decorate(pad);
            }
        }

        ClipUtils.applyCircularClip(circuitGroup, planetCircle, innerPadding);

        if (glowEnabled) {
            Glow glowH = new Glow(0.0);
            horizontalPath.setEffect(glowH);
            glowMap.put(horizontalPath, glowH);

            Glow glowV = new Glow(0.0);
            verticalPath.setEffect(glowV);
            glowMap.put(verticalPath, glowV);

            if (drawJumpers) {
                Glow glowJ = new Glow(0.0);
                jumperPath.setEffect(glowJ);
                glowMap.put(jumperPath, glowJ);
            }
        }

        if (animateGlow) {
            startGlowAnimation();
        }
    }

    private void decorate(Node node) {
        node.setMouseTransparent(true);

        if (node instanceof Shape shape) {
            shape.setStrokeType(StrokeType.CENTERED);
            shape.setStroke(lineColor);
            shape.setStrokeWidth(lineThickness);
            shape.setFill((shape instanceof Line || shape instanceof Arc || shape instanceof Path) ? null : lineColor);
        }

        // Apply glow to individual dots/pads
        if (glowEnabled && !(node instanceof Path)) {
            Glow glow = new Glow(0.0);
            node.setEffect(glow);
            glowMap.put(node, glow);
        }

        circuitGroup.getChildren().add(node);
    }

    private void startGlowAnimation() {
        glowAnimator = new Timeline(
            new KeyFrame(Duration.seconds(0.3), e -> animateRandomGlow())
        );
        glowAnimator.setCycleCount(Animation.INDEFINITE);
        glowAnimator.play();
    }

    private void animateRandomGlow() {
        List<Node> nodes = new ArrayList<>(glowMap.keySet());
        if (nodes.isEmpty()) {
            return;
        }

        int pulses = Math.min(10, nodes.size());
        for (int i = 0; i < pulses; i++) {
            Node node = nodes.get(random.nextInt(nodes.size()));
            Glow glow = glowMap.get(node);
            if (glow != null) {
                double intensity = 0.3 + random.nextDouble() * 0.7;

                Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0)),
                    new KeyFrame(Duration.seconds(0.3), new KeyValue(glow.levelProperty(), intensity)),
                    new KeyFrame(Duration.seconds(0.6), new KeyValue(glow.levelProperty(), 0))
                );
                pulse.play();
            }
        }
    }

    @Override
    public void update(double occlusion) {
        circuitGroup.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return circuitGroup;
    }

    public static class Builder {

        private int baseLineCount = 6;
        private int maxLineCount = 40;
        private int horizontalLines;
        private int verticalLines;

        private Color lineColor = Color.LIMEGREEN;
        private double lineThickness = 2.0;
        private double innerPadding = 6.0;
        private double padRadius = 2.0;
        private boolean drawPads = true;
        private boolean drawDots = true;
        private boolean drawJumpers = true;
        private boolean glowEnabled = false;
        private boolean animateGlow = false;

        public Builder() {
            this.horizontalLines = baseLineCount;
            this.verticalLines = baseLineCount;
        }

        public Builder baseLineCount(int base) {
            this.baseLineCount = Math.max(1, base);
            return this;
        }

        public Builder maxLineCount(int max) {
            this.maxLineCount = Math.max(baseLineCount + 1, max);
            return this;
        }

        public Builder horizontalDensity(double d) {
            d = clampDensity(d);
            this.horizontalLines = (int) (baseLineCount + (maxLineCount - baseLineCount) * d);
            return this;
        }

        public Builder verticalDensity(double d) {
            d = clampDensity(d);
            this.verticalLines = (int) (baseLineCount + (maxLineCount - baseLineCount) * d);
            return this;
        }

        private double clampDensity(double d) {
            return Math.max(0.05, Math.min(1.0, d));
        }

        public Builder horizontalLines(int count) {
            this.horizontalLines = count;
            return this;
        }

        public Builder verticalLines(int count) {
            this.verticalLines = count;
            return this;
        }

        public Builder lineColor(Color color) {
            this.lineColor = color;
            return this;
        }

        public Builder lineThickness(double thickness) {
            this.lineThickness = thickness;
            return this;
        }

        public Builder innerPadding(double padding) {
            this.innerPadding = padding;
            return this;
        }

        public Builder padRadius(double radius) {
            this.padRadius = radius;
            return this;
        }

        public Builder drawPads(boolean val) {
            this.drawPads = val;
            return this;
        }

        public Builder drawDots(boolean val) {
            this.drawDots = val;
            return this;
        }

        public Builder drawJumpers(boolean val) {
            this.drawJumpers = val;
            return this;
        }

        public Builder glowEnabled(boolean val) {
            this.glowEnabled = val;
            return this;
        }

        public Builder animateGlow(boolean val) {
            this.animateGlow = val;
            return this;
        }

        public CircuitBoardEffect build() {
            return new CircuitBoardEffect(this);
        }
    }
}
