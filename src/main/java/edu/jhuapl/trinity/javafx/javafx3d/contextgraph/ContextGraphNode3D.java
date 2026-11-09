package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.javafx.javafx3d.animated.Selectable3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

/** Independent graph node with shared, immutable unit-sphere geometry. */
public final class ContextGraphNode3D extends MeshView implements Selectable3D {
    private static final Map<Integer, TriangleMesh> SHARED_MESHES = new HashMap<>();
    private final Scale dimensions = new Scale();
    private final ContextGraphNodeInteraction interaction;
    private double radius;

    public ContextGraphNode3D(PhongMaterial material, double radius, int divisions, boolean animateOnHover) {
        int resolution = Math.max(4, divisions);
        synchronized (SHARED_MESHES) {
            setMesh(SHARED_MESHES.computeIfAbsent(resolution, ContextGraphNode3D::createUnitSphere));
        }
        setCullFace(CullFace.BACK);
        getTransforms().add(dimensions);
        setSphereRadius(radius);
        interaction = new ContextGraphNodeInteraction(this, material, DEFAULT_SELECTED_MATERIAL, animateOnHover);
    }

    public void setSphereRadius(double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("Invalid node radius");
        radius = value;
        dimensions.setX(value);
        dimensions.setY(value);
        dimensions.setZ(value);
    }

    public double getSphereRadius() { return radius; }
    public double getRadius() { return radius; }
    public void setRadius(double value) { setSphereRadius(value); }
    public void setPhongMaterial(PhongMaterial material) { interaction.setNormalMaterial(material); }
    public void setSelectedMaterial(PhongMaterial material) { interaction.setSelectedMaterial(material); }
    public void setAnimateOnHover(boolean enabled) { interaction.setHoverEnabled(enabled); }
    public boolean isAnimateOnHover() { return interaction.isHoverEnabled(); }
    public void setExpansionScale(double value) { interaction.setExpansionScale(value); }
    public double getExpansionScale() { return interaction.getExpansionScale(); }
    public void setAnimationTimeMS(double value) { interaction.setAnimationTimeMS(value); }
    public double getAnimationTimeMS() { return interaction.getAnimationTimeMS(); }
    @Override public void select() { interaction.select(); }
    @Override public void unselect() { interaction.unselect(); }
    @Override public boolean isSelected() { return interaction.isSelected(); }

    private static TriangleMesh createUnitSphere(int divisions) {
        int longitude = divisions;
        int latitude = Math.max(3, divisions / 2);
        int ringCount = latitude - 1;
        int vertexCount = ringCount * longitude + 2;
        float[] points = new float[vertexCount * 3];
        float[] texCoords = new float[vertexCount * 2];
        for (int ring = 1; ring < latitude; ring++) {
            double theta = Math.PI * ring / latitude;
            double y = Math.cos(theta);
            double r = Math.sin(theta);
            for (int lon = 0; lon < longitude; lon++) {
                double phi = 2 * Math.PI * lon / longitude;
                int index = (ring - 1) * longitude + lon;
                points[index * 3] = (float) (r * Math.cos(phi));
                points[index * 3 + 1] = (float) y;
                points[index * 3 + 2] = (float) (r * Math.sin(phi));
                texCoords[index * 2] = (float) lon / longitude;
                texCoords[index * 2 + 1] = (float) ring / latitude;
            }
        }
        int north = ringCount * longitude;
        int south = north + 1;
        points[north * 3 + 1] = 1;
        points[south * 3 + 1] = -1;
        texCoords[north * 2] = 0.5f;
        texCoords[south * 2] = 0.5f;
        texCoords[south * 2 + 1] = 1;
        int triangleCount = 2 * longitude * (latitude - 1);
        int[] faces = new int[triangleCount * 6];
        int cursor = 0;
        for (int lon = 0; lon < longitude; lon++) {
            int next = (lon + 1) % longitude;
            cursor = triangle(faces, cursor, north, next, lon);
            for (int ring = 0; ring < ringCount - 1; ring++) {
                int a = ring * longitude + lon;
                int b = ring * longitude + next;
                int c = (ring + 1) * longitude + lon;
                int d = (ring + 1) * longitude + next;
                cursor = triangle(faces, cursor, a, b, c);
                cursor = triangle(faces, cursor, b, d, c);
            }
            int last = (ringCount - 1) * longitude;
            cursor = triangle(faces, cursor, last + lon, last + next, south);
        }
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    private static int triangle(int[] faces, int cursor, int a, int b, int c) {
        faces[cursor++] = a; faces[cursor++] = a;
        faces[cursor++] = b; faces[cursor++] = b;
        faces[cursor++] = c; faces[cursor++] = c;
        return cursor;
    }
}
