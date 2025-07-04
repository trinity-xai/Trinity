package SuperMDS;

import com.github.trinity.supermds.CVAE;
import static com.github.trinity.supermds.CVAEHelper.mseLoss;
import static com.github.trinity.supermds.CVAEHelper.shuffledIndices;
import com.github.trinity.supermds.Normalizer;
import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDS.Mode;
import com.github.trinity.supermds.SuperMDS.Params;
import com.github.trinity.supermds.SuperMDSHelper;
import static com.github.trinity.supermds.SuperMDSValidator.generateSphereData;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author Sean Phillips
 */

public class CVAEInverseTest {

    public static void main(String[] args) {
        // Example synthetic test case
        int numPoints = 1000;
        int inputDim = 10;      // Original high-dimensional space
        int embeddingDim = 3;    // From SMACOF MDS
        int latentDim = 16;
        int hiddenDim = 64;
        int batchSize = 128;
        int epochs = 2000;
        
        // Generate dummy original data (e.g., MDS input)
//        double[][] originalData = generateRandomData(numPoints, inputDim);
        double[][] originalData = generateSphereData(numPoints, inputDim, 42);
        // Optional: generate weights... for equal weighting use all 1.0s
        System.out.println("Initializing weights...");
        long startTime = System.nanoTime();
        double[][] weights = new double[originalData.length][originalData.length]; 
        for (int i = 0; i < originalData.length; i++) {
            Arrays.fill(weights[i], 1.0);
        }
        printTotalTime(startTime); 
        // Build params
        Params params = new Params();
        params.outputDim = embeddingDim;
        params.mode = Mode.PARALLEL;          // Try CLASSICAL, SUPERVISED, LANDMARK, etc.
        params.useSMACOF = true;                     // Enable SMACOF optimization
        params.weights = weights;                   // No weighting
        params.autoSymmetrize = true;             // Auto symmetrization of distance matrix
        params.useKMeansForLandmarks = true;         // If LANDMARK mode is selected
        params.classLabels = null;                 // Only used by SUPERVISED mode
        params.numLandmarks = 20;                    // Used if LANDMARK mode is active
        params.useParallel = false;               // Toggle parallelized SMACOF
        params.useStressSampling = true;         // allows SMACOF to drastically reduce iterations
        params.stressSampleCount = 1000; //number of stress samples per SMACOF interation
        
        // Run SuperMDS/SMACOF to get embeddings
        System.out.println("Running SMACOF MDS...");
        startTime = System.nanoTime();
        double[][] symmetricDistanceMatrix = SuperMDS.ensureSymmetricDistanceMatrix(originalData);
        //normalize
        double[][] normalizedDistanceMatrix = SuperMDSHelper.normalizeDistancesParallel(symmetricDistanceMatrix);
        double[][] mdsEmbedding = SuperMDS.runMDS(normalizedDistanceMatrix, params);
        printTotalTime(startTime);

        //Sanity check on CVAE
        Normalizer normalizer = new Normalizer(originalData, Normalizer.Type.Z_SCORE);
        double[][] normalizedData = normalizer.normalizeAll(originalData);

        // Sanity check: set conditional to first 3 dimensions of original input
        Normalizer embeddingNormalizer = new Normalizer(mdsEmbedding, Normalizer.Type.Z_SCORE);
        double[][] normalizedEmbedding = embeddingNormalizer.normalizeAll(mdsEmbedding);
        double[][] conditions = new double[numPoints][embeddingDim];
        for (int i = 0; i < numPoints; i++) {
            conditions[i] = normalizedEmbedding[i];  // full 3D embedding as condition
        }

        for(int outerLoop=0;outerLoop<10;outerLoop++) {
            // Initialize CVAE
            CVAE cvae = new CVAE(inputDim, embeddingDim, latentDim, hiddenDim);
            cvae.setDebug(false);
            cvae.setUseDropout(false);
            cvae.setIsTraining(true);
            // Train the CVAE
            System.out.println("Training CVAE...");
            startTime = System.nanoTime();
            Random rand = new Random(42L);
            for (int epoch = 0; epoch < epochs; epoch++) {
                double totalLoss = 0.0;
                int numBatches = numPoints / batchSize;

                // Shuffle the dataset at the beginning of each epoch
                int[] indices = shuffledIndices(numPoints, rand);

                for (int b = 0; b < numBatches; b++) {
                    double[][] xBatch = new double[batchSize][inputDim];
                    double[][] cBatch = new double[batchSize][embeddingDim];

                    for (int i = 0; i < batchSize; i++) {
                        int idx = indices[b * batchSize + i];
                        xBatch[i] = normalizedData[idx];
                        cBatch[i] = conditions[idx];
                    }
                    totalLoss += cvae.trainBatch(xBatch, cBatch);
                }
            }
            cvae.setIsTraining(false);

            printTotalTime(startTime);
            // Test inverseTransform
            double totalReconError = 0.0;
            double totalMeanVar = 0.0;
            for (int i = 0; i < numPoints; i++) {
                double[] recon = cvae.inverseTransform(conditions[i]);
    //            double[] recon = cvae.inverseTransform(mdsEmbedding[i]);
    //            double mse = mseLoss(originalData[i], recon);
                double mse = mseLoss(normalizedData[i], recon);
                totalReconError += mse;

                double[] var = cvae.confidenceEstimate(conditions[i], 50);
                totalMeanVar += Arrays.stream(var).average().orElse(Double.NaN);
//                System.out.printf("Condition %d: Mean variance = %.6f\n", i, meanVar);                
            }

            double avgReconError = totalReconError / numPoints;
            double avgMeanVariance = totalMeanVar / numPoints;
            System.out.printf("\nAverage Reconstruction MSE: %.6f%n", avgReconError);
            System.out.printf("\nAverage Mean Variance: %.6f%n", avgMeanVariance);
        }
    }
    public static void printTotalTime(long startTime) {
        System.out.println(totalTimeString(startTime));
    }      
    public static String totalTimeString(long startTime) {
        long estimatedTime = System.nanoTime() - startTime;
        long totalNanos = estimatedTime;
        long s = totalNanos / 1000000000;
        totalNanos -= s * 1000000000;
        long ms = totalNanos / 1000000;
        totalNanos -= ms * 1000000;

        long us = totalNanos / 1000;
        totalNanos -= us * 1000;
        return "Total elapsed time: " + s + ":s:" + ms + ":ms:" + us + ":us:" + totalNanos + ":ns";
    }    
}