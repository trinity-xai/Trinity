package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.utils.MatrixViewUtil;
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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * MatrixHeatmapView
 * -----------------
 * Reusable JavaFX control to render a 2D numeric matrix as a heatmap.
 * - Renders on Canvas (fast, lightweight).
 * - Sequential and diverging palettes.
 * - Auto or fixed range mapping.
 * - Optional color legend (with optional title).
 * - Axis labels (top / left).
 * - Hover tooltips with (row, col, value).
 * - Click callback with cell picking.
 * <p>
 * Public API is intentionally generic so it can be reused by multiple Trinity tools.
 *
 * @author Sean Phillips
 */
public final class MatrixHeatmapView extends BorderPane {

    // -------------------- Types --------------------

    public enum ValueMode {RAW, ABS_VALUE}

    public enum ScaleMode {AUTO, FIXED}

    /**
     * Color palette styles.
     */
    public enum PaletteKind {
        SEQUENTIAL,   // low -> high mapped 0..1
        DIVERGING     // < center on cool, > center on warm (center maps to mid color)
    }

    /**
     * Simple click payload.
     */
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
    private String legendTitle = null; // short, optional; rendered above legend inside canvas

    // Palette & range
    private PaletteKind paletteKind = PaletteKind.SEQUENTIAL;
    private boolean autoRange = true;
    private Double fixedMin = null;
    private Double fixedMax = null;
    private Double divergingCenter = 0.0; // for DIVERGING only

    // Interaction
    private Consumer<MatrixClick> onCellClick = null;
    private final Tooltip hoverTip = new Tooltip();
    private final DecimalFormat df = new DecimalFormat("0.####");

    // Cached draw metrics
    private double contentX;
    private double contentY;
    private double contentW;
    private double contentH;

    // Label behavior
    private boolean compactColumnLabels = true; // default: strip "Comp " → just the index for columns

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

    /**
     * Set a new matrix. Null or empty → clears view.
     */
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

    /**
     * Convenience: accept List<List<Double>>.
     */
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

    public List<String> getRowLabels() {
        return Arrays.asList(rowLabels);
    }

    /**
     * Optional row labels (length must equal rows).
     */
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

