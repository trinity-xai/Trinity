package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.callouts.CalloutBuilder;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.javafx3d.images.SolidColorTexture;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import org.fxyz3d.geometry.Point3D;

import java.util.HashMap;
import java.util.Map;

/**
 * Owns the auxiliary 3D/2D visual overlays used by {@link Hypersurface3DPane}:
 * axis markers and labels, the hover marker, timeline row markers, and FeatureVector
 * callouts. Surface geometry, source data, interaction interpretation, and camera
 * behavior remain outside this class.
 *
 * @author Sean Phillips
 */
public final class HypersurfaceOverlayManager {

    private static final double TIMELINE_POLE_HEIGHT = 60.0;
    private static final double TIMELINE_RADIUS = 3.0;

    private final Hypersurface3DPane owner;
    private final Scene scene;
    private final SubScene subScene;
    private final SurfaceCoordinateMapper coordinateMapper;

    private final Group nodeGroup = new Group();
    private final Group labelGroup = new Group();
    private final Group extrasGroup = new Group();

    private final Map<Shape3D, Node> shape3DToLabel = new HashMap<>();
    private final Map<Shape3D, Callout> shape3DToCalloutMap = new HashMap<>();

    private final Sphere xSphere = new Sphere(10);
    private final Sphere ySphere = new Sphere(10);
    private final Sphere zSphere = new Sphere(10);
    private final Sphere highlightedPoint = new Sphere(1, 32);

    private final Label xLabel = new Label("Features (ordered)");
    private final Label yLabel = new Label("Magnitude");
    private final Label zLabel = new Label("Time (Samples)");

    private Box glowLineBox;
    private Cylinder eastPole;
    private Cylinder westPole;
    private Sphere eastKnob;
    private Sphere westKnob;
    private Label eastLabel;
    private Label westLabel;
    private int anchorIndex;
    private Callout anchorCallout;

    public HypersurfaceOverlayManager(Hypersurface3DPane owner,
                                      Scene scene,
                                      SubScene subScene,
                                      SurfaceCoordinateMapper coordinateMapper,
                                      double initialPlaneSize) {
        this.owner = owner;
        this.scene = scene;
        this.subScene = subScene;
        this.coordinateMapper = coordinateMapper;

        configureAxisMarkers(initialPlaneSize);
        configureHighlightedPoint();
        configureLabels();
        wireSelectionHandlers();
        createAnchorCalloutWhenSceneReady();
    }

    private void configureAxisMarkers(double initialPlaneSize) {
        nodeGroup.getChildren().addAll(xSphere, ySphere, zSphere);
        nodeGroup.getTransforms().addAll(
            new Rotate(0, Rotate.X_AXIS),
            new Rotate(0, Rotate.Y_AXIS),
            new Rotate(0, Rotate.Z_AXIS));

        xSphere.setTranslateX(initialPlaneSize / 2.0);
        xSphere.setMaterial(new PhongMaterial(Color.RED));
        ySphere.setTranslateY(-initialPlaneSize / 2.0);
        ySphere.setMaterial(new PhongMaterial(Color.GREEN));
        zSphere.setTranslateZ(initialPlaneSize / 2.0);
        zSphere.setMaterial(new PhongMaterial(Color.BLUE));
    }

    private void configureHighlightedPoint() {
        PhongMaterial highlightedPointMaterial =
            new PhongMaterial(null, null, null, null, null);
        highlightedPointMaterial.setSelfIlluminationMap(
            SolidColorTexture.of(Color.ALICEBLUE.deriveColor(1, 1, 1, 0.666)));
        highlightedPoint.setMaterial(highlightedPointMaterial);
        highlightedPoint.setDrawMode(DrawMode.FILL);
        highlightedPoint.setMouseTransparent(true);
    }

