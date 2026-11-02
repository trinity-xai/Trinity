package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.geometry.Point3D;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

/**
 * Diagnostic implementation using JavaFX's built-in Cylinder instead of TubeMesh.
 * The public API remains compatible with ContextGraphController.
 * Geometry is sized with Cylinder radius/height properties, never Scale transforms.
 */
public final class ContextGraphEdge3D extends Cylinder {
    private final Rotate orientation = new Rotate();

    public ContextGraphEdge3D(float radius, int radialDivisions, int lengthDivisions) {
        super(radius, 1.0, Math.max(3, radialDivisions));
        if (radius <= 0f) throw new IllegalArgumentException("radius must be positive");
        // Cylinder has no independent axial subdivisions; lengthDivisions is
        // accepted solely to preserve the existing controller API.
        setCullFace(CullFace.BACK);
        getTransforms().add(orientation);
        setMouseTransparent(true);
    }

    public void setRadius(float radius) {
        if (radius <= 0f) throw new IllegalArgumentException("radius must be positive");
        super.setRadius(radius);
    }

    public void setEndpoints(Point3D start, Point3D end) {
        Point3D delta = end.subtract(start);
        double length = delta.magnitude();
        if (!Double.isFinite(length) || length < 1.0e-6) {
            setVisible(false);
            return;
        }
        setVisible(true);
        setHeight(length);
        Point3D midpoint = start.midpoint(end);
        setTranslateX(midpoint.getX());
        setTranslateY(midpoint.getY());
        setTranslateZ(midpoint.getZ());

        Point3D axis = Rotate.Y_AXIS.crossProduct(delta);
        if (axis.magnitude() < 1.0e-9) {
            orientation.setAxis(Rotate.X_AXIS);
            orientation.setAngle(delta.getY() < 0 ? 180 : 0);
        } else {
            orientation.setAxis(axis);
            orientation.setAngle(Math.toDegrees(Math.acos(
                Math.max(-1.0, Math.min(1.0, delta.getY() / length)))));
        }
    }
}
