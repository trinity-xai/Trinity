package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.utils.ResourceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
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
 */
public final class PairGridPane extends BorderPane {

    // ---------- Public API types ----------

    public static final class PairItem {
        public final String xLabel;
        public final String yLabel;
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

    // ---------- Fields & configuration ----------

    private final TilePane tilePane = new TilePane();
    private final ScrollPane scroller = new ScrollPane(tilePane);
    private final StackPane contentStack = new StackPane();
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

    // ---------- Construction ----------

    public PairGridPane() {
        // ScrollPane: cap viewport so it never pushes parent
        scroller.setFitToWidth(true);
        scroller.setPannable(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setMinHeight(0);
        scroller.setPrefHeight(Region.USE_COMPUTED_SIZE);
        scroller.setMaxHeight(Region.USE_COMPUTED_SIZE);
        // modest defaults; parent can override via setPreferredViewport(...)
        scroller.setPrefViewportWidth(720);
        scroller.setPrefViewportHeight(420);

        // TilePane: width bound to viewport; height doesn’t drive parent
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
        placeholderLabel.setStyle("-fx-text-fill: derive(-fx-text-base-color, -30%); -fx-font-style: italic;");

        contentStack.getChildren().addAll(scroller, placeholder);
        placeholder.toFront();

        setCenter(contentStack);
    }

    // Allow parent to suggest viewport size (prevents ScrollPane from enlarging parent)
    public void setPreferredViewport(double width, double height) {
        scroller.setPrefViewportWidth(Math.max(0, width));
        scroller.setPrefViewportHeight(Math.max(0, height));
    }

    // ---------- Public API ----------

    public void setItems(List<PairItem> items) {
        allItems.clear();
        if (items != null) allItems.addAll(items);
        applyFilterSortAndRender();
    }

    public void addItem(PairItem item) {
        if (item != null) {
            allItems.add(item);
            applyFilterSortAndRender();
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
        header.setTextFill(Color.web("#E0E0E0"));

        HeatmapThumbnailView view = new HeatmapThumbnailView();
        view.setPrefSize(cellWidth, cellHeight);
        view.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        view.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        if (item.palette == HeatmapThumbnailView.PaletteKind.DIVERGING) {
            double c = item.center == null ? 0.0 : item.center;
            view.useDivergingPalette(c);
        } else {
            view.useSequentialPalette();
        }
        view.setShowLegend(item.showLegend);
        view.setFlipY(item.flipY);

        if (item.autoRange) {
            view.setAutoRange(true);
        } else {
            double mn = item.vmin == null ? 0.0 : item.vmin;
            double mx = item.vmax == null ? 1.0 : item.vmax;
            view.setFixedRange(mn, mx);
        }

        if (item.grid != null) {
            view.setGrid(item.grid);
        } else if (item.res != null) {
            view.setFromGridDensity(item.res, item.useCDF, item.flipY);
        }

        BorderPane cell = new BorderPane();
        cell.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER_LEFT);
        BorderPane.setMargin(header, new Insets(0, 0, 4, 0));

        StackPane center = new StackPane(view);
        center.setPadding(cellPadding);
        cell.setCenter(center);

        cell.setBorder(new Border(new BorderStroke(
                Color.web("#3A3A3A"),
                BorderStrokeStyle.SOLID,
                new CornerRadii(6),
                new BorderWidths(1.0)
        )));
        cell.setBackground(null);

        cell.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> cell.setCursor(Cursor.HAND));
        cell.addEventHandler(MouseEvent.MOUSE_EXITED, e -> cell.setCursor(Cursor.DEFAULT));
        cell.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onCellClick != null) onCellClick.accept(new CellClick(index, item));
        });

        return cell;
    }
}
