package edu.jhuapl.trinity.javafx.components.panes;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.messages.llm.AiModel;
import edu.jhuapl.trinity.data.messages.llm.AliveModels;
import edu.jhuapl.trinity.data.messages.llm.ChatCaptionResponse;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsInput;
import edu.jhuapl.trinity.data.messages.llm.ChatCompletionsOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageBatchInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageData;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageInput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl;
import edu.jhuapl.trinity.data.messages.llm.Prompts;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.hyperdrive.CaptionChooserBox;
import edu.jhuapl.trinity.javafx.components.hyperdrive.ChooseCaptionsTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.HyperdriveTask.REQUEST_STATUS;
import edu.jhuapl.trinity.javafx.components.hyperdrive.LoadImagesTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.LoadTextTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.RequestCaptionsTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.RequestEmbeddingsTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.RequestLandmarkSimilarityTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.RequestTextEmbeddingsTask;
import edu.jhuapl.trinity.javafx.components.hyperdrive.RequestTextLandmarkSimilarityTask;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.listviews.LandmarkImageBuilderBox;
import edu.jhuapl.trinity.javafx.components.listviews.LandmarkListItem;
import edu.jhuapl.trinity.javafx.components.listviews.LandmarkTextBuilderBox;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.metric.Metric;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.jhuapl.trinity.data.messages.llm.EmbeddingsImageUrl.imageUrlFromImage;
import edu.jhuapl.trinity.javafx.components.hyperdrive.BatchRequestManager;
import edu.jhuapl.trinity.javafx.components.hyperdrive.ImageEmbeddingsBatchLauncher;
import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalSuccess;
import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalWarning;
import static edu.jhuapl.trinity.messages.RestAccessLayer.*;
import java.text.DecimalFormat;
import javafx.stage.FileChooser;

/**
 * @author Sean Phillips
 */
public class HyperdrivePane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(HyperdrivePane.class);
    private static int EMBEDDINGS_IMAGE_TESTID = -9001;
    private static int EMBEDDINGS_TEXT_TESTID = -9002;
    private static int CHAT_CHAT_TESTID = -9003;
    private static int CHAT_VISION_TESTID = -9004;
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
    Tab filesTab;
    CheckBox renderIconsCheckBox;

    ArrayList<FeatureVector> currentFeatureList;
    ArrayList<FeatureVector> currentTextFeatureList;
    ArrayList<File> imageFilesList;
    ArrayList<File> textFilesList;
    ListView<EmbeddingsImageListItem> imageEmbeddingsListView;
    ListView<EmbeddingsTextListItem> textEmbeddingsListView;
    Label imageFilesCountLabel;
    Label textFilesCountLabel;
    CircleProgressIndicator textEmbeddingRequestIndicator;
    CircleProgressIndicator imageEmbeddingRequestIndicator;
    LandmarkTextBuilderBox landmarkTextBuilderBox;
    LandmarkImageBuilderBox landmarkImageBuilderBox;
    TextField embeddingsLocationTextField;
    TextField chatLocationTextField;
    ChoiceBox<String> metricChoiceBox;
    AtomicInteger requestNumber;
    AtomicInteger batchNumber;
    
    HashMap<Integer, REQUEST_STATUS> outstandingRequests;
    ImageEmbeddingsBatchLauncher imageBatchLauncher;
    BatchRequestManager<List<EmbeddingsImageListItem>> imageEmbeddingManager;

    int batchSize = 1;
    int maxInFlightBatches = 10;
    long requestDelay = 50;
    long requestTimeoutMS = 180000;
    int chunkSize = 16384;
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
        requestNumber = new AtomicInteger(0);
        batchNumber = new AtomicInteger(0);
        waitingImage = ResourceUtils.loadIconFile("waitingforimage");
        setBackground(Background.EMPTY);
        //container for the floating window itself
        borderPane = (BorderPane) this.contentPane;
        imageryEmbeddingsTab = new Tab("Imagery");
        textEmbeddingsTab = new Tab("Text");
        similarityTab = new Tab("Similarity");
        servicesTab = new Tab("Services");
        filesTab = new Tab("Files");

        tabPane = new TabPane(imageryEmbeddingsTab, textEmbeddingsTab, 
            similarityTab, servicesTab, filesTab);
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
        textEmbeddingsListView.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            if (!textEmbeddingsListView.getSelectionModel().isEmpty()) {
                String text = textEmbeddingsListView.getSelectionModel()
                    .getSelectedItems().get(0).contents;
                if (null == text) {
                    textEmbeddingsListView.getSelectionModel()
                        .getSelectedItems().get(0).readText();
                }
                baseTextArea.setText(text);
            }
        });

        MenuItem selectAllTextMenuItem = new MenuItem("Select All");
        selectAllTextMenuItem.setOnAction(e ->
            textEmbeddingsListView.getSelectionModel().selectAll());

        MenuItem textEmbbedingsTextLandmarkCaptionItem = new MenuItem("Label by Text Landmark Similarity");
        textEmbbedingsTextLandmarkCaptionItem.setOnAction(e -> {
            if (!textEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty())
                requestTextLandmarkSimilarityTask(textEmbeddingsListView.getSelectionModel().getSelectedItems()
                    , landmarkTextBuilderBox.getItems().stream()
                        .map(LandmarkListItem::getFeatureVector).toList());
        });
        MenuItem imageEmbeddingsImageLandmarkCaptionItem = new MenuItem("Label by Image Landmark Similarity");
        imageEmbeddingsImageLandmarkCaptionItem.setOnAction(e -> {
            if (!textEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
                requestTextLandmarkSimilarityTask(textEmbeddingsListView.getSelectionModel().getSelectedItems(),
                    landmarkImageBuilderBox.getItems().stream()
                        .map(LandmarkListItem::getFeatureVector).toList()
                );
            }
        });

        MenuItem clearTextRequestsItem = new MenuItem("Clear Requests");
        clearTextRequestsItem.setOnAction(e -> {
            outstandingRequests.clear();
            textEmbeddingRequestIndicator.spin(false);
            textEmbeddingRequestIndicator.fadeBusy(true);
        });
        ContextMenu textEmbeddingsContextMenu =
            new ContextMenu(selectAllTextMenuItem, textEmbbedingsTextLandmarkCaptionItem,
                imageEmbeddingsImageLandmarkCaptionItem, clearTextRequestsItem);
        textEmbeddingsListView.setContextMenu(textEmbeddingsContextMenu);

        BorderPane textEmbeddingsBorderPane = new BorderPane(textEmbeddingsListView);
        textEmbeddingsBorderPane.setPrefWidth(600);

        Label textFilesLabel = new Label("Total Text Files: ");
        textFilesCountLabel = new Label("0");
        HBox textFileControlsBox = new HBox(10, textFilesLabel, textFilesCountLabel);
        textFileControlsBox.setAlignment(Pos.CENTER);
        textEmbeddingsBorderPane.setTop(textFileControlsBox);

        Button getTextEmbeddingsButton = new Button("Request Embeddings");
        getTextEmbeddingsButton.setWrapText(true);
        getTextEmbeddingsButton.setTextAlignment(TextAlignment.CENTER);
        getTextEmbeddingsButton.setOnAction(e -> {
            if (!textEmbeddingsListView.getItems().isEmpty()) {
                requestTextEmbeddingsTask();
            }
        });
        //add controls to execute over embeddings to the bottom
        Button clearTextEmbeddingsButton = new Button("Clear All");
        clearTextEmbeddingsButton.setWrapText(true);
        clearTextEmbeddingsButton.setTextAlignment(TextAlignment.CENTER);
        clearTextEmbeddingsButton.setOnAction(e -> {
            currentTextFeatureList.clear();
            textEmbeddingsListView.getItems().clear();
            textFilesCountLabel.setText(String.valueOf(textFilesList.size()));
        });
        clearTextEmbeddingsButton.setCancelButton(true);

        Button clearCompleteTextEmbeddingsButton = new Button("Clear Complete");
        clearCompleteTextEmbeddingsButton.setWrapText(true);
        clearCompleteTextEmbeddingsButton.setTextAlignment(TextAlignment.CENTER);
        clearCompleteTextEmbeddingsButton.setOnAction(e -> {
            List<EmbeddingsTextListItem> keep = textEmbeddingsListView.getItems()
                .stream().filter(i -> !i.embeddingsReceived()).toList();
            textEmbeddingsListView.getItems().clear();
            currentTextFeatureList.clear();
            textEmbeddingsListView.getItems().addAll(keep);
        });

        Button injectTextFeaturesButton = new Button("Inject Features");
        injectTextFeaturesButton.setWrapText(true);
        injectTextFeaturesButton.setTextAlignment(TextAlignment.CENTER);
        injectTextFeaturesButton.setOnAction(e -> {
            currentTextFeatureList.clear();
            currentTextFeatureList.addAll(textEmbeddingsListView.getItems().stream()
                .filter(EmbeddingsTextListItem::embeddingsReceived) //only inject if the embeddings are set
                .map(EmbeddingsTextListItem::getFeatureVector)
                .toList());
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(currentTextFeatureList);
            injectTextFeaturesButton.getScene().getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
        });

        HBox textControlsBox = new HBox(10,
            getTextEmbeddingsButton, clearTextEmbeddingsButton,
            clearCompleteTextEmbeddingsButton, injectTextFeaturesButton
        );
        textControlsBox.setAlignment(Pos.CENTER);
        textEmbeddingsBorderPane.setBottom(textControlsBox);

        textEmbeddingRequestIndicator = new CircleProgressIndicator();
        ProgressStatus ps = new ProgressStatus("Working", 0.5);
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

        // Image Import Controls ///////////////////////////////////////////////
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

        Button imageEmbeddingsButton = new Button("Request Embeddings");
        imageEmbeddingsButton.setWrapText(true);
        imageEmbeddingsButton.setTextAlignment(TextAlignment.CENTER);
        
