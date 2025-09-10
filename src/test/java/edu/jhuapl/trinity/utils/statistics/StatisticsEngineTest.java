package edu.jhuapl.trinity.utils.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.metric.Metric;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsEngineTest {
    public static void main(String[] args) {
        randomGaussianTest();
        bimodalTest();
    }
public static void randomGaussianTest() {
        // === 1. Generate synthetic data ===
        int numVectors = 1000;
        int dim = 10;
        Random rand = new Random(42); // fixed seed for reproducibility

        List<FeatureVector> vectors = new ArrayList<>();
        for (int i = 0; i < numVectors; i++) {
            List<Double> data = new ArrayList<>();
            for (int j = 0; j < dim; j++) {
                data.add(rand.nextGaussian()); // Standard normal distribution
            }
            FeatureVector fv = new FeatureVector();
            fv.setData(data);
            vectors.add(fv);
        }

        // === 2. Select statistics to compute ===
        Set<StatisticEngine.ScalarType> types = Set.of(
            StatisticEngine.ScalarType.NORM,
            StatisticEngine.ScalarType.MEAN,
            StatisticEngine.ScalarType.MAX
        );

        // === 3. Run the statistics engine ===
        int pdfBins = 40;
        Map<StatisticEngine.ScalarType, StatisticResult> stats =
            StatisticEngine.computeStatistics(
                vectors,
                types,
                pdfBins,
                null,      // no metric for generic
                null       // no reference vector
            );

        // === 4. Print results for inspection & theory spot-check ===

        // Theory values
        double normTheoreticalMean = Math.sqrt(dim);
        double meanTheoreticalMean = 0.0;
        double meanTheoreticalStd = 1.0 / Math.sqrt(dim);
        // Theoretical expected max of dim normals: approx Φ^-1(1-1/dim) ~ inverse CDF
        // For dim=10: approx 1.538 (by order stats), but sample max ~ 2.0–2.3 is typical
        double approxMaxTheoretical = approxNormalMax(dim);

        System.out.println("========= Theory vs. Empirical Spot Check =========");
        System.out.println("Dimension: " + dim + "   Number of vectors: " + numVectors);
        System.out.printf("Expected L2 norm:       mean ≈ %.3f\n", normTheoreticalMean);
        System.out.printf("Expected mean of vec:   mean = %.3f   std = %.3f\n", meanTheoreticalMean, meanTheoreticalStd);
        System.out.printf("Expected max per vec:   approx ≈ %.3f (see comments)\n", approxMaxTheoretical);
        System.out.println();

        for (StatisticEngine.ScalarType type : types) {
            StatisticResult sr = stats.get(type);
            double empiricalMean = sr.getValues().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double empiricalStd = std(sr.getValues(), empiricalMean);

            System.out.println("---- " + type + " ----");
            System.out.printf("Sample min:  %.4f\n", Collections.min(sr.getValues()));
            System.out.printf("Sample max:  %.4f\n", Collections.max(sr.getValues()));
            System.out.printf("Sample mean: %.4f\n", empiricalMean);
            System.out.printf("Sample std:  %.4f\n", empiricalStd);

            System.out.println("First 5 PDF bins:  " + Arrays.toString(Arrays.copyOf(sr.getPdfBins(), 5)));
            System.out.println("First 5 PDF vals:  " + Arrays.toString(Arrays.copyOf(sr.getPdf(), 5)));
            System.out.println("First 5 CDF vals:  " + Arrays.toString(Arrays.copyOf(sr.getCdf(), 5)));
            System.out.println();
        }

        // === 5. Spot-check summary ===
        System.out.println("If these match theory (see above), PDF/CDF logic is likely correct!");
    
}    
public static void bimodalTest() {
    int numVectors = 1000;
    int dim = 10;
    List<FeatureVector> vectors = new ArrayList<>();
    // First half: all zeros
    for (int i = 0; i < numVectors / 2; i++) {
        FeatureVector fv = new FeatureVector();
        fv.setData(Collections.nCopies(dim, 0.0));
        vectors.add(fv);
    }
    // Second half: all ones
    for (int i = 0; i < numVectors / 2; i++) {
        FeatureVector fv = new FeatureVector();
        fv.setData(Collections.nCopies(dim, 1.0));
        vectors.add(fv);
    }

    Set<StatisticEngine.ScalarType> types = Set.of(
        StatisticEngine.ScalarType.NORM,
        StatisticEngine.ScalarType.MEAN,
        StatisticEngine.ScalarType.MAX
    );
    int pdfBins = 10; // fewer bins to make PDF more obvious for discrete values

    Map<StatisticEngine.ScalarType, StatisticResult> stats =
        StatisticEngine.computeStatistics(
            vectors, types, pdfBins, null, null
        );

    double normPeak = Math.sqrt(dim);

    System.out.println("========= Bimodal (two-cluster) Test =========");
    System.out.println("Dimension: " + dim + "   Number of vectors: " + numVectors);
    System.out.println("Expected NORM: two peaks at 0 and " + normPeak);
    System.out.println("Expected MEAN: two peaks at 0 and 1");
    System.out.println("Expected MAX: two peaks at 0 and 1\n");

    for (StatisticEngine.ScalarType type : types) {
        StatisticResult sr = stats.get(type);
        Map<Double, Long> counts = sr.getValues().stream()
            .collect(Collectors.groupingBy(x -> Math.round(x*10000.0)/10000.0, Collectors.counting()));

        System.out.println("---- " + type + " ----");
        System.out.printf("Unique value counts: %s\n", counts);
        System.out.printf("Sample min:  %.4f\n", Collections.min(sr.getValues()));
        System.out.printf("Sample max:  %.4f\n", Collections.max(sr.getValues()));
        System.out.printf("Sample mean: %.4f\n", sr.getValues().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("PDF: " + Arrays.toString(sr.getPdf()));
        System.out.println("CDF: " + Arrays.toString(sr.getCdf()));
        System.out.println();
    }

    System.out.println("Check that all values are at expected peaks, and that the PDF/CDF reflects a perfect 50/50 split.");
}
    // Utility: sample stddev
    private static double std(List<Double> vals, double mean) {
        double sum = 0.0;
        for (double v : vals) sum += (v - mean) * (v - mean);
        return Math.sqrt(sum / vals.size());
    }

    // Approximate the expected maximum of N iid standard normal variables
    private static double approxNormalMax(int n) {
        // Gumbel approximation: μ + σ * Φ^-1(1 - 1/n), for N(μ, σ^2)
        // For N(0,1): use inverse error function
        // Approx: sqrt(2*log(n))
        return Math.sqrt(2 * Math.log(n));
    }
}