package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.scene.shape.TriangleMesh;

/**
 * TubeMesh
 *
 * Open cylinder (tube without caps) TriangleMesh oriented along +Y.
 * Base at y=0, top at y=height.
 *
 * Features:
 *   - Seam-aware UVs with duplicate seam column.
 *   - UV tiling updates via setTiling(...).
 *   - Per-frame UV scrolling via setVOffset / setUOffset using a single
 *     setAll(...) update to avoid per-element flicker.
 *   - Optional inner wall: a second set of faces wound in the opposite
 *     direction sharing the same point positions. The inner wall mirrors
 *     the U coordinate (tilesAround - u) so the texture reads correctly
 *     when viewed from inside.
 *   - WindingOrder control for the outer wall. The inner wall always uses
 *     the opposite winding to the outer wall.
 *
 * Winding:
 *   Default CCW for the outer wall when viewed from outside.
 *   With innerWall=true, the inner wall is automatically wound CW
 *   (opposite the outer wall) so it faces inward.
 */
public class TubeMesh extends TriangleMesh {

    private int radialDivisions;
    private int lengthDivisions;

    private int cols;   // radialDivisions + 1 (duplicate seam column)
    private int rows;   // lengthDivisions + 1

    private float radius;
    private float height;

    private float tilesAround = 1f;
    private float tilesAlong  = 1f;

    private boolean innerWall = false;
    private WindingOrder windingOrder = WindingOrder.CCW;

    // Base UV arrays — outer and inner walls have separate arrays because
    // inner wall mirrors U. Both exclude scroll offsets.
    private float[] baseTexCoordsOuter;
    private float[] baseTexCoordsInner;

    // Working UV array (outer + inner concatenated) used for setAll()
    private float[] workTexCoords;

    private float uOffset = 0f;
    private float vOffset = 0f;

    // --- Constructors --------------------------------------------------------

    public TubeMesh(float radius, float height, int radialDivisions, int lengthDivisions) {
        this(radius, height, radialDivisions, lengthDivisions, 1f, 1f, false, WindingOrder.CCW);
    }

    public TubeMesh(float radius, float height, int radialDivisions, int lengthDivisions,
                    WindingOrder windingOrder) {
        this(radius, height, radialDivisions, lengthDivisions, 1f, 1f, false, windingOrder);
    }

    public TubeMesh(float radius, float height, int radialDivisions, int lengthDivisions,
                    float tilesAround, float tilesAlong) {
        this(radius, height, radialDivisions, lengthDivisions, tilesAround, tilesAlong, false, WindingOrder.CCW);
    }

    public TubeMesh(float radius, float height, int radialDivisions, int lengthDivisions,
                    float tilesAround, float tilesAlong, WindingOrder windingOrder) {
        this(radius, height, radialDivisions, lengthDivisions, tilesAround, tilesAlong, false, windingOrder);
    }

    public TubeMesh(float radius, float height, int radialDivisions, int lengthDivisions,
                    float tilesAround, float tilesAlong, boolean innerWall) {
        this(radius, height, radialDivisions, lengthDivisions, tilesAround, tilesAlong, innerWall, WindingOrder.CCW);
    }

    public TubeMesh(float radius, float height, int radialDivisions, int lengthDivisions,
                    float tilesAround, float tilesAlong, boolean innerWall, WindingOrder windingOrder) {
        super();
        if (radius <= 0f) throw new IllegalArgumentException("radius must be > 0");
        if (height <= 0f) throw new IllegalArgumentException("height must be > 0");

        this.radius           = radius;
        this.height           = height;
        this.radialDivisions  = Math.max(3, radialDivisions);
        this.lengthDivisions  = Math.max(1, lengthDivisions);
        this.tilesAround      = (tilesAround > 0f) ? tilesAround : 1f;
        this.tilesAlong       = (tilesAlong  > 0f) ? tilesAlong  : 1f;
        this.innerWall        = innerWall;
        this.windingOrder     = windingOrder;

        rebuildAll();
    }

    /** Updates tube dimensions in the existing point buffer without changing topology. */
    public void setDimensions(float newRadius, float newHeight) {
        if (newRadius <= 0 || newHeight <= 0) throw new IllegalArgumentException("positive dimensions required");
        if (radius == newRadius && height == newHeight) return;
        radius = newRadius;
        height = newHeight;
        buildPoints();
    }

    // --- Public API ----------------------------------------------------------

    public int getRadialDivisions()  { return radialDivisions; }
    public int getLengthDivisions()  { return lengthDivisions; }
    public float getRadius()         { return radius; }
    public float getHeight()         { return height; }
    public float getTilesAround()    { return tilesAround; }
    public float getTilesAlong()     { return tilesAlong; }
    public boolean isInnerWall()     { return innerWall; }