imageEmbeddingsButton.setOnAction(e -> {
    List<EmbeddingsImageListItem> items = imageEmbeddingsListView.getSelectionModel().getSelectedItems();
    List<List<EmbeddingsImageListItem>> batches = new ArrayList<>();
    for (int i = 0; i < items.size(); i += batchSize) {
        batches.add(
            new ArrayList<>(items.subList(i, Math.min(i + batchSize, items.size())))
        );
    }
    System.out.println("Total Batches: " + batches.size());
    //Progress & OutstandingRequests Initialization
    outstandingRequests.clear();
    for (EmbeddingsImageListItem item : items) {  // use the flat, full list!
        outstandingRequests.put(item.imageID, REQUEST_STATUS.REQUESTED);
    }
    imageEmbeddingRequestIndicator.setPercentComplete(0);
    imageEmbeddingRequestIndicator.setTopLabelLater("Received 0 of " + items.size());
    imageEmbeddingRequestIndicator.spin(true);
    if(!imageEmbeddingRequestIndicator.inView())
        imageEmbeddingRequestIndicator.fadeBusy(false);    
    imageEmbeddingManager.clearCounters();
    // Launch batches
    imageEmbeddingManager.enqueue(batches);
});

        ImageView embeddingsPlaceholderIV = ResourceUtils.loadIcon("data", 50);
        HBox embeddingsPlaceholder = new HBox(10, embeddingsPlaceholderIV, new Label("No Data Sources Marked"));
        embeddingsPlaceholder.setAlignment(Pos.CENTER);
        imageEmbeddingsListView = new ListView<>();
        imageEmbeddingsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        imageEmbeddingsListView.setPlaceholder(embeddingsPlaceholder);
        imageEmbeddingsListView.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            if (!imageEmbeddingsListView.getSelectionModel().isEmpty()) {
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
            if (!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
                TextInputDialog td = new TextInputDialog("enter any text");
                td.setHeaderText("Manually set Label for "
                    + imageEmbeddingsListView.getSelectionModel().getSelectedItems().size()
                    + " items.");
                td.setGraphic(ResourceUtils.loadIcon("console", 75));
                td.initStyle(StageStyle.TRANSPARENT);
                DialogPane dialogPane = td.getDialogPane();
                dialogPane.setBackground(Background.EMPTY);
                dialogPane.getScene().setFill(Color.TRANSPARENT);
                String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
                dialogPane.getStylesheets().add(DIALOGCSS);
                Optional<String> captionOptional = td.showAndWait();
                if (captionOptional.isPresent()) {
                    imageEmbeddingsListView.getSelectionModel().getSelectedItems()
                        .forEach(i -> i.setFeatureVectorLabel(captionOptional.get()));
                }
            }
        });

        MenuItem requestCaptionItem = new MenuItem("Request Label/Captions");
        requestCaptionItem.setOnAction(e -> {
            if (!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty())
                requestCaptionsTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems());
        });
        MenuItem chooseCaptionItem = new MenuItem("Auto-choose Caption");
        chooseCaptionItem.setOnAction(e -> {
            if (!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
                imageEmbeddingRequestIndicator.setLabelLater("Choose Captions...");
                imageEmbeddingRequestIndicator.spin(true);
                if(!imageEmbeddingRequestIndicator.inView())
                    imageEmbeddingRequestIndicator.fadeBusy(false);                //LOG.info("Prompting User for Labels...");
                Platform.runLater(() -> {
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
                    String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
                    dialogPane.getStylesheets().add(DIALOGCSS);
                    Optional<ButtonType> captionOptional = alert.showAndWait();
                    if (captionOptional.get() == ButtonType.OK) {
                        //@DEBUG SMP
                        //System.out.println("Choices from user: " + box.getChoices());
                        landmarkTextBuilderBox.setChoices(box.getChoices());
                        if (!box.getChoices().isEmpty()) {
                            chooseCaptionsTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems(),
                                box.getChoices());
                        }
                    }
                });
            }
        });

        MenuItem textLandmarkCaptionItem = new MenuItem("Label by Text Landmark Similarity");
        textLandmarkCaptionItem.setOnAction(e -> {
            if (!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty())
                requestLandmarkSimilarityTask(imageEmbeddingsListView.getSelectionModel().getSelectedItems()
                    , landmarkTextBuilderBox.getItems().stream()
                        .map(LandmarkListItem::getFeatureVector).toList());
        });
        MenuItem imageLandmarkCaptionItem = new MenuItem("Label by Image Landmark Similarity");
        imageLandmarkCaptionItem.setOnAction(e -> {
            if (!imageEmbeddingsListView.getSelectionModel().getSelectedItems().isEmpty()) {
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
        MenuItem stopAndClearMenuItem = new MenuItem("Stop and Clear Enqueued");
        stopAndClearMenuItem.setOnAction(e -> {
            imageEmbeddingManager.stopAndClear();
            // Clear GUI progress and outstandingRequests map too, if desired
            outstandingRequests.clear();
            imageEmbeddingRequestIndicator.setPercentComplete(0);
            imageEmbeddingRequestIndicator.setTopLabelLater("Stopped");
            imageEmbeddingRequestIndicator.spin(false);
            imageEmbeddingRequestIndicator.fadeBusy(true);
        });        
        ContextMenu embeddingsContextMenu =
            new ContextMenu(selectAllMenuItem, setCaptionItem, requestCaptionItem,
                chooseCaptionItem, textLandmarkCaptionItem, imageLandmarkCaptionItem,
                clearRequestsItem, stopAndClearMenuItem);
        imageEmbeddingsListView.setContextMenu(embeddingsContextMenu);

        embeddingsCenterStack = new StackPane();
        embeddingsCenterStack.setAlignment(Pos.CENTER);
        embeddingsBorderPane = new BorderPane(embeddingsCenterStack);
        imageEmbeddingRequestIndicator = new CircleProgressIndicator();
        imageEmbeddingRequestIndicator.updateStatus(ps);
        imageEmbeddingRequestIndicator.defaultOpacity = 1.0;
        imageEmbeddingRequestIndicator.setOpacity(0.0); ///instead of setVisible(false)

        embeddingsCenterStack.getChildren().addAll(imageEmbeddingsListView);

        //add controls to execute over embeddings to the bottom
        Button clearImageEmbeddingsButton = new Button("Clear embeddings");
        clearImageEmbeddingsButton.setWrapText(true);
        clearImageEmbeddingsButton.setTextAlignment(TextAlignment.CENTER);
        clearImageEmbeddingsButton.setOnAction(e -> {
            currentFeatureList.clear();
            imageEmbeddingsListView.getItems().clear();
        });
        clearImageEmbeddingsButton.setCancelButton(true);

        Button clearCompleteImageEmbeddingsButton = new Button("Clear Complete");
        clearCompleteImageEmbeddingsButton.setWrapText(true);
        clearCompleteImageEmbeddingsButton.setTextAlignment(TextAlignment.CENTER);
        clearCompleteImageEmbeddingsButton.setOnAction(e -> {
            List<EmbeddingsImageListItem> keep = imageEmbeddingsListView.getItems()
                .stream().filter(i -> !i.embeddingsReceived()).toList();
            imageEmbeddingsListView.getItems().clear();
            currentFeatureList.clear();
            imageEmbeddingsListView.getItems().addAll(keep);
        });

        Button injectFeaturesButton = new Button("Inject Features");
        injectFeaturesButton.setWrapText(true);
        injectFeaturesButton.setTextAlignment(TextAlignment.CENTER);
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
            imageEmbeddingsButton, clearImageEmbeddingsButton,
            clearCompleteImageEmbeddingsButton, injectFeaturesButton
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
        captionMenuItem.setOnAction(e -> {
            ChatCompletionsInput input;
            try {
                input = ChatCompletionsInput.defaultImageInput(baseImage);
                if (null != currentChatModel)
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
        if (defaultSelection >= 0)
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
                        if (null != currentEmbeddingsModel)
                            input.setModel(currentEmbeddingsModel);
                        List<Integer> inputIDs = new ArrayList<>();
                        inputIDs.add(item.landmarkID);
                        RestAccessLayer.requestLandmarkTextEmbeddings(
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

                        if (null != currentEmbeddingsModel)
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

        TextField serviceDirTextField = new TextField(RestAccessLayer.SERVICES_DEFAULT_PATH);
        serviceDirTextField.setEditable(false);
        Button browseServiceDirButton = new Button("Browse Services");
        browseServiceDirButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Browse to Desired Services Directory");
            File dir = dc.showDialog(null);
            if (null != dir) {
                RestAccessLayer.SERVICES_DEFAULT_PATH = dir.getAbsolutePath() + File.separator;
                Prompts.PROMPTS_DEFAULT_PATH = dir.getAbsolutePath();
                serviceDirTextField.setText(dir.getAbsolutePath() + File.separator);
            }
        });
        Button applyServiceDirButton = new Button("Reload Services");
        applyServiceDirButton.setOnAction(e -> {
            applyServiceDir();
        });

        HBox serviceDirHBox = new HBox(20, browseServiceDirButton, applyServiceDirButton);
        VBox serviceDirVBox = new VBox(10,
            new Label("Services Directory"), serviceDirTextField, serviceDirHBox);

        Spinner<Integer> batchSizeSpinner = new Spinner(1, 256, batchSize, 1);
        batchSizeSpinner.valueProperty().addListener(c -> {
            batchSize = batchSizeSpinner.getValue();
        });
        batchSizeSpinner.setEditable(true);
        batchSizeSpinner.setPrefWidth(100);

        Spinner<Integer> timeoutSpinner = new Spinner(1, 600, requestTimeoutMS/1000, 1);
        timeoutSpinner.valueProperty().addListener(c -> {
            requestTimeoutMS = timeoutSpinner.getValue() * 1000; //spinner is in seconds
            imageEmbeddingManager.setTimeoutMillis(requestTimeoutMS);
        });
        timeoutSpinner.setEditable(true);
        timeoutSpinner.setPrefWidth(100);

        Spinner requestDelaySpinner = new Spinner(1, 1000, requestDelay, 1);
        requestDelaySpinner.valueProperty().addListener(c -> {
            Double delay = (Double) requestDelaySpinner.getValue();
            requestDelay = delay.longValue();
            imageEmbeddingManager.setRequestDelayMillis(requestDelay);
        });
        requestDelaySpinner.setEditable(true);
        requestDelaySpinner.setPrefWidth(100);

        VBox requestsSpinnerVBox = new VBox(20,
            new VBox(5, new Label("Request Batch Size"), batchSizeSpinner),
            new VBox(5, new Label("Request Delay ms"), requestDelaySpinner)
        );

        Spinner<Integer> outstandingSpinner = new Spinner(1, 256, maxInFlightBatches, 1);
        outstandingSpinner.valueProperty().addListener(c -> {
            maxInFlightBatches = outstandingSpinner.getValue();
            imageEmbeddingManager.setMaxInFlight(maxInFlightBatches);
        });
        outstandingSpinner.setEditable(true);
        outstandingSpinner.setPrefWidth(100);
        
        VBox outstandingSpinnerVBox = new VBox(20,
            new VBox(5, new Label("Max In Flight Batches"), outstandingSpinner),
            new VBox(5, new Label("Request Timeout (Seconds)"), timeoutSpinner)
        );

        CheckBox enableJSONcheckBox = new CheckBox("Enable Special JSON Processing");
        enableJSONcheckBox.selectedProperty().addListener(e -> {
            EmbeddingsTextListItem.ENABLE_JSON_PROCESSING = enableJSONcheckBox.isSelected();
            enableJSONcheckBox.getScene().getRoot().fireEvent(
                new HyperdriveEvent(HyperdriveEvent.ENABLE_JSON_PROCESSING, enableJSONcheckBox.isSelected()));
        });
        Spinner<Integer> chunkSizeSpinner = new Spinner(256, 262144, chunkSize, 256);
        chunkSizeSpinner.valueProperty().addListener(c -> {
            chunkSize = chunkSizeSpinner.getValue();
            EmbeddingsTextListItem.LARGEFILE_SPLIT_SIZE = chunkSize;
            chunkSizeSpinner.getScene().getRoot().fireEvent(
                new HyperdriveEvent(HyperdriveEvent.SET_CHUNK_SIZE, chunkSize));
        });
        chunkSizeSpinner.setEditable(true);
        chunkSizeSpinner.setPrefWidth(100);

        VBox chunkingSpinnerVBox = new VBox(20,
            enableJSONcheckBox,
            new VBox(5, new Label("Chunk Size (bytes)"), chunkSizeSpinner)
        );

        GridPane servicesGrid = new GridPane(20, 10);
        servicesGrid.setPadding(new Insets(10));
        servicesGrid.setAlignment(Pos.TOP_LEFT);
        servicesTab.setContent(servicesGrid);
        servicesGrid.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOverDirectory(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        servicesGrid.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            event.consume();
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0); //only support the first
                if (file.isDirectory()) {
                    RestAccessLayer.SERVICES_DEFAULT_PATH = file.getAbsolutePath() + File.separator;
                    Prompts.PROMPTS_DEFAULT_PATH = file.getAbsolutePath();
                    serviceDirTextField.setText(file.getAbsolutePath() + File.separator);
                    applyServiceDir();
                }
            }
        });

        Button mergeButton = new Button("Merge FeatureCollections");
        mergeButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Browse and Select FeatureCollections to Merge");
            List<File> files = fc.showOpenMultipleDialog(getScene().getWindow());
            if(!files.isEmpty()) {
                ArrayList<FeatureCollection> collections = new ArrayList<>();
                for(File file : files) {
                    try {
                        if(FeatureCollectionFile.isFeatureCollectionFile(file)){
                            FeatureCollectionFile fcf = new FeatureCollectionFile(file.getAbsolutePath(), true);
                            collections.add(fcf.featureCollection);
                        }
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
                if(!collections.isEmpty()) {
                    FileChooser saver = new FileChooser();
                    saver.setTitle("Save " + collections.size() + " merged files as...");
                    File saveAsFile = saver.showSaveDialog(getScene().getWindow());
                    if(null != saveAsFile){
                        FeatureCollection merged = FeatureCollection.merge(collections);
                        FeatureCollectionFile mergedFile = new FeatureCollectionFile(saveAsFile.getAbsolutePath());
                        mergedFile.featureCollection = merged;
                        try {
                            mergedFile.writeContent();
                        } catch (IOException ex) {
                            LOG.error(null, ex);
                        }
                    }
                }
            }
        });
        VBox filesVBox = new VBox(10, mergeButton);
        
        filesTab.setContent(filesVBox);
        
        //Image Embeddings Service
        embeddingsLocationTextField = new TextField(
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
            if (null != selectedModel && !selectedModel.isBlank()) {
                currentEmbeddingsModel = selectedModel;
                imageBatchLauncher.setCurrentEmbeddingsModel(currentEmbeddingsModel);
            }
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
                inputIDs.add(EMBEDDINGS_IMAGE_TESTID);
                List<EmbeddingsImageUrl> inputs = new ArrayList<>();
                inputs.add(imageUrlFromImage.apply(ResourceUtils.load3DTextureImage("carl-b-portrait")));
                new RequestEmbeddingsTask(scene, currentEmbeddingsModel).
                    requestEmbeddings(inputs, inputIDs);
                notifyTerminalWarning("Sent Image Embeddings Request test using Carl-b.", scene);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });
        Button testTextEmbeddingButton = new Button("Test Text");
        testTextEmbeddingButton.setOnAction(e -> {
            try {
                EmbeddingsImageInput input = EmbeddingsImageInput.hellocarlTextEmbeddingsImageInput();
                if (null != currentEmbeddingsModel)
                    input.setModel(currentEmbeddingsModel);
                List<Integer> inputIDs = new ArrayList<>();
                inputIDs.add(EMBEDDINGS_TEXT_TESTID);
                RestAccessLayer.requestTextEmbeddings(input, scene, inputIDs, 666);
                notifyTerminalWarning("Sent Text Embeddings Request Test.", scene);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });

        //Chat Completion Service
        chatLocationTextField = new TextField(
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
            if (null != selectedModel && !selectedModel.isBlank())
                currentChatModel = selectedModel;
        });

        Button refreshChatModelsButton = new Button("Refresh");
        refreshChatModelsButton.setOnAction(e -> {
            RestAccessLayer.requestChatModels(refreshChatModelsButton.getScene());
        });
        Button testChatModelButton = new Button("Test Chat");
        testChatModelButton.setOnAction(e -> {
            ChatCompletionsInput input = ChatCompletionsInput.helloworldChatCompletionsInput();
            if (null != currentChatModel)
                input.setModel(currentChatModel);
            try {
                RestAccessLayer.requestChatCompletion(input, testChatModelButton.getScene(), CHAT_CHAT_TESTID, 9001);
                notifyTerminalWarning("Sent Chat Completions Test.", scene);
            } catch (JsonProcessingException ex) {
                LOG.error(null, ex);
            }
        });
        Button testVisionModelButton = new Button("Test Vision");
        testVisionModelButton.setOnAction(e -> {
            try {
                ChatCompletionsInput input = ChatCompletionsInput.hellocarlChatCompletionsInput();
                if (null != currentChatModel)
                    input.setModel(currentChatModel);
                RestAccessLayer.requestChatCompletion(input, testVisionModelButton.getScene(), CHAT_VISION_TESTID, 9001);
                notifyTerminalWarning("Sent Vision Test using Carl-b.", scene);
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
            chatStatusButton), 0, 1);

        servicesGrid.add(new VBox(5,
                new Label("Current Chat Model"),
                chatModelChoiceBox,
                new HBox(10, refreshChatModelsButton, testChatModelButton, testVisionModelButton)),
            1, 1);

        serviceDirVBox.setAlignment(Pos.CENTER_LEFT);
        servicesGrid.add(serviceDirVBox, 0, 2);

        Separator separator = new Separator();
        GridPane.setColumnSpan(separator, GridPane.REMAINING);
        servicesGrid.add(separator, 0, 3);

        requestsSpinnerVBox.setAlignment(Pos.TOP_LEFT);
        outstandingSpinnerVBox.setAlignment(Pos.TOP_LEFT);
        //@TODO SMP sorry for the magic numbers
        HBox requestsHBox = new HBox(75, requestsSpinnerVBox, outstandingSpinnerVBox);
        requestsHBox.setAlignment(Pos.TOP_LEFT);
        servicesGrid.add(requestsHBox, 0, 4);

        chunkingSpinnerVBox.setAlignment(Pos.CENTER_LEFT);
        servicesGrid.add(chunkingSpinnerVBox, 1, 4);

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
            AliveModels models = (AliveModels) event.object;
            embeddingsModelChoiceBox.getItems().clear();
            for (AiModel model : models.getAlive_models()) {
                embeddingsModelChoiceBox.getItems().add(model.getId());
            }
            if (embeddingsModelChoiceBox.getItems().contains(currentEmbeddingsModel)) {
                embeddingsModelChoiceBox.getSelectionModel().select(currentEmbeddingsModel);
            } else if (!embeddingsModelChoiceBox.getItems().isEmpty()) {
                embeddingsModelChoiceBox.getSelectionModel().selectFirst();
            }
        });
        scene.getRoot().addEventHandler(RestEvent.CHAT_MODELS_ALIVE, event -> {
            AliveModels models = (AliveModels) event.object;
            chatModelChoiceBox.getItems().clear();
            for (AiModel model : models.getAlive_models()) {
                chatModelChoiceBox.getItems().add(model.getId());
            }
            if (chatModelChoiceBox.getItems().contains(currentChatModel)) {
                chatModelChoiceBox.getSelectionModel().select(currentChatModel);
            } else if (!chatModelChoiceBox.getItems().isEmpty()) {
                chatModelChoiceBox.getSelectionModel().selectFirst();
            }
        });

        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_LANDMARKIMAGE, event -> {
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<Integer> inputIDs = (List<Integer>) event.object2;
            String msg = "Received " + output.getData().size() + " embeddings at "
                + format.format(LocalDateTime.now());

            int totalListItems = landmarkImageBuilderBox.getItems().size();
            for (int i = 0; i < output.getData().size(); i++) {
                if (i <= totalListItems) {
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
            outstandingRequests.put(output.getRequestNumber(), REQUEST_STATUS.SUCCEEDED);
            //@DEBUG SMP
            //System.out.println(msg);
            if (!outstandingRequests.containsValue(REQUEST_STATUS.REQUESTED)) {
                outstandingRequests.clear();
            }
        });

scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_IMAGE, event -> {
    EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
    List<Integer> inputIDs = (List<Integer>) event.object2;

    int totalListItems = outstandingRequests.size(); // Track total once at start
    // For each result, mark as SUCCEEDED in outstandingRequests
    for (int i = 0; i < output.getData().size(); i++) {
        int currentInputID = inputIDs.get(i);
        outstandingRequests.put(currentInputID, REQUEST_STATUS.SUCCEEDED);
        // (Optionally update imageEmbeddingsListView items here as before)
        EmbeddingsImageData currentOutput = output.getData().get(i);
        if (currentInputID == EMBEDDINGS_IMAGE_TESTID) {
            notifyTerminalSuccess("Image Embeddings Test Successful", scene);
        } else {        
            imageEmbeddingsListView.getItems()
            .filtered(fi -> fi.imageID == currentInputID)
            .forEach(item -> {
                item.setEmbeddings(currentOutput.getEmbedding());
                item.addMetaData("object", currentOutput.getObject());
                item.addMetaData("type", currentOutput.getType());
            });
        }
    }
    // Progress calculation
    long succeeded = outstandingRequests.values().stream().filter(s -> s == REQUEST_STATUS.SUCCEEDED).count();
    long failed = outstandingRequests.values().stream().filter(s -> s == REQUEST_STATUS.FAILED).count();
    long completed = succeeded + failed;

    imageEmbeddingRequestIndicator.setPercentComplete(completed / (double) totalListItems);
    imageEmbeddingRequestIndicator.setTopLabelLater("Received " + completed + " of " + totalListItems);

    // When ALL are done (succeeded or failed), clean up
    if (completed == totalListItems) {
        imageEmbeddingRequestIndicator.spin(false);
        imageEmbeddingRequestIndicator.fadeBusy(true);
        outstandingRequests.clear();
    }
});     
scene.getRoot().addEventHandler(RestEvent.ERROR_EMBEDDINGS_IMAGE, event -> {
    List<Integer> failedInputIDs = (List<Integer>) event.object;
    int totalListItems = outstandingRequests.size();

    for (Integer failedId : failedInputIDs) {
        outstandingRequests.put(failedId, REQUEST_STATUS.FAILED);
    }

    long succeeded = outstandingRequests.values().stream().filter(s -> s == REQUEST_STATUS.SUCCEEDED).count();
    long failed = outstandingRequests.values().stream().filter(s -> s == REQUEST_STATUS.FAILED).count();
    long completed = succeeded + failed;

    imageEmbeddingRequestIndicator.setPercentComplete(completed / (double) totalListItems);
    imageEmbeddingRequestIndicator.setTopLabelLater("Received " + completed + " of " + totalListItems);

    if (completed == totalListItems) {
        imageEmbeddingRequestIndicator.spin(false);
        imageEmbeddingRequestIndicator.fadeBusy(true);
        outstandingRequests.clear();
    }
});

        scene.getRoot().addEventHandler(RestEvent.ERROR_EMBEDDINGS_TEXT, event -> {
            List<File> inputFiles = (List<File>) event.object;
            int request = (int) event.object2;
            outstandingRequests.put(request, REQUEST_STATUS.FAILED);
            long totalRequests = outstandingRequests.entrySet().size();
            long remainingRequests = outstandingRequests.entrySet().stream()
                .filter(t -> t.getValue() == REQUEST_STATUS.REQUESTED).count();
            textEmbeddingRequestIndicator.setTopLabelLater("Received "
                + (totalRequests - remainingRequests) + " of " + totalRequests);
            if (!outstandingRequests.containsValue(REQUEST_STATUS.REQUESTED)) {
                outstandingRequests.clear();
                textEmbeddingRequestIndicator.spin(false);
                textEmbeddingRequestIndicator.fadeBusy(true);
            }
        });
        scene.getRoot().addEventHandler(RestEvent.NEW_CHAT_COMPLETION, event -> {
            ChatCompletionsOutput output = (ChatCompletionsOutput) event.object;
            //@DEBUG SMP
            //String msg = "Received " + output.getChoices().size() + " Chat Choices at " + LocalDateTime.now();
            outstandingRequests.put(output.getRequestNumber(), REQUEST_STATUS.SUCCEEDED);
            if (output.getInputID() == CHAT_CHAT_TESTID) {
                notifyTerminalSuccess("Chat Model Response Successful", scene);
                return;
            }
            if (output.getInputID() == CHAT_VISION_TESTID) {
                notifyTerminalSuccess("Vision Model Response Successful", scene);
                return;
            }
            imageEmbeddingsListView.getItems().stream()
                .filter(t -> t.imageID == output.getInputID())
                .forEach(item -> {
                    ChatCaptionResponse response = stringToChatCaptionResponse
                        .apply(output.getChoices().get(0).getRaw());
                    if (null != response) {
                        if (null != response.getCaption())
                            item.setFeatureVectorLabel(response.getCaption());
                        if (null != response.getDescription())
                            item.addDescription(response.getDescription());
                        if (null != response.getExplanation())
                            item.addExplanation(response.getExplanation());
                    }
                });
            outstandingRequests.remove(output.getRequestNumber());
            if (!outstandingRequests.containsValue(REQUEST_STATUS.REQUESTED)) {
                imageEmbeddingRequestIndicator.spin(false);
                imageEmbeddingRequestIndicator.fadeBusy(true);
            }
        });
        scene.getRoot().addEventHandler(RestEvent.ERROR_CHAT_COMPLETIONS, event -> {
            int request = (int) event.object2;
            outstandingRequests.put(request, REQUEST_STATUS.FAILED);
        });
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_TEXT, event -> {
            //Even though its a text embeddings event we reuse the same output data structure
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<Integer> inputIDs = (List<Integer>) event.object2;
            String msg = "Received " + output.getData().size() + " embeddings at "
                + format.format(LocalDateTime.now());

            int totalListItems = textEmbeddingsListView.getItems().size();
            for (int i = 0; i < output.getData().size(); i++) {
                if (i <= totalListItems) {
                    int currentInputID = inputIDs.get(i);
                    if (currentInputID == EMBEDDINGS_TEXT_TESTID) {
                        notifyTerminalSuccess("Image Embeddings Test Successful", scene);
                    } else {
                        EmbeddingsImageData currentOutput = output.getData().get(i);
                        textEmbeddingsListView.getItems().stream()
                            .filter(li -> li.textID == currentInputID)
                            .forEach(item -> {
                                item.setEmbeddings(currentOutput.getEmbedding());
                                item.addMetaData("object", currentOutput.getObject());
                                item.addMetaData("type", currentOutput.getType());
                            });
                    }
                }
            }

            outstandingRequests.put(output.getRequestNumber(), REQUEST_STATUS.SUCCEEDED);
            textEmbeddingRequestIndicator.setTopLabelLater(msg);
            int totalRequests = outstandingRequests.size();
            long remainingRequests = outstandingRequests.entrySet().stream()
                .filter(t -> t.getValue() == REQUEST_STATUS.REQUESTED).count();
            textEmbeddingRequestIndicator.setTopLabelLater("Received "
                + (totalRequests - remainingRequests) + " of " + totalRequests);
            if (!outstandingRequests.containsValue(REQUEST_STATUS.REQUESTED)) {
                outstandingRequests.clear();
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
            for (int i = 0; i < output.getData().size(); i++) {
                if (i <= totalListItems) {
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

            outstandingRequests.put(output.getRequestNumber(), REQUEST_STATUS.SUCCEEDED);
            textEmbeddingRequestIndicator.setTopLabelLater(msg);
            //@DEBUG SMP
            //System.out.println(msg);
            outstandingRequests.remove(output.getRequestNumber());
            if (!outstandingRequests.containsValue(REQUEST_STATUS.REQUESTED)) {
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
                imageFilesCountLabel.setText(String.valueOf(imageEmbeddingsListView.getItems().size()));
                if (!imageEmbeddingsListView.getItems().isEmpty()) {
                    //trigger baseImageView to change
                    imageEmbeddingsListView.getSelectionModel().selectFirst();
                } else {
                    baseImage = waitingImage;
                    baseImageView.setImage(baseImage);
                }
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });
        scene.getRoot().addEventHandler(HyperdriveEvent.NEW_BATCH_IMAGELOAD, event -> {
            if (null != event.object1) {
                List<EmbeddingsImageListItem> newItems = (List<EmbeddingsImageListItem>) event.object1;
                imageEmbeddingsListView.getItems().addAll(newItems);
            }
            if (null != event.object2) {
                ArrayList<File> newImageFiles = (ArrayList<File>) event.object2;
                imageFilesList.addAll(newImageFiles);
            }
            imageFilesCountLabel.setText(String.valueOf(imageEmbeddingsListView.getItems().size()));
            if (!imageEmbeddingsListView.getItems().isEmpty()) {
                //trigger baseImageView to change
                imageEmbeddingsListView.getSelectionModel().selectFirst();
            } else {
                baseImage = waitingImage;
                baseImageView.setImage(baseImage);
            }
        });
        scene.getRoot().addEventHandler(HyperdriveEvent.NEW_BATCH_TEXTLOAD, event -> {
            if (null != event.object1) {
                List<EmbeddingsTextListItem> newItems = (List<EmbeddingsTextListItem>) event.object1;
                textEmbeddingsListView.getItems().addAll(newItems);
            }
            if (null != event.object2) {
                ArrayList<File> newImageFiles = (ArrayList<File>) event.object2;
                textFilesList.addAll(newImageFiles);
            }
            textFilesCountLabel.setText(String.valueOf(textFilesList.size()));

            if (!textEmbeddingsListView.getItems().isEmpty()) {
                textEmbeddingsListView.getSelectionModel().selectFirst();
            } else {
                baseTextArea.clear();
            }
        });

        getStyleClass().add("hyperdrive-pane");
        // Initialize the batch launcher with current Scene and model
        imageBatchLauncher = new ImageEmbeddingsBatchLauncher(scene, currentEmbeddingsModel);
        // Instantiate the manager
        imageEmbeddingManager = new BatchRequestManager<>(
            maxInFlightBatches,   // Maximum concurrent requests 
            requestTimeoutMS,                    // Timeout in ms (e.g., 60s)
            3,                        // Maximum retries per batch
            batchNumber::getAndIncrement,       // batchNumber supplier
            requestNumber::getAndIncrement, // Unique request ID supplier
            // Task Factory: launch a batch
            (batch, batchNum, reqId) -> () -> {
                Platform.runLater(() -> {
                    if (imageEmbeddingRequestIndicator != null) {
                        imageEmbeddingRequestIndicator.setFadeTimeMS(250);
                        imageEmbeddingRequestIndicator.spin(true);
                        if(!imageEmbeddingRequestIndicator.inView())
                            imageEmbeddingRequestIndicator.fadeBusy(false);
                        imageEmbeddingRequestIndicator.setLabelLater(
                            "Encoding and sending Batch: " + batchNum 
                            + " of " + imageEmbeddingManager.getTotalBatches()
                        );                                 
                    }           
                });    
                imageBatchLauncher.launchBatch(batch, batchNum, reqId, (success, ex) -> {
                    if (success) {
                        imageEmbeddingManager.completeSuccess(reqId, batchNum, batch, 0);
                    } else {
                        imageEmbeddingManager.completeFailure(reqId, batchNum, batch, 0, ex);
                    }
                });
            },
            // onComplete: (success/failure/timeout)
            result -> {
                //@DEBUG SMP 
                System.out.println("Batch: " + result.getBatchNumber() 
                    + " Request: " + result.getRequestId() 
                    + " Status: " + result.getStatus()
                    + " Attempt: " + result.getRetryCount()
                    + " In Flight: " + imageEmbeddingManager.getInFlight()
                );
                System.out.println(" Duration: " + imageEmbeddingManager.getBatchDurationByID(result.getRequestId())/1000 + " s"
                    + " Avg Duration: " + new DecimalFormat("#.000").format(
                        imageEmbeddingManager.getAvgBatchDurationMillis()/1000) + " s"
                    + " Total Duration: " + imageEmbeddingManager.getTotalBatchDurationMillis()/1000 + " s"
                );
                
                Platform.runLater(() -> {
                    // update progress indicator, show alerts on failure, etc.
                    long failed = outstandingRequests.values().stream().filter(s -> s == REQUEST_STATUS.FAILED).count();
                    imageEmbeddingRequestIndicator.setLabelLater(
                        "Batches completed: " + imageEmbeddingManager.getBatchesCompleted() 
                        + " of " + imageEmbeddingManager.getTotalBatches()
                        + (failed > 0 ? (" | Errors: " + failed) : "")
                    );                    
                });
            }
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (this != null) {
                imageEmbeddingManager.shutdown();
            }
        }));                
    }

    private void applyServiceDir() {
        try {
            RestAccessLayer.loadDefaultRestConfig();
            embeddingsLocationTextField.setText(
                RestAccessLayer.restAccessLayerconfig.getBaseRestURL() +
                    RestAccessLayer.restAccessLayerconfig.getImageEmbeddingsEndpoint()
            );
            chatLocationTextField.setText(
                RestAccessLayer.restAccessLayerconfig.getBaseRestURL() +
                    RestAccessLayer.restAccessLayerconfig.getChatCompletionEndpoint()
            );

        } catch (IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Check services directory path");
            alert.setContentText("Error loading from: \n" + RestAccessLayer.SERVICES_DEFAULT_PATH);
            alert.setGraphic(ResourceUtils.loadIcon("error", 75));
            alert.initStyle(StageStyle.TRANSPARENT);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setBackground(Background.EMPTY);
            dialogPane.getScene().setFill(Color.TRANSPARENT);
            String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
            dialogPane.getStylesheets().add(DIALOGCSS);
            alert.showAndWait();
        }
    }

    public void chooseCaptionsTask(List<EmbeddingsImageListItem> items, List<String> choices) {
        ChooseCaptionsTask requestTask = new ChooseCaptionsTask(scene,
            imageEmbeddingRequestIndicator, requestNumber, currentChatModel,
            outstandingRequests, items, choices);
        Thread t = new Thread(requestTask, "Trinity Image Auto-choose Captions Request");
        t.setDaemon(true);
        t.start();
    }

    public void requestTextLandmarkSimilarityTask(
        List<EmbeddingsTextListItem> items, List<FeatureVector> landmarkFeatures) {
        Metric metric = Metric.getMetric(metricChoiceBox.getSelectionModel().getSelectedItem());
        RequestTextLandmarkSimilarityTask requestTask =
            new RequestTextLandmarkSimilarityTask(scene, textEmbeddingRequestIndicator,
                items, landmarkFeatures, metric);
        Thread t = new Thread(requestTask, "Trinity Text Landmark Similarity Task");
        t.setDaemon(true);
        t.start();
    }

    public void requestLandmarkSimilarityTask(
        List<EmbeddingsImageListItem> items, List<FeatureVector> landmarkFeatures) {
        Metric metric = Metric.getMetric(metricChoiceBox.getSelectionModel().getSelectedItem());
        RequestLandmarkSimilarityTask requestTask =
            new RequestLandmarkSimilarityTask(scene, imageEmbeddingRequestIndicator,
                items, landmarkFeatures, metric);
        Thread t = new Thread(requestTask, "Trinity Image Landmark Similarity Task");
        t.setDaemon(true);
        t.start();
    }

    public void requestCaptionsTask(List<EmbeddingsImageListItem> items) {
        RequestCaptionsTask requestTask = new RequestCaptionsTask(
            scene, imageEmbeddingRequestIndicator, requestNumber,
            currentChatModel, outstandingRequests, items);
        Thread t = new Thread(requestTask, "Trinity Image Captioning Request");
        t.setDaemon(true);
        t.start();
    }

    public void loadTextTask(List<File> files) {
        currentTextFeatureList.clear();
        textFilesList.clear();
        LoadTextTask loadTask = new LoadTextTask(scene, textEmbeddingRequestIndicator, files);
        Thread t = new Thread(loadTask, "Trinity Batch Text File Load Task");
        t.setDaemon(true);
        t.start();
    }

    public void requestTextEmbeddingsTask() {
        RequestTextEmbeddingsTask requestTask = new RequestTextEmbeddingsTask(
            scene, textEmbeddingRequestIndicator, requestNumber, currentEmbeddingsModel,
            outstandingRequests, textEmbeddingsListView.getSelectionModel().getSelectedItems());
        Thread t = new Thread(requestTask, "Trinity Embeddings Text Request");
        t.setDaemon(true);
        t.start();
    }

    public void loadImagesTask(List<File> files) {
        currentFeatureList.clear();
        imageFilesList.clear();
        LoadImagesTask loadTask = new LoadImagesTask(scene,
            imageEmbeddingRequestIndicator, renderIconsCheckBox.isSelected(), files);
        Thread t = new Thread(loadTask, "Trinity Batch Image Load Task");
        t.setDaemon(true);
        t.start();
    }

    public void requestEmbeddingsTask() {
        RequestEmbeddingsTask task = new RequestEmbeddingsTask(scene,
            imageEmbeddingRequestIndicator, requestNumber, currentEmbeddingsModel,
            outstandingRequests, imageEmbeddingsListView.getSelectionModel().getSelectedItems());
        Thread t = new Thread(task, "Trinity Embeddings Image Request");
        t.setDaemon(true);
        t.start();
    }
}
