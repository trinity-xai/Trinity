package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import edu.jhuapl.trinity.utils.statistics.StatPdfCdfChartPanel;
import edu.jhuapl.trinity.utils.statistics.StatisticEngine;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * Floating statistics PDF/CDF pane for Trinity analytics.
 * Displays a single StatPdfCdfChartPanel, fully resizable and ready for future controls.
 *
 * @author Sean Phillips
 */
public class StatPdfCdfPane extends LitPathPane {

    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 500;

    private BorderPane borderPane;
    private StatPdfCdfChartPanel chartPanel;

    /**
     * Create a floating PDF/CDF chart pane, with NO data or chart selected.
     * User can set data and chart options later.
     */
    public StatPdfCdfPane(Scene scene, Pane parent) {
        super(
            scene,
            parent,
            DEFAULT_WIDTH,
            DEFAULT_HEIGHT,
            new BorderPane(),
            "Statistics Chart",
            "PDF and CDF",
            300.0,
            400.0
        );
        borderPane = (BorderPane) this.contentPane;
        borderPane.setPadding(new Insets(8));

        chartPanel = new StatPdfCdfChartPanel(); // empty state
        chartPanel.setOnComputeSurface(result -> onComputeSurface(result));        
        borderPane.setCenter(chartPanel);
    }

    /**
     * Create a floating PDF/CDF chart pane with initial data and settings.
     */
    public StatPdfCdfPane(
            Scene scene,
            Pane parent,
            List<FeatureVector> vectors,
            StatisticEngine.ScalarType scalarType,
            int bins
    ) {
        super(
            scene,
            parent,
            DEFAULT_WIDTH,
            DEFAULT_HEIGHT,
            new BorderPane(),
            "Statistics Chart",
            "PDF and CDF",
            300.0,
            400.0
        );
        borderPane = (BorderPane) this.contentPane;
        borderPane.setPadding(new Insets(8));

        chartPanel = new StatPdfCdfChartPanel(vectors, scalarType, bins);
        chartPanel.setOnComputeSurface(result -> onComputeSurface(result));        
        borderPane.setCenter(chartPanel);
    }

    private void onComputeSurface(GridDensityResult result) {
        boolean useCDF = chartPanel.isSurfaceCDF();

        List<List<Double>> grid = useCDF
            ? result.cdfAsListGrid()
            : result.pdfAsListGrid();

        String label = (useCDF ? "CDF" : "PDF") + " : "
                     + chartPanel.getScalarType() + " vs "
                     + chartPanel.getYFeatureTypeForDisplay();

        if (getScene() != null) {
            getScene().getRoot().fireEvent(
                new edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent(
                    useCDF
                        ? edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent.RENDER_CDF
                        : edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent.RENDER_PDF,
                    grid,
                    result.getxCenters(),
                    result.getyCenters(),
                    label
                )
            );
        }        
    }
    public void setFeatureVectors(List<FeatureVector> vectors) {
        chartPanel.setFeatureVectors(vectors);
    }

    public StatPdfCdfChartPanel getChartPanel() {
        return chartPanel;
    }

    public StatisticEngine.ScalarType getScalarType() {
        return chartPanel.getScalarType();
    }

    public int getBins() {
        return chartPanel.getBins();
    }
}
