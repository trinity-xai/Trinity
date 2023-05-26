package edu.jhuapl.trinity.utils;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.utils.umap.Umap;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static edu.jhuapl.trinity.utils.AnalysisUtils.EPISILON;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * @author Sean Phillips
 */
public class AnalysisUtilsTest {

    public AnalysisUtilsTest() {
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    @Test
    public void testDoCommonsPCA() {
        System.out.println("doCommonsPca");
        double[] expected = new double[]{2.0, 0.666666666666667};
        //create points in a double array
        double[][] array = new double[][]{
            new double[]{-1.0, -1.0},
            new double[]{-1.0, 1.0},
            new double[]{1.0, 1.0}};
        double[] eigenValues = AnalysisUtils.doCommonsPCA(array);
        assertArrayEquals(expected, eigenValues, EPISILON);
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    @Test
    public void testDoCommonsSVD() {
        System.out.println("doCommonsSVD");
        double[] expected = new double[]{2.0, 0.666666666666667};
        //create points in a double array
        double[][] array = new double[][]{
            new double[]{-1.0, -1.0},
            new double[]{-1.0, 1.0},
            new double[]{1.0, 1.0}};
        double[] singularValues = AnalysisUtils.doCommonsSVD(array);
        assertArrayEquals(expected, singularValues, EPISILON);
    }

    //    @Test
    public void testUMAP() throws IOException {
        FeatureCollectionFile fcf = new FeatureCollectionFile("CLIP_data.json", true);
        System.out.println("Feature Vectors discovered: " + fcf.featureCollection.getFeatures().size());
        System.out.print("Converting to double 2D array... ");
        long startTime = System.nanoTime();
        // input data instances * attributes
        double[][] data = fcf.featureCollection.convertFeaturesToArray();
        Utils.printTotalTime(startTime);
        Umap umap = new Umap();
        umap.setNumberComponents(3); // number of dimensions in result
        umap.setNumberNearestNeighbours(15);
        umap.setThreads(1);  // use > 1 to enable parallelism
        System.out.print("Fitting Transform... ");
        startTime = System.nanoTime();
        double[][] result = umap.fitTransform(data);
        Utils.printTotalTime(startTime);
        System.out.println("UMAP Test complete.");
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    @Test
    public void testSpikeySVD() {
        System.out.println("spikeySVD");
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
        double[] singularValues = AnalysisUtils.doCommonsSVD(array);
        assertArrayEquals(expected, singularValues, EPISILON);
    }
}
