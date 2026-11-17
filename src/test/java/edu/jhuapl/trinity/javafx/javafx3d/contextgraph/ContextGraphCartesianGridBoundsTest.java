package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Standalone test runnable without JavaFX dependencies. */
public final class ContextGraphCartesianGridBoundsTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var empty = ContextGraphCartesianGridBounds.empty();
        check(empty.width() == 200 && empty.length() == 200, "startup grid");
        var b = ContextGraphCartesianGridBounds.fromExtent(-100, 100, -30, 30, 40, 140);
        check(b.centerX() == 0 && b.centerZ() == 90, "grid centered on XZ bounds");
        check(b.width() >= 200 && b.length() >= 100, "grid spans node bounds");
        check(b.floorY() > 30, "floor below JavaFX node Y maximum");
        var line = ContextGraphCartesianGridBounds.fromExtent(1000, 1000, 1, 1, -500, 500);
        check(line.width() >= 20 && line.length() >= 1000, "degenerate axis remains visible");
        var one = ContextGraphCartesianGridBounds.fromExtent(2, 2, 3, 3, 4, 4);
        check(one.width() >= 20 && one.length() >= 20, "single entity grid");
        var invalid = ContextGraphCartesianGridBounds.fromExtent(Double.NaN, 1, 0, 0, 0, 0);
        check(invalid.equals(empty), "invalid bounds fall back to startup grid");
        System.out.println("ContextGraphCartesianGridBoundsTest PASS");
    }
}
