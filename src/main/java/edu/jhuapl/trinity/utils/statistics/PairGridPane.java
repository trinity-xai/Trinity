package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.utils.ResourceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * PairGridPane (TilePane-backed)
 * ------------------------------
 * Responsive grid of heatmap thumbnails that wraps on resize without
 * forcing the parent to expand.
 *
 * Header slot via {@link #setHeader(Node)} to host controls like Search/Sort.
 *
 * New (appearance & UX):
 *  - Grid-wide "display state" (palette, legend, background, smoothing, gamma, clip%, range mode)
 *  - Setters to update existing tiles immediately and apply to future tiles
 *  - Global range compute across visible items
 *  - Per-tile context menu (autoscale, pin fixed range from tile, compute global, export PNG)
 *  - Hover preview: temporarily autoscale a tile while hovered if grid is in Global/Fixed
 */
public final class PairGridPane extends BorderPane {
    // --- Streaming support (thread-safe buffer -> periodic FX flush) ---
    private final Object streamLock = new Object();
    private final List<PairItem> pendingBuffer = new ArrayList<>();
    private long lastFlushNanos = System.nanoTime();
    private boolean flushScheduled = false;

    // ---------- Public API types ----------

    public static final class PairItem {
        public final String xLabel;
        public final String yLabel;

        /** Optional component indices for sort-by-i/j convenience; may be null. */
        public final Integer iIndex;
        public final Integer jIndex;

        public final List<List<Double>> grid;
        public final GridDensityResult res;
        public final boolean useCDF;
        public final boolean flipY;
        public final HeatmapThumbnailView.PaletteKind palette;
        public final Double center;
        public final boolean autoRange;
        public final Double vmin;
        public final Double vmax;
        public final boolean showLegend;
        public final Double score;
        public final Object userData;

        private PairItem(Builder b) {
            this.xLabel = b.xLabel;
            this.yLabel = b.yLabel;
            this.iIndex = b.iIndex;
            this.jIndex = b.jIndex;
            this.grid = b.grid;
            this.res = b.res;
            this.useCDF = b.useCDF;
            this.flipY = b.flipY;
            this.palette = b.palette;
            this.center = b.center;
            this.autoRange = b.autoRange;
            this.vmin = b.vmin;
            this.vmax = b.vmax;
            this.showLegend = b.showLegend;
            this.score = b.score;
            this.userData = b.userData;
        }

        public static Builder newBuilder(String xLabel, String yLabel) {
            return new Builder(xLabel, yLabel);
        }

        public static final class Builder {
            private final String xLabel;
            private final String yLabel;

            private Integer iIndex = null;
            private Integer jIndex = null;

            private List<List<Double>> grid;
            private GridDensityResult res;
            private boolean useCDF = false;
            private boolean flipY = true;

            private HeatmapThumbnailView.PaletteKind palette = HeatmapThumbnailView.PaletteKind.SEQUENTIAL;
            private Double center = 0.0;
            private boolean autoRange = true;
            private Double vmin = 0.0;
            private Double vmax = 1.0;
            private boolean showLegend = false;

            private Double score = null;
            private Object userData = null;

            private Builder(String xLabel, String yLabel) {
                this.xLabel = Objects.requireNonNull(xLabel, "xLabel");
                this.yLabel = Objects.requireNonNull(yLabel, "yLabel");
            }

            public Builder grid(List<List<Double>> g) { this.grid = g; this.res = null; return this; }
            public Builder from(GridDensityResult r, boolean useCDF, boolean flipY) {
                this.res = r; this.grid = null; this.useCDF = useCDF; this.flipY = flipY; return this;
            }
            public Builder palette(HeatmapThumbnailView.PaletteKind p) { this.palette = p; return this; }
            public Builder divergingCenter(double c) { this.center = c; return this; }
            public Builder autoRange(boolean on) { this.autoRange = on; return this; }
            public Builder fixedRange(double min, double max) { this.autoRange = false; this.vmin = min; this.vmax = max; return this; }
            public Builder flipY(boolean flip) { this.flipY = flip; return this; }
            public Builder showLegend(boolean show) { this.showLegend = show; return this; }
            public Builder score(Double s) { this.score = s; return this; }
            public Builder userData(Object d) { this.userData = d; return this; }
            /** Optional: attach component indices for downstream sort-by-i/j. */
            public Builder indices(Integer i, Integer j) { this.iIndex = i; this.jIndex = j; return this; }

            public PairItem build() {
                if (grid == null && res == null)
                    throw new IllegalArgumentException("Provide grid OR GridDensityResult.");
                return new PairItem(this);
            }
        }
    }

    public static final class CellClick {
        public final int index;
        public final PairItem item;
        public final String xLabel;
        public final String yLabel;
        public CellClick(int index, PairItem item) {
            this.index = index; this.item = item;
            this.xLabel = item.xLabel; this.yLabel = item.yLabel;
        }
    }

    /** Range struct for global/fixed usage. */
    public static final class Range {
        public final double vmin, vmax;
        public Range(double vmin, double vmax) { this.vmin = vmin; this.vmax = vmax; }
    }

    public enum RangeMode { AUTO, GLOBAL, FIXED }

    /** Captures grid-wide display preferences to apply to tiles. */
    private static final class DisplayState {
        HeatmapThumbnailView.PaletteKind palette = HeatmapThumbnailView.PaletteKind.SEQUENTIAL;
        boolean legend = false;
        boolean smoothing = true;
        double gamma = 1.0;
        double clipLow = 0.0;
        double clipHigh = 0.0;
        Color background = Color.BLACK;
        RangeMode rangeMode = RangeMode.AUTO;
        double fixedMin = Double.NaN;
        double fixedMax = Double.NaN;
    }

    // ---------- Fields & configuration ----------

    private final TilePane tilePane = new TilePane();
    private final ScrollPane scroller = new ScrollPane(tilePane);
    private final StackPane contentStack = new StackPane();

    /** Optional header area shown above the grid (e.g., Search/Sort). */
    private Node headerNode = null;

    private VBox placeholder;
    private final Label placeholderLabel = new Label("No results loaded");

    private final List<PairItem> allItems = new ArrayList<>();
    private List<PairItem> visibleItems = new ArrayList<>();

    private Predicate<PairItem> filter = null;
    private Comparator<PairItem> sorter = null;

    private int columns = 4;                 // hint only; wrapping adapts to viewport width
    private double cellWidth = 180;
    private double cellHeight = 140;
    private double cellHGap = 8;
    private double cellVGap = 8;
    private Insets cellPadding = new Insets(6);

    private boolean showScoresInHeader = true;
    private Consumer<CellClick> onCellClick = null;

    //display state applied to tiles
    private final DisplayState display = new DisplayState();
    private Range globalRange = null;

    // Optional export hook
    public static final class ExportRequest {
        public final PairItem item;
        public final WritableImage image;
        public ExportRequest(PairItem item, WritableImage image) { this.item = item; this.image = image; }
    }
    private Consumer<ExportRequest> onExportRequest;

    public void setOnExportRequest(Consumer<ExportRequest> handler) {
        this.onExportRequest = handler;
    }

    // ---------- Construction ----------

    public PairGridPane() {
        // ScrollPane: cap viewport so it never pushes parent
        scroller.setFitToWidth(true);
        scroller.setFitToHeight(false); // content height won’t try to expand the parent
        scroller.setPannable(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setMinHeight(0);
        scroller.setPrefHeight(Region.USE_COMPUTED_SIZE);
        scroller.setMaxHeight(Region.USE_COMPUTED_SIZE);
        // modest defaults; parent can override via setPreferredViewport(...)
        scroller.setPrefViewportWidth(720);
        scroller.setPrefViewportHeight(420);

        // TilePane: width bound to the viewport; height doesn’t drive parent
        tilePane.setPadding(new Insets(6));
        tilePane.setHgap(cellHGap);
        tilePane.setVgap(cellVGap);
        tilePane.setTileAlignment(Pos.TOP_LEFT);
        tilePane.setPrefColumns(columns);
        tilePane.setMinWidth(0);
        tilePane.setPrefWidth(0);
        tilePane.setMaxWidth(Region.USE_PREF_SIZE);
        tilePane.setMinHeight(0);
        tilePane.setPrefHeight(Region.USE_COMPUTED_SIZE);
        tilePane.setMaxHeight(Region.USE_PREF_SIZE);

        // Bind tilePane width to the viewport so wrapping responds to resize
        scroller.viewportBoundsProperty().addListener((obs, ov, nb) -> {
            double w = Math.max(0, nb.getWidth() - 2);
            tilePane.setPrefWidth(w);
        });

        // Placeholder (image + label) establishes an initial footprint
        ImageView iv = ResourceUtils.loadIcon("data", 256);
        placeholder = new VBox(10, iv, placeholderLabel);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setMinSize(0, 0);
        placeholder.setPrefSize(560, 360); // gives initial size w/o pushing parent

        contentStack.getChildren().addAll(scroller, placeholder);
        placeholder.toFront();

        // Layout: optional header at TOP, content in CENTER
        setCenter(contentStack);
    }

    // ---------- Header API ----------

    /** Optional header node above the grid (e.g., search/sort bar). Pass null to clear. */
    public void setHeader(Node header) {
        this.headerNode = header;
        if (header == null) {
            setTop(null);
        } else {
            BorderPane.setAlignment(header, Pos.CENTER_LEFT);
            BorderPane.setMargin(header, new Insets(2, 8, 2, 8));
            setTop(header);
        }
    }

    // Allow parent to suggest viewport size (prevents ScrollPane from enlarging parent)
    public void setPreferredViewport(double width, double height) {
        scroller.setPrefViewportWidth(Math.max(0, width));
        scroller.setPrefViewportHeight(Math.max(0, height));
    }

    // ---------- Public API (items) ----------

    public void setItems(List<PairItem> items) {
        allItems.clear();
        if (items != null) allItems.addAll(items);
        applyFilterSortAndRender();
    }

    /** Legacy add (rebuilds grid, respects filter/sort). */
    public void addItem(PairItem item) {
        if (item != null) {
            allItems.add(item);
            applyFilterSortAndRender();
        }
    }

    /**
     * Streaming add: buffer items and flush them to the UI based on either
     * a count threshold (flushN) or a time threshold (flushMs).
     *
     * @param item              the item to append
     * @param flushN            flush when at least this many pending items (>=1)
     * @param flushMs           flush if at least this many ms since last flush (>=0; 0 disables)
     * @param incrementalSort   if true, apply current sorter on each flush; if false, preserve append order
     */
    public void addItemStreaming(PairItem item, int flushN, int flushMs, boolean incrementalSort) {
        if (item == null) return;

        // sanitize thresholds
        if (flushN < 1) flushN = 1;
        if (flushMs < 0) flushMs = 0;

        boolean schedule = false;
        long now = System.nanoTime();

        synchronized (streamLock) {
            pendingBuffer.add(item);
            long elapsedMs = (now - lastFlushNanos) / 1_000_000L;
            if (pendingBuffer.size() >= flushN || (flushMs > 0 && elapsedMs >= flushMs)) {
                if (!flushScheduled) {
                    flushScheduled = true;
                    schedule = true;
                }
            }
        }

        if (schedule) {
            final boolean doSort = incrementalSort;
            Platform.runLater(() -> {
                List<PairItem> toAdd;
                synchronized (streamLock) {
                    toAdd = new ArrayList<>(pendingBuffer);
                    pendingBuffer.clear();
                    lastFlushNanos = System.nanoTime();
                    flushScheduled = false;
                }
                allItems.addAll(toAdd);
                applyFilterSortAndRenderInternal(doSort);
            });
        }
    }

    public void clearItems() {
        allItems.clear();
        applyFilterSortAndRender();
    }

    public void setFilter(Predicate<PairItem> filter) {
        this.filter = filter;
        applyFilterSortAndRender();
    }

    public void setSorter(Comparator<PairItem> sorter) {
        this.sorter = sorter;
        applyFilterSortAndRender();
    }

    public void sortByScoreDescending() {
        setSorter(Comparator.nullsLast((a, b) -> {
            double sa = a.score == null ? Double.NEGATIVE_INFINITY : a.score;
            double sb = b.score == null ? Double.NEGATIVE_INFINITY : b.score;
            return -Double.compare(sa, sb);
        }));
    }

    /** Preferred columns hint; wrapping still adapts to viewport width. */
    public void setColumns(int cols) {
        this.columns = Math.max(1, cols);
        tilePane.setPrefColumns(this.columns);
        renderTiles();
    }

    /** Sets the preferred content size of the heatmap region inside each tile. */
    public void setCellSize(double width, double height) {
        this.cellWidth = Math.max(60, width);
        this.cellHeight = Math.max(60, height);
        renderTiles();
    }

    public void setCellGaps(double hgap, double vgap) {
        this.cellHGap = Math.max(0, hgap);
        this.cellVGap = Math.max(0, vgap);
        tilePane.setHgap(cellHGap);
        tilePane.setVgap(cellVGap);
        renderTiles();
    }

    public void setCellPadding(Insets insets) {
        this.cellPadding = insets == null ? Insets.EMPTY : insets;
        renderTiles();
    }

    public void setShowScoresInHeader(boolean show) {
        this.showScoresInHeader = show;
        renderTiles();
    }

    public void setOnCellClick(Consumer<CellClick> onClick) {
        this.onCellClick = onClick;
    }

    public List<PairItem> getVisibleItems() {
        return Collections.unmodifiableList(visibleItems);
    }

    // ---------- Public API (appearance) ----------

    public void setPalette(HeatmapThumbnailView.PaletteKind p) {
        if (p == null) return;
        display.palette = p;
        refreshExistingTiles();
    }

    public void setLegendVisible(boolean visible) {
        display.legend = visible;
        refreshExistingTiles();
    }

    public void setBackgroundColor(Color c) {
        display.background = (c == null ? Color.BLACK : c);
        refreshExistingTiles();
    }

    public void setImageSmoothing(boolean smoothing) {
        display.smoothing = smoothing;
        refreshExistingTiles();
    }

    public void setGamma(double gamma) {
        display.gamma = clamp(gamma, 0.1, 5.0);
        refreshExistingTiles();
    }

    public void setClipLowPct(double pct) {
        display.clipLow = clamp(pct, 0.0, 20.0);
        recomputeGlobalIfNeeded();
        refreshExistingTiles();
    }

    public void setClipHighPct(double pct) {
        display.clipHigh = clamp(pct, 0.0, 20.0);
        recomputeGlobalIfNeeded();
        refreshExistingTiles();
    }

    public void setRangeModeAuto() {
        display.rangeMode = RangeMode.AUTO;
        refreshExistingTiles();
    }

    public void setRangeModeGlobal() {
        display.rangeMode = RangeMode.GLOBAL;
        this.globalRange = computeGlobalRange();
        refreshExistingTiles();
    }

    public void setRangeModeFixed(double vmin, double vmax) {
        if (!(Double.isFinite(vmin) && Double.isFinite(vmax))) return;
        double mn = Math.min(vmin, vmax);
        double mx = Math.max(vmin, vmax);
        if (mx - mn < 1e-12) mx = mn + 1e-12;
        display.rangeMode = RangeMode.FIXED;
        display.fixedMin = mn; display.fixedMax = mx;
        refreshExistingTiles();
    }

    /** Computes min/max across visible items’ data (grids or res). */
    public Range computeGlobalRange() {
        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
        for (PairItem it : visibleItems) {
            List<List<Double>> g = it.grid;
            if (g == null && it.res != null) {
                g = (it.useCDF ? it.res.cdfAsListGrid() : it.res.pdfAsListGrid());
            }
            if (g == null) continue;
            for (int r=0; r<g.size(); r++){
                List<Double> row = g.get(r);
                if (row == null) continue;
                for (int c=0; c<row.size(); c++){
                    Double vv = row.get(c);
                    if (vv!=null && Double.isFinite(vv)){
                        if (vv < lo) lo = vv;
                        if (vv > hi) hi = vv;
                    }
                }
            }
        }
        if (!Double.isFinite(lo) || !Double.isFinite(hi)) return null;
        if (hi - lo < 1e-12) hi = lo + 1e-12;
        return new Range(lo, hi);
    }

    // ---------- Internal rendering ----------

    private void applyFilterSortAndRender() {
        List<PairItem> tmp = new ArrayList<>();
        for (PairItem it : allItems) {
            if (filter == null || filter.test(it)) tmp.add(it);
        }
        if (sorter != null) tmp.sort(sorter);
        visibleItems = tmp;
        renderTiles();
    }

    /** Recompute visibleItems with optional sort, then render. */
    private void applyFilterSortAndRenderInternal(boolean doSort) {
        List<PairItem> tmp = new ArrayList<>();
        for (PairItem it : allItems) {
            if (filter == null || filter.test(it)) tmp.add(it);
        }
        if (doSort && sorter != null) {
            tmp.sort(sorter);
        }
        visibleItems = tmp;
        renderTiles();
    }

    private void renderTiles() {
        tilePane.getChildren().clear();
        if (visibleItems.isEmpty()) {
            placeholder.setVisible(true);
            scroller.setVisible(false);
            return;
        }
        placeholder.setVisible(false);
        scroller.setVisible(true);

        for (int i = 0; i < visibleItems.size(); i++) {
            PairItem item = visibleItems.get(i);
            Node cell = buildCell(i, item);
            tilePane.getChildren().add(cell);
        }
    }

    private Node buildCell(int index, PairItem item) {
        String title = item.xLabel + " | " + item.yLabel;
        if (showScoresInHeader && item.score != null) {
            title += "   " + String.format("score=%.4f", item.score);
        }
        Label header = new Label(title);
        header.setPadding(new Insets(2, 4, 2, 4));

        HeatmapThumbnailView view = new HeatmapThumbnailView();
        view.setPrefSize(cellWidth, cellHeight);
        view.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        view.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Apply item-specific basics first (flip, palette choice)
        view.setFlipY(item.flipY);
        if (item.palette == HeatmapThumbnailView.PaletteKind.DIVERGING) {
            double c = item.center == null ? 0.0 : item.center;
            view.useDivergingPalette(c);
        } else {
            view.useSequentialPalette();
        }
        view.setShowLegend(item.showLegend);

        // Data
        if (item.grid != null) {
            view.setGrid(item.grid);
        } else if (item.res != null) {
            view.setFromGridDensity(item.res, item.useCDF, item.flipY);
        }

        // Apply grid-wide display state (overrides range choice)
        applyViewDisplay(view, item);

        BorderPane cell = new BorderPane();
        cell.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER_LEFT);
        BorderPane.setMargin(header, new Insets(0, 0, 4, 0));

        StackPane center = new StackPane(view);
        center.setPadding(cellPadding);
        cell.setCenter(center);

        // keep programmatic border (no inline CSS classes)
        cell.setBorder(new Border(new BorderStroke(
                Color.web("#3A3A3A"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(6),
                new BorderWidths(1.0)
        )));
        cell.setBackground(null);

        // Click handling
        cell.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> cell.setCursor(Cursor.HAND));
        cell.addEventHandler(MouseEvent.MOUSE_EXITED, e -> cell.setCursor(Cursor.DEFAULT));
        cell.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onCellClick != null) onCellClick.accept(new CellClick(index, item));
        });

        // Context menu per-tile
        ContextMenu cm = buildContextMenuForTile(view, item);
        cell.setOnContextMenuRequested(e -> cm.show(cell, e.getScreenX(), e.getScreenY()));
        view.setOnContextMenuRequested(e -> cm.show(view, e.getScreenX(), e.getScreenY()));

        // Hover preview: temporarily autoscale this tile if grid is in Global/Fixed
        cell.setOnMouseEntered(e -> {
            if (display.rangeMode != RangeMode.AUTO) {
                // Temporarily show local auto contrast
                view.setAutoRange(true);
                // respect clip/gamma/background/smoothing
                view.setClipPercent(display.clipLow, display.clipHigh);
                view.setGamma(display.gamma);
                view.setImageSmoothing(display.smoothing);
                view.setBackgroundColor(display.background);
            }
        });
        cell.setOnMouseExited(e -> {
            if (display.rangeMode != RangeMode.AUTO) {
                // Restore grid-wide settings
                applyViewDisplay(view, item);
            }
        });

        return cell;
    }

    private ContextMenu buildContextMenuForTile(HeatmapThumbnailView view, PairItem item) {
        MenuItem autoscaleThis = new MenuItem("Autoscale this tile");
        autoscaleThis.setOnAction(e -> {
            view.setAutoRange(true);
            view.setClipPercent(display.clipLow, display.clipHigh);
        });

        MenuItem pinFixedFromThis = new MenuItem("Pin fixed range from this tile");
        pinFixedFromThis.setOnAction(e -> {
            double mn = view.getVmin();
            double mx = view.getVmax();
            setRangeModeFixed(mn, mx);
        });

        MenuItem computeGlobal = new MenuItem("Compute global range from visible");
        computeGlobal.setOnAction(e -> {
            Range r = computeGlobalRange();
            if (r != null) {
                this.globalRange = r;
                setRangeModeGlobal();
            }
        });

        MenuItem exportPng = new MenuItem("Export PNG…");
        exportPng.setOnAction(e -> {
            if (onExportRequest != null) {
                SnapshotParameters sp = new SnapshotParameters();
                WritableImage snap = view.snapshot(sp, null);
                onExportRequest.accept(new ExportRequest(item, snap));
            }
        });

        ContextMenu cm = new ContextMenu(autoscaleThis, pinFixedFromThis, computeGlobal, exportPng);

        // Optional live summary row (title only) as disabled CustomMenuItem
        Label meta = new Label(item.xLabel + " vs " + item.yLabel);
        CustomMenuItem info = new CustomMenuItem(meta, false);
        info.setDisable(true);
        cm.getItems().add(0, info);

        return cm;
    }

    /** Apply current grid-wide display state to a specific view. */
    private void applyViewDisplay(HeatmapThumbnailView view, PairItem item) {
        // palette base is already set from the item; allow grid palette override if desired
        if (display.palette == HeatmapThumbnailView.PaletteKind.DIVERGING) {
            double c = (item.center != null) ? item.center : view.getDivergingCenter();
            view.useDivergingPalette(c);
        } else {
            view.useSequentialPalette();
        }

        view.setShowLegend(display.legend);
        view.setBackgroundColor(display.background);
        view.setImageSmoothing(display.smoothing);
        view.setGamma(display.gamma);
        view.setClipPercent(display.clipLow, display.clipHigh);

        // Range precedence: FIXED > GLOBAL > item-setting
        if (display.rangeMode == RangeMode.FIXED &&
                Double.isFinite(display.fixedMin) && Double.isFinite(display.fixedMax)) {
            view.setFixedRange(display.fixedMin, display.fixedMax);
        } else if (display.rangeMode == RangeMode.GLOBAL && globalRange != null) {
            view.setFixedRange(globalRange.vmin, globalRange.vmax);
        } else {
            // respect item preference
            if (!item.autoRange && item.vmin != null && item.vmax != null) {
                view.setFixedRange(item.vmin, item.vmax);
            } else {
                view.setAutoRange(true);
            }
        }
    }

    /** Re-apply display state to all existing tiles. */
    private void refreshExistingTiles() {
        for (Node n : tilePane.getChildren()) {
            if (!(n instanceof BorderPane bp)) continue;
            Node center = bp.getCenter();
            if (!(center instanceof StackPane sp)) continue;
            if (sp.getChildren().isEmpty()) continue;

            Node hv = sp.getChildren().get(0);
            if (!(hv instanceof HeatmapThumbnailView view)) continue;

            // Find the PairItem associated to this cell (by header label lookup index)
            // Safer: rebuild association by index position in tilePane.
            int idx = tilePane.getChildren().indexOf(n);
            if (idx < 0 || idx >= visibleItems.size()) continue;
            PairItem item = visibleItems.get(idx);

            applyViewDisplay(view, item);
        }
    }

    private void recomputeGlobalIfNeeded() {
        if (display.rangeMode == RangeMode.GLOBAL) {
            this.globalRange = computeGlobalRange();
        }
    }
    // In PairGridPane (public helper)
    public interface ViewConsumer { void accept(HeatmapThumbnailView v); }
    public void forEachView(ViewConsumer fn) {
        for (Node n : tilePane.getChildren()) {
            if (!(n instanceof BorderPane bp)) continue;
            Node center = bp.getCenter();
            if (!(center instanceof StackPane sp) || sp.getChildren().isEmpty()) continue;
            Node hv = sp.getChildren().get(0);
            if (hv instanceof HeatmapThumbnailView v) fn.accept(v);
        }
    }

    // ---------- Utils ----------

    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi ? hi : v);
    }
}
