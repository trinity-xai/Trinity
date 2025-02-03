/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Distance;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.messages.UmapConfig;
import edu.jhuapl.trinity.javafx.components.listviews.DistanceListItem;
import edu.jhuapl.trinity.javafx.components.listviews.ManifoldListItem;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.PCAConfig;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.umap.Umap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Sean Phillips
 */
public class ManifoldControlController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(ManifoldControlController.class);

    @FXML
    private Node root;

    //Geometry Tab
    @FXML
    private ListView<ManifoldListItem> manifoldsListView;
    private Manifold3D activeManifold3D = null;
    @FXML
    private RadioButton useVisibleRadioButton;
    @FXML
    private RadioButton useAllRadioButton;
    ToggleGroup pointsToggleGroup;
    @FXML
    private ChoiceBox labelChoiceBox;
    @FXML
    private CheckBox automaticCheckBox;
    @FXML
    private Spinner manualSpinner;
    @FXML
    private ColorPicker manifoldDiffuseColorPicker;
    @FXML
    private ColorPicker manifoldSpecularColorPicker;
    @FXML
    private ColorPicker manifoldWireMeshColorPicker;
    @FXML
    private RadioButton frontCullFaceRadioButton;
    @FXML
    private RadioButton backCullFaceRadioButton;
    @FXML
    private RadioButton noneCullFaceRadioButton;
    ToggleGroup cullfaceToggleGroup;
    @FXML
    private RadioButton fillDrawModeRadioButton;
    @FXML
    private RadioButton linesDrawModeRadioButton;
    ToggleGroup drawModeToggleGroup;
    @FXML
    private CheckBox showWireframeCheckBox;
    @FXML
    private CheckBox showControlPointsCheckBox;

    //PCA Tab
    @FXML
    private RadioButton pcaRadioButton;
    @FXML
    private RadioButton svdRadioButton;
    ToggleGroup componentAnalysisToggleGroup;
    @FXML
    private Spinner numPcaComponentsSpinner;
    @FXML
    private Spinner fitStartIndexSpinner;
    @FXML
    private Spinner fitEndIndexSpinner;
    @FXML
    private CheckBox rangedFittingCheckBox;

    @FXML
    private Spinner pcaScalingSpinner;

    @FXML
    private RadioButton pcaUseHyperspaceButton;
    @FXML
    private RadioButton pcaUseHypersurfaceButton;
    ToggleGroup pcahyperSourceGroup;

    //UMAP tab
    @FXML
    private Slider targetWeightSlider;
    @FXML
    private Slider repulsionSlider;
    @FXML
    private Slider minDistanceSlider;
    @FXML
    private Slider spreadSlider;
    @FXML
    private Slider opMixSlider;
    @FXML
    private Spinner numComponentsSpinner;
    @FXML
    private Spinner numEpochsSpinner;
    @FXML
    private Spinner nearestNeighborsSpinner;
    @FXML
    private Spinner negativeSampleRateSpinner;
    @FXML
    private Spinner localConnectivitySpinner;
    @FXML
    private Spinner<Double> thresholdSpinner;
    @FXML
    private ChoiceBox metricChoiceBox;
    @FXML
    private CheckBox verboseCheckBox;
    @FXML
    private RadioButton useHyperspaceButton;
    @FXML
    private RadioButton useHypersurfaceButton;
    ToggleGroup hyperSourceGroup;


    //Distances Tab
    @FXML
    private ListView<DistanceListItem> distancesListView;
    @FXML
    private RadioButton pointToPointRadioButton;
    @FXML
    private RadioButton pointToGroupRadioButton;
    ToggleGroup pointModeToggleGroup;
    @FXML
    private TextField distanceMetricTextField;
    @FXML
    private ColorPicker connectorColorPicker;
    @FXML
    private Spinner connectorThicknessSpinner;

    Scene scene;
    private final String ALL = "ALL";
    boolean reactive = true;
    Umap latestUmapObject = null;
    File latestDir = new File(".");

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scene = App.getAppScene();
        setupPcaControls();
        setupHullControls();
        setupUmapControls();
        setupDistanceControls();
        if (null != root) {
            root.addEventHandler(DragEvent.DRAG_OVER, event -> {
                event.acceptTransferModes(TransferMode.COPY);
            });
            root.addEventHandler(DragEvent.DRAG_DROPPED, e -> {
                Dragboard db = e.getDragboard();
                if (db.hasFiles()) {
                    File file = db.getFiles().get(0);
                    try {

                        if (UmapConfig.isUmapConfig(Files.readString(file.toPath()))) {
                            ObjectMapper mapper = new ObjectMapper();
                            UmapConfig uc = mapper.readValue(file, UmapConfig.class);
                            setUmapConfig(uc);
                            sendUmapConfig();
                        }
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
            });
            root.addEventHandler(ManifoldEvent.NEW_UMAP_CONFIG, event -> {
                UmapConfig config = (UmapConfig) event.object1;
                setUmapConfig(config);
            });
        }
    }

    private void setupDistanceControls() {
        distanceMetricTextField.setText("Select Distance Object");
        distanceMetricTextField.setEditable(false);
        connectorThicknessSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5, 1));
        connectorThicknessSpinner.setEditable(true);
        connectorThicknessSpinner.valueProperty().addListener(e -> {
            DistanceListItem item = distancesListView.getSelectionModel().getSelectedItem();
            if (null != item) {
                Integer width = (Integer) connectorThicknessSpinner.getValue();
                item.getDistance().setWidth(width);
                scene.getRoot().fireEvent(
                    new ManifoldEvent(ManifoldEvent.DISTANCE_CONNECTOR_WIDTH, item.getDistance()));
            }
        });
        connectorThicknessSpinner.setInitialDelay(Duration.millis(500));
        connectorThicknessSpinner.setRepeatDelay(Duration.millis(500));

        connectorColorPicker.valueProperty().addListener(cl -> {
            DistanceListItem item = distancesListView.getSelectionModel().getSelectedItem();
            if (null != item) {
                item.getDistance().setColor(connectorColorPicker.getValue());
                scene.getRoot().fireEvent(
                    new ManifoldEvent(ManifoldEvent.DISTANCE_CONNECTOR_COLOR, item.getDistance()));
            }
        });
        //Get a reference to any Distances already collected
        List<DistanceListItem> existingItems = new ArrayList<>();
        for (Distance d : Distance.getDistances()) {
            DistanceListItem item = new DistanceListItem(d);
            existingItems.add(item);
        }
        //add them all in one shot
        distancesListView.getItems().addAll(existingItems);
        ImageView iv = ResourceUtils.loadIcon("metric", 200);
        VBox placeholder = new VBox(10, iv, new Label("No Distances Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        distancesListView.setPlaceholder(placeholder);

        //Bind disable properties so that controls only active when item is selected
        distanceMetricTextField.disableProperty().bind(
            distancesListView.getSelectionModel().selectedIndexProperty().lessThan(0));
        connectorThicknessSpinner.disableProperty().bind(
            distancesListView.getSelectionModel().selectedIndexProperty().lessThan(0));
        connectorColorPicker.disableProperty().bind(
            distancesListView.getSelectionModel().selectedIndexProperty().lessThan(0));

        pointModeToggleGroup = new ToggleGroup();
        pointToPointRadioButton.setToggleGroup(pointModeToggleGroup);
        pointToGroupRadioButton.setToggleGroup(pointModeToggleGroup);
        scene.addEventHandler(ManifoldEvent.DISTANCE_CONNECTOR_SELECTED, e -> {
            Distance distance = (Distance) e.object1;
            for (DistanceListItem item : distancesListView.getItems()) {
                if (item.getDistance() == distance) {
                    distancesListView.getSelectionModel().select(item);
                    distanceMetricTextField.setText(distance.getMetric());
                    connectorColorPicker.setValue(distance.getColor());
                    connectorThicknessSpinner.getValueFactory().setValue(distance.getWidth());
                    return; //break out early
                }
            }
            //if we get here its because that Distance object wasn't in the list
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Distance object not found!", new Font("Consolas", 20), Color.YELLOW));
        });
        scene.addEventHandler(ManifoldEvent.DISTANCE_OBJECT_SELECTED, e -> {
            Distance distance = (Distance) e.object1;
            distanceMetricTextField.setText(distance.getMetric());
            connectorColorPicker.setValue(distance.getColor());
            connectorThicknessSpinner.getValueFactory().setValue(distance.getWidth());
        });
        scene.addEventHandler(ManifoldEvent.CREATE_NEW_DISTANCE, e -> {
            Distance distance = (Distance) e.object1;
            DistanceListItem distanceListItem = new DistanceListItem(distance);
            Distance.addDistance(distance);
            distancesListView.getItems().add(distanceListItem);
        });
    }

    private void getCurrentLabels() {
        labelChoiceBox.getItems().clear();
        labelChoiceBox.getItems().add(ALL);
        labelChoiceBox.getItems().addAll(
            FactorLabel.getFactorLabels().stream()
                .map(f -> f.getLabel()).sorted().toList());
    }

    private void setupPcaControls() {
        numPcaComponentsSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 100, 3, 1));
        fitStartIndexSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 0, 5));
        fitEndIndexSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 2000, 50, 5));
        //"min","max","initialValue","amountToStepBy"
        fitStartIndexSpinner.disableProperty().bind(rangedFittingCheckBox.selectedProperty().not());
        fitEndIndexSpinner.disableProperty().bind(rangedFittingCheckBox.selectedProperty().not());

        pcaScalingSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 1000, 100, 10));

        componentAnalysisToggleGroup = new ToggleGroup();
        pcaRadioButton.setToggleGroup(componentAnalysisToggleGroup);
        svdRadioButton.setToggleGroup(componentAnalysisToggleGroup);

        pcahyperSourceGroup = new ToggleGroup();
        pcaUseHyperspaceButton.setToggleGroup(pcahyperSourceGroup);
        pcaUseHypersurfaceButton.setToggleGroup(pcahyperSourceGroup);
    }

    private void setupUmapControls() {
        hyperSourceGroup = new ToggleGroup();
        useHyperspaceButton.setToggleGroup(hyperSourceGroup);
        useHypersurfaceButton.setToggleGroup(hyperSourceGroup);

        metricChoiceBox.getItems().addAll(Metric.getMetricNames());
        metricChoiceBox.getSelectionModel().selectFirst();

        numComponentsSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 50, 3, 1));
        numEpochsSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(25, 500, 200, 25));
        nearestNeighborsSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 500, 15, 5));
        negativeSampleRateSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 250, 5, 1));
        localConnectivitySpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 250, 1, 1));
