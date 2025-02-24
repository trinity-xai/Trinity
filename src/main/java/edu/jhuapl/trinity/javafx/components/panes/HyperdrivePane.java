/* Copyright (C) 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageInput;
import edu.jhuapl.trinity.data.messages.EmbeddingsImageOutput;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.messages.RestAccessLayer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import static edu.jhuapl.trinity.utils.MessageUtils.embeddingsToFeatureVector;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;



/**
 * @author Sean Phillips
 */
public class HyperdrivePane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(HyperdrivePane.class);
    public static int PANE_WIDTH = 1100;
    public static int PANE_HEIGHT = 575;
    private double imageSize = 512;
    private Color fillColor = Color.SLATEGREY;
    public BorderPane borderPane;
    private TilePane tilePane;
    private BorderPane imageViewBorderPane;
    BorderPane imageFFTBorderPane;
    public StackPane centerStack;
    public Image baseImage;
    public ImageView baseImageView;
    ScrollPane scrollPane;
    List<FeatureVector> currentFeatureList;   
    List<File> imageFilesList;
//    ListView<EmbeddingsListItem> embeddingsListView;
    ListView<String> embeddingsListView;
    
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public HyperdrivePane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Image Inspector", "", 300.0, 400.0);
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
        tileScrollPane.setFitToHeight(true);
        tileScrollPane.setFitToWidth(true);

        borderPane.setCenter(tileScrollPane);

        centerStack = new StackPane();
        centerStack.setAlignment(Pos.CENTER);
        imageViewBorderPane = new BorderPane(centerStack);

        try {
            baseImage = (ResourceUtils.load3DTextureImage("carl-b-portrait"));
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(HyperdrivePane.class.getName()).log(Level.SEVERE, null, ex);
            baseImage = ResourceUtils.loadIconFile("waitingforimage");
        }
        
        baseImageView = new ImageView(baseImage);
        baseImageView.setPreserveRatio(true);
        scrollPane = new ScrollPane(baseImageView);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(512, 512);
        centerStack.getChildren().add(scrollPane);

        Button runFFTButton = new Button("Check Status");
        runFFTButton.setOnAction(e -> {
            RestAccessLayer.requestRestIsAlive(runFFTButton.getScene());
        });

        Button embeddingsButton = new Button("Request Embeddings");
        embeddingsButton.setOnAction(e -> {
            EmbeddingsImageInput input = new EmbeddingsImageInput();
            try {
                input.setInput(EmbeddingsImageInput.BASE64_PREFIX_PNG 
                        + ResourceUtils.imageToBase64(baseImage));
                input.setDimensions(Double.valueOf(baseImage.getWidth()).intValue());
                input.setEmbedding_type("all");
                input.setEncoding_format("float");
                input.setModel("openai/clip-vit-large-patch14");
                input.setUser("string");
                RestAccessLayer.requestImageEmbeddings(input, embeddingsButton.getScene());
            } catch (JsonProcessingException ex) {
                java.util.logging.Logger.getLogger(HyperdrivePane.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(HyperdrivePane.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        HBox powerBottomHBox = new HBox(10, embeddingsButton, runFFTButton);
        powerBottomHBox.setPadding(new Insets(10));
        powerBottomHBox.setAlignment(Pos.CENTER);
        imageViewBorderPane.setBottom(powerBottomHBox);

        baseImageView.setOnMouseEntered(e -> baseImageView.setCursor(Cursor.CROSSHAIR));
        baseImageView.setOnMouseExited(e -> baseImageView.setCursor(Cursor.DEFAULT));


        Button clearMaskButton = new Button("Clear Mask");
        clearMaskButton.setOnAction(e -> {
//            clearMask();
        });
        HBox controlsBox = new HBox(10,
            clearMaskButton
        );
        controlsBox.setAlignment(Pos.CENTER);
        ImageView iv = ResourceUtils.loadIcon("data", 50);
        HBox placeholder = new HBox(10, iv, new Label("No Data Sources Marked"));
        placeholder.setAlignment(Pos.CENTER);
        embeddingsListView = new ListView<>();
        embeddingsListView.setPlaceholder(placeholder);
        ScrollPane imageFFTScrollPane = new ScrollPane(embeddingsListView);
        imageFFTScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageFFTScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
//        imageFFTScrollPane.setPannable(true);
        imageFFTScrollPane.setFitToHeight(true);
        imageFFTScrollPane.setFitToWidth(true);
        imageFFTScrollPane.setPrefSize(512, 512);
        imageFFTBorderPane = new BorderPane(imageFFTScrollPane);
        imageFFTBorderPane.setBottom(controlsBox);

        tilePane.getChildren().addAll(imageViewBorderPane, imageFFTBorderPane);

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
                currentFeatureList.clear();
                imageFilesList.clear();
                final File file = db.getFiles().get(0);
                if (JavaFX3DUtils.isTextureFile(file)) {
                    try {
                        setImage(new Image(file.toURI().toURL().toExternalForm()));
                    } catch (MalformedURLException ex) {
                        java.util.logging.Logger.getLogger(PixelSelectionPane.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                imageFilesList = db.getFiles().stream()
                    .filter(f-> JavaFX3DUtils.isTextureFile(f))
                    .toList();
            }
        });
        scene.getRoot().addEventHandler(RestEvent.NEW_EMBEDDINGS_IMAGE, event -> {
            EmbeddingsImageOutput output = (EmbeddingsImageOutput) event.object;
            List<FeatureVector> fvList = output.getData().stream()
                .map(embeddingsToFeatureVector).toList();
//            fvList.forEach(fv -> fv.setMediaURL(baseImage.getUrl()));
            if(fvList.size() >= imageFilesList.size()) {
                for(int imageIndex=0; imageIndex<fvList.size();imageIndex++){
                    try {
                        fvList.get(imageIndex).setMediaURL(
                            imageFilesList.get(imageIndex).toURI().toURL().toExternalForm());
                    } catch (MalformedURLException ex) {
                        java.util.logging.Logger.getLogger(HyperdrivePane.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            currentFeatureList.clear();
            currentFeatureList.addAll(fvList);
            System.out.println("New Feature Vector List obtained.");
        });
    }

    public void setImage(Image image) {
        this.baseImage = image;
        baseImageView.setImage(this.baseImage);
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
        int height = Double.valueOf(baseImage.getHeight()).intValue();
        int width = Double.valueOf(baseImage.getWidth()).intValue();
        imageSize = width;
    }
}
