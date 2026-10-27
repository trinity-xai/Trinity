package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import javafx.application.Platform;
import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Prepares file contents off the JavaFX thread; dispatch is handled by BatchRequestManager. */
public class RequestTextEmbeddingsTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTextEmbeddingsTask.class);
    private final List<EmbeddingsTextListItem> items;
    private final Consumer<List<EmbeddingsTextListItem>> onReady;

    public RequestTextEmbeddingsTask(Scene scene, List<EmbeddingsTextListItem> items,
                                     Consumer<List<EmbeddingsTextListItem>> onReady) {
        super(scene, null, new AtomicInteger(), null);
        this.items = List.copyOf(items);
        this.onReady = onReady;
    }

    @Override
    protected void processTask() {
        List<EmbeddingsTextListItem> valid = new ArrayList<>();
        for (EmbeddingsTextListItem item : items) {
            if (isCancelled()) return;
            if (item.contents == null) item.readText();
            if (item.contents == null || item.contents.isBlank()) {
                LOG.warn("Skipping empty text embedding input from {}", item.getFile());
                continue;
            }
            // Retain the exact request payload as FeatureVector text provenance.
            if (!item.contents.equals(item.getFeatureVector().getText())) {
                item.getFeatureVector().setText(item.contents);
            }
            valid.add(item);
        }
        if (!isCancelled()) Platform.runLater(() -> onReady.accept(valid));
    }
}
