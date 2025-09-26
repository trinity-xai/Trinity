package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.MatrixHeatmapView;
import edu.jhuapl.trinity.javafx.components.MatrixHeatmapView.MatrixClick;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;
import java.util.function.Consumer;

/**
 * MatrixHeatmapPane
 * -----------------
 * Floating-pane wrapper around {@link MatrixHeatmapView} so it integrates with
 * Trinity's windowing system (matches the pattern used by PairwiseJpdfPane).
 *
 * Usage:
 *   MatrixHeatmapPane pane = new MatrixHeatmapPane(scene, parent);
 *   pane.setMatrix(values);
 *   pane.setAxisLabels(labels);
 *   pane.useSequentialPalette(); // or pane.useDivergingPalette(0.0);
 *   pane.setAutoRange(true);     // or pane.setFixedRange(min, max);
 *   pane.setShowLegend(true);
 *   pane.setOnCellClick(click -> { ... });
 *
 * @author Sean Phillips
 */
public final class MatrixHeatmapPane extends LitPathPane {

    private final MatrixHeatmapView view;

    /**
     * Preferred constructor matching your floating-pane pattern.
     */
    public MatrixHeatmapPane(Scene scene, Pane parent) {
        super(
                scene,
                parent,
                900,                 // pref width
                640,                 // pref height
                new MatrixHeatmapView(),
                "Matrix Heatmap",    // window title
                "Analysis",          // category badge
                380.0,               // min width before popout
                300.0                // min height before popout
        );
        this.view = (MatrixHeatmapView) this.contentPane;

        // Wire default toast handler to CommandTerminal
        setToastHandler(msg -> Platform.runLater(() ->
                scene.getRoot().fireEvent(
                        new CommandTerminalEvent(msg, new Font("Consolas", 18), Color.LIGHTGREEN)
                )));
    }

    /**
     * Alternate constructor allowing an already-created view (rare).
     */
    public MatrixHeatmapPane(Scene scene, Pane parent, MatrixHeatmapView customView) {
        super(
                scene,
                parent,
                900,
                640,
                customView != null ? customView : new MatrixHeatmapView(),
                "Matrix Heatmap",
                "Analysis",
                380.0,
                300.0
        );
        this.view = (MatrixHeatmapView) this.contentPane;

        setToastHandler(msg -> Platform.runLater(() ->
                scene.getRoot().fireEvent(
                        new CommandTerminalEvent(msg, new Font("Consolas", 18), Color.LIGHTGREEN)
                )));
    }

    // ---------------------------------------------------------------------
    // Public API (thin pass-throughs to MatrixHeatmapView)
    // ---------------------------------------------------------------------

    /** Replace the matrix (null/empty clears the view). */
    public void setMatrix(double[][] matrix) {
        view.setMatrix(matrix);
    }

    /** Convenience overload for List<List<Double>> matrices. */
    public void setMatrix(List<List<Double>> matrix) {
        view.setMatrix(matrix);
    }

    /** Apply the same labels to rows and columns (square matrices). */
    public void setAxisLabels(List<String> labels) {
        if (labels == null) return;
        view.setRowLabels(labels);
        view.setColLabels(labels);
    }

    /** Set row labels only. */
    public void setRowLabels(List<String> labels) {
        view.setRowLabels(labels);
    }

    /** Set column labels only. */
    public void setColLabels(List<String> labels) {
        view.setColLabels(labels);
    }

    /** Use a sequential (single-hue) palette. */
    public void useSequentialPalette() {
        view.useSequentialPalette();
    }

    /** Use a diverging palette split around the given center value. */
    public void useDivergingPalette(double center) {
        view.useDivergingPalette(center);
    }

    /** Map values using auto min/max derived from current matrix. */
    public void setAutoRange(boolean on) {
        view.setAutoRange(on);
    }

    /** Map values using an explicit [vmin, vmax] range. */
    public void setFixedRange(double vmin, double vmax) {
        view.setFixedRange(vmin, vmax);
    }

    /** Show or hide the legend bar. */
    public void setShowLegend(boolean show) {
        view.setShowLegend(show);
    }

    /** Handle cell clicks (row, col, value). */
    public void setOnCellClick(Consumer<MatrixClick> handler) {
        view.setOnCellClick(handler);
    }

    /** Access to the embedded view for advanced customization. */
    public MatrixHeatmapView getView() {
        return view;
    }

    // ---------------------------------------------------------------------
    // Toast helper (same pattern used in PairwiseJpdfPane)
    // ---------------------------------------------------------------------

    private Consumer<String> toastHandler;

    public void setToastHandler(Consumer<String> handler) {
        this.toastHandler = handler;
    }

    public void toast(String msg, boolean isError) {
        Consumer<String> h = this.toastHandler;
        String prefixed = (isError ? "[Error] " : "[Info] ") + (msg == null ? "" : msg);
        if (h != null) {
            h.accept(prefixed);
        } else {
            System.out.println(prefixed);
        }
    }

    // ---------------------------------------------------------------------
    // Optional: You can override maximize() to emit a popout ApplicationEvent
    // when you add a dedicated event type for matrix heatmaps in your app.
    // ---------------------------------------------------------------------
    // @Override
    // public void maximize() {
    //     scene.getRoot().fireEvent(
    //             new ApplicationEvent(ApplicationEvent.POPOUT_MATRIX_HEATMAP, Boolean.TRUE)
    //     );
    // }
}
