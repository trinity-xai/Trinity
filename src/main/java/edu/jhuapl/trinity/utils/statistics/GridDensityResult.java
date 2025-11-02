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
 * @param pdfZ     size binsY x binsX (row-major: y, then x)
 * @param cdfZ     same shape, cumulative in +x and +y
 * @param xEdges   length binsX+1
 * @param yEdges   length binsY+1
 * @param xCenters length binsX
 * @param yCenters length binsY
 * @author Sean Phillips
 */
public record GridDensityResult(double[][] pdfZ, double[][] cdfZ, double[] xEdges, double[] yEdges, double[] xCenters, double[] yCenters, double dx,
                                double dy) {

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