//        thresholdSpinner.setValueFactory(
//            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.001, 1.0, 0.001, 0.001));
//        thresholdSpinner.setEditable(true);

        hyperSourceGroup = new ToggleGroup();
        useHyperspaceButton.setToggleGroup(hyperSourceGroup);
        useHypersurfaceButton.setToggleGroup(hyperSourceGroup);
    }

    private void setupHullControls() {
        //Get a reference to any Distances already collected
        List<ManifoldListItem> existingItems = new ArrayList<>();
        for (Manifold m : Manifold.getManifolds()) {
            ManifoldListItem item = new ManifoldListItem(m);
            existingItems.add(item);
        }
        //add them all in one shot
        manifoldsListView.getItems().addAll(existingItems);
        ImageView iv = ResourceUtils.loadIcon("manifold", 200);
        VBox placeholder = new VBox(10, iv, new Label("No Manifolds Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        manifoldsListView.setPlaceholder(placeholder);

        getCurrentLabels();
        labelChoiceBox.getSelectionModel().selectFirst();
        labelChoiceBox.setOnShown(e -> getCurrentLabels());
        manualSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1, 0.5, 0.1));
        manualSpinner.setEditable(true);
        //whenever the spinner value is changed...
        manualSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.SET_DISTANCE_TOLERANCE,
                    (Double) manualSpinner.getValue()));
        });
        manualSpinner.disableProperty().bind(automaticCheckBox.selectedProperty());

        manifoldDiffuseColorPicker.setValue(Color.CYAN);
        manifoldDiffuseColorPicker.valueProperty().addListener(cl -> {
            if (!reactive) return;
            ManifoldListItem item = manifoldsListView.getSelectionModel().getSelectedItem();
            if (null != item) {
                Manifold m = item.getManifold();
                if (null != m) {
                    m.setColor(manifoldDiffuseColorPicker.getValue());
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_DIFFUSE_COLOR,
                        manifoldDiffuseColorPicker.getValue(), m));
                }
            }
        });
        manifoldSpecularColorPicker.setValue(Color.RED);
        manifoldSpecularColorPicker.valueProperty().addListener(cl -> {
            if (!reactive) return;
            ManifoldListItem item = manifoldsListView.getSelectionModel().getSelectedItem();
            if (null != item) {
                Manifold m = item.getManifold();
                if (null != m)
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_SPECULAR_COLOR,
                        manifoldSpecularColorPicker.getValue(), m));
            }
        });
        manifoldWireMeshColorPicker.setValue(Color.BLUE);
        manifoldWireMeshColorPicker.valueProperty().addListener(cl -> {
            if (!reactive) return;
            ManifoldListItem item = manifoldsListView.getSelectionModel().getSelectedItem();
            if (null != item) {
                Manifold m = item.getManifold();
                if (null != m)
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_WIREFRAME_COLOR,
                        manifoldWireMeshColorPicker.getValue(), m));
            }
        });

        pointsToggleGroup = new ToggleGroup();
        useVisibleRadioButton.setToggleGroup(pointsToggleGroup);
        useAllRadioButton.setToggleGroup(pointsToggleGroup);
        pointsToggleGroup.selectedToggleProperty().addListener(cl -> {
            if (useVisibleRadioButton.isSelected())
                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.USE_VISIBLE_POINTS, true));
            else
                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.USE_ALL_POINTS, true));
        });

        cullfaceToggleGroup = new ToggleGroup();
        frontCullFaceRadioButton.setToggleGroup(cullfaceToggleGroup);
        backCullFaceRadioButton.setToggleGroup(cullfaceToggleGroup);
        noneCullFaceRadioButton.setToggleGroup(cullfaceToggleGroup);
        cullfaceToggleGroup.selectedToggleProperty().addListener(cl -> {
            Manifold m = manifoldsListView.getSelectionModel().getSelectedItem().getManifold();
            if (null != m)
                if (frontCullFaceRadioButton.isSelected())
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_FRONT_CULLFACE, true, m));
                else if (backCullFaceRadioButton.isSelected())
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_BACK_CULLFACE, true, m));
                else
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_NONE_CULLFACE, true, m));
        });
        drawModeToggleGroup = new ToggleGroup();
        fillDrawModeRadioButton.setToggleGroup(drawModeToggleGroup);
        linesDrawModeRadioButton.setToggleGroup(drawModeToggleGroup);
        drawModeToggleGroup.selectedToggleProperty().addListener(cl -> {
            Manifold m = manifoldsListView.getSelectionModel().getSelectedItem().getManifold();
            if (null != m)
                if (fillDrawModeRadioButton.isSelected())
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_FILL_DRAWMODE, true, m));
                else
                    scene.getRoot().fireEvent(new ManifoldEvent(
                        ManifoldEvent.MANIFOLD_LINE_DRAWMODE, true, m));
        });

        showWireframeCheckBox.selectedProperty().addListener(cl -> {
            Manifold m = manifoldsListView.getSelectionModel().getSelectedItem().getManifold();
            if (null != m)
                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.MANIFOLD_SHOW_WIREFRAME,
                    showWireframeCheckBox.isSelected(), m));
        });
        showControlPointsCheckBox.selectedProperty().addListener(cl -> {
            Manifold m = manifoldsListView.getSelectionModel().getSelectedItem().getManifold();
            if (null != m)
                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.MANIFOLD_SHOW_CONTROL,
                    showControlPointsCheckBox.isSelected(), m));
        });

        scene.addEventHandler(ManifoldEvent.MANIFOLD_3D_SELECTED, e -> {
            Manifold manifold = (Manifold) e.object1;
            for (ManifoldListItem item : manifoldsListView.getItems()) {
                if (item.getManifold() == manifold) {
                    manifoldsListView.getSelectionModel().select(item);
                    Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(manifold);
                    updateActiveManifold3D(manifold3D);
                    return; //break out early
                }
            }
            //if we get here its because that Distance object wasn't in the list
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Manifold object not found!", new Font("Consolas", 20), Color.YELLOW));
        });
        scene.addEventHandler(ManifoldEvent.MANIFOLD_OBJECT_SELECTED, e -> {
            Manifold manifold = (Manifold) e.object1;
            Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(manifold);
            updateActiveManifold3D(manifold3D);
        });
        scene.addEventHandler(ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, e -> {
            Manifold manifold = (Manifold) e.object1;
            updateActiveManifold3D((Manifold3D) e.object2);
            ManifoldListItem manifoldListItem = new ManifoldListItem(manifold);
            manifoldsListView.getItems().add(manifoldListItem);
            manifoldsListView.getSelectionModel().selectLast();
        });
    }

    private void updateActiveManifold3D(Manifold3D manifold3D) {
        reactive = false;
        activeManifold3D = manifold3D;
        if (null != manifold3D) {
            PhongMaterial phong = (PhongMaterial) manifold3D.quickhullMeshView.getMaterial();
            manifoldDiffuseColorPicker.setValue(phong.getDiffuseColor());
            manifoldSpecularColorPicker.setValue(phong.getSpecularColor());
            manifoldWireMeshColorPicker.setValue(((PhongMaterial)
                manifold3D.quickhullLinesMeshView.getMaterial()).getDiffuseColor());
        }
        reactive = true;
    }

    @FXML
    public void exportMatrix() {
        if (null != latestUmapObject) {
            if (null != latestUmapObject.getmEmbedding()) {
                LOG.info("latestUmapObject: {}", latestUmapObject.getmEmbedding().toStringNumpy());
            } else {
                LOG.info("UMAP Embeddings not generated yet.");
            }
        } else {
            LOG.info("UMAP object not yet established.");
        }
    }

    @FXML
    public void loadUmapConfig() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose UMAP Config to load...");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showOpenDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            ObjectMapper mapper = new ObjectMapper();
            UmapConfig uc;
            try {
                uc = mapper.readValue(file, UmapConfig.class);
                setUmapConfig(uc);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        }
    }

    public void setUmapConfig(UmapConfig uc) {
        if (null != uc.getTargetWeight())
            targetWeightSlider.setValue(uc.getTargetWeight());
        repulsionSlider.setValue(uc.getRepulsionStrength());
        minDistanceSlider.setValue(uc.getMinDist());
        spreadSlider.setValue(uc.getSpread());
        opMixSlider.setValue(uc.getOpMixRatio());
        numComponentsSpinner.getValueFactory().setValue(uc.getNumberComponents());
        numEpochsSpinner.getValueFactory().setValue(uc.getNumberEpochs());
        nearestNeighborsSpinner.getValueFactory().setValue(uc.getNumberNearestNeighbours());
        negativeSampleRateSpinner.getValueFactory().setValue(uc.getNegativeSampleRate());
        localConnectivitySpinner.getValueFactory().setValue(uc.getLocalConnectivity());
        if (null != uc.getThreshold())
            thresholdSpinner.getValueFactory().setValue(uc.getThreshold());
        metricChoiceBox.getSelectionModel().select(uc.getMetric());
        verboseCheckBox.setSelected(uc.getVerbose());

    }

    @FXML
    public void saveUmapConfig() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose UMAP Config file output...");
        fc.setInitialFileName(UmapConfig.configToFilename(getCurrentUmapConfig()).concat(".json"));
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showSaveDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            writeConfigFile(file);
        }
    }
    public UmapConfig getCurrentUmapConfig(){
        UmapConfig uc = new UmapConfig();
        uc.setTargetWeight((float) targetWeightSlider.getValue());
        uc.setRepulsionStrength((float) repulsionSlider.getValue());
        uc.setMinDist((float) minDistanceSlider.getValue());
        uc.setSpread((float) spreadSlider.getValue());
        uc.setOpMixRatio((float) opMixSlider.getValue());
        uc.setNumberComponents((int) numComponentsSpinner.getValue());
        uc.setNumberEpochs((int) numEpochsSpinner.getValue());
        uc.setNumberNearestNeighbours((int) nearestNeighborsSpinner.getValue());
        uc.setNegativeSampleRate((int) negativeSampleRateSpinner.getValue());
        uc.setLocalConnectivity((int) localConnectivitySpinner.getValue());
        uc.setThreshold((double) thresholdSpinner.getValue());
        uc.setMetric((String) metricChoiceBox.getValue());
        uc.setVerbose(verboseCheckBox.isSelected());
        return uc;
    }
    private void writeConfigFile(File file) {
        UmapConfig uc = getCurrentUmapConfig();
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(file, uc);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }        
    }
//    private String configToFilename(){
//        NumberFormat format = new DecimalFormat("0.00");
//        StringBuilder sb = new StringBuilder("UmapConfig-");
////        sb.append(targetWeightSlider.getValue()).append("-");
//        sb.append((String) metricChoiceBox.getValue()).append("-");
//        sb.append("R").append(format.format(repulsionSlider.getValue())).append("-");
//        sb.append("MD").append(format.format(minDistanceSlider.getValue())).append("-");
//        sb.append("S").append(format.format(spreadSlider.getValue())).append("-");
//        sb.append("OPM").append(format.format(opMixSlider.getValue())).append("-");
////        uc.setNumberComponents((int) numComponentsSpinner.getValue());
////        uc.setNumberEpochs((int) numEpochsSpinner.getValue());
//        sb.append("NN").append(nearestNeighborsSpinner.getValue()).append("-");
//        sb.append("NSR").append(negativeSampleRateSpinner.getValue()).append("-");
//        sb.append("LC").append(localConnectivitySpinner.getValue());
////        uc.setThreshold((double) thresholdSpinner.getValue());
////        uc.setVerbose(verboseCheckBox.isSelected());
//        return sb.toString();
//    }
    private void sendUmapConfig() {
        String name = latestDir.getAbsolutePath() + File.separator 
            + UmapConfig.configToFilename(getCurrentUmapConfig()).concat(".json");
        scene.getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.NEW_UMAP_CONFIG, getCurrentUmapConfig(), name));
        
        
    }
    @FXML
    public void exportScene() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Location file output...");
        fc.setInitialFileName(UmapConfig.configToFilename(getCurrentUmapConfig()).concat(".json"));
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showSaveDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file.getParentFile();
            writeConfigFile(file);
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.EXPORT_PROJECTION_SCENE, file));
        }
    }

    @FXML
    public void saveProjections() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Projection file output...");
        fc.setInitialFileName("ProjectionData.json");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showSaveDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.SAVE_PROJECTION_DATA, file));
        }
    }

    @FXML
    public void runPCA() {
        AnalysisUtils.SOURCE source = useHypersurfaceButton.isSelected() ?
            AnalysisUtils.SOURCE.HYPERSURFACE : AnalysisUtils.SOURCE.HYPERSPACE;
        AnalysisUtils.ANALYSIS_METHOD method = svdRadioButton.isSelected() ?
            AnalysisUtils.ANALYSIS_METHOD.SVD : AnalysisUtils.ANALYSIS_METHOD.PCA;

        int startIndex = 0;
        int endIndex = -1; //use max indicator
        if (rangedFittingCheckBox.isSelected()) {
            startIndex = (int) fitStartIndexSpinner.getValue();
            endIndex = (int) fitEndIndexSpinner.getValue();
            if (endIndex <= startIndex)
                endIndex = -1;
        }

        PCAConfig config = new PCAConfig(source, method,
            (int) numPcaComponentsSpinner.getValue(), (double) pcaScalingSpinner.getValue(),
            startIndex, endIndex);
        ManifoldEvent.POINT_SOURCE pointSource = useHypersurfaceButton.isSelected() ?
            ManifoldEvent.POINT_SOURCE.HYPERSURFACE : ManifoldEvent.POINT_SOURCE.HYPERSPACE;
        scene.getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.GENERATE_NEW_PCA, config, pointSource));
    }

    @FXML
    public void project() {
        Umap umap = new Umap();
        umap.setTargetWeight((float) targetWeightSlider.getValue());
        umap.setRepulsionStrength((float) repulsionSlider.getValue());
        umap.setMinDist((float) minDistanceSlider.getValue());
        umap.setSpread((float) spreadSlider.getValue());
        umap.setSetOpMixRatio((float) opMixSlider.getValue());
        umap.setNumberComponents((int) numComponentsSpinner.getValue());
        umap.setNumberEpochs((int) numEpochsSpinner.getValue());
        umap.setNumberNearestNeighbours((int) nearestNeighborsSpinner.getValue());
        umap.setNegativeSampleRate((int) negativeSampleRateSpinner.getValue());
        umap.setLocalConnectivity((int) localConnectivitySpinner.getValue());
        umap.setThreshold((double) thresholdSpinner.getValue());
        umap.setMetric((String) metricChoiceBox.getValue());
        umap.setVerbose(verboseCheckBox.isSelected());
        ManifoldEvent.POINT_SOURCE pointSource = useHypersurfaceButton.isSelected() ?
            ManifoldEvent.POINT_SOURCE.HYPERSURFACE : ManifoldEvent.POINT_SOURCE.HYPERSPACE;
        scene.getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.GENERATE_NEW_UMAP, umap, pointSource));
        latestUmapObject = umap;
    }

    @FXML
    public void generate() {
        Double tolerance = null;
        if (!automaticCheckBox.isSelected())
            tolerance = (Double) manualSpinner.getValue();
        scene.getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.GENERATE_PROJECTION_MANIFOLD,
            useVisibleRadioButton.isSelected(),
            (String) labelChoiceBox.getValue(), tolerance
        ));
    }

    @FXML
    public void clearAll() {
        scene.getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.CLEAR_ALL_MANIFOLDS));
        //add them all in one shot
        manifoldsListView.getItems().clear();
    }

    @FXML
    public void exportAll() {
        DirectoryChooser fc = new DirectoryChooser();
        fc.setTitle("Choose Directory to export ManifoldData files...");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory()) {
                latestDir = file;
                int sequence = 0;
                for (ManifoldListItem m : manifoldsListView.getItems()) {
                    Manifold3D m3D = Manifold.globalManifoldToManifold3DMap.get(m.getManifold());
                    if (null != m3D) {
                        String fileName = "ManifoldData_" + sequence + "_" + m.getManifold().getLabel() + ".json";
                        File exportFile = new File(latestDir.getPath() + fileName);
                        scene.getRoot().fireEvent(new ManifoldEvent(
                            ManifoldEvent.EXPORT_MANIFOLD_DATA, exportFile, m3D));
                    }
                    sequence++;
                }
            }
        }
    }

    @FXML
    public void clusterBuilder() {
        //fire event to put projection view into cluster selection mode
        scene.getRoot().fireEvent(new ApplicationEvent(
            ApplicationEvent.SHOW_SHAPE3D_CONTROLS));
    }

    @FXML
    public void startConnector() {
        //fire event to put projection view into connector mode
        //@TODO SMP Hardcoded for now to Euclidean
        if (pointToGroupRadioButton.isSelected())
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.DISTANCE_MODE_POINTGROUP, pointToGroupRadioButton.isSelected(), "euclidean"));
        else
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.DISTANCE_MODE_POINTPOINT, pointToPointRadioButton.isSelected(), "euclidean"));
    }

    @FXML
    public void clearAllDistances() {
        distancesListView.getItems().clear();
        Distance.removeAllDistances(); //will fire event notifying scene
        scene.getRoot().fireEvent(
            new ManifoldEvent(ManifoldEvent.CLEAR_DISTANCE_CONNECTORS));
    }
}
