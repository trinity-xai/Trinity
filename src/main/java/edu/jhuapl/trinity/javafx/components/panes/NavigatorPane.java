package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.VectorMaskCollection;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceEvent;
import edu.jhuapl.trinity.javafx.javafx3d.hypersurface.SurfaceInspection;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static edu.jhuapl.trinity.data.messages.xai.FeatureVector.bboxToString;

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

    public String imageryBasePath = "";
    boolean auto = false;
    Image currentImage = null;
    Label imageLabel;
    Label urlLabel;
    TitledPane detailsTP;
    TitledPane metaTP;
    GridPane detailsGridPane;
    ImageView imageView;
    StackPane imageStack;
    Pane imageOverlayPane;
    Line imageCrosshairHorizontal;
    Line imageCrosshairVertical;
    Circle imagePositionMarker;
    Rectangle imageTileOutline;
    TitledPane surfaceTP;
    GridPane surfaceGridPane;
    Label surfacePositionKey;
    Label surfacePixelValue;
    Label surfaceRenderCellValue;
    Label surfaceValueValue;
    Label surfaceRowRangeValue;
    Label surfaceColumnRangeValue;
    Label surfaceWorldValue;
    Label surfaceRgbValue;
    Label surfaceIntensityValue;
    Label surfaceTileValue;
    Label surfaceLodValue;
    Label surfaceTileBoundsValue;
    SurfaceInspection currentSurfaceInspection;
    TextArea textArea;
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

        imageOverlayPane = new Pane();
        imageOverlayPane.setMouseTransparent(true);
        imageOverlayPane.setPickOnBounds(false);

        imageCrosshairHorizontal = new Line();
        imageCrosshairHorizontal.setStroke(Color.CYAN);
        imageCrosshairHorizontal.setStrokeWidth(1.25);
        imageCrosshairHorizontal.setMouseTransparent(true);
        imageCrosshairVertical = new Line();
        imageCrosshairVertical.setStroke(Color.CYAN);
        imageCrosshairVertical.setStrokeWidth(1.25);
        imageCrosshairVertical.setMouseTransparent(true);

        imagePositionMarker = new Circle(4.0, Color.TRANSPARENT);
        imagePositionMarker.setStroke(Color.WHITE);
        imagePositionMarker.setStrokeWidth(1.5);
        imagePositionMarker.setMouseTransparent(true);

        imageTileOutline = new Rectangle();
        imageTileOutline.setFill(Color.TRANSPARENT);
        imageTileOutline.setStroke(Color.YELLOW);
        imageTileOutline.setStrokeWidth(1.5);
        imageTileOutline.getStrokeDashArray().setAll(8.0, 5.0);
        imageTileOutline.setMouseTransparent(true);

        imageOverlayPane.getChildren().addAll(
            imageTileOutline, imageCrosshairHorizontal, imageCrosshairVertical, imagePositionMarker);
        setImageInspectionOverlayVisible(false);

        imageStack = new StackPane(imageView, imageOverlayPane);
        imageStack.setAlignment(Pos.CENTER);
        imageStack.setPrefSize(DEFAULT_FIT_WIDTH, DEFAULT_FIT_WIDTH);
        imageStack.setMinSize(DEFAULT_FIT_WIDTH, DEFAULT_FIT_WIDTH);
        imageStack.setMaxSize(DEFAULT_FIT_WIDTH, DEFAULT_FIT_WIDTH);
        imageStack.widthProperty().addListener((obs, oldValue, newValue) -> refreshImageInspectionOverlay());
        imageStack.heightProperty().addListener((obs, oldValue, newValue) -> refreshImageInspectionOverlay());

        textArea = new TextArea();
        textArea.setPrefWidth(DEFAULT_FIT_WIDTH);
        textArea.setPrefHeight(DEFAULT_FIT_WIDTH);

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

        surfaceGridPane = new GridPane();
        surfaceGridPane.setPadding(new Insets(4));
        surfaceGridPane.setHgap(8);
        surfaceGridPane.setVgap(2);
        surfacePositionKey = new Label("Source Pixel");
        surfacePositionKey.setMinWidth(100);
        surfacePixelValue = new Label("-");
        surfacePixelValue.setWrapText(true);
        surfaceGridPane.addRow(0, surfacePositionKey, surfacePixelValue);
        surfaceRenderCellValue = addSurfaceRow(1, "Render Cell");
        surfaceValueValue = addSurfaceRow(2, "Value");
        surfaceRowRangeValue = addSurfaceRow(3, "Row Range");
        surfaceColumnRangeValue = addSurfaceRow(4, "Column Range");
        surfaceWorldValue = addSurfaceRow(5, "World XYZ");
        surfaceRgbValue = addSurfaceRow(6, "RGB");
        surfaceIntensityValue = addSurfaceRow(7, "Intensity");
        surfaceTileValue = addSurfaceRow(8, "Tile");
        surfaceLodValue = addSurfaceRow(9, "LOD");
        surfaceTileBoundsValue = addSurfaceRow(10, "Tile Bounds");
        surfaceTP = new TitledPane("Surface", surfaceGridPane);
        surfaceTP.setExpanded(true);
        surfaceTP.setPrefWidth(DEFAULT_TITLEDPANE_WIDTH);
        surfaceTP.setVisible(false);
        surfaceTP.setManaged(false);

        Tab imageTab = new Tab("Image");
        imageTab.setClosable(false);
        imageTab.setContent(imageStack);
        Tab textTab = new Tab("Text");
        textTab.setClosable(false);
        textTab.setContent(textArea);
        TabPane tabPane = new TabPane(imageTab, textTab);

        contentVBox = new VBox(5,
            tabPane, urlLabel, imageLabel,
            new HBox(10, hypersurfaceButton, imageInspectionButton),
            surfaceTP, detailsTP, metaTP);

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
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        getScene().getRoot().fireEvent(
                            new CommandTerminalEvent("Unable to load Image, check Path.",
                                new Font("Consolas", 20), Color.RED));
                    });

                    LOG.error(null, ex);
                }
            }
            if (null != fv.getText()) {
                textArea.setText(fv.getText());
            }
            createDetails(fv);
        });
        scene.addEventHandler(HypersurfaceEvent.SURFACE_SOURCE_IMAGE_CHANGED, e -> {
            if (e.object instanceof Image image) {
                setImage(image);
                imageLabel.setText("Hypersurface Source");
                String imageUrl = image.getUrl();
                if (imageUrl != null && !imageUrl.isBlank()) {
                    urlLabel.setText(imageUrl);
                    urlLabel.setTooltip(new Tooltip(imageUrl));
                } else {
                    String dimensions = (int) image.getWidth() + " x " + (int) image.getHeight();
                    urlLabel.setText(dimensions);
                    urlLabel.setTooltip(new Tooltip("Hypersurface source image: " + dimensions));
                }
            }
        });
        scene.addEventHandler(HypersurfaceEvent.SURFACE_INSPECTION_UPDATED, e -> {
            if (e.object instanceof SurfaceInspection inspection) {
                updateSurfaceInspection(inspection);
            } else {
                clearSurfaceInspection();
            }
        });
    }

    private Label addSurfaceRow(int row, String name) {
        Label key = new Label(name);
        key.setMinWidth(100);
        Label value = new Label("-");
        value.setWrapText(true);
        surfaceGridPane.addRow(row, key, value);
        return value;
    }

    private void updateSurfaceInspection(SurfaceInspection inspection) {
        currentSurfaceInspection = inspection;
        surfaceTP.setManaged(true);
        surfaceTP.setVisible(true);

        int displayX = inspection.imageBacked() ? inspection.imageColumn() : inspection.sourceColumn();
        int displayY = inspection.imageBacked() ? inspection.imageRow() : inspection.sourceRow();
        surfacePositionKey.setText(inspection.imageBacked() ? "Source Pixel" : "Source Cell");
        surfacePixelValue.setText(displayX + ", " + displayY);
        surfaceRenderCellValue.setText(inspection.renderColumn() + ", " + inspection.renderRow());
        surfaceValueValue.setText(formatNumber(inspection.surfaceValue()));
        surfaceRowRangeValue.setText(formatRange(inspection.rowMinimum(), inspection.rowMaximum()));
        surfaceColumnRangeValue.setText(formatRange(
            inspection.columnMinimum(), inspection.columnMaximum()));
        surfaceWorldValue.setText(formatNumber(inspection.worldX()) + ", "
            + formatNumber(inspection.worldY()) + ", " + formatNumber(inspection.worldZ()));

        if (inspection.hasImageSample()) {
            surfaceRgbValue.setText(inspection.red() + ", " + inspection.green() + ", "
                + inspection.blue());
            surfaceIntensityValue.setText(formatNumber(inspection.imageIntensity()));
        } else {
            surfaceRgbValue.setText("-");
            surfaceIntensityValue.setText("-");
        }

        if (inspection.hasTile()) {
            surfaceTileValue.setText(Integer.toString(inspection.tileId()));
            surfaceLodValue.setText(inspection.activeLod() >= 0
                ? "L" + inspection.activeLod() : "pending");
            surfaceTileBoundsValue.setText(
                inspection.tileImageMinX() + "-" + (inspection.tileImageMaxXExclusive() - 1)
                    + " x " + inspection.tileImageMinY() + "-"
                    + (inspection.tileImageMaxYExclusive() - 1));
        } else {
            surfaceTileValue.setText("-");
            surfaceLodValue.setText("-");
            surfaceTileBoundsValue.setText("-");
        }
        refreshImageInspectionOverlay();
    }

    private void clearSurfaceInspection() {
        currentSurfaceInspection = null;
        surfaceTP.setVisible(false);
        surfaceTP.setManaged(false);
        setImageInspectionOverlayVisible(false);
    }

    private void refreshImageInspectionOverlay() {
        SurfaceInspection inspection = currentSurfaceInspection;
        Image image = imageView.getImage();
        if (inspection == null || !inspection.imageBacked() || image == null
            || !(image.getWidth() > 0.0) || !(image.getHeight() > 0.0)) {
            setImageInspectionOverlayVisible(false);
            return;
        }

        double stackWidth = imageStack.getWidth() > 0.0
            ? imageStack.getWidth() : DEFAULT_FIT_WIDTH;
        double stackHeight = imageStack.getHeight() > 0.0
            ? imageStack.getHeight() : DEFAULT_FIT_WIDTH;
        double scale = Math.min(
            imageView.getFitWidth() / image.getWidth(),
            imageView.getFitHeight() / image.getHeight());
        if (!Double.isFinite(scale) || !(scale > 0.0)) {
            setImageInspectionOverlayVisible(false);
            return;
        }

        double displayedWidth = image.getWidth() * scale;
        double displayedHeight = image.getHeight() * scale;
        double originX = (stackWidth - displayedWidth) * 0.5;
        double originY = (stackHeight - displayedHeight) * 0.5;

        double markerX = originX
            + ((inspection.imageColumn() + 0.5) / image.getWidth()) * displayedWidth;
        double markerY = originY
            + ((inspection.imageRow() + 0.5) / image.getHeight()) * displayedHeight;
        markerX = clamp(markerX, originX, originX + displayedWidth);
        markerY = clamp(markerY, originY, originY + displayedHeight);

        imageCrosshairHorizontal.setStartX(originX);
        imageCrosshairHorizontal.setEndX(originX + displayedWidth);
        imageCrosshairHorizontal.setStartY(markerY);
        imageCrosshairHorizontal.setEndY(markerY);
        imageCrosshairVertical.setStartX(markerX);
        imageCrosshairVertical.setEndX(markerX);
        imageCrosshairVertical.setStartY(originY);
        imageCrosshairVertical.setEndY(originY + displayedHeight);
        imagePositionMarker.setCenterX(markerX);
        imagePositionMarker.setCenterY(markerY);

        if (inspection.hasTile()) {
            double tileMinX = clamp(inspection.tileImageMinX(), 0.0, image.getWidth());
            double tileMinY = clamp(inspection.tileImageMinY(), 0.0, image.getHeight());
            double tileMaxX = clamp(inspection.tileImageMaxXExclusive(), 0.0, image.getWidth());
            double tileMaxY = clamp(inspection.tileImageMaxYExclusive(), 0.0, image.getHeight());
            double tileX = originX + tileMinX / image.getWidth() * displayedWidth;
            double tileY = originY + tileMinY / image.getHeight() * displayedHeight;
            double tileWidth = (tileMaxX - tileMinX) / image.getWidth() * displayedWidth;
            double tileHeight = (tileMaxY - tileMinY) / image.getHeight() * displayedHeight;
            imageTileOutline.setX(tileX);
            imageTileOutline.setY(tileY);
            imageTileOutline.setWidth(tileWidth);
            imageTileOutline.setHeight(tileHeight);
            imageTileOutline.setVisible(true);
        } else {
            imageTileOutline.setVisible(false);
        }

        imageCrosshairHorizontal.setVisible(true);
        imageCrosshairVertical.setVisible(true);
        imagePositionMarker.setVisible(true);
    }

    private void setImageInspectionOverlayVisible(boolean visible) {
        imageCrosshairHorizontal.setVisible(visible);
        imageCrosshairVertical.setVisible(visible);
        imagePositionMarker.setVisible(visible);
        imageTileOutline.setVisible(visible);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static String formatRange(double minimum, double maximum) {
        return formatNumber(minimum) + " - " + formatNumber(maximum);
    }

    private static String formatNumber(double value) {
        if (!Double.isFinite(value)) return "-";
        double abs = Math.abs(value);
        if (abs > 0.0 && (abs < 0.001 || abs >= 10000.0)) {
            return String.format("%.3e", value);
        }
        return String.format("%.4f", value);
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
        Platform.runLater(this::refreshImageInspectionOverlay);
    }
}
