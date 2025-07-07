package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Sean Phillips
 */
public class LoadTextTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(LoadTextTask.class);
    List<File> files;
    
    public LoadTextTask(Scene scene, CircleProgressIndicator progressIndicator, List<File> files) {
        super(scene, progressIndicator, new AtomicInteger(), null);
        this.files = files;
    }

    @Override
    protected void processTask() throws Exception {
        AtomicInteger atomicCount = new AtomicInteger(0);
        if(null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Loading " + atomicCount.toString() + " files...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        ArrayList<File> textFilesList = new ArrayList<>();
        LOG.info("Searching for files, filtering on ASCII/PDF....");
        long startTime = System.nanoTime();
        for (File file : files) {
            //@DEBUB SMP LOG.info(file.getAbsolutePath());
            if (file.isDirectory()) {
                textFilesList.addAll(
                    Files.walk(file.toPath())
                        .map(Path::toFile)
                        .filter(f -> ResourceUtils.isTextFile(f) || ResourceUtils.isPDF(file))
                        .toList());
            } else {
                if (ResourceUtils.isTextFile(file) || ResourceUtils.isPDF(file))
                    textFilesList.add(file);
            }
        }
        Utils.logTotalTime(startTime);
        final double total = textFilesList.size();
        LOG.info("Loading textfiles into listitems....");
        startTime = System.nanoTime();
        List<EmbeddingsTextListItem> newItems =
            textFilesList.parallelStream()
                .map(EmbeddingsTextListItem.itemsSplitFromFile)
                .flatMap(List::stream)
                .peek(i -> {
                    double completed = atomicCount.incrementAndGet();
                    if(null != progressIndicator) {
                        progressIndicator.setLabelLater(completed + " of " + total);
                    }
                }).toList();
        Utils.logTotalTime(startTime);

        LOG.info("loaded {} ASCII/PDF files.", String.valueOf(textFilesList.size()));
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new HyperdriveEvent(HyperdriveEvent.NEW_BATCH_TEXTLOAD, newItems, textFilesList));
        });
        if(null != progressIndicator) {
            progressIndicator.setLabelLater("Complete");
            progressIndicator.spin(false);
            progressIndicator.fadeBusy(true);
        }
    }
}
