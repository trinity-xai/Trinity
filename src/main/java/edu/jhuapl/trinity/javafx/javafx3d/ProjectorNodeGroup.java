package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.javafx.components.projector.ProjectorNode;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorRow;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.scene.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.Camera;
import javafx.scene.Node;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Reflection;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape3D;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.fxyz3d.utils.CameraTransformer;


/**
 * @author Sean Phillips
 */
public class ProjectorNodeGroup extends Group {
    public Group labelGroup; 
    public double transitionXOffset = -15000;
    public double transitionYOffset = -15000;
    public double transitionZOffset = 0;
    public double originRadius = 9001;
    public boolean positiveRow = false;
    public SubScene subScene;
    public Camera camera;
    public CameraTransformer cameraTransform;
    //allows 2D labels to track their 3D counterparts
    public HashMap<Shape3D, Label> shape3DToLabel;    
    public ArrayList<ProjectorRow> rows;    
    ArrayList<ProjectorNode> nodes;
    List<ParallelTransition> transitionList;
    
    public ProjectorNodeGroup(SubScene subScene, Camera camera, CameraTransformer cameraTransform, Group labelGroup) {
        this.subScene = subScene;
        this.camera = camera;
        this.cameraTransform = cameraTransform;
        this.labelGroup = labelGroup;
        nodes = new ArrayList<>();
        getChildren().addAll(nodes);
        transitionList = new ArrayList<>();
        shape3DToLabel = new HashMap<>();
        rows = new ArrayList<>();
    }
    public void clearAll() {
        nodes.clear();
        transitionList.clear();
        shape3DToLabel.clear();
        rows.clear();
        getChildren().clear();
    }

    public ProjectorNode addNodeToScene(ProjectorNode projectorNode, String rowLabel) {
        Optional<ProjectorRow> optRow = rows.stream()
            .filter(r -> r.rowLabel.contentEquals(rowLabel)).findFirst();
        if(!optRow.isPresent()) {
            //we need to create new row
            //this logic should alternate row placement in the 3D scene such that 
            //0 is equitorial, negative goes Y up and positive goes Y down... 
            int r = rows.isEmpty() ? 0 : rows.size()/2 + 1;
            if(positiveRow == false)
                r *= -1;
            System.out.println("r = " + r + " " + rowLabel);

            ProjectorRow newRow = new ProjectorRow(rowLabel, r, originRadius);
            rows.add(newRow);
            positiveRow = !positiveRow;
            return addNodeToScene(projectorNode, newRow.row, newRow.getAngleAndStep(), newRow.getRadius());
        } else {    
            ProjectorRow row = optRow.get();
            return addNodeToScene(projectorNode, row.row, row.getAngleAndStep(), row.getRadius());
        }
    }    
    public ProjectorNode addNodeToScene(ProjectorNode projectorNode, int row, double angle1, double radius) {
//        double yOffset = 1300; //a bit more than 1080p height
        double yOffset = 636; //a bit more than 512 height
        double angle2 = Math.PI;

        // Ring formula
        final double x = radius * Math.sin(angle1) * Math.cos(angle2);
        final double y = yOffset * row;
        final double z = radius * Math.cos(angle1);

        projectorNode.setTranslateX(x);
        projectorNode.setTranslateY(y);
        projectorNode.setTranslateZ(z);

        Rotate ry = new Rotate();
        ry.setAxis(Rotate.Y_AXIS);
        ry.setAngle(Math.toDegrees(-angle1));
        projectorNode.getTransforms().addAll(ry);
        final double ryAngle = ry.getAngle();

        //Add special click handler to zoom camera
        projectorNode.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                long milliseconds = 500;
                Timeline rotateTimeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                    0, 0, 0, 0.0, ryAngle, 0.0);
                rotateTimeline.setOnFinished(eh -> {
                    updateLabels();
                    javafx.geometry.Point3D p3D = new javafx.geometry.Point3D(x, y, z);
                    javafx.geometry.Point3D lessP3D = p3D.subtract(x * 0.5, y * 0.5, z * 0.5);
                    cameraTransform.setPivot(0, 0, 0);
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(milliseconds),
                            new KeyValue(cameraTransform.translateXProperty(), lessP3D.getX()),
                            new KeyValue(cameraTransform.translateYProperty(), y),
                            new KeyValue(cameraTransform.translateZProperty(), lessP3D.getZ())
                        )
                    );
                    timeline.setOnFinished(fin -> updateLabels());
                    timeline.playFromStart();
                });
                rotateTimeline.playFromStart();
            }
        });

        projectorNode.setVisible(false); //must animate or manually set visible later
        nodes.add(projectorNode);
        getChildren().add(projectorNode);
