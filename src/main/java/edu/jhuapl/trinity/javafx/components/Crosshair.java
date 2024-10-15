/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * @author Sean Phillips
 */
public class Crosshair extends Group {
    public Line leftHorizontalLine;
    public Line topVerticalLine;
    public Line rightHorizontalLine;
    public Line bottomVerticalLine;
    public SimpleObjectProperty<Paint> leftStrokePaint = new SimpleObjectProperty<>(Color.STEELBLUE);
    public SimpleObjectProperty<Paint> rightStrokePaint = new SimpleObjectProperty<>(Color.STEELBLUE);
    public SimpleObjectProperty<Paint> topStrokePaint = new SimpleObjectProperty<>(Color.STEELBLUE);
    public SimpleObjectProperty<Paint> bottomStrokePaint = new SimpleObjectProperty<>(Color.STEELBLUE);
    Node parent;
    public boolean mouseEnabled = true;

    public Crosshair(Node parent) {
        this.parent = parent;

        leftHorizontalLine = new Line(0, 0, 0, 0);
        topVerticalLine = new Line(0, 0, 0, 0);
        rightHorizontalLine = new Line(0, 0, 0, 0);
        bottomVerticalLine = new Line(0, 0, 0, 0);

        leftHorizontalLine.strokeProperty().bind(leftStrokePaint);
        leftHorizontalLine.setStrokeDashOffset(50);
        leftHorizontalLine.setStrokeWidth(1);

        topVerticalLine.setStrokeDashOffset(50);
        topVerticalLine.setStrokeWidth(1);
        topVerticalLine.strokeProperty().bind(topStrokePaint);

        rightHorizontalLine.setStrokeDashOffset(50);
        rightHorizontalLine.setStrokeWidth(1);
        rightHorizontalLine.strokeProperty().bind(rightStrokePaint);

        bottomVerticalLine.setStrokeDashOffset(50);
        bottomVerticalLine.setStrokeWidth(1);
        bottomVerticalLine.strokeProperty().bind(bottomStrokePaint);

        getChildren().addAll(leftHorizontalLine, topVerticalLine,
            rightHorizontalLine, bottomVerticalLine);

        this.parent.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (mouseEnabled) {
                setVisible(true);
                this.parent.setCursor(Cursor.NONE);
            }
        });
        this.parent.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (mouseEnabled) {
                setVisible(false);
                this.parent.setCursor(Cursor.DEFAULT);
            }
        });
        this.parent.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (mouseEnabled) {
                updateCrossHair(e.getX(), e.getY());
            }
        });
        this.parent.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (mouseEnabled) {
                setVisible(false);
            }
        });

        this.parent.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (mouseEnabled) {
                setVisible(true);
                updateCrossHair(e.getX(), e.getY());
            }
        });
    }

    public void setCenter(double x, double y) {
        double width = parent.getBoundsInParent().getWidth();
        double height = parent.getBoundsInParent().getHeight();
        double gap = 20;

        leftHorizontalLine.setStartY(y);
        leftHorizontalLine.setEndY(y);
        leftHorizontalLine.setStartX(x - gap);
        leftHorizontalLine.setEndX(gap);

        topVerticalLine.setStartX(x);
        topVerticalLine.setEndX(x);
        topVerticalLine.setStartY(y - gap);
        topVerticalLine.setEndY(gap);

        rightHorizontalLine.setStartY(y);
        rightHorizontalLine.setEndY(y);
        rightHorizontalLine.setStartX(x + gap);
        rightHorizontalLine.setEndX(width - gap);

        bottomVerticalLine.setStartX(x);
        bottomVerticalLine.setEndX(x);
        bottomVerticalLine.setStartY(y + gap);
        bottomVerticalLine.setEndY(height - gap);
    }

    public void updateCrossHair(double mouseX, double mouseY) {
        double width = parent.getBoundsInParent().getWidth();
        double height = parent.getBoundsInParent().getHeight();
        double gap = 20;
        if ((mouseX > gap && mouseX < width - gap)
            && (mouseY > gap && mouseY < height - gap)) {
//            Platform.runLater(() -> {
            leftHorizontalLine.setStartY(mouseY);
            leftHorizontalLine.setEndY(mouseY);
            leftHorizontalLine.setStartX(mouseX - gap);
            leftHorizontalLine.setEndX(gap);

            topVerticalLine.setStartX(mouseX);
            topVerticalLine.setEndX(mouseX);
            topVerticalLine.setStartY(mouseY - gap);
            topVerticalLine.setEndY(gap);

            rightHorizontalLine.setStartY(mouseY);
            rightHorizontalLine.setEndY(mouseY);
            rightHorizontalLine.setStartX(mouseX + gap);
            rightHorizontalLine.setEndX(width - gap);

            bottomVerticalLine.setStartX(mouseX);
            bottomVerticalLine.setEndX(mouseX);
            bottomVerticalLine.setStartY(mouseY + gap);
            bottomVerticalLine.setEndY(height - gap);
//            });
        }
    }
}
