package edu.jhuapl.trinity.utils.statistics;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PairGridRecord
 * ---------------
 * Minimal, serializable container for a single (X,Y) joint surface result intended for UI / IO.
 *
 * Carries:
 *  - Axes (AxisParams snapshots used during compute)
 *  - The canonical GridSpec used
 *  - Optional PDF and/or CDF grids (List<List<Double>>), same shape as GridSpec
 *  - Provenance objects for PDF/CDF (if present)
 *
 * Notes:
 *  - This is a data record, not a compute class.
 *  - It is agnostic to the source (single cohort vs A/B); use the static factories.
 *  - If you plan to persist, prefer List<List<Double>> over primitive arrays for JSON friendliness.
 *
 * @author Sean Phillips
 */
public final class PairGridRecord implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final AxisParams xAxis;
    private final AxisParams yAxis;
    private final GridSpec grid;

    /** Optional PDF values; null if not requested. */
    private final List<List<Double>> pdf;

    /** Optional CDF values; null if not requested. */
    private final List<List<Double>> cdf;

    /** Provenance for the PDF; null when pdf == null. */
    private final JpdfProvenance pdfProvenance;

    /** Provenance for the CDF; null when cdf == null. */
    private final JpdfProvenance cdfProvenance;

    /** Optional human label for this record (e.g., cohort name or composite tag). */
    private final String label;

    // ---------------------------- Constructors ----------------------------

    private PairGridRecord(Builder b) {
        this.xAxis = b.xAxis;
        this.yAxis = b.yAxis;
        this.grid = b.grid;
        this.pdf = b.pdf;
        this.cdf = b.cdf;
        this.pdfProvenance = b.pdfProvenance;
        this.cdfProvenance = b.cdfProvenance;
        this.label = b.label;
    }

    public static Builder newBuilder(AxisParams xAxis, AxisParams yAxis, GridSpec grid) {
        return new Builder(xAxis, yAxis, grid);
    }

    public static final class Builder {
        private final AxisParams xAxis;
        private final AxisParams yAxis;
        private final GridSpec grid;

        private List<List<Double>> pdf;
        private List<List<Double>> cdf;
        private JpdfProvenance pdfProvenance;
        private JpdfProvenance cdfProvenance;
        private String label;

        private Builder(AxisParams xAxis, AxisParams yAxis, GridSpec grid) {
            this.xAxis = Objects.requireNonNull(xAxis, "xAxis");
            this.yAxis = Objects.requireNonNull(yAxis, "yAxis");
            this.grid = Objects.requireNonNull(grid, "grid");
        }

        public Builder pdf(List<List<Double>> v) { this.pdf = v; return this; }
        public Builder cdf(List<List<Double>> v) { this.cdf = v; return this; }
        public Builder pdfProvenance(JpdfProvenance p) { this.pdfProvenance = p; return this; }
        public Builder cdfProvenance(JpdfProvenance p) { this.cdfProvenance = p; return this; }
        public Builder label(String v) { this.label = v; return this; }

        public PairGridRecord build() {
            return new PairGridRecord(this);
        }
    }

    // ---------------------------- Factories ----------------------------

    /**
     * Create a record from a single cohort GridDensityResult.
     * Use this from JpdfBatchEngine outputs.
     */
    public static PairGridRecord fromSingle(AxisParams xAxis,
                                            AxisParams yAxis,
                                            GridSpec grid,
                                            GridDensityResult res,
                                            JpdfRecipe.OutputKind ok,
                                            JpdfProvenance pdfProv,
                                            JpdfProvenance cdfProv,
                                            String label) {
        Objects.requireNonNull(res, "res");

        List<List<Double>> pdf = null, cdf = null;
        if (ok == JpdfRecipe.OutputKind.PDF_ONLY || ok == JpdfRecipe.OutputKind.PDF_AND_CDF) {
            pdf = safeCopy2D(res.pdfAsListGrid());
        }
        if (ok == JpdfRecipe.OutputKind.CDF_ONLY || ok == JpdfRecipe.OutputKind.PDF_AND_CDF) {
            cdf = safeCopy2D(res.cdfAsListGrid());
        }

        return PairGridRecord.newBuilder(xAxis, yAxis, grid)
                .pdf(pdf)
                .cdf(cdf)
                .pdfProvenance(pdfProv)
                .cdfProvenance(cdfProv)
                .label(label)
                .build();
    }

    /**
     * Create two records (A and B) plus an optional signed-difference record from ABComparisonEngine.
     * The diff record is returned as the third element; it may be null if not requested.
     */
    public static Triple<PairGridRecord, PairGridRecord, PairGridRecord> fromAb(ABComparisonEngine.AbResult ab,
                                                                                AxisParams xAxis,
                                                                                AxisParams yAxis,
                                                                                JpdfRecipe.OutputKind ok,
                                                                                String labelA,
                                                                                String labelB,
                                                                                String labelDiff) {
        Objects.requireNonNull(ab, "ab");
        GridSpec grid = ab.grid;

        PairGridRecord a = fromSingle(
                xAxis, yAxis, grid, ab.a, ok,
                ab.pdfProvA, ab.cdfProvA, labelA
        );
        PairGridRecord b = fromSingle(
                xAxis, yAxis, grid, ab.b, ok,
                ab.pdfProvB, ab.cdfProvB, labelB
        );

        PairGridRecord diff = null;
        boolean needPdf = ok == JpdfRecipe.OutputKind.PDF_ONLY || ok == JpdfRecipe.OutputKind.PDF_AND_CDF;
        boolean needCdf = ok == JpdfRecipe.OutputKind.CDF_ONLY || ok == JpdfRecipe.OutputKind.PDF_AND_CDF;

        List<List<Double>> pdf = needPdf ? safeCopy2D(ab.pdfDiff) : null;
        List<List<Double>> cdf = needCdf ? safeCopy2D(ab.cdfDiff) : null;

        if ((pdf != null) || (cdf != null)) {
            diff = PairGridRecord.newBuilder(xAxis, yAxis, grid)
                    .pdf(pdf)
                    .cdf(cdf)
                    .pdfProvenance(ab.pdfProvDiff)
                    .cdfProvenance(ab.cdfProvDiff)
                    .label(labelDiff)
                    .build();
        }

        return new Triple<>(a, b, diff);
    }

    // ---------------------------- Getters ----------------------------

    public AxisParams getxAxis() { return xAxis; }
    public AxisParams getyAxis() { return yAxis; }
    public GridSpec getGrid() { return grid; }
    public List<List<Double>> getPdf() { return pdf; }
    public List<List<Double>> getCdf() { return cdf; }
    public JpdfProvenance getPdfProvenance() { return pdfProvenance; }
    public JpdfProvenance getCdfProvenance() { return cdfProvenance; }
    public String getLabel() { return label; }

    // ---------------------------- Helpers ----------------------------

    private static List<List<Double>> safeCopy2D(List<List<Double>> src) {
        if (src == null) return null;
        List<List<Double>> out = new ArrayList<>(src.size());
        for (List<Double> row : src) out.add(new ArrayList<>(row));
        return out;
    }

    /**
     * Tiny generic triple carrier (kept local to avoid extra dependencies).
     */
    public static final class Triple<A, B, C> implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        public final A first;
        public final B second;
        public final C third;
        public Triple(A first, B second, C third) {
            this.first = first; this.second = second; this.third = third;
        }
    }
}
