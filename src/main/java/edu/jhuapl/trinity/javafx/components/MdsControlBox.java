package edu.jhuapl.trinity.javafx.components;

import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDS.Params;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.utils.DoubleConverter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class MdsControlBox extends VBox {
    private static final Logger LOG = LoggerFactory.getLogger(MdsControlBox.class);
    public static double CONTROL_PREF_WIDTH = 200.0;
    public static double CONTROL_SPACING = 50.0;
    ChoiceBox<SuperMDS.Mode> modeChoiceBox;
    Spinner<Integer> stressSampleSpinner;
    Spinner<Integer> outputDimensionsSpinner;
    Spinner<Integer> numLandmarksSpinner;
    Spinner<Integer> maxIterationsSpinner;
    Spinner<Double> toleranceSpinner;
    CheckBox useStressSamplesCheckBox;
    CheckBox useKmeansForLandmarks;
    CheckBox forceSMACOFCheckBox;
    CheckBox autoSymmetrizeCheckBox;
    CheckBox stressMetricsCheckBox;

    public MdsControlBox() {
        modeChoiceBox = new ChoiceBox<>(
            FXCollections.observableArrayList(SuperMDS.Mode.values()));
        modeChoiceBox.getSelectionModel().select(SuperMDS.Mode.PARALLEL);
        modeChoiceBox.setTooltip(new Tooltip(
            "Determines MDS Algorithm. \nNote: PARALLEL uses SMACOF and overrides runParallel as true."));
        VBox mdsMode = new VBox(5,
            new Label("MDS Mode"),
            modeChoiceBox
        );
        modeChoiceBox.setPrefWidth(CONTROL_PREF_WIDTH);

        outputDimensionsSpinner = new Spinner(2, 1000, 3, 5);
        outputDimensionsSpinner.setEditable(true);
        outputDimensionsSpinner.setPrefWidth(100);
        VBox outputDim = new VBox(5,
            new Label("Output Dimensions"),
            outputDimensionsSpinner
        );

        forceSMACOFCheckBox = new CheckBox("Force SMACOF Optimization");
        forceSMACOFCheckBox.setSelected(true);
        forceSMACOFCheckBox.setPrefWidth(CONTROL_PREF_WIDTH);

        autoSymmetrizeCheckBox = new CheckBox("Auto Symmetrize Input Matrix");
        autoSymmetrizeCheckBox.setSelected(true);

        useStressSamplesCheckBox = new CheckBox("Optimize via Stress Sampling");
        useStressSamplesCheckBox.setSelected(true);
        useStressSamplesCheckBox.setPrefWidth(CONTROL_PREF_WIDTH);

        stressSampleSpinner = new Spinner(10, 10000, 100, 10);
        stressSampleSpinner.setEditable(true);
        stressSampleSpinner.setPrefWidth(CONTROL_PREF_WIDTH);
        stressSampleSpinner.disableProperty().bind(
            useStressSamplesCheckBox.selectedProperty().not());
        VBox stressSamples = new VBox(5,
            new Label("Stress Sample Count"),
            stressSampleSpinner
        );

        useKmeansForLandmarks = new CheckBox("KMeans++ for Landmarks");
        useKmeansForLandmarks.setSelected(true);
        useKmeansForLandmarks.setPrefWidth(CONTROL_PREF_WIDTH);
        numLandmarksSpinner = new Spinner(2, 1000, 10, 10);
        numLandmarksSpinner.setEditable(true);
        numLandmarksSpinner.setPrefWidth(CONTROL_PREF_WIDTH);

        VBox numLandmarks = new VBox(5,
            new Label("Number of Landmarks"),
            numLandmarksSpinner
        );

        maxIterationsSpinner = new Spinner(100, 2000, 500, 100);
        maxIterationsSpinner.setEditable(true);
        maxIterationsSpinner.setPrefWidth(CONTROL_PREF_WIDTH);
        VBox maxIterations = new VBox(5,
            new Label("Max Iterations"),
            maxIterationsSpinner
        );

        toleranceSpinner = new Spinner();
        SpinnerValueFactory<Double> valueFactory = new SpinnerValueFactory
            .DoubleSpinnerValueFactory(1e-8, 1e-2, 1e-6, 1e-6);
        DoubleConverter doubleConverter = new DoubleConverter("###.######");
        valueFactory.setConverter(doubleConverter);
        toleranceSpinner.setValueFactory(valueFactory);
        toleranceSpinner.setEditable(true);
        toleranceSpinner.setPrefWidth(CONTROL_PREF_WIDTH);
        VBox tolerance = new VBox(5,
            new Label("Tolerance"),
            toleranceSpinner
        );

        stressMetricsCheckBox = new CheckBox("Generate Metrics");
        stressMetricsCheckBox.setSelected(false);
        stressMetricsCheckBox.setPrefWidth(CONTROL_PREF_WIDTH);

        Button runMDSButton = new Button("Run MDS");
        runMDSButton.setDefaultButton(true);
        runMDSButton.setOnAction(e -> runMDSTask());
        setSpacing(CONTROL_SPACING / 2.0);
        setPadding(new Insets(10));
        getChildren().addAll(
            new HBox(CONTROL_SPACING, mdsMode, outputDim),
            new HBox(CONTROL_SPACING, forceSMACOFCheckBox, autoSymmetrizeCheckBox),
            new HBox(CONTROL_SPACING, useStressSamplesCheckBox, stressSamples),
            new HBox(CONTROL_SPACING, useKmeansForLandmarks, numLandmarks),
            new HBox(CONTROL_SPACING, tolerance, maxIterations),
            new HBox(CONTROL_SPACING, stressMetricsCheckBox, runMDSButton)
        );
    }

    public void runMDSTask() {
        // Build params
        Params params = new SuperMDS.Params();
        params.outputDim = outputDimensionsSpinner.getValue();
        params.mode = modeChoiceBox.getSelectionModel().getSelectedItem(); //CLASSICAL, SUPERVISED, LANDMARK, etc.
        params.useSMACOF = forceSMACOFCheckBox.isSelected();// Enable SMACOF optimization
        params.weights = null;                   // No weighting
        params.autoSymmetrize = autoSymmetrizeCheckBox.isSelected(); // Auto symmetrization of distance matrix
        params.useKMeansForLandmarks = useKmeansForLandmarks.isSelected();// If LANDMARK mode is selected
        params.classLabels = null;                 // Only used by SUPERVISED mode
        params.numLandmarks = numLandmarksSpinner.getValue();   // Used if LANDMARK mode is active
        params.useStressSampling = useStressSamplesCheckBox.isSelected(); // allows SMACOF to drastically reduce iterations
        params.stressSampleCount = stressSampleSpinner.getValue(); //number of stress samples per SMACOF interation
        params.tolerance = toleranceSpinner.getValue();
        params.maxIterations = maxIterationsSpinner.getValue();

        Platform.runLater(() -> {
            getScene().getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.GENERATE_NEW_MDS,
                    params, stressMetricsCheckBox.isSelected()));
        });
    }
}
