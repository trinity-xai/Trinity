package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.components.panes.LitPathPane;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Floating, non-modal review of candidate topics. All commands use HyperdriveEvent. */
public final class TopicDiscoveryReviewPane extends LitPathPane {
    private final Scene reviewScene;
    private final List<TopicDiscoveryEngine.Topic> topics = new ArrayList<>();
    private final ListView<TopicDiscoveryEngine.Topic> list = new ListView<>();
    private final TextField title = new TextField();
    private final TextArea summary = new TextArea();
    private final TextArea evidence = new TextArea();
    private final Label stats = new Label();
    private final CheckBox include = new CheckBox("Include in generated landmarks");
    private int nextTopicId = 100000;
    private final EventHandler<HyperdriveEvent> refreshHandler = event -> refreshDisplay();

    public TopicDiscoveryReviewPane(Scene scene, Pane parent, List<TopicDiscoveryEngine.Topic> candidates) {
        super(scene, parent, 1000, 690, new BorderPane(),
            "Hyperdrive", "Topic Discovery Review", 300.0, 400.0);
        reviewScene = scene;
        topics.addAll(candidates);
        getStylesheets().add(StyleResourceProvider.getResource("styles.css").toExternalForm());
        getStylesheets().add(StyleResourceProvider.getResource("covalent.css").toExternalForm());

        list.getItems().setAll(topics);
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.setPrefWidth(390);
        list.setPrefHeight(470);
        title.setPromptText("Topic name");
        summary.setWrapText(true);
        summary.setPrefRowCount(3);
        evidence.setEditable(false);
        evidence.setWrapText(true);
        evidence.setPrefRowCount(12);
        VBox.setVgrow(evidence, Priority.ALWAYS);

        list.getSelectionModel().selectedItemProperty().addListener((obs, before, after) -> populate());
        include.setOnAction(e -> {
            TopicDiscoveryEngine.Topic topic = list.getSelectionModel().getSelectedItem();
            if (topic == null) return;
            topic.setIncluded(include.isSelected());
            notifyUpdated();
        });

        Button save = new Button("Save Title / Summary");
        save.setOnAction(e -> {
            TopicDiscoveryEngine.Topic topic = list.getSelectionModel().getSelectedItem();
            if (topic == null) return;
            topic.setName(title.getText());
            topic.setSummary(summary.getText());
            notifyUpdated();
            populate();
        });
        Button merge = new Button("Merge Selected");
        merge.setOnAction(e -> {
            List<TopicDiscoveryEngine.Topic> chosen = List.copyOf(list.getSelectionModel().getSelectedItems());
            if (chosen.size() < 2) {
                warn("Select at least two topics to merge.");
                return;
            }
            // Use a fresh ID even after reopening the review workspace.
            while (containsTopicId(nextTopicId)) nextTopicId++;
            TopicDiscoveryEngine.Topic merged = TopicDiscoveryEngine.merge(nextTopicId++, chosen);
            topics.removeAll(chosen);
            topics.add(merged);
            topics.sort(Comparator.comparingInt(TopicDiscoveryEngine.Topic::size).reversed());
            TopicDiscoveryEngine.scoreDistinctiveness(topics);
            list.getItems().setAll(topics);
            list.getSelectionModel().select(merged);
            notifyUpdated();
        });
        Button summarize = new Button("Summarize Selected (Chat Model)");
        summarize.setOnAction(e -> {
            TopicDiscoveryEngine.Topic topic = list.getSelectionModel().getSelectedItem();
            if (topic == null) return;
            topic.setName(title.getText());
            topic.setSummary(summary.getText());
            notifyUpdated();
            reviewScene.getRoot().fireEvent(new HyperdriveEvent(
                HyperdriveEvent.SUMMARIZE_DISCOVERED_TOPIC, topic));
        });
        Button generate = new Button("Generate Selected Landmarks");
        generate.setOnAction(e -> {
            List<TopicDiscoveryEngine.Topic> selected = topics.stream()
                .filter(TopicDiscoveryEngine.Topic::included)
                .filter(t -> !t.name().isBlank())
                .toList();
            if (selected.isEmpty()) {
                warn("Select at least one topic for landmark generation.");
                return;
            }
            reviewScene.getRoot().fireEvent(new HyperdriveEvent(
                HyperdriveEvent.GENERATE_TOPIC_LANDMARKS, List.copyOf(selected)));
            close();
        });
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> close());

        VBox left = new VBox(8, new Label("Candidate Topics (Ctrl-click to merge)"),
            list, merge, generate);
        left.setPrefWidth(400);
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox details = new VBox(8, new Label("Topic Name"), title,
            new Label("Description"), summary, include, stats, save,
            summarize, new Label("Representative Content"), evidence);
        details.setPrefWidth(520);
        HBox.setHgrow(details, Priority.ALWAYS);
        HBox body = new HBox(12, left, details);
        HBox.setHgrow(left, Priority.ALWAYS);
        VBox.setVgrow(body, Priority.ALWAYS);
        HBox footer = new HBox(closeButton);
        footer.setPadding(new Insets(5));
        VBox layout = new VBox(10, body, footer);
        layout.setPadding(new Insets(10));
        VBox.setVgrow(body, Priority.ALWAYS);
        ((BorderPane) contentPane).setCenter(layout);
        reviewScene.getRoot().addEventHandler(HyperdriveEvent.REFRESH_TOPIC_REVIEW, refreshHandler);
        list.getSelectionModel().selectFirst();
    }

    private boolean containsTopicId(int id) {
        return topics.stream().anyMatch(topic -> topic.id() == id);
    }

    private void populate() {
        TopicDiscoveryEngine.Topic topic = list.getSelectionModel().getSelectedItem();
        if (topic == null) {
            title.clear();
            summary.clear();
            evidence.clear();
            stats.setText("");
            return;
        }
        title.setText(topic.name());
        summary.setText(topic.summary());
        include.setSelected(topic.included());
        stats.setText(topic.size() + " chunks · cohesion "
            + String.format(Locale.ROOT, "%.3f", topic.cohesion())
            + " · distinctiveness "
            + String.format(Locale.ROOT, "%.3f", topic.distinctiveness())
            + " · keywords: " + String.join(", ", topic.keywords()));
        evidence.setText(String.join("\n\n---\n\n", topic.representative()));
    }

    private void refreshDisplay() {
        list.refresh();
        populate();
    }

    private void notifyUpdated() {
        list.refresh();
        reviewScene.getRoot().fireEvent(new HyperdriveEvent(
            HyperdriveEvent.TOPIC_REVIEW_UPDATED, List.copyOf(topics)));
    }

    private void warn(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setTitle("Topic Discovery");
        alert.setHeaderText("Review topics");
        alert.getDialogPane().getStylesheets().add(
            StyleResourceProvider.getResource("dialogstyles.css").toExternalForm());
        alert.showAndWait();
    }

    @Override
    public void onClose() {
        reviewScene.getRoot().removeEventHandler(HyperdriveEvent.REFRESH_TOPIC_REVIEW, refreshHandler);
        super.onClose();
    }
}
