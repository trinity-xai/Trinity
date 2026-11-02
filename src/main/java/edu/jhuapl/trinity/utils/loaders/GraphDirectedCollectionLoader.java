package edu.jhuapl.trinity.utils.loaders;

import edu.jhuapl.trinity.data.files.GraphDirectedCollectionFile;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Sean Phillips
 */
public class GraphDirectedCollectionLoader extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(GraphDirectedCollectionLoader.class);
    Scene scene;
    File file;

    public GraphDirectedCollectionLoader(Scene scene, File file) {
        this.scene = scene;
        this.file = file;
        setOnSucceeded(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnFailed(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnCancelled(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
    }

    @Override
    protected Void call() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Deserializing GraphDirectedCollection File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });

        try {
            long parseStart = System.nanoTime();
            GraphDirectedCollectionFile graphFile = new GraphDirectedCollectionFile(file.getAbsolutePath(), true);
            System.out.printf("[CG-TIMING] file parse completed %.3f ms thread=%s%n", (System.nanoTime()-parseStart)/1e6, Thread.currentThread().getName());
            long queuedAt = System.nanoTime();
            Platform.runLater(() -> {
                System.out.printf("[CG-TIMING] graph event FX queue wait %.3f ms%n", (System.nanoTime()-queuedAt)/1e6);
                scene.getRoot().fireEvent(
                    new GraphEvent(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, graphFile.graphDirectedCollection));
            });
        } catch (IOException ex) {
            LOG.error(null, ex);
        }

        return null;
    }
}