    /**
     * Optional column labels (length must equal cols). Applies compacting if enabled.
     */
    public void setColLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            this.colLabels = new String[cols()];
        } else {
            if (labels.size() != cols()) {
                throw new IllegalArgumentException("Column labels length must match matrix cols.");
            }
            String[] incoming = labels.toArray(new String[0]);
            if (compactColumnLabels) {
                String[] compacted = new String[incoming.length];
                for (int i = 0; i < incoming.length; i++) {
                    compacted[i] = MatrixViewUtil.compactCompLabel(incoming[i]);
                }
                this.colLabels = compacted;
            } else {
                this.colLabels = incoming;
            }
        }
        layoutAndDraw();
    }

    /**
     * Control whether columns compact "Comp 7" → "7". Default: true.
     */
    public void setCompactColumnLabels(boolean compact) {
        this.compactColumnLabels = compact;
        // Re-apply compaction on currently loaded labels
        if (this.colLabels != null && this.colLabels.length > 0) {
            String[] out = new String[this.colLabels.length];
            for (int i = 0; i < this.colLabels.length; i++) {
                out[i] = compact ? MatrixViewUtil.compactCompLabel(this.colLabels[i]) : this.colLabels[i];
            }
            this.colLabels = out;
            layoutAndDraw();
        }
    }

    /**
     * Set a uniform label prefix if none provided yet (e.g., "F0..F(n-1)").
     */
    public void setDefaultAxisLabels(String rowPrefix, String colPrefix) {
        if (rows() > 0 && (rowLabels == null || rowLabels.length != rows())) {
            rowLabels = defaultLabels(rows(), rowPrefix == null ? "r" : rowPrefix);
        }
        if (cols() > 0 && (colLabels == null || colLabels.length != cols())) {
            colLabels = defaultLabels(cols(), colPrefix == null ? "c" : colPrefix);
        }
        layoutAndDraw();
    }

    /**
     * Optional legend title (short); rendered above the legend inside the canvas.
     */
    public void setLegendTitle(String title) {
        this.legendTitle = (title == null || title.isBlank()) ? null : title.trim();
        layoutAndDraw();
    }

    /**
     * Sequential palette.
     */
    public void useSequentialPalette() {
        this.paletteKind = PaletteKind.SEQUENTIAL;
        layoutAndDraw();
    }

    /**
     * Diverging palette (values below/above center split colors).
     */
    public void useDivergingPalette(double center) {
        this.paletteKind = PaletteKind.DIVERGING;
        this.divergingCenter = center;
        layoutAndDraw();
    }

    /**
     * Auto-range on (min/max derived from current matrix).
     */
    public void setAutoRange(boolean on) {
        this.autoRange = on;
        layoutAndDraw();
    }

    /**
     * Fixed range mapping.
     */
    public void setFixedRange(double vmin, double vmax) {
        if (!(vmax > vmin)) {
            throw new IllegalArgumentException("vmax must be greater than vmin.");
        }
        this.autoRange = false;
        this.fixedMin = vmin;
        this.fixedMax = vmax;
        layoutAndDraw();
    }

    /**
     * Show or hide the color legend bar.
     */
    public void setShowLegend(boolean show) {
        this.showLegend = show;
        layoutAndDraw();
    }

    /**
     * Set axis label font.
     */
    public void setLabelFont(Font f) {
        if (f != null) {
            this.labelFont = f;
            layoutAndDraw();
        }
    }

    /**
     * Set click handler for cell picks.
     */
    public void setOnCellClick(Consumer<MatrixClick> handler) {
        this.onCellClick = handler;
    }

    /**
     * Return a defensive copy of the matrix (for external reads).
     */
    public double[][] getMatrixCopy() {
        int r = rows(), c = cols();
        double[][] out = new double[r][c];
        for (int i = 0; i < r; i++) {
            System.arraycopy(matrix[i], 0, out[i], 0, c);
        }
        return out;
    }

    // -------------------- Internals: sizing + render --------------------

    private int rows() {
        return matrix.length;
    }

    private int cols() {
        return (rows() == 0) ? 0 : matrix[0].length;
    }

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
        double topLabelH = estimateTopLabelHeight(g, c);
        double leftLabelW = estimateLeftLabelWidth(g, r);
        double legendW = showLegend ? (legendWidth + legendGap) : 0.0;

        // Content box (matrix area)
        contentX = Math.ceil(leftLabelW + labelMargin);
        contentY = Math.ceil(topLabelH + labelMargin);
        contentW = Math.max(1, w - contentX - legendW - 6);
        contentH = Math.max(1, h - contentY - 6);

        // Value mapping range
        double vmin, vmax;
        if (autoRange) {
            double[] mm = MatrixViewUtil.minMax(matrix);
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

        g.setImageSmoothing(false);

        for (int i = 0; i < r; i++) {
            double y = contentY + i * cellH;
            for (int j = 0; j < c; j++) {
                double x = contentX + j * cellW;
                double v = MatrixViewUtil.sanitize(matrix[i][j]);

                Color col = switch (paletteKind) {
                    case SEQUENTIAL -> MatrixViewUtil.sequentialColor(MatrixViewUtil.norm01(v, vmin, vmax));
                    case DIVERGING -> MatrixViewUtil.divergingColor(v, vmin, vmax,
                        divergingCenter != null ? divergingCenter.doubleValue() : 0.0);
                };

                g.setFill(col);
                g.fillRect(x, y, Math.ceil(cellW), Math.ceil(cellH));
            }
        }

        // Subtle grid lines
        g.setStroke(Color.color(0, 0, 0, 0.15));
        g.setLineWidth(1.0);
        for (int j = 0; j <= c; j++) {
            double x = contentX + j * cellW + 0.5;
            g.strokeLine(x, contentY, x, contentY + contentH);
        }
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
            String lbl = (j < colLabels.length && colLabels[j] != null) ? colLabels[j] : ("c" + j);
            double x = contentX + (j + 0.5) * cellW;
            double y = contentY - labelMargin - textHeight(g, lbl);
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

        // Optional title above legend
        if (legendTitle != null) {
            g.save();
            g.setFill(Color.LIGHTGRAY);
            g.setFont(labelFont);
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.TOP);
            double th = textHeight(g, legendTitle);
            double ty = Math.max(0, y0 - Math.min(th + 2, 18));
            g.fillText(legendTitle, x0 + w * 0.5, ty);
            g.restore();
        }

        // Vertical gradient ramp
        int steps = 200;
        for (int k = 0; k < steps; k++) {
            double t = 1.0 - (k / (double) (steps - 1));   // top (max) → bottom (min)
            double v = vmin + t * (vmax - vmin);
            Color col = switch (paletteKind) {
                case SEQUENTIAL -> MatrixViewUtil.sequentialColor(MatrixViewUtil.norm01(v, vmin, vmax));
                case DIVERGING -> MatrixViewUtil.divergingColor(v, vmin, vmax,
                    divergingCenter != null ? divergingCenter.doubleValue() : 0.0);
            };
            g.setFill(col);
            double yy = y0 + k * (h / steps);
            g.fillRect(x0, yy, w, (h / steps) + 1);
        }

        // Legend border
        g.setStroke(Color.gray(0.2));
        g.setLineWidth(1.0);
        g.strokeRect(x0 + 0.5, y0 + 0.5, w - 1, h - 1);

        // Ticks & labels inside the bar
        double pad = 2.0;
        g.setFill(Color.LIGHTGRAY);
        g.setFont(labelFont);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);

        g.fillText(df.format(vmax), x0 + pad, y0 + pad + textHeight(g, "X") * 0.5);
        g.fillText(df.format(vmin), x0 + pad, y0 + h - pad - textHeight(g, "X") * 0.5);

        if (paletteKind == PaletteKind.DIVERGING) {
            double t = MatrixViewUtil.norm01(
                divergingCenter != null ? divergingCenter.doubleValue() : 0.0, vmin, vmax);
            double y = y0 + (1.0 - t) * h;
            g.fillText(df.format(
                divergingCenter != null ? divergingCenter.doubleValue() : 0.0), x0 + pad, y);
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
        double v = MatrixViewUtil.sanitize(matrix[i][j]);

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
        onCellClick.accept(new MatrixClick(i, j, MatrixViewUtil.sanitize(matrix[i][j])));
    }

    /**
     * Return {row, col} or null if mouse is outside content box.
     */
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

    private double estimateTopLabelHeight(GraphicsContext g, int cols) {
        if (cols == 0) return 0;
        g.setFont(labelFont);
        return Math.ceil(textHeight(g, "X") + 2);
    }

    private double estimateLeftLabelWidth(GraphicsContext g, int rows) {
        if (rows == 0) return 0;
        g.setFont(labelFont);
        double maxW = 0;
        int sample = Math.min(rows, 40);
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

    private double textHeight(GraphicsContext g, String s) {
        measureText.setText((s == null || s.isEmpty()) ? "Mg" : s);
        measureText.setFont(g.getFont());
        return measureText.getLayoutBounds().getHeight();
    }
}
