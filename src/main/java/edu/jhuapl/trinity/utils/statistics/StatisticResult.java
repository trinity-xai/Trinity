package edu.jhuapl.trinity.utils.statistics;

import java.util.List;

public class StatisticResult {
    private List<Double> values;   // Scalar values for each FeatureVector
    private double[] pdfBins;      // Bin centers for PDF
    private double[] pdf;          // PDF values
    private double[] cdf;          // CDF values

    public StatisticResult(List<Double> values, double[] pdfBins, double[] pdf, double[] cdf) {
        this.values = values;
        this.pdfBins = pdfBins;
        this.pdf = pdf;
        this.cdf = cdf;
    }

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
}
