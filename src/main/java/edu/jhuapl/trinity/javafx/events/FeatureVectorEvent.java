package edu.jhuapl.trinity.javafx.events;

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

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class FeatureVectorEvent extends Event {

    public Object object;
    public Object object2;

    public static final EventType<FeatureVectorEvent> PROJECT_SURFACE_GRID = new EventType(ANY, "PROJECT_SURFACE_GRID");
    public static final EventType<FeatureVectorEvent> PROJECT_FEATURE_COLLECTION = new EventType(ANY, "PROJECT_FEATURE_COLLECTION");
    public static final EventType<FeatureVectorEvent> NEW_SURFACE_COLLECTION = new EventType(ANY, "NEW_SURFACE_COLLECTION");
    public static final EventType<FeatureVectorEvent> REQUEST_FEATURE_COLLECTION = new EventType(ANY, "REQUEST_FEATURE_COLLECTION");
    public static final EventType<FeatureVectorEvent> EXPORT_FEATURE_COLLECTION = new EventType(ANY, "EXPORT_FEATURE_COLLECTION");
    public static final EventType<FeatureVectorEvent> NEW_FEATURE_COLLECTION = new EventType(ANY, "NEW_FEATURE_COLLECTION");
    public static final EventType<FeatureVectorEvent> NEW_FEATURE_VECTOR = new EventType(ANY, "NEW_FEATURE_VECTOR");
    public static final EventType<FeatureVectorEvent> LOCATE_FEATURE_VECTOR = new EventType(ANY, "LOCATE_FEATURE_VECTOR");
    public static final EventType<FeatureVectorEvent> SELECT_FEATURE_VECTOR = new EventType(ANY, "SELECT_FEATURE_VECTOR");
    public static final EventType<FeatureVectorEvent> RESCAN_FEATURE_LAYERS = new EventType(ANY, "RESCAN_FEATURE_LAYERS");
    public static final EventType<FeatureVectorEvent> RESCAN_FACTOR_LABELS = new EventType(ANY, "RESCAN_FACTOR_LABELS");
    public static final EventType<FeatureVectorEvent> NEW_LABEL_CONFIG = new EventType(ANY, "NEW_LABEL_CONFIG");

    public FeatureVectorEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public FeatureVectorEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public FeatureVectorEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object = arg1;
        object2 = arg2;
    }

    public FeatureVectorEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