    public WindingOrder getWindingOrder()                  { return windingOrder; }
    public void setWindingOrder(WindingOrder windingOrder) { this.windingOrder = windingOrder; }

    public void setInnerWall(boolean innerWall)            { this.innerWall = innerWall; }

    public float getUOffset() { return uOffset; }
    public float getVOffset() { return vOffset; }

    /** Per-frame friendly: apply a U offset via a single setAll(). */
    public void setUOffset(float uOffset) {
        this.uOffset = uOffset;
        applyOffsetsToLiveTexCoords();
    }

    /** Per-frame friendly: apply a V offset via a single setAll(). */
    public void setVOffset(float vOffset) {
        this.vOffset = vOffset;
        applyOffsetsToLiveTexCoords();
    }

    /** Convenience: set both offsets at once (single setAll()). */
    public void setUVOffset(float uOffset, float vOffset) {
        this.uOffset = uOffset;
        this.vOffset = vOffset;
        applyOffsetsToLiveTexCoords();
    }

    /**
     * Occasional update: change UV tiling and rebuild texcoords only.
     * Not suitable for per-frame use.
     */
    public void setTiling(float tilesAround, float tilesAlong) {
        if (tilesAround <= 0f) tilesAround = 1f;
        if (tilesAlong  <= 0f) tilesAlong  = 1f;
        if (this.tilesAround == tilesAround && this.tilesAlong == tilesAlong) return;
        this.tilesAround = tilesAround;
        this.tilesAlong  = tilesAlong;
        buildTexCoords();
        applyOffsetsToLiveTexCoords();
    }

    /**
     * Full rebuild: update geometry parameters and reconstruct all buffers.
     * Do not call every frame.
     */
    public void rebuildGeometry(float radius, float height, int radialDivisions, int lengthDivisions) {
        if (radius <= 0f) throw new IllegalArgumentException("radius must be > 0");
        if (height <= 0f) throw new IllegalArgumentException("height must be > 0");

        boolean changed = this.radius != radius
                || this.height != height
                || this.radialDivisions != Math.max(3, radialDivisions)
                || this.lengthDivisions != Math.max(1, lengthDivisions);

        if (!changed) return;

        this.radius          = radius;
        this.height          = height;
        this.radialDivisions = Math.max(3, radialDivisions);
        this.lengthDivisions = Math.max(1, lengthDivisions);

        rebuildAll();
    }

    /** Rebuild all buffers using current stored parameters. */
    public void rebuild() {
        rebuildAll();
    }

    // --- Build internals -----------------------------------------------------

    private void rebuildAll() {
        this.cols = radialDivisions + 1;
        this.rows = lengthDivisions + 1;

        buildPoints();
        buildTexCoords();
        buildFaces();
        buildSmoothingGroups();
        applyOffsetsToLiveTexCoords();
    }

    private void buildPoints() {
        // Shared by both outer and inner walls — same physical positions.
        // index = row * cols + col
        float[] points = new float[rows * cols * 3];
        int p = 0;

        for (int row = 0; row < rows; row++) {
            float y = ((float) row / lengthDivisions) * height;

            for (int col = 0; col < cols; col++) {
                double theta = 2.0 * Math.PI * ((double) col / radialDivisions);
                points[p++] = (float) (radius * Math.cos(theta));
                points[p++] = y;
                points[p++] = (float) (radius * Math.sin(theta));
            }
        }

        getPoints().setAll(points);
    }

    private void buildTexCoords() {
        // Outer wall texcoords: U increases with col (standard)
        // Inner wall texcoords: U mirrors — (tilesAround - u) so texture
        // reads correctly when viewed from inside.
        //
        // Both walls use the same V coordinate.
        // Layout: outer block first, then inner block (if innerWall).
        // Each block is rows * cols UV pairs.

        final int blockSize = rows * cols * 2;
        final int totalSize = innerWall ? blockSize * 2 : blockSize;

        baseTexCoordsOuter = new float[blockSize];
        int t = 0;

        for (int row = 0; row < rows; row++) {
            float v = ((float) row / lengthDivisions) * tilesAlong;
            for (int col = 0; col < cols; col++) {
                float u = ((float) col / radialDivisions) * tilesAround;
                baseTexCoordsOuter[t++] = u;
                baseTexCoordsOuter[t++] = v;
            }
        }

        if (innerWall) {
            baseTexCoordsInner = new float[blockSize];
            t = 0;
            for (int row = 0; row < rows; row++) {
                float v = ((float) row / lengthDivisions) * tilesAlong;
                for (int col = 0; col < cols; col++) {
                    // Mirror U so inner surface texture reads correctly
                    float u = tilesAround - ((float) col / radialDivisions) * tilesAround;
                    baseTexCoordsInner[t++] = u;
                    baseTexCoordsInner[t++] = v;
                }
            }
        } else {
            baseTexCoordsInner = null;
        }

        // Allocate/resize work buffer
        if (workTexCoords == null || workTexCoords.length != totalSize) {
            workTexCoords = new float[totalSize];
        }

        // Initialise live texcoords to base (offsets applied below)
        System.arraycopy(baseTexCoordsOuter, 0, workTexCoords, 0, blockSize);
        if (innerWall) {
            System.arraycopy(baseTexCoordsInner, 0, workTexCoords, blockSize, blockSize);
        }
        getTexCoords().setAll(workTexCoords);
    }

