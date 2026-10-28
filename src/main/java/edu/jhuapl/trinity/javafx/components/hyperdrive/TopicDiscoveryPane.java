package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.utils.clustering.ClusterMethod;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalError;
import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalSuccess;
import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalWarning;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Topic candidate discovery and human review; no embedded text is mutated here. */
public final class TopicDiscoveryPane extends VBox {
    private static final Logger LOG = LoggerFactory.getLogger(TopicDiscoveryPane.class);
    private final Scene scene;
    private final ChoiceBox<ClusterMethod> methodChoice = new ChoiceBox<>();
    private final ChoiceBox<String> scopeChoice = new ChoiceBox<>();
    private final Spinner<Integer> clusters = new Spinner<>(2, 500, 12);
    private final Spinner<Integer> minSize = new Spinner<>(2, 2000, 10);
    private final Spinner<Integer> minPts = new Spinner<>(2, 2000, 5);
    private final Spinner<Double> epsilon = new Spinner<>(0.01, 5.0, 0.5, 0.05);
    private final Spinner<Integer> iterations = new Spinner<>(10, 1000, 100);
    private final GridPane parameters = new GridPane();
    private final ListView<TopicDiscoveryEngine.Topic> candidates = new ListView<>();
    private final Label summaryLabel = new Label("No topics discovered");
    private final Button discoverButton = new Button("Discover Topics");
    private final Button reviewButton = new Button("Review Topics...");
    private final List<TopicDiscoveryEngine.Topic> topics = new ArrayList<>();
    private Task<TopicDiscoveryEngine.Result> discoveryTask;
    private long generation;
    private String sourceEmbeddingModel;

    public TopicDiscoveryPane(Scene scene) {
        this.scene = scene;
        setPadding(new Insets(5));
        setSpacing(7);
        methodChoice.getItems().setAll(ClusterMethod.values());
        methodChoice.setValue(ClusterMethod.HDDBSCAN);
        scopeChoice.getItems().setAll("All embedded text", "Selected text chunks");
        scopeChoice.getSelectionModel().selectFirst();
        for (Spinner<?> spinner : List.of(clusters, minSize, minPts, epsilon, iterations)) {
            spinner.setEditable(true);
            spinner.setPrefWidth(110);
        }
        parameters.setHgap(6);
        parameters.setVgap(4);
        methodChoice.valueProperty().addListener((obs, oldValue, value) -> updateParameters());
        updateParameters();
        candidates.setPlaceholder(new Label("Run Discover Topics"));
        candidates.setMinHeight(90);
        VBox.setVgrow(candidates, Priority.ALWAYS);
        discoverButton.setMaxWidth(Double.MAX_VALUE);
        reviewButton.setMaxWidth(Double.MAX_VALUE);
        discoverButton.setOnAction(e -> scene.getRoot().fireEvent(
            new HyperdriveEvent(HyperdriveEvent.REQUEST_TOPIC_DISCOVERY)));
        reviewButton.setOnAction(e -> openReview());
        reviewButton.setDisable(true);
        getChildren().addAll(new Label("Source"), scopeChoice, parameters, discoverButton,
            summaryLabel, candidates, reviewButton);
        scene.getRoot().addEventHandler(HyperdriveEvent.TOPIC_REVIEW_UPDATED, event -> {
            if (!(event.object1 instanceof List<?> list)) return;
            if (!list.stream().allMatch(TopicDiscoveryEngine.Topic.class::isInstance)) return;
            // Apply review edits to the same candidate collection used for landmark validation.
            topics.clear();
            for (Object value : list) topics.add((TopicDiscoveryEngine.Topic) value);
            candidates.getItems().setAll(topics);
            reviewButton.setDisable(topics.isEmpty());
        });
    }

    /** Displayed in Hyperdrive's top toolbar, not duplicated inside this pane. */
    public ChoiceBox<ClusterMethod> methodSelector() { return methodChoice; }
    public boolean selectedOnly() { return scopeChoice.getSelectionModel().getSelectedIndex() == 1; }
    public String sourceEmbeddingModel() { return sourceEmbeddingModel; }
    public void refreshDisplay() {
        candidates.refresh();
        scene.getRoot().fireEvent(new HyperdriveEvent(HyperdriveEvent.REFRESH_TOPIC_REVIEW));
    }
    public boolean stillContains(TopicDiscoveryEngine.Topic topic) { return topics.contains(topic); }

