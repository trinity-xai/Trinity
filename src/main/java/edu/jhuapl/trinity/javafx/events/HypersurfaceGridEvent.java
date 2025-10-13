package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

import java.util.List;

/**
 * Dispatch a surface grid (PDF or CDF) to the hypersurface renderer.
 * <p>
 * Z is row-major by Y then X (double[rowsY][colsX]).
 * If your renderer expects Y-up, flip rows before rendering.
 *
 * @author Sean Phillips
 */
public class HypersurfaceGridEvent extends Event {
    public static final EventType<HypersurfaceGridEvent> ANY =
        new EventType<>(Event.ANY, "HYPERSURFACE_GRID_ANY");

    public static final EventType<HypersurfaceGridEvent> RENDER_PDF =
        new EventType<>(ANY, "HYPERSURFACE_GRID_RENDER_PDF");

    public static final EventType<HypersurfaceGridEvent> RENDER_CDF =
        new EventType<>(ANY, "HYPERSURFACE_GRID_RENDER_CDF");

    private final List<List<Double>> zGrid;  // or keep double[][] if preferred
    private final double[] xCenters;
    private final double[] yCenters;
    private final String label;

    public HypersurfaceGridEvent(
        EventType<? extends Event> eventType,
        List<List<Double>> zGrid,
        double[] xCenters,
        double[] yCenters,
        String label
    ) {
        super(eventType);
        this.zGrid = zGrid;
        this.xCenters = xCenters;
        this.yCenters = yCenters;
        this.label = label;
    }

    public HypersurfaceGridEvent(
        Object source,
        EventTarget target,
        EventType<? extends Event> eventType,
        List<List<Double>> zGrid,
        double[] xCenters,
        double[] yCenters,
        String label
    ) {
        super(source, target, eventType);
        this.zGrid = zGrid;
        this.xCenters = xCenters;
        this.yCenters = yCenters;
        this.label = label;
    }

    public List<List<Double>> getZGrid() {
        return zGrid;
    }

    public double[] getXCenters() {
        return xCenters;
    }

    public double[] getYCenters() {
        return yCenters;
    }

    public String getLabel() {
        return label;
    }
}
