package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Positions virtual semantic community centers (coarse to fine) and original entities
 * using hyperbolic parent-child steps and hierarchical spherical caps. This is a
 * derived VIEW hierarchy; it never inserts a node or edge into the source graph.
 * The original edge-backed spanning scaffold is retained separately for disclosure.
 */
public final class ContextGraphCommunitySpatialLayout {
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    // Triangle inequality guarantees distance from origin <= sum of local steps;
    // tanh(4.5/2) < 0.979. No destructive boundary-normalization is needed.
    private static final double PATH_DISTANCE_BUDGET = 4.5;

    private ContextGraphCommunitySpatialLayout() { }

    public record Result(ContextGraphHyperbolicLayout.LayoutResult layout,
                         ContextGraphSpatialDiagnostics diagnostics) { }

    public static Result compute(GraphDirectedCollection graph,
                                 ContextGraphCommunityHierarchy hierarchy,
                                 ContextGraphHyperbolicPackingParams params) {
        if (graph == null || hierarchy == null || params == null)
            throw new IllegalArgumentException("Graph, hierarchy, and packing params are required");
        // This is the ORIGINAL edge identity scaffold; its positions are replaced only
        // when the user chooses the multilevel semantic projection.
        boolean classic = params.placement()
            == ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE;
        ContextGraphHyperbolicLayout.LayoutResult scaffold = classic
            ? ContextGraphHyperbolicLayout.originalStructuralLayout(graph)
            : ContextGraphHyperbolicLayout.computedLayout(graph, hierarchy);
        ContextGraphPackedPositions packed = classic ? null : pack(graph, hierarchy, params);
        Map<String, double[]> positions = classic ? scaffold.positions() : packed.positions();
        ContextGraphHyperbolicLayout.LayoutResult layout =
            new ContextGraphHyperbolicLayout.LayoutResult(positions,
                scaffold.backboneEdges(), classic ? scaffold.communities() : hierarchy.layoutMembership(),
                classic ? scaffold.communityCount() : hierarchy.levels().isEmpty() ? 0
                    : hierarchy.levels().get(hierarchy.selectedLayoutLevel()).count());
        int packedLayers = classic ? 0 : hierarchy.levels().size() - hierarchy.selectedLayoutLevel() + 1;
        double factor = budgetFactor(hierarchy, params);
        return new Result(layout, diagnose(positions, hierarchy.entityIds(), packedLayers, factor,
            classic ? Double.NaN : packed.linkAngleBefore(),
            classic ? Double.NaN : packed.linkAngleAfter()));
    }

    private static double budgetFactor(ContextGraphCommunityHierarchy hierarchy,
                                       ContextGraphHyperbolicPackingParams params) {
        if (params.placement() == ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE)
            return 1.0;
        int layers = hierarchy.levels().size() - hierarchy.selectedLayoutLevel() + 1;
        double sum = 0;
        for (int depth = 0; depth < layers; depth++) sum += params.stepAt(depth, layers);
        return sum <= PATH_DISTANCE_BUDGET ? 1.0 : PATH_DISTANCE_BUDGET / sum;
    }

