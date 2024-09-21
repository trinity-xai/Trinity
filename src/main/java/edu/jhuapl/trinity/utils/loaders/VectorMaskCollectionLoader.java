package edu.jhuapl.trinity.utils.loaders;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.jhuapl.trinity.data.files.VectorMaskCollectionFile;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
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
public class VectorMaskCollectionLoader extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(VectorMaskCollectionLoader.class);
    Scene scene;
    File file;

    public VectorMaskCollectionLoader(Scene scene, File file) {
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
            ProgressStatus ps = new ProgressStatus("Deserializing Vector Mask Collection File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        try {
            VectorMaskCollectionFile scFile = new VectorMaskCollectionFile(file.getAbsolutePath(), true);
            Platform.runLater(() -> scene.getRoot().fireEvent(
                new ImageEvent(ImageEvent.NEW_VECTORMASK_COLLECTION, scFile.vectorMaskCollection)));
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
        return null;
    }
}
