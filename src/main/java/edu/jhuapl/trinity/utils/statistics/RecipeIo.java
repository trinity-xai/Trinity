package edu.jhuapl.trinity.utils.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * RecipeIo
 * --------
 * JSON save/load helper for JpdfRecipe. Uses a DTO schema (RecipeDto) so
 * the JSON layout is explicit, versioned, and stable even if JpdfRecipe evolves.
 *
 */
public final class RecipeIo {
    private static final ObjectMapper M = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private RecipeIo() {}

    /** Write a recipe as JSON to the given stream. */
    public static void write(OutputStream os, JpdfRecipe r) throws IOException {
        if (r == null) throw new IllegalArgumentException("recipe null");
        M.writeValue(os, RecipeDto.from(r));
    }

    /** Read a recipe from JSON stream. */
    public static JpdfRecipe read(InputStream is) throws IOException {
        RecipeDto dto = M.readValue(is, RecipeDto.class);
        return dto.toRecipe();
    }

    // -----------------------------------------------------------------
    // DTO for stable JSON schema
    // -----------------------------------------------------------------
    public static final class RecipeDto {
        public String _schema = "trinity.jpdf.recipe.v1";

        public String name;
        public String description;

        public String pairSelection;
        public String scoreMetric;
        public Integer topK;
        public Double scoreThreshold;

        public Boolean componentPairsMode;
        public Integer componentIndexStart;
        public Integer componentIndexEnd;
        public Boolean includeSelfPairs;
        public Boolean orderedPairs;

        public Integer binsX;
        public Integer binsY;
        public String boundsPolicy;
        public String canonicalPolicyId;

        public Double minAvgCountPerCell;

        public String outputKind;
        public Boolean cacheEnabled;
        public Boolean saveThumbnails;

        public String cohortALabel;
        public String cohortBLabel;

        public static RecipeDto from(JpdfRecipe r) {
            RecipeDto d = new RecipeDto();
            d.name = r.getName();
            d.description = r.getDescription();
            d.pairSelection = r.getPairSelection().name();
            d.scoreMetric = r.getScoreMetric().name();
            d.topK = r.getTopK();
            d.scoreThreshold = r.getScoreThreshold();
            d.componentPairsMode = r.isComponentPairsMode();
            d.componentIndexStart = r.getComponentIndexStart();
            d.componentIndexEnd = r.getComponentIndexEnd();
            d.includeSelfPairs = r.isIncludeSelfPairs();
            d.orderedPairs = r.isOrderedPairs();
            d.binsX = r.getBinsX();
            d.binsY = r.getBinsY();
            d.boundsPolicy = r.getBoundsPolicy().name();
            d.canonicalPolicyId = r.getCanonicalPolicyId();
            d.minAvgCountPerCell = r.getMinAvgCountPerCell();
            d.outputKind = r.getOutputKind().name();
            d.cacheEnabled = r.isCacheEnabled();
            d.saveThumbnails = r.isSaveThumbnails();
            d.cohortALabel = r.getCohortALabel();
            d.cohortBLabel = r.getCohortBLabel();
            return d;
        }

        public JpdfRecipe toRecipe() {
            JpdfRecipe.Builder b = JpdfRecipe.newBuilder(nonBlank(name, "Unnamed"))
                    .description(nvl(description, ""))
                    .pairSelection(JpdfRecipe.PairSelection.valueOf(nvl(pairSelection, "ALL")))
                    .scoreMetric(JpdfRecipe.ScoreMetric.valueOf(nvl(scoreMetric, "PEARSON")))
                    .bins(nvl(binsX, 64), nvl(binsY, 64))
                    .boundsPolicy(JpdfRecipe.BoundsPolicy.valueOf(nvl(boundsPolicy, "DATA_MIN_MAX")))
                    .canonicalPolicyId(nvl(canonicalPolicyId, "default"))
                    .minAvgCountPerCell(nvl(minAvgCountPerCell, 3.0))
                    .outputKind(JpdfRecipe.OutputKind.valueOf(nvl(outputKind, "PDF_AND_CDF")))
                    .cacheEnabled(nvl(cacheEnabled, true))
                    .saveThumbnails(nvl(saveThumbnails, true))
                    .componentPairsMode(nvl(componentPairsMode, true))
                    .componentIndexRange(nvl(componentIndexStart, 0), nvl(componentIndexEnd, 1))
                    .includeSelfPairs(nvl(includeSelfPairs, false))
                    .orderedPairs(nvl(orderedPairs, false));

            if (topK != null) b.topK(topK);
            if (scoreThreshold != null) b.scoreThreshold(scoreThreshold);
            if (cohortALabel != null || cohortBLabel != null)
                b.cohortLabels(nvl(cohortALabel, "A"), nvl(cohortBLabel, "B"));

            return b.build();
        }

        // helpers
        private static String nonBlank(String s, String def) { return (s == null || s.isBlank()) ? def : s; }
        private static String nvl(String s, String def) { return s == null ? def : s; }
        private static int nvl(Integer v, int def) { return v == null ? def : v; }
        private static double nvl(Double v, double def) { return v == null ? def : v; }
        private static boolean nvl(Boolean v, boolean def) { return v == null ? def : v; }
    }
}
