/* Copyright (C) 2025 Sean Phillips */
package edu.jhuapl.trinity.javafx.components.panes;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.llm.AiModel;
import edu.jhuapl.trinity.data.messages.llm.AliveModels;
import edu.jhuapl.trinity.data.messages.llm.ChatCaptionResponse;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsInput;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsInput.CAPTION_TYPE;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageData;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;
import edu.jhuapl.trinity.data.messages.llm.Prompts;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import static edu.jhuapl.trinity.data.messages.xai.FeatureVector.mapToStateArray;
import edu.jhuapl.trinity.javafx.components.CaptionChooserBox;
import edu.jhuapl.trinity.javafx.components.listviews.LandmarkTextBuilderBox;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import static edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem.itemFromFile;
import static edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem.itemNoRenderFromFile;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.listviews.LandmarkImageBuilderBox;
import edu.jhuapl.trinity.javafx.components.listviews.LandmarkListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.messages.EmbeddingsImageCallback.STATUS;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import static edu.jhuapl.trinity.messages.RestAccessLayer.currentChatModel;
import static edu.jhuapl.trinity.messages.RestAccessLayer.currentEmbeddingsModel;
import static edu.jhuapl.trinity.messages.RestAccessLayer.stringToChatCaptionResponse;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import edu.jhuapl.trinity.utils.metric.Metric;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

/**
 * @author Sean Phillips
 */
public class HyperdrivePane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(HyperdrivePane.class);
    public static int PANE_WIDTH = 1200;
    public static int PANE_HEIGHT = 575;
    Image waitingImage;     
    public BorderPane borderPane;
    HBox imageryTabHBox;
    HBox textTabHBox;
    Image baseImage;
    ImageView baseImageView;    
    BorderPane embeddingsBorderPane;
    StackPane embeddingsCenterStack;
    TextArea baseTextArea;
    
    TabPane tabPane;
    Tab imageryEmbeddingsTab;
    Tab textEmbeddingsTab;    
    Tab similarityTab;
    Tab servicesTab;
    CheckBox renderIconsCheckBox;
    
    ArrayList<FeatureVector> currentFeatureList;  
    ArrayList<FeatureVector> currentTextFeatureList;  
    ArrayList<File> imageFilesList;
    ArrayList<File> textFilesList;
    ListView<EmbeddingsImageListItem> imageEmbeddingsListView;
    ListView<EmbeddingsTextListItem> textEmbeddingsListView;
    Label imageFilesCountLabel;
    CircleProgressIndicator textEmbeddingRequestIndicator;
    CircleProgressIndicator imageEmbeddingRequestIndicator;
    LandmarkTextBuilderBox landmarkTextBuilderBox;
    LandmarkImageBuilderBox landmarkImageBuilderBox;    
    ChoiceBox<String> metricChoiceBox;
    AtomicInteger requestNumber;
    HashMap<Integer, STATUS> outstandingRequests;    
    int batchSize = 1;
    long requestDelay = 25;
    DateTimeFormatter format; 
    
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public HyperdrivePane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Hyperdrive", " Embeddings Service", 300.0, 400.0);
        this.scene = scene;
        format = DateTimeFormatter.ofPattern("HH.mm.ss");
        currentFeatureList = new ArrayList<>();
        currentTextFeatureList = new ArrayList<>();
        imageFilesList = new ArrayList<>();
        textFilesList = new ArrayList<>();
        
        outstandingRequests = new HashMap<>();
        requestNumber = new AtomicInteger();
        waitingImage = ResourceUtils.loadIconFile("waitingforimage");
        setBackground(Background.EMPTY);
        //container for the floating window itself
        borderPane = (BorderPane) this.contentPane;
        imageryEmbeddingsTab = new Tab("Imagery");
        textEmbeddingsTab = new Tab("Text");
        similarityTab = new Tab("Similarity");
        servicesTab = new Tab("Services");
        
        tabPane = new TabPane(imageryEmbeddingsTab, textEmbeddingsTab, similarityTab, servicesTab);
        tabPane.setPadding(Insets.EMPTY);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        borderPane.setCenter(tabPane);

        textTabHBox = new HBox(20);
        textTabHBox.setAlignment(Pos.CENTER);
        textEmbeddingsTab.setContent(textTabHBox);

        ImageView textEmbeddingsPlaceholderIV = ResourceUtils.loadIcon("console", 50);
        HBox textEmbeddingsPlaceholder = new HBox(10, textEmbeddingsPlaceholderIV, new Label("No Data Sources Marked"));
        textEmbeddingsPlaceholder.setAlignment(Pos.CENTER);
        textEmbeddingsListView = new ListView<>();
        textEmbeddingsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        textEmbeddingsListView.setPlaceholder(textEmbeddingsPlaceholder);

        BorderPane textEmbeddingsBorderPane = new BorderPane(textEmbeddingsListView);
        textEmbeddingsBorderPane.setPrefWidth(600);

        Button getTextEmbeddingsButton = new Button("Request Embeddings");
        getTextEmbeddingsButton.setOnAction(e -> {
            if(!textEmbeddingsListView.getItems().isEmpty()) {
                requestTextEmbeddingsTask();
            }
        });        
        //add controls to execute over embeddings to the bottom
        Button clearTextEmbeddingsButton = new Button("Clear embeddings");
        clearTextEmbeddingsButton.setOnAction(e -> {
            currentFeatureList.clear();
            textEmbeddingsListView.getItems().clear();
        });
        clearTextEmbeddingsButton.setCancelButton(true);

        Button injectTextFeaturesButton = new Button("Inject Features");
        injectTextFeaturesButton.setOnAction(e -> {
             currentFeatureList.clear();
             currentFeatureList.addAll(textEmbeddingsListView.getItems().stream()
                .map(EmbeddingsTextListItem::getFeatureVector).toList());
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(currentFeatureList);
            injectTextFeaturesButton.getScene().getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
        });
        
        HBox textControlsBox = new HBox(10,
            getTextEmbeddingsButton, clearTextEmbeddingsButton, injectTextFeaturesButton
        );
        textControlsBox.setAlignment(Pos.CENTER);
        textEmbeddingsBorderPane.setBottom(textControlsBox);
        
        textEmbeddingRequestIndicator = new CircleProgressIndicator();
        ProgressStatus ps = new ProgressStatus("Working",0.5);
        ps.fillStartColor = Color.AZURE;
        ps.fillEndColor = Color.LIME;
        ps.innerStrokeColor = Color.AZURE;
        ps.outerStrokeColor = Color.LIME;
        textEmbeddingRequestIndicator.updateStatus(ps);
        textEmbeddingRequestIndicator.defaultOpacity = 1.0;
        textEmbeddingRequestIndicator.setOpacity(0.0); ///instead of setVisible(false)
        baseTextArea = new TextArea();
        baseTextArea.setPrefWidth(512);
        baseTextArea.setPrefHeight(512);
        StackPane baseTextStackPane = new StackPane(baseTextArea, textEmbeddingRequestIndicator);
        textTabHBox.getChildren().addAll(textEmbeddingsBorderPane, baseTextStackPane);
        
        
