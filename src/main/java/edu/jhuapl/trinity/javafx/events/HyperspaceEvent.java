/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class HyperspaceEvent extends Event {

    public Object object;

    public static final EventType<HyperspaceEvent> SCALING_AUTO_NORMALIZE = new EventType(ANY, "SCALING_AUTO_NORMALIZE");
    public static final EventType<HyperspaceEvent> SCALING_MANUAL_BOUNDS = new EventType(ANY, "SCALING_MANUAL_BOUNDS");
    public static final EventType<HyperspaceEvent> SCALING_MEAN_CENTERED = new EventType(ANY, "SCALING_MEAN_CENTERED");
    public static final EventType<HyperspaceEvent> NEW_MAX_ABS = new EventType(ANY, "NEW_MAX_ABS");
    public static final EventType<HyperspaceEvent> NEW_MEANCENTEREDMAX_ABS = new EventType(ANY, "NEW_MEANCENTEREDMAX_ABS");
    public static final EventType<HyperspaceEvent> RESET_MAX_ABS = new EventType(ANY, "RESET_MAX_ABS");
    public static final EventType<HyperspaceEvent> RECOMPUTE_MAX_ABS = new EventType(ANY, "RECOMPUTE_MAX_ABS");
    public static final EventType<HyperspaceEvent> REMOVED_FACTOR_LABEL = new EventType(ANY, "REMOVED_FACTOR_LABEL");
    public static final EventType<HyperspaceEvent> CLEARED_FACTOR_LABELS = new EventType(ANY, "CLEARED_FACTOR_LABELS");
    public static final EventType<HyperspaceEvent> ADDED_FACTOR_LABEL = new EventType(ANY, "ADDED_FACTOR_LABEL");
    public static final EventType<HyperspaceEvent> ADDEDALL_FACTOR_LABELS = new EventType(ANY, "ADDEDALL_FACTOR_LABEL");
    public static final EventType<HyperspaceEvent> UPDATED_FACTOR_LABEL = new EventType(ANY, "UPDATED_FACTOR_LABEL");
    public static final EventType<HyperspaceEvent> UPDATEDALL_FACTOR_LABELS = new EventType(ANY, "UPDATEDALL_FACTOR_LABELS");
    public static final EventType<HyperspaceEvent> FACTOR_COORDINATES_KEYPRESS = new EventType(ANY, "FACTOR_COORDINATES_KEYPRESS");
    public static final EventType<HyperspaceEvent> DIMENSION_LABELS_SET = new EventType(ANY, "DIMENSION_LABELS_SET");
    public static final EventType<HyperspaceEvent> DIMENSION_LABEL_REMOVED = new EventType(ANY, "DIMENSION_LABEL_REMOVED");
    public static final EventType<HyperspaceEvent> CLEARED_DIMENSION_LABELS = new EventType(ANY, "CLEARED_DIMENSION_LABELS");
    public static final EventType<HyperspaceEvent> DIMENSION_LABEL_UPDATE = new EventType(ANY, "DIMENSION_LABEL_UPDATE");
    public static final EventType<HyperspaceEvent> ENABLE_DIRECTION_COORDINATES = new EventType(ANY, "ENABLE_DIRECTION_COORDINATES");
    public static final EventType<HyperspaceEvent> FACTOR_COORDINATES_GUI = new EventType(ANY, "FACTOR_COORDINATES_GUI");
    public static final EventType<HyperspaceEvent> FACTOR_VECTORMAX_KEYPRESS = new EventType(ANY, "FACTOR_VECTORMAX_KEYPRESS");
    public static final EventType<HyperspaceEvent> FACTOR_VECTORMAX_GUI = new EventType(ANY, "FACTOR_VECTORMAX_GUI");
    public static final EventType<HyperspaceEvent> NODE_QUEUELIMIT_KEYPRESS = new EventType(ANY, "NODE_QUEUELIMIT_KEYPRESS");
    public static final EventType<HyperspaceEvent> NODE_QUEUELIMIT_GUI = new EventType(ANY, "NODE_QUEUELIMIT_GUI");
    public static final EventType<HyperspaceEvent> REFRESH_RATE_KEYPRESS = new EventType(ANY, "REFRESH_RATE_KEYPRESS");
    public static final EventType<HyperspaceEvent> REFRESH_RATE_GUI = new EventType(ANY, "REFRESH_RATE_GUI");
    public static final EventType<HyperspaceEvent> POINT3D_SIZE_KEYPRESS = new EventType(ANY, "POINT3D_SIZE_KEYPRESS");
    public static final EventType<HyperspaceEvent> POINT3D_SIZE_GUI = new EventType(ANY, "POINT3D_SIZE_GUI");
    public static final EventType<HyperspaceEvent> POINT_SCALE_KEYPRESS = new EventType(ANY, "POINT_SCALE_KEYPRESS");
    public static final EventType<HyperspaceEvent> POINT_SCALE_GUI = new EventType(ANY, "POINT_SCALE_GUI");
    public static final EventType<HyperspaceEvent> SCATTERBUFF_SCALING_KEYPRESS = new EventType(ANY, "SCATTERBUFF_SCALING_KEYPRESS");
    public static final EventType<HyperspaceEvent> SCATTERBUFF_SCALING_GUI = new EventType(ANY, "SCATTERBUFF_SCALING_GUI");
    public static final EventType<HyperspaceEvent> HYPERSPACE_BACKGROUND_COLOR = new EventType(ANY, "HYPERSPACE_BACKGROUND_COLOR");
    public static final EventType<HyperspaceEvent> ENABLE_HYPERSPACE_SKYBOX = new EventType(ANY, "ENABLE_HYPERSPACE_SKYBOX");
    public static final EventType<HyperspaceEvent> ENABLE_FEATURE_DATA = new EventType(ANY, "ENABLE_FEATURE_DATA");
    public static final EventType<HyperspaceEvent> ENABLE_FACTOR_DIRECTION = new EventType(ANY, "ENABLE_FACTOR_DIRECTION");
    public static final EventType<HyperspaceEvent> SHOW_EXTRAS_GROUP = new EventType(ANY, "SHOW_EXTRAS_GROUP");
    public static final EventType<HyperspaceEvent> REMOVED_FEATURE_LAYER = new EventType(ANY, "REMOVED_FEATURE_LAYER");
    public static final EventType<HyperspaceEvent> ADDED_FEATURE_LAYER = new EventType(ANY, "ADDED_FEATURE_LAYER");
    public static final EventType<HyperspaceEvent> ADDEDALL_FEATURE_LAYER = new EventType(ANY, "ADDEDALL_FEATURE_LAYER");
    public static final EventType<HyperspaceEvent> UPDATED_FEATURE_LAYER = new EventType(ANY, "UPDATED_FEATURE_LAYER");
    public static final EventType<HyperspaceEvent> CLEARED_FEATURE_LAYERS = new EventType(ANY, "CLEARED_FEATURE_LAYERS");
    public static final EventType<HyperspaceEvent> CLEAR_HYPERSPACE_NOW = new EventType(ANY, "CLEAR_HYPERSPACE_NOW");
    public static final EventType<HyperspaceEvent> COLOR_BY_LABEL = new EventType(ANY, "COLOR_BY_LABEL");
    public static final EventType<HyperspaceEvent> COLOR_BY_LAYER = new EventType(ANY, "COLOR_BY_LAYER");
    public static final EventType<HyperspaceEvent> COLOR_BY_GRADIENT = new EventType(ANY, "COLOR_BY_GRADIENT");
    public static final EventType<HyperspaceEvent> COLOR_BY_SCORE = new EventType(ANY, "COLOR_BY_SCORE");
    public static final EventType<HyperspaceEvent> COLOR_BY_PFA = new EventType(ANY, "COLOR_BY_PFA");

    public enum COLOR_MODE {
        COLOR_BY_LABEL, COLOR_BY_LAYER, COLOR_BY_GRADIENT,
        COLOR_BY_SCORE, COLOR_BY_PFA
    }

    public HyperspaceEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public HyperspaceEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public HyperspaceEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
