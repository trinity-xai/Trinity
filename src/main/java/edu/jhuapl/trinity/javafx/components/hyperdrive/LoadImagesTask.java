package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.javafx.events.HyperdriveEvent;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem.itemFromFile;
import static edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsImageListItem.itemNoRenderFromFile;


/**
 * @author Sean Phillips
 */
public class LoadImagesTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(LoadImagesTask.class);
    boolean renderIcons;
    List<File> files;

    public LoadImagesTask(Scene scene, CircleProgressIndicator progressIndicator,
                          boolean renderIcons, List<File> files) {
        super(scene, progressIndicator, new AtomicInteger(), null);
        this.renderIcons = renderIcons;
        this.files = files;
    }

    @Override
    protected void processTask() throws Exception {
        AtomicInteger atomicCount = new AtomicInteger(0);
        if (null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Loading " + atomicCount.toString() + " images...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        ArrayList<File> imageFilesList = new ArrayList<>();
        LOG.info("Searching for files, filtering images....");
        long startTime = System.nanoTime();
        for (File file : files) {
            LOG.info(file.getAbsolutePath());
            if (file.isDirectory()) {
                imageFilesList.addAll(
                    Files.walk(file.toPath())
                        .map(Path::toFile)
                        .filter(f -> JavaFX3DUtils.isTextureFile(f))
                        .toList());
            } else {
                if (JavaFX3DUtils.isTextureFile(file))
                    imageFilesList.add(file);
            }
        }
        imageFilesList.removeIf(f -> !JavaFX3DUtils.isTextureFile(f));
        Utils.logTotalTime(startTime);
        final double total = imageFilesList.size();
        LOG.info("Loading images into listitems....");
        startTime = System.nanoTime();
        List<EmbeddingsImageListItem> newItems =
            imageFilesList.parallelStream()
                .map(renderIcons ? itemFromFile : itemNoRenderFromFile)
                .peek(i -> {
                    double completed = atomicCount.incrementAndGet();
                    if (null != progressIndicator) {
                        progressIndicator.setPercentComplete(completed / total);
                        progressIndicator.setLabelLater(completed + " of " + total);
                    }
                }).toList();
        Utils.logTotalTime(startTime);

        LOG.info("Populating ListView....");
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new HyperdriveEvent(HyperdriveEvent.NEW_BATCH_IMAGELOAD,
                    newItems, imageFilesList));
        });
        if (null != progressIndicator) {
            progressIndicator.setLabelLater("Complete");
            progressIndicator.spin(false);
            progressIndicator.fadeBusy(true);
        }
    }
}