///////////////////////////////////////////////////////////////        
        imageryTabHBox = new HBox(20);
        imageryTabHBox.setAlignment(Pos.CENTER);
        imageryEmbeddingsTab.setContent(imageryTabHBox);

        //Add controls to manipulate Image File ListView on the top
        Label imageFilesLabel = new Label("Total Image Files: ");
        imageFilesCountLabel = new Label("0");
        renderIconsCheckBox = new CheckBox("Render Icons");
        renderIconsCheckBox.setSelected(true);

        HBox fileControlsBox = new HBox(10,
            imageFilesLabel, imageFilesCountLabel, renderIconsCheckBox
        );
        fileControlsBox.setAlignment(Pos.CENTER);        

        Button embeddingsButton = new Button("Request Embeddings");
        embeddingsButton.setOnAction(e -> {
            if(!imageEmbeddingsListView.getItems().isEmpty()) {
                requestEmbeddingsTask();
            }
        });

        ImageView embeddingsPlaceholderIV = ResourceUtils.loadIcon("data", 50);
        HBox embeddingsPlaceholder = new HBox(10, embeddingsPlaceholderIV, new Label("No Data Sources Marked"));
        embeddingsPlaceholder.setAlignment(Pos.CENTER);
        imageEmbeddingsListView = new ListView<>();
        imageEmbeddingsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        imageEmbeddingsListView.setPlaceholder(embeddingsPlaceholder);
        imageEmbeddingsListView.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            if(!imageEmbeddingsListView.getSelectionModel().isEmpty()) {
                baseImage = imageEmbeddingsListView.getSelectionModel()
                    .getSelectedItems().get(0).getCurrentImage();
                baseImageView.setImage(baseImage);
            }
        });
        
        MenuItem selectAllMenuItem = new MenuItem("Select All");
        selectAllMenuItem.setOnAction(e -> 
            imageEmbeddingsListView.getSelectionModel().selectAll());        

        MenuItem setCaptionItem = new MenuItem("Set Label");
        setCaptionItem.setOnAction(e -> {
            if(!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
                TextInputDialog td = new TextInputDialog("enter any text"); 
                td.setHeaderText("Manually set Label for " 
                    + imageEmbeddingsListView.getSelectionModel().getSelectedItems().size()
                    + " items.");
                td.setGraphic(ResourceUtils.loadIcon("console", 75));
                td.initStyle(StageStyle.TRANSPARENT);
                DialogPane dialogPane = td.getDialogPane();
                dialogPane.setBackground(Background.EMPTY);
                dialogPane.getScene().setFill(Color.TRANSPARENT);
                String DIALOGCSS = this.getClass().getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
                dialogPane.getStylesheets().add(DIALOGCSS);
                Optional<String> captionOptional = td.showAndWait();
                if(captionOptional.isPresent()) {
                    imageEmbeddingsListView.getSelectionModel().getSelectedItems()
                        .forEach(i -> i.setFeatureVectorLabel(captionOptional.get()));
                }
            }
        });

        MenuItem requestCaptionItem = new MenuItem("Request Label/Captions");
        requestCaptionItem.setOnAction(e -> {
            if(!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty())
                requestCaptionsTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems());
        });
        MenuItem chooseCaptionItem = new MenuItem("Auto-choose Caption");
        chooseCaptionItem.setOnAction(e -> {
            if(!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
                imageEmbeddingRequestIndicator.setLabelLater("Choose Captions...");
                imageEmbeddingRequestIndicator.spin(true);
                imageEmbeddingRequestIndicator.fadeBusy(false);
                System.out.println("Prompting User for Labels...");
                Platform.runLater(()-> {
                    CaptionChooserBox box = new CaptionChooserBox();
                    box.setChoices(landmarkTextBuilderBox.getChoices());
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION); 
                    alert.setHeaderText("Add Labels for model to choose from");
                    alert.setGraphic(ResourceUtils.loadIcon("console", 75));
                    alert.initStyle(StageStyle.TRANSPARENT);
                    DialogPane dialogPane = alert.getDialogPane();
                    dialogPane.setBackground(Background.EMPTY);
                    dialogPane.getScene().setFill(Color.TRANSPARENT);
                    dialogPane.setContent(box);
                    String DIALOGCSS = HyperdrivePane.class.getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
                    dialogPane.getStylesheets().add(DIALOGCSS);
                    Optional<ButtonType> captionOptional = alert.showAndWait();
                    if(captionOptional.get() == ButtonType.OK) {
                        System.out.println("Choices from user: " + box.getChoices());
                        landmarkTextBuilderBox.setChoices(box.getChoices());
                        if(!box.getChoices().isEmpty()){
                            chooseCaptionsTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems(),
                                box.getChoices());                        
                        }
                    }
                });
            }
        });
        
        MenuItem textLandmarkCaptionItem = new MenuItem("Label by Text Landmark Similarity");
        textLandmarkCaptionItem.setOnAction(e -> {
            if(!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty())
                requestLandmarkSimilarityTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems()
                    , landmarkTextBuilderBox.getItems().stream()
                        .map(LandmarkListItem::getFeatureVector).toList());
        });
        MenuItem imageLandmarkCaptionItem = new MenuItem("Label by Image Landmark Similarity");
        imageLandmarkCaptionItem.setOnAction(e -> {
            if(!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
                requestLandmarkSimilarityTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems(), 
                    landmarkImageBuilderBox.getItems().stream()
                        .map(LandmarkListItem::getFeatureVector).toList()
                );
            }
        });
        
        
        MenuItem clearRequestsItem = new MenuItem("Clear Requests");
        clearRequestsItem.setOnAction(e -> {
            outstandingRequests.clear();
            imageEmbeddingRequestIndicator.spin(false);
            imageEmbeddingRequestIndicator.fadeBusy(true);            
        });
        ContextMenu embeddingsContextMenu = 
            new ContextMenu(selectAllMenuItem, setCaptionItem, requestCaptionItem, 
                chooseCaptionItem, textLandmarkCaptionItem, imageLandmarkCaptionItem,
                clearRequestsItem);
        imageEmbeddingsListView.setContextMenu(embeddingsContextMenu);

        embeddingsCenterStack = new StackPane();
        embeddingsCenterStack.setAlignment(Pos.CENTER);
        embeddingsBorderPane = new BorderPane(embeddingsCenterStack);
        imageEmbeddingRequestIndicator = new CircleProgressIndicator();
