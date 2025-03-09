/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.VectorMaskCollection;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static edu.jhuapl.trinity.data.messages.xai.FeatureVector.bboxToString;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class NavigatorPane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(NavigatorPane.class);
    BorderPane bp;
    public static double DEFAULT_FIT_WIDTH = 512;
    public static double DEFAULT_TITLEDPANE_WIDTH = 256;
    public static double DEFAULT_LABEL_WIDTH = 64;
    public static int PANE_WIDTH = 600;
    public static int PANE_HEIGHT = 850;

    public String imageryBasePath = "imagery/";
    boolean auto = false;
    Image currentImage = null;
    Label imageLabel;
    Label urlLabel;
    TitledPane detailsTP;
    TitledPane metaTP;
    GridPane detailsGridPane;
    ImageView imageView;
    VBox contentVBox;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        MediaView mediaView = new MediaView();
        bpOilSpill.setCenter(mediaView);
        return bpOilSpill;
    }

    public NavigatorPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Content Navigator", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;

        Image waitingImage = ResourceUtils.loadIconFile("waitingforimage");
        imageView = new ImageView(waitingImage);
        imageView.setFitWidth(DEFAULT_FIT_WIDTH);
        imageView.setFitHeight(DEFAULT_FIT_WIDTH);
        imageView.setPreserveRatio(true);

        urlLabel = new Label("Waiting for Image");
        urlLabel.setMaxWidth(DEFAULT_FIT_WIDTH);
        urlLabel.setTooltip(new Tooltip("Waiting for Image"));
        imageLabel = new Label("No Label");
        imageLabel.setMaxWidth(DEFAULT_FIT_WIDTH);
        Button hypersurfaceButton = new Button("Hypersurface");
        hypersurfaceButton.setOnAction(e -> {
            hypersurfaceButton.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_HYPERSURFACE, true));
            hypersurfaceButton.getScene().getRoot().fireEvent(
                new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, currentImage));
        });
        Button imageInspectionButton = new Button("Image Inspection");
        imageInspectionButton.setOnAction(e -> {
            imageInspectionButton.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_IMAGE_INSPECTION, true));
            imageInspectionButton.getScene().getRoot().fireEvent(
                new ImageEvent(ImageEvent.NEW_IMAGE_INSPECTION, currentImage));
        });
        
        detailsGridPane = new GridPane();
        detailsGridPane.setPadding(new Insets(1));
        detailsGridPane.setHgap(5);
        detailsTP = new TitledPane("Details", detailsGridPane);
        detailsTP.setExpanded(false);
        detailsTP.setPrefWidth(DEFAULT_TITLEDPANE_WIDTH);
        metaTP = new TitledPane();
        metaTP.setText("Metadata");
        metaTP.setExpanded(false);
        metaTP.setPrefWidth(DEFAULT_TITLEDPANE_WIDTH);
        contentVBox = new VBox(5, 
            imageView, urlLabel, imageLabel,
            new HBox(10, hypersurfaceButton, imageInspectionButton), 
            detailsTP, metaTP);

        ImageView refresh = ResourceUtils.loadIcon("refresh", 32);

        VBox refreshVBox = new VBox(1, refresh, new Label("Refresh"));
        refreshVBox.setAlignment(Pos.BOTTOM_CENTER);

        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setOffsetX(4);
        innerShadow.setOffsetY(4);
        innerShadow.setColor(Color.CYAN);

        refreshVBox.setOnMouseEntered(e -> {
            refresh.setEffect(innerShadow);
        });
        refreshVBox.setOnMouseClicked(e -> {
            toggleAuto();
            if (auto)
                refreshVBox.setEffect(innerShadow);
            else
                refreshVBox.setEffect(null);
        });
        refreshVBox.setOnMouseExited(e -> {
            refresh.setEffect(null);
        });

        bp.setCenter(contentVBox);

        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> {
            this.imageryBasePath = (String) e.object;
        });
        scene.addEventHandler(ImageEvent.NEW_VECTORMASK_COLLECTION, e -> {
            VectorMaskCollection vmc = (VectorMaskCollection) e.object;

        });
        scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            FeatureVector fv = (FeatureVector) e.object;
            if (null != fv.getLabel())
                imageLabel.setText(fv.getLabel());
            else
                imageLabel.setText("No Label");
            if (null != fv.getImageURL()) {
                try {
                    File file = new File(imageryBasePath + fv.getImageURL());
                    currentImage = new Image(file.toURI().toURL().toExternalForm());
                    imageView.setImage(currentImage);
                    urlLabel.setText(fv.getImageURL());
                    urlLabel.setTooltip(new Tooltip(file.toURI().toURL().toExternalForm()));
                    createDetails(fv);
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        getScene().getRoot().fireEvent(
                            new CommandTerminalEvent("Unable to load Image, check Path.",
                                new Font("Consolas", 20), Color.RED));
                    });

                    LOG.error(null, ex);
                }
            }
        });
    }

    private void createDetails(FeatureVector featureVector) {
        detailsGridPane.getChildren().clear();
        detailsGridPane.addRow(0, new Label("imageURL"),
            new Label(featureVector.getImageURL()));
        String bboxStr = "";
        if (null != featureVector.getBbox())
            bboxStr = bboxToString(featureVector);
        Label bboxLabel = new Label("bbox");
        bboxLabel.setMinWidth(DEFAULT_LABEL_WIDTH);
        detailsGridPane.addRow(1, bboxLabel, new Label(bboxStr));
        Label frameLabel = new Label("frameId");
        frameLabel.setMinWidth(DEFAULT_LABEL_WIDTH);
        detailsGridPane.addRow(2, frameLabel, new Label(String.valueOf(featureVector.getFrameId())));
        Label scoreLabel = new Label("score");
        scoreLabel.setMinWidth(DEFAULT_LABEL_WIDTH);
        detailsGridPane.addRow(3, scoreLabel, new Label(String.valueOf(featureVector.getScore())));
        Label layerLabel = new Label("layer");
        layerLabel.setMinWidth(DEFAULT_LABEL_WIDTH);
        detailsGridPane.addRow(4, layerLabel, new Label(String.valueOf(featureVector.getLayer())));
        Label messageLabel = new Label("messageId");
        messageLabel.setMinWidth(DEFAULT_LABEL_WIDTH);
        detailsGridPane.addRow(5, messageLabel, new Label(String.valueOf(featureVector.getLayer())));

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        Text metaText = new Text(sb.toString());
        metaText.setWrappingWidth(50); //something smallish just to initialize
        metaText.wrappingWidthProperty().bind(metaTP.widthProperty().subtract(10));
        metaText.setFont(new Font("Consolas", 18));
        metaText.setStroke(Color.ALICEBLUE);
        metaTP.setContent(metaText);
    }

    public void shutdown() {
        close();
        parent.getChildren().remove(this);
    }

    public void toggleAuto() {
        auto = !auto;
    }

    public void setImage(Image image) {
        currentImage = image;
        imageView.setImage(currentImage);
    }
}
