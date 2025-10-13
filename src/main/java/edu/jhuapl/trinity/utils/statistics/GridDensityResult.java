package edu.jhuapl.trinity.utils.statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Result container for 2D joint PDF/CDF surfaces.
 * <p>
 * Holds:
 * - PDF grid (normalized density)
 * - CDF grid (cumulative probability)
 * - Axis edges and centers
 * - Bin widths
 * </p>
 * Designed for use in Trinity’s 3D Hypersurface renderer.
 *
 * @author Sean Phillips
 */
public final class GridDensityResult {
    private final double[][] pdfZ;     // size binsY x binsX (row-major: y, then x)
    private final double[][] cdfZ;     // same shape, cumulative in +x and +y
    private final double[] xEdges;     // length binsX+1
    private final double[] yEdges;     // length binsY+1
    private final double[] xCenters;   // length binsX
    private final double[] yCenters;   // length binsY
    private final double dx;
    private final double dy;

    public GridDensityResult(double[][] pdfZ,
                             double[][] cdfZ,
                             double[] xEdges,
                             double[] yEdges,
                             double[] xCenters,
                             double[] yCenters,
                             double dx,
                             double dy) {
        this.pdfZ = pdfZ;
        this.cdfZ = cdfZ;
        this.xEdges = xEdges;
        this.yEdges = yEdges;
        this.xCenters = xCenters;
        this.yCenters = yCenters;
        this.dx = dx;
        this.dy = dy;
    }

    public double[][] getPdfZ() {
        return pdfZ;
    }

    public double[][] getCdfZ() {
        return cdfZ;
    }

    public double[] getxEdges() {
        return xEdges;
    }

    public double[] getyEdges() {
        return yEdges;
    }

    public double[] getxCenters() {
        return xCenters;
    }

    public double[] getyCenters() {
        return yCenters;
    }

    public double getDx() {
        return dx;
    }

    public double getDy() {
        return dy;
    }

    /**
     * Convert PDF grid to List<List<Double>> (row-major).
     */
    public List<List<Double>> pdfAsListGrid() {
        List<List<Double>> grid = new ArrayList<>(pdfZ.length);
        for (double[] row : pdfZ) {
            List<Double> r = new ArrayList<>(row.length);
            for (double v : row) r.add(v);
            grid.add(r);
        }
        return grid;
    }

    /**
     * Convert CDF grid to List<List<Double>> (row-major).
     */
    public List<List<Double>> cdfAsListGrid() {
        List<List<Double>> grid = new ArrayList<>(cdfZ.length);
        for (double[] row : cdfZ) {
            List<Double> r = new ArrayList<>(row.length);
            for (double v : row) r.add(v);
            grid.add(r);
        }
        return grid;
    }
}
