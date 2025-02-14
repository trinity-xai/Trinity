/*
 * Copyright (c) 2018. Carl Dea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */
/* Copyright (C) 2024 - 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.callouts;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * @author Carl Dea (LitFX)
 * @extended Sean Phillips
 * This class represent the parts that make up a callout.
 * Many protected methods allow an implementer to override to customize
 * their own callout. For example you can override the buildHeadAnim(Node head) method
 * with your own JavaFX shape such as a rectangle or circle Node.
 */
public class Callout extends Group {
    public static int LEFT = -1;
    public static int RIGHT = 1;
    public static double ICON_FIT_HEIGHT = 32;
    public static double ICON_FIT_WIDTH = 32;
    public static double DEFAULT_HEAD_RADIUS = 5;
    private Point2D headPoint;
    private Point2D leaderLineToPoint;
    private double endLeaderLineLength;
    private int endLeaderLineDirection = RIGHT;
    private String mainTitleText;
    private String subTitleText;
    private long pauseInMillis = 2000;
    private SequentialTransition calloutAnimation;

    public Circle head;
    public Line firstLeaderLine;
    public Line firstInnerLine;
    public Line secondLeaderLine;
    public Line secondInnerLine;
    public HBox mainTitleHBox;
    public Rectangle subTitleRect;
    public HBox subTitleHBox;
    public Text mainTitleTextNode;
    public Text subTitleTextNode;
    public Node mainTitleNode = null;
    public ContextMenu contextMenu;

    private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;

    public Callout() {

    }

    public void setMainTitleNode(Node node) {
        mainTitleNode = node;
    }

    public void build() {
        getStyleClass().add("callout");

        // Create head
        head = new Circle(getHeadPoint().getX(), getHeadPoint().getY(), DEFAULT_HEAD_RADIUS);
        head.setFill(Color.SKYBLUE);//.deriveColor(1, 1, 1, 0.75));
        head.setStroke(Color.ALICEBLUE);
        head.setStrokeWidth(1.0);
        head.getStyleClass().add("callout-head");
        // First leader line
        firstLeaderLine = new Line(getHeadPoint().getX(),
            getHeadPoint().getY(),
            getHeadPoint().getX(),
            getHeadPoint().getY());
        firstLeaderLine.setStroke(Color.SKYBLUE.deriveColor(1, 1, 1, 0.75));
        firstLeaderLine.setStrokeWidth(4);
        firstLeaderLine.setStrokeLineCap(StrokeLineCap.ROUND);
        firstLeaderLine.getStyleClass().add("callout-firstLeaderLine");

        // Inner leader line
        firstInnerLine = new Line(getHeadPoint().getX(),
            getHeadPoint().getY(),
            getHeadPoint().getX(),
            getHeadPoint().getY());
        firstInnerLine.setStroke(Color.ALICEBLUE);//.deriveColor(1, 1, 1, 0.75));
        firstInnerLine.setStrokeWidth(0.5);
        firstInnerLine.setStrokeLineCap(StrokeLineCap.ROUND);
        firstInnerLine.getStyleClass().add("callout-firstInnerLine");

        // Second part of the leader line
        secondLeaderLine = new Line(getLeaderLineToPoint().getX(),
            getLeaderLineToPoint().getY(),
            getLeaderLineToPoint().getX(),
            getLeaderLineToPoint().getY());

        secondLeaderLine.setStroke(Color.SKYBLUE.deriveColor(1, 1, 1, 0.75));
        secondLeaderLine.setStrokeWidth(4);
        secondLeaderLine.setStrokeLineCap(StrokeLineCap.ROUND);
        secondLeaderLine.getStyleClass().add("callout-secondLeaderLine");

        // second Inner leader line
        secondInnerLine = new Line(getLeaderLineToPoint().getX(),
            getLeaderLineToPoint().getY() - 1,
            getLeaderLineToPoint().getX(),
            getLeaderLineToPoint().getY() - 1);
        secondInnerLine.setStroke(Color.ALICEBLUE);//.deriveColor(1, 1, 1, 0.75));
        Glow glow = new Glow(0.75);
        secondInnerLine.setEffect(glow);
        secondInnerLine.setStrokeWidth(0.333);
        secondInnerLine.setStrokeLineCap(StrokeLineCap.ROUND);
        secondInnerLine.setBlendMode(BlendMode.COLOR_DODGE);
        secondInnerLine.getStyleClass().add("callout-secondInnerLine");
        // Main title Rectangle
        mainTitleHBox = new HBox();
        mainTitleHBox.setBackground(
            new Background(
                new BackgroundFill(Color.SKYBLUE.deriveColor(1, 1, 1, 0.75),
                    new CornerRadii(3),
                    new Insets(0)))
        );
        mainTitleHBox.setBorder(new Border(new BorderStroke(
            Color.ALICEBLUE, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(2))));
        mainTitleHBox.getStyleClass().add("callout-mainTitleHBox");

        // Main title text
        mainTitleTextNode = new Text(getMainTitleText());
        HBox.setMargin(mainTitleTextNode, new Insets(5));
        mainTitleTextNode.setFont(Font.font(20));
        mainTitleTextNode.getStyleClass().add("callout-mainTitleTextNode");
        VBox mainTitleVBox = new VBox(5, mainTitleTextNode);
        mainTitleVBox.setPickOnBounds(false);
        mainTitleHBox.getChildren().add(mainTitleVBox);
        if (null != mainTitleNode)
            mainTitleVBox.getChildren().add(mainTitleNode);

        mainTitleHBox.setOnMousePressed(me -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        mainTitleHBox.setOnMouseDragged(me -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);
            mainTitleHBox.setTranslateX(mainTitleHBox.getTranslateX() + mouseDeltaX);
            mainTitleHBox.setTranslateY(mainTitleHBox.getTranslateY() + mouseDeltaY);
            subTitleHBox.setTranslateX(subTitleHBox.getTranslateX() + mouseDeltaX);
            subTitleHBox.setTranslateY(subTitleHBox.getTranslateY() + mouseDeltaY);
            secondLeaderLine.setEndX(secondLeaderLine.getEndX() + mouseDeltaX);
            secondLeaderLine.setEndY(secondLeaderLine.getEndY() + mouseDeltaY);
            secondInnerLine.setEndX(secondInnerLine.getEndX() + mouseDeltaX);
            secondInnerLine.setEndY(secondInnerLine.getEndY() + mouseDeltaY);
        });

