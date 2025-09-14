package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.FeatureVectorManagerView;
import edu.jhuapl.trinity.javafx.services.FeatureVectorManagerService;
import edu.jhuapl.trinity.javafx.services.FeatureVectorManagerService.SamplingMode;
import edu.jhuapl.trinity.javafx.services.FeatureVectorManagerServiceImpl;
import edu.jhuapl.trinity.javafx.services.InMemoryFeatureVectorRepository;
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
    private final FeatureVectorManagerService service;

    // Windowing context menu (RMB)
    private final ContextMenu ctxMenu = new ContextMenu();
    private final MenuItem miOpenFull = new MenuItem("Open Full View");
    private final MenuItem miPopOut   = new MenuItem("Pop-out");

    // External hooks
    private Runnable onOpenFullRequested = () -> {};
    private Runnable onPopOutRequested   = () -> {};

    public FeatureVectorManagerPane(Scene scene, Pane parent) {
        this(scene, parent, new FeatureVectorManagerServiceImpl(new InMemoryFeatureVectorRepository()));
    }

    public FeatureVectorManagerPane(Scene scene, Pane parent, FeatureVectorManagerService service) {
        super(scene, parent, DEFAULT_WIDTH, DEFAULT_HEIGHT,
              new BorderPane(), "Feature Vectors", "Manager", 300.0, 400.0);

        this.service = service;

        // Allow service to fire events to the scene root (flattened list to workspace)
        if (service instanceof FeatureVectorManagerServiceImpl impl && scene != null) {
            impl.setEventTarget(scene.getRoot());
        }

        view = new FeatureVectorManagerView();
        view.setDetailLevel(FeatureVectorManagerView.DetailLevel.COMPACT);

        BorderPane root = (BorderPane) this.contentPane;
        root.setCenter(view);
        root.setPadding(new Insets(2));

        // ---- Wire lists ----
        view.getTable().setItems(service.getDisplayedVectors());
        view.setCollections(service.getCollectionNames());

        // ---- Active collection (two-way) ----
        view.selectedCollectionProperty().addListener((obs, o, n) -> {
            if (n != null && !n.equals(service.activeCollectionNameProperty().get())) {
                service.activeCollectionNameProperty().set(n);
            }
        });
        service.activeCollectionNameProperty().addListener((obs, o, n) -> {
            if (n != null && !n.equals(view.getSelectedCollection())) {
                view.getCollectionSelector().getSelectionModel().select(n);
            }
        });
        if (service.activeCollectionNameProperty().get() != null) {
            view.getCollectionSelector().getSelectionModel().select(service.activeCollectionNameProperty().get());
        }

        // ---- Sampling mode (string <-> enum) ----
        view.samplingModeProperty().addListener((obs, o, n) -> {
            SamplingMode m = fromStringSampling(n);
            if (m != service.samplingModeProperty().get()) {
                service.samplingModeProperty().set(m);
            }
        });
        service.samplingModeProperty().addListener((obs, o, n) -> {
            String s = toStringSampling(n);
            if (!s.equals(view.getSamplingMode())) {
                view.samplingModeProperty().set(s);
            }
        });

        // ---- Status / progress passthrough ----
        service.statusProperty().addListener((obs, o, n) -> view.setStatus(n));
        service.progressProperty().addListener((obs, o, n) -> {
            double v = (n == null) ? -1 : n.doubleValue();
            view.showProgress(v >= 0);
            // If you decide to show determinate progress later, extend the view to set value.
        });

        // ---- Context menu (windowing only, as requested) ----
        miOpenFull.setOnAction(e -> onOpenFullRequested.run());
        miPopOut.setOnAction(e -> onPopOutRequested.run());
        ctxMenu.getItems().setAll(miOpenFull, miPopOut);

        this.setOnContextMenuRequested(e -> ctxMenu.show(this, e.getScreenX(), e.getScreenY()));
        view.setOnContextMenuRequested(e -> ctxMenu.show(view, e.getScreenX(), e.getScreenY()));
        view.getTable().setOnContextMenuRequested(e -> ctxMenu.show(view.getTable(), e.getScreenX(), e.getScreenY()));
    }

    // --- Sampling mapping helpers ---
    private static SamplingMode fromStringSampling(String s) {
        if (s == null) return SamplingMode.ALL;
        String l = s.toLowerCase();
        if (l.startsWith("head"))   return SamplingMode.HEAD_1000;
        if (l.startsWith("tail"))   return SamplingMode.TAIL_1000;
        if (l.startsWith("random")) return SamplingMode.RANDOM_1000;
        return SamplingMode.ALL;
    }
    private static String toStringSampling(SamplingMode m) {
        if (m == null) return "All";
        switch (m) {
            case HEAD_1000:   return "Head (1000)";
            case TAIL_1000:   return "Tail (1000)";
            case RANDOM_1000: return "Random (1000)";
            default:          return "All";
        }
    }

    // External hooks for windowing
    public void setOnOpenFullRequested(Runnable r) { this.onOpenFullRequested = (r != null) ? r : () -> {}; }
    public void setOnPopOutRequested(Runnable r)   { this.onPopOutRequested   = (r != null) ? r : () -> {}; }

    // Convenience
    public FeatureVectorManagerView getView() { return view; }
    public void setDetailLevel(FeatureVectorManagerView.DetailLevel level) { view.setDetailLevel(level); }
    public void setVectors(List<FeatureVector> vectors) { service.replaceActiveVectors(vectors); }
}