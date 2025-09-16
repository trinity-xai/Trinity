package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Hypersurface-specific UI + render events.
 */
public class HypersurfaceEvent extends Event {

    public Object object; // loose payload, mirrors HyperspaceEvent pattern

    public static final EventType<HypersurfaceEvent> ANY =
            new EventType<>(Event.ANY, "HYPERSURFACE_ANY");

    // --- Core geometry & scaling ---
    public static final EventType<HypersurfaceEvent> Y_SCALE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_Y_SCALE_CHANGED");            // Double
    public static final EventType<HypersurfaceEvent> SURF_SCALE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SURF_SCALE_CHANGED");         // Double
    public static final EventType<HypersurfaceEvent> XWIDTH_CHANGED =
            new EventType<>(ANY, "HYPERSURF_XWIDTH_CHANGED");             // Integer
    public static final EventType<HypersurfaceEvent> ZWIDTH_CHANGED =
            new EventType<>(ANY, "HYPERSURF_ZWIDTH_CHANGED");             // Integer

    // --- Rendering mode & draw settings ---
    public static final EventType<HypersurfaceEvent> SURFACE_RENDER_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SURFACE_RENDER_CHANGED");     // Boolean
    public static final EventType<HypersurfaceEvent> DRAW_MODE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_DRAW_MODE_CHANGED");          // DrawMode
    public static final EventType<HypersurfaceEvent> CULL_FACE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_CULL_FACE_CHANGED");          // CullFace

    // --- Color/material mapping ---
    public static final EventType<HypersurfaceEvent> COLORATION_CHANGED =
            new EventType<>(ANY, "HYPERSURF_COLORATION_CHANGED");         // Hypersurface3DPane.COLORATION

    // --- Interpolation (vertex lookup) ---
    public static final EventType<HypersurfaceEvent> INTERP_MODE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_INTERP_MODE_CHANGED");        // SurfaceUtils.Interpolation

    // --- Processing pipeline: smoothing ---
    public static final EventType<HypersurfaceEvent> SMOOTHING_ENABLE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SMOOTHING_ENABLE_CHANGED");   // Boolean
    public static final EventType<HypersurfaceEvent> SMOOTHING_METHOD_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SMOOTHING_METHOD_CHANGED");   // SurfaceUtils.Smoothing
    public static final EventType<HypersurfaceEvent> SMOOTHING_RADIUS_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SMOOTHING_RADIUS_CHANGED");   // Integer
    public static final EventType<HypersurfaceEvent> GAUSSIAN_SIGMA_CHANGED =
            new EventType<>(ANY, "HYPERSURF_GAUSSIAN_SIGMA_CHANGED");     // Double

    // --- Processing pipeline: tone mapping ---
    public static final EventType<HypersurfaceEvent> TONEMAP_ENABLE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_TONEMAP_ENABLE_CHANGED");     // Boolean
    public static final EventType<HypersurfaceEvent> TONEMAP_OPERATOR_CHANGED =
            new EventType<>(ANY, "HYPERSURF_TONEMAP_OPERATOR_CHANGED");   // SurfaceUtils.ToneMap
    public static final EventType<HypersurfaceEvent> TONEMAP_PARAM_CHANGED =
            new EventType<>(ANY, "HYPERSURF_TONEMAP_PARAM_CHANGED");      // Double

    // --- Height normalization mode ---
    public static final EventType<HypersurfaceEvent> HEIGHT_MODE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_HEIGHT_MODE_CHANGED");        // DataUtils.HeightMode

    // --- Lighting ---
    public static final EventType<HypersurfaceEvent> AMBIENT_ENABLED_CHANGED =
            new EventType<>(ANY, "HYPERSURF_AMBIENT_ENABLED_CHANGED");    // Boolean
    public static final EventType<HypersurfaceEvent> AMBIENT_COLOR_CHANGED =
            new EventType<>(ANY, "HYPERSURF_AMBIENT_COLOR_CHANGED");      // Color
    public static final EventType<HypersurfaceEvent> POINT_ENABLED_CHANGED =
            new EventType<>(ANY, "HYPERSURF_POINT_ENABLED_CHANGED");      // Boolean
    public static final EventType<HypersurfaceEvent> SPECULAR_COLOR_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SPECULAR_COLOR_CHANGED");     // Color

    // --- UX / overlay toggles ---
    public static final EventType<HypersurfaceEvent> HOVER_ENABLE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_HOVER_ENABLE_CHANGED");       // Boolean
    public static final EventType<HypersurfaceEvent> SURFACE_CHARTS_ENABLE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_SURFACE_CHARTS_ENABLE_CHANGED"); // Boolean
    public static final EventType<HypersurfaceEvent> DATA_MARKERS_ENABLE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_DATA_MARKERS_ENABLE_CHANGED");   // Boolean
    public static final EventType<HypersurfaceEvent> CROSSHAIRS_ENABLE_CHANGED =
            new EventType<>(ANY, "HYPERSURF_CROSSHAIRS_ENABLE_CHANGED");     // Boolean

