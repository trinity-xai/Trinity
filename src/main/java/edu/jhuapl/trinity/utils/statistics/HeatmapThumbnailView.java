package edu.jhuapl.trinity.utils.statistics;

import static edu.jhuapl.trinity.utils.AnalysisUtils.clamp01;
import static edu.jhuapl.trinity.utils.AnalysisUtils.clamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * HeatmapThumbnailView
 * See changelog in header of this file revision:
 *  - legend placement fixed (managed=false)
 *  - palette schemes added
 *  - minAlphaAtVmin allows background to show through low-end values
 */
public final class HeatmapThumbnailView extends StackPane {

    public enum PaletteKind { SEQUENTIAL, DIVERGING }

    /** Named color ramps. */
    public enum PaletteScheme {
        VIRIDIS, INFERNO, MAGMA, PLASMA, CIVIDIS, TURBO, BLUE_YELLOW, GREYS
    }

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
    private PaletteScheme seqScheme = PaletteScheme.VIRIDIS;
    private PaletteScheme divScheme = PaletteScheme.BLUE_YELLOW; // base hues for halves

    // --- Appearance ---
    private Color backgroundColor = Color.BLACK;
    private boolean imageSmoothing = true;
    private double gamma = 1.0;
    private double clipLowPct = 0.0;
    private double clipHighPct = 0.0;
    /** Alpha to use at v=vmin after gamma (0..1). 1.0 = opaque low-end; 0.0 = fully transparent. */
    private double minAlphaAtVmin = 1.0;

