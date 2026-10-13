package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

/**
 * One requested tile LOD transition, ordered by rendering priority.
 *
 * <p>Lower {@code priorityClass} values are applied first. Within the same
 * class, tiles nearer the viewport center are preferred, followed by nearer
 * tiles.</p>
 */
public record LodTransition(
    int tileId,
    int fromLod,
    int toLod,
    int priorityClass,
    double screenDistance,
    double depth,
    double pixelsPerCell
) implements Comparable<LodTransition> {

    public boolean isInitial() {
        return fromLod < 0;
    }

    public boolean isCoarsening() {
        return fromLod >= 0 && toLod > fromLod;
    }

    public boolean isRefining() {
        return fromLod >= 0 && toLod < fromLod;
    }

    @Override
    public int compareTo(LodTransition other) {
        int c = Integer.compare(priorityClass, other.priorityClass);
        if (c != 0) return c;
        c = Double.compare(screenDistance, other.screenDistance);
        if (c != 0) return c;
        c = Double.compare(depth, other.depth);
        if (c != 0) return c;
        return Integer.compare(tileId, other.tileId);
    }
}
