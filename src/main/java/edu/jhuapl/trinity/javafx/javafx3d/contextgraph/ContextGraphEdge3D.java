package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.geometry.Point3D;
import javafx.scene.shape.CullFace;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

/** One MeshView per edge, with shared unit-cylinder geometry. */
public final class ContextGraphEdge3D extends MeshView {
    private static final Map<Integer, TriangleMesh> SHARED_MESHES = new HashMap<>();
    private final Rotate orientation = new Rotate();
    private final Scale dimensions = new Scale();
    private float radius;
    private PhongMaterial baseMaterial;
    private boolean selected;
    private boolean hovered;
    private static final PhongMaterial HOVER_MATERIAL = new PhongMaterial(Color.LIGHTSKYBLUE);
    private static final PhongMaterial SELECTED_MATERIAL = new PhongMaterial(Color.GOLD);

    public void setBaseMaterial(PhongMaterial material) {
        baseMaterial = material;
        refreshMaterial();
    }

    public void select() { selected = true; refreshMaterial(); }
    public void unselect() { selected = false; refreshMaterial(); }
    public boolean isSelected() { return selected; }

    private void refreshMaterial() {
        setMaterial(selected ? SELECTED_MATERIAL : hovered ? HOVER_MATERIAL : baseMaterial);
    }


    public ContextGraphEdge3D(float radius, int radialDivisions, int lengthDivisions) {
        if (!Float.isFinite(radius) || radius <= 0f) {
            throw new IllegalArgumentException("radius must be positive and finite");
        }
        this.radius = radius;
        // Construct the shared mesh once for each radial resolution. The caller
        // creates all edge views on its existing geometry-preparation worker.
        int divisions = Math.max(3, radialDivisions);
        synchronized (SHARED_MESHES) {
            setMesh(SHARED_MESHES.computeIfAbsent(divisions, ContextGraphEdge3D::createUnitCylinder));
        }
        setCullFace(CullFace.BACK);
        // Transform list is evaluated right-to-left: local dimensions first, then orientation.
        getTransforms().addAll(orientation, dimensions);
        dimensions.setX(radius);
        dimensions.setZ(radius);
        dimensions.setY(1.0);
        setMouseTransparent(false);
        setOnMouseEntered(e -> { hovered = true; refreshMaterial(); });
        setOnMouseExited(e -> { hovered = false; refreshMaterial(); });
        // lengthDivisions retained for API compatibility; no axial subdivisions.
    }

    public void setRadius(float radius) {
        if (!Float.isFinite(radius) || radius <= 0f) {
            throw new IllegalArgumentException("radius must be positive and finite");
        }
        this.radius = radius;
        dimensions.setX(radius);
        dimensions.setZ(radius);
    }

    public float getRadius() {
        return radius;
    }

    public void setEndpoints(Point3D start, Point3D end) {
        Point3D delta = end.subtract(start);
        double length = delta.magnitude();
        if (!Double.isFinite(length) || length < 1.0e-6) {
            setVisible(false);
            return;
        }
        setVisible(true);
        dimensions.setY(length);
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

    private static TriangleMesh createUnitCylinder(int divisions) {
        TriangleMesh mesh = new TriangleMesh();
        float[] points = new float[divisions * 2 * 3 + 6];
        for (int i = 0; i < divisions; i++) {
            double angle = 2.0 * Math.PI * i / divisions;
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);
            int p = i * 6;
            points[p] = x;
            points[p + 1] = -0.5f;
            points[p + 2] = z;
            points[p + 3] = x;
            points[p + 4] = 0.5f;
            points[p + 5] = z;
        }
        int bottomCenter = divisions * 2;
        int topCenter = bottomCenter + 1;
        points[bottomCenter * 3 + 1] = -0.5f;
        points[topCenter * 3 + 1] = 0.5f;
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(0f, 0f);

        int[] faces = new int[divisions * 4 * 6];
        int cursor = 0;
        for (int i = 0; i < divisions; i++) {
            int next = (i + 1) % divisions;
            int bi = i * 2;
            int ti = bi + 1;
            int bn = next * 2;
            int tn = bn + 1;
            // Winding points outward on the side, upward on the top,
            // and downward on the bottom (JavaFX front-face convention).
            int[][] triangles = {
                {bi, ti, bn}, {ti, tn, bn},
                {topCenter, tn, ti}, {bottomCenter, bi, bn}
            };
            for (int[] triangle : triangles) {
                for (int vertex : triangle) {
                    faces[cursor++] = vertex;
                    faces[cursor++] = 0;
                }
            }
        }
        mesh.getFaces().setAll(faces);
        return mesh;
    }
}
