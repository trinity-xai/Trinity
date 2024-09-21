package edu.jhuapl.trinity;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
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

import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists because of the current state of JavaFX in Java11+.
 * Please see https://stackoverflow.com/questions/52569724/javafx-11-create-a-jar-file-with-gradle/52571719#52571719
 * for solution origin.
 */
public class TrinityMain {
    private static final Logger LOG = LoggerFactory.getLogger(TrinityMain.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LOG.info("Entering Trinity main...");
        Application.launch(App.class, args);
    }
}