    private static ContextGraphPackedPositions pack(GraphDirectedCollection graph,
                                                    ContextGraphCommunityHierarchy hierarchy,
                                                    ContextGraphHyperbolicPackingParams params) {
        List<ContextGraphCommunityLevel> levels = hierarchy.levels();
        List<String> ids = hierarchy.entityIds();
        if (levels.isEmpty() || ids.isEmpty())
            return new ContextGraphPackedPositions(Map.of(), Double.NaN, Double.NaN);
        int lowest = hierarchy.selectedLayoutLevel();
        int top = levels.size() - 1;
        int layers = top - lowest + 2;
        double factor = budgetFactor(hierarchy, params);
        ContextGraphCommunityConnectivity connectivity = params.placement()
            == ContextGraphHyperbolicPackingParams.Placement.RELATIONSHIP_AWARE
                ? ContextGraphCommunityConnectivity.from(graph, hierarchy) : null;

        double[][][] centers = new double[levels.size()][][];
        double[][][] axes = new double[levels.size()][][];
        for (int l = lowest; l <= top; l++) {
            centers[l] = new double[levels.get(l).count()][3];
            axes[l] = new double[levels.get(l).count()][3];
        }

        List<ContextGraphCommunityCluster> coarse = new ArrayList<>(levels.get(top).clusters());
        coarse.sort(Comparator.comparingInt(ContextGraphCommunityCluster::memberCount).reversed()
            .thenComparingInt(ContextGraphCommunityCluster::id));
        double rootNorm = Math.tanh(0.5 * params.stepAt(0, layers) * factor);
        List<Integer> rootIds = new ArrayList<>(coarse.size());
        double[][] rootDirections = new double[coarse.size()][];
        for (int i = 0; i < coarse.size(); i++) {
            rootIds.add(coarse.get(i).id());
            rootDirections[i] = fibonacciDirection(i, coarse.size());
        }
        double linkAngleBefore = Double.NaN;
        double linkAngleAfter = Double.NaN;
        if (connectivity != null) {
            linkAngleBefore = ContextGraphCommunityConnectivity.weightedAngularDistance(
                rootDirections, rootIds, connectivity.linksAt(top));
            rootDirections = connectivity.arrange(top, rootIds, rootDirections, null, Math.PI);
            linkAngleAfter = ContextGraphCommunityConnectivity.weightedAngularDistance(
                rootDirections, rootIds, connectivity.linksAt(top));
        }
        for (int i = 0; i < coarse.size(); i++) {
            checkCancelled();
            int id = coarse.get(i).id();
            double[] direction = rootDirections[i];
            axes[top][id] = direction;
            centers[top][id] = multiply(direction, rootNorm);
        }

        int depth = 1;
        for (int level = top - 1; level >= lowest; level--, depth++) {
            for (ContextGraphCommunityCluster parent : levels.get(level + 1).clusters()) {
                checkCancelled();
                List<Integer> children = new ArrayList<>(parent.childIds());
                int childLevel = level;
                children.sort(Comparator.comparingInt((Integer c) ->
                        -levels.get(childLevel).clusters().get(c).memberCount())
                    .thenComparingInt(Integer::intValue));
                double[] parentAxis = axes[level + 1][parent.id()];
                double[] parentPoint = centers[level + 1][parent.id()];
                List<Integer> masses = new ArrayList<>(children.size());
                for (int child : children) masses.add(levels.get(level).clusters().get(child).memberCount());
                double stepNorm = Math.tanh(0.5 * params.stepAt(depth, layers) * factor);
                double[] fractions = weightedFractions(masses, params);
                double[][] childDirections = new double[children.size()][];
                for (int i = 0; i < children.size(); i++)
                    childDirections[i] = localDirection(parentAxis, i, children.size(), fractions[i], params);
                if (connectivity != null && children.size() > 1) {
                    double maxAngle = Math.min(2.65,
                        Math.toRadians(params.angularSpreadDegrees()) * 0.5
                            + Math.toRadians(params.siblingGapDegrees())
                                * Math.min(2.0, Math.sqrt(children.size()) / 2.0));
                    childDirections = connectivity.arrange(level, children,
                        childDirections, parentAxis, maxAngle);
                }
                for (int i = 0; i < children.size(); i++) {
                    int child = children.get(i);
                    double[] direction = childDirections[i];
                    axes[level][child] = direction;
                    centers[level][child] = ContextGraphHyperbolicMath.add(
                        parentPoint, multiply(direction, stepNorm));
                }
            }
        }

        List<List<Integer>> memberships = new ArrayList<>(levels.get(lowest).count());
        for (int i = 0; i < levels.get(lowest).count(); i++) memberships.add(new ArrayList<>());
        int[] partition = levels.get(lowest).membership();
        for (int i = 0; i < ids.size(); i++) memberships.get(partition[i]).add(i);

        Map<String, double[]> result = new LinkedHashMap<>(ids.size() * 2);
        double entityStep = Math.tanh(0.5 * params.stepAt(layers - 1, layers) * factor);
        for (int group = 0; group < memberships.size(); group++) {
            checkCancelled();
            List<Integer> members = memberships.get(group);
            double[] parentAxis = axes[lowest][group];
            double[] parentPoint = centers[lowest][group];
            List<Integer> masses = new ArrayList<>(members.size());
            for (int i = 0; i < members.size(); i++) masses.add(1);
            double[] fractions = weightedFractions(masses, params);
            for (int i = 0; i < members.size(); i++) {
                double[] direction = localDirection(parentAxis, i, members.size(), fractions[i], params);
                double[] position = ContextGraphHyperbolicMath.add(parentPoint,
                    multiply(direction, entityStep));
                result.put(ids.get(members.get(i)), position);
            }
        }
        return new ContextGraphPackedPositions(java.util.Collections.unmodifiableMap(result),
            linkAngleBefore, linkAngleAfter);
    }

    private static double[] weightedFractions(List<Integer> sizes,
                                              ContextGraphHyperbolicPackingParams params) {
        double[] weights = new double[sizes.size()];
        double total = 0;
        for (int i = 0; i < sizes.size(); i++) {
            weights[i] = params.allocation()
                == ContextGraphHyperbolicPackingParams.AngularAllocation.EQUAL
                    ? 1.0 : Math.pow(Math.max(1, sizes.get(i)), params.massInfluence());
            total += weights[i];
        }
        double[] fractions = new double[sizes.size()];
        double before = 0;
        for (int i = 0; i < sizes.size(); i++) {
            fractions[i] = (before + 0.5 * weights[i]) / Math.max(total, 1.0);
            before += weights[i];
        }
        return fractions;
    }

