package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Lightweight grid for a Poincare disk embedded in the XZ plane (Y=0) through
 * the center of a 3D Poincare ball. All circles are hyperbolic equidistance
 * contours, while radial spokes are hyperbolic geodesics.
 *
 * Each category of major/minor lines is batched into one TriangleMesh:
 * no Sphere or Cylinder is created per ring/segment/spoke.
 *
 * A grid is a visual reference only, never graph data or a pick target.
 */
public final class PoincareGrid3D extends Group {
    private final DoubleProperty ballRadius = new SimpleDoubleProperty(this, "ballRadius", 100.0);
    private final DoubleProperty ringStep = new SimpleDoubleProperty(this, "ringStep", 0.5);
    private final IntegerProperty ringCount = new SimpleIntegerProperty(this, "ringCount", 12);
    private final IntegerProperty spokeCount = new SimpleIntegerProperty(this, "spokeCount", 12);
    private final IntegerProperty majorEvery = new SimpleIntegerProperty(this, "majorEvery", 2);
    private final IntegerProperty circleSegments = new SimpleIntegerProperty(this, "circleSegments", 128);
    private final DoubleProperty lineThickness = new SimpleDoubleProperty(this, "lineThickness", -1.0);
    private final ObjectProperty<Color> minorLineColor = new SimpleObjectProperty<>(
        this, "minorLineColor", Color.color(0.45, 0.58, 0.92, 0.24));
    private final ObjectProperty<Color> majorLineColor = new SimpleObjectProperty<>(
        this, "majorLineColor", Color.color(0.65, 0.78, 1.0, 0.48));

    private final MeshView minorView = new MeshView();
    private final MeshView majorView = new MeshView();

    public PoincareGrid3D() {
        setMouseTransparent(true);
        minorView.setMouseTransparent(true);
        majorView.setMouseTransparent(true);
        minorView.setDrawMode(DrawMode.FILL);
        majorView.setDrawMode(DrawMode.FILL);
        minorView.setCullFace(CullFace.NONE);
        majorView.setCullFace(CullFace.NONE);
        getChildren().addAll(minorView, majorView);

        ballRadius.addListener((o, oldValue, newValue) -> rebuild());
        ringStep.addListener((o, oldValue, newValue) -> rebuild());
        ringCount.addListener((o, oldValue, newValue) -> rebuild());
        spokeCount.addListener((o, oldValue, newValue) -> rebuild());
        majorEvery.addListener((o, oldValue, newValue) -> rebuild());
        circleSegments.addListener((o, oldValue, newValue) -> rebuild());
        lineThickness.addListener((o, oldValue, newValue) -> rebuild());
        minorLineColor.addListener((o, oldValue, newValue) -> updateMaterials());
        majorLineColor.addListener((o, oldValue, newValue) -> updateMaterials());
        updateMaterials();
        rebuild();
    }

    private void updateMaterials() {
        minorView.setMaterial(ContextGraphGridMaterial.forColor(minorLineColor.get()));
        majorView.setMaterial(ContextGraphGridMaterial.forColor(majorLineColor.get()));
        minorView.setVisible(minorLineColor.get() != null && minorLineColor.get().getOpacity() > 0.0);
        majorView.setVisible(majorLineColor.get() != null && majorLineColor.get().getOpacity() > 0.0);
    }

    private void rebuild() {
        double radius = ballRadius.get();
        double step = ringStep.get();
        if (!(radius > 0.0) || !Double.isFinite(radius)
                || !(step > 0.0) || !Double.isFinite(step)) {
            // Property mutation can temporarily be invalid during editing.
            minorView.setMesh(new TriangleMesh());
            majorView.setMesh(new TriangleMesh());
            return;
        }
        int rings = Math.min(64, Math.max(0, ringCount.get()));
        int spokes = Math.min(128, Math.max(0, spokeCount.get()));
        int every = Math.max(1, majorEvery.get());
        int segments = Math.min(512, Math.max(24, circleSegments.get()));
        // Small constant width in world units; not the circle's radial spacing.
        double width = lineThickness.get() > 0.0 && Double.isFinite(lineThickness.get())
                ? lineThickness.get() : Math.max(0.0001, radius * 0.0010);
        width = Math.min(width, radius * 0.05);

        MeshBuilder minor = new MeshBuilder();
        MeshBuilder major = new MeshBuilder();
        double finalRadius = rings == 0 ? radius * 0.98
                : Math.min(radius * 0.995, PoincareGridMath.ringRadius(radius, rings * step));

        for (int ring = 1; ring <= rings; ring++) {
            double r = PoincareGridMath.ringRadius(radius, ring * step);
            if (r >= radius * 0.995) break; // suppress precision collapse at boundary
            boolean isMajor = ring % every == 0;
            MeshBuilder target = isMajor ? major : minor;
            double t = isMajor ? width * 1.7 : width;
            target.addRing(r, t, segments, isMajor ? -radius * 0.0006 : 0.0);
        }

        for (int spoke = 0; spoke < spokes; spoke++) {
            double theta = 2.0 * Math.PI * spoke / spokes;
            // Four (or fewer) emphasized cardinal axes, other spokes subdued.
            boolean isMajor = (spoke * 4) % spokes == 0;
            double t = isMajor ? width * 1.7 : width;
            double x = finalRadius * Math.cos(theta);
            double z = finalRadius * Math.sin(theta);
            (isMajor ? major : minor).addSegment(0.0, 0.0, x, z, t,
                isMajor ? -radius * 0.0012 : -radius * 0.0003);
        }

        minorView.setMesh(minor.finish());
        majorView.setMesh(major.finish());
    }

