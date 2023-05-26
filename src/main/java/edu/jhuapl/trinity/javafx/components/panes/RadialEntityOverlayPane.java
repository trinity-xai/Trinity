package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.data.BBox;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.callouts.CalloutBuilder;
import edu.jhuapl.trinity.javafx.components.radial.RadialEntity;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.utils.Coordinates;
import edu.jhuapl.trinity.utils.Dimensions;
import edu.jhuapl.trinity.utils.HttpsUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.FadeTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lit.litfx.controls.menus.LitRadialMenuItem;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toList;

/**
 * @author Sean Phillips
 */
public class RadialEntityOverlayPane extends Pane {
    public static double CHIP_FIT_WIDTH = 200;
    public static double IMAGE_FIT_HEIGHT = 200;
    public static double IMAGE_FIT_WIDTH = 200;
    public static double DEFAULT_FADE_TIMEMS = 500;
    public static double DEFAULT_GAP = 10.0;
    public SimpleBooleanProperty showing = new SimpleBooleanProperty(false);
    public BorderPane borderPane;
    public StackPane centerStack;
    public Scene scene;
    HashMap<FeatureVector, RadialEntity> vectorToEntityMap;
    HashMap<FeatureVector, Callout> vectorToCalloutMap;
    HashMap<Shape3D, Callout> shape3DToCalloutMap;

    ObservableList<RadialEntity> entityList;
    ObservableList<Callout> calloutList;
    public List<FeatureVector> featureVectors;
    public String imageryBasePath = "imagery/";