    private void updateParameters() {
        parameters.getChildren().clear();
        ClusterMethod method = methodChoice.getValue();
        if (method == null) return;
        switch (method) {
            case KMEANS, KMEDIODS, EX_MAX -> {
                parameter("Target topics", clusters, 0);
                if (method != ClusterMethod.EX_MAX) parameter("Max iterations", iterations, 1);
            }
            case DBSCAN -> {
                parameter("Epsilon", epsilon, 0);
                parameter("Min points", minPts, 1);
            }
            case HDDBSCAN -> {
                parameter("Min cluster size", minSize, 0);
                parameter("Min points", minPts, 1);
            }
            case AFFINITY -> parameter("Max iterations", iterations, 0);
        }
    }
    private void parameter(String label, Control control, int row) {
        parameters.add(new Label(label), 0, row);
        parameters.add(control, 1, row);
    }

    public void discover(List<TopicDiscoveryEngine.Sample> samples, int excluded, String embeddingModel) {
        if (samples.size() < 2) { warn("Topic discovery requires two or more valid embeddings."); return; }
        if (methodChoice.getValue() == ClusterMethod.AFFINITY && samples.size() > 1000) {
            warn("Affinity Propagation is limited to 1,000 chunks. Select a subset or another method.");
            return;
        }
        // Spinner editors may contain an uncommitted typed value.
        for (Spinner<?> spinner : List.of(clusters, minSize, minPts, epsilon, iterations)) spinner.commitValue();
        // Snapshot controls and vectors before starting the worker.
        TopicDiscoveryEngine.Options settings = new TopicDiscoveryEngine.Options(methodChoice.getValue(),
            clusters.getValue(), minSize.getValue(), minPts.getValue(), epsilon.getValue(), iterations.getValue());
        if (discoveryTask != null) discoveryTask.cancel();
        scene.getRoot().fireEvent(new HyperdriveEvent(HyperdriveEvent.CLOSE_TOPIC_REVIEW));
        long run = ++generation;
        sourceEmbeddingModel = embeddingModel;
        topics.clear();
        candidates.getItems().clear();
        reviewButton.setDisable(true);
        discoverButton.setDisable(true);
        summaryLabel.setText("Discovering topics...");
        discoveryTask = new Task<>() {
            @Override protected TopicDiscoveryEngine.Result call() {
                return TopicDiscoveryEngine.discover(samples, settings);
            }
        };
        Task<TopicDiscoveryEngine.Result> task = discoveryTask;
        task.setOnSucceeded(e -> {
            if (run != generation) return;
            TopicDiscoveryEngine.Result result = task.getValue();
            topics.addAll(result.topics());
            candidates.getItems().setAll(topics);
            if (!topics.isEmpty()) candidates.getSelectionModel().selectFirst();
            summaryLabel.setText(topics.size() + " topics · " + result.noiseCount() + " noise · " + excluded + " excluded");
            discoverButton.setDisable(false);
            reviewButton.setDisable(topics.isEmpty());
            if (topics.isEmpty()) notifyTerminalWarning("Topic discovery found no clusters. Adjust the method parameters.", scene);
            else notifyTerminalSuccess("Discovered " + topics.size() + " candidate topics from " + result.sampleCount() + " text chunks.", scene);
        });
        task.setOnFailed(e -> {
            if (run != generation) return;
            discoverButton.setDisable(false);
            summaryLabel.setText("Discovery failed");
            LOG.error("Topic discovery failed", task.getException());
            notifyTerminalError("Topic discovery failed: " + task.getException().getMessage(), scene);
        });
        task.setOnCancelled(e -> { if (run == generation) discoverButton.setDisable(false); });
        Thread worker = new Thread(task, "Trinity Hyperdrive Topic Discovery");
        worker.setDaemon(true);
        worker.start();
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Topic Discovery");
        alert.setHeaderText("Unable to discover topics");
        alert.getDialogPane().getStylesheets().add(
            StyleResourceProvider.getResource("dialogstyles.css").toExternalForm());
        alert.showAndWait();
    }

    private void openReview() {
        if (topics.isEmpty()) return;
        scene.getRoot().fireEvent(new HyperdriveEvent(
            HyperdriveEvent.OPEN_TOPIC_REVIEW, List.copyOf(topics)));
    }
}