    private static double[] localDirection(double[] axis, int index, int count,
                                           double fraction,
                                           ContextGraphHyperbolicPackingParams params) {
        if (count <= 1) return axis;
        double spread = Math.toRadians(params.angularSpreadDegrees()) * 0.5;
        double gap = Math.toRadians(params.siblingGapDegrees());
        // A requested gap expands available angular room. It is a target, not a
        // guarantee, since a dense sphere cannot satisfy arbitrary minimum gaps.
        double aperture = Math.min(2.65,
            spread + gap * Math.min(2.0, Math.sqrt(count) / 2.0));
        double theta = Math.acos(1.0 - fraction * (1.0 - Math.cos(aperture)));
        double azimuth = GOLDEN_ANGLE * index;
        double[] auxiliary = Math.abs(axis[1]) < 0.9
            ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
        double[] u = normalize(cross(axis, auxiliary));
        double[] v = normalize(cross(axis, u));
        return normalize(new double[]{
            axis[0] * Math.cos(theta) + Math.sin(theta) * (u[0] * Math.cos(azimuth) + v[0] * Math.sin(azimuth)),
            axis[1] * Math.cos(theta) + Math.sin(theta) * (u[1] * Math.cos(azimuth) + v[1] * Math.sin(azimuth)),
            axis[2] * Math.cos(theta) + Math.sin(theta) * (u[2] * Math.cos(azimuth) + v[2] * Math.sin(azimuth))});
    }

    private static ContextGraphSpatialDiagnostics diagnose(Map<String, double[]> positions,
                                                             List<String> sortedIds, int layers,
                                                             double factor, double linkAngleBefore,
                                                             double linkAngleAfter) {
        if (positions.isEmpty())
            return new ContextGraphSpatialDiagnostics(0, layers, factor, 0, 0, 0, 0,
                linkAngleBefore, linkAngleAfter);
        List<double[]> points = new ArrayList<>(sortedIds.size());
        double maxRadius = 0;
        int nearBoundary = 0;
        for (String id : sortedIds) {
            double[] p = positions.get(id);
            if (!ContextGraphHyperbolicMath.insideBall(p))
                throw new IllegalStateException("Computed point is outside the open Poincare ball: " + id);
            points.add(p);
            double radius = Math.sqrt(ContextGraphHyperbolicMath.dot(p, p));
            maxRadius = Math.max(maxRadius, radius);
            if (radius >= 0.984) nearBoundary++;
        }
        // For a stable reference sample, evaluate each sampled entity against ALL
        // other entities, rather than comparing only against other sampled points.
        int sampleCount = Math.min(128, points.size());
        double chartSum = 0, hyperSum = 0;
        for (int s = 0; s < sampleCount; s++) {
            checkCancelled();
            int i = (int) ((long) s * points.size() / sampleCount);
            double[] a = points.get(i);
            double aa = ContextGraphHyperbolicMath.dot(a, a);
            double chartMin = Double.POSITIVE_INFINITY, hyperQMin = Double.POSITIVE_INFINITY;
            for (int j = 0; j < points.size(); j++) {
                if (i == j) continue;
                double[] b = points.get(j);
                double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
                double squared = dx * dx + dy * dy + dz * dz;
                chartMin = Math.min(chartMin, squared);
                double q = 2.0 * squared / ((1.0 - aa)
                    * (1.0 - ContextGraphHyperbolicMath.dot(b, b)));
                hyperQMin = Math.min(hyperQMin, q);
            }
            if (Double.isFinite(chartMin)) chartSum += Math.sqrt(chartMin);
            if (Double.isFinite(hyperQMin))
                hyperSum += Math.log1p(hyperQMin + Math.sqrt(hyperQMin * (hyperQMin + 2.0)));
        }
        return new ContextGraphSpatialDiagnostics(points.size(), layers, factor,
            maxRadius, nearBoundary, chartSum / sampleCount, hyperSum / sampleCount,
            linkAngleBefore, linkAngleAfter);
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("Semantic packing cancelled");
    }

    private static double[] fibonacciDirection(int index, int count) {
        double y = 1.0 - 2.0 * (index + 0.5) / Math.max(1, count);
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        double azimuth = GOLDEN_ANGLE * index;
        return new double[]{Math.cos(azimuth) * r, y, Math.sin(azimuth) * r};
    }

    private static double[] multiply(double[] a, double factor) {
        return new double[]{a[0] * factor, a[1] * factor, a[2] * factor};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        double norm = Math.sqrt(ContextGraphHyperbolicMath.dot(a, a));
        return norm < 1e-12 ? new double[]{0, 0, 1} : multiply(a, 1.0 / norm);
    }
}

/** Detached worker result; not part of the source graph or JavaFX node model. */
record ContextGraphPackedPositions(Map<String, double[]> positions, double linkAngleBefore,
                                   double linkAngleAfter) { }
