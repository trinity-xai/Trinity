/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.UmapConfig;
import edu.jhuapl.trinity.utils.umap.Umap;
import javafx.geometry.Point2D;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * @author Sean Phillips
 * @link https://stats.stackexchange.com/questions/2691/making-sense-of-principal-component-analysis-eigenvectors-eigenvalues
 */
public enum AnalysisUtils {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(AnalysisUtils.class);

    public static enum ANALYSIS_METHOD {PCA, SVD, KPCA}

    ;

    public static enum SOURCE {HYPERSPACE, HYPERSURFACE}

    ;

    public static enum KERNEL {Gaussian, Laplacian, Linear, Pearson, Polynomial}

    ;

    public static enum RANGE {MINIMUM, MAXIMUM}

    ;
    public static double EPISILON = 0.0000000001;

    public static double lerp1(double start, double end, double ratio) {
        return start * (1 - ratio) + end * ratio;
    }

    public static double lerp2(double s, double e, double t) {
        return s + (e - s) * t;
    }

    public static double blerp(double c11, double c21, double c12, double c22, double tx, double ty) {
        return lerp2(lerp2(c22, c21, tx), lerp2(c12, c22, tx), ty);
    }

    public static double cerp(double y1, double y2, double mu) {
        double mu2 = (1 - Math.cos(mu * Math.PI)) / 2;
        return (y1 * (1 - mu2) + y2 * mu2);
    }

    private List<Point2D> arcTerp(Point2D center, int count, double arcDegrees, double radius) {
        double fx = Math.cos(Math.toRadians(arcDegrees));
        double fy = Math.sin(Math.toRadians(arcDegrees));
        double lx = -Math.sin(Math.toRadians(arcDegrees));
        double ly = Math.cos(Math.toRadians(arcDegrees));
        List<Point2D> arcPoints = new ArrayList<>();
        for (int i = 0; i <= count; i++) {
            double sub_angle = (i / count) * Math.toRadians(arcDegrees);
            double xi = center.getX() + radius * (Math.sin(sub_angle) * fx + (1 - Math.cos(sub_angle)) * (-lx));
            double yi = center.getY() + radius * (Math.sin(sub_angle) * fy + (1 - Math.cos(sub_angle)) * (-ly));
            arcPoints.add(new Point2D(xi, yi));
        }
        return arcPoints;
    }

    /**
     * Interpolates between two end points.
     *
     * @param a The end point at <code>t = 0</code>.
     * @param b The end point at <code>t = 1</code>.
     * @param t The value at which to interpolate.
     * @return The value that is the fraction <code>t</code> of the way from
     * <code>a</code> to <code>b</code>: <code>(1-t)a + tb</code>.
     */
    public static double interpolate(double a, double b, double t) {
        return a + t * (b - a);
    }

    /**
     * Interpolates between two points on a line.
     *
     * @param x0 The x-coordinate of the first point.
     * @param y0 The y-coordinate of the first point.
     * @param x1 The x-coordinate of the second point.
     * @param y1 The y-coordinate of the second point.
     * @param x  The x-coordinate at which to interpolate.
     * @return The y-coordinate corresponding to <code>x</code>.
     */
    public static double interpolate(double x0, double y0, double x1,
                                     double y1, double x) {
        double t = (x - x0) / (x1 - x0);
        return interpolate(y0, y1, t);
    }

    /**
     * Performs a bilinear interpolation between four values.
     *
     * @param _00 The value at <code>(t, u) = (0, 0)</code>.
     * @param _10 The value at <code>(t, u) = (1, 0)</code>.
     * @param _01 The value at <code>(t, u) = (0, 1)</code>.
     * @param _11 The value at <code>(t, u) = (1, 1)</code>.
     * @param t   The first value at which to interpolate.
     * @param u   The second value at which to interpolate.
     * @return The interpolated value at <code>(t, u)</code>.
     */
    public static double bilinearInterpolate(double _00, double _10,
                                             double _01, double _11, double t, double u) {

        return interpolate(interpolate(_00, _10, t),
            interpolate(_01, _11, t), u);

    }

    public static double l2Norm(double[] x) {
        double result = 0.0;
        for (int i = 0; i < x.length; i++) {
            result += x[i] * x[i];
        }
        return Math.sqrt(result);
    }

    public static Double[][] boxDoubleArrays(double[][] arrays) {
        Double[][] inverse = Arrays.stream(arrays)
            .map(d -> Arrays.stream(d).boxed().toArray(Double[]::new))
            .toArray(Double[][]::new);
        return inverse;
    }

