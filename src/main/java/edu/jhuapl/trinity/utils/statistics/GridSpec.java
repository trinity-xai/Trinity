package edu.jhuapl.trinity.utils.statistics;

/**
 * Discretization spec for the 2D grid used by Density3DEngine.
 * Immutable, with sensible minimums.
 *
 * @author Sean Phillips
 */
public final class GridSpec {
    private int binsX;
    private int binsY;
    private Double minX;
    private Double maxX;
    private Double minY;
    private Double maxY;

    /**
     * Create a GridSpec with automatic bounds.
     *
     * @param binsX number of bins along X (>=5 enforced)
     * @param binsY number of bins along Y (>=5 enforced)
     */
    public GridSpec(int binsX, int binsY) {
        this(binsX, binsY, null, null, null, null);
    }

    /**
     * Create a GridSpec with optional explicit bounds (any null is auto-computed).
     *
     * @param binsX number of bins along X (>=5 enforced)
     * @param binsY number of bins along Y (>=5 enforced)
     * @param minX  explicit minimum X (nullable)
     * @param maxX  explicit maximum X (nullable)
     * @param minY  explicit minimum Y (nullable)
     * @param maxY  explicit maximum Y (nullable)
     */
    public GridSpec(int binsX, int binsY, Double minX, Double maxX, Double minY, Double maxY) {
        this.binsX = Math.max(5, binsX);
        this.binsY = Math.max(5, binsY);
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public int getBinsX() { return binsX; }
    public int getBinsY() { return binsY; }
    public Double getMinX() { return minX; }
    public Double getMaxX() { return maxX; }
    public Double getMinY() { return minY; }
    public Double getMaxY() { return maxY; }

    /**
     * @param binsX the binsX to set
     */
    public void setBinsX(int binsX) {
        this.binsX = binsX;
    }

    /**
     * @param binsY the binsY to set
     */
    public void setBinsY(int binsY) {
        this.binsY = binsY;
    }

    /**
     * @param minX the minX to set
     */
    public void setMinX(Double minX) {
        this.minX = minX;
    }

    /**
     * @param maxX the maxX to set
     */
    public void setMaxX(Double maxX) {
        this.maxX = maxX;
    }

    /**
     * @param minY the minY to set
     */
    public void setMinY(Double minY) {
        this.minY = minY;
    }

    /**
     * @param maxY the maxY to set
     */
    public void setMaxY(Double maxY) {
        this.maxY = maxY;
    }
}
