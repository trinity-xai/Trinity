package edu.jhuapl.trinity.javafx.components.panes;

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

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

/**
 * A Pane containing a dynamic resizable canvas.
 * A canvas is transparent allowing a user to draw.
 * To debug the resizing setDebugBorder() to true.
 *
 * @author Carl Dea, Sean Phillips
 * Taken from LitFX
 */
public class CanvasOverlayPane extends Pane {

    private Canvas canvas;

    private boolean clearCanvas;

    private Consumer<GraphicsContext> drawBackground;

    private boolean debugBorder;

    public CanvasOverlayPane() {
        this(false, true);
    }

    public CanvasOverlayPane(boolean debugBorder, boolean clearCanvas) {
        this(new Canvas(), debugBorder, clearCanvas);
    }

    public CanvasOverlayPane(Canvas canvas, boolean debugBorder, boolean clearCanvas) {
        this.canvas = canvas;
        this.debugBorder = debugBorder;
        this.clearCanvas = clearCanvas;
        getChildren().add(canvas);
        init();
    }

    public void init() {

        if (debugBorder) {
            setBorder(new Border(new BorderStroke(Color.WHITE,
                BorderStrokeStyle.DASHED,
                new CornerRadii(0, false),
                new BorderWidths(2.0))));
        }

        setPickOnBounds(false); // allows you to click to pass through.
        setMouseTransparent(true);
        getCanvas().setPickOnBounds(false);
        getCanvas().setMouseTransparent(true);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas canvas) {
        getChildren().remove(this.canvas);
        getChildren().add(canvas);
    }

    public void setClearCanvasOnResize(boolean clearCanvas) {
        this.clearCanvas = clearCanvas;
    }

    public boolean isDebugBorder() {
        return debugBorder;
    }

    public void setDebugBorder(boolean debugBorder) {
        this.debugBorder = debugBorder;
        init();
    }

    public void drawBackground(Consumer<GraphicsContext> drawBackground) {
        this.drawBackground = drawBackground;
    }

    @Override
    protected void layoutChildren() {
        final int top = (int) snappedTopInset();
        final int right = (int) snappedRightInset();
        final int bottom = (int) snappedBottomInset();
        final int left = (int) snappedLeftInset();
        final int w = (int) getWidth() - left - right;
        final int h = (int) getHeight() - top - bottom;
        canvas.setLayoutX(left);
        canvas.setLayoutY(top);
        if (w != canvas.getWidth() || h != canvas.getHeight()) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            GraphicsContext g = canvas.getGraphicsContext2D();

            if (clearCanvas) {
                g.clearRect(0, 0, w, h);
            }

            if (drawBackground != null) {
                drawBackground.accept(g);
            }

        }
    }
}
