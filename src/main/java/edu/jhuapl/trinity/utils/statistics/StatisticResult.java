package edu.jhuapl.trinity.utils.statistics;

import java.util.List;

/**
 * Container for scalar-values-derived statistics and histogram metadata.
 *
 * Fields include:
 *  - values: per-sample scalar value used to build the histogram
 *  - pdfBins: histogram bin centers (length = N)
 *  - pdf: density at each bin (normalized so sum(pdf[i] * binWidth) ≈ 1)
 *  - cdf: cumulative probability at each bin center (in [0,1])
 *  - binEdges: histogram bin edges (length = N + 1)
 *  - sampleToBin: for each sample index, the assigned bin index (or -1)
 *  - binCounts: count of samples in each bin (length = N)
 *  - binToSampleIdx: for each bin, the list of sample indices assigned to it
 *  - totalSamples: number of valid samples considered in the histogram
 */
public class StatisticResult {

    // Per-sample scalar values that were histogrammed (aligned to original order)
    private List<Double> values;

    // Histogram data
    private double[] pdfBins;   // bin centers (length = N)
    private double[] pdf;       // density values at centers (length = N)
    private double[] cdf;       // cumulative probability at centers (length = N)

    // Additional histogram metadata for interactivity/linking
    private double[] binEdges;      // bin edges (length = N + 1)
    private int[] sampleToBin;      // mapping sample index -> bin index (or -1)
    private int[] binCounts;        // counts per bin (length = N)
    private int[][] binToSampleIdx; // for each bin, indices of samples in that bin
    private int totalSamples;       // number of (valid) samples accumulated

    public StatisticResult(List<Double> values, double[] pdfBins, double[] pdf, double[] cdf) {
        this.values = values;
        this.pdfBins = pdfBins;
        this.pdf = pdf;
        this.cdf = cdf;
    }

    // ----- Getters / Setters -----

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
    }

    public double[] getPdfBins() {
        return pdfBins;
    }

    public void setPdfBins(double[] pdfBins) {
        this.pdfBins = pdfBins;
    }

    public double[] getPdf() {
        return pdf;
    }

    public void setPdf(double[] pdf) {
        this.pdf = pdf;
    }

    public double[] getCdf() {
        return cdf;
    }

    public void setCdf(double[] cdf) {
        this.cdf = cdf;
    }

    /** @return bin edges array of length N+1 (may be null if not provided) */
    public double[] getBinEdges() {
        return binEdges;
    }

    /** @param binEdges bin edges array of length N+1 */
    public void setBinEdges(double[] binEdges) {
        this.binEdges = binEdges;
    }

    /** @return per-sample mapping to bin index (or -1), length equals number of samples; may be null */
    public int[] getSampleToBin() {
        return sampleToBin;
    }

    /** @param sampleToBin per-sample mapping to bin index (or -1), length equals number of samples */
    public void setSampleToBin(int[] sampleToBin) {
        this.sampleToBin = sampleToBin;
    }

    /** @return counts per bin (length = number of bins); may be null */
    public int[] getBinCounts() {
        return binCounts;
    }

    /** @param binCounts counts per bin (length = number of bins) */
    public void setBinCounts(int[] binCounts) {
        this.binCounts = binCounts;
    }

    /** @return for each bin, the array of sample indices assigned to that bin; may be null */
    public int[][] getBinToSampleIdx() {
        return binToSampleIdx;
    }

    /** @param binToSampleIdx for each bin, the array of sample indices assigned to that bin */
    public void setBinToSampleIdx(int[][] binToSampleIdx) {
        this.binToSampleIdx = binToSampleIdx;
    }

    /** @return total number of valid samples used to build the histogram */
    public int getTotalSamples() {
        return totalSamples;
    }

    /** @param totalSamples total number of valid samples used to build the histogram */
    public void setTotalSamples(int totalSamples) {
        this.totalSamples = totalSamples;
    }

    // ----- Convenience -----

    /** @return number of bins (0 if unknown) */
    public int getBinCount() {
        return (pdfBins != null) ? pdfBins.length : 0;
    }

    /** @return true if binEdges are present and consistent with pdfBins */
    public boolean hasValidBinEdges() {
        return binEdges != null && pdfBins != null && binEdges.length == pdfBins.length + 1;
    }
}