    public RadialEntityOverlayPane(Scene scene, List<FeatureVector> featureVectors) {
        setBackground(Background.EMPTY);
        getStyleClass().add("radial-entity-overlay-pane");
        this.scene = scene;
        this.featureVectors = featureVectors; //shared resource. Hacky I know.
        centerStack = new StackPane();
        centerStack.setAlignment(Pos.CENTER);
        centerStack.minWidthProperty().bind(widthProperty());
        centerStack.minHeightProperty().bind(heightProperty());
        centerStack.maxWidthProperty().bind(widthProperty());
        centerStack.maxHeightProperty().bind(heightProperty());
        getChildren().addAll(centerStack);

        setPickOnBounds(false); //prevent it from blocking mouse clicks to sublayers
        centerStack.setPickOnBounds(false);
        shape3DToCalloutMap = new HashMap<>();
        entityList = FXCollections.observableArrayList();
        vectorToEntityMap = new HashMap<>();
        calloutList = FXCollections.observableArrayList();
        vectorToCalloutMap = new HashMap<>();
        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> {
            imageryBasePath = (String) e.object;
            System.out.println("Callout image base path set to " + imageryBasePath);
        });
    }

    public void addEntity(RadialEntity radialEntity) {
        entityList.add(radialEntity);
        getChildren().add(radialEntity);
        getChildren().add(radialEntity.centerBe);
    }

    public void removeEntity(RadialEntity radialEntity) {
        entityList.remove(radialEntity);
        getChildren().remove(radialEntity);
    }

    public void removeEntity(FeatureVector featureVector) {
        RadialEntity re = vectorToEntityMap.get(featureVector);
        entityList.remove(re);
        getChildren().remove(re);
        vectorToEntityMap.remove(featureVector);
    }

    public void clearEntities() {
        entityList.clear();
        getChildren().removeIf(node -> node instanceof RadialEntity);
        vectorToEntityMap.clear();
    }

    public Callout createCallout(Shape3D shape3D, FeatureVector featureVector, SubScene subScene) {
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
        iv.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1) {
                //add radial entity
                RadialEntity radialEntity = createEntity(featureVector);
                //radialEntity.resizeItemsToFit();
                addEntity(radialEntity);
                radialEntity.setTranslateX(getWidth() / 2.0);
                radialEntity.setTranslateY(getHeight() / 2.0);
            }
        });
        iv.setOnZoom(e -> {
            //add radial entity
            RadialEntity radialEntity = createEntity(featureVector);
            //radialEntity.resizeItemsToFit();
            addEntity(radialEntity);
            radialEntity.setTranslateX(getWidth() / 2.0);
            radialEntity.setTranslateY(getHeight() / 2.0);
        });
        TitledPane imageTP = new TitledPane();
        imageTP.setContent(iv);
        imageTP.setText("Imagery");

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

        Point2D p2D = getTransformedP2D(shape3D, subScene, Callout.DEFAULT_HEAD_RADIUS + 5);
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        Text metaText = new Text(sb.toString());
        TitledPane metaTP = new TitledPane();
        metaTP.setContent(metaText);
        metaTP.setText("Metadata");
        metaTP.setExpanded(false);

        VBox mainTitleVBox = new VBox(3, imageTP, detailsTP, metaTP);
        mainTitleVBox.setPrefWidth(250);
        mainTitleVBox.setPrefHeight(100);

        Callout infoCallout = CalloutBuilder.create()
            .headPoint(p2D.getX(), p2D.getY())
            .leaderLineToPoint(p2D.getX() - 100, p2D.getY() - 150)
            .endLeaderLineRight()
            .mainTitle(featureVector.getLabel(), mainTitleVBox)
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

        addCallout(infoCallout, shape3D);
        infoCallout.play().setOnFinished(eh -> {
            if (null == featureVector.getImageURL() || featureVector.getImageURL().isBlank()) {
                imageTP.setExpanded(false);
            }
        });
        return infoCallout;
    }

    public void updateCalloutByFeatureVector(Callout callout, FeatureVector featureVector) {
        //UPdate label
        callout.setMainTitleText(featureVector.getLabel());
        callout.mainTitleTextNode.setText(callout.getMainTitleText());
        //update image (incoming hypersonic hack)
        VBox vbox = (VBox) callout.mainTitleNode;
        TitledPane tp0 = (TitledPane) vbox.getChildren().get(0);
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        Image image = iv.getImage();
        ((ImageView) tp0.getContent()).setImage(image);

        //update details
        String bboxStr = "";
        if (null != featureVector.getBbox())
            bboxStr = bboxToString(featureVector);
        TitledPane tp1 = (TitledPane) vbox.getChildren().get(1);
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
        tp1.setContent(detailsGridPane);

        //update metadata
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        TitledPane tp2 = (TitledPane) vbox.getChildren().get(2);
        ((Text) tp2.getContent()).setText(sb.toString());
    }

    public RadialEntity createEntity(FeatureVector fv) {
        RadialEntity radialEntity = new RadialEntity(fv.getImageURL(), IMAGE_FIT_WIDTH);
        radialEntity.setText(fv.getImageURL());
        radialEntity.setScene(scene);
        radialEntity.setEmitterColors(Color.CYAN.deriveColor(1, 1, 1, 0.5),
            Color.CYAN.deriveColor(1, 1, 1, 0.15));
        radialEntity.setShowEmitter(false);
        radialEntity.setManaged(false);

        ImageView iv = loadImageView(fv, false);
        if (null != iv) {
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
            iv.setFitWidth(IMAGE_FIT_WIDTH);
            iv.setFitHeight(IMAGE_FIT_HEIGHT);
            radialEntity.setCenterGraphic(iv);
            radialEntity.getCenterGraphic().setTranslateX(-iv.getFitWidth() / 2.0);
            radialEntity.getCenterGraphic().setTranslateY(-IMAGE_FIT_HEIGHT / 2.0);
        }
        radialEntity.getCenterGroup().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isControlDown() && event.getClickCount() > 1) {
                radialEntity.hideRadialMenu();
                event.consume();
                removeEntity(radialEntity);
            }
        });
        radialEntity.getCenterGroup().addEventHandler(SwipeEvent.SWIPE_RIGHT, event -> {
            radialEntity.hideRadialMenu();
            event.consume();
            removeEntity(radialEntity);
        });
        radialEntity.addEventHandler(RotateEvent.ROTATE, event -> {
            radialEntity.setInitialAngle(radialEntity.getInitialAngle() + event.getAngle());
            event.consume();
        });
        Task task = new Task() {
            @Override
            protected Void call() throws Exception {
                SequentialTransition st =
                    radialEntity.itemReticuleLivingSpin(90, 180, 360, 0.5, 1.0, 2.0);
                //Add in Image Chips as subItems
                List<FeatureVector> chips = getFeatureVectorsByImage(fv);
                for (int i = 0; i < chips.size(); i++) {
                    FeatureVector chip = chips.get(i);
                    ImageView chipIV = loadImageView(chip, true);
                    String label = chip.getLabel();
                    Platform.runLater(() -> {
                        LitRadialMenuItem item = radialEntity.addItem(
                            label, chipIV, true, true);
                        //radialEntity.requestDraw();
                        item.setUserData(chip); //if it hacks like a duck...
                        item.setOnMouseClicked(event -> {
                            System.out.println("item clicked.");
                            if (event.isControlDown() && event.getClickCount() > 1)
                                getScene().getRoot().fireEvent(new FeatureVectorEvent(
                                    FeatureVectorEvent.LOCATE_FEATURE_VECTOR, (FeatureVector) item.getUserData()));
                        });
                    });
                }
                Platform.runLater(() -> {
                    st.stop();
                    radialEntity.showRadialMenu();
                });
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return radialEntity;
    }

    private void addBBoxRectangle(BBox bbox, double frameWidth, double frameHeight, double viewWidth, double viewHeight) {
        //local coordinats for managing placement of bounding boxes
        Coordinates bbCoordinates = new Coordinates(
            new Dimensions(0, 0, frameWidth, frameHeight),
            new Dimensions(0, 0, viewWidth, viewHeight)
        );
        double x = bbCoordinates.transformXToScreen(bbox.getX1());
        double y = bbCoordinates.transformYToScreen(bbox.getY1());
        double width = bbCoordinates.transformXToScreen(bbox.getWidth());
        double height = bbCoordinates.transformYToScreen(bbox.getHeight());
        //make a bounding box
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.setFill(Color.TRANSPARENT);
        rect.setStroke(Color.ALICEBLUE);
        rect.setStrokeWidth(3.0);
        //TODO ADD RECTANGLE TO SOME GROUP SOME WHERE OVER THE IMAGE.
    }

    public List<FeatureVector> getFeatureVectorsByImage(FeatureVector featureVector) {
        return featureVectors.stream()
            .filter(fv -> fv.getImageURL().contentEquals(featureVector.getImageURL()))
            .collect(toList());
    }

    private String bboxToString(FeatureVector featureVector) {
        NumberFormat format = new DecimalFormat("0.00");
        StringBuilder sb = new StringBuilder("[ ");
        for (Double d : featureVector.getBbox()) {
            sb.append(format.format(d));
            sb.append(" ");
        }
        sb.append("]");
        String bboxStr = sb.toString();
        return bboxStr;
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
                    System.out.println("Exception info: " + image.getException().toString());
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
            System.out.println("Oops... problem getting image! " + ex.getMessage());
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        }
        if (iv == null)
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        return iv;
    }

    private Point2D getTransformedP2D(Shape3D node, SubScene subScene, double clipDistance) {
        javafx.geometry.Point3D coordinates = node.localToScene(javafx.geometry.Point3D.ZERO, true);
        //@DEBUG SMP  useful debugging print
        //System.out.println("subSceneToScene Coordinates: " + coordinates.toString());
        //Clipping Logic
        //if coordinates are outside of the scene it could
        //stretch the screen so don't transform them
        double x = coordinates.getX();
        double y = coordinates.getY();

        //is it left of the view?
        if (x < 0) {
            x = 0;
        }
        //is it right of the view?
        if ((x + clipDistance) > subScene.getWidth()) {
            x = subScene.getWidth() - (clipDistance);
        }
        //is it above the view?
        if (y < 0) {
            y = 0;
        }
        //is it below the view
        if ((y + clipDistance) > subScene.getHeight())
            y = subScene.getHeight() - (clipDistance);
        return new Point2D(x, y);
    }

    public void updateCalloutHeadPoint(Shape3D node, Callout callout, SubScene subScene) {
        Point2D p2d = getTransformedP2D(node, subScene, callout.head.getRadius() + 5);
        callout.updateHeadPoint(p2d.getX(), p2d.getY());
    }

    public void updateCalloutHeadPoints(SubScene subScene) {
        shape3DToCalloutMap.forEach((node, callout) -> {
            updateCalloutHeadPoint(node, callout, subScene);
        });
    }

    public void addCallout(Callout callout, Shape3D shape3D) {
        calloutList.add(callout);
        callout.setManaged(false);
        getChildren().add(callout);
        //Anchor mapping for callout in 3D space
        shape3DToCalloutMap.put(shape3D, callout);
    }

    public void removeCallout(FeatureVector featureVector) {
        Callout callout = vectorToCalloutMap.get(featureVector);
        calloutList.remove(callout);
        getChildren().remove(callout);
        vectorToCalloutMap.remove(featureVector);
    }

    public void clearCallouts() {
        calloutList.clear();
        getChildren().removeIf(node -> node instanceof Callout);
        vectorToCalloutMap.clear();
    }

    public void hide(double timeMS) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), this);
        fadeTransition.setToValue(0);
        fadeTransition.setOnFinished(e -> {
            setOpacity(0.0);
            getScene().getRoot().fireEvent(new CommandTerminalEvent(
                "Radial Overlay Disengaged.", new Font("Consolas", 20), Color.GREEN));
            showing.set(false);
        });
        fadeTransition.play();
    }

    public void show(double timeMS) {
        setVisible(true);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), this);
        fadeTransition.setToValue(1);
        fadeTransition.setOnFinished(e -> {
            setOpacity(1.0);
            getScene().getRoot().fireEvent(new CommandTerminalEvent(
                "Radial Overlay Engaged.", new Font("Consolas", 20), Color.GREEN));
            showing.set(true);
        });
        fadeTransition.play();
    }
}
