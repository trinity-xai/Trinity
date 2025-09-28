package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean
 */
public class ApplicationEvent extends Event {

    public Object object = null;
    public Object object2 = null;

    public static final EventType<ApplicationEvent> SET_IMAGERY_BASEPATH = new EventType(ANY, "SET_IMAGERY_BASEPATH");
    public static final EventType<ApplicationEvent> SHOW_ABOUT = new EventType(ANY, "SHOW_ABOUT");
    public static final EventType<ApplicationEvent> SHUTDOWN = new EventType(ANY, "SHUTDOWN");
    public static final EventType<ApplicationEvent> RESTORE_PANES = new EventType(ANY, "RESTORE_PANES");
    public static final EventType<ApplicationEvent> SHOW_ANALYSISLOG_PANE = new EventType(ANY, "SHOW_ANALYSISLOG_PANE");
    public static final EventType<ApplicationEvent> SHOW_SPARK_LINES = new EventType(ANY, "SHOW_SPARK_LINES");
    public static final EventType<ApplicationEvent> SHOW_DATA = new EventType(ANY, "SHOW_DATA");
    public static final EventType<ApplicationEvent> SHOW_TEXT_CONSOLE = new EventType(ANY, "SHOW_TEXT_CONSOLE");
    public static final EventType<ApplicationEvent> SHOW_VIDEO_PANE = new EventType(ANY, "SHOW_VIDEO_PANE");
    public static final EventType<ApplicationEvent> SHOW_JUKEBOX_PANE = new EventType(ANY, "SHOW_JUKEBOX_PANE");
    public static final EventType<ApplicationEvent> SHOW_NAVIGATOR_PANE = new EventType(ANY, "SHOW_NAVIGATOR_PANE");
    public static final EventType<ApplicationEvent> SHOW_COCOVIEWER_PANE = new EventType(ANY, "SHOW_COCOVIEWER_PANE");
    public static final EventType<ApplicationEvent> SHOW_WAVEFORM_PANE = new EventType(ANY, "SHOW_WAVEFORM_PANE");
    public static final EventType<ApplicationEvent> SHOW_HYPERSPACE = new EventType(ANY, "SHOW_HYPERSPACE");
    public static final EventType<ApplicationEvent> SHOW_HYPERSURFACE = new EventType(ANY, "SHOW_HYPERSURFACE");
    public static final EventType<ApplicationEvent> SHOW_HYPERDRIVE_PANE = new EventType(ANY, "SHOW_HYPERDRIVE_PANE");
    public static final EventType<ApplicationEvent> SHOW_PROJECTIONS = new EventType(ANY, "SHOW_PROJECTIONS");
    public static final EventType<ApplicationEvent> SHOW_PROJECTOR_PANE = new EventType(ANY, "SHOW_PROJECTOR_PANE");
    public static final EventType<ApplicationEvent> SHOW_BUSY_INDICATOR = new EventType(ANY, "SHOW_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> HIDE_BUSY_INDICATOR = new EventType(ANY, "HIDE_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> UPDATE_BUSY_INDICATOR = new EventType(ANY, "PROGRESS_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> SHOW_SHAPE3D_CONTROLS = new EventType(ANY, "SHOW_SHAPE3D_CONTROLS");
    public static final EventType<ApplicationEvent> AUTO_PROJECTION_MODE = new EventType(ANY, "AUTO_PROJECTION_MODE");
    public static final EventType<ApplicationEvent> CAMERA_ORBIT_MODE = new EventType(ANY, "CAMERA_ORBIT_MODE");
    public static final EventType<ApplicationEvent> SHOW_JOYSTICK_CONTROLS = new EventType(ANY, "SHOW_JOYSTICK_CONTROLS");
    public static final EventType<ApplicationEvent> BACK_TO_WORK = new EventType(ANY, "BACK_TO_WORK");
    public static final EventType<ApplicationEvent> FPS_CAMERA_MODE = new EventType(ANY, "FPS_CAMERA_MODE");
    public static final EventType<ApplicationEvent> FREE_CAMERA_MODE = new EventType(ANY, "FREE_CAMERA_MODE");
    public static final EventType<ApplicationEvent> SHOULDER_CAMERA_MODE = new EventType(ANY, "SHOULDER_CAMERA_MODE");
    public static final EventType<ApplicationEvent> SHOW_PIXEL_SELECTION = new EventType(ANY, "SHOW_PIXEL_SELECTION");
    public static final EventType<ApplicationEvent> SHOW_IMAGE_INSPECTION = new EventType(ANY, "SHOW_IMAGE_INSPECTION");
    public static final EventType<ApplicationEvent> SHOW_SPECIALEFFECTS_PANE = new EventType(ANY, "SHOW_SPECIALEFFECTS_PANE");
    public static final EventType<ApplicationEvent> SHOW_STATISTICS_PANE = new EventType(ANY, "SHOW_STATISTICS_PANE");
    public static final EventType<ApplicationEvent> SHOW_FEATUREVECTOR_MANAGER = new EventType(ANY, "SHOW_FEATUREVECTOR_MANAGER");
    public static final EventType<ApplicationEvent> POPOUT_FEATUREVECTOR_MANAGER = new EventType(ANY, "POPOUT_FEATUREVECTOR_MANAGER");
    public static final EventType<ApplicationEvent> SHOW_HYPERSPACE_CONTROLS = new EventType(ANY, "SHOW_HYPERSPACE_CONTROLS");
    public static final EventType<ApplicationEvent> SHOW_PAIRWISEJPDF_PANE = new EventType(ANY, "SHOW_PAIRWISEJPDF_PANE");
    public static final EventType<ApplicationEvent> POPOUT_PAIRWISEJPDF_JPDF = new EventType(ANY, "POPOUT_PAIRWISEJPDF_JPDF");
    public static final EventType<ApplicationEvent> POPOUT_MATRIX_HEATMAP = new EventType(ANY, "POPOUT_MATRIX_HEATMAP");
    public static final EventType<ApplicationEvent> SHOW_PAIRWISEMATRIX_PANE = new EventType(ANY, "SHOW_PAIRWISEMATRIX_PANE");

    public ApplicationEvent(EventType<? extends Event> eventType, Object object) {
        this(eventType);
        this.object = object;
    }

    public ApplicationEvent(EventType<? extends Event> eventType, Object object, Object object2) {
        this(eventType);
        this.object = object;
        this.object2 = object2;
    }

    public ApplicationEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
