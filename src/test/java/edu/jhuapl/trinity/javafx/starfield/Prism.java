package edu.jhuapl.trinity.javafx.starfield;

/*-
 * #%L
 * trinity
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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;


public class Prism extends Application {

    public static ObservableList<Node> child;
    //
    private static final String title = "JellyBeanci(c) Pink Floyd";
    public static final int width = 1200;
    public static final int height = 800;
    private static Color backcolor = Color.rgb(51, 51, 51);

    private final Point2D view = new Point2D(-1, height / 2 + 200);
    private final Circle viewCircle = new Circle(view.getX(), view.getY(), 4);

    //private ArrayList<Line> lines = new ArrayList<>();
    //private ArrayList<Point2D> points = new ArrayList<>();


    private Point2D mouse = new Point2D(0, 0);

    private Point2D center = new Point2D(width / 2, height / 2 + 75);

    private double angle = 0;
    private boolean intersect = false;

    private Line ray = new Line(-10, -10, -10, -10);
    private ArrayList<Line> grayRays = new ArrayList<>();
    private ArrayList<Line> colorRays = new ArrayList<>();

    private ArrayList<Line> lines = new ArrayList<>();

    private double deflect = 5;

    private static Timeline update;

    @Override
    public void start(Stage stage) throws Exception {
        Pane root = new Pane();
        child = root.getChildren();
        //
        Point2D up = Utils.endPoint(center, 90, 300);
        Point2D left = Utils.endPoint(center, 210, 300);
        Point2D right = Utils.endPoint(center, 330, 300);
        Polygon triangle = new Polygon(up.getX(), up.getY(), left.getX(), left.getY(), right.getX(), right.getY());
        triangle.setStroke(Color.SNOW);
        triangle.setStrokeWidth(15);
        triangle.setFill(Color.TRANSPARENT);
        //
        viewCircle.setFill(Color.GREEN);
        //
        for (int i = 0; i < 25; i++) {
            grayRays.add(new Line(-10, -10, -10, -10));
        }
        for (int i = 0; i < grayRays.size(); i++) {
            colorRays.add(new Line(-10, -10, -10, -10));
        }


        for (int i = 0; i < grayRays.size(); i++) {
            Line line = grayRays.get(i);
            int v = 180 - (120 / grayRays.size()) * i;
            line.setStroke(Color.rgb(v, v, v));
            line.setStrokeWidth(8);
            child.add(line);
        }
        for (int i = 0; i < colorRays.size(); i++) {
            Line line = colorRays.get(i);
            int hsv = 0 + (300 / colorRays.size()) * i;
            line.setStroke(Color.hsb(hsv, 1, 1));
            line.setStrokeWidth(13);
            child.add(line);
        }
        child.addAll(ray, triangle);
        //
        Line line1 = new Line(up.getX(), up.getY(), left.getX(), left.getY());
        Line line2 = new Line(left.getX(), left.getY(), right.getX(), right.getY());
        Line line3 = new Line(right.getX(), right.getY(), up.getX(), up.getY());
        lines.add(line2);
        lines.add(line3);
        //
        ray.setStrokeWidth(10);
        ray.setStroke(Color.SNOW);
        //
        root.setOnMouseMoved(e -> {
            this.mouse = new Point2D(e.getSceneX(), e.getSceneY());
            angle = Utils.calculateAngle(view, mouse);
        });

        //
        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case F1: {
                    //PLAY
                    update.play();
                    stage.setTitle(title + "[ON]");
                    break;
                }
                case F2: {
                    //PAUSE
                    update.pause();
                    stage.setTitle(title + "[OFF]");
                    break;
                }
                case F3: {
                    //Show Child Count
                    System.out.println("Child Count: " + child.size());
                    break;
                }
                case F5: {
                    deflect += 1;
                    break;
                }
                case F6: {
                    deflect -= 1;
                    break;
                }
            }
        });
        //
        update = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            //loop
            intersect = false;
            //Point2D closest = new Point2D(0, 0);
            //double record = Double.MAX_VALUE;
            ray.setStartX(view.getX());
            ray.setStartY(view.getY());
            ray.setEndX(mouse.getX());
            ray.setEndY(mouse.getY());
            //
            Point2D p1 = Utils.lineXlineIntersection(line1, ray);
            if (Utils.isIntersecting(line1, p1)) {
                intersect = true;
            }

            if (intersect) {
                ray.setEndX(p1.getX());
                ray.setEndY(p1.getY());
                //phase l1 = 60degree
                Point2D p2;
                for (int i = 0; i < grayRays.size(); i++) {
                    Line ray2 = grayRays.get(i);
                    p2 = Utils.endPoint(p1, angle - (deflect + i), 500);
                    ray2.setStartX(p1.getX());
                    ray2.setStartY(p1.getY());
                    ray2.setEndX(p2.getX());
                    ray2.setEndY(p2.getY());
                }
                for (Line ray2 : grayRays) {
                    for (int i = 0; i < lines.size(); i++) {
                        Line current = lines.get(i);
                        Point2D p3 = Utils.lineXlineIntersection(ray2, current);
                        if (Utils.isIntersecting(current, p3)) {
                            //intersecting
                            ray2.setEndX(p3.getX());
                            ray2.setEndY(p3.getY());
                        }
                    }
                }
                //
                for (int i = 0; i < colorRays.size(); i++) {
                    Line ray3 = colorRays.get(i);
                    Line ray2 = grayRays.get(i);
                    Point2D curr = new Point2D(ray2.getEndX(), ray2.getEndY());
                    ray3.setStartX(curr.getX());
                    ray3.setStartY(curr.getY());
                    Point2D p4 = Utils.endPoint(curr, angle - (5 * deflect + i * 1.01), 1000);
                    ray3.setEndX(p4.getX());
                    ray3.setEndY(p4.getY());
                }
            } else {
                for (Line ray2 : grayRays) {
                    //move to outer screen
                    ray2.setStartX(-10);
                    ray2.setStartY(-10);
                    ray2.setEndX(-10);
                    ray2.setEndY(-10);
                }
                for (Line ray3 : colorRays) {
                    //move to outer screen
                    ray3.setStartX(-10);
                    ray3.setStartY(-10);
                    ray3.setEndX(-10);
                    ray3.setEndY(-10);
                }
            }
            //
        }));
        update.setCycleCount(Timeline.INDEFINITE);
        update.setRate(1);
        update.setAutoReverse(false);
        //update.play(); //uncomment for play when start
        //
        stage.setTitle(title + " Press [F1] to Start");
        stage.setResizable(false);
        stage.setScene(new Scene(root, width - 10, height - 10, backcolor));
        stage.show();
        root.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
