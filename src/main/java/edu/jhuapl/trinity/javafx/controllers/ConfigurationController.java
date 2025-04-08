/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.controllers;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.data.files.LabelConfigFile;
import edu.jhuapl.trinity.data.messages.xai.LabelConfig;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.components.listviews.DimensionLabelItem;
import edu.jhuapl.trinity.javafx.components.listviews.FactorLabelListItem;
import edu.jhuapl.trinity.javafx.components.listviews.FeatureLayerListItem;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.RadialEntityEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.utils.Configuration;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Sean Phillips
 */
public class ConfigurationController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationController.class);

    @FXML
    private CheckBox enableEmittersCheckBox;
    @FXML
    private CheckBox enableOrbitingCheckBox;
    @FXML
    private CheckBox enableEmptyVisionCheckBox;
    @FXML
    private Slider itemSizeSlider;
    @FXML
    private Slider innerRadiusSlider;
    @FXML
    private Slider itemFitWidthSlider;
    @FXML
    private Slider menuSizeSlider;
    @FXML
    private Slider offsetSlider;
    @FXML
    private Slider initialAngleSlider;
    @FXML
    private Slider strokeWidthSlider;
    @FXML
    private Label itemSizeLabel;
    @FXML
    private Label innerRadiusLabel;
    @FXML
    private Label itemFitWidthLabel;
    @FXML
    private Label menuSizeLabel;
    @FXML
    private Label offsetLabel;
    @FXML
    private Label initialAngleLabel;
    @FXML
    private Label strokeWidthLabel;

    //Shadow Cube controls
    @FXML
    private CheckBox cubeWallsVisibleCheckBox;
    @FXML
    private CheckBox controlPointsVisibleCheckBox;
    @FXML
    private CheckBox cubeVisibleCheckBox;
    @FXML
    private CheckBox enableCubeRenderingCheckBox;
    @FXML
    private CheckBox frameVisibleCheckBox;
    @FXML
    private CheckBox gridlinesVisibleCheckBox;
    @FXML
    private CheckBox showNearsidePointsCheckBox;
    @FXML
    private CheckBox overrideDomainTransformCheckBox;
    @FXML
    private CheckBox showAxesAndLabelsCheckBox;
    @FXML
    private CheckBox overrideXFormCheckBox;
    @FXML
    private Spinner domainMinimumSpinner;
    @FXML
    private Spinner domainMaximumSpinner;
    @FXML
    private Spinner pointScalingSpinner;

    @FXML
    private RadioButton fixedOrthographicRadioButton;
    @FXML
    private RadioButton rotatingPerspectiveRadioButton;

    ToggleGroup projectionToggleGroup;

    @FXML
    private ColorPicker backgroundColorPicker;
    @FXML
    private CheckBox skyboxCheckBox;
    @FXML
    private CheckBox featureDataCheckBox;

    //Dimensionality Controls
    @FXML
    private ListView<DimensionLabelItem> dimensionLabelsListView;
    @FXML
    private ScrollPane dimensionLabelsScrollPane;

    @FXML
    private Spinner<Integer> xFactorSpinner;
    @FXML
    private Spinner<Integer> yFactorSpinner;
    @FXML
    private Spinner<Integer> zFactorSpinner;
    @FXML
    private CheckBox enableDirectionCheckBox;

    @FXML
    private Spinner<Integer> xDirectionFactorSpinner;
    @FXML
    private Spinner<Integer> yDirectionFactorSpinner;
    @FXML
    private Spinner<Integer> zDirectionFactorSpinner;

    @FXML
    private Spinner featureVectorMaxSpinner;
    @FXML
    private Spinner nodeQueueLimitSpinner;
    @FXML
    private Spinner refreshRateSpinner;
    @FXML
    private Spinner point3DSizeSpinner;
    @FXML
    private Spinner pointScaleSpinner;
    @FXML
    private Spinner scatterBuffScalingSpinner;
    @FXML
    private CheckBox meanCenteredCheckBox;
    @FXML
    private RadioButton autoNormalizeRadioButton;
    @FXML
    private RadioButton manualScalingRadioButton;
    @FXML
    private TextField currentMaxAbsTextField;
    @FXML
    private TextField meanCenteredMaxAbsTextField;

    @FXML
    private Spinner scoreMinimumSpinner;
    @FXML
    private Spinner scoreMaximumSpinner;
    @FXML
    private Spinner pfaMinimumSpinner;
    @FXML
    private Spinner pfaMaximumSpinner;

    @FXML
    private RadioButton colorByLabelRadioButton;
    @FXML
    private RadioButton colorByLayerRadioButton;
    @FXML
    private RadioButton colorByGradientRadioButton;
    @FXML
    private RadioButton colorByScoreRadioButton;
    @FXML
    private RadioButton colorByPfaRadioButton;
    @FXML
    private CheckBox toggleAllVisibleCheckBox;

    @FXML
    private ChoiceBox<String> colorMapChoiceBox;
    @FXML
    private RadioButton twoColorRadioButton;
    @FXML
    private RadioButton singleColorRadioButton;
    @FXML
    private RadioButton hsbWheelRadioButton;
    @FXML
    private RadioButton colorMapRadioButton;
    ToggleGroup colorModeToggleGroup;
    @FXML
    private ColorPicker colorSpectrumColorPicker;
    @FXML
    private ColorPicker twoColor1ColorPicker;
    @FXML
    private ColorPicker twoColor2ColorPicker;

    @FXML
    private ListView<FactorLabelListItem> factorLabelListView;
    @FXML
    private ListView<FeatureLayerListItem> featureLayerListView;

    ToggleGroup scalingToggleGroup;
    ToggleGroup colorToggleGroup;

    int lockedValue = 0;
    int lockedDirectionValue = 3;
    boolean enableCoordinateNotifications = true;
    int featureVectorMax = 512;
    int nodeQueueLimit = 50000;
    int refreshRate = 500; //ms
    double pointScale = 1.0;
    double point3dSize = 10;
    double scatterBuffScaling = 1.0;
    double maxAbsVal = 1.0;
    double meanCenteredMaxAbsVal = 1.0;
    File latestDir = new File(".");
    Scene scene;
    Configuration config;
    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00");

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scene = App.getAppScene();
        config = App.getConfig();
        setupShadowCubeControls();
        setupMenuSystemControls();
        setupHyperspaceControls();

        projectionToggleGroup = new ToggleGroup();
        fixedOrthographicRadioButton.setToggleGroup(projectionToggleGroup);
        rotatingPerspectiveRadioButton.setToggleGroup(projectionToggleGroup);
        projectionToggleGroup.selectedToggleProperty().addListener(cl -> {
            if (fixedOrthographicRadioButton.isSelected())
                scene.getRoot().fireEvent(new ShadowEvent(
                    ShadowEvent.FIXED_ORHOGRAPHIC_PROJECTION, true));
            else
                scene.getRoot().fireEvent(new ShadowEvent(
                    ShadowEvent.ROTATING_PERSPECTIVE_PROJECTION, true));
        });
        setupPositionSpinners();
        setupDirectionSpinners();
        //Dimensional Label stuff
        ImageView iv = ResourceUtils.loadIcon("dimensions", 250);
        VBox placeholder = new VBox(20, iv, new Label("No Custom Dimension Labels"));
        placeholder.setAlignment(Pos.CENTER);
        dimensionLabelsListView.setPlaceholder(placeholder);
        //get all existing dimension labels
        List<DimensionLabelItem> existingDimensions = new ArrayList<>();
        for (Dimension d : Dimension.getDimensions()) {
            existingDimensions.add(new DimensionLabelItem(d));
        }
        //add them all in one shot to avoid massive event firings
        dimensionLabelsListView.getItems().addAll(existingDimensions);
        //add event listener for new features
        scene.getRoot().addEventHandler(HyperspaceEvent.DIMENSION_LABELS_SET, e -> {
            dimensionLabelsListView.getItems().clear();
            List<DimensionLabelItem> newDimensions = new ArrayList<>();
            for (Dimension d : Dimension.getDimensions()) {
                newDimensions.add(new DimensionLabelItem(d));
            }
            //add them all in one shot to avoid massive event firings
            dimensionLabelsListView.getItems().addAll(newDimensions);
        });

        List<FactorLabelListItem> existingLabelItems = new ArrayList<>();
        for (FactorLabel fl : FactorLabel.getFactorLabels()) {
            FactorLabelListItem item = new FactorLabelListItem(fl);
            existingLabelItems.add(item);
        }
        factorLabelListView.getItems().addAll(existingLabelItems); //add them all in one shot

        //add event listener for new features
        scene.getRoot().addEventHandler(HyperspaceEvent.ADDED_FACTOR_LABEL, e -> {
            FactorLabel fl = (FactorLabel) e.object;
            FactorLabelListItem item = new FactorLabelListItem(fl);
            factorLabelListView.getItems().add(item);
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.ADDEDALL_FACTOR_LABELS, e -> {
            List<FactorLabel> newLabels = (List<FactorLabel>) e.object;
            List<FactorLabelListItem> items = new ArrayList<>();
            for (FactorLabel fl : newLabels) {
                FactorLabelListItem item = new FactorLabelListItem(fl);
                items.add(item);
            }
            factorLabelListView.getItems().addAll(items); //add them all in one shot
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.CLEARED_FACTOR_LABELS, e -> {
            factorLabelListView.getItems().clear();
        });

        List<FeatureLayerListItem> existingLayerItems = new ArrayList<>();
        for (FeatureLayer fl : FeatureLayer.getFeatureLayers()) {
            FeatureLayerListItem item = new FeatureLayerListItem(fl);
            existingLayerItems.add(item);
        }
        featureLayerListView.getItems().addAll(existingLayerItems); //add them all in one shot

        //add event listener for new features
        scene.getRoot().addEventHandler(HyperspaceEvent.ADDED_FEATURE_LAYER, e -> {
            FeatureLayer fl = (FeatureLayer) e.object;
            FeatureLayerListItem item = new FeatureLayerListItem(fl);
            featureLayerListView.getItems().add(item);
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.ADDEDALL_FEATURE_LAYER, e -> {
            List<FeatureLayer> newLayers = (List<FeatureLayer>) e.object;
            List<FeatureLayerListItem> items = new ArrayList<>();
            for (FeatureLayer fl : newLayers) {
                FeatureLayerListItem item = new FeatureLayerListItem(fl);
                items.add(item);
            }
            featureLayerListView.getItems().addAll(items); //add them all in one shot
        });

        //Data bounds spinners
        setupDataBoundsSpinners();
        addHyperspaceListeners();
        setupColorControls();
    }

    private void setupColorControls() {

        scoreMinimumSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, 0.0, 0.1));
        scoreMinimumSpinner.setEditable(true);
        scoreMinimumSpinner.valueProperty().addListener(e -> {
            ColorMap.domainMin1 = (Double) scoreMinimumSpinner.getValue();
            scene.getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.COLOR_DOMAIN_CHANGE));
        });

        scoreMaximumSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, 1.0, 0.1));
        scoreMaximumSpinner.setEditable(true);
        scoreMaximumSpinner.valueProperty().addListener(e -> {
            ColorMap.domainMax1 = (Double) scoreMaximumSpinner.getValue();
            scene.getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.COLOR_DOMAIN_CHANGE));
        });
        pfaMinimumSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, 0.0, 0.1));
        pfaMinimumSpinner.setEditable(true);
        pfaMinimumSpinner.valueProperty().addListener(e -> {
            ColorMap.domainMin2 = (Double) pfaMinimumSpinner.getValue();
            scene.getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.COLOR_DOMAIN_CHANGE));
        });
        pfaMaximumSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, 1.0, 0.1));
        pfaMaximumSpinner.setEditable(true);
        pfaMaximumSpinner.valueProperty().addListener(e -> {
            ColorMap.domainMax2 = (Double) pfaMaximumSpinner.getValue();
            scene.getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.COLOR_DOMAIN_CHANGE));
        });

        //Set default colors
        colorSpectrumColorPicker.setValue(ColorMap.singleColorSpectrum);
        colorSpectrumColorPicker.valueProperty().addListener(cl -> {
            ColorMap.singleColorSpectrum = colorSpectrumColorPicker.getValue();
            colorSpectrumColorPicker.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.ONE_COLOR_SPECTRUM,
                    colorSpectrumColorPicker.getValue()));
        });

        twoColor1ColorPicker.setValue(ColorMap.twoColorSpectrum1);
        twoColor1ColorPicker.valueProperty().addListener(cl -> {
            ColorMap.twoColorSpectrum1 = twoColor1ColorPicker.getValue();
            twoColor1ColorPicker.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.TWO_COLOR_SPECTRUM,
                    twoColor1ColorPicker.getValue(), twoColor2ColorPicker.getValue()));
        });
        twoColor2ColorPicker.setValue(ColorMap.twoColorSpectrum2);
        twoColor2ColorPicker.valueProperty().addListener(cl -> {
            ColorMap.twoColorSpectrum2 = twoColor2ColorPicker.getValue();
            twoColor2ColorPicker.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.TWO_COLOR_SPECTRUM,
                    twoColor1ColorPicker.getValue(), twoColor2ColorPicker.getValue()));
        });

        colorMapChoiceBox.getItems().setAll(FXCollections.observableArrayList(ColorMap.getColorMapNames()));
        colorMapChoiceBox.getSelectionModel().select(0);
        colorMapChoiceBox.getSelectionModel().selectedItemProperty().addListener(cl -> {
            ColorMap.currentMap = ColorMap.getColorMap(colorMapChoiceBox.getValue());
            colorMapChoiceBox.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.PRESET_COLOR_PALETTE,
                    colorMapChoiceBox.getValue()));
        });

        colorModeToggleGroup = new ToggleGroup();
        twoColorRadioButton.setToggleGroup(colorModeToggleGroup);
        twoColor1ColorPicker.disableProperty().bind(twoColorRadioButton.selectedProperty().not());
        twoColor2ColorPicker.disableProperty().bind(twoColorRadioButton.selectedProperty().not());

        singleColorRadioButton.setToggleGroup(colorModeToggleGroup);
        colorSpectrumColorPicker.disableProperty().bind(singleColorRadioButton.selectedProperty().not());

        hsbWheelRadioButton.setToggleGroup(colorModeToggleGroup);
        colorMapRadioButton.setToggleGroup(colorModeToggleGroup);
        colorMapChoiceBox.disableProperty().bind(colorMapRadioButton.selectedProperty().not());

        twoColorRadioButton.selectedProperty().addListener(cl -> {
            twoColor1ColorPicker.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.TWO_COLOR_SPECTRUM,
                    twoColor1ColorPicker.getValue(), twoColor2ColorPicker.getValue()));
        });
        singleColorRadioButton.selectedProperty().addListener(cl -> {
            singleColorRadioButton.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.ONE_COLOR_SPECTRUM,
                    colorSpectrumColorPicker.getValue()));
        });
        hsbWheelRadioButton.selectedProperty().addListener(cl -> {
            hsbWheelRadioButton.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.HSB_WHEEL_SPECTRUM,
                    colorMapChoiceBox.getValue()));
        });

        colorMapRadioButton.selectedProperty().addListener(cl -> {
            colorMapRadioButton.getScene().getRoot().fireEvent(
                new ColorMapEvent(ColorMapEvent.PRESET_COLOR_PALETTE,
                    colorMapChoiceBox.getValue()));
        });
    }

    private void addHyperspaceListeners() {
        scene.getRoot().addEventHandler(HyperspaceEvent.NEW_MAX_ABS, e -> {
            maxAbsVal = (double) e.object;
            currentMaxAbsTextField.setText(String.valueOf(maxAbsVal));
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.NEW_MEANCENTEREDMAX_ABS, e -> {
            meanCenteredMaxAbsVal = (double) e.object;
            meanCenteredMaxAbsTextField.setText(String.valueOf(meanCenteredMaxAbsVal));
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS, e -> {
            enableCoordinateNotifications = false;
            CoordinateSet cs = (CoordinateSet) e.object;
            if (!cs.coordinateIndices.isEmpty())
                xFactorSpinner.getValueFactory().setValue(cs.coordinateIndices.get(0));
            if (cs.coordinateIndices.size() > 1)
                yFactorSpinner.getValueFactory().setValue(cs.coordinateIndices.get(1));
            if (cs.coordinateIndices.size() > 2)
                zFactorSpinner.getValueFactory().setValue(cs.coordinateIndices.get(2));
            if (cs.coordinateIndices.size() > 3)
                xDirectionFactorSpinner.getValueFactory().setValue(cs.coordinateIndices.get(3));
            if (cs.coordinateIndices.size() > 4)
                yDirectionFactorSpinner.getValueFactory().setValue(cs.coordinateIndices.get(4));
            if (cs.coordinateIndices.size() > 5)
                zDirectionFactorSpinner.getValueFactory().setValue(cs.coordinateIndices.get(5));
            enableCoordinateNotifications = true;
        });

        scene.getRoot().addEventHandler(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, e -> {
            point3dSize = (double) e.object;
            point3DSizeSpinner.getValueFactory().setValue(point3dSize);
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.POINT_SCALE_KEYPRESS, e -> {
            pointScale = (double) e.object;
            pointScaleSpinner.getValueFactory().setValue(pointScale);
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.SCATTERBUFF_SCALING_KEYPRESS, e -> {
            scatterBuffScaling = (double) e.object;
            scatterBuffScalingSpinner.getValueFactory().setValue(scatterBuffScaling);
        });
        scene.getRoot().addEventHandler(HyperspaceEvent.FACTOR_VECTORMAX_KEYPRESS, e -> {
            featureVectorMax = (int) e.object;
            featureVectorMaxSpinner.getValueFactory().setValue(featureVectorMax);
            //update position and direction sliders
            updateSpinnerMaxValues();
        });
    }

    private void updateSpinnerMaxValues() {
        //update position and direction spinners
        Integer newMax = (Integer) featureVectorMaxSpinner.getValue();
        Integer current = xFactorSpinner.getValue();
        xFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, newMax, current, 1));

        current = yFactorSpinner.getValue();
        yFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, newMax, current, 1));

        current = zFactorSpinner.getValue();
        zFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, newMax, current, 1));

        current = xDirectionFactorSpinner.getValue();
        xDirectionFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, newMax, current, 1));

        current = yDirectionFactorSpinner.getValue();
        yDirectionFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, newMax, current, 1));

        current = zDirectionFactorSpinner.getValue();
        zDirectionFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, newMax, current, 1));
    }

    private void setupDataBoundsSpinners() {
        featureVectorMaxSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1024, featureVectorMax, 16));
        featureVectorMaxSpinner.setEditable(true);
        //whenever the spinner value is changed...
        featureVectorMaxSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.FACTOR_VECTORMAX_GUI,
                    featureVectorMaxSpinner.getValue()));
            updateSpinnerMaxValues();
        });

        nodeQueueLimitSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(10, nodeQueueLimit * 10, nodeQueueLimit, 1000));
        nodeQueueLimitSpinner.setEditable(true);
        //whenever the spinner value is changed...
        nodeQueueLimitSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.NODE_QUEUELIMIT_GUI,
                    nodeQueueLimitSpinner.getValue()));
        });

        refreshRateSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, refreshRate * 10, refreshRate, 50));
        refreshRateSpinner.setEditable(true);
        //whenever the spinner value is changed...
        refreshRateSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.REFRESH_RATE_GUI,
                    ((Integer) refreshRateSpinner.getValue()).longValue()));
        });

        point3DSizeSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(1, point3dSize * 10, point3dSize, 5));
        point3DSizeSpinner.setEditable(true);
        //whenever the spinner value is changed...
        point3DSizeSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_GUI,
                    (Double) point3DSizeSpinner.getValue()));
        });

        pointScaleSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, pointScale * 10, pointScale, 0.1));
        pointScaleSpinner.setEditable(true);
        //whenever the spinner value is changed...
        pointScaleSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.POINT_SCALE_GUI,
                    (Double) pointScaleSpinner.getValue()));
        });

        scatterBuffScalingSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, scatterBuffScaling * 10, scatterBuffScaling, 0.1));
        scatterBuffScalingSpinner.setEditable(true);
        //whenever the spinner value is changed...
        scatterBuffScalingSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.SCATTERBUFF_SCALING_GUI,
                    (Double) scatterBuffScalingSpinner.getValue()));
        });
    }

    private void setupPositionSpinners() {
        xFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, featureVectorMax, 0, 1));
        xFactorSpinner.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications)
                updateHyperspaceFactors();
        });

        yFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, featureVectorMax, 1, 1));
        yFactorSpinner.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications)
                updateHyperspaceFactors();
        });

        zFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, featureVectorMax, 2, 1));
        zFactorSpinner.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
    }

    private void setupDirectionSpinners() {
        enableDirectionCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.ENABLE_DIRECTION_COORDINATES,
                    enableDirectionCheckBox.isSelected()));
        });

        xDirectionFactorSpinner.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());
        yDirectionFactorSpinner.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());
        zDirectionFactorSpinner.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());

        xDirectionFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, featureVectorMax, 3, 1));
        xDirectionFactorSpinner.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
        yDirectionFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, featureVectorMax, 4, 1));
        yDirectionFactorSpinner.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
        zDirectionFactorSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, featureVectorMax, 5, 1));
        zDirectionFactorSpinner.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
    }

    @FXML
    public void addDimensionLabel() {
        int size = dimensionLabelsListView.getItems().size();
        Dimension dimension = new Dimension("Factor", size, Color.ALICEBLUE);
        dimensionLabelsListView.getItems().add(new DimensionLabelItem(dimension));
        dimensionLabelsListView.getSelectionModel().selectLast();
        Dimension.addDimension(dimension);
        dimensionLabelsListView.getScene().getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.DIMENSION_LABEL_UPDATE, dimension));
    }

    @FXML
    public void removeDimensionLabel() {
        int selected = dimensionLabelsListView.getSelectionModel().getSelectedIndex();
        if (selected < 0) return;
        Dimension.removeDimension(selected);
        Dimension removed = dimensionLabelsListView.getItems().get(selected).dimension;
        dimensionLabelsListView.getItems().remove(selected);
        dimensionLabelsListView.getSelectionModel().clearAndSelect(selected - 1);
        dimensionLabelsScrollPane.setVvalue(1.0);
        dimensionLabelsListView.getScene().getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.DIMENSION_LABEL_REMOVED, removed));
    }

    @FXML
    public void clearAllDimensionLabels() {
        dimensionLabelsListView.getScene().getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.CLEARED_DIMENSION_LABELS));
        dimensionLabelsListView.getItems().clear();
    }

    @FXML
    public void exportLabels() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose LabelConfig file output...");
        fc.setInitialFileName("LabelConfig.json");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showSaveDialog(scene.getWindow());
        if (null != file) {
            LabelConfig lc = new LabelConfig();
            lc.setDimensionLabels(Dimension.getDimensionsAsStrings());
            HashMap<String, String> labelsHashMap = new HashMap<>();
            FactorLabel.globalLabelMap.forEach((s, f) -> {
                labelsHashMap.put(s, Utils.convertColorToString(f.getColor()));
            });
            lc.setLabels(labelsHashMap);
            lc.setClearAll(false);
            try {
                LabelConfigFile lcf = new LabelConfigFile(file.getAbsolutePath(), false);
                lcf.labelConfig = lc;
                lcf.writeContent();
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
            Platform.runLater(() -> {
                App.getAppScene().getRoot().fireEvent(
                    new CommandTerminalEvent("LabelConfig Serialized.",
                        new Font("Consolas", 20), Color.GREEN));
            });
        }
    }

    @FXML
    public void exportData() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose FeatureCollection file output...");
        fc.setInitialFileName("FeatureCollection.json");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showSaveDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            scene.getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.EXPORT_FEATURE_COLLECTION, file));
        }
    }

    @FXML
    public void resetMaxAbs() {
        scene.getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.RESET_MAX_ABS, 1.0));
        currentMaxAbsTextField.setText("1.0");
        meanCenteredMaxAbsTextField.setText("1.0");
    }

    @FXML
    public void recomputeMaxAbs() {
        scene.getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.RECOMPUTE_MAX_ABS));
    }

    @FXML
    public void clearLabels() {
        factorLabelListView.getItems().clear();
        featureLayerListView.getItems().clear();
        FactorLabel.removeAllFactorLabels(); //will fire event notifying scene
        FeatureLayer.removeAllFeatureLayers();//will fire event notifying scene
    }

    @FXML
    public void rescanLabels() {
        scene.getRoot().fireEvent(
            new FeatureVectorEvent(FeatureVectorEvent.RESCAN_FACTOR_LABELS));
    }

    @FXML
    public void updateHyperspaceFactors() {
        ArrayList<Integer> coordList = new ArrayList<>();
        coordList.add(Double.valueOf(xFactorSpinner.getValue()).intValue());
        coordList.add(Double.valueOf(yFactorSpinner.getValue()).intValue());
        coordList.add(Double.valueOf(zFactorSpinner.getValue()).intValue());
        coordList.add(Double.valueOf(xDirectionFactorSpinner.getValue()).intValue());
        coordList.add(Double.valueOf(yDirectionFactorSpinner.getValue()).intValue());
        coordList.add(Double.valueOf(zDirectionFactorSpinner.getValue()).intValue());
        CoordinateSet coords = new CoordinateSet(coordList);
        scene.getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_GUI, coords));
    }

    private void setupHyperspaceControls() {
        scalingToggleGroup = new ToggleGroup();
        autoNormalizeRadioButton.setToggleGroup(scalingToggleGroup);
        manualScalingRadioButton.setToggleGroup(scalingToggleGroup);
        scalingToggleGroup.selectedToggleProperty().addListener(cl -> {
            if (autoNormalizeRadioButton.isSelected())
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.SCALING_AUTO_NORMALIZE, true));
            else
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.SCALING_MANUAL_BOUNDS, true));
        });

        colorToggleGroup = new ToggleGroup();
        colorByLabelRadioButton.setToggleGroup(colorToggleGroup);
        colorByLayerRadioButton.setToggleGroup(colorToggleGroup);
        colorByGradientRadioButton.setToggleGroup(colorToggleGroup);
        colorByScoreRadioButton.setToggleGroup(colorToggleGroup);
        colorByPfaRadioButton.setToggleGroup(colorToggleGroup);

        colorToggleGroup.selectedToggleProperty().addListener(cl -> {
            if (colorByLabelRadioButton.isSelected())
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.COLOR_BY_LABEL, true));
            else if (colorByLayerRadioButton.isSelected())
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.COLOR_BY_LAYER, true));
            else if (colorByGradientRadioButton.isSelected())
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.COLOR_BY_GRADIENT, true));
            else if (colorByScoreRadioButton.isSelected())
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.COLOR_BY_SCORE, true));
            else if (colorByPfaRadioButton.isSelected())
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.COLOR_BY_PFA, true));
        });
        meanCenteredCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.SCALING_MEAN_CENTERED,
                meanCenteredCheckBox.isSelected()));
        });
        backgroundColorPicker.setValue(Color.BLACK);
        backgroundColorPicker.valueProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR,
                backgroundColorPicker.getValue()));
        });
        skyboxCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX,
                skyboxCheckBox.isSelected()));
        });
        featureDataCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ENABLE_FEATURE_DATA,
                featureDataCheckBox.isSelected()));
        });
        enableDirectionCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new HyperspaceEvent(
                HyperspaceEvent.ENABLE_FACTOR_DIRECTION,
                enableDirectionCheckBox.isSelected()));
        });
        toggleAllVisibleCheckBox.selectedProperty().addListener(cl -> {
            boolean visible = toggleAllVisibleCheckBox.isSelected();
            factorLabelListView.getItems().forEach(item -> {
                item.reactive = false;
                item.setDataVisible(visible);
                item.setEllipsoidVisible(visible);
                item.reactive = true;
            });
            FactorLabel.setAllVisible(visible);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.UPDATEDALL_FACTOR_LABELS,
                    FactorLabel.getFactorLabels().stream().toList()));
            });
        });
    }

    private void setupShadowCubeControls() {
        showAxesAndLabelsCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SHOW_AXES_LABELS,
                showAxesAndLabelsCheckBox.isSelected()));
        });
        overrideDomainTransformCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.OVERRIDE_DOMAIN_TRANSFORM,
                overrideDomainTransformCheckBox.isSelected()));
        });
        overrideXFormCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.OVERRIDE_XFORM,
                overrideXFormCheckBox.isSelected()));
        });

        showNearsidePointsCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SHOW_NEARSIDE_POINTS,
                showNearsidePointsCheckBox.isSelected()));
        });
        cubeWallsVisibleCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SET_CUBEWALLS_VISIBLE,
                cubeWallsVisibleCheckBox.isSelected()));
        });
        controlPointsVisibleCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SET_CONTROLPOINTS_VISIBLE,
                controlPointsVisibleCheckBox.isSelected()));
        });

        frameVisibleCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SET_FRAME_VISIBLE,
                frameVisibleCheckBox.isSelected()));
        });
        gridlinesVisibleCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SET_GRIDLINES_VISIBLE,
                gridlinesVisibleCheckBox.isSelected()));
        });
        cubeVisibleCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.SET_CUBE_VISIBLE,
                cubeVisibleCheckBox.isSelected()));
        });
        enableCubeRenderingCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new ShadowEvent(
                ShadowEvent.ENABLE_CUBE_PROJECTIONS,
                enableCubeRenderingCheckBox.isSelected()));
        });

        domainMinimumSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(-10.0, 10.0, -1.0, 0.1));
        domainMinimumSpinner.setEditable(true);
        //whenever the spinner value is changed...
        domainMinimumSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new ShadowEvent(ShadowEvent.SET_DOMAIN_MINIMUM,
                    (double) domainMinimumSpinner.getValue()));
        });
        domainMinimumSpinner.disableProperty().bind(
            overrideDomainTransformCheckBox.selectedProperty().not());

        domainMaximumSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(-10.0, 10.0, 1.0, 0.1));
        domainMaximumSpinner.setEditable(true);
        //whenever the spinner value is changed...
        domainMaximumSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new ShadowEvent(ShadowEvent.SET_DOMAIN_MAXIMUM,
                    (double) domainMaximumSpinner.getValue()));
        });
        domainMaximumSpinner.disableProperty().bind(
            overrideDomainTransformCheckBox.selectedProperty().not());

        pointScalingSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 2.0, 0.5, 0.1));
        pointScalingSpinner.setEditable(true);
        pointScalingSpinner.disableProperty().bind(
            overrideDomainTransformCheckBox.selectedProperty().not());
        //whenever the spinner value is changed...
        pointScalingSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new ShadowEvent(ShadowEvent.SET_POINT_SCALING,
                    (double) pointScalingSpinner.getValue()));
        });
    }

    private void setupMenuSystemControls() {
        itemSizeSlider.setValue(Double.parseDouble((String) config.configProps.get("ITEM_SIZE")));
        itemSizeLabel.setText(format.format(itemSizeSlider.getValue()));
        itemSizeSlider.valueProperty().addListener((ov, t, t1) -> {
            itemSizeLabel.setText(format.format(itemSizeSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_MENU_ITEM,
                t.doubleValue(), t1.doubleValue()));
        });

        innerRadiusSlider.setValue(Double.parseDouble((String) config.configProps.get("INNER_RADIUS")));
        innerRadiusLabel.setText(format.format(innerRadiusSlider.getValue()));
        innerRadiusSlider.valueProperty().addListener((ov, t, t1) -> {
            innerRadiusLabel.setText(format.format(innerRadiusSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_INNER_RADIUS,
                t.doubleValue(), t1.doubleValue()));
        });

        itemFitWidthSlider.setValue(Double.parseDouble((String) config.configProps.get("ITEM_FIT_WIDTH")));
        itemFitWidthLabel.setText(format.format(itemFitWidthSlider.getValue()));
        itemFitWidthSlider.valueProperty().addListener((ov, t, t1) -> {
            itemFitWidthLabel.setText(format.format(itemFitWidthSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_ITEM_FIT_WIDTH,
                t.doubleValue(), t1.doubleValue()));
        });

        menuSizeSlider.setValue(Double.parseDouble((String) config.configProps.get("MENU_RADIUS")));
        menuSizeLabel.setText(format.format(menuSizeSlider.getValue()));
        menuSizeSlider.valueProperty().addListener((ov, t, t1) -> {
            menuSizeLabel.setText(format.format(menuSizeSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_MENU_SIZE,
                t.doubleValue(), t1.doubleValue()));
        });

        offsetSlider.setValue(Double.parseDouble((String) config.configProps.get("OFFSET")));
        offsetLabel.setText(format.format(offsetSlider.getValue()));
        offsetSlider.valueProperty().addListener((ov, t, t1) -> {
            offsetLabel.setText(format.format(offsetSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_OFFSET,
                t.doubleValue(), t1.doubleValue()));
        });

        initialAngleSlider.setValue(Double.parseDouble((String) config.configProps.get("INITIAL_ANGLE")));
        initialAngleLabel.setText(format.format(initialAngleSlider.getValue()));
        initialAngleSlider.valueProperty().addListener((ov, t, t1) -> {
            initialAngleLabel.setText(format.format(initialAngleSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_INITIAL_ANGLE,
                t.doubleValue(), t1.doubleValue()));
        });

        strokeWidthSlider.setValue(Double.parseDouble((String) config.configProps.get("STROKE_WIDTH")));
        strokeWidthLabel.setText(format.format(strokeWidthSlider.getValue()));
        strokeWidthSlider.valueProperty().addListener((ov, t, t1) -> {
            strokeWidthLabel.setText(format.format(strokeWidthSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_STROKE_WIDTH,
                t.doubleValue(), t1.doubleValue()));
        });
        enableOrbitingCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new EffectEvent(
                EffectEvent.OPTICON_ENABLE_ORBITING, enableOrbitingCheckBox.isSelected()));
        });
        enableEmittersCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new EffectEvent(
                EffectEvent.ENABLE_EMITTERS, enableEmittersCheckBox.isSelected()));
        });
        enableEmptyVisionCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new EffectEvent(
                EffectEvent.ENABLE_EMPTY_VISION, enableEmptyVisionCheckBox.isSelected()));
        });
    }
}