    private void configureLabels() {
        Font font = new Font("Consolas", 20);
        xLabel.setTextFill(Color.YELLOW);
        xLabel.setFont(font);
        xLabel.setMouseTransparent(true);
        yLabel.setTextFill(Color.SKYBLUE);
        yLabel.setFont(font);
        yLabel.setMouseTransparent(true);
        zLabel.setTextFill(Color.LIGHTGREEN);
        zLabel.setFont(font);
        zLabel.setMouseTransparent(true);

        labelGroup.getChildren().addAll(xLabel, yLabel, zLabel);
        labelGroup.setManaged(false);
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
    }

    private void wireSelectionHandlers() {
        scene.addEventHandler(TimelineEvent.TIMELINE_SAMPLE_INDEX, e -> {
            if (glowLineBox == null) return;
            anchorIndex = (int) e.object;
            int maxSourceRow = Math.max(0, owner.getSourceHeight() - 1);
            anchorIndex = Math.max(0, Math.min(anchorIndex, maxSourceRow));
            glowLineBox.setTranslateZ(coordinateMapper.sourceRowToWorldZ(anchorIndex));
            owner.setSpheroidAnchor(true, anchorIndex);
            updateTimelineLabels();
            refreshProjectedOverlays();
        });

        scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            if (anchorCallout == null || !(e.object instanceof FeatureVector featureVector)) return;
            updateCalloutByFeatureVector(anchorCallout, featureVector);
        });
    }

    private void createAnchorCalloutWhenSceneReady() {
        subScene.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null || anchorCallout != null) return;
            Platform.runLater(() -> {
                if (anchorCallout != null) return;
                FeatureVector dummy = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
                anchorCallout = createCallout(highlightedPoint, dummy, subScene);
                anchorCallout.play().setOnFinished(fin -> anchorCallout.setVisible(false));
            });
        });
    }

    /** Creates the timeline row marker geometry after the initial surface is available. */
    public void initializeTimelineMarkers() {
        if (glowLineBox != null) return;

        Glow glow = new Glow(0.8);
        double poleHeight = TIMELINE_POLE_HEIGHT;
        double radius = TIMELINE_RADIUS;

        glowLineBox = new Box(owner.getWorldWidth(), poleHeight, radius);
        glowLineBox.setMaterial(new PhongMaterial(Color.ALICEBLUE.deriveColor(1, 1, 1, 0.2)));
        glowLineBox.setDrawMode(DrawMode.FILL);
        glowLineBox.setEffect(glow);
        glowLineBox.setTranslateZ(-owner.getWorldDepth() / 2.0);

        eastPole = new Cylinder(radius * 2, poleHeight * 1.2);
        westPole = new Cylinder(radius * 2, poleHeight * 1.2);
        eastKnob = new Sphere(radius * 3);
        westKnob = new Sphere(radius * 3);

        eastPole.setMaterial(new PhongMaterial(Color.STEELBLUE));
        westPole.setMaterial(new PhongMaterial(Color.STEELBLUE));
        PhongMaterial knobMaterial = new PhongMaterial(Color.ALICEBLUE);
        eastKnob.setMaterial(knobMaterial);
        westKnob.setMaterial(knobMaterial);

        eastPole.setTranslateX(owner.getWorldWidth() / 2.0);
        westPole.setTranslateX(-owner.getWorldWidth() / 2.0);
        eastKnob.setTranslateX(owner.getWorldWidth() / 2.0);
        westKnob.setTranslateX(-owner.getWorldWidth() / 2.0);
        eastKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        westKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);

        eastPole.translateZProperty().bind(glowLineBox.translateZProperty());
        westPole.translateZProperty().bind(glowLineBox.translateZProperty());
        eastKnob.translateZProperty().bind(glowLineBox.translateZProperty());
        westKnob.translateZProperty().bind(glowLineBox.translateZProperty());

        eastLabel = createTimelineLabel();
        westLabel = createTimelineLabel();
        labelGroup.getChildren().addAll(eastLabel, westLabel);
        shape3DToLabel.put(eastKnob, eastLabel);
        shape3DToLabel.put(westKnob, westLabel);
        updateTimelineLabels();

        extrasGroup.getChildren().addAll(eastPole, eastKnob, westPole, westKnob, glowLineBox);
        updateLabels();
    }

    private static Label createTimelineLabel() {
        Label label = new Label("Data Index");
        label.setTextFill(Color.ALICEBLUE);
        label.setFont(new Font("calibri", 20));
        return label;
    }

    public Group getNodeGroup() {
        return nodeGroup;
    }

    public Group getLabelGroup() {
        return labelGroup;
    }

    public Group getExtrasGroup() {
        return extrasGroup;
    }

    public Sphere getHighlightedPoint() {
        return highlightedPoint;
    }

    public Callout getAnchorCallout() {
        return anchorCallout;
    }

    public void setDataMarkersVisible(boolean visible) {
        extrasGroup.setVisible(visible);
        labelGroup.setVisible(visible);
        if (anchorCallout != null) anchorCallout.setVisible(visible);
    }

    public void setExtrasVisible(boolean visible) {
        extrasGroup.setVisible(visible);
    }

    public void setAxesAndLabelsVisible(boolean visible) {
        nodeGroup.setVisible(visible);
        labelGroup.setVisible(visible);
    }

    public void setInitialVisibility(boolean visible) {
        extrasGroup.setVisible(visible);
        labelGroup.setVisible(visible);
    }

    public void moveTimeline(double deltaZ) {
        if (glowLineBox != null) {
            glowLineBox.setTranslateZ(glowLineBox.getTranslateZ() + deltaZ);
        }
    }

    public void updateAxisExtents() {
        xSphere.setTranslateX(owner.getWorldWidth() / 2.0);
        zSphere.setTranslateZ(owner.getWorldDepth() / 2.0);
    }

    public void updateTimelineGeometry(double poleHeight) {
        if (glowLineBox == null) return;
        glowLineBox.setWidth(owner.getWorldWidth());
        glowLineBox.setHeight(poleHeight);
        eastPole.setHeight(poleHeight * 1.2);
        westPole.setHeight(poleHeight * 1.2);
        eastPole.setTranslateX(-owner.getWorldWidth() / 2.0);
        westPole.setTranslateX(owner.getWorldWidth() / 2.0);
        eastKnob.setTranslateX(owner.getWorldWidth() / 2.0);
        westKnob.setTranslateX(-owner.getWorldWidth() / 2.0);
        eastKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        westKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        updateTimelineLabels();
        updateLabels();
    }

    private void updateTimelineLabels() {
        if (eastLabel == null || westLabel == null) return;
        eastLabel.setText("Sample: " + anchorIndex + ", Neural Feature: " + owner.getSourceWidth());
        westLabel.setText("Sample: " + anchorIndex + ", Neural Feature: 0");
    }

    public void updateHoverMarker(Point3D surfacePoint) {
        highlightedPoint.setTranslateX(surfacePoint.x - owner.getWorldWidth() / 2.0);
        highlightedPoint.setTranslateY(surfacePoint.y);
        highlightedPoint.setTranslateZ(surfacePoint.z - owner.getWorldDepth() / 2.0);
        refreshProjectedOverlays();
    }

    public void refreshProjectedOverlays() {
        updateLabels();
        updateCalloutHeadPoints(subScene);
    }

    public void updateLabels() {
        shape3DToLabel.forEach((shape3D, node) -> {
            Point2D point = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, 5);
            node.getTransforms().setAll(new Translate(point.getX(), point.getY() - 25));
        });
    }

    public void resetLabelTracking() {
        shape3DToLabel.clear();
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
    }

    public void updateCalloutHeadPoint(Shape3D node, Callout callout, SubScene targetSubScene) {
        Point2D point = JavaFX3DUtils.getTransformedP2D(
            node, targetSubScene, callout.head.getRadius() + 5);
        callout.updateHeadPoint(point.getX(), point.getY());
    }

    public void updateCalloutHeadPoints(SubScene targetSubScene) {
        shape3DToCalloutMap.forEach((node, callout) ->
            updateCalloutHeadPoint(node, callout, targetSubScene));
    }

    public Callout createCallout(Shape3D shape3D,
                                 FeatureVector featureVector,
                                 SubScene targetSubScene) {
        ImageView imageView = loadImageView(featureVector, featureVector.isBBoxValid());
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(Hypersurface3DPane.CHIP_FIT_WIDTH);
        imageView.setFitHeight(Hypersurface3DPane.CHIP_FIT_WIDTH);

        TitledPane imagePane = new TitledPane();
        imagePane.setContent(imageView);
        imagePane.setText("Imagery");

        Point2D point = JavaFX3DUtils.getTransformedP2D(
            shape3D, targetSubScene, Callout.DEFAULT_HEAD_RADIUS + 5);

        StringBuilder metadata = new StringBuilder();
        for (Map.Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            metadata.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        TitledPane metadataPane = new TitledPane();
        metadataPane.setContent(new Text(metadata.toString()));
        metadataPane.setText("Metadata");

        Callout callout = CalloutBuilder.create()
            .headPoint(point.getX(), point.getY())
            .leaderLineToPoint(point.getX() - 100, point.getY() - 150)
            .endLeaderLineRight()
            .mainTitle(featureVector.getLabel(), new VBox(3, imagePane, metadataPane))
            .subTitle(featureVector.getEntityId())
            .pause(10)
            .build();
        callout.setPickOnBounds(false);
        callout.setManaged(false);
        addCallout(callout, shape3D);
        callout.play().setOnFinished(eh -> {
            if (featureVector.getImageURL() == null || featureVector.getImageURL().isBlank()) {
                imagePane.setExpanded(false);
            }
        });
        return callout;
    }

    public void addCallout(Callout callout, Shape3D shape3D) {
        callout.setManaged(false);
        owner.getChildren().add(callout);
        shape3DToCalloutMap.put(shape3D, callout);
    }

    public void updateCalloutByFeatureVector(Callout callout, FeatureVector featureVector) {
        callout.setMainTitleText(featureVector.getLabel());
        callout.mainTitleTextNode.setText(callout.getMainTitleText());
        VBox vbox = (VBox) callout.mainTitleNode;
        TitledPane imagePane = (TitledPane) vbox.getChildren().get(0);
        ImageView imageView = loadImageView(featureVector, featureVector.isBBoxValid());
        Image image = imageView.getImage();
        ((ImageView) imagePane.getContent()).setImage(image);

        StringBuilder metadata = new StringBuilder();
        for (Map.Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            metadata.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        TitledPane metadataPane = (TitledPane) vbox.getChildren().get(1);
        ((Text) metadataPane.getContent()).setText(metadata.toString());
    }

    private ImageView loadImageView(FeatureVector featureVector, boolean bboxOnly) {
        try {
            if (bboxOnly) {
                WritableImage image = ResourceUtils.loadImageFileSubset(
                    owner.imageryBasePath + featureVector.getImageURL(),
                    featureVector.getBbox().get(0).intValue(),
                    featureVector.getBbox().get(1).intValue(),
                    featureVector.getBbox().get(2).intValue(),
                    featureVector.getBbox().get(3).intValue());
                return new ImageView(image);
            }
            if (featureVector.getImageURL() != null && !featureVector.getImageURL().isBlank()) {
                return new ImageView(ResourceUtils.loadImageFile(
                    owner.imageryBasePath + featureVector.getImageURL()));
            }
            return new ImageView(ResourceUtils.loadIconFile("noimage"));
        } catch (Exception ex) {
            return new ImageView(ResourceUtils.loadIconFile("noimage"));
        }
    }
}
