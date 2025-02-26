/* Copyright (C) 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageUrl;
import static edu.jhuapl.trinity.data.messages.EmbeddingsImageUrl.imageUrlFromFile;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.javafx.components.listviews.ImageFileListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;



/**
 * @author Sean Phillips
 */
public class HyperdrivePane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(HyperdrivePane.class);
    public static int PANE_WIDTH = 1200;
    public static int PANE_HEIGHT = 575;
    public BorderPane borderPane;
    private TilePane tilePane;
    BorderPane imageFilesBorderPane;
    BorderPane embeddingsBorderPane;
    StackPane imageFilesCenterStack;
    StackPane embeddingsCenterStack;
    ArrayList<FeatureVector> currentFeatureList;   
    ArrayList<File> imageFilesList;
    ListView<ImageFileListItem> imageFileListView;
    ListView<EmbeddingsImageListItem> embeddingsListView;
    Label imageFilesCountLabel;
    CircleProgressIndicator imageLoadingIndicator;    
    CircleProgressIndicator embeddingRequestIndicator;
        
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public HyperdrivePane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Hyperdrive", " Embeddings Service", 300.0, 400.0);
        currentFeatureList = new ArrayList<>();
        imageFilesList = new ArrayList<>();
        setBackground(Background.EMPTY);
        this.scene = scene;
        
        borderPane = (BorderPane) this.contentPane;
        tilePane = new TilePane();
        tilePane.setPrefColumns(2);
        tilePane.setHgap(10);
        tilePane.setAlignment(Pos.CENTER_LEFT);
        ScrollPane tileScrollPane = new ScrollPane(tilePane);
        tileScrollPane.setPannable(true);
        tileScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tileScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
//        tileScrollPane.setFitToHeight(true);
//        tileScrollPane.setFitToWidth(true);
        borderPane.setCenter(tileScrollPane);

        imageFilesCenterStack = new StackPane();
        imageFilesCenterStack.setAlignment(Pos.CENTER);
        imageFilesBorderPane = new BorderPane(imageFilesCenterStack);
        imageLoadingIndicator = new CircleProgressIndicator();
        imageLoadingIndicator.setLabelLater("...Working...");
        imageLoadingIndicator.defaultOpacity = 1.0;
        imageLoadingIndicator.setOpacity(0.0); ///instead of setVisible(false)

        imageFileListView = new ListView<>();
        ImageView iv = ResourceUtils.loadIcon("noimage", 50);
        HBox placeholder = new HBox(10, iv, new Label("No Files Loaded"));
        placeholder.setAlignment(Pos.CENTER);
        imageFileListView.setPlaceholder(placeholder);
        imageFilesCenterStack.getChildren().addAll(imageLoadingIndicator, imageFileListView);
        //Add controls to manipulate Image File ListView on the top
        Label imageFilesLabel = new Label("Total Image Files: ");
        imageFilesCountLabel = new Label("0");
        CheckBox renderIconsCheckBox = new CheckBox("Render Icons");
        renderIconsCheckBox.setSelected(true);
        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(e -> {
            imageLoadingIndicator.setFadeTimeMS(250);
            imageLoadingIndicator.setLabelLater("Loading images...");
            imageLoadingIndicator.fadeBusy(false);
            imageLoadingIndicator.spin(true);
            refreshImageFiles(renderIconsCheckBox.isSelected());
            imageLoadingIndicator.setLabelLater("Complete");
            imageLoadingIndicator.spin(false);
            imageLoadingIndicator.fadeBusy(true);
        });
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> {
            imageFileListView.getItems().clear();
        });

        HBox fileControlsBox = new HBox(10,
            imageFilesLabel, imageFilesCountLabel, renderIconsCheckBox, refreshButton, clearButton
        );
        fileControlsBox.setAlignment(Pos.CENTER);        
        imageFilesBorderPane.setTop(fileControlsBox);

        
        Button runFFTButton = new Button("Check Status");
        runFFTButton.setOnAction(e -> {
            RestAccessLayer.requestRestIsAlive(runFFTButton.getScene());
        });

        Button embeddingsButton = new Button("Request Embeddings");
        embeddingsButton.setOnAction(e -> {
            if(!imageFilesList.isEmpty()) {
                requestEmbeddingsTask();
            }
        });

        HBox powerBottomHBox = new HBox(10, embeddingsButton, runFFTButton);
        powerBottomHBox.setPadding(new Insets(10));
        powerBottomHBox.setAlignment(Pos.CENTER);
        imageFilesBorderPane.setBottom(powerBottomHBox);

        ImageView embeddingsPlaceholderIV = ResourceUtils.loadIcon("data", 50);
        HBox embeddingsPlaceholder = new HBox(10, embeddingsPlaceholderIV, new Label("No Data Sources Marked"));
        embeddingsPlaceholder.setAlignment(Pos.CENTER);
        embeddingsListView = new ListView<>();
        embeddingsListView.setPlaceholder(embeddingsPlaceholder);
        embeddingsCenterStack = new StackPane();
        embeddingsCenterStack.setAlignment(Pos.CENTER);
        embeddingsBorderPane = new BorderPane(embeddingsCenterStack);
        embeddingRequestIndicator = new CircleProgressIndicator();
        embeddingRequestIndicator.setLabelLater("...Working...");
        embeddingRequestIndicator.defaultOpacity = 1.0;
        embeddingRequestIndicator.setOpacity(0.0); ///instead of setVisible(false)
        embeddingsCenterStack.getChildren().addAll(embeddingRequestIndicator, embeddingsListView);
        
        //add controls to execute over embeddings to the bottom
        Button clearEmbeddingsButton = new Button("Clear embeddings");
        clearEmbeddingsButton.setOnAction(e -> {
            currentFeatureList.clear();
            embeddingsListView.getItems().clear();
        });
        clearEmbeddingsButton.setCancelButton(true);

        Button injectFeaturesButton = new Button("Inject Features");
        injectFeaturesButton.setOnAction(e -> {
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(currentFeatureList);
//            Platform.runLater(() -> {
//                scene.getRoot().fireEvent(new RestEvent(RestEvent.NEW_EMBEDDINGS_IMAGE, output, fvList));
//            });  
            
            injectFeaturesButton.getScene().getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
        });
        
