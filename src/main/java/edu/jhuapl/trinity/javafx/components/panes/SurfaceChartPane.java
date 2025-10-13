package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.AnalysisVectorBox;
import edu.jhuapl.trinity.javafx.components.FactorControlBox;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class SurfaceChartPane extends LitPathPane {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    BorderPane bp;
    FactorControlBox fcb;
    AnalysisVectorBox avb;

    private static BorderPane createContent() {
        return new BorderPane();
    }

    public SurfaceChartPane(Scene scene, Pane parent) {
        super(scene, parent, 450, 350, createContent(), "Hypersurface Gradients ", "", 200.0, 300.0);
        this.scene = scene;
        fcb = new FactorControlBox(500, 400);
        avb = new AnalysisVectorBox(500, 400);
        bp = (BorderPane) this.contentPane;

        Tab factorTab = new Tab("Factor Vectors");
        factorTab.setClosable(false);
        factorTab.setContent(fcb);

        Tab analysisTab = new Tab("Analysis Vector");
        analysisTab.setClosable(false);
        analysisTab.setContent(avb);

        TabPane tabPane = new TabPane(factorTab, analysisTab);
        bp.setCenter(tabPane);

        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.SURFACE_XFACTOR_VECTOR, e -> {
            fcb.setFactorVector(fcb.xFactorVector, (Double[]) e.object1);
        });
        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.SURFACE_ZFACTOR_VECTOR, e -> {
            fcb.setFactorVector(fcb.zFactorVector, (Double[]) e.object1);
        });
        this.scene.getRoot().addEventHandler(FactorAnalysisEvent.ANALYSIS_DATA_VECTOR, e -> {
            avb.setAnalysisVector((String) e.object1, (Double[]) e.object2);
        });

    }
}