    private void buildFaces() {
        final int quadsPerWall  = lengthDivisions * radialDivisions;
        final int trisPerWall   = quadsPerWall * 2;
        final int wallCount     = innerWall ? 2 : 1;
        final int[] faces       = new int[trisPerWall * wallCount * 6];

        int f = 0;

        // Outer wall
        final boolean invertOuter = windingOrder == WindingOrder.CW;
        f = buildWallFaces(faces, f, 0, invertOuter);

        // Inner wall — opposite winding to outer, UV offset by one block
        if (innerWall) {
            final int uvOffset    = rows * cols; // inner UVs start after outer block
            final boolean invertInner = !invertOuter;
            f = buildWallFaces(faces, f, uvOffset, invertInner);
        }

        getFaces().setAll(faces);
    }

    /**
     * Writes quad-pair face entries for one wall into the faces array.
     *
     * @param faces     target array
     * @param fi        write offset into faces
     * @param uvBase    UV index offset (0 for outer wall, rows*cols for inner)
     * @param invert    true to invert winding for this wall
     * @return updated fi
     */
    private int buildWallFaces(int[] faces, int fi, int uvBase, boolean invert) {
        for (int row = 0; row < lengthDivisions; row++) {
            for (int col = 0; col < radialDivisions; col++) {
                int p00 = row * cols + col;
                int p01 = row * cols + (col + 1);
                int p10 = (row + 1) * cols + col;
                int p11 = (row + 1) * cols + (col + 1);

                int t00 = uvBase + p00;
                int t01 = uvBase + p01;
                int t10 = uvBase + p10;
                int t11 = uvBase + p11;

                // Triangle 1: p00, p10, p01  (CCW from outside)
                faces[fi++] = p00; faces[fi++] = t00;
                faces[fi++] = invert ? p01 : p10; faces[fi++] = invert ? t01 : t10;
                faces[fi++] = invert ? p10 : p01; faces[fi++] = invert ? t10 : t01;

                // Triangle 2: p01, p10, p11  (CCW from outside)
                faces[fi++] = p01; faces[fi++] = t01;
                faces[fi++] = invert ? p11 : p10; faces[fi++] = invert ? t11 : t10;
                faces[fi++] = invert ? p10 : p11; faces[fi++] = invert ? t10 : t11;
            }
        }
        return fi;
    }

    private void buildSmoothingGroups() {
        final int trisPerWall = lengthDivisions * radialDivisions * 2;
        final int wallCount   = innerWall ? 2 : 1;
        final int faceCount   = trisPerWall * wallCount;

        int[] sm = new int[faceCount];
        // Both walls share smoothing group 1 — continuous smooth shading
        for (int i = 0; i < faceCount; i++) sm[i] = 1;
        getFaceSmoothingGroups().setAll(sm);
    }

    // --- Offset application --------------------------------------------------

    /**
     * Push baseTexCoords + scroll offsets into live texCoords via a single setAll().
     * Avoids per-element mutations that cause visual flicker.
     */
    private void applyOffsetsToLiveTexCoords() {
        if (baseTexCoordsOuter == null) return;

        final int blockSize = baseTexCoordsOuter.length;
        final int totalSize = innerWall ? blockSize * 2 : blockSize;

        if (workTexCoords == null || workTexCoords.length != totalSize) {
            workTexCoords = new float[totalSize];
        }

        final float uo = uOffset;
        final float vo = vOffset;

        for (int i = 0; i < blockSize; i += 2) {
            workTexCoords[i]     = baseTexCoordsOuter[i]     + uo;
            workTexCoords[i + 1] = baseTexCoordsOuter[i + 1] + vo;
        }

        if (innerWall && baseTexCoordsInner != null) {
            for (int i = 0; i < blockSize; i += 2) {
                // Mirror the U offset direction for the inner wall
                workTexCoords[blockSize + i]     = baseTexCoordsInner[i]     - uo;
                workTexCoords[blockSize + i + 1] = baseTexCoordsInner[i + 1] + vo;
            }
        }

        getTexCoords().setAll(workTexCoords);
    }
}