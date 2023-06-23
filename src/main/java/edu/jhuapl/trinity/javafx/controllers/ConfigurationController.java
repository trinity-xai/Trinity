package edu.jhuapl.trinity.javafx.controllers;

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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.components.FactorLabelListItem;
import edu.jhuapl.trinity.javafx.components.FeatureLayerListItem;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.RadialEntityEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.utils.Configuration;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
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
public class ConfigurationController implements Initializable {

    @FXML
    private CheckBox enableEmittersCheckBox;
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

    @FXML
    private CheckBox lockedCheckBox;
    @FXML
    private Slider lockedSlider;
    @FXML
    private Slider xFactorSlider;
    @FXML
    private Slider yFactorSlider;
    @FXML
    private Slider zFactorSlider;
    @FXML
    private CheckBox enableDirectionCheckBox;
    @FXML
    private CheckBox lockedDirectionCheckBox;
    @FXML
    private Slider lockedDirectionSlider;
    @FXML
    private Slider xDirectionFactorSlider;
    @FXML
    private Slider yDirectionFactorSlider;
    @FXML
    private Slider zDirectionFactorSlider;
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
        setupPositionSliders();
        setupDirectionSliders();

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
            if (!cs.coordinateIndices.isEmpty()) {
                xFactorSlider.setValue(cs.coordinateIndices.get(0));
                lockedSlider.setValue(cs.coordinateIndices.get(0));
            }
            if (cs.coordinateIndices.size() > 1)
                yFactorSlider.setValue(cs.coordinateIndices.get(1));
            if (cs.coordinateIndices.size() > 2)
                zFactorSlider.setValue(cs.coordinateIndices.get(2));
            if (cs.coordinateIndices.size() > 3) {
                xDirectionFactorSlider.setValue(cs.coordinateIndices.get(3));
                lockedDirectionSlider.setValue(cs.coordinateIndices.get(3));
            }
            if (cs.coordinateIndices.size() > 4)
                yDirectionFactorSlider.setValue(cs.coordinateIndices.get(4));
            if (cs.coordinateIndices.size() > 5)
                zDirectionFactorSlider.setValue(cs.coordinateIndices.get(5));
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
            //update position sliders
            lockedSlider.setMax((int) featureVectorMaxSpinner.getValue());
            xFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            yFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            zFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            xDirectionFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            yDirectionFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            zDirectionFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
        });
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
            //update position sliders
            lockedSlider.setMax((int) featureVectorMaxSpinner.getValue());
            xFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            yFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            zFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            xDirectionFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            yDirectionFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
            zDirectionFactorSlider.setMax((int) featureVectorMaxSpinner.getValue());
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

    private void setupPositionSliders() {
        lockedSlider.disableProperty().bind(lockedCheckBox.selectedProperty().not());
        xFactorSlider.mouseTransparentProperty().bind(lockedCheckBox.selectedProperty());
        yFactorSlider.mouseTransparentProperty().bind(lockedCheckBox.selectedProperty());
        zFactorSlider.mouseTransparentProperty().bind(lockedCheckBox.selectedProperty());

        lockedSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (enableCoordinateNotifications) {
                if (newVal.intValue() > lockedValue) {
                    xFactorSlider.setValue(xFactorSlider.getValue() + 1.0);
                    yFactorSlider.setValue(yFactorSlider.getValue() + 1.0);
                    zFactorSlider.setValue(zFactorSlider.getValue() + 1.0);
                    lockedValue = newVal.intValue();
                } else if (newVal.intValue() < lockedValue) {
                    xFactorSlider.setValue(xFactorSlider.getValue() - 1.0);
                    yFactorSlider.setValue(yFactorSlider.getValue() - 1.0);
                    zFactorSlider.setValue(zFactorSlider.getValue() - 1.0);
                    lockedValue = newVal.intValue();
                }
                lockedSlider.setValue(newVal.intValue());
                updateHyperspaceFactors();
            }
        });
        xFactorSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (!lockedCheckBox.isSelected() && enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
        yFactorSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (!lockedCheckBox.isSelected() && enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
        zFactorSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (!lockedCheckBox.isSelected() && enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
    }

    private void setupDirectionSliders() {
        enableDirectionCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.ENABLE_DIRECTION_COORDINATES,
                    enableDirectionCheckBox.isSelected()));
        });
        lockedDirectionSlider.disableProperty().bind(lockedDirectionCheckBox.selectedProperty().not()
            .or(enableDirectionCheckBox.selectedProperty().not()));
        xDirectionFactorSlider.mouseTransparentProperty().bind(lockedDirectionCheckBox.selectedProperty());
        yDirectionFactorSlider.mouseTransparentProperty().bind(lockedDirectionCheckBox.selectedProperty());
        zDirectionFactorSlider.mouseTransparentProperty().bind(lockedDirectionCheckBox.selectedProperty());

        lockedDirectionCheckBox.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());
        xDirectionFactorSlider.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());
        yDirectionFactorSlider.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());
        zDirectionFactorSlider.disableProperty().bind(enableDirectionCheckBox.selectedProperty().not());


        lockedDirectionSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (newVal.intValue() > lockedDirectionValue) {
                xDirectionFactorSlider.setValue(xDirectionFactorSlider.getValue() + 1.0);
                yDirectionFactorSlider.setValue(yDirectionFactorSlider.getValue() + 1.0);
                zDirectionFactorSlider.setValue(zDirectionFactorSlider.getValue() + 1.0);
                lockedDirectionValue = newVal.intValue();
            } else if (newVal.intValue() < lockedDirectionValue) {
                xDirectionFactorSlider.setValue(xDirectionFactorSlider.getValue() - 1.0);
                yDirectionFactorSlider.setValue(yDirectionFactorSlider.getValue() - 1.0);
                zDirectionFactorSlider.setValue(zDirectionFactorSlider.getValue() - 1.0);
                lockedDirectionValue = newVal.intValue();
            }
            lockedDirectionSlider.setValue(newVal.intValue());
            updateHyperspaceFactors();
        });
        xDirectionFactorSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (!lockedDirectionCheckBox.isSelected() && enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
        yDirectionFactorSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (!lockedDirectionCheckBox.isSelected() && enableCoordinateNotifications)
                updateHyperspaceFactors();
        });
        zDirectionFactorSlider.valueProperty().addListener((obs, oldval, newVal) -> {
            if (!lockedDirectionCheckBox.isSelected() && enableCoordinateNotifications)
                updateHyperspaceFactors();
        });

    }

    @FXML
    public void exportData() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose FeatureCollection file output...");
        fc.setInitialFileName("FeatureCollection.json");
        fc.setInitialDirectory(latestDir);
        File file = fc.showSaveDialog(scene.getWindow());
        if (null != file) {
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
//        scene.getRoot().fireEvent(
//            new FeatureVectorEvent(FeatureVectorEvent.RESCAN_FEATURE_LAYERS));
    }

    @FXML
    public void updateHyperspaceFactors() {
        ArrayList<Integer> coordList = new ArrayList<>();
        coordList.add(Double.valueOf(xFactorSlider.getValue()).intValue());
        coordList.add(Double.valueOf(yFactorSlider.getValue()).intValue());
        coordList.add(Double.valueOf(zFactorSlider.getValue()).intValue());
        coordList.add(Double.valueOf(xDirectionFactorSlider.getValue()).intValue());
        coordList.add(Double.valueOf(yDirectionFactorSlider.getValue()).intValue());
        coordList.add(Double.valueOf(zDirectionFactorSlider.getValue()).intValue());
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
        itemSizeSlider.setValue(Double.valueOf((String) config.configProps.get("ITEM_SIZE")));
        itemSizeLabel.setText(format.format(itemSizeSlider.getValue()));
        itemSizeSlider.valueProperty().addListener((ov, t, t1) -> {
            itemSizeLabel.setText(format.format(itemSizeSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_MENU_ITEM,
                t.doubleValue(), t1.doubleValue()));
        });

        innerRadiusSlider.setValue(Double.valueOf((String) config.configProps.get("INNER_RADIUS")));
        innerRadiusLabel.setText(format.format(innerRadiusSlider.getValue()));
        innerRadiusSlider.valueProperty().addListener((ov, t, t1) -> {
            innerRadiusLabel.setText(format.format(innerRadiusSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_INNER_RADIUS,
                t.doubleValue(), t1.doubleValue()));
        });

        itemFitWidthSlider.setValue(Double.valueOf((String) config.configProps.get("ITEM_FIT_WIDTH")));
        itemFitWidthLabel.setText(format.format(itemFitWidthSlider.getValue()));
        itemFitWidthSlider.valueProperty().addListener((ov, t, t1) -> {
            itemFitWidthLabel.setText(format.format(itemFitWidthSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_ITEM_FIT_WIDTH,
                t.doubleValue(), t1.doubleValue()));
        });

        menuSizeSlider.setValue(Double.valueOf((String) config.configProps.get("MENU_RADIUS")));
        menuSizeLabel.setText(format.format(menuSizeSlider.getValue()));
        menuSizeSlider.valueProperty().addListener((ov, t, t1) -> {
            menuSizeLabel.setText(format.format(menuSizeSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_MENU_SIZE,
                t.doubleValue(), t1.doubleValue()));
        });

        offsetSlider.setValue(Double.valueOf((String) config.configProps.get("OFFSET")));
        offsetLabel.setText(format.format(offsetSlider.getValue()));
        offsetSlider.valueProperty().addListener((ov, t, t1) -> {
            offsetLabel.setText(format.format(offsetSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_OFFSET,
                t.doubleValue(), t1.doubleValue()));
        });

        initialAngleSlider.setValue(Double.valueOf((String) config.configProps.get("INITIAL_ANGLE")));
        initialAngleLabel.setText(format.format(initialAngleSlider.getValue()));
        initialAngleSlider.valueProperty().addListener((ov, t, t1) -> {
            initialAngleLabel.setText(format.format(initialAngleSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_INITIAL_ANGLE,
                t.doubleValue(), t1.doubleValue()));
        });

        strokeWidthSlider.setValue(Double.valueOf((String) config.configProps.get("STROKE_WIDTH")));
        strokeWidthLabel.setText(format.format(strokeWidthSlider.getValue()));
        strokeWidthSlider.valueProperty().addListener((ov, t, t1) -> {
            strokeWidthLabel.setText(format.format(strokeWidthSlider.getValue()));
            scene.getRoot().fireEvent(new RadialEntityEvent(
                RadialEntityEvent.RADIAL_ENTITY_STROKE_WIDTH,
                t.doubleValue(), t1.doubleValue()));
        });

        enableEmittersCheckBox.selectedProperty().addListener(cl -> {
            scene.getRoot().fireEvent(new EffectEvent(
                EffectEvent.ENABLE_EMITTERS, enableEmittersCheckBox.isSelected()));
        });
    }
}