    // A function to randomly select k items from
    // stream[0..n-1].
    public static double[][] selectKItems(double stream[][], int n, int k) {
        int i; // index for elements in stream[]

        // reservoir[] is the output array. Initialize it
        // with first k elements from stream[]
        double reservoir[][] = new double[k][stream[0].length];
        for (i = 0; i < k; i++)
            reservoir[i] = stream[i];

        Random r = new Random();

        // Iterate from the (k+1)th element to nth element
        for (; i < n; i++) {
            // Pick a random index from 0 to i.
            int j = r.nextInt(i + 1);

            // If the randomly  picked index is smaller than
            // k, then replace the element present at the
            // index with new element from stream
            if (j < k)
                reservoir[j] = stream[i];
        }
        return reservoir;
    }

    public static double[][] featuresMultWeights(double[] features, double[][] weights) {
        RealMatrix realMatrix = MatrixUtils.createRealMatrix(weights);
        RealMatrix realMatrixColumn = MatrixUtils.createColumnRealMatrix(features);
        RealMatrix resultMatrix = realMatrix.multiply(realMatrixColumn);
        return resultMatrix.getData();
    }

    public static double[] getColumnMean(RealMatrix originalMatrix) {
        int dataWidth = originalMatrix.getColumnDimension();
        double[] columnMeanArray = new double[dataWidth];
        for (int columnIndex = 0; columnIndex < dataWidth; columnIndex++) {
            columnMeanArray[columnIndex] = Arrays.stream(
                originalMatrix.getColumn(columnIndex)).average().getAsDouble();
        }
        return columnMeanArray;
    }

    public static RealMatrix centerMatrixByColumnMean(RealMatrix originalMatrix) {
        int dataWidth = originalMatrix.getColumnDimension();
        double[] columnMeanArray = getColumnMean(originalMatrix);

        int dataHeight = originalMatrix.getRowDimension();
        double[][] centeredArray = new double[dataHeight][dataWidth];
        final double[][] originalData = originalMatrix.getData();
        for (int rowIndex = 0; rowIndex < dataHeight; rowIndex++) {
            int row = rowIndex;
            Arrays.parallelSetAll(centeredArray[row], (int value) ->
                originalData[row][value] - columnMeanArray[value]);
        }

        return MatrixUtils.createRealMatrix(centeredArray);
    }

