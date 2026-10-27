package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.jhuapl.trinity.javafx.events.CommandTerminalEvent.notifyTerminalWarning;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/** Parses text on a worker thread and creates JavaFX list items on the FX thread. */
public class LoadTextTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(LoadTextTask.class);
    private final List<File> files;
    private final TextChunkParser.Options options;

    public LoadTextTask(Scene scene, CircleProgressIndicator progressIndicator, List<File> files) {
        super(scene, progressIndicator, new AtomicInteger(), null);
        this.files = List.copyOf(files);
        // Consistent processing options for every file in this import job.
        this.options = EmbeddingsTextListItem.parsingOptions();
    }

    private record ParsedFile(File file, List<TextChunkParser.ParsedChunk> chunks) { }

    @Override
    protected void processTask() throws Exception {
        if (progressIndicator != null) {
            Platform.runLater(() -> {
                progressIndicator.setFadeTimeMS(250);
                progressIndicator.setLabelLater("Discovering text files...");
                progressIndicator.spin(true);
                progressIndicator.fadeBusy(false);
            });
        }

        List<File> found = new ArrayList<>();
        for (File file : files) {
            if (isCancelled()) return;
            if (file.isDirectory()) {
                try (Stream<Path> paths = Files.walk(file.toPath())) {
                    paths.map(Path::toFile)
                        .filter(f -> ResourceUtils.isTextFile(f) || ResourceUtils.isPDF(f))
                        .forEach(found::add);
                } catch (IOException ex) {
                    LOG.error("Unable to discover files in {}", file, ex);
                }
            } else if (ResourceUtils.isTextFile(file) || ResourceUtils.isPDF(file)) {
                found.add(file);
            }
        }

        // Deduplicate files discovered from overlapping input directories.
        List<File> discovered = new ArrayList<>(found.stream().distinct().toList());
        int total = discovered.size();
        int failed = 0;
        int empty = 0;
        List<ParsedFile> parsed = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            if (isCancelled()) return;
            File file = discovered.get(i);
            try {
                List<TextChunkParser.ParsedChunk> chunks = TextChunkParser.parse(file, options);
                if (chunks.isEmpty()) {
                    empty++;
                    LOG.warn("No usable text found in {}", file);
                } else {
                    parsed.add(new ParsedFile(file, chunks));
                }
            } catch (Exception ex) {
                failed++;
                LOG.error("Unable to parse text file {}", file, ex);
            }
            int processed = i + 1;
            if (progressIndicator != null) {
                progressIndicator.setLabelLater("Read " + processed + " of " + total + " files");
            }
        }
        int failedCount = failed;
        int emptyCount = empty;
        int parsedCount = parsed.size();
        LOG.info("Parsed {} text files ({} failures, {} empty)", parsedCount, failedCount, emptyCount);

        Platform.runLater(() -> {
            List<EmbeddingsTextListItem> items = new ArrayList<>();
            ArrayList<File> loadedFiles = new ArrayList<>();
            for (ParsedFile file : parsed) {
                loadedFiles.add(file.file());
                for (TextChunkParser.ParsedChunk chunk : file.chunks()) {
                    items.add(EmbeddingsTextListItem.fromParsedChunk(file.file(), chunk));
                }
            }
            scene.getRoot().fireEvent(
                new HyperdriveEvent(HyperdriveEvent.NEW_BATCH_TEXTLOAD, items, loadedFiles));
            if (failedCount + emptyCount > 0) {
                notifyTerminalWarning("Text import: " + failedCount + " failed file(s), "
                    + emptyCount + " with no usable text (see application log).", scene);
            }
            if (progressIndicator != null) {
                progressIndicator.setLabelLater("Loaded " + parsedCount + " files / " + items.size() + " chunks"
                    + (failedCount + emptyCount > 0 ? " (" + failedCount + " errors, " + emptyCount + " empty)" : ""));
                progressIndicator.spin(false);
                progressIndicator.fadeBusy(true);
            }
        });
    }
}
