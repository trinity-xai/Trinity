package edu.jhuapl.trinity.utils.graph;

import java.util.Random;

/**
 * Simple 3D Fruchterman–Reingold force layout.
 * - Uses optional edge weight matrix (symmetric) to scale attraction.
 * - Adds a gravity term to avoid drift.
 * - No JavaFX dependencies.
 *
 * @author Sean Phillips
 */
public final class ForceFrLayout3D implements GraphLayoutEngine {

    @Override
    public double[][] layout(int n,
                             java.util.List<String> labels,
                             double[][] distances,
                             double[][] weights,
                             GraphLayoutParams p) {
        if (n <= 0) return new double[0][0];

        // Positions
        double[][] pos = new double[n][3];
        // Displacements
        double[][] disp = new double[n][3];

        // Init on a sphere (stable start)
        initOnSphere(pos, p.radius);

        final double area = 8.0 * p.radius * p.radius; // not literal area in 3D; used in k
        final double k = Math.cbrt(area / Math.max(1.0, n)); // 3D scale (heuristic)

        double t = p.step; // temperature
        final Random rnd = new Random(1337);

        for (int iter = 0; iter < p.iterations; iter++) {
            // zero displacements
            for (int i = 0; i < n; i++) {
                disp[i][0] = disp[i][1] = disp[i][2] = 0.0;
            }

            // Repulsive forces (all pairs)
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dx = pos[i][0] - pos[j][0];
                    double dy = pos[i][1] - pos[j][1];
                    double dz = pos[i][2] - pos[j][2];
                    double d2 = dx*dx + dy*dy + dz*dz + 1e-9;
                    double d = Math.sqrt(d2);
                    // Fr repulsion ~ (k^2 / d)
                    double f = (p.repulsion * k * k) / d;
                    double fx = f * dx / d;
                    double fy = f * dy / d;
                    double fz = f * dz / d;

                    disp[i][0] += fx; disp[i][1] += fy; disp[i][2] += fz;
                    disp[j][0] -= fx; disp[j][1] -= fy; disp[j][2] -= fz;
                }
            }

            // Attractive forces (edges)
            if (weights != null) {
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        double w = weights[i][j];
                        if (w <= 0.0) continue;
                        double dx = pos[i][0] - pos[j][0];
                        double dy = pos[i][1] - pos[j][1];
                        double dz = pos[i][2] - pos[j][2];
                        double d2 = dx*dx + dy*dy + dz*dz + 1e-9;
                        double d = Math.sqrt(d2);
                        // Fr attraction ~ (d^2 / k)
                        double f = p.attraction * w * (d2 / k);
                        double fx = f * dx / d;
                        double fy = f * dy / d;
                        double fz = f * dz / d;

                        disp[i][0] -= fx; disp[i][1] -= fy; disp[i][2] -= fz;
                        disp[j][0] += fx; disp[j][1] += fy; disp[j][2] += fz;
                    }
                }
            }

            // Gravity (pull to origin)
            if (p.gravity > 0) {
                for (int i = 0; i < n; i++) {
                    double x = pos[i][0], y = pos[i][1], z = pos[i][2];
                    disp[i][0] += -p.gravity * x;
                    disp[i][1] += -p.gravity * y;
                    disp[i][2] += -p.gravity * z;
                }
            }

            // Move with clipping to temperature t
            for (int i = 0; i < n; i++) {
                double dx = disp[i][0], dy = disp[i][1], dz = disp[i][2];
                double m = Math.sqrt(dx*dx + dy*dy + dz*dz) + 1e-9;
                double lim = t / m;
                pos[i][0] += dx * Math.min(1.0, lim);
                pos[i][1] += dy * Math.min(1.0, lim);
                pos[i][2] += dz * Math.min(1.0, lim);
            }

            // Cool
            t *= p.cooling;
            if (t < 1e-4) t = 1e-4;

            // Tiny jitter to escape symmetry
            if ((iter % 50) == 0) {
                for (int i = 0; i < n; i++) {
                    pos[i][0] += (rnd.nextDouble() - 0.5) * 0.01;
                    pos[i][1] += (rnd.nextDouble() - 0.5) * 0.01;
                    pos[i][2] += (rnd.nextDouble() - 0.5) * 0.01;
                }
            }
        }
        return pos;
    }

    private static void initOnSphere(double[][] pos, double r) {
        final int n = pos.length;
        // Fibonacci sphere distribution (nice uniform spread)
        final double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < n; i++) {
            double y = 1.0 - (i / (double)(n - 1)) * 2.0;
            double radius = Math.sqrt(1.0 - y*y);
            double theta = phi * i;
            double x = Math.cos(theta) * radius;
            double z = Math.sin(theta) * radius;
            pos[i][0] = r * x;
            pos[i][1] = r * y;
            pos[i][2] = r * z;
        }
    }
}
