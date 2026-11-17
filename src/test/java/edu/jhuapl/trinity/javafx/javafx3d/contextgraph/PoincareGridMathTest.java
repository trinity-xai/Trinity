package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** No JUnit dependency; run with java -ea. */
public final class PoincareGridMathTest {
    public static void main(String[] args) {
        double lastR = -1.0;
        double lastGap = Double.POSITIVE_INFINITY;
        for (int i = 0; i <= 24; i++) {
            double d = i / 4.0;
            double r = PoincareGridMath.euclideanFraction(d);
            assert r >= lastR : "r should increase with hyperbolic distance";
            assert r >= 0.0 && r < 1.0 : "radius remains inside disk";
            assert Math.abs(PoincareGridMath.hyperbolicDistance(r) - d) < 1e-12
                : "inverse round-trip mismatch";
            if (i > 0) {
                double gap = r - lastR;
                assert gap <= lastGap + 1e-12 : "equal hyperbolic steps compress toward boundary";
                lastGap = gap;
            }
            lastR = r;
        }
        assert Math.abs(PoincareGridMath.ringRadius(100.0, 2.0) - 76.15941559557782) < 1e-10;
        for (double bad : new double[] {-0.1, Double.NaN, Double.NEGATIVE_INFINITY}) {
            boolean threw = false;
            try { PoincareGridMath.euclideanFraction(bad); }
            catch (IllegalArgumentException expected) { threw = true; }
            assert threw;
        }
        boolean invalidFraction = false;
        try { PoincareGridMath.hyperbolicDistance(1.0); }
        catch (IllegalArgumentException expected) { invalidFraction = true; }
        assert invalidFraction;
        System.out.println("PoincareGridMathTest PASS");
    }
}