//        ParallelTransition pt = createTransition(projectorNode);
//        transitionList.add(pt);
        return projectorNode;
    }
    public void animateImages() {
        shape3DToLabel.values().forEach(label -> label.setOpacity(0.0));
        nodes.forEach(n -> n.setVisible(false));
        AnimationTimer timer = createAnimation();
        timer.start();
    }

    private AnimationTimer createAnimation() {
        Collections.sort(transitionList, (ParallelTransition arg0, ParallelTransition arg1) -> {
            // bottom right to top left
            Point2D ref = new Point2D(1000, 1000);
            Point2D pt0 = new Point2D(arg0.getNode().getTranslateX(), arg0.getNode().getTranslateY());
            Point2D pt1 = new Point2D(arg1.getNode().getTranslateX(), arg1.getNode().getTranslateY());

            return Double.compare(ref.distance(pt0), ref.distance(pt1));
            // bottom row first
            // return -Double.compare( arg0.getNode().getTranslateY(), arg1.getNode().getTranslateY());
        });

        AnimationTimer timer = new AnimationTimer() {
            long last = 0;
            int transitionIndex = 0;

            @Override
            public void handle(long now) {
                if ((now - last) > 30_000_000) {
                    if (transitionIndex < transitionList.size()) {
                        ParallelTransition t = transitionList.get(transitionIndex);
                        t.getNode().setVisible(true);
                        t.play();
                        transitionIndex++;
                    }
                    last = now;
                }
                if (transitionIndex >= transitionList.size()) {
                    stop();
                    animateLabelVisibility(1000);
                }
            }
        };
        return timer;
    }
    private void animateLabelVisibility(long ms) {
        //sweet music reference: https://en.wikipedia.org/wiki/Street_Spirit_(Fade_Out)
        FadeTransition streetSpiritFadeIn = new FadeTransition(Duration.millis(ms), labelGroup);
        streetSpiritFadeIn.setFromValue(0.0);
        streetSpiritFadeIn.setToValue(1);
        streetSpiritFadeIn.playFromStart();
    }    
    public ParallelTransition createTransition(final Node node) {
        Path path = new Path();
        path.getElements().add(new MoveToAbs(node,
            node.getTranslateX() + transitionXOffset,
            node.getTranslateY() + transitionYOffset));
        path.getElements().add(new LineToAbs(node, node.getTranslateX(), node.getTranslateY()));

        Duration duration = Duration.millis(1000);

        PathTransition pt = new PathTransition(duration, path, node);

        RotateTransition rt = new RotateTransition(duration, node);
        rt.setByAngle(720);
        rt.setAutoReverse(true);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.setNode(node);
        parallelTransition.getChildren().addAll(pt, rt);
        parallelTransition.setCycleCount(1);
        return parallelTransition;
    }    

    public void updateLabels() {
        shape3DToLabel.forEach((node, label) -> {
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
            if ((x + label.getWidth() + 5) > subScene.getWidth()) {
                x = subScene.getWidth() - (label.getWidth() + 5);
            }
            //is it above the view?
            if (y < 0) {
                y = 0;
            }
            //is it below the view
            if ((y + label.getHeight()) > subScene.getHeight())
                y = subScene.getHeight() - (label.getHeight() + 5);
            //@DEBUG SMP  useful debugging print
            //System.out.println("clipping Coordinates: " + x + ", " + y);
            //update the local transform of the label.
            label.getTransforms().setAll(new Translate(x, y));
        });
    }    
    public static class MoveToAbs extends MoveTo {

        public MoveToAbs(Node node, double x, double y) {
            super(x - node.getLayoutX() + node.getLayoutBounds().getWidth() / 2,
                y - node.getLayoutY() + node.getLayoutBounds().getHeight() / 2);
        }
    }

    public static class LineToAbs extends LineTo {

        public LineToAbs(Node node, double x, double y) {
            super(x - node.getLayoutX() + node.getLayoutBounds().getWidth() / 2,
                y - node.getLayoutY() + node.getLayoutBounds().getHeight() / 2);
        }
    }    
}