    /**
     * @param array rows of data to be used for input into Covariance
     * @return the projected points
     * @author Sean Phillips
     * Principal Component Analysis using Apache Math Commons EigenDecomposition
     * Will mean center data by subtracting column means prior to
     * calculating covariance matrix
     * @link https://stackoverflow.com/questions/10604507/pca-implementation-in-java
     * @link https://blog.clairvoyantsoft.com/eigen-decomposition-and-pca-c50f4ca15501
     */
    public static double[][] doCommonsPCA(double[][] array) {
        LOG.info("centering matrix... ");
        long startTime = System.nanoTime();
        //create real matrix
        RealMatrix originalMatrix = MatrixUtils.createRealMatrix(array);
        //center columns by subtracting column means
        RealMatrix centeredMatrix = centerMatrixByColumnMean(originalMatrix);
        Utils.printTotalTime(startTime);

        LOG.info("Calculating covariance matrix... ");
        startTime = System.nanoTime();
        //Calculate covariance matrix of centered matrix
        Covariance covariance = new Covariance(centeredMatrix);
        RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
        Utils.printTotalTime(startTime);

        LOG.info("EigenDecomposition... ");
        startTime = System.nanoTime();
        EigenDecomposition ed = new EigenDecomposition(covarianceMatrix);
        Utils.printTotalTime(startTime);

        //Project the original matrix against the new axes defined by the eigenvectors
        int rowCount = originalMatrix.getRowDimension();
        int columnCount = originalMatrix.getColumnDimension();
        double[][] projectedVectors = new double[rowCount][columnCount];
        LOG.info("Projection... ");
        startTime = System.nanoTime();
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                projectedVectors[row][column] = originalMatrix.getRowVector(row)
                    .dotProduct(ed.getEigenvector(column));
            }
        }
        Utils.printTotalTime(startTime);

        return projectedVectors;
    }

    /**
     * @param array rows of data to be used for input into Covariance
     * @return the projected points
     * @author Sean Phillips
     * Singular Value Decomposition using Apache Math Commons
     * Will mean center data by subtracting column means prior to
     * calculating covariance matrix
     * @link https://stackoverflow.com/questions/10604507/pca-implementation-in-java
     * @link https://blog.clairvoyantsoft.com/eigen-decomposition-and-pca-c50f4ca15501
     */
    public static double[][] doCommonsSVD(double[][] array) {
        LOG.info("Starting SVD Process. Centering matrix... ");
        long startTime = System.nanoTime();
        //create real matrix of original inputs
        RealMatrix originalMatrix = MatrixUtils.createRealMatrix(array);
        //center columns by subtracting column means
        RealMatrix centeredMatrix = centerMatrixByColumnMean(originalMatrix);
        Utils.printTotalTime(startTime);

        LOG.info("Calculating covariance matrix... ");
        startTime = System.nanoTime();
        //Calculate covariance matrix of centered matrix
        Covariance covariance = new Covariance(centeredMatrix);
        RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
        Utils.printTotalTime(startTime);

        LOG.info("Singular Value Decomposition... ");
        SingularValueDecomposition svd = new SingularValueDecomposition(covarianceMatrix);
        Utils.printTotalTime(startTime);

        LOG.info("Projection... ");
        //Project the original matrix against the new axes defined by the eigenvectors
        int rowCount = originalMatrix.getRowDimension();
        int columnCount = originalMatrix.getColumnDimension();
        double[][] projectedVectors = new double[rowCount][columnCount];
        RealVector singularVector = MatrixUtils.createRealVector(svd.getSingularValues());
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                projectedVectors[row][column] = originalMatrix.getRowVector(row)
                    .dotProduct(singularVector);
            }
        }
        Utils.printTotalTime(startTime);

        return projectedVectors;
    }

    public static SingularValueDecomposition getSVD(double[][] array) {
        //create real matrix
        RealMatrix realMatrix = MatrixUtils.createRealMatrix(array);
        Covariance covariance = new Covariance(realMatrix);
        RealMatrix covarianceMatrix = covariance.getCovarianceMatrix();
        return new SingularValueDecomposition(covarianceMatrix);
    }

    public static List<Double> gmmFullCovToDiag(List<List<Double>> fullCov) {
        //Copy our covariance matrix into a 2D array (required by apache commons)
        int xSize = fullCov.size();
        int ySize = fullCov.get(0).size();
        double[][] array = new double[xSize][ySize];
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                array[x][y] = fullCov.get(x).get(y);
            }
        }
        //perform the SVD on the covariance matrix
        SingularValueDecomposition svd = getSVD(array);
        //@TODO SMP Rotate the values to get orientation
        //Copy rotated values into List<Double>
        ArrayList<Double> svdValues = new ArrayList<>();
        for (double d : svd.getSingularValues()) {
            svdValues.add(d);
        }
        return svdValues;
    }

    public static Umap umapConfigToUmap(UmapConfig config) {
        Umap latestUmap = new Umap();
        if (null != config.getTargetWeight())
            latestUmap.setTargetWeight(config.getTargetWeight());
        if (null != config.getRepulsionStrength())
            latestUmap.setRepulsionStrength(config.getRepulsionStrength());
        if (null != config.getMinDist())
            latestUmap.setMinDist(config.getMinDist());
        if (null != config.getSpread())
            latestUmap.setSpread(config.getSpread());
        if (null != config.getOpMixRatio())
            latestUmap.setSetOpMixRatio(config.getOpMixRatio());
        if (null != config.getNumberComponents())
            latestUmap.setNumberComponents(config.getNumberComponents());
        if (null != config.getNumberEpochs())
            latestUmap.setNumberEpochs(config.getNumberEpochs());
        if (null != config.getNumberNearestNeighbours())
            latestUmap.setNumberNearestNeighbours(config.getNumberNearestNeighbours());
        if (null != config.getNegativeSampleRate())
            latestUmap.setNegativeSampleRate(config.getNegativeSampleRate());
        if (null != config.getLocalConnectivity())
            latestUmap.setLocalConnectivity(config.getLocalConnectivity());
        if (null != config.getThreshold())
            latestUmap.setThreshold(config.getThreshold());
        if (null != config.getMetric())
            latestUmap.setMetric(config.getMetric());
        if (null != config.getVerbose())
            latestUmap.setVerbose(config.getVerbose());
        return latestUmap;
    }

    public static Umap getDefaultUmap() {
        Umap umap = new Umap();
        umap.setVerbose(true);
        umap.setNumberComponents(3);
        umap.setNumberEpochs(100);
        umap.setNumberNearestNeighbours(4);
        umap.setMinDist(0.25f);
        umap.setSpread(0.75f);
        umap.setNegativeSampleRate(20);
        return umap;

    }

    public static double[][] fitUMAP(FeatureCollection featureCollection) {
        Umap umap = getDefaultUmap();
        return fitUMAP(featureCollection, umap);
    }

    public static double[][] fitUMAP(FeatureCollection featureCollection, Umap umap) {
        //for each dimension extract transform via UMAP
        double[][] data = featureCollection.convertFeaturesToArray();
        LOG.info("Starting UMAP Fit... ");
        long start = System.nanoTime();
        double[][] projected = umap.fitTransform(data);
        Utils.printTotalTime(start);
        return projected;
    }

    public static double[][] transformUMAP(FeatureCollection featureCollection, Umap umap) {
        double[][] data = featureCollection.convertFeaturesToArray();
        LOG.info("Starting UMAP Transform... ");
        long start = System.nanoTime();
        double[][] projected = umap.transform(data);
        Utils.printTotalTime(start);
        return projected;
    }

}