    // --- Commands / actions ---
    public static final EventType<HypersurfaceEvent> RESET_VIEW =
            new EventType<>(ANY, "HYPERSURF_RESET_VIEW");                 // no payload
    public static final EventType<HypersurfaceEvent> UPDATE_RENDER =
            new EventType<>(ANY, "HYPERSURF_UPDATE_RENDER");              // no payload
    public static final EventType<HypersurfaceEvent> CLEAR_DATA =
            new EventType<>(ANY, "HYPERSURF_CLEAR_DATA");                 // no payload
    public static final EventType<HypersurfaceEvent> UNROLL_REQUESTED =
            new EventType<>(ANY, "HYPERSURF_UNROLL_REQUESTED");           // no payload
    public static final EventType<HypersurfaceEvent> COMPUTE_VECTOR_DISTANCES =
            new EventType<>(ANY, "HYPERSURF_COMPUTE_VECTOR_DISTANCES");   // no payload
    public static final EventType<HypersurfaceEvent> COMPUTE_COLLECTION_DIFF =
            new EventType<>(ANY, "HYPERSURF_COMPUTE_COLLECTION_DIFF");    // FeatureCollection
    public static final EventType<HypersurfaceEvent> COMPUTE_COSINE_DISTANCE =
            new EventType<>(ANY, "HYPERSURF_COMPUTE_COSINE_DISTANCE");    // FeatureCollection

    // --- Constructors (matching your style) ---
    public HypersurfaceEvent(EventType<? extends Event> type) { super(type); }

    public HypersurfaceEvent(EventType<? extends Event> type, Object payload) {
        this(type);
        this.object = payload;
    }

    public HypersurfaceEvent(Object source, EventTarget target, EventType<? extends Event> type) {
        super(source, target, type);
        this.object = source;
    }

    // --- Convenience factories (optional) ---
    public static HypersurfaceEvent of(EventType<HypersurfaceEvent> type) { return new HypersurfaceEvent(type); }
    public static HypersurfaceEvent of(EventType<HypersurfaceEvent> type, Object payload) { return new HypersurfaceEvent(type, payload); }

    // Examples:
    public static HypersurfaceEvent yScale(double v) { return of(Y_SCALE_CHANGED, v); }
    public static HypersurfaceEvent surfScale(double v) { return of(SURF_SCALE_CHANGED, v); }
    public static HypersurfaceEvent xWidth(int v) { return of(XWIDTH_CHANGED, v); }
    public static HypersurfaceEvent zWidth(int v) { return of(ZWIDTH_CHANGED, v); }
    public static HypersurfaceEvent surfaceRender(boolean surface) { return of(SURFACE_RENDER_CHANGED, surface); }
    public static HypersurfaceEvent drawMode(Object drawMode) { return of(DRAW_MODE_CHANGED, drawMode); }
    public static HypersurfaceEvent cullFace(Object cullFace) { return of(CULL_FACE_CHANGED, cullFace); }
    public static HypersurfaceEvent coloration(Object colorationEnum) { return of(COLORATION_CHANGED, colorationEnum); }
    public static HypersurfaceEvent interp(Object interpEnum) { return of(INTERP_MODE_CHANGED, interpEnum); }
    public static HypersurfaceEvent smoothingEnabled(boolean b) { return of(SMOOTHING_ENABLE_CHANGED, b); }
    public static HypersurfaceEvent smoothingMethod(Object methodEnum) { return of(SMOOTHING_METHOD_CHANGED, methodEnum); }
    public static HypersurfaceEvent smoothingRadius(int r) { return of(SMOOTHING_RADIUS_CHANGED, r); }
    public static HypersurfaceEvent gaussianSigma(double s) { return of(GAUSSIAN_SIGMA_CHANGED, s); }
    public static HypersurfaceEvent toneEnabled(boolean b) { return of(TONEMAP_ENABLE_CHANGED, b); }
    public static HypersurfaceEvent toneOperator(Object opEnum) { return of(TONEMAP_OPERATOR_CHANGED, opEnum); }
    public static HypersurfaceEvent toneParam(double v) { return of(TONEMAP_PARAM_CHANGED, v); }
    public static HypersurfaceEvent heightMode(Object modeEnum) { return of(HEIGHT_MODE_CHANGED, modeEnum); }
    public static HypersurfaceEvent ambientEnabled(boolean b) { return of(AMBIENT_ENABLED_CHANGED, b); }
    public static HypersurfaceEvent ambientColor(Object color) { return of(AMBIENT_COLOR_CHANGED, color); }
    public static HypersurfaceEvent pointEnabled(boolean b) { return of(POINT_ENABLED_CHANGED, b); }
    public static HypersurfaceEvent specularColor(Object color) { return of(SPECULAR_COLOR_CHANGED, color); }
    public static HypersurfaceEvent hoverEnabled(boolean b) { return of(HOVER_ENABLE_CHANGED, b); }
    public static HypersurfaceEvent surfaceChartsEnabled(boolean b) { return of(SURFACE_CHARTS_ENABLE_CHANGED, b); }
    public static HypersurfaceEvent dataMarkersEnabled(boolean b) { return of(DATA_MARKERS_ENABLE_CHANGED, b); }
    public static HypersurfaceEvent crosshairsEnabled(boolean b) { return of(CROSSHAIRS_ENABLE_CHANGED, b); }
    public static HypersurfaceEvent resetView() { return of(RESET_VIEW); }
    public static HypersurfaceEvent updateRender() { return of(UPDATE_RENDER); }
    public static HypersurfaceEvent clearData() { return of(CLEAR_DATA); }
    public static HypersurfaceEvent unroll() { return of(UNROLL_REQUESTED); }
    public static HypersurfaceEvent computeVectorDistances() { return of(COMPUTE_VECTOR_DISTANCES); }
    public static HypersurfaceEvent computeCollectionDiff(Object featureCollection) { return of(COMPUTE_COLLECTION_DIFF, featureCollection); }
    public static HypersurfaceEvent computeCosineDistance(Object featureCollection) { return of(COMPUTE_COSINE_DISTANCE, featureCollection); }
}