//        ProgressStatus ps = new ProgressStatus("Working",0.5);
//        ps.fillStartColor = Color.AZURE;
//        ps.fillEndColor = Color.LIME;
//        ps.innerStrokeColor = Color.AZURE;
//        ps.outerStrokeColor = Color.LIME;
        imageEmbeddingRequestIndicator.updateStatus(ps);
        imageEmbeddingRequestIndicator.defaultOpacity = 1.0;
        imageEmbeddingRequestIndicator.setOpacity(0.0); ///instead of setVisible(false)

        embeddingsCenterStack.getChildren().addAll(imageEmbeddingsListView);
        
        //add controls to execute over embeddings to the bottom
        Button clearEmbeddingsButton = new Button("Clear embeddings");
        clearEmbeddingsButton.setOnAction(e -> {
            currentFeatureList.clear();
            imageEmbeddingsListView.getItems().clear();
        });
        clearEmbeddingsButton.setCancelButton(true);

        Button injectFeaturesButton = new Button("Inject Features");
        injectFeaturesButton.setOnAction(e -> {
             currentFeatureList.clear();
             currentFeatureList.addAll(imageEmbeddingsListView.getItems().stream()
                .map(EmbeddingsImageListItem::getFeatureVector).toList());
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(currentFeatureList);
            injectFeaturesButton.getScene().getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
        });
        
        HBox controlsBox = new HBox(10,
            embeddingsButton, clearEmbeddingsButton, injectFeaturesButton
        );
        controlsBox.setAlignment(Pos.CENTER);
        embeddingsBorderPane.setBottom(controlsBox);
        embeddingsBorderPane.setTop(new VBox(5, fileControlsBox));
        
        baseImage = waitingImage;
        baseImageView = new ImageView(baseImage);
        baseImageView.setPreserveRatio(true);
        baseImageView.setFitWidth(512);
        baseImageView.setFitHeight(512);
        ScrollPane baseImageScrollPane = new ScrollPane(baseImageView);
        baseImageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        baseImageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        baseImageScrollPane.setPannable(true);
        baseImageScrollPane.setFitToHeight(true);
        baseImageScrollPane.setFitToWidth(true);
        baseImageScrollPane.setPrefSize(512, 512);
        StackPane baseImageStackPane = new StackPane(baseImageScrollPane, imageEmbeddingRequestIndicator);
        
        MenuItem captionMenuItem = new MenuItem("Caption");
        captionMenuItem.setOnAction(e-> {
            ChatCompletionsInput input;
            try {
                input = ChatCompletionsInput.defaultImageInput(baseImage);
                if(null != currentChatModel)
                    input.setModel(currentChatModel);
                RestAccessLayer.requestChatCompletion(input, baseImageScrollPane.getScene(), 666, 9001); 
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });
        ContextMenu baseImageContextMenu = new ContextMenu(captionMenuItem);
        baseImageScrollPane.setContextMenu(baseImageContextMenu);
        baseImageScrollPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        baseImageScrollPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            event.consume();
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                final File file = db.getFiles().get(0);
                if(ResourceUtils.isTextFile(file)){
                    System.out.println("its text!!");
                }
                if (JavaFX3DUtils.isTextureFile(file)) {
                    try {
                        baseImage = new Image(file.toURI().toURL().toExternalForm());
                        baseImageView.setImage(baseImage);
                        baseImageScrollPane.setHvalue(0);
                        baseImageScrollPane.setVvalue(0);
                    } catch (MalformedURLException ex) {
                        LOG.error(null, ex);
                    }
                }
            }
        });
        embeddingsBorderPane.setPrefWidth(600);
        imageryTabHBox.getChildren().addAll(embeddingsBorderPane, baseImageStackPane);

        //Landmarks And Similarity Tab ////////////////////////////////////////////////
        landmarkTextBuilderBox = new LandmarkTextBuilderBox();
        metricChoiceBox = new ChoiceBox<>();
        metricChoiceBox.getItems().addAll(Metric.getMetricNames());
        int defaultSelection = metricChoiceBox.getItems().indexOf("cosine");
        if(defaultSelection >= 0)
            metricChoiceBox.getSelectionModel().select(defaultSelection);
        else
            metricChoiceBox.getSelectionModel().selectFirst();

        landmarkImageBuilderBox = new LandmarkImageBuilderBox();        
        
        //Controls to choose metric and refresh current landmark embeddings
        Button refreshLandmarkEmbeddingsButton = new Button("Refresh Embeddings");
        refreshLandmarkEmbeddingsButton.setOnAction(e -> {
            landmarkTextBuilderBox.getItems().stream()
                .filter(i -> !i.getFeatureVectorLabel().isBlank())
                .forEach(item -> {
                try {
                    EmbeddingsImageInput input = EmbeddingsImageInput.defaultTextInput(item.getFeatureVectorLabel());
                    if(null != currentEmbeddingsModel)
                        input.setModel(currentEmbeddingsModel);
                    List<Integer> inputIDs = new ArrayList<>();
                    inputIDs.add(item.landmarkID);
                    RestAccessLayer.requestTextEmbeddings(
                        input, scene, inputIDs, requestNumber.getAndIncrement());
                } catch (IOException ex) {
                    LOG.error(null, ex);
                }
            });
        }); 
        Button refreshImageLandmarkEmbeddingsButton = new Button("Refresh Embeddings");
        refreshImageLandmarkEmbeddingsButton.setOnAction(e -> {
            landmarkImageBuilderBox.getItems().stream()
                .filter(i -> !i.getFeatureVectorLabel().isBlank())
                .forEach(item -> {
                try {
                    List<EmbeddingsImageUrl> inputs = new ArrayList<>();
                    inputs.add(imageUrlFromImage.apply(item.getCurrentImage()));
                    EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
                    input.setInput(inputs);
                    input.setDimensions(512);
                    input.setEmbedding_type("all");
                    input.setEncoding_format("float");
                    input.setModel(currentEmbeddingsModel);
                    input.setUser("string");

                    if(null != currentEmbeddingsModel)
                        input.setModel(currentEmbeddingsModel);
                    List<Integer> inputIDs = new ArrayList<>();
                    inputIDs.add(item.landmarkID);
                    RestAccessLayer.requestLandmarkImageEmbeddings(
                        input, scene, inputIDs, requestNumber.getAndIncrement());
                } catch (IOException ex) {
                    LOG.error(null, ex);
                }
            });
        }); 
        
        HBox visionHBox = new HBox(20,
            new VBox(5, new Label("Text Landmarks / Caption Labels"), landmarkTextBuilderBox, refreshLandmarkEmbeddingsButton),
            new VBox(5, new Label("Image Landmarks"), landmarkImageBuilderBox, refreshImageLandmarkEmbeddingsButton),
            new VBox(5, new Label("Distance Metric"), metricChoiceBox)
        );
        visionHBox.setPadding(new Insets(10));
        similarityTab.setContent(visionHBox);
        
        
        //Services Tab ////////////////////////////////////////////////
        Button isAliveButton = new Button("Check Status");
        isAliveButton.setOnAction(e -> {
            RestAccessLayer.requestRestIsAlive(isAliveButton.getScene());
        });
        Button chatStatusButton = new Button("Check Status");
        chatStatusButton.setOnAction(e -> {
            RestAccessLayer.requestChatModels(chatStatusButton.getScene());
        });
        
        Spinner<Integer> batchSizeSpinner = new Spinner(1, 256, batchSize, 1);
        batchSizeSpinner.valueProperty().addListener(c -> {
            batchSize = batchSizeSpinner.getValue();
        });
        batchSizeSpinner.setEditable(true);
        batchSizeSpinner.setPrefWidth(100);
        
        Spinner<Long> requestDelaySpinner = new Spinner(1, 1000, requestDelay, 1);
        requestDelaySpinner.valueProperty().addListener(c -> {
            requestDelay = requestDelaySpinner.getValue();
        });
        requestDelaySpinner.setEditable(true);
        requestDelaySpinner.setPrefWidth(100);
        
        VBox spinnerVBox = new VBox(20, 
            new VBox(5,new Label("Request Batch Size"), batchSizeSpinner), 
            new VBox(5,new Label("Request Delay ms"), requestDelaySpinner)
        );
        
        GridPane servicesGrid = new GridPane(20, 10);
        servicesGrid.setPadding(new Insets(10));
        servicesGrid.setAlignment(Pos.TOP_LEFT);
        servicesTab.setContent(servicesGrid);
        
        //Image Embeddings Service
        TextField embeddingsLocationTextField = new TextField(
            RestAccessLayer.restAccessLayerconfig.getBaseRestURL() +
            RestAccessLayer.restAccessLayerconfig.getImageEmbeddingsEndpoint()        
        );
        embeddingsLocationTextField.setPrefWidth(500);
        embeddingsLocationTextField.setEditable(false);

        ChoiceBox<String> embeddingsModelChoiceBox = new ChoiceBox();
        embeddingsModelChoiceBox.getItems().add(currentEmbeddingsModel);
        embeddingsModelChoiceBox.getSelectionModel().selectFirst();
        embeddingsModelChoiceBox.setPrefWidth(400);
        embeddingsModelChoiceBox.setOnAction(e -> {
            String selectedModel = embeddingsModelChoiceBox.getSelectionModel().getSelectedItem();
            if(null != selectedModel && !selectedModel.isBlank())
                currentEmbeddingsModel = selectedModel;
        });        

        //Multimodal Embedding Service
        Button refreshEmbeddingsModelsButton = new Button("Refresh");
        refreshEmbeddingsModelsButton.setOnAction(e -> {
            RestAccessLayer.requestRestIsAlive(refreshEmbeddingsModelsButton.getScene());
        });        
        Button testEmbeddingsImageButton = new Button("Test Image");
        testEmbeddingsImageButton.setOnAction(e -> {
            try {
                List<Integer> inputIDs = new ArrayList<>();
                inputIDs.add(-1);
                List<EmbeddingsImageUrl> inputs = new ArrayList<>();
                inputs.add(imageUrlFromImage.apply(ResourceUtils.load3DTextureImage("carl-b-portrait")));
                requestEmbeddings(inputs, inputIDs);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });   
        Button testTextEmbeddingButton = new Button("Test Text");
        testTextEmbeddingButton.setOnAction(e -> {
            try {
                EmbeddingsImageInput input = EmbeddingsImageInput.hellocarlTextEmbeddingsImageInput();
                if(null != currentEmbeddingsModel)
                    input.setModel(currentEmbeddingsModel);
                List<Integer> inputIDs = new ArrayList<>();
                inputIDs.add(-1);
                RestAccessLayer.requestTextEmbeddings(input, scene, inputIDs, 666);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });   

        //Chat Completion Service
        TextField chatLocationTextField = new TextField(
            RestAccessLayer.restAccessLayerconfig.getBaseRestURL() +
            RestAccessLayer.restAccessLayerconfig.getChatCompletionEndpoint()        
        );
        chatLocationTextField.setPrefWidth(500);
        chatLocationTextField.setEditable(false);

        ChoiceBox<String> chatModelChoiceBox = new ChoiceBox();
        chatModelChoiceBox.getItems().add(currentChatModel);
        chatModelChoiceBox.getSelectionModel().selectFirst();
        chatModelChoiceBox.setPrefWidth(400);
        chatModelChoiceBox.setOnAction(e -> {
            String selectedModel = chatModelChoiceBox.getSelectionModel().getSelectedItem();
            if(null != selectedModel && !selectedModel.isBlank())
                currentChatModel = selectedModel;
        });        

        Button refreshChatModelsButton = new Button("Refresh");
        refreshChatModelsButton.setOnAction(e -> {
            RestAccessLayer.requestChatModels(refreshChatModelsButton.getScene());
        });   
        Button testChatModelButton = new Button("Test Chat");
        testChatModelButton.setOnAction(e -> {
            ChatCompletionsInput input = ChatCompletionsInput.helloworldChatCompletionsInput();
            if(null != currentChatModel)
                input.setModel(currentChatModel);
            try {
                RestAccessLayer.requestChatCompletion(input, testChatModelButton.getScene(), 666, 9001);
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);            
            }
        });   
        Button testVisionModelButton = new Button("Test Vision");
        testVisionModelButton.setOnAction(e -> {
            try {
                ChatCompletionsInput input = ChatCompletionsInput.hellocarlChatCompletionsInput();
                if(null != currentChatModel)
                    input.setModel(currentChatModel);
                RestAccessLayer.requestChatCompletion(input, testVisionModelButton.getScene(), 666, 9001);
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            } catch (IOException ex) {
                LOG.error(null, ex);            
            }
        });   

        //Map components into GridPane container
        servicesGrid.add(new VBox(5, 
            new Label("Embeddings Service Location"), 
            embeddingsLocationTextField, 
            isAliveButton), 0, 0);
        
        servicesGrid.add(new VBox(5,
            new Label("Current Embeddings Model"), 
            embeddingsModelChoiceBox,
            new HBox(10, refreshEmbeddingsModelsButton, testEmbeddingsImageButton, testTextEmbeddingButton)), 
            1, 0);
        
        servicesGrid.add(new VBox(5, 
            new Label("Chat Service Location"), 
            chatLocationTextField,
            chatStatusButton ), 0, 1);
        
        servicesGrid.add(new VBox(5,
            new Label("Current Chat Model"), 
            chatModelChoiceBox,
            new HBox(10, refreshChatModelsButton, testChatModelButton, testVisionModelButton)), 
            1, 1);

        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, GridPane.REMAINING);
        servicesGrid.add(separator, 0, 2);

        spinnerVBox.setAlignment(Pos.CENTER_LEFT);
        servicesGrid.add(spinnerVBox, 0, 3);

        textEmbeddingsBorderPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        textEmbeddingsBorderPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            event.consume();
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                loadTextTask(db.getFiles());
            }
        });
                
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
        
        scene.getRoot().addEventHandler(RestEvent.EMBEDDING_MODELS_ALIVE, event -> {
            AliveModels models = (AliveModels)event.object;
            embeddingsModelChoiceBox.getItems().clear();
            for(AiModel model : models.getAlive_models()){
                embeddingsModelChoiceBox.getItems().add(model.getId());
            }
            if(embeddingsModelChoiceBox.getItems().contains(currentEmbeddingsModel)){
                embeddingsModelChoiceBox.getSelectionModel().select(currentEmbeddingsModel);
            } else if(!embeddingsModelChoiceBox.getItems().isEmpty()) {
                embeddingsModelChoiceBox.getSelectionModel().selectFirst();
            }
        });
        scene.getRoot().addEventHandler(RestEvent.CHAT_MODELS_ALIVE, event -> {
            AliveModels models = (AliveModels)event.object;
            chatModelChoiceBox.getItems().clear();
            for(AiModel model : models.getAlive_models()){
                chatModelChoiceBox.getItems().add(model.getId());
            }
            if(chatModelChoiceBox.getItems().contains(currentChatModel)){
                chatModelChoiceBox.getSelectionModel().select(currentChatModel);
            } else if(!chatModelChoiceBox.getItems().isEmpty()) {
                chatModelChoiceBox.getSelectionModel().selectFirst();
            }
        });
   
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_LANDMARKIMAGE, event -> {
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<Integer> inputIDs = (List<Integer>) event.object2;
            String msg = "Received " + output.getData().size() + " embeddings at " 
                + format.format(LocalDateTime.now());
            
            int totalListItems = landmarkImageBuilderBox.getItems().size();
            for(int i=0;i<output.getData().size();i++){
                if(i<=totalListItems) {
                    EmbeddingsImageData currentOutput = output.getData().get(i);
                    int currentInputID = inputIDs.get(i);
                    landmarkImageBuilderBox.getItems().stream()
                        .filter(fi -> fi.landmarkID == currentInputID)
                        .forEach(item -> {
                            item.setEmbeddings(currentOutput.getEmbedding());
                            item.addMetaData("object", currentOutput.getObject());
                            item.addMetaData("type", currentOutput.getType());
                        });
                }
            }
            outstandingRequests.put(output.getRequestNumber(), STATUS.SUCCEEDED);
            System.out.println(msg);
        });
        
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_IMAGE, event -> {
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<Integer> inputIDs = (List<Integer>) event.object2;
            String msg = "Received " + output.getData().size() + " embeddings at " 
                + format.format(LocalDateTime.now());
            int totalListItems = imageEmbeddingsListView.getItems().size();
            for(int i=0;i<output.getData().size();i++){
                if(i<=totalListItems) {
                    EmbeddingsImageData currentOutput = output.getData().get(i);
                    int currentInputID = inputIDs.get(i);
                    imageEmbeddingsListView.getItems()
                        .filtered(fi -> fi.imageID == currentInputID)
                        .forEach(item -> {
                            item.setEmbeddings(currentOutput.getEmbedding());
                            item.addMetaData("object", currentOutput.getObject());
                            item.addMetaData("type", currentOutput.getType());
                        });
                }
            }
            
            outstandingRequests.put(output.getRequestNumber(), STATUS.SUCCEEDED);
            System.out.println(msg);
            int totalRequests = outstandingRequests.size();
            long remainingRequests = outstandingRequests.entrySet().stream()
                .filter(t -> t.getValue() == STATUS.REQUESTED).count();
            imageEmbeddingRequestIndicator.setTopLabelLater("Received " 
                + (totalRequests-remainingRequests) + " of " + totalRequests);

            if(!outstandingRequests.containsValue(STATUS.REQUESTED)) {
                imageEmbeddingRequestIndicator.spin(false);
                imageEmbeddingRequestIndicator.fadeBusy(true);
                outstandingRequests.clear();
            }
        });
        scene.getRoot().addEventHandler(RestEvent.ERROR_EMBEDDINGS_IMAGE, event -> {
            List<File> inputFiles = (List<File>) event.object;
            int request = (int) event.object2;
            outstandingRequests.put(request, STATUS.FAILED);
        });
        scene.getRoot().addEventHandler(RestEvent.NEW_CHAT_COMPLETION, event -> {
            ChatCompletionsOutput output = (ChatCompletionsOutput) event.object;
            String msg = "Received " + output.getChoices().size() + " Chat Choices at " + LocalDateTime.now();
            outstandingRequests.put(output.getRequestNumber(), STATUS.SUCCEEDED);
            System.out.println(msg);
            //System.out.println(output.getChoices().get(0).getText());
            
            imageEmbeddingsListView.getItems().stream()
                .filter(t -> t.imageID == output.getInputID())
                .forEach(item -> {
                    ChatCaptionResponse response = stringToChatCaptionResponse
                        .apply(output.getChoices().get(0).getRaw());
                    if(null != response) {
                        if(null != response.getCaption())
                            item.setFeatureVectorLabel(response.getCaption());
                        if(null != response.getDescription())
                            item.addDescription(response.getDescription());
                        if(null != response.getExplanation())
                            item.addExplanation(response.getExplanation());
                    }
                });
            outstandingRequests.remove(output.getRequestNumber());
            if(!outstandingRequests.containsValue(STATUS.REQUESTED)) {
                imageEmbeddingRequestIndicator.spin(false);
                imageEmbeddingRequestIndicator.fadeBusy(true);            
            }
            
        });
        scene.getRoot().addEventHandler(RestEvent.ERROR_CHAT_COMPLETIONS, event -> {
//            List<File> inputFiles = (List<File>) event.object;
            int request = (int) event.object2;
            outstandingRequests.put(request, STATUS.FAILED);
        });
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_TEXT, event -> {
            //Even though its a text embeddings event we reuse the same output data structure
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<Integer> inputIDs = (List<Integer>) event.object2;
            String msg = "Received " + output.getData().size() + " embeddings at " 
                + format.format(LocalDateTime.now());
            
            int totalListItems = textEmbeddingsListView.getItems().size();
            for(int i=0;i<output.getData().size();i++){
                if(i<=totalListItems) {
                    EmbeddingsImageData currentOutput = output.getData().get(i);
                    int currentInputID = inputIDs.get(i);
                    textEmbeddingsListView.getItems().stream()
                        .filter(li -> li.textID == currentInputID)
                        .forEach(item -> {
                            item.setEmbeddings(currentOutput.getEmbedding());
                            item.addMetaData("object", currentOutput.getObject());
                            item.addMetaData("type", currentOutput.getType());
                        });
                }
            }
            
            outstandingRequests.put(output.getRequestNumber(), STATUS.SUCCEEDED);
            textEmbeddingRequestIndicator.setTopLabelLater(msg);
            System.out.println(msg);
            outstandingRequests.remove(output.getRequestNumber());
            if(!outstandingRequests.containsValue(STATUS.REQUESTED)) {
                textEmbeddingRequestIndicator.spin(false);
                textEmbeddingRequestIndicator.fadeBusy(true);            
            }
        });        
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_LANDMARKTEXT, event -> {
            //Even though its a text embeddings event we reuse the same output data structure
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<Integer> inputIDs = (List<Integer>) event.object2;
            String msg = "Received " + output.getData().size() + " embeddings at " 
                + format.format(LocalDateTime.now());
            
            int totalListItems = landmarkTextBuilderBox.getItems().size();
            for(int i=0;i<output.getData().size();i++){
                if(i<=totalListItems) {
                    EmbeddingsImageData currentOutput = output.getData().get(i);
                    int currentInputID = inputIDs.get(i);
                    landmarkTextBuilderBox.getItems().stream()
                        .filter(li -> li.landmarkID == currentInputID)
                        .forEach(item -> {
                            item.setEmbeddings(currentOutput.getEmbedding());
                            item.addMetaData("object", currentOutput.getObject());
                            item.addMetaData("type", currentOutput.getType());
                        });
                }
            }
            
            outstandingRequests.put(output.getRequestNumber(), STATUS.SUCCEEDED);
            textEmbeddingRequestIndicator.setTopLabelLater(msg);
            System.out.println(msg);
            outstandingRequests.remove(output.getRequestNumber());
            if(!outstandingRequests.containsValue(STATUS.REQUESTED)) {
                textEmbeddingRequestIndicator.spin(false);
                textEmbeddingRequestIndicator.fadeBusy(true);            
            }
        });
        scene.getRoot().addEventHandler(ImageEvent.NEW_SCAN_IMAGE, event -> {
            Image scanImage = (Image) event.object;
            //write out image to file
            File newFile;
            try {
                newFile = ResourceUtils.saveImageFile(scanImage);
                EmbeddingsImageListItem newItem = new EmbeddingsImageListItem(newFile, renderIconsCheckBox.isSelected());
                imageEmbeddingsListView.getItems().add(0, newItem);
                imageFilesCountLabel.setText(String.valueOf(imageFilesList.size()));
                if(!imageEmbeddingsListView.getItems().isEmpty()) {
                    //trigger baseImageView to change
                    imageEmbeddingsListView.getSelectionModel().selectFirst();
                } else {
                    baseImage = waitingImage;
                    baseImageView.setImage(baseImage);
                }        
            } catch (IOException ex) {
                LOG.error(ex.getMessage());
            }
        });
        getStyleClass().add("hyperdrive-pane");
    }

    public void chooseCaptionsTask(List<EmbeddingsImageListItem> items, List<String> choices) {
        Task requestTask = new Task() {
            @Override
            protected Void call() throws Exception {
                for(EmbeddingsImageListItem item : items) {
                    EmbeddingsImageUrl url = imageUrlFromImage.apply(item.getCurrentImage());
                    try {
                        ChatCompletionsInput input = ChatCompletionsInput.defaultImageInput(
                            url.getImage_url(), CAPTION_TYPE.AUTOCHOOOSE);
                        String choosePrompt = input.getMessages().get(0).getContent().get(0).getText();
                        choosePrompt = Prompts.insertAutochooseChoices(choosePrompt, choices);
                        input.getMessages().get(0).getContent().get(0).setText(choosePrompt);
                        if(null != currentChatModel)
                            input.setModel(currentChatModel);
                        RestAccessLayer.requestChatCompletion(input, 
                            imageEmbeddingRequestIndicator.getScene(), 
                            item.imageID , requestNumber.getAndIncrement());
                    } catch (JsonProcessingException ex) {
                        LOG.error(null, ex);
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                    try {
                        Thread.sleep(requestDelay);
                    } catch (InterruptedException ex) {
                        LOG.error(null, ex);
                    }
                }
                return null;
            }
        };
        Thread t = new Thread(requestTask, "Trinity Image Auto-choose Captions Request");
        t.setDaemon(true);
        t.start();
    }    

    public void requestLandmarkSimilarityTask(
        List<EmbeddingsImageListItem> items, List<FeatureVector> landmarkFeatures) {
        Task requestTask = new Task() {
            @Override
            protected Void call() throws Exception {
                imageEmbeddingRequestIndicator.setLabelLater("Computing Landmark Similarity Distances...");
                imageEmbeddingRequestIndicator.spin(true);
                imageEmbeddingRequestIndicator.fadeBusy(false);
                System.out.println("Computing Landmark Simularity Distances...");
                Metric metric = Metric.getMetric(metricChoiceBox.getSelectionModel().getSelectedItem());
                long startTime = System.nanoTime();

                List<double[]> landmarkVectors = landmarkFeatures.stream()
                    .map(mapToStateArray).toList();
                items.stream().forEach(item -> {
                    double[] itemVector = mapToStateArray.apply(item.getFeatureVector());
                    Double shortestDistance = null;
                    Integer shortestLandmarkIndex = null;
                    for(int i=0;i<landmarkVectors.size();i++){
                        double currentDistance = metric.distance(itemVector, landmarkVectors.get(i));
                        //System.out.println(i + " : " + currentDistance);
                        if(null == shortestDistance || currentDistance < shortestDistance) {
                            shortestDistance = currentDistance;
                            shortestLandmarkIndex = i;
                        }
                    }
                    item.setFeatureVectorLabel(
                        landmarkFeatures.get(shortestLandmarkIndex).getLabel());
                });
                Utils.printTotalTime(startTime);
                imageEmbeddingRequestIndicator.setLabelLater("Finished.");
                imageEmbeddingRequestIndicator.spin(false);
                imageEmbeddingRequestIndicator.fadeBusy(true);
                return null;
            }
        };
        Thread t = new Thread(requestTask, "Trinity Landmark Simulatrity Task");
        t.setDaemon(true);
        t.start();
    }    
    public void requestCaptionsTask(List<EmbeddingsImageListItem> items) {
        Task requestTask = new Task() {
            @Override
            protected Void call() throws Exception {
                imageEmbeddingRequestIndicator.setLabelLater("Requesting Captions...");
                imageEmbeddingRequestIndicator.spin(true);
                imageEmbeddingRequestIndicator.fadeBusy(false);
                System.out.println("Requesting Captions...");
                for(EmbeddingsImageListItem item : items) {
                    EmbeddingsImageUrl url = imageUrlFromImage.apply(item.getCurrentImage());
                    try {
                        ChatCompletionsInput input = ChatCompletionsInput.defaultImageInput(url.getImage_url(), CAPTION_TYPE.DEFAULT);
                        if(null != currentChatModel)
                            input.setModel(currentChatModel);
                        RestAccessLayer.requestChatCompletion(input, 
                            imageEmbeddingRequestIndicator.getScene(), 
                            item.imageID , requestNumber.getAndIncrement());
                    } catch (JsonProcessingException ex) {
                        LOG.error(null, ex);
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                    Thread.sleep(requestDelay);
                }
                return null;
            }
        };
        Thread t = new Thread(requestTask, "Trinity Image Captioning Request");
        t.setDaemon(true);
        t.start();
    }    
    
    public void loadTextTask(List<File> files) {
        Task loadTask = new Task() {
            @Override
            protected Void call() throws Exception {
                AtomicInteger atomicCount = new AtomicInteger(0);
                textEmbeddingRequestIndicator.setFadeTimeMS(250);
                textEmbeddingRequestIndicator.setLabelLater("Loading " + atomicCount.toString() + " files...");
                textEmbeddingRequestIndicator.spin(true);
                textEmbeddingRequestIndicator.fadeBusy(false);
                
                currentTextFeatureList.clear();
                textFilesList.clear();
                System.out.println("Searching for files, filtering on ASCII....");
                long startTime = System.nanoTime();
                for(File file : files) {
                    System.out.println(file.getAbsolutePath());
                    if(file.isDirectory()) {
                        textFilesList.addAll(
                            Files.walk(file.toPath())
                                .map(Path::toFile)
                                .filter(f -> ResourceUtils.isTextFile(f))
                                .toList());
                    } else {
                        if(ResourceUtils.isTextFile(file))
                            textFilesList.add(file);
                    }
                }
                textFilesList.removeIf(f -> !ResourceUtils.isTextFile(f));
                Utils.printTotalTime(startTime);
                final double total = textFilesList.size();
                System.out.println("Loading textfiles into listitems....");
                startTime = System.nanoTime();
//                final boolean renderIcons = renderIconsCheckBox.isSelected();
                List<EmbeddingsTextListItem> newItems = 
                    textFilesList.parallelStream()
                        .map(EmbeddingsTextListItem.itemNoParseFromFile)
                        .peek(i -> {
                            double completed = atomicCount.incrementAndGet();
                            textEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                            textEmbeddingRequestIndicator.setLabelLater(completed + " of " + total);
                        }).toList();
                Utils.printTotalTime(startTime);
                
                System.out.println("Populating ListView....");
                Platform.runLater(()-> {
                    long start = System.nanoTime();
                    textEmbeddingsListView.getItems().addAll(newItems);
                    System.out.println("loaded " + String.valueOf(textFilesList.size()) + " ASCII files.");
//                    imageFilesCountLabel.setText(String.valueOf(textFilesList.size()));
                    if(!textEmbeddingsListView.getItems().isEmpty()) {
                        textEmbeddingsListView.getSelectionModel().selectFirst();
                    } else {
                        baseTextArea.clear();
                    }        
                    Utils.printTotalTime(start);
                });
                textEmbeddingRequestIndicator.setLabelLater("Complete");
                textEmbeddingRequestIndicator.spin(false);
                textEmbeddingRequestIndicator.fadeBusy(true);
                return null;
            }
        };
        Thread t = new Thread(loadTask, "Trinity Batch Image Load Task");
        t.setDaemon(true);
        t.start();
    }
            
    public void loadImagesTask(List<File> files) {
        Task loadTask = new Task() {
            @Override
            protected Void call() throws Exception {
                AtomicInteger atomicCount = new AtomicInteger(0);
                imageEmbeddingRequestIndicator.setFadeTimeMS(250);
                imageEmbeddingRequestIndicator.setLabelLater("Loading " + atomicCount.toString() + " images...");
                imageEmbeddingRequestIndicator.spin(true);
                imageEmbeddingRequestIndicator.fadeBusy(false);
                
                currentFeatureList.clear();
                imageFilesList.clear();
                System.out.println("Searching for files, filtering images....");
                long startTime = System.nanoTime();
                for(File file : files) {
                    System.out.println(file.getAbsolutePath());
                    if(file.isDirectory()) {
                        imageFilesList.addAll(
                            Files.walk(file.toPath())
                                .map(Path::toFile)
                                .filter(f -> JavaFX3DUtils.isTextureFile(f))
                                .toList());
                    } else {
                        if(JavaFX3DUtils.isTextureFile(file))
                            imageFilesList.add(file);
                    }
                }
                imageFilesList.removeIf(f -> !JavaFX3DUtils.isTextureFile(f));
                Utils.printTotalTime(startTime);
                final double total = imageFilesList.size();
                System.out.println("Loading images into listitems....");
                startTime = System.nanoTime();
                final boolean renderIcons = renderIconsCheckBox.isSelected();
                List<EmbeddingsImageListItem> newItems = 
                    imageFilesList.parallelStream()
                        .map(renderIcons ? itemFromFile : itemNoRenderFromFile)
                        .peek(i -> {
                            double completed = atomicCount.incrementAndGet();
                            imageEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                            imageEmbeddingRequestIndicator.setLabelLater(completed + " of " + total);
                        }).toList();
                Utils.printTotalTime(startTime);
                
                System.out.println("Populating ListView....");
                Platform.runLater(()-> {
                    long start = System.nanoTime();
                    imageEmbeddingsListView.getItems().addAll(newItems);
                    imageFilesCountLabel.setText(String.valueOf(imageFilesList.size()));
                    if(!imageEmbeddingsListView.getItems().isEmpty()) {
                        //trigger baseImageView to change
                        imageEmbeddingsListView.getSelectionModel().selectFirst();
                    } else {
                        baseImage = waitingImage;
                        baseImageView.setImage(baseImage);
                    }        
                    Utils.printTotalTime(start);
                });
                imageEmbeddingRequestIndicator.setLabelLater("Complete");
                imageEmbeddingRequestIndicator.spin(false);
                imageEmbeddingRequestIndicator.fadeBusy(true);
                return null;
            }
        };
        Thread t = new Thread(loadTask, "Trinity Batch Image Load Task");
        t.setDaemon(true);
        t.start();
    }
    private void requestEmbeddings(List<EmbeddingsImageUrl> currentBatch, List<Integer> inputIDs) {
        EmbeddingsImageBatchInput input = new EmbeddingsImageBatchInput();
        input.setInput(currentBatch);
        input.setDimensions(512);
        input.setEmbedding_type("all");
        input.setEncoding_format("float");
        input.setModel(currentEmbeddingsModel);
        input.setUser("string");
        try {
            int rn = requestNumber.incrementAndGet();
            imageEmbeddingRequestIndicator.setLabelLater("Embeddings Request " + rn + "...");
            System.out.println("Sending " + currentBatch.size() + " images for processing at " + LocalDateTime.now());
            RestAccessLayer.requestImageEmbeddings(input, 
                imageEmbeddingRequestIndicator.getScene(), inputIDs, rn);
            outstandingRequests.put(rn, STATUS.REQUESTED);
        } catch (JsonProcessingException ex) {
            LOG.error(null, ex);
        }
    }
    public void requestTextEmbeddingsTask() {
        Task requestTask = new Task() {
            @Override
            protected Void call() throws Exception {
                AtomicInteger atomicCount = new AtomicInteger(0);
                textEmbeddingRequestIndicator.setFadeTimeMS(250);
                textEmbeddingRequestIndicator.setLabelLater("Encoding Text...");
                textEmbeddingRequestIndicator.spin(true);
                textEmbeddingRequestIndicator.fadeBusy(false);
                System.out.println("Loading and Encoding Text...");
                long startTime = System.nanoTime();
//                List<EmbeddingsImageUrl> inputs = new ArrayList<>();
//                List<Integer> inputIDs = new ArrayList<>();
                final double total = textEmbeddingsListView.getSelectionModel().getSelectedItems().size();
                textEmbeddingsListView.getSelectionModel().getSelectedItems()
                    .parallelStream().forEach(item -> {
                        if(null == item.contents)
                            item.readText();
                        EmbeddingsImageInput input = EmbeddingsImageInput.defaultTextInput(item.contents);
                        if(null != currentEmbeddingsModel)
                            input.setModel(currentEmbeddingsModel);
                        List<Integer> inputIDs = new ArrayList<>();
                        inputIDs.add(item.textID);
                        try {
                            RestAccessLayer.requestTextEmbeddings(
                                    input, scene, inputIDs, requestNumber.getAndIncrement());
                        } catch (JsonProcessingException ex) {
                            java.util.logging.Logger.getLogger(HyperdrivePane.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        double completed = atomicCount.incrementAndGet();
                        textEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                        textEmbeddingRequestIndicator.setLabelLater("Encoding " + completed + " of " + total);
                });
                Utils.printTotalTime(startTime);
                double completed = atomicCount.get();
                textEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                textEmbeddingRequestIndicator.setLabelLater("Requested " + completed + " of " + total);
                
//                //break up the requests based on batch size
//                int currentIndex = 0;
//                while(currentIndex < inputs.size()) {
//                    int endCurrentIndex = currentIndex + batchSize;
//                    if(endCurrentIndex > inputs.size())
//                        endCurrentIndex = inputs.size();
//                    List<EmbeddingsImageUrl > currentBatch = 
//                        inputs.subList(currentIndex, endCurrentIndex);
//                    System.out.println("Batch created: " + currentBatch.size());
//                    requestEmbeddings(currentBatch, inputIDs.subList(currentIndex, endCurrentIndex));
//                    currentIndex += batchSize;
//
//                    completed = Integer.valueOf(currentIndex).doubleValue();
//                    imageEmbeddingRequestIndicator.setPercentComplete(completed / total); 
//                    imageEmbeddingRequestIndicator.setLabelLater("Requested " + completed + " of " + total);
//                    Thread.sleep(requestDelay);
//                }
                return null;
            }
        };
        Thread t = new Thread(requestTask, "Trinity Embeddings Text Request");
        t.setDaemon(true);
        t.start();
    }        
    public void requestEmbeddingsTask() {
        Task requestTask = new Task() {
            @Override
            protected Void call() throws Exception {
                AtomicInteger atomicCount = new AtomicInteger(0);
                imageEmbeddingRequestIndicator.setFadeTimeMS(250);
                imageEmbeddingRequestIndicator.setLabelLater("Encoding Images...");
                imageEmbeddingRequestIndicator.spin(true);
                imageEmbeddingRequestIndicator.fadeBusy(false);
                System.out.println("Loading and Encoding Images...");
                long startTime = System.nanoTime();
                List<EmbeddingsImageUrl> inputs = new ArrayList<>();
                List<Integer> inputIDs = new ArrayList<>();
                final double total = imageEmbeddingsListView.getSelectionModel().getSelectedItems().size();
                imageEmbeddingsListView.getSelectionModel().getSelectedItems()
                    .parallelStream().forEach(item -> {
                        inputs.add(imageUrlFromImage.apply(item.getCurrentImage()));
                        inputIDs.add(item.imageID);
                        double completed = inputs.size();
                        imageEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                        imageEmbeddingRequestIndicator.setLabelLater("Encoding " + completed + " of " + total);
                        
                });
                Utils.printTotalTime(startTime);
                double completed = atomicCount.incrementAndGet();
                imageEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                imageEmbeddingRequestIndicator.setLabelLater("Requested " + completed + " of " + total);
                
                //break up the requests based on batch size
                int currentIndex = 0;
                while(currentIndex < inputs.size()) {
                    int endCurrentIndex = currentIndex + batchSize;
                    if(endCurrentIndex > inputs.size())
                        endCurrentIndex = inputs.size();
                    List<EmbeddingsImageUrl > currentBatch = 
                        inputs.subList(currentIndex, endCurrentIndex);
                    System.out.println("Batch created: " + currentBatch.size());
                    requestEmbeddings(currentBatch, inputIDs.subList(currentIndex, endCurrentIndex));
                    currentIndex += batchSize;

                    completed = Integer.valueOf(currentIndex).doubleValue();
                    imageEmbeddingRequestIndicator.setPercentComplete(completed / total); 
                    imageEmbeddingRequestIndicator.setLabelLater("Requested " + completed + " of " + total);
                    Thread.sleep(requestDelay);
                }
                return null;
            }
        };
        Thread t = new Thread(requestTask, "Trinity Embeddings Image Request");
        t.setDaemon(true);
        t.start();
    }
    private void refreshImageFiles(boolean renderIcons) {
        imageEmbeddingsListView.getItems().forEach(item -> item.reloadImage(renderIcons));
    }
    public void updateLabels(String labelText, boolean onlySelected, boolean overwrite) {
        if(onlySelected && overwrite) {
            imageEmbeddingsListView.getSelectionModel().getSelectedItems().forEach(i -> 
                i.setFeatureVectorLabel(labelText));
        } else if(onlySelected && !overwrite) {
            imageEmbeddingsListView.getSelectionModel().getSelectedItems()
                .filtered(t -> t.getFeatureVectorLabel().isBlank())
                .forEach(i -> i.setFeatureVectorLabel(labelText));
        } else if(!onlySelected && overwrite) {
            imageEmbeddingsListView.getItems().forEach(i -> 
                i.setFeatureVectorLabel(labelText));
        } else {
            imageEmbeddingsListView.getItems()
                .filtered(t -> t.getFeatureVectorLabel().isBlank())
                .forEach(i -> i.setFeatureVectorLabel(labelText));
        }
    }

}
