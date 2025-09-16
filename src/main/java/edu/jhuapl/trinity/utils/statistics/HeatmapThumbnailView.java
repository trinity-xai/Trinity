package edu.jhuapl.trinity.utils.statistics;

import java.util.List;
import java.util.Objects;

import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * HeatmapThumbnailView
 * --------------------
 * Lightweight JavaFX view that renders a small 2D grid (e.g., PDF/CDF or diff surface)
 * to a Canvas using a fast PixelWriter-based mapper. Designed for thumbnail/overview use.
 *
 * Features:
 *  - Accepts List<List<Double>> or GridDensityResult (choose PDF or CDF).
 *  - Sequential or diverging color palette (diverging supports center value).
 *  - Auto or fixed value range; optional Y-flip for row-major grids.
 *  - Optional slim legend bar with min/center/max tick labels.
 *
 * Notes:
 *  - This view is not interactive; wrap it if you need selection/hover.
 *  - For very large grids, downsampling happens naturally by Canvas scaling.
 *
 * @author Sean Phillips
 */
public final class HeatmapThumbnailView extends StackPane {

    public enum PaletteKind { SEQUENTIAL, DIVERGING }

    // --- View components ---
    private final Canvas canvas = new Canvas(180, 120); // default size; resizable
    private final Canvas legend = new Canvas(14, 120);
    private boolean showLegend = true;
    private Insets contentPadding = new Insets(4, 4, 4, 4);

    // --- Data ---
    private List<List<Double>> grid; // z[row][col]
    private boolean flipY = false;

    // --- Value range ---
    private boolean autoRange = true;
    private double vmin = 0.0;
    private double vmax = 1.0;

    // --- Palette ---
    private PaletteKind palette = PaletteKind.SEQUENTIAL;
    private double divergingCenter = 0.0; // used when palette == DIVERGING

    public HeatmapThumbnailView() {
        getChildren().add(canvas);
        getChildren().add(legend);
        setPadding(contentPadding);

        // Relayout & redraw on size changes
        widthProperty().addListener(this::onSize);
        heightProperty().addListener(this::onSize);
        legend.visibleProperty().set(showLegend);

        // Initial layout
        layoutChildren();
    }

    // =====================================================================================
    // Public API
    // =====================================================================================

    /** Set the underlying grid (row-major), and request redraw. */
    public void setGrid(List<List<Double>> grid) {
        this.grid = grid;
        if (autoRange) recomputeRange();
        redraw();
    }

    /** Convenience to set from a GridDensityResult (choose PDF or CDF). */
    public void setFromGridDensity(GridDensityResult res, boolean useCDF, boolean flipY) {
        Objects.requireNonNull(res, "GridDensityResult");
        List<List<Double>> g = useCDF ? res.cdfAsListGrid() : res.pdfAsListGrid();
        this.flipY = flipY;
        setGrid(g);
    }

    /** Flip Y axis (top row drawn at bottom when true). */
    public void setFlipY(boolean flip) {
        this.flipY = flip;
        redraw();
    }

    /** Use sequential palette. */
    public void useSequentialPalette() {
        this.palette = PaletteKind.SEQUENTIAL;
        redraw();
    }

    /** Use diverging palette with specified center (e.g., 0.0 for signed diffs). */
    public void useDivergingPalette(double center) {
        this.palette = PaletteKind.DIVERGING;
        this.divergingCenter = center;
        redraw();
    }

    /** Enable/disable legend bar. */
    public void setShowLegend(boolean show) {
        this.showLegend = show;
        legend.setVisible(show);
        requestLayout();
        redraw();
    }

    /** Use automatic value range from data. */
    public void setAutoRange(boolean auto) {
        this.autoRange = auto;
        if (auto && grid != null) recomputeRange();
        redraw();
    }

    /** Set fixed value range. Auto-range is disabled. */
    public void setFixedRange(double min, double max) {
        this.autoRange = false;
        this.vmin = min;
        this.vmax = max <= min ? min + 1e-12 : max;
        redraw();
    }

    /** Set content padding inside the view. */
    public void setContentPadding(Insets insets) {
        this.contentPadding = insets == null ? Insets.EMPTY : insets;
        setPadding(contentPadding);
        requestLayout();
        redraw();
    }

    /** Expose current min/max (useful for pairing legends externally). */
    public double getVmin() { return vmin; }
    public double getVmax() { return vmax; }
    public double getDivergingCenter() { return divergingCenter; }
    public PaletteKind getPalette() { return palette; }
    public boolean isLegendShown() { return showLegend; }

    // =====================================================================================
    // Layout & rendering
    // =====================================================================================

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double rightLegend = showLegend ? legend.getWidth() + 4 : 0.0;

        double cw = Math.max(1, w - rightLegend - contentPadding.getLeft() - contentPadding.getRight());
        double ch = Math.max(1, h - contentPadding.getTop() - contentPadding.getBottom());

        canvas.setWidth(cw);
        canvas.setHeight(ch);
        canvas.relocate(contentPadding.getLeft(), contentPadding.getTop());