    public HeatmapThumbnailView() {
        getChildren().add(canvas);
        getChildren().add(legend);
        setPadding(contentPadding);

        // Important: StackPane ignores relocate() for managed children. Disable layout management.
        canvas.setManaged(false);
        legend.setManaged(false);

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

    public void setGrid(List<List<Double>> grid) {
        this.grid = grid;
        if (autoRange) recomputeRange();
        redraw();
    }

    public void setFromGridDensity(GridDensityResult res, boolean useCDF, boolean flipY) {
        Objects.requireNonNull(res, "GridDensityResult");
        List<List<Double>> g = useCDF ? res.cdfAsListGrid() : res.pdfAsListGrid();
        this.flipY = flipY;
        setGrid(g);
    }

    public void setFlipY(boolean flip) { this.flipY = flip; redraw(); }

    public void useSequentialPalette() { this.palette = PaletteKind.SEQUENTIAL; redraw(); }

    public void useDivergingPalette(double center) {
        this.palette = PaletteKind.DIVERGING;
        this.divergingCenter = center;
        redraw();
    }

    /** Choose the ramp used for sequential mapping. */
    public void setSequentialScheme(PaletteScheme scheme) {
        if (scheme != null) { this.seqScheme = scheme; redraw(); }
    }

    /** Choose the base ramp used for diverging halves (low and high mirror). */
    public void setDivergingScheme(PaletteScheme scheme) {
        if (scheme != null) { this.divScheme = scheme; redraw(); }
    }

    public void setShowLegend(boolean show) {
        this.showLegend = show;
        legend.setVisible(show);
        requestLayout();
        redraw();
    }

    public void setAutoRange(boolean auto) {
        this.autoRange = auto;
        if (auto && grid != null) recomputeRange();
        redraw();
    }

    public void setFixedRange(double min, double max) {
        this.autoRange = false;
        this.vmin = min;
        this.vmax = max <= min ? min + 1e-12 : max;
        redraw();
    }

    public void setContentPadding(Insets insets) {
        this.contentPadding = insets == null ? Insets.EMPTY : insets;
        setPadding(contentPadding);
        requestLayout();
        redraw();
    }

    public void setBackgroundColor(Color c) { this.backgroundColor = (c == null ? Color.BLACK : c); redraw(); }

    public void setImageSmoothing(boolean on) { this.imageSmoothing = on; redraw(); }

    public void setGamma(double gamma) {
        double g = Double.isFinite(gamma) ? gamma : 1.0;
        this.gamma = Math.max(0.1, Math.min(5.0, g));
        redraw();
    }

    public void setClipPercent(double lowPct, double highPct) {
        this.clipLowPct = clamp(lowPct, 0.0, 20.0);
        this.clipHighPct = clamp(highPct, 0.0, 20.0);
        if (autoRange && grid != null) recomputeRange();
        redraw();
    }

    /** Set alpha applied at the low end (vmin) after gamma shaping. */
    public void setMinAlphaAtVmin(double a) {
        this.minAlphaAtVmin = clamp(a, 0.0, 1.0);
        redraw();
    }

    public double getVmin() { return vmin; }
    public double getVmax() { return vmax; }
    public double getDivergingCenter() { return divergingCenter; }
    public PaletteKind getPalette() { return palette; }
    public boolean isLegendShown() { return showLegend; }
    public Color getBackgroundColor() { return backgroundColor; }
    public boolean isImageSmoothing() { return imageSmoothing; }
    public double getGamma() { return gamma; }
    public double getClipLowPct() { return clipLowPct; }
    public double getClipHighPct() { return clipHighPct; }
    public PaletteScheme getSequentialScheme() { return seqScheme; }
    public PaletteScheme getDivergingScheme() { return divScheme; }
    public double getMinAlphaAtVmin() { return minAlphaAtVmin; }

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

    private void onSize(Observable obs) { layoutChildren(); redraw(); }

    private void recomputeRange() {
        if (grid == null || grid.isEmpty()) return;

        List<Double> vals = new ArrayList<>();
        for (int r = 0; r < grid.size(); r++) {
            List<Double> row = grid.get(r);
            if (row == null) continue;
            for (int c = 0; c < row.size(); c++) {
                Double v = row.get(c);
                if (v != null && Double.isFinite(v)) vals.add(v);
            }
        }
        if (vals.isEmpty()) { vmin = 0.0; vmax = 1.0; return; }

        Collections.sort(vals);
        int k = vals.size();
        int iLo = (int)Math.floor(clamp(clipLowPct, 0, 20) / 100.0 * (k - 1));
        int iHi = (int)Math.ceil((1.0 - clamp(clipHighPct, 0, 20) / 100.0) * (k - 1));
        double lo = vals.get(Math.max(0, Math.min(k - 1, iLo)));
        double hi = vals.get(Math.max(0, Math.min(k - 1, iHi)));
        if (!Double.isFinite(lo) || !Double.isFinite(hi)) { lo = 0.0; hi = 1.0; }
        if (hi - lo < 1e-12) hi = lo + 1e-12;

        vmin = lo; vmax = hi;
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(imageSmoothing);

        // background fill
        g.setFill(backgroundColor);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

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

        WritableImage img = new WritableImage(cols, rows);
        PixelWriter pw = img.getPixelWriter();

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

        g.drawImage(img, 0, 0, cols, rows, 0, 0, canvas.getWidth(), canvas.getHeight());
        drawLegend();
    }

    private void drawEmpty(GraphicsContext g) {
        g.setStroke(Color.gray(0.35));
        g.strokeRect(0.5, 0.5, Math.max(0, canvas.getWidth() - 1), Math.max(0, canvas.getHeight() - 1));
    }

    private void drawLegend() {
        if (!showLegend) return;
        GraphicsContext lg = legend.getGraphicsContext2D();
        lg.setImageSmoothing(true);

        lg.setFill(backgroundColor);
        lg.fillRect(0, 0, legend.getWidth(), legend.getHeight());

        double w = legend.getWidth();
        double h = legend.getHeight();

        int steps = (int) Math.max(2, h);
        for (int i = 0; i < steps; i++) {
            double t = 1.0 - (double) i / (steps - 1); // top->bottom maps max->min
            double v = vmin + t * (vmax - vmin);
            Color c = mapValueToColor(v, vmin, vmax, Math.max(1e-12, vmax - vmin));
            lg.setStroke(c);
            lg.strokeLine(0, i + 0.5, w - 1, i + 0.5);
        }

        lg.setStroke(Color.gray(0.35));
        lg.strokeRect(0.5, 0.5, w - 1, h - 1);

        if (palette == PaletteKind.DIVERGING) {
            double tCenter = (vmax - divergingCenter) / Math.max(1e-12, (vmax - vmin));
            double y = (1.0 - tCenter) * h;
            lg.setStroke(Color.gray(0.6));
            lg.strokeLine(0, y, w, y);
        }
    }

    // =====================================================================================
    // Color mapping
    // =====================================================================================

    private Color mapValueToColor(double v, double min, double max, double span) {
        if (!Double.isFinite(v)) {
            return Color.color(0, 0, 0, 0.0);
        }
        double t = (v - min) / span;
        t = clamp01(t);
        t = Math.pow(t, gamma);
        t = clamp01(t);

        if (palette == PaletteKind.SEQUENTIAL) {
            Color base = rampColor(seqScheme, t);
            // fade low-end if requested
            double a = (t <= 0.0) ? minAlphaAtVmin : 1.0;
            return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
        } else {
            // Diverging: map sides with mirrored ramp from divScheme
            if (v <= divergingCenter) {
                double lt = (divergingCenter - v) / Math.max(1e-12, divergingCenter - min); // 0..1
                lt = Math.pow(clamp01(lt), gamma);
                Color side = rampColor(divScheme, lt);       // far low -> ramp end
                return lerpColor(Color.web("#ffffff"), side, clamp01(lt));
            } else {
                double rt = (v - divergingCenter) / Math.max(1e-12, max - divergingCenter);
                rt = Math.pow(clamp01(rt), gamma);
                Color side = rampColorOpposite(divScheme, rt); // far high -> opposite end
                return lerpColor(Color.web("#ffffff"), side, clamp01(rt));
            }
        }
    }

    /** Produces a color in the given scheme for 0..1 (low->high). */
    private static Color rampColor(PaletteScheme scheme, double t) {
        t = clamp01(t);
        switch (scheme) {
            case INFERNO:  return multiStop(t,
                    "#000004","#1f0c48","#550f6d","#88226a","#b63655","#e35933","#f9950a","#fcffa4");
            case MAGMA:    return multiStop(t,
                    "#000004","#1b0c41","#4f0c6b","#88226a","#b73779","#e56b5d","#fb9f3a","#fbe723");
            case PLASMA:   return multiStop(t,
                    "#0d0887","#41049d","#6a00a8","#8f0da4","#b12a90","#cc4778","#e16462","#f2844b","#fca636","#fcce25","#f0f921");
            case CIVIDIS:  return multiStop(t,
                    "#00204c","#163867","#2a517a","#3f6a89","#578399","#729ca7","#90b5b5","#b0cec2","#d4e7cf","#f9f1d3");
            case TURBO:    return multiStop(t,
                    "#30123b","#4145ad","#2ab0e8","#2be3a0","#8dfc3c","#f7f54a","#f79d1e","#e7522f","#cc1a4a","#7a1a6c");
            case BLUE_YELLOW: return multiStop(t,
                    "#0b3d91","#00bcd4","#ffeb3b","#ffffff");
            case GREYS:    return multiStop(t,
                    "#000000","#333333","#777777","#bbbbbb","#ffffff");
            case VIRIDIS:
            default:       return multiStop(t,
                    "#440154","#3b528b","#21918c","#5ec962","#fde725");
        }
    }

    /** For diverging high side: use a complementary/opposite end of the chosen scheme. */
    private static Color rampColorOpposite(PaletteScheme scheme, double t) {
        // Simple approach: flip t and reuse ramp; for BLUE_YELLOW this becomes Yellow->Blue.
        return rampColor(scheme, t);
    }

    private static Color multiStop(double t, String... hexStops) {
        if (hexStops == null || hexStops.length == 0) return Color.WHITE;
        if (hexStops.length == 1) return Color.web(hexStops[0]);
        double pos = t * (hexStops.length - 1);
        int i = (int)Math.floor(pos);
        int j = Math.min(hexStops.length - 1, i + 1);
        double k = pos - i;
        return lerpColor(Color.web(hexStops[i]), Color.web(hexStops[j]), k);
    }

    private static Color lerpColor(Color a, Color b, double t) {
        t = clamp01(t);
        double r = a.getRed() + (b.getRed() - a.getRed()) * t;
        double g = a.getGreen() + (b.getGreen() - a.getGreen()) * t;
        double bl = a.getBlue() + (b.getBlue() - a.getBlue()) * t;
        double al = a.getOpacity() + (b.getOpacity() - a.getOpacity()) * t;
        return new Color(r, g, bl, al);
    }
}