    private static final class MeshBuilder {
        private final TriangleMesh mesh = new TriangleMesh();
        MeshBuilder() { mesh.getTexCoords().addAll(0f, 0f); }

        private int vertex(double x, double z, double y) {
            int index = mesh.getPoints().size() / 3;
            mesh.getPoints().addAll((float) x, (float) y, (float) z);
            return index;
        }

        private void quad(int a, int b, int c, int d) {
            mesh.getFaces().addAll(a, 0, b, 0, c, 0);
            mesh.getFaces().addAll(a, 0, c, 0, d, 0);
        }

        private void addRing(double radius, double thickness, int segments, double y) {
            double inner = Math.max(0.0, radius - thickness * 0.5);
            double outer = radius + thickness * 0.5;
            // Make a contiguous annular strip, not one Shape3D per segment.
            int[] inside = new int[segments + 1];
            int[] outside = new int[segments + 1];
            for (int s = 0; s <= segments; s++) {
                double angle = 2.0 * Math.PI * s / segments;
                double cos = Math.cos(angle), sin = Math.sin(angle);
                inside[s] = vertex(inner * cos, inner * sin, y);
                outside[s] = vertex(outer * cos, outer * sin, y);
            }
            for (int s = 0; s < segments; s++) {
                quad(inside[s], outside[s], outside[s + 1], inside[s + 1]);
            }
        }

        private void addSegment(double x0, double z0, double x1, double z1,
                double thickness, double y) {
            double dx = x1 - x0, dz = z1 - z0;
            double length = Math.hypot(dx, dz);
            if (length < 1.0e-12) return;
            double nx = -dz / length * thickness * 0.5;
            double nz = dx / length * thickness * 0.5;
            int a = vertex(x0 - nx, z0 - nz, y);
            int b = vertex(x0 + nx, z0 + nz, y);
            int c = vertex(x1 + nx, z1 + nz, y);
            int d = vertex(x1 - nx, z1 - nz, y);
            quad(a, b, c, d);
        }

        private TriangleMesh finish() { return mesh; }
    }

    public double getBallRadius() { return ballRadius.get(); }
    public void setBallRadius(double value) { ballRadius.set(value); }
    public DoubleProperty ballRadiusProperty() { return ballRadius; }

    public double getRingStep() { return ringStep.get(); }
    public void setRingStep(double value) { ringStep.set(value); }
    public DoubleProperty ringStepProperty() { return ringStep; }

    public int getRingCount() { return ringCount.get(); }
    public void setRingCount(int value) { ringCount.set(value); }
    public IntegerProperty ringCountProperty() { return ringCount; }

    public int getSpokeCount() { return spokeCount.get(); }
    public void setSpokeCount(int value) { spokeCount.set(value); }
    public IntegerProperty spokeCountProperty() { return spokeCount; }

    public int getMajorEvery() { return majorEvery.get(); }
    public void setMajorEvery(int value) { majorEvery.set(value); }
    public IntegerProperty majorEveryProperty() { return majorEvery; }

    public int getCircleSegments() { return circleSegments.get(); }
    public void setCircleSegments(int value) { circleSegments.set(value); }
    public IntegerProperty circleSegmentsProperty() { return circleSegments; }

    public double getLineThickness() { return lineThickness.get(); }
    public void setLineThickness(double value) { lineThickness.set(value); }
    public DoubleProperty lineThicknessProperty() { return lineThickness; }

    public Color getMinorLineColor() { return minorLineColor.get(); }
    public void setMinorLineColor(Color value) { minorLineColor.set(value); }
    public ObjectProperty<Color> minorLineColorProperty() { return minorLineColor; }

    public Color getMajorLineColor() { return majorLineColor.get(); }
    public void setMajorLineColor(Color value) { majorLineColor.set(value); }
    public ObjectProperty<Color> majorLineColorProperty() { return majorLineColor; }
}
