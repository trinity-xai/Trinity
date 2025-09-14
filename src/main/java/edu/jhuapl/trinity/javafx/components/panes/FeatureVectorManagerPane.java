// File: src/main/java/edu/jhuapl/trinity/javafx/components/panes/FeatureVectorManagerPane.java
package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.FeatureVectorManagerView;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.util.List;

public class FeatureVectorManagerPane extends LitPathPane {

    private static final int DEFAULT_WIDTH = 980;
    private static final int DEFAULT_HEIGHT = 680;

    private final FeatureVectorManagerView view;

    private final ContextMenu ctxMenu = new ContextMenu();
    private final MenuItem miOpenFull = new MenuItem("Open Full View");
    private final MenuItem miPopOut   = new MenuItem("Pop-out");

    private Runnable onOpenFullRequested = () -> {};
    private Runnable onPopOutRequested = () -> {};

    public FeatureVectorManagerPane(Scene scene, Pane parent) {
        super(scene, parent, DEFAULT_WIDTH, DEFAULT_HEIGHT,
              new BorderPane(), "Feature Vectors", "Manager", 300.0, 400.0);

        view = new FeatureVectorManagerView();

        BorderPane root = (BorderPane) this.contentPane;
        root.setCenter(view);
        root.setPadding(new Insets(2)); // was 8

        view.setDetailLevel(FeatureVectorManagerView.DetailLevel.COMPACT);

        miOpenFull.setOnAction(e -> onOpenFullRequested.run());
        miPopOut.setOnAction(e -> onPopOutRequested.run());
        ctxMenu.getItems().setAll(miOpenFull, miPopOut);

        this.setOnContextMenuRequested(e -> ctxMenu.show(this, e.getScreenX(), e.getScreenY()));
        view.setOnContextMenuRequested(e -> ctxMenu.show(view, e.getScreenX(), e.getScreenY()));
        view.getTable().setOnContextMenuRequested(e -> ctxMenu.show(view.getTable(), e.getScreenX(), e.getScreenY()));
    }

    public void setOnOpenFullRequested(Runnable r) { this.onOpenFullRequested = (r != null) ? r : () -> {}; }
    public void setOnPopOutRequested(Runnable r) { this.onPopOutRequested = (r != null) ? r : () -> {}; }

    public FeatureVectorManagerView getView() { return view; }

    public void setDetailLevel(FeatureVectorManagerView.DetailLevel level) { view.setDetailLevel(level); }
    public void setVectors(List<FeatureVector> vectors) { view.setVectors(vectors); }
    public void setCollections(List<String> names) { view.setCollections(names); }
    public void setStatus(String message) { view.setStatus(message); }
    public void showProgress(boolean show) { view.showProgress(show); }
}
