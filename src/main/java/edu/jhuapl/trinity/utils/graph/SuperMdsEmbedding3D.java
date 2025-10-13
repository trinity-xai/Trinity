package edu.jhuapl.trinity.utils.graph;

import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDS.Params;
import com.github.trinity.supermds.SuperMDSHelper;

import java.util.List;

/**
 * SuperMdsEmbedding3D
 * -------------------
 * Pluggable adapter that uses SuperMDS to produce a 3D embedding from a distance matrix.
 * <p>
 * Input must be an NxN distance matrix (symmetric, non-negative). We normalize it
 * similarly to your ProjectMdsFeaturesTask before running MDS.
 *
 * @author Sean Phillips
 */
public final class SuperMdsEmbedding3D implements MatrixToGraphAdapter.MdsEmbedding3D {

    private final Params params;

    public SuperMdsEmbedding3D(Params params) {
        if (params == null) {
            throw new IllegalArgumentException("SuperMDS Params must not be null.");
        }
        this.params = params;
    }

    @Override
    public double[][] embed(double[][] distances, List<String> labels) {
        if (distances == null || distances.length == 0) return new double[0][0];

        // SuperMDS expects distances; normalize for stability (matches your example task).
        double[][] normalized = SuperMDSHelper.normalizeDistancesParallel(distances);

        // Run SuperMDS with whatever mode/knobs you pass in Params; outputDim should be 3.
        double[][] embedding = SuperMDS.runMDS(normalized, params);

        // Ensure shape [n][3]; SuperMDS should already do this if params.outputDim == 3.
        if (embedding.length != distances.length || embedding[0].length < 3) {
            throw new IllegalStateException("SuperMDS returned unexpected shape; expected [n][3].");
        }
        return embedding;
    }
}
