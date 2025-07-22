package edu.jhuapl.trinity.javafx.components;

import com.github.trinity.supermds.CVAE;
import com.github.trinity.supermds.Normalizer;
import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDSHelper;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;

import static com.github.trinity.supermds.CVAEHelper.mseLoss;
import static com.github.trinity.supermds.CVAEHelper.shuffledIndices;
import static com.github.trinity.supermds.SuperMDSValidator.generateSphereData;
import static edu.jhuapl.trinity.utils.Utils.printTotalTime;

/**
 * @author Sean Phillips
 */
public class CvaeControlBox extends VBox {
    private static final Logger LOG = LoggerFactory.getLogger(CvaeControlBox.class);
    ListView<String> musicTracks;
    CheckBox enableFadeCheckBox;

    public CvaeControlBox() {
        Spinner<Integer> numPointsSpinner = new Spinner(100, 10000, 100, 100);
        numPointsSpinner.setEditable(true);
        numPointsSpinner.setPrefWidth(100);

        VBox numPoints = new VBox(5,
            new Label("Number of Points"),
            numPointsSpinner
        );
        numPoints.setPrefWidth(200);

        Spinner<Integer> inputDimensionsSpinner = new Spinner(3, 1000, 10, 5);
        inputDimensionsSpinner.setEditable(true);
        inputDimensionsSpinner.setPrefWidth(100);

        VBox inputDim = new VBox(5,
            new Label("Input Dimensions"),
            inputDimensionsSpinner
        );

        Spinner<Integer> outputDimensionsSpinner = new Spinner(2, 1000, 3, 5);
        outputDimensionsSpinner.setEditable(true);
        outputDimensionsSpinner.setPrefWidth(100);

        VBox outputDim = new VBox(5,
            new Label("Output Dimensions"),
            outputDimensionsSpinner
        );

        Spinner<Integer> numLandmarksSpinner = new Spinner(2, 1000, 10, 10);
        numLandmarksSpinner.setEditable(true);
        numLandmarksSpinner.setPrefWidth(100);

        VBox numLandmarks = new VBox(5,
            new Label("Number of Landmarks"),
            numLandmarksSpinner
        );

//        ChoiceBox<Strategy> inverseAnchorChoiceBox = new ChoiceBox<>(
//            FXCollections.observableArrayList(SuperMDSAnchors.Strategy.values()));
//        inverseAnchorChoiceBox.getSelectionModel().selectFirst();
//        VBox inverseAnchors = new VBox(5,
//            new Label("Inverse Anchor Selection Strategy"),
//            inverseAnchorChoiceBox
//        );
//        ChoiceBox<OptimizerType> optimizerChoiceBox = new ChoiceBox<>(
//            FXCollections.observableArrayList(MultilaterationConfig.OptimizerType.values()));
//        optimizerChoiceBox.getSelectionModel().selectFirst();
//        VBox optimizer = new VBox(5,
//            new Label("Multilateration Optimizer"),
//            optimizerChoiceBox
//        );

        Button testButton = new Button("Train CVAE");
        testButton.setOnAction(e -> {
            trainCVAE(
                numPointsSpinner.getValue(),
                inputDimensionsSpinner.getValue(),
                outputDimensionsSpinner.getValue(),
                numLandmarksSpinner.getValue()
            );
        });

//            reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
//                AudioEvent.RELOAD_MUSIC_FILES));
//
        setSpacing(10);

        getChildren().addAll(
            testButton, numPoints, inputDim, outputDim, numLandmarks
        );
    }

    public void trainCVAE(int numPoints, int inputDim, int embeddingDim, int numberOfLandmarks) {
        int latentDim = 16;
        int hiddenDim = 64;
        int batchSize = 128;
        int epochs = 2000;

        // Generate dummy original data (e.g., MDS input)
//        double[][] originalData = generateRandomData(numPoints, inputDim);
        double[][] originalData = generateSphereData(numPoints, inputDim, 42);
        // Optional: generate weights... for equal weighting use all 1.0s
        //System.out.println("Initializing weights...");
        long startTime = System.nanoTime();
        double[][] weights = new double[originalData.length][originalData.length];
        for (int i = 0; i < originalData.length; i++) {
            Arrays.fill(weights[i], 1.0);
        }
        printTotalTime(startTime);
        // Build params
        SuperMDS.Params params = new SuperMDS.Params();
        params.outputDim = embeddingDim;
        params.mode = SuperMDS.Mode.PARALLEL;          // Try CLASSICAL, SUPERVISED, LANDMARK, etc.
        params.useSMACOF = true;                     // Enable SMACOF optimization
        params.weights = weights;                   // No weighting
        params.autoSymmetrize = true;             // Auto symmetrization of distance matrix
        params.useKMeansForLandmarks = true;         // If LANDMARK mode is selected
        params.classLabels = null;                 // Only used by SUPERVISED mode
        params.numLandmarks = numberOfLandmarks;                    // Used if LANDMARK mode is active
        params.useParallel = false;               // Toggle parallelized SMACOF
        params.useStressSampling = true;         // allows SMACOF to drastically reduce iterations
        params.stressSampleCount = 1000; //number of stress samples per SMACOF interation

        // Run SuperMDS/SMACOF to get embeddings
        //System.out.println("Running SMACOF MDS...");
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
        System.arraycopy(normalizedEmbedding, 0, conditions, 0, numPoints); // full 3D embedding as condition
        // Initialize CVAE
        CVAE cvae = new CVAE(inputDim, embeddingDim, latentDim, hiddenDim);
        cvae.setDebug(false);
        cvae.setUseDropout(false);
        cvae.setIsTraining(true);
        // Train the CVAE
        //System.out.println("Training CVAE...");
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
        //System.out.printf("\nAverage Reconstruction MSE: %.6f%n", avgReconError);
        //System.out.printf("\nAverage Mean Variance: %.6f%n", avgMeanVariance);

    }

}
