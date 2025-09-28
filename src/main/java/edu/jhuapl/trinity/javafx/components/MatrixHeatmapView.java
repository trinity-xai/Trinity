package edu.jhuapl.trinity.javafx.components;

import java.text.DecimalFormat;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/**
 * MatrixHeatmapView
 * -----------------
 * Reusable JavaFX control to render a 2D numeric matrix as a heatmap.
 * - Renders on Canvas (fast, lightweight).
 * - Sequential and diverging palettes.
 * - Auto or fixed range mapping.
 * - Optional color legend.
 * - Axis labels (top / left).
 * - Hover tooltips with (row, col, value).
 * - Click callback with cell picking.
 *
 * Public API is intentionally generic so it can be reused by multiple Trinity tools.
 *
 * @author Sean Phillips
 */
public final class MatrixHeatmapView extends BorderPane {

    // -------------------- Types --------------------
    // Compatibility enums (simple placeholders for future options)
    public enum ValueMode { RAW, ABS_VALUE }
    public enum ScaleMode { AUTO, FIXED }

    /** Color palette styles. */
    public enum PaletteKind {
        SEQUENTIAL,   // low -> high mapped 0..1
        DIVERGING     // < center on cool, > center on warm (center maps to mid color)
    }

    /** Simple click payload. */
    public static final class MatrixClick {
        public final int row;
        public final int col;
        public final double value;

        public MatrixClick(int row, int col, double value) {
            this.row = row;
            this.col = col;
            this.value = value;
        }
    }

    // -------------------- State --------------------

    private double[][] matrix = new double[0][0]; // row-major [rows][cols]
    private String[] rowLabels = new String[0];   // optional
    private String[] colLabels = new String[0];   // optional

    // Rendering & layout knobs
    private final Canvas canvas = new Canvas();
    private final Insets padding = new Insets(6, 8, 8, 8);

    // Reusable node for text measurement to avoid per-call allocations
    private final Text measureText = new Text();

    // Axis label styling
    private Font labelFont = Font.font("Arial", 12);
    private double labelMargin = 4.0;

    // Legend controls
    private boolean showLegend = true;
    private double legendWidth = 16.0;
    private double legendGap = 8.0;

    // Palette & range
    private PaletteKind paletteKind = PaletteKind.SEQUENTIAL;
    private boolean autoRange = true;
    private Double fixedMin = null;
    private Double fixedMax = null;
    private Double divergingCenter = 0.0; // for DIVERGING only

    // Column label compaction (e.g., "Comp 12" -> "12")
    private boolean compactColumnLabels = false;

    // Interaction
    private Consumer<MatrixClick> onCellClick = null;
    private final Tooltip hoverTip = new Tooltip();
    private final DecimalFormat df = new DecimalFormat("0.####");

    // Cached draw metrics
    private double contentX;
    private double contentY;
    private double contentW;
    private double contentH;

    // -------------------- Construction --------------------

