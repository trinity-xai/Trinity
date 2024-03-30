package edu.jhuapl.trinity.utils.marchingcubes;

/*-
 * #%L
 * trinity-2024.01.08
 * %%
 * Copyright (C) 2021 - 2024 The Johns Hopkins University Applied Physics Laboratory LLC
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

import java.util.ArrayList;

/**
 * Created by Primoz on 8.7.2016.
 */
abstract class CallbackMC implements Runnable {
    private ArrayList<float[]> vertices;

    void setVertices(ArrayList<float[]> vertices) {
        this.vertices = vertices;
    }

    ArrayList<float[]> getVertices() {
        return this.vertices;
    }
}
