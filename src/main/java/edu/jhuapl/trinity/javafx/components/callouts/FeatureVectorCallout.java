/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.callouts;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.HttpsUtils;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map.Entry;

import static edu.jhuapl.trinity.data.messages.xai.FeatureVector.bboxToString;

/**
 * @author Sean Phillips
 */
public class FeatureVectorCallout extends VBox {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureVectorCallout.class);
    public static double CHIP_FIT_WIDTH = 200;
    public static double IMAGE_FIT_HEIGHT = 200;
    public static double IMAGE_FIT_WIDTH = 200;
    public String imageryBasePath = "imagery/";

    public static Callout createByShape3D(Shape3D shape3D,
                                          FeatureVector featureVector, SubScene subScene, String imageryBasePath) {
        FeatureVectorCallout featureVectorCallout = new FeatureVectorCallout(shape3D,
            featureVector, subScene, imageryBasePath);
        Point2D p2D = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, Callout.DEFAULT_HEAD_RADIUS + 5);
        Callout infoCallout = CalloutBuilder.create()
            .headPoint(p2D.getX(), p2D.getY())
            .leaderLineToPoint(p2D.getX() - 100, p2D.getY() - 150)
            .endLeaderLineRight()
            .mainTitle(featureVector.getLabel(), featureVectorCallout)
            .pause(10)
            .build();

        infoCallout.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1) {
                infoCallout.hide();
            }
        });

        infoCallout.setOnZoom(e -> {
            if (e.getZoomFactor() < 1)
                infoCallout.hide(); //pinch hides it
        });

        infoCallout.setPickOnBounds(false);
        infoCallout.setManaged(false);

        return infoCallout;
    }

    public FeatureVectorCallout(Shape3D shape3D, FeatureVector featureVector,
                                SubScene subScene, String imageryBasePath) {
        this.imageryBasePath = imageryBasePath;
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        String bboxStr = "";
        if (null != featureVector.getBbox())
            bboxStr = bboxToString(featureVector);
        Tooltip.install(iv, new Tooltip(
            featureVector.getImageURL() + "\n BBOX: " + bboxStr
        ));
        iv.setPreserveRatio(true);
        iv.setFitWidth(CHIP_FIT_WIDTH);
        iv.setFitHeight(CHIP_FIT_WIDTH);
//@TODO SMP This feature has been found to be more problematic than beneficial
//However its pretty friggen cool so hopefully we can leverage it later
//        iv.setOnMouseClicked(e -> {
//            if (e.getClickCount() > 1) {
//                //add radial entity
//                RadialEntity radialEntity = createEntity(featureVector);
//                //radialEntity.resizeItemsToFit();
//                addEntity(radialEntity);
//                radialEntity.setTranslateX(getWidth() / 2.0);
//                radialEntity.setTranslateY(getHeight() / 2.0);
//            }
//        });
//@TODO SMP This feature has been found to be more problematic than beneficial
//        iv.setOnZoom(e -> {
//            //add radial entity
//            RadialEntity radialEntity = createEntity(featureVector);
//            //radialEntity.resizeItemsToFit();
//            addEntity(radialEntity);
//            radialEntity.setTranslateX(getWidth() / 2.0);
//            radialEntity.setTranslateY(getHeight() / 2.0);
//        });
        Glow glow = new Glow(0.95);
        TitledPane imageTP = new TitledPane();
        ImageView imageToolsIV = ResourceUtils.loadIcon("defaultimage", 30);
        VBox imageToolsVBox = new VBox(imageToolsIV);
        imageToolsVBox.setOnMouseEntered(e -> imageToolsVBox.setEffect(glow));
        imageToolsVBox.setOnMouseExited(e -> imageToolsVBox.setEffect(null));
        imageToolsVBox.setOnMouseClicked(e -> {
            imageToolsVBox.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_HYPERSURFACE, true));
            imageToolsVBox.getScene().getRoot().fireEvent(
                new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, iv.getImage()));
        });
        HBox imageToolsVBoxHBox = new HBox(15, imageToolsVBox);
        imageToolsVBoxHBox.setAlignment(Pos.TOP_LEFT);
        imageTP.setContent(new VBox(imageToolsVBoxHBox, iv));
        imageTP.setText("Imagery");
        imageTP.setExpanded(false);

        TitledPane detailsTP = new TitledPane();
        GridPane detailsGridPane = new GridPane();
        detailsGridPane.setPadding(new Insets(1));
        detailsGridPane.setHgap(5);
        detailsGridPane.addRow(0, new Label("imageURL"),
            new Label(featureVector.getImageURL()));
        detailsGridPane.addRow(1, new Label("bbox"),
            new Label(bboxStr));
        detailsGridPane.addRow(2, new Label("frameId"),
            new Label(String.valueOf(featureVector.getFrameId())));
        detailsGridPane.addRow(3, new Label("score"),
            new Label(String.valueOf(featureVector.getScore())));
        detailsGridPane.addRow(4, new Label("layer"),
            new Label(String.valueOf(featureVector.getLayer())));
        detailsGridPane.addRow(5, new Label("messageId"),
            new Label(String.valueOf(featureVector.getLayer())));

        detailsTP.setContent(detailsGridPane);
        detailsTP.setText("Details");
        detailsTP.setExpanded(false);

        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        Text metaText = new Text(sb.toString());
        TitledPane metaTP = new TitledPane();
        metaTP.setContent(metaText);
        metaTP.setText("Metadata");
        metaTP.setExpanded(false);

        TextArea textArea = new TextArea(featureVector.getText());
        textArea.setMaxWidth(225);
        textArea.setEditable(false);
        textArea.setMinHeight(200);
        textArea.setPrefHeight(200);
        textArea.setWrapText(true);

        ImageView selectAllIV = ResourceUtils.loadIcon("selectall", 30);
        VBox selectAllVBox = new VBox(selectAllIV);
        selectAllVBox.setOnMouseEntered(e -> selectAllIV.setEffect(glow));
        selectAllVBox.setOnMouseExited(e -> selectAllIV.setEffect(null));
        selectAllVBox.setOnMouseClicked(e -> textArea.selectAll());

        ImageView copyIV = ResourceUtils.loadIcon("copy", 30);
        VBox copyVBox = new VBox(copyIV);
        copyVBox.setOnMouseEntered(e -> copyVBox.setEffect(glow));
        copyVBox.setOnMouseExited(e -> copyVBox.setEffect(null));
        copyVBox.setOnMouseClicked(e -> textArea.copy());

        ImageView textIV = ResourceUtils.loadIcon("console", 30);
        VBox textIVVBox = new VBox(textIV);
        textIVVBox.setOnMouseEntered(e -> textIVVBox.setEffect(glow));
        textIVVBox.setOnMouseExited(e -> textIVVBox.setEffect(null));
        textIVVBox.setOnMouseClicked(e -> {
            textIVVBox.getScene().getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, featureVector.getText()));
        });

        HBox hbox = new HBox(15, selectAllVBox, copyVBox, textIVVBox);
        hbox.setAlignment(Pos.TOP_LEFT);

        VBox textVBox = new VBox(5, hbox, textArea);
        TitledPane textTP = new TitledPane();
        textTP.setContent(textVBox);
        textTP.setText("Text");
        textTP.setExpanded(false);

        ImageView openMediaIV = ResourceUtils.loadIcon("forward", 30);
        VBox openMediaVBox = new VBox(openMediaIV);
        openMediaVBox.setOnMouseEntered(e -> openMediaVBox.setEffect(glow));
        openMediaVBox.setOnMouseExited(e -> openMediaVBox.setEffect(null));
        openMediaVBox.setOnMouseClicked(e -> {
            if (null != featureVector.getMediaURL()) {
                openMediaVBox.getScene().getRoot().fireEvent(new ApplicationEvent(
                    ApplicationEvent.SHOW_WAVEFORM_PANE, new File(this.imageryBasePath + featureVector.getMediaURL())));
            }
        });
        HBox mediaHBox = new HBox(15, openMediaVBox);
        mediaHBox.setAlignment(Pos.TOP_LEFT);
        VBox mediaVBox = new VBox(5, new Label(featureVector.getMediaURL()), mediaHBox);
        TitledPane mediaTP = new TitledPane();
        mediaTP.setContent(mediaVBox);
        mediaTP.setText("Media");
        mediaTP.setExpanded(false);

        getChildren().addAll(imageTP, mediaTP, textTP, detailsTP, metaTP);
        setSpacing(3);
        setPrefWidth(250);
        setPrefHeight(100);

        subScene.getParent().getScene().addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> {
            this.imageryBasePath = (String) e.object;
            //System.out.println("Callout image base path set to " + featureVectorCallout.imageryBasePath);
        });

    }

    private ImageView loadImageView(FeatureVector featureVector, boolean bboxOnly) {
        if (null == featureVector.getImageURL())
            return new ImageView(ResourceUtils.loadIconFile("noimage"));
        ImageView iv = null;
        try {
            if (featureVector.getImageURL().startsWith("http")) {
                //@DEBUG SMP Useful print
                //System.out.println("<Trinity Debug> HTTP Link: fv.getImageURL()== " + featureVector.getImageURL());
                Image image = HttpsUtils.getImage(featureVector.getImageURL());
                if (image.getException() != null)
                    LOG.info("Exception info: {}", image.getException().toString());
                iv = new ImageView(image);
            } else {
                if (bboxOnly) {
                    //@DEBUG SMP Useful print
                    //System.out.println("<Trinity Debug> BoundingBox Request: file == " + imageryBasePath + featureVector.getImageURL());
                    WritableImage image = ResourceUtils.loadImageFileSubset(imageryBasePath + featureVector.getImageURL(),
                        featureVector.getBbox().get(0).intValue(),
                        featureVector.getBbox().get(1).intValue(),
                        featureVector.getBbox().get(2).intValue(),
                        featureVector.getBbox().get(3).intValue()
                    );
                    iv = new ImageView(image);
                } else {
                    //@DEBUG SMP Useful print
                    //System.out.println("<Trinity Debug> Full Image Request: file == " + imageryBasePath + featureVector.getImageURL());
                    iv = new ImageView(ResourceUtils.loadImageFile(imageryBasePath + featureVector.getImageURL()));
                }
            }
        } catch (Exception ex) {
            LOG.info("Oops... problem getting image! {}", ex.getMessage(), ex);
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        }
        if (iv == null)
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        return iv;
    }


}