    public MatrixHeatmapView() {
        setPadding(padding);
        setCenter(canvas);

        // Resize canvas when control size changes
        InvalidationListener resizer = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                layoutAndDraw();
            }
        };
        widthProperty().addListener(resizer);
        heightProperty().addListener(resizer);

        // Mouse events for hover + click
        Tooltip.install(canvas, hoverTip);
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMove);
        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> hoverTip.hide());
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClick);

        // Initial size preference (can be overridden by parent)
        setPrefSize(640, 420);
        canvas.setWidth(getPrefWidth());
        canvas.setHeight(getPrefHeight());
    }

    // -------------------- Public API --------------------

    /** Set a new matrix. Null or empty → clears view. */
    public void setMatrix(double[][] values) {
        if (values == null || values.length == 0 || values[0].length == 0) {
            this.matrix = new double[0][0];
            this.rowLabels = new String[0];
            this.colLabels = new String[0];
        } else {
            int rows = values.length;
            int cols = values[0].length;
            this.matrix = new double[rows][cols];
            for (int r = 0; r < rows; r++) {
                if (values[r].length != cols) {
                    throw new IllegalArgumentException("Matrix rows must have equal length.");
                }
                System.arraycopy(values[r], 0, this.matrix[r], 0, cols);
            }
            if (rowLabels.length != rows) {
                rowLabels = defaultLabels(rows, "r");
            }
            if (colLabels.length != cols) {
                colLabels = defaultLabels(cols, "c");
            }
        }
        layoutAndDraw();
    }

    /** Convenience: accept List<List<Double>>. */
    public void setMatrix(List<List<Double>> values) {
        if (values == null || values.isEmpty()) {
            setMatrix((double[][]) null);
            return;
        }
        int rows = values.size();
        int cols = values.get(0).size();
        double[][] arr = new double[rows][cols];
        for (int r = 0; r < rows; r++) {
            List<Double> row = values.get(r);
            if (row.size() != cols) {
                throw new IllegalArgumentException("Matrix rows must have equal length.");
            }
            for (int c = 0; c < cols; c++) {
                Double v = row.get(c);
                arr[r][c] = (v == null || Double.isNaN(v)) ? 0.0 : v.doubleValue();
            }
        }
        setMatrix(arr);
    }

    /** Optional row labels (length must equal rows). */
    public void setRowLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            this.rowLabels = new String[rows()];
        } else {
            if (labels.size() != rows()) {
                throw new IllegalArgumentException("Row labels length must match matrix rows.");
            }
            this.rowLabels = labels.toArray(new String[0]);
        }
        layoutAndDraw();
    }

    /** Optional column labels (length must equal cols). */
    public void setColLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            this.colLabels = new String[cols()];
        } else {
            if (labels.size() != cols()) {
                throw new IllegalArgumentException("Column labels length must match matrix cols.");
            }
            this.colLabels = labels.toArray(new String[0]);
        }
        layoutAndDraw();
    }

    /** Set a uniform label prefix if none provided yet (e.g., "F0..F(n-1)"). */
    public void setDefaultAxisLabels(String rowPrefix, String colPrefix) {
        if (rows() > 0 && (rowLabels == null || rowLabels.length != rows())) {
            rowLabels = defaultLabels(rows(), rowPrefix == null ? "r" : rowPrefix);
        }
        if (cols() > 0 && (colLabels == null || colLabels.length != cols())) {
            colLabels = defaultLabels(cols(), colPrefix == null ? "c" : colPrefix);
        }
        layoutAndDraw();
    }

    /** Sequential palette. */
    public void useSequentialPalette() {
        this.paletteKind = PaletteKind.SEQUENTIAL;
        layoutAndDraw();
    }

    /** Diverging palette (values below/above center split colors). */
    public void useDivergingPalette(double center) {
        this.paletteKind = PaletteKind.DIVERGING;
        this.divergingCenter = center;
        layoutAndDraw();
    }

    /** Auto-range on (min/max derived from current matrix). */
    public void setAutoRange(boolean on) {
        this.autoRange = on;
        layoutAndDraw();
    }

    /** Fixed range mapping. */
    public void setFixedRange(double vmin, double vmax) {
        if (!(vmax > vmin)) {
            throw new IllegalArgumentException("vmax must be greater than vmin.");
        }
        this.autoRange = false;
        this.fixedMin = vmin;
        this.fixedMax = vmax;
        layoutAndDraw();
    }

    /** Show or hide the color legend bar. */
    public void setShowLegend(boolean show) {
        this.showLegend = show;
        layoutAndDraw();
    }

    /** Set axis label font. */
    public void setLabelFont(Font f) {
        if (f != null) {
            this.labelFont = f;
            layoutAndDraw();
        }
    }

    /** When true, column labels like "Comp 12" are rendered without the "Comp " prefix. */
    public void setCompactColumnLabels(boolean on) {
        if (this.compactColumnLabels != on) {
            this.compactColumnLabels = on;
            layoutAndDraw();
        }
    }

    /** Set click handler for cell picks. */
    public void setOnCellClick(Consumer<MatrixClick> handler) {
        this.onCellClick = handler;
    }

    /** Return a defensive copy of the matrix (for external reads). */
    public double[][] getMatrixCopy() {
        int r = rows(), c = cols();
        double[][] out = new double[r][c];
        for (int i = 0; i < r; i++) {
            System.arraycopy(matrix[i], 0, out[i], 0, c);
        }
        return out;
    }

    // -------------------- Internals: sizing + render --------------------

    private int rows() { return matrix.length; }
    private int cols() { return (rows() == 0) ? 0 : matrix[0].length; }

    private void layoutAndDraw() {
        double w = Math.max(1, getWidth() - getInsets().getLeft() - getInsets().getRight());
        double h = Math.max(1, getHeight() - getInsets().getTop() - getInsets().getBottom());
        canvas.setWidth(w);
        canvas.setHeight(h);
        draw();
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Clear
        g.setFill(Color.web("#1E1E1E"));
        g.fillRect(0, 0, w, h);

        int r = rows();
        int c = cols();
        if (r == 0 || c == 0) {
            drawCenteredMessage(g, w, h, "No data");
            return;
        }

        // Compute layout boxes:
        // [top labels]
        double topLabelH = estimateTopLabelHeight(g, c);
        // [left labels]
        double leftLabelW = estimateLeftLabelWidth(g, r);
        // [legend]
        double legendW = showLegend ? (legendWidth + legendGap) : 0.0;

        // Content box (matrix area)
        contentX = Math.ceil(leftLabelW + labelMargin);
        contentY = Math.ceil(topLabelH + labelMargin);
        contentW = Math.max(1, w - contentX - legendW - 6);
        contentH = Math.max(1, h - contentY - 6);

        // Value mapping range
        double vmin, vmax;
        if (autoRange) {
            double[] mm = minMax(matrix);
            vmin = mm[0];
            vmax = mm[1];
            if (!(vmax > vmin)) vmax = vmin + 1e-9;
        } else {
            vmin = fixedMin == null ? 0.0 : fixedMin.doubleValue();
            vmax = fixedMax == null ? 1.0 : fixedMax.doubleValue();
            if (!(vmax > vmin)) vmax = vmin + 1e-9;
        }

        // Draw matrix
        drawMatrix(g, vmin, vmax);

        // Axes
        drawAxisLabels(g);

        // Legend
        if (showLegend) {
            drawLegend(g, vmin, vmax);
        }
    }

    private void drawMatrix(GraphicsContext g, double vmin, double vmax) {
        int r = rows();
        int c = cols();
        if (r == 0 || c == 0) return;

        double cellW = contentW / c;
        double cellH = contentH / r;

        // Disable pixel smoothing for crisp cell edges
        g.setImageSmoothing(false);

        for (int i = 0; i < r; i++) {
            double y = contentY + i * cellH;
            for (int j = 0; j < c; j++) {
                double x = contentX + j * cellW;
                double v = matrix[i][j];

                Color col = switch (paletteKind) {
                    case SEQUENTIAL -> sequentialColor(norm01(v, vmin, vmax));
                    case DIVERGING -> divergingColor(v, vmin, vmax, divergingCenter != null ? divergingCenter.doubleValue() : 0.0);
                };

                g.setFill(col);
                g.fillRect(x, y, Math.ceil(cellW), Math.ceil(cellH));
            }
        }

        // Optional thin grid lines (subtle)
        g.setStroke(Color.color(0, 0, 0, 0.15));
        g.setLineWidth(1.0);
        // Vertical
        for (int j = 0; j <= c; j++) {
            double x = contentX + j * cellW + 0.5;
            g.strokeLine(x, contentY, x, contentY + contentH);
        }
        // Horizontal
        for (int i = 0; i <= r; i++) {
            double y = contentY + i * cellH + 0.5;
            g.strokeLine(contentX, y, contentX + contentW, y);
        }
    }

    private void drawAxisLabels(GraphicsContext g) {
        g.setFill(Color.LIGHTGRAY);
        g.setFont(labelFont);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);

        int r = rows();
        int c = cols();
        if (r == 0 || c == 0) return;

        double cellW = contentW / c;
        double cellH = contentH / r;

        // Top (columns)
        for (int j = 0; j < c; j++) {
            String base = (j < colLabels.length && colLabels[j] != null) ? colLabels[j] : ("c" + j);
            String lbl = compactColLabel(base, j);
            double x = contentX + (j + 0.5) * cellW;
            double y = contentY - labelMargin - textHeight(g);
            g.fillText(lbl, x, Math.max(0, y));
        }

        // Left (rows)
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.CENTER);
        for (int i = 0; i < r; i++) {
            String lbl = (i < rowLabels.length && rowLabels[i] != null) ? rowLabels[i] : ("r" + i);
            double x = contentX - labelMargin;
            double y = contentY + (i + 0.5) * cellH;
            g.fillText(lbl, Math.max(0, x), y);
        }
    }

    private void drawLegend(GraphicsContext g, double vmin, double vmax) {
        double x0 = contentX + contentW + legendGap;
        double y0 = contentY;
        double w = legendWidth;
        double h = contentH;

        // Vertical gradient
        int steps = 200;
        for (int k = 0; k < steps; k++) {
            double t = 1.0 - (k / (double) (steps - 1));   // top (max) → bottom (min)
            double v = vmin + t * (vmax - vmin);
            Color col = switch (paletteKind) {
                case SEQUENTIAL -> sequentialColor(norm01(v, vmin, vmax));
                case DIVERGING -> divergingColor(v, vmin, vmax, divergingCenter != null ? divergingCenter.doubleValue() : 0.0);
            };
            g.setFill(col);
            double yy = y0 + k * (h / steps);
            g.fillRect(x0, yy, w, (h / steps) + 1);
        }

        // Legend border
        g.setStroke(Color.gray(0.2));
        g.setLineWidth(1.0);
        g.strokeRect(x0 + 0.5, y0 + 0.5, w - 1, h - 1);

        // Ticks and labels (min, center (diverging), max)
        g.setFill(Color.LIGHTGRAY);
        g.setFont(labelFont);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);

        double tickX = x0 + w + 4;
        // max (top)
        g.fillText(df.format(vmax), tickX, y0);
        // min (bottom)
        g.fillText(df.format(vmin), tickX, y0 + h);

        // center (if diverging)
        if (paletteKind == PaletteKind.DIVERGING) {
            double t = norm01(divergingCenter != null ? divergingCenter.doubleValue() : 0.0, vmin, vmax);
            double y = y0 + (1.0 - t) * h;
            g.fillText(df.format(divergingCenter != null ? divergingCenter.doubleValue() : 0.0), tickX, y);
        }
    }

    private void drawCenteredMessage(GraphicsContext g, double w, double h, String msg) {
        g.setFill(Color.gray(0.7));
        g.setFont(Font.font("Arial", 16));
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(msg == null ? "" : msg, w * 0.5, h * 0.5);
    }

    // -------------------- Interaction --------------------

    private void onMouseMove(MouseEvent e) {
        int r = rows(), c = cols();
        if (r == 0 || c == 0) return;

        int[] rc = pickCell(e.getX(), e.getY());
        if (rc == null) {
            hoverTip.hide();
            return;
        }
        int i = rc[0], j = rc[1];
        double v = matrix[i][j];

        String rl = (i < rowLabels.length && rowLabels[i] != null) ? rowLabels[i] : ("r" + i);
        String cl = (j < colLabels.length && colLabels[j] != null) ? colLabels[j] : ("c" + j);

        hoverTip.setText(rl + ", " + cl + " = " + df.format(v));
        if (!hoverTip.isShowing()) {
            hoverTip.show(canvas, e.getScreenX() + 12, e.getScreenY() + 12);
        } else {
            hoverTip.setX(e.getScreenX() + 12);
            hoverTip.setY(e.getScreenY() + 12);
        }
    }

    private void onMouseClick(MouseEvent e) {
        if (onCellClick == null) return;
        int[] rc = pickCell(e.getX(), e.getY());
        if (rc == null) return;
        int i = rc[0], j = rc[1];
        onCellClick.accept(new MatrixClick(i, j, matrix[i][j]));
    }

    /** Return {row, col} or null if mouse is outside content box. */
    private int[] pickCell(double x, double y) {
        int r = rows(), c = cols();
        if (r == 0 || c == 0) return null;
        if (x < contentX || y < contentY || x > contentX + contentW || y > contentY + contentH) return null;

        double cellW = contentW / c;
        double cellH = contentH / r;

        int j = (int) Math.floor((x - contentX) / cellW);
        int i = (int) Math.floor((y - contentY) / cellH);

        if (i < 0 || i >= r || j < 0 || j >= c) return null;
        return new int[]{i, j};
    }

    // -------------------- Utilities --------------------

    private static String[] defaultLabels(int n, String prefix) {
        String p = (prefix == null || prefix.isBlank()) ? "a" : prefix.trim();
        String[] out = new String[n];
        for (int i = 0; i < n; i++) out[i] = p + i;
        return out;
    }

    private static double[] minMax(double[][] m) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < m.length; i++) {
            double[] row = m[i];
            for (int j = 0; j < row.length; j++) {
                double v = row[j];
                if (Double.isNaN(v) || Double.isInfinite(v)) continue;
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        if (min == Double.POSITIVE_INFINITY) { min = 0.0; max = 1.0; }
        if (max - min < 1e-12) max = min + 1e-12;
        return new double[]{min, max};
    }

    private static double norm01(double v, double min, double max) {
        double t = (v - min) / (max - min);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    // Sequential palette: simple gradient (dark blue → teal → yellow)
    private static Color sequentialColor(double t) {
        double r = clamp01(-0.5 + 2.2 * t);
        double g = clamp01(0.1 + 1.9 * t);
        double b = clamp01(1.0 - 0.9 * t);
        double gamma = 0.95;
        return Color.color(Math.pow(r, gamma), Math.pow(g, gamma), Math.pow(b, gamma));
    }

    // Diverging palette: cool (blue) below center, warm (red) above center; white at center
    private static Color divergingColor(double v, double min, double max, double center) {
        double t = (v - center) / (Math.max(Math.abs(max - center), Math.abs(center - min)) + 1e-12);
        if (t < -1) t = -1;
        if (t > 1) t = 1;
        if (t >= 0) {
            double a = t;
            double r = 1.0;
            double g = 1.0 - 0.6 * a;
            double b = 1.0 - 0.9 * a;
            return Color.color(r, g, b);
        } else {
            double a = -t;
            double r = 1.0 - 0.9 * a;
            double g = 1.0 - 0.8 * a;
            double b = 1.0;
            return Color.color(r, g, b);
        }
    }

    private static double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    private double estimateTopLabelHeight(GraphicsContext g, int cols) {
        if (cols == 0) return 0;
        g.setFont(labelFont);
        return Math.ceil(textHeight(g));
    }

    private double estimateLeftLabelWidth(GraphicsContext g, int rows) {
        if (rows == 0) return 0;
        g.setFont(labelFont);
        double maxW = 0;
        int sample = Math.min(rows, 40); // cap measurement work
        int step = Math.max(1, rows / sample);
        for (int i = 0; i < rows; i += step) {
            String lbl = (i < rowLabels.length && rowLabels[i] != null) ? rowLabels[i] : ("r" + i);
            double w = textWidth(g, lbl);
            if (w > maxW) maxW = w;
        }
        return Math.ceil(maxW + 2);
    }

    private double textWidth(GraphicsContext g, String s) {
        if (s == null || s.isEmpty()) return 0.0;
        measureText.setText(s);
        measureText.setFont(g.getFont());
        return measureText.getLayoutBounds().getWidth();
    }

    private double textHeight(GraphicsContext g) {
        measureText.setText("Mg"); // representative sample
        measureText.setFont(g.getFont());
        return measureText.getLayoutBounds().getHeight();
    }

    /** If compactColumnLabels=true and label starts with "Comp", strip that prefix. */
    private String compactColLabel(String original, int colIndex) {
        if (!compactColumnLabels) return original;
        if (original == null || original.isBlank()) return Integer.toString(colIndex);
        String s = original.trim();
        if (s.length() >= 4 && s.substring(0, 4).equalsIgnoreCase("comp")) {
            s = s.substring(4).trim(); // drop "comp" + any space
        }
        return s.isEmpty() ? Integer.toString(colIndex) : s;
    }
}
