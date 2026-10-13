package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

/**
 * Persistent JavaFX render state for one tiled hypersurface region.
 *
 * <p>Each LOD receives its own {@link HyperSurfacePlotMesh}. Once created, that
 * MeshView remains permanently associated with its TriangleMesh; switching LOD
 * therefore becomes a visibility change rather than a MeshView.setMesh(...)
 * operation.</p>
 */
public final class TileRenderState {

    final TiledLodManager.TileSpec spec;
    final int startX0;
    final int endX0;
    final int startZ0;
    final int endZ0;
    private final HyperSurfacePlotMesh[] lodViews;

    private int activeLod = -1;
    private boolean tileVisible;

    TileRenderState(TiledLodManager.TileSpec spec,
                    int startX0, int endX0,
                    int startZ0, int endZ0,
                    int lodCount) {
        this.spec = spec;
        this.startX0 = startX0;
        this.endX0 = endX0;
        this.startZ0 = startZ0;
        this.endZ0 = endZ0;
        this.lodViews = new HyperSurfacePlotMesh[Math.max(1, lodCount)];
    }

    public int getTileId() {
        return spec.id;
    }

    public int getActiveLod() {
        return activeLod;
    }

    void setActiveLod(int activeLod) {
        this.activeLod = activeLod;
    }

    public boolean isTileVisible() {
        return tileVisible;
    }

    void setTileVisible(boolean tileVisible) {
        this.tileVisible = tileVisible;
    }

    public HyperSurfacePlotMesh getLodView(int lod) {
        return lod >= 0 && lod < lodViews.length ? lodViews[lod] : null;
    }

    void setLodView(int lod, HyperSurfacePlotMesh view) {
        if (lod < 0 || lod >= lodViews.length) {
            throw new IndexOutOfBoundsException("LOD index out of range: " + lod);
        }
        lodViews[lod] = view;
    }

    public int getLodCount() {
        return lodViews.length;
    }

    void clearViews() {
        for (int i = 0; i < lodViews.length; i++) {
            lodViews[i] = null;
        }
        activeLod = -1;
    }
}
