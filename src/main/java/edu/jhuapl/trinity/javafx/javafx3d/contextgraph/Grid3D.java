package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Grid3D: a lightweight, configurable Y=0 planar grid for JavaFX 3D SubScenes.
 *
 * Style.LINES draws thin “line” quads so empty space stays transparent.
 * Style.CHECKERBOARD fills alternating cells with low alpha + overlays the same line grid.
 *
 * Major lines are rendered as a separate mesh so they can be thicker + brighter.
 */
public class Grid3D extends Group {

    public enum Style { LINES, CHECKERBOARD }

    private final DoubleProperty width = new SimpleDoubleProperty(this, "width", 100);
    private final DoubleProperty length = new SimpleDoubleProperty(this, "length", 100);
    private final IntegerProperty xDivs = new SimpleIntegerProperty(this, "xDivs", 20); // columns
    private final IntegerProperty zDivs = new SimpleIntegerProperty(this, "zDivs", 20); // rows

    private final ObjectProperty<Color> lineColor =
            new SimpleObjectProperty<>(this, "lineColor", Color.gray(1.0, 0.35));

    private final DoubleProperty lineThickness =
            new SimpleDoubleProperty(this, "lineThickness", -1); // -1 => auto

    private final IntegerProperty majorEvery =
            new SimpleIntegerProperty(this, "majorEvery", 10);

    private final ObjectProperty<Color> majorLineColor =
            new SimpleObjectProperty<>(this, "majorLineColor", Color.gray(1.0, 0.6));

    private final ObjectProperty<Style> meshStyle =
            new SimpleObjectProperty<>(this, "style", Style.LINES);

    private final ObjectProperty<Color> checkA =
            new SimpleObjectProperty<>(this, "checkA", Color.color(1,1,1,0.10));

    private final ObjectProperty<Color> checkB =
            new SimpleObjectProperty<>(this, "checkB", Color.color(1,1,1,0.00)); // almost transparent

    // The currently installed root node for the grid visuals
    private Node gridNode;

    // References to line MeshViews (so we can update materials without rebuild)
    private MeshView minorLinesView;
    private MeshView majorLinesView;
    private MeshView checkerAView;
    private MeshView checkerBView;

    public Grid3D() {
        build();

        // Rebuild geometry when a key property changes
        width.addListener((obs, o, n) -> build());
        length.addListener((obs, o, n) -> build());
        xDivs.addListener((obs, o, n) -> build());
        zDivs.addListener((obs, o, n) -> build());
        lineThickness.addListener((obs, o, n) -> build());
        majorEvery.addListener((obs, o, n) -> build());
        meshStyle.addListener((obs, o, n) -> build());
        checkA.addListener((obs, o, n) -> updateCheckerMaterials());
        checkB.addListener((obs, o, n) -> updateCheckerMaterials());

        // Material-only updates (no need to rebuild)
        lineColor.addListener((obs, o, n) -> updateMaterial());
        majorLineColor.addListener((obs, o, n) -> updateMaterial());

        setMouseTransparent(true);
    }

    public Grid3D(double width, double length, int xDivs, int zDivs) {
        this();
        setWidth(width);
        setLength(length);
        setXDivs(xDivs);
        setZDivs(zDivs);
    }

    // --- Properties
    public final double getWidth() { return width.get(); }
    public final void setWidth(double v) { width.set(v); }
    public final DoubleProperty widthProperty() { return width; }

    public final double getLength() { return length.get(); }
    public final void setLength(double v) { length.set(v); }
    public final DoubleProperty lengthProperty() { return length; }

    public final int getXDivs() { return xDivs.get(); }
    public final void setXDivs(int v) { xDivs.set(Math.max(1, v)); }
    public final IntegerProperty xDivsProperty() { return xDivs; }

    public final int getZDivs() { return zDivs.get(); }
    public final void setZDivs(int v) { zDivs.set(Math.max(1, v)); }
    public final IntegerProperty zDivsProperty() { return zDivs; }

    public final Color getLineColor() { return lineColor.get(); }
    public final void setLineColor(Color c) { lineColor.set(c); }
    public final ObjectProperty<Color> lineColorProperty() { return lineColor; }

    public final double getLineThickness() { return lineThickness.get(); }
    public final void setLineThickness(double v) { lineThickness.set(v); } // world units; -1 = auto
    public final DoubleProperty lineThicknessProperty() { return lineThickness; }

    public final int getMajorEvery() { return majorEvery.get(); }
    public final void setMajorEvery(int v) { majorEvery.set(Math.max(1, v)); }
    public final IntegerProperty majorEveryProperty() { return majorEvery; }

    public final Color getMajorLineColor() { return majorLineColor.get(); }
    public final void setMajorLineColor(Color c) { majorLineColor.set(c); }
    public final ObjectProperty<Color> majorLineColorProperty() { return majorLineColor; }

    public final Style getMeshStyle() { return meshStyle.get(); }
    public final void setMeshStyle(Style s) { meshStyle.set(s); }
    public final ObjectProperty<Style> meshStyleProperty() { return meshStyle; }

    /** Backwards-compat alias (note the capital M); prefer meshStyleProperty(). */
    @Deprecated
    public final ObjectProperty<Style> MeshStyleProperty() { return meshStyle; }

    public final Color getCheckA() { return checkA.get(); }
    public final void setCheckA(Color c) { checkA.set(c); }
    public final ObjectProperty<Color> checkAProperty() { return checkA; }

    public final Color getCheckB() { return checkB.get(); }
    public final void setCheckB(Color c) { checkB.set(c); }
    public final ObjectProperty<Color> checkBProperty() { return checkB; }

    // -------------------------------------------------------------------------
    // Build / update
    // -------------------------------------------------------------------------

    private void build() {
        // Clear refs (will be repopulated by buildLinesGroup())
        minorLinesView = null;
        majorLinesView = null;
        checkerAView = null;
        checkerBView = null;

        Node newNode = switch (getMeshStyle()) {
            case LINES -> buildLinesGroup();           // Group(minor, major)
            case CHECKERBOARD -> buildCheckerNode();   // Group(checkA, checkB, linesGroup)
        };

        newNode.setMouseTransparent(true);

        if (gridNode == null) {
            gridNode = newNode;
            getChildren().setAll(gridNode);
        } else {
            int idx = getChildren().indexOf(gridNode);
            gridNode = newNode;
            if (idx >= 0) getChildren().set(idx, gridNode);
            else getChildren().setAll(gridNode);
        }

        // Ensure current colors applied (esp. after rebuild)
        updateMaterial();
        updateCheckerMaterials();
    }

    private void updateCheckerMaterials() {
        updateCheckerMaterial(checkerAView, getCheckA());
        updateCheckerMaterial(checkerBView, getCheckB());
    }

    /** Apply the full RGBA color through a transparent self-illumination texture. */
    private static void updateCheckerMaterial(MeshView view, Color color) {
        if (view == null) return;
        Color value = color == null ? Color.TRANSPARENT : color;
        view.setMaterial(ContextGraphGridMaterial.forColor(value));
        view.setVisible(value.getOpacity() > 0.0);
    }

    private void updateMaterial() {
        if (minorLinesView != null) {
            minorLinesView.setMaterial(materialForLine(false));
            minorLinesView.setVisible(getLineColor() != null && getLineColor().getOpacity() > 0.0);
        }
        if (majorLinesView != null) {
            majorLinesView.setMaterial(materialForLine(true));
            majorLinesView.setVisible(getMajorLineColor() != null && getMajorLineColor().getOpacity() > 0.0);
        }
    }

    // -------------------------------------------------------------------------
    // Lines rendering: build two meshes (minor + major) so major lines can be brighter
    // -------------------------------------------------------------------------

    private Group buildLinesGroup() {
        var pair = buildLinesMeshes();
        MeshView minor = new MeshView(pair.minorMesh);
        MeshView major = new MeshView(pair.majorMesh);

        configureLineMeshView(minor);
        configureLineMeshView(major);

        // Materials
        minor.setMaterial(materialForLine(false));
        major.setMaterial(materialForLine(true));

        // Keep refs for later color updates
        this.minorLinesView = minor;
        this.majorLinesView = major;

        Group g = new Group(minor, major);
        g.setMouseTransparent(true);
        return g;
    }

    private void configureLineMeshView(MeshView mv) {
        mv.setCullFace(CullFace.NONE);  // visible from both sides
        mv.setDrawMode(DrawMode.FILL);  // we are drawing quads as triangles (not wireframe)
        mv.setMouseTransparent(true);
    }

    private static final class MeshPair {
        final TriangleMesh minorMesh;
        final TriangleMesh majorMesh;
        MeshPair(TriangleMesh minorMesh, TriangleMesh majorMesh) {
            this.minorMesh = minorMesh;
            this.majorMesh = majorMesh;
        }
    }

    private MeshPair buildLinesMeshes() {
        double w = getWidth();
        double l = getLength();
        int nx = getXDivs();
        int nz = getZDivs();

        double x0 = -w / 2.0;
        double z0 = -l / 2.0;
        double dx = w / nx;
        double dz = l / nz;

        // Auto thickness ~ 2% of smaller cell size (clamped)
        double baseT = (getLineThickness() > 0)
                ? getLineThickness()
                : Math.max(0.0001, 0.02 * Math.min(dx, dz));

        double majorT = Math.max(baseT * 1.8, baseT + 0.0001);

        TriangleMesh minor = new TriangleMesh();
        TriangleMesh major = new TriangleMesh();
        minor.getTexCoords().addAll(0, 0);
        major.getTexCoords().addAll(0, 0);

        class Adder {
            int p(TriangleMesh m, float x, float y, float z) {
                m.getPoints().addAll(x, y, z);
                return (m.getPoints().size() / 3) - 1;
            }
            void f(TriangleMesh m, int p0, int p1, int p2, int p3) {
                m.getFaces().addAll(p0, 0, p1, 0, p2, 0);
                m.getFaces().addAll(p0, 0, p2, 0, p3, 0);
            }
            void addQuad(TriangleMesh m, float x1, float z1, float x2, float z2, float thickness) {
                // Small Y offsets prevent z-fighting at crossings and over checker cells.
                float lineY = (float) (m == major ? -0.16 * baseT : -0.08 * baseT);
                if (x1 == x2) {
                    // line along Z (constant X)
                    float t = thickness / 2.0f;
                    float xL = x1 - t, xR = x1 + t;
                    float zA = z1, zB = z2;
                    int p0 = p(m, xL, lineY, zA);
                    int p1 = p(m, xR, lineY, zA);
                    int p2 = p(m, xR, lineY, zB);
                    int p3 = p(m, xL, lineY, zB);
                    f(m, p0, p1, p2, p3);
                } else {
                    // line along X (constant Z)
                    float t = thickness / 2.0f;
                    float zL = z1 - t, zR = z1 + t;
                    float xA = x1, xB = x2;
                    int p0 = p(m, xA, lineY, zL);
                    int p1 = p(m, xB, lineY, zL);
                    int p2 = p(m, xB, lineY, zR);
                    int p3 = p(m, xA, lineY, zR);
                    f(m, p0, p1, p2, p3);
                }
            }
        }
        Adder adder = new Adder();

        int me = Math.max(1, getMajorEvery());

        // Lines parallel to X (vary X, constant Z)
        for (int r = 0; r <= nz; r++) {
            double z = z0 + r * dz;
            boolean isMajor = (r % me == 0);
            TriangleMesh target = isMajor ? major : minor;
            float t = (float) (isMajor ? majorT : baseT);
            adder.addQuad(target, (float) x0, (float) z, (float) (x0 + w), (float) z, t);
        }

        // Lines parallel to Z (vary Z, constant X)
        for (int c = 0; c <= nx; c++) {
            double x = x0 + c * dx;
            boolean isMajor = (c % me == 0);
            TriangleMesh target = isMajor ? major : minor;
            float t = (float) (isMajor ? majorT : baseT);
            adder.addQuad(target, (float) x, (float) z0, (float) x, (float) (z0 + l), t);
        }

        return new MeshPair(minor, major);
    }

    // -------------------------------------------------------------------------
    // Checkerboard: two fill meshes + the same line group overlay
    // -------------------------------------------------------------------------

    private Group buildCheckerNode() {
        double w = getWidth();
        double l = getLength();
        int nx = getXDivs();
        int nz = getZDivs();

        double x0 = -w / 2.0;
        double z0 = -l / 2.0;
        double dx = w / nx;
        double dz = l / nz;

        TriangleMesh meshA = new TriangleMesh();
        TriangleMesh meshB = new TriangleMesh();
        meshA.getTexCoords().addAll(0, 0);
        meshB.getTexCoords().addAll(0, 0);

        class Adder {
            int p(TriangleMesh m, float x, float y, float z) {
                m.getPoints().addAll(x, y, z);
                return (m.getPoints().size() / 3) - 1;
            }
            void f(TriangleMesh m, int p0, int p1, int p2, int p3) {
                m.getFaces().addAll(p0, 0, p1, 0, p2, 0);
                m.getFaces().addAll(p0, 0, p2, 0, p3, 0);
            }
            void cell(TriangleMesh m, float x, float z, float dx, float dz) {
                int p0 = p(m, x, 0f, z);
                int p1 = p(m, x + dx, 0f, z);
                int p2 = p(m, x + dx, 0f, z + dz);
                int p3 = p(m, x, 0f, z + dz);
                f(m, p0, p1, p2, p3);
            }
        }
        Adder adder = new Adder();

        for (int r = 0; r < nz; r++) {
            for (int c = 0; c < nx; c++) {
                boolean a = ((r + c) % 2 == 0);
                float x = (float) (x0 + c * dx);
                float z = (float) (z0 + r * dz);
                if (a) adder.cell(meshA, x, z, (float) dx, (float) dz);
                else   adder.cell(meshB, x, z, (float) dx, (float) dz);
            }
        }

        MeshView mvA = new MeshView(meshA);
        MeshView mvB = new MeshView(meshB);

        mvA.setCullFace(CullFace.NONE);
        mvB.setCullFace(CullFace.NONE);
        mvA.setDrawMode(DrawMode.FILL);
        mvB.setDrawMode(DrawMode.FILL);

        checkerAView = mvA;
        checkerBView = mvB;
        updateCheckerMaterials();

        // Lines overlay: same two-mesh line system (and keeps refs for material updates)
        Group linesGroup = buildLinesGroup();

        Group g = new Group(mvA, mvB, linesGroup);
        g.setMouseTransparent(true);
        return g;
    }

    // -------------------------------------------------------------------------
    // Materials
    // -------------------------------------------------------------------------

    private PhongMaterial materialForLine(boolean major) {
        Color c = major ? getMajorLineColor() : getLineColor();
        if (c == null) c = Color.color(1, 1, 1, 1.0);

        return ContextGraphGridMaterial.forColor(c);
    }

    // --- Convenience for adding to an existing 3D root
    public Node asNode() { return this; }
}