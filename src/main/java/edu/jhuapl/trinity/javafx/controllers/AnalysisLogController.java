/* Copyright (C) 2021 - 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Distance;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.messages.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.UmapConfig;
import edu.jhuapl.trinity.javafx.components.listviews.DistanceListItem;
import edu.jhuapl.trinity.javafx.components.listviews.ManifoldListItem;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
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
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;

/**
 * FXML Controller class
 *
 * @author Sean Phillips
 */
public class AnalysisLogController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(AnalysisLogController.class);

    @FXML
    private Node root;

    @FXML
    private TextField analysisFileTextField;
    @FXML
    private TextField umapConfigurationTextField;
    @FXML
    private ListView<String> dataSourcesListView;
    @FXML
    private TextArea notesTextArea;
    @FXML
    private ImageView sceneImageView;
    
    
    Scene scene;
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
        scene.addEventHandler(ManifoldEvent.NEWSNAPSHOT_PROJECTION_SCENE, e-> {
            WritableImage image = (WritableImage) e.object1;
            sceneImageView.setImage(image);
        });
        if (null != root) {
            root.addEventHandler(DragEvent.DRAG_OVER, event -> {
                event.acceptTransferModes(TransferMode.COPY);
            });
            root.addEventHandler(DragEvent.DRAG_DROPPED, e -> {
                Dragboard db = e.getDragboard();
                if (db.hasFiles()) {
                    File file = db.getFiles().get(0);
                    try {

                        if (AnalysisConfig.isAnalysisConfig(Files.readString(file.toPath()))) {
                            ObjectMapper mapper = new ObjectMapper();
                            AnalysisConfig acDC = mapper.readValue(file, AnalysisConfig.class);
                            setAnalysisConfig(acDC);
                        }
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
            });
            
//            root.addEventHandler(ManifoldEvent.NEW_ANALYSIS_CONFIG, event -> {
//                AnalysisConfig config = (AnalysisConfig) event.object1;
//                setAnalysisConfig(config);
//            });
            scene.addEventHandler(ManifoldEvent.NEW_UMAP_CONFIG, event -> {
                UmapConfig config = (UmapConfig) event.object1;
                setUmapConfig(config);
            });
            scene.addEventHandler(FeatureVectorEvent.NEW_FEATURES_SOURCE, event -> {
                 File file = (File) event.object;
                dataSourcesListView.getItems().add(file.getAbsolutePath());
            });
        }
        ImageView iv = ResourceUtils.loadIcon("data", 50);
        HBox placeholder = new HBox(10, iv, new Label("No Data Sources Marked"));
        placeholder.setAlignment(Pos.CENTER);
        dataSourcesListView.setPlaceholder(placeholder);
        
    }
    @FXML
    public void takeSnapshot() {
        Platform.runLater(()-> {
            sceneImageView.getScene().getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.TAKESNAPSHOT_PROJECTION_SCENE)          
            );
        });        
    }
    @FXML
    public void importAnalysisConfig(){
        FileChooser fc = new FileChooser();
        fc.setTitle("Import Existing Analysis Config File...");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showOpenDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            ObjectMapper mapper = new ObjectMapper();
            AnalysisConfig acDC;
            try {
                acDC = mapper.readValue(file, AnalysisConfig.class);
                setAnalysisConfig(acDC);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        }        
    }
    public void setAnalysisConfig(AnalysisConfig acDC) {
        analysisFileTextField.setText(acDC.getAnalysisName());
        clearAllSources();
        dataSourcesListView.getItems().addAll(acDC.getDataSources());
        notesTextArea.setText(acDC.getNotes());
        umapConfigurationTextField.setText(acDC.getUmapConfigFile());
    }
    @FXML
    public void clearAllSources() {
        dataSourcesListView.getItems().clear();
    }
    @FXML
    public void browseAnalysisConfig() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Browse location...");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showOpenDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            analysisFileTextField.setText(file.getAbsolutePath());
        }
    }
    @FXML
    public void browseUmapConfig() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Browse UMAP Configuration location...");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        File file = fc.showOpenDialog(scene.getWindow());
        if (null != file) {
            if (file.getParentFile().isDirectory())
                latestDir = file;
            umapConfigurationTextField.setText(file.getAbsolutePath());
        }
    }
    @FXML
    public void addDataSource() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Browse data file locations...");
        if (!latestDir.isDirectory())
            latestDir = new File(".");
        fc.setInitialDirectory(latestDir);
        List<File> files = fc.showOpenMultipleDialog(scene.getWindow());
        if (null != files) {
            if (files.get(0).getParentFile().isDirectory())
                latestDir = files.get(0);
            for(File file : files)
                dataSourcesListView.getItems().add(file.getAbsolutePath());
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
    @FXML
    public void clearAllNotes() {
        notesTextArea.clear();
    }

    public void setUmapConfig(UmapConfig uc) {

    }

    @FXML
    public void exportAnalysisConfig() {
        AnalysisConfig acDC = new AnalysisConfig();
        acDC.setAnalysisName(analysisFileTextField.getText());
        acDC.setNotes(notesTextArea.getText());
        acDC.setUmapConfigFile(umapConfigurationTextField.getText());
        acDC.setDataSources(dataSourcesListView.getItems());
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(acDC.getAnalysisName()), acDC);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }        
    }
}
