package edu.jhuapl.trinity.javafx.components;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Lightweight Canvas-backed line chart intended for rapidly changing dense vectors.
 *
 * <p>Unlike JavaFX {@code LineChart}, this control does not create a scene-graph node
 * for every sample. The complete input vector is rendered directly into a Canvas and
 * hover inspection is handled by one overlay Canvas plus one reusable Label.</p>
 *
 * @author Sean Phillips
 */
public final class CanvasLineChart extends Pane {

    private static final double LEFT_MARGIN = 54.0;
    private static final double RIGHT_MARGIN = 12.0;
    private static final double TOP_MARGIN = 12.0;
    private static final double BOTTOM_MARGIN = 28.0;
    private static final int GRID_DIVISIONS = 4;

    private final Canvas plotCanvas = new Canvas();
    private final Canvas hoverCanvas = new Canvas();
    private final Label hoverLabel = new Label();

    private double[] values = new double[0];
    private double dataMinimum = Double.NaN;
    private double dataMaximum = Double.NaN;
    private int hoverIndex = -1;
    private String hoverPrefix = "Vector";

    public CanvasLineChart() {
        setMinSize(0.0, 0.0);
        setPrefSize(420.0, 150.0);

        plotCanvas.setManaged(false);
        hoverCanvas.setManaged(false);
        hoverCanvas.setMouseTransparent(false);

        hoverLabel.setManaged(false);
        hoverLabel.setMouseTransparent(true);
        hoverLabel.setVisible(false);
        hoverLabel.setStyle(
            "-fx-font-size: 13;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: aliceblue;"
                + "-fx-background-color: rgba(20, 24, 30, 0.86);"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 4 7 4 7;"
        );

        getChildren().addAll(plotCanvas, hoverCanvas, hoverLabel);

        hoverCanvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
        hoverCanvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> clearHover());
    }

    public void setHoverPrefix(String hoverPrefix) {
        this.hoverPrefix = hoverPrefix == null || hoverPrefix.isBlank()
            ? "Vector"
            : hoverPrefix;
    }

    public void setData(Double[] newData) {
        if (newData == null || newData.length == 0) {
            values = new double[0];
            dataMinimum = Double.NaN;
            dataMaximum = Double.NaN;
            clearHover();
            redraw();
            return;
        }

        double[] primitive = new double[newData.length];
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < newData.length; i++) {
            Double boxed = newData[i];
            double value = boxed == null ? Double.NaN : boxed;
            primitive[i] = value;
            if (Double.isFinite(value)) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        values = primitive;
        dataMinimum = min == Double.POSITIVE_INFINITY ? Double.NaN : min;
        dataMaximum = max == Double.NEGATIVE_INFINITY ? Double.NaN : max;
        hoverIndex = -1;
        hoverLabel.setVisible(false);
        clearHoverCanvas();
        redraw();
    }

    public void setData(double[] newData) {
        if (newData == null || newData.length == 0) {
            values = new double[0];
            dataMinimum = Double.NaN;
            dataMaximum = Double.NaN;
            clearHover();
            redraw();
            return;
        }

        values = newData.clone();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (Double.isFinite(value)) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }
        dataMinimum = min == Double.POSITIVE_INFINITY ? Double.NaN : min;
        dataMaximum = max == Double.NEGATIVE_INFINITY ? Double.NaN : max;
        hoverIndex = -1;
        hoverLabel.setVisible(false);
        clearHoverCanvas();
        redraw();
    }

    @Override
    protected void layoutChildren() {
        double width = Math.max(0.0, getWidth());
        double height = Math.max(0.0, getHeight());
        if (plotCanvas.getWidth() != width) plotCanvas.setWidth(width);
        if (plotCanvas.getHeight() != height) plotCanvas.setHeight(height);
        if (hoverCanvas.getWidth() != width) hoverCanvas.setWidth(width);
        if (hoverCanvas.getHeight() != height) hoverCanvas.setHeight(height);
        plotCanvas.relocate(0.0, 0.0);
        hoverCanvas.relocate(0.0, 0.0);
        redraw();
    }

    private void redraw() {
        GraphicsContext g = plotCanvas.getGraphicsContext2D();
        double width = plotCanvas.getWidth();
        double height = plotCanvas.getHeight();
        g.clearRect(0.0, 0.0, width, height);
        if (width <= LEFT_MARGIN + RIGHT_MARGIN || height <= TOP_MARGIN + BOTTOM_MARGIN) return;

        double plotX = LEFT_MARGIN;
        double plotY = TOP_MARGIN;
        double plotWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
        double plotHeight = height - TOP_MARGIN - BOTTOM_MARGIN;

        drawGridAndAxes(g, plotX, plotY, plotWidth, plotHeight);
        if (values.length == 0 || !Double.isFinite(dataMinimum) || !Double.isFinite(dataMaximum)) {
            drawCenteredMessage(g, width, height, "No data");
            return;
        }

        double yMin = dataMinimum;
        double yMax = dataMaximum;
        if (Double.compare(yMin, yMax) == 0) {
            double padding = Math.max(1.0, Math.abs(yMin) * 0.05);
            yMin -= padding;
            yMax += padding;
        } else {
            double padding = (yMax - yMin) * 0.05;
            yMin -= padding;
            yMax += padding;
        }

        drawAxisLabels(g, plotX, plotY, plotWidth, plotHeight, yMin, yMax);
        drawSeries(g, plotX, plotY, plotWidth, plotHeight, yMin, yMax);
    }

    private void drawGridAndAxes(GraphicsContext g,
                                 double x,
                                 double y,
                                 double width,
                                 double height) {
        g.setLineWidth(1.0);
        g.setStroke(Color.rgb(180, 190, 200, 0.22));
        for (int i = 0; i <= GRID_DIVISIONS; i++) {
            double fraction = i / (double) GRID_DIVISIONS;
            double gy = y + height * fraction;
            g.strokeLine(x, gy, x + width, gy);
            double gx = x + width * fraction;
            g.strokeLine(gx, y, gx, y + height);
        }

        g.setStroke(Color.rgb(210, 220, 230, 0.72));
        g.strokeLine(x, y, x, y + height);
        g.strokeLine(x, y + height, x + width, y + height);
    }

    private void drawAxisLabels(GraphicsContext g,
                                double x,
                                double y,
                                double width,
                                double height,
                                double yMin,
                                double yMax) {
        g.setFill(Color.rgb(220, 225, 232, 0.90));
        g.setFont(Font.font(11.0));
        g.setTextBaseline(VPos.CENTER);

        g.setTextAlign(TextAlignment.RIGHT);
        for (int i = 0; i <= GRID_DIVISIONS; i++) {
            double fraction = i / (double) GRID_DIVISIONS;
            double value = yMax - (yMax - yMin) * fraction;
            double py = y + height * fraction;
            g.fillText(formatValue(value), x - 6.0, py);
        }

        g.setTextBaseline(VPos.TOP);
        g.setTextAlign(TextAlignment.CENTER);
        int lastIndex = Math.max(0, values.length - 1);
        for (int i = 0; i <= GRID_DIVISIONS; i++) {
            double fraction = i / (double) GRID_DIVISIONS;
            int index = (int) Math.round(lastIndex * fraction);
            double px = x + width * fraction;
            g.fillText(Integer.toString(index), px, y + height + 6.0);
        }
    }

    private void drawSeries(GraphicsContext g,
                            double x,
                            double y,
                            double width,
                            double height,
                            double yMin,
                            double yMax) {
        double yRange = yMax - yMin;
        int lastIndex = Math.max(1, values.length - 1);

        g.setStroke(Color.ALICEBLUE);
        g.setLineWidth(1.35);
        g.beginPath();

        boolean pathStarted = false;
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (!Double.isFinite(value)) {
                pathStarted = false;
                continue;
            }

            double px = x + width * (i / (double) lastIndex);
            double py = y + height * ((yMax - value) / yRange);
            if (!pathStarted) {
                g.moveTo(px, py);
                pathStarted = true;
            } else {
                g.lineTo(px, py);
            }
        }
        g.stroke();
    }

    private void handleMouseMoved(MouseEvent event) {
        if (values.length == 0 || !Double.isFinite(dataMinimum) || !Double.isFinite(dataMaximum)) {
            clearHover();
            return;
        }

        double width = hoverCanvas.getWidth();
        double height = hoverCanvas.getHeight();
        double plotWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
        double plotHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        if (plotWidth <= 0.0 || plotHeight <= 0.0) return;

        double clampedX = clamp(event.getX(), LEFT_MARGIN, LEFT_MARGIN + plotWidth);
        double fraction = (clampedX - LEFT_MARGIN) / plotWidth;
        int index = (int) Math.round(fraction * Math.max(0, values.length - 1));
        index = Math.max(0, Math.min(values.length - 1, index));
        double value = values[index];
        if (!Double.isFinite(value)) {
            clearHover();
            return;
        }

        hoverIndex = index;
        drawHover(index, value);
        hoverLabel.setText(hoverPrefix + "(" + index + ") = " + formatValue(value));
        hoverLabel.applyCss();
        double labelWidth = hoverLabel.prefWidth(-1.0);
        double labelHeight = hoverLabel.prefHeight(-1.0);
        double labelX = clamp(event.getX() + 10.0, 0.0, Math.max(0.0, width - labelWidth));
        double labelY = clamp(event.getY() - labelHeight - 10.0, 0.0, Math.max(0.0, height - labelHeight));
        hoverLabel.resizeRelocate(labelX, labelY, labelWidth, labelHeight);
        hoverLabel.setVisible(true);
    }

    private void drawHover(int index, double value) {
        clearHoverCanvas();

        double width = hoverCanvas.getWidth();
        double height = hoverCanvas.getHeight();
        double plotWidth = width - LEFT_MARGIN - RIGHT_MARGIN;
        double plotHeight = height - TOP_MARGIN - BOTTOM_MARGIN;

        double yMin = dataMinimum;
        double yMax = dataMaximum;
        if (Double.compare(yMin, yMax) == 0) {
            double padding = Math.max(1.0, Math.abs(yMin) * 0.05);
            yMin -= padding;
            yMax += padding;
        } else {
            double padding = (yMax - yMin) * 0.05;
            yMin -= padding;
            yMax += padding;
        }

        int lastIndex = Math.max(1, values.length - 1);
        double px = LEFT_MARGIN + plotWidth * (index / (double) lastIndex);
        double py = TOP_MARGIN + plotHeight * ((yMax - value) / (yMax - yMin));

        GraphicsContext g = hoverCanvas.getGraphicsContext2D();
        g.setStroke(Color.rgb(240, 248, 255, 0.50));
        g.setLineWidth(1.0);
        g.strokeLine(px, TOP_MARGIN, px, TOP_MARGIN + plotHeight);
        g.setFill(Color.ALICEBLUE);
        g.fillOval(px - 3.0, py - 3.0, 6.0, 6.0);
    }

    private void clearHover() {
        hoverIndex = -1;
        hoverLabel.setVisible(false);
        clearHoverCanvas();
    }

    private void clearHoverCanvas() {
        GraphicsContext g = hoverCanvas.getGraphicsContext2D();
        g.clearRect(0.0, 0.0, hoverCanvas.getWidth(), hoverCanvas.getHeight());
    }

    private static void drawCenteredMessage(GraphicsContext g,
                                            double width,
                                            double height,
                                            String message) {
        g.setFill(Color.rgb(220, 225, 232, 0.80));
        g.setFont(Font.font(12.0));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(message, width / 2.0, height / 2.0);
    }

    private static String formatValue(double value) {
        double abs = Math.abs(value);
        if ((abs > 0.0 && abs < 0.001) || abs >= 100000.0) {
            return String.format("%.3e", value);
        }
        if (abs >= 1000.0) return String.format("%.1f", value);
        if (abs >= 10.0) return String.format("%.2f", value);
        return String.format("%.4f", value);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