//        Button clearFilesButton = new Button("Clear Files");
//        clearFilesButton.setOnAction(e -> {
//            imageFilesList.clear();
//        });
        HBox controlsBox = new HBox(10,
            clearEmbeddingsButton, injectFeaturesButton
        );
        controlsBox.setAlignment(Pos.CENTER);
        embeddingsBorderPane.setBottom(controlsBox);

        //Add controls to manipulate feature vector labels on the top
        Label labelLabel = new Label("Label");
        CheckBox overwriteCheckBox = new CheckBox("Overwrite");
        TextField batchLabelTextField = new TextField();
        batchLabelTextField.setPrefWidth(150);
        batchLabelTextField.setOnAction(e -> {
            updateLabels(labelLabel.getText(), true, overwriteCheckBox.isSelected());
        });
        batchLabelTextField.setTooltip(new Tooltip("Use this field to set FeatureVector Label fields in groups"));
        Button setSelectedButton = new Button("Selected");
        setSelectedButton.setPrefWidth(125);
        setSelectedButton.setOnAction(e -> {
            updateLabels(labelLabel.getText(), true, overwriteCheckBox.isSelected());
        });
        Button setAllButton = new Button("All");
        setAllButton.setPrefWidth(125);
        setAllButton.setOnAction(e -> {
            updateLabels(batchLabelTextField.getText(), false, overwriteCheckBox.isSelected());
        });

        HBox labelControlsBox = new HBox(10,
            labelLabel, batchLabelTextField, overwriteCheckBox, setSelectedButton, setAllButton
        );
        labelControlsBox.setAlignment(Pos.CENTER);        
        embeddingsBorderPane.setTop(labelControlsBox);

        imageFilesBorderPane.setPrefWidth(500);
        embeddingsBorderPane.setPrefWidth(600);
        tilePane.getChildren().addAll(imageFilesBorderPane, embeddingsBorderPane);

        borderPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        borderPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            event.consume();
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                loadImagesTask(db.getFiles());
            }
        });
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_IMAGE, event -> {
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            String msg = "Received " + output.getData().size() + " embeddings at " + LocalDateTime.now();
            embeddingRequestIndicator.setLabelLater(msg);
            System.out.println(msg);
            embeddingsListView.getItems().clear();
            currentFeatureList.clear();
            List<FeatureVector> fvList = (List<FeatureVector>) event.object2;
            List<EmbeddingsImageListItem> listItems = fvList.stream()
                .map(EmbeddingsImageListItem::new).toList();
            currentFeatureList.addAll(fvList);
            embeddingsListView.getItems().addAll(listItems);
            embeddingRequestIndicator.setLabelLater("Feature Vector List Obtained.");
            System.out.println("New Feature Vector List obtained.");
            embeddingRequestIndicator.spin(false);
            embeddingRequestIndicator.fadeBusy(true);            
        });
    }
    public void loadImagesTask(List<File> files) {
        
        Task loadTask = new Task() {
            @Override
            protected Void call() throws Exception {
                imageLoadingIndicator.setFadeTimeMS(250);
                imageLoadingIndicator.setLabelLater("Loading images...");
                imageLoadingIndicator.fadeBusy(false);
                imageLoadingIndicator.spin(true);
                
                currentFeatureList.clear();
                imageFilesList.clear();
                imageFilesList.addAll(files.stream()
                    .filter(f-> JavaFX3DUtils.isTextureFile(f))
                    .toList());
                imageFileListView.getItems().addAll(
                    imageFilesList.stream().map(itemFromFile).toList());
                Platform.runLater(()-> 
                    imageFilesCountLabel.setText(String.valueOf(imageFilesList.size()))
                );
                imageLoadingIndicator.setLabelLater("Complete");
                imageLoadingIndicator.spin(false);
                imageLoadingIndicator.fadeBusy(true);
                return null;
            }
        };
        Thread t = new Thread(loadTask, "Trinity Batch Image Load Task");
        t.setDaemon(true);
        t.start();
    }
    public void requestEmbeddingsTask() {
        Task requestTask = new Task() {
            @Override
            protected Void call() throws Exception {
                embeddingRequestIndicator.setLabelLater("Encoding Images...");
                embeddingRequestIndicator.spin(true);
                embeddingRequestIndicator.fadeBusy(false);
                System.out.println("Loading and Encoding Images...");
                long startTime = System.nanoTime();
                List<EmbeddingsImageUrl> inputs = 
                    imageFilesList.stream().map(imageUrlFromFile).toList();
                Utils.printTotalTime(startTime);

                EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
                input.setInput(inputs);
                input.setDimensions(512);
                input.setEmbedding_type("all");
                input.setEncoding_format("float");
                input.setModel("openai/clip-vit-large-patch14");
                input.setUser("string");
                try {
                    embeddingRequestIndicator.setLabelLater("Requesting Embeddings...");
                    System.out.println("Sending " + inputs.size() + " images for processing at " + LocalDateTime.now());
                    RestAccessLayer.requestImageEmbeddings(imageFilesList, input, embeddingRequestIndicator.getScene());
                } catch (JsonProcessingException ex) {
                    java.util.logging.Logger.getLogger(HyperdrivePane.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        };
        Thread t = new Thread(requestTask, "Trinity Embeddings Image Request");
        t.setDaemon(true);
        t.start();
    }
    private void refreshImageFiles(boolean renderIcons) {
        imageFileListView.getItems().clear();
        imageFileListView.getItems().addAll(
            imageFilesList.stream().map(t -> new ImageFileListItem(t, renderIcons)).toList());
    }
    public void updateLabels(String labelText, boolean onlySelected, boolean overwrite) {
        if(onlySelected && overwrite) {
            embeddingsListView.getSelectionModel().getSelectedItems().forEach(i -> 
                i.setFeatureVectorLabel(labelText));
        } else if(onlySelected && !overwrite) {
            embeddingsListView.getSelectionModel().getSelectedItems()
                .filtered(t -> t.getFeatureVectorLabel().isBlank())
                .forEach(i -> i.setFeatureVectorLabel(labelText));
        } else if(!onlySelected && overwrite) {
            embeddingsListView.getItems().forEach(i -> 
                i.setFeatureVectorLabel(labelText));
        } else {
            embeddingsListView.getItems()
                .filtered(t -> t.getFeatureVectorLabel().isBlank())
                .forEach(i -> i.setFeatureVectorLabel(labelText));
        }
    }
    
    public static Function<File, ImageFileListItem> itemFromFile = file -> {
        return new ImageFileListItem(file);
    };
}