        // Position sub tile rectangle under main title
        subTitleRect = new Rectangle(2, 20);
        subTitleRect.setFill(Color.WHITE);
        subTitleRect.getStyleClass().add("callout-subTitleRect");

        // Create the sub title
        subTitleHBox = new HBox();
        subTitleHBox.setBackground(
            new Background(
                new BackgroundFill(Color.color(0, 0, 0, .30),
                    new CornerRadii(0),
                    new Insets(0)))
        );
        subTitleHBox.getStyleClass().add("callout-subTitleHBox");

        subTitleTextNode = new Text(getSubTitleText());
        subTitleTextNode.setVisible(true);
        subTitleTextNode.setFill(Color.WHITE);
        subTitleTextNode.setFont(Font.font(14));
        subTitleTextNode.getStyleClass().add("callout-subTitleTextNode");
        subTitleHBox.getChildren().add(subTitleTextNode);

        // Build the animation code.
        buildAnimation(head,
            firstLeaderLine,
            firstInnerLine,
            secondLeaderLine,
            secondInnerLine,
            mainTitleHBox,
            subTitleRect,
            subTitleHBox);

        // Must add nodes after buildAnimation.
        // Positioning calculations are done
        // outside of this Group.
        getChildren().addAll(head,
            firstLeaderLine,
            firstInnerLine,
            secondLeaderLine,
            secondInnerLine,
            mainTitleHBox,
            subTitleRect,
            subTitleHBox);
        //Certain components should not block mouse picking
        head.setMouseTransparent(true);
        firstLeaderLine.setMouseTransparent(true);
        firstInnerLine.setMouseTransparent(true);
        secondLeaderLine.setMouseTransparent(true);
        secondInnerLine.setMouseTransparent(true);
//        mainTitleHBox.setMouseTransparent(true);
        subTitleRect.setMouseTransparent(true);
        subTitleHBox.setMouseTransparent(true);

        head.setPickOnBounds(false);
        firstLeaderLine.setPickOnBounds(false);
        firstInnerLine.setPickOnBounds(false);
        secondLeaderLine.setPickOnBounds(false);
        secondInnerLine.setPickOnBounds(false);
        mainTitleHBox.setPickOnBounds(false);
        subTitleRect.setPickOnBounds(false);
        subTitleHBox.setPickOnBounds(false);

        this.setPickOnBounds(false);
//        Glow glow = new Glow(0.5);
        ImageView callouts = ResourceUtils.loadIcon("callouts", ICON_FIT_HEIGHT);
        callouts.setEffect(glow);
        MenuItem closeThisItem = new MenuItem("Close This", callouts);
        closeThisItem.setOnAction(e -> {
            hide();
        });

