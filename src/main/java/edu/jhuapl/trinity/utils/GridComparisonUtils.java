package edu.jhuapl.trinity.utils;

/**
 *
 * @author Sean Phillips
 */
public class GridComparisonUtils {

    /**
     * Calculates the Euclidean distance between two 2D grids.
     * 
     * @param grid1 the first grid
     * @param grid2 the second grid
     * @return the Euclidean distance between the two grids
     */
    public static double euclideanDistance(double[][] grid1, double[][] grid2) {
        double sumOfSquares = 0.0;
        for (int i = 0; i < grid1.length; i++) {
            for (int j = 0; j < grid1[0].length; j++) {
                double diff = grid1[i][j] - grid2[i][j];
                sumOfSquares += diff * diff;
            }
        }
        return Math.sqrt(sumOfSquares);
    }

    /**
     * Calculates the root mean squared error (RMSE) between two 2D grids.
     * 
     * @param grid1 the first grid
     * @param grid2 the second grid
     * @return the RMSE between the two grids
     */
    public static double rootMeanSquaredError(double[][] grid1, double[][] grid2) {
        double sumOfSquares = 0.0;
        int count = 0;
        for (int i = 0; i < grid1.length; i++) {
            for (int j = 0; j < grid1[0].length; j++) {
                double diff = grid1[i][j] - grid2[i][j];
                sumOfSquares += diff * diff;
                count++;
            }
        }
        return Math.sqrt(sumOfSquares / count);
    }

    /**
     * Calculates the mean absolute difference (MAD) between two 2D grids.
     * 
     * @param grid1 the first grid
     * @param grid2 the second grid
     * @return the MAD between the two grids
     */
    public static double meanAbsoluteDifference(double[][] grid1, double[][] grid2) {
        double sumOfAbsDiff = 0.0;
        int count = 0;
        for (int i = 0; i < grid1.length; i++) {
            for (int j = 0; j < grid1[0].length; j++) {
                double diff = Math.abs(grid1[i][j] - grid2[i][j]);
                sumOfAbsDiff += diff;
                count++;
            }
        }
        return sumOfAbsDiff / count;
    }

    /**
     * Calculates the coefficient of variation (CV) for a single 2D grid.
     * 
     * @param grid the grid
     * @return the CV of the grid
     */
    public static double coefficientOfVariation(double[][] grid) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                sum += grid[i][j];
                count++;
            }
        }
        double mean = sum / count;
        double sumOfSquares = 0.0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                double diff = grid[i][j] - mean;
                sumOfSquares += diff * diff;
            }
        }
        double stdDev = Math.sqrt(sumOfSquares / count);
        return stdDev / mean;
    }

    /**
     * Calculates theCharsets structural similarity index (SSIM) between two 2D grids.
     * Note that this implementation uses a simplified version of the SSIM formula.
     * 
     * @param grid1 the first grid
     * @param grid2 the second grid
     * @return the SSIM between the two grids
     */
    public static double structuralSimilarityIndex(double[][] grid1, double[][] grid2) {
        double mean1 = mean(grid1);
        double mean2 = mean(grid2);
        double stdDev1 = stdDev(grid1, mean1);
        double stdDev2 = stdDev(grid2, mean2);
        double cov = covariance(grid1, grid2, mean1, mean2);
        double k1 = 0.01;
        double k2 = 0.03;
        double c1 = k1 * k1;
        double c2 = k2 * k2;
        double l = 2 * mean1 * mean2 / (mean1 * mean1 + mean2 * mean2);
        double s = (2 * stdDev1 * stdDev2 + c2) / (stdDev1 * stdDev1 + stdDev2 * stdDev2 + c2);
        double c = (cov + c1) / (stdDev1 * stdDev2 + c1);
        return l * s * c;
    }

    private static double mean(double[][] grid) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                sum += grid[i][j];
                count++;
            }
        }
        return sum / count;
    }

    private static double stdDev(double[][] grid, double mean) {
        double sumOfSquares = 0.0;
        int count = 0;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                double diff = grid[i][j] - mean;
                sumOfSquares += diff * diff;
                count++;
            }
        }
        return Math.sqrt(sumOfSquares / count);
    }

    private static double covariance(double[][] grid1, double[][] grid2, double mean1, double mean2) {
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < grid1.length; i++) {
            for (int j = 0; j < grid1[0].length; j++) {
                sum += (grid1[i][j] - mean1) * (grid2[i][j] - mean2);
                count++;
            }
        }
        return sum / count;
    }
}