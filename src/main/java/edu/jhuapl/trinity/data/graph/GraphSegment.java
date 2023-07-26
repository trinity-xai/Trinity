package edu.jhuapl.trinity.data.graph;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphSegment {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /**
     * {
     * "start": {
     * "identity": 12525037,
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "7630a867296188c0566d0a73",
     * "numberHashes": 276
     * }
     * },
     * "relationship": {
     * "identity": 32684,
     * "start": 12525037,
     * "end": 12487335,
     * "type": "SIMILAR",
     * "properties": {
     * "jaccard": 1.0
     * }
     * },
     * "end": {
     * "identity": 12487335,
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "c735cd9a7620c80fc53de855",
     * "numberHashes": 276
     * }
     * }
     * }
     */
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private GraphNode start;
    private GraphRelationship relationship;
    private GraphNode end;
    //</editor-fold>
}