        contextMenu = new ContextMenu(closeThisItem);
        contextMenu.setAutoFix(true);
        contextMenu.setAutoHide(true);
        contextMenu.setHideOnEscape(true);
        contextMenu.setOpacity(0.85);
        mainTitleHBox.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!contextMenu.isShowing())
                    contextMenu.show(this.getParent(), e.getScreenX(), e.getScreenY());
                else
                    contextMenu.hide();
                e.consume();
            }
        });

        //Hide at the start
        getChildren().forEach(node -> node.setVisible(false));
    }

    protected void buildAnimation(Node head,
                                  Line beginLeaderLine,
                                  Line beginInnerLine,
                                  Line endLeaderLine,
                                  Line endInnerLine,
                                  HBox mainTitle,
                                  Rectangle subTitleRect,
                                  HBox subTitle) {

        // generate a sequence animation
        calloutAnimation = new SequentialTransition();
        calloutAnimation.setCycleCount(1);
        calloutAnimation.setAutoReverse(false);

        // Animation of head
        calloutAnimation.getChildren().add(buildHeadAnim(head));

        // Animation of the beginning leader line.
        calloutAnimation.getChildren().add(buildBeginLeaderLineAnim(beginLeaderLine, beginInnerLine));

        // Animation of the ending leader line.
        calloutAnimation.getChildren().add(buildEndLeaderLineAnim(endLeaderLine, endInnerLine));

        // Animation of the main title
        calloutAnimation.getChildren().add(buildMainTitleAnim(mainTitle));

//        // Animation of the subtitle rectangle
//        calloutAnimation.getChildren().add(buildSubtitleRectAnim(mainTitle, subTitleRect));

        // Animation of the subtitle
        calloutAnimation.getChildren().add(buildSubTitleAnim(mainTitle, subTitle));

        Timeline pause = new Timeline(new KeyFrame(Duration.millis(getPauseTime() / 2)));
        calloutAnimation.getChildren().add(pause);

    }

    protected Rectangle2D getBoundsUpfront(Region node) {
        // Calculate main title width and height
        Group titleRoot = new Group();
        titleRoot.setPickOnBounds(false);
        new Scene(titleRoot);
        titleRoot.getChildren().add(node);
        titleRoot.applyCss();
        titleRoot.layout();
        return new Rectangle2D(0, 0, node.getWidth(), node.getHeight() * 10);
    }

    /**
     * Returns the x,y coordinate of the end point of the end leader line.
     *
     * @return Point2D the x,y coordinate of the end point of end leader line.
     */
    protected Point2D calcEndPointOfLeaderLine() {
        // Position of the end of the leader line
        double x = getLeaderLineToPoint().getX() + getEndLeaderLineLength() * getEndLeaderLineDirection();
        double y = getLeaderLineToPoint().getY();
        return new Point2D(x, y);
    }

    public void updateHeadPoint(double x, double y) {
        setHeadPoint(x, y);
        head.setCenterX(x);
        head.setCenterY(y);
        firstLeaderLine.setStartX(x);
        firstLeaderLine.setStartY(y);
        firstInnerLine.setStartX(x);
        firstInnerLine.setStartY(y);
    }

    public void updateLeaderLineEndPoint(double x, double y) {
        setLeaderLineToPoint(x, y);
        firstLeaderLine.setEndX(x);
        firstLeaderLine.setEndY(y);
        firstInnerLine.setEndX(x);
        firstInnerLine.setEndY(y);
    }

    /**
     * Create the head animation. The head can be any node or shape,
     * by default it's a circle.
     *
     * @param head The head is the starting point of the callout.
     * @return Animation of the head.
     */
    protected Animation buildHeadAnim(Node head) {
        Circle headCircle = (Circle) head;
        return new Timeline(
            new KeyFrame(Duration.millis(1),
                new KeyValue(headCircle.visibleProperty(), true),
                new KeyValue(headCircle.radiusProperty(), 0)), // show
            new KeyFrame(Duration.millis(200),
                new KeyValue(headCircle.radiusProperty(), 5.0d)) // max value
        );
    }

    protected Animation buildBeginLeaderLineAnim(Line firstLeaderLine, Line firstInnerLine) {
        return new Timeline(
            new KeyFrame(Duration.millis(1),
                new KeyValue(firstLeaderLine.visibleProperty(), true), // show
                new KeyValue(firstInnerLine.visibleProperty(), true)), // show

            new KeyFrame(Duration.millis(200),
                new KeyValue(firstLeaderLine.endXProperty(), getLeaderLineToPoint().getX()),
                new KeyValue(firstLeaderLine.endYProperty(), getLeaderLineToPoint().getY()),
                new KeyValue(firstInnerLine.endXProperty(), getLeaderLineToPoint().getX()),
                new KeyValue(firstInnerLine.endYProperty(), getLeaderLineToPoint().getY())
            )
        );
    }

    protected Animation buildEndLeaderLineAnim(Line endLeaderLine, Line endInnerLine) {
        return new Timeline(
            new KeyFrame(Duration.millis(1),
                new KeyValue(endLeaderLine.visibleProperty(), true), // show
                new KeyValue(endInnerLine.visibleProperty(), true)), // show
            new KeyFrame(Duration.millis(200),
                new KeyValue(endLeaderLine.endXProperty(),
                    calcEndPointOfLeaderLine().getX()),
                new KeyValue(endLeaderLine.endYProperty(), getLeaderLineToPoint().getY()),
                new KeyValue(endInnerLine.endXProperty(),
                    calcEndPointOfLeaderLine().getX()),
                new KeyValue(endInnerLine.endYProperty(), getLeaderLineToPoint().getY())
            )
        );
    }

    protected Animation buildMainTitleAnim(HBox mainTitleBackground) {

        // main title box
        // Calculate main title width and height upfront
        Rectangle2D mainTitleBounds = getBoundsUpfront(mainTitleBackground);

        double mainTitleWidth = mainTitleBounds.getWidth();
        double mainTitleHeight = mainTitleBounds.getHeight();

        // Position mainTitleText background beside the end part of the leader line.
        Point2D endPointLLine = calcEndPointOfLeaderLine();
        double x = endPointLLine.getX();
        double y = endPointLLine.getY();

        // Viewport to make main title appear to scroll
        Rectangle mainTitleViewPort = new Rectangle();
        mainTitleViewPort.setMouseTransparent(true);
        mainTitleViewPort.setPickOnBounds(false);
        mainTitleViewPort.setWidth(0);
        mainTitleViewPort.setHeight(mainTitleHeight);

        mainTitleBackground.setClip(mainTitleViewPort);
//        mainTitleBackground.setLayoutX(x);
        mainTitleBackground.setLayoutX(x - mainTitleWidth / 2.0);

//        mainTitleBackground.setLayoutY(y - (mainTitleHeight/2));
        mainTitleBackground.setLayoutY(y);

        // Animate main title from end point to the left.
        if (LEFT == getEndLeaderLineDirection()) {
            // animate layout x and width
            return new Timeline(
                new KeyFrame(Duration.millis(1),
                    new KeyValue(mainTitleBackground.visibleProperty(), true),
                    new KeyValue(mainTitleBackground.layoutXProperty(), x)
                ), // show
                new KeyFrame(Duration.millis(200),
                    new KeyValue(mainTitleBackground.layoutXProperty(), x - mainTitleWidth),
                    new KeyValue(mainTitleViewPort.widthProperty(), mainTitleWidth)
                )
            );
        }

        // Animate main title from end point to the right
        return new Timeline(
            new KeyFrame(Duration.millis(1),
                new KeyValue(mainTitleBackground.visibleProperty(), true)), // show
            new KeyFrame(Duration.millis(200),
                new KeyValue(mainTitleViewPort.widthProperty(), mainTitleWidth)
            )
        );
    }

    protected Animation buildSubtitleRectAnim(HBox mainTitleBackground, Rectangle subTitleRect) {

        // Small rectangle (prompt)
        // Calculate main title width and height upfront
        Rectangle2D mainTitleBounds = getBoundsUpfront(mainTitleBackground);

        double mainTitleWidth = mainTitleBounds.getWidth();
        double mainTitleHeight = mainTitleBounds.getHeight();

        // Position of the end
        Point2D endPointLL = calcEndPointOfLeaderLine();
        double x = endPointLL.getX();
        double y = endPointLL.getY();

        int direction = getEndLeaderLineDirection();
        if (direction == LEFT) {
            subTitleRect.setLayoutX(x + (subTitleRect.getWidth() * direction));
        } else {
            subTitleRect.setLayoutX(x);
        }

        subTitleRect.setLayoutY(y + (mainTitleHeight / 2) + 2);
        subTitleRect.layoutYProperty().bind(mainTitleBackground.layoutYProperty()
            .add(mainTitleBackground.heightProperty()
                .add(3)));
        return new Timeline(
            new KeyFrame(Duration.millis(1),
                new KeyValue(subTitleRect.visibleProperty(), true),
                new KeyValue(subTitleRect.heightProperty(), 0)), // show
            new KeyFrame(Duration.millis(200),
                new KeyValue(subTitleRect.heightProperty(), 20)
            )
        );
    }

    protected Animation buildSubTitleAnim(HBox mainTitle, HBox subTitle) {
        // Calculate main title width and height upfront
        Rectangle2D mainTitleBounds = getBoundsUpfront(mainTitle);

        double mainTitleWidth = mainTitleBounds.getWidth();
        double mainTitleHeight = mainTitleBounds.getHeight();

        Pos textPos = (LEFT == getEndLeaderLineDirection()) ? Pos.CENTER_LEFT : Pos.CENTER_RIGHT;
        subTitle.setAlignment(textPos);

        Rectangle2D subTitleBounds = getBoundsUpfront(subTitle);

        double subTitleTextWidth = subTitleBounds.getWidth();
        double subTitleTextHeight = subTitleBounds.getHeight();

        Point2D endPointLL = calcEndPointOfLeaderLine();
        int direction = getEndLeaderLineDirection();
        double x = endPointLL.getX() + (5 * direction);
        double y = endPointLL.getY();
//        subTitle.setLayoutX( x );
        subTitle.setLayoutX(x - mainTitleWidth / 2.0);
        subTitle.setLayoutY(y + (mainTitleHeight / 2.0) + 4);
//        subTitle.setLayoutY( y + 4);

        subTitle.layoutYProperty().bind(mainTitle.layoutYProperty()
            .add(mainTitle.heightProperty()
                .add(3)));

        Rectangle subTitleViewPort = new Rectangle();
        subTitleViewPort.setPickOnBounds(false);
        subTitleViewPort.setMouseTransparent(true);
        subTitleViewPort.setWidth(0);
        subTitleViewPort.setHeight(subTitleTextHeight);
        subTitle.setClip(subTitleViewPort);

        // Animate subtitle from end point to the left.
        if (LEFT == getEndLeaderLineDirection()) {
            return new Timeline(
                new KeyFrame(Duration.millis(1),
                    new KeyValue(subTitle.visibleProperty(), true),
                    new KeyValue(subTitle.layoutXProperty(), x)) // show
                , new KeyFrame(Duration.millis(200),
                new KeyValue(subTitle.layoutXProperty(), x - subTitleTextWidth),
                new KeyValue(subTitleViewPort.widthProperty(), subTitleTextWidth))
            );
        }

        // Animate subtitle from end point to the right.
        return new Timeline(
            new KeyFrame(Duration.millis(1),
                new KeyValue(subTitle.visibleProperty(), true)) // show
            , new KeyFrame(Duration.millis(200),
            new KeyValue(subTitleViewPort.widthProperty(), subTitleTextWidth))
        );
    }

    public void hide() {
        calloutAnimation.stop();
        setVisible(false);
    }

    public SequentialTransition play() {
        calloutAnimation.stop();
        calloutAnimation.setRate(1.0);
        getChildren().forEach(node -> node.setVisible(false));
        setVisible(true);
        if (calloutAnimation != null) {
            calloutAnimation.playFromStart();
        }
        return calloutAnimation;
    }

    public SequentialTransition play(double rate) {
        calloutAnimation.stop();
        getChildren().forEach(node -> node.setVisible(false));
        calloutAnimation.setRate(rate);
        setVisible(true);
        calloutAnimation.playFromStart();
        return calloutAnimation;
    }

    public Point2D getHeadPoint() {
        return headPoint;
    }

    public void setHeadPoint(double x, double y) {
        this.headPoint = new Point2D(x, y);
    }

    public Point2D getLeaderLineToPoint() {
        return leaderLineToPoint;
    }

    public void setLeaderLineToPoint(double x, double y) {
        this.leaderLineToPoint = new Point2D(x, y);
    }

    public double getEndLeaderLineLength() {
        return endLeaderLineLength;
    }

    public void setEndLeaderLineLength(double endLeaderLineLength) {
        this.endLeaderLineLength = endLeaderLineLength;
    }

    public int getEndLeaderLineDirection() {
        return endLeaderLineDirection;
    }

    public void setEndLeaderLineDirection(int endLeaderLineDirection) {
        this.endLeaderLineDirection = endLeaderLineDirection;
    }

    public String getMainTitleText() {
        return mainTitleText;
    }

    public void setMainTitleText(String mainTitleText) {
        this.mainTitleText = mainTitleText;
    }

    public String getSubTitleText() {
        return subTitleText;
    }

    public void setSubTitleText(String subTitleText) {
        this.subTitleText = subTitleText;
    }

    public long getPauseTime() {
        return pauseInMillis;
    }

    public void setPauseTime(long pauseInMillis) {
        this.pauseInMillis = pauseInMillis;
    }
}