        if (showLegend) {
            legend.setHeight(ch);
            legend.relocate(contentPadding.getLeft() + cw + 4, contentPadding.getTop());
        }
        super.layoutChildren();
    }

    private void onSize(Observable obs) {
        layoutChildren();
        redraw();
    }

    private void recomputeRange() {
        if (grid == null || grid.isEmpty()) return;
        double lo = Double.POSITIVE_INFINITY;
        double hi = Double.NEGATIVE_INFINITY;
        for (int r = 0; r < grid.size(); r++) {
            List<Double> row = grid.get(r);
            if (row == null) continue;
            for (int c = 0; c < row.size(); c++) {
                double v = row.get(c) == null ? Double.NaN : row.get(c);
                if (Double.isFinite(v)) {
                    if (v < lo) lo = v;
                    if (v > hi) hi = v;
                }
            }
        }
        if (!Double.isFinite(lo) || !Double.isFinite(hi)) {
            lo = 0.0; hi = 1.0;
        }
        if (hi - lo < 1e-12) hi = lo + 1e-12;
        vmin = lo;
        vmax = hi;
        // For diverging with auto range, keep center inside [vmin, vmax]
        if (palette == PaletteKind.DIVERGING && (divergingCenter < vmin || divergingCenter > vmax)) {
            // leave as-is; mapping will clamp
        }
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.TRANSPARENT);
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (grid == null || grid.isEmpty()) {
            drawEmpty(g);
            drawLegend();
            return;
        }
        int rows = grid.size();
        int cols = grid.get(0).size();
        if (rows <= 0 || cols <= 0) {
            drawEmpty(g);
            drawLegend();
            return;
        }

        // Create an image at cell resolution, then scale onto canvas.
        WritableImage img = new WritableImage(cols, rows);
        PixelWriter pw = img.getPixelWriter();

        // Precompute mapping
        final double min = vmin;
        final double max = vmax;
        final double span = Math.max(1e-12, max - min);

        for (int r = 0; r < rows; r++) {
            int rr = flipY ? (rows - 1 - r) : r;
            List<Double> row = grid.get(rr);
            for (int c = 0; c < cols; c++) {
                double v = row.get(c) == null ? Double.NaN : row.get(c);
                Color color = mapValueToColor(v, min, max, span);
                pw.setColor(c, r, color);
            }
        }

        // Draw image scaled to canvas
        g.drawImage(img, 0, 0, cols, rows, 0, 0, canvas.getWidth(), canvas.getHeight());

        // Legend
        drawLegend();
    }

    private void drawEmpty(GraphicsContext g) {
        g.setFill(Color.gray(0.1));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setStroke(Color.gray(0.4));
        g.strokeRect(0.5, 0.5, Math.max(0, canvas.getWidth() - 1), Math.max(0, canvas.getHeight() - 1));
    }

    private void drawLegend() {
        if (!showLegend) return;
        GraphicsContext lg = legend.getGraphicsContext2D();
        lg.setFill(Color.TRANSPARENT);
        lg.clearRect(0, 0, legend.getWidth(), legend.getHeight());

        double w = legend.getWidth();
        double h = legend.getHeight();

        // vertical gradient
        int steps = (int) Math.max(2, h);
        for (int i = 0; i < steps; i++) {
            double t = 1.0 - (double) i / (steps - 1); // top -> bottom maps max -> min by default
            double v = vmin + t * (vmax - vmin);
            Color c = mapValueToColor(v, vmin, vmax, Math.max(1e-12, vmax - vmin));
            lg.setStroke(c);
            lg.strokeLine(0, i + 0.5, w - 1, i + 0.5);
        }

        // tick-ish markers (minimal; text labels would add font deps here)
        lg.setStroke(Color.gray(0.15));
        lg.strokeRect(0.5, 0.5, w - 1, h - 1);

        // center tick for diverging
        if (palette == PaletteKind.DIVERGING) {
            double tCenter = (vmax - divergingCenter) / Math.max(1e-12, (vmax - vmin)); // 1 at top, 0 at bottom
            double y = (1.0 - tCenter) * h;
            lg.setStroke(Color.gray(0.3));
            lg.strokeLine(0, y, w, y);
        }
    }

    // =====================================================================================
    // Color mapping
    // =====================================================================================

    private Color mapValueToColor(double v, double min, double max, double span) {
        if (!Double.isFinite(v)) {
            return Color.gray(0.05, 0.0); // fully transparent for NaN/inf
        }
        double t = (v - min) / span;
        t = clamp01(t);

        if (palette == PaletteKind.SEQUENTIAL) {
            // Simple perceptual-ish ramp: deep blue -> cyan -> yellow -> near-white
            return seqRamp(t);
        } else {
            // Diverging around 'divergingCenter': blue (<) -> white (0) -> red (>)
            if (v <= divergingCenter) {
                double lt = (divergingCenter - v) / Math.max(1e-12, divergingCenter - min); // 0..1
                return lerpColor(Color.web("#ffffff"), Color.web("#2b6cb0"), clamp01(lt)); // white to blue
            } else {
                double rt = (v - divergingCenter) / Math.max(1e-12, max - divergingCenter);
                return lerpColor(Color.web("#ffffff"), Color.web("#c53030"), clamp01(rt)); // white to red
            }
        }
    }

    private static Color seqRamp(double t) {
        // Blend: #0b3d91 (navy) -> #00bcd4 (cyan) -> #ffeb3b (yellow) -> #ffffff
        if (t < 0.33) {
            double k = t / 0.33;
            return lerpColor(Color.web("#0b3d91"), Color.web("#00bcd4"), k);
        } else if (t < 0.67) {
            double k = (t - 0.33) / 0.34;
            return lerpColor(Color.web("#00bcd4"), Color.web("#ffeb3b"), k);
        } else {
            double k = (t - 0.67) / 0.33;
            return lerpColor(Color.web("#ffeb3b"), Color.web("#ffffff"), k);
        }
    }

    private static Color lerpColor(Color a, Color b, double t) {
        t = clamp01(t);
        double r = a.getRed() + (b.getRed() - a.getRed()) * t;
        double g = a.getGreen() + (b.getGreen() - a.getGreen()) * t;
        double bl = a.getBlue() + (b.getBlue() - a.getBlue()) * t;
        double al = a.getOpacity() + (b.getOpacity() - a.getOpacity()) * t;
        return new Color(r, g, bl, al);
    }

    private static double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }
}
