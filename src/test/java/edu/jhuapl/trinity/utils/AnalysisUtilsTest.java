/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.utils.umap.Umap;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static edu.jhuapl.trinity.utils.AnalysisUtils.EPISILON;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Sean Phillips
 */
public class AnalysisUtilsTest {
    private static final Logger LOG = LoggerFactory.getLogger(AnalysisUtilsTest.class);

    public AnalysisUtilsTest() {
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    @Test
    public void testDoCommonsPCA() {
        LOG.info("doCommonsPca");
        double[][] expected = new double[][]{
            {1.414213562373095, -1.1102230246251565E-16},
            {-1.1102230246251565E-16, -1.414213562373095},
            {-1.414213562373095, 1.1102230246251565E-16}
        };
        //create points in a double array
        double[][] array = new double[][]{
            new double[]{-1.0, -1.0},
            new double[]{-1.0, 1.0},
            new double[]{1.0, 1.0}};
        double[][] pcaProjection = AnalysisUtils.doCommonsPCA(array);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], pcaProjection[i], EPISILON);
        }
        assert true;
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    @Test
    public void testDoCommonsSVD() {
        LOG.info("doCommonsSVD");
        double[][] expected = new double[][]{
            {-2.6666666666666665, -2.6666666666666665},
            {-1.3333333333333326, -1.3333333333333326},
            {2.6666666666666665, 2.6666666666666665},
        };
        //create points in a double array
        double[][] array = new double[][]{
            new double[]{-1.0, -1.0},
            new double[]{-1.0, 1.0},
            new double[]{1.0, 1.0}};
        double[][] projectedValues = AnalysisUtils.doCommonsSVD(array);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], projectedValues[i], EPISILON);
        }
        assert true;
    }

    //    @Test
    public void testUMAP() throws IOException {
        FeatureCollectionFile fcf = new FeatureCollectionFile("CLIP_data.json", true);
        LOG.info("Feature Vectors discovered: {}", fcf.featureCollection.getFeatures().size());
        LOG.info("Converting to double 2D array... ");
        long startTime = System.nanoTime();
        // input data instances * attributes
        double[][] data = fcf.featureCollection.convertFeaturesToArray();
        Utils.printTotalTime(startTime);
        Umap umap = new Umap();
        umap.setNumberComponents(3); // number of dimensions in result
        umap.setNumberNearestNeighbours(15);
        umap.setThreads(1);  // use > 1 to enable parallelism
        LOG.info("Fitting Transform... ");
        startTime = System.nanoTime();
        double[][] result = umap.fitTransform(data);
        Utils.printTotalTime(startTime);
        LOG.info("UMAP Test complete.");
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    @Test
    public void testSpikeySVD() {
        LOG.info("spikeySVD");
        double[] expected = new double[]{0.20527110065548962};
        double[][] array = new double[][]{
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.18862942235340174},
            new double[]{0.3175438838965584},
            new double[]{0.34748405819033845},
            new double[]{0.41893404526853784},
            new double[]{0.5122691218898938},
            new double[]{0.6882497833223584},
            new double[]{0.7429144302027225},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{1.0},
            new double[]{0.7199251939886013},
            new double[]{0.7386902065829919},
            new double[]{0.7641767715087128},
            new double[]{0.8196730323449362},
            new double[]{0.8272402938027279},
            new double[]{0.9188452896235546},
            new double[]{0.9232794471043959},
            new double[]{0.9740604725687169},
            new double[]{0.9314549109757023},
            new double[]{0.8992303160244066},
            new double[]{0.8100398486776101},
            new double[]{0.6876652775839744},
            new double[]{0.6788232387123216},
            new double[]{0.6747742481548563},
            new double[]{0.592711030864342},
            new double[]{0.48704426653592126},
            new double[]{0.07789452265161767},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0},
            new double[]{0.0411789163222166},
            new double[]{0.679070284444705},
            new double[]{0.6821951551460912}
        };
        SingularValueDecomposition svd = AnalysisUtils.getSVD(array);
        double[] singularValues = svd.getSingularValues();
        assertArrayEquals(expected, singularValues, EPISILON);
    }
}
