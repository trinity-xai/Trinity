package edu.jhuapl.trinity.javafx.components.dialogs;

import edu.jhuapl.trinity.utils.statistics.SyntheticMatrixFactory;
import edu.jhuapl.trinity.utils.statistics.SyntheticMatrixFactory.Cohorts;
import edu.jhuapl.trinity.utils.statistics.SyntheticMatrixFactory.SyntheticMatrix;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SyntheticDataDialog
 * -------------------
 * A reusable JavaFX dialog that lets the user generate:
 * • Similarity synthetic matrices (2-cluster, K-cluster, Ring, Grid)
 * • Divergence synthetic matrices (3 clusters)
 * • Synthetic cohorts (Gaussian vs Uniform) for divergence workflows
 * <p>
 * New: You can optionally attach cohorts to any generated matrix so
 * PairwiseMatrixView can render JPDF/ΔPDF even without real data.
 */
public final class SyntheticDataDialog extends Dialog<SyntheticDataDialog.Result> {

    public enum Kind {SIMILARITY_MATRIX, DIVERGENCE_MATRIX, COHORTS}

    /**
     * Discriminated union for dialog output.
     */
    public static final class Result {
        private final Kind kind;
        private final SyntheticMatrix matrix; // non-null for SIMILARITY_MATRIX or DIVERGENCE_MATRIX
        private final Cohorts cohorts;        // non-null for COHORTS

        private Result(Kind kind, SyntheticMatrix matrix, Cohorts cohorts) {
            this.kind = Objects.requireNonNull(kind);
            this.matrix = matrix;
            this.cohorts = cohorts;
        }

        public Kind kind() {
            return kind;
        }

        public SyntheticMatrix matrix() {
            return matrix;
        }

        public Cohorts cohorts() {
            return cohorts;
        }

        public static Result similarity(SyntheticMatrix m) {
            return new Result(Kind.SIMILARITY_MATRIX, m, null);
        }

        public static Result divergence(SyntheticMatrix m) {
            return new Result(Kind.DIVERGENCE_MATRIX, m, null);
        }

        public static Result cohorts(Cohorts c) {
            return new Result(Kind.COHORTS, null, c);
        }
    }

    // ------------------------------
    // Controls shared helpers
    // ------------------------------
    private static TextField tf(String text, int prefWidth) {
        TextField t = new TextField(text);
        t.setPrefWidth(prefWidth);
        return t;
    }

    private static HBox row(String label, Node control) {
        Label l = new Label(label);
        l.setPrefWidth(160);
        HBox hb = new HBox(8, l, control);
        hb.setAlignment(Pos.CENTER_LEFT);
        return hb;
    }

    private static VBox section(String title, Node... rows) {
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        VBox v = new VBox(8);
        v.getChildren().add(t);
        v.getChildren().add(new Separator());
        v.getChildren().addAll(rows);
        return v;
    }

    private static double dval(TextField tf, double def) {
        try {
            return Double.parseDouble(tf.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static int ival(TextField tf, int def) {
        try {
            return Integer.parseInt(tf.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static long lval(TextField tf, long def) {
        try {
            return Long.parseLong(tf.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    // ------------------------------
    // Tabs
    // ------------------------------
    private final TabPane tabs = new TabPane();

    // Similarity tab controls
    private final ComboBox<String> simKind = new ComboBox<>();
    private final TextField simN1 = tf("10", 80);
    private final TextField simN2 = tf("10", 80);
    private final TextField simK = tf("3", 80);
    private final TextField simSizes = tf("8,8,8", 160);
    private final TextField simRingN = tf("24", 80);
    private final TextField simRingSigma = tf("3.0", 80);
    private final TextField simGridNx = tf("6", 80);
    private final TextField simGridNy = tf("6", 80);
    private final TextField simGridSigma = tf("1.5", 80);
    private final TextField simWithin = tf("0.90", 80);
    private final TextField simBetween = tf("0.10", 80);
    private final TextField simNoise = tf("0.02", 80);
    private final TextField simSeed = tf("42", 120);

    // Similarity: optional cohorts
    private final CheckBox simAttachCohorts = new CheckBox("Attach cohorts (Gaussian A vs Uniform B)");
    private final TextField simCohN = tf("400", 100);
    private final TextField simCohMu = tf("0.0", 80);
    private final TextField simCohSigma = tf("1.0", 80);
    private final TextField simCohMin = tf("-2.0", 80);
    private final TextField simCohMax = tf("2.0", 80);
    private final TextField simCohSeedA = tf("100", 120);
    private final TextField simCohSeedB = tf("101", 120);
    private VBox simCohSection; // toggled

    // Divergence tab controls
    private final TextField divSizes = tf("10,10,10", 160);
    private final TextField divWithin = tf("0.10", 80);
    private final TextField divBetween = tf("0.90", 80);
    private final TextField divNoise = tf("0.02", 80);
    private final TextField divSeed = tf("46", 120);

    // Divergence: optional cohorts
    private final CheckBox divAttachCohorts = new CheckBox("Attach cohorts (Gaussian A vs Uniform B)");
    private final TextField divCohN = tf("400", 100);
    private final TextField divCohD = tf("16", 100);
    private final TextField divCohMu = tf("0.0", 80);
    private final TextField divCohSigma = tf("1.0", 80);
    private final TextField divCohMin = tf("-2.0", 80);
    private final TextField divCohMax = tf("2.0", 80);
    private final TextField divCohSeedA = tf("100", 120);
    private final TextField divCohSeedB = tf("101", 120);
    private VBox divCohSection; // toggled

    // Cohorts tab controls (standalone)
    private final TextField cohN = tf("200", 100);
    private final TextField cohD = tf("16", 100);
    private final TextField cohMu = tf("0.0", 80);
    private final TextField cohSigma = tf("1.0", 80);
    private final TextField cohMin = tf("-2.0", 80);
    private final TextField cohMax = tf("2.0", 80);
    private final TextField cohSeedA = tf("100", 120);
    private final TextField cohSeedB = tf("101", 120);

    public SyntheticDataDialog() {
        setTitle("Generate Synthetic Data");
        setHeaderText("Create synthetic matrices or cohorts for visual verification.");

        // Buttons
        ButtonType buildBtn = new ButtonType("Build", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(buildBtn, cancelBtn);

        // Content
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Similarity", buildSimilarityContent()),
            new Tab("Divergence", buildDivergenceContent()),
            new Tab("Cohorts", buildCohortsContent())
        );
        getDialogPane().setContent(tabs);
        getDialogPane().setPadding(new Insets(8));

        // Result converter
        setResultConverter((ButtonType param) -> {
            if (param != buildBtn) return null;
            Tab sel = tabs.getSelectionModel().getSelectedItem();
            if (sel == null) return null;
            String name = sel.getText();
            try {
                return switch (name) {
                    case "Similarity" -> buildSimilarity();
                    case "Divergence" -> buildDivergence();
                    case "Cohorts" -> buildCohorts();
                    default -> null;
                };
            } catch (Throwable t) {
                Alert a = new Alert(Alert.AlertType.ERROR,
                    "Build failed: " + t.getClass().getSimpleName() + " – " + t.getMessage(),
                    ButtonType.OK);
                a.showAndWait();
                return null;
            }
        });
    }

    // ------------------------------
    // Similarity tab
    // ------------------------------
    private Node buildSimilarityContent() {
        simKind.getItems().addAll("2 Clusters", "K Clusters", "Ring", "Grid");
        simKind.getSelectionModel().selectFirst();

        HBox sizesRow = new HBox(8, new Label("Sizes (csv)"), simSizes);
        sizesRow.setAlignment(Pos.CENTER_LEFT);

        // Cohort section (hidden unless checkbox selected)
        simAttachCohorts.setSelected(false);
        simCohSection = section("Attach Cohorts (optional)",
            row("N (vectors)", simCohN),
            row("Gaussian A (μ, σ)", new HBox(6, simCohMu, simCohSigma)),
            row("Uniform B [min, max]", new HBox(6, simCohMin, simCohMax)),
            row("Seed A / B", new HBox(6, simCohSeedA, simCohSeedB))
        );
        simCohSection.setDisable(true);
        simCohSection.setOpacity(0.45);

        simAttachCohorts.setOnAction(e -> {
            boolean on = simAttachCohorts.isSelected();
            simCohSection.setDisable(!on);
            simCohSection.setOpacity(on ? 1.0 : 0.45);
        });

        VBox top = new VBox(10,
            section("Structure",
                row("Kind", simKind),
                row("N1 / N2 (2 clusters)", new HBox(6, simN1, simN2)),
                row("K (K clusters)", simK),
                sizesRow,
                row("Ring N, σ", new HBox(6, simRingN, simRingSigma)),
                row("Grid Nx, Ny, σ", new HBox(6, simGridNx, simGridNy, simGridSigma))
            ),
            section("Weights / Noise",
                row("Within weight", simWithin),
                row("Between weight", simBetween),
                row("Noise (ε)", simNoise)
            ),
            section("Random",
                row("Seed", simSeed)
            ),
            new Separator(),
            row("", simAttachCohorts),
            simCohSection
        );
        top.setPadding(new Insets(8));
        return new ScrollPane(top);
    }

    private Result buildSimilarity() {
        String kind = simKind.getValue();
        double within = dval(simWithin, 0.90);
        double between = dval(simBetween, 0.10);
        double noise = dval(simNoise, 0.02);
        long seed = lval(simSeed, 42L);

        SyntheticMatrix sm;
        switch (kind) {
            case "2 Clusters" -> {
                int n1 = ival(simN1, 10);
                int n2 = ival(simN2, 10);
                sm = SyntheticMatrixFactory.twoClustersSimilarity(n1, n2, within, between, noise, seed);
            }
            case "K Clusters" -> {
                int k = Math.max(2, ival(simK, 3));
                int[] sizes = parseSizes(simSizes.getText(), k);
                sm = SyntheticMatrixFactory.kClustersSimilarity(sizes, within, between, noise, seed);
            }
            case "Ring" -> {
                int n = ival(simRingN, 24);
                double sigma = dval(simRingSigma, 3.0);
                sm = SyntheticMatrixFactory.ringSimilarity(n, sigma, noise, seed);
            }
            case "Grid" -> {
                int nx = ival(simGridNx, 6);
                int ny = ival(simGridNy, 6);
                double sigma = dval(simGridSigma, 1.5);
                sm = SyntheticMatrixFactory.gridSimilarity(nx, ny, sigma, noise, seed);
            }
            default -> throw new IllegalArgumentException("Unsupported similarity kind: " + kind);
        }

        if (simAttachCohorts.isSelected()) {
            int N = ival(simCohN, 400);
            int D = sm.matrix().length; // match feature dimension to matrix size
            double mu = dval(simCohMu, 0.0);
            double sigma = dval(simCohSigma, 1.0);
            double min = dval(simCohMin, -2.0);
            double max = dval(simCohMax, 2.0);
            long seedA = lval(simCohSeedA, 100L);
            long seedB = lval(simCohSeedB, 101L);

            Cohorts c = SyntheticMatrixFactory.makeCohorts_GaussianVsUniform(N, D, mu, sigma, min, max, seedA, seedB);
            sm = sm.withCohorts(c.cohortA(), c.cohortB()).withTitle(" + Cohorts(Gauss μ=" + mu + ",σ=" + sigma + " vs Uniform[" + min + "," + max + "], N=" + N + ")");
        }

        return Result.similarity(sm);
    }

    // ------------------------------
    // Divergence tab
    // ------------------------------
    private Node buildDivergenceContent() {
        // Cohort section (hidden unless checkbox selected)
        divAttachCohorts.setSelected(false);
        divCohSection = section("Attach Cohorts (optional)",
            row("N (vectors)", divCohN),
            row("D (dimensions)", divCohD),
            row("Gaussian A (μ, σ)", new HBox(6, divCohMu, divCohSigma)),
            row("Uniform B [min, max]", new HBox(6, divCohMin, divCohMax)),
            row("Seed A / B", new HBox(6, divCohSeedA, divCohSeedB))
        );
        divCohSection.setDisable(true);
        divCohSection.setOpacity(0.45);

        divAttachCohorts.setOnAction(e -> {
            boolean on = divAttachCohorts.isSelected();
            divCohSection.setDisable(!on);
            divCohSection.setOpacity(on ? 1.0 : 0.45);
        });

        VBox top = new VBox(10,
            section("Cluster Sizes",
                row("Sizes (csv)", divSizes)
            ),
            section("Weights / Noise",
                row("Within divergence", divWithin),
                row("Between divergence", divBetween),
                row("Noise (ε)", divNoise)
            ),
            section("Random",
                row("Seed", divSeed)
            ),
            new Separator(),
            row("", divAttachCohorts),
            divCohSection
        );
        top.setPadding(new Insets(8));
        return new ScrollPane(top);
    }

    private Result buildDivergence() {
        int[] sizes = parseSizes(divSizes.getText(), -1);
        double within = dval(divWithin, 0.10);
        double between = dval(divBetween, 0.90);
        double noise = dval(divNoise, 0.02);
        long seed = lval(divSeed, 46L);

        SyntheticMatrix sm = SyntheticMatrixFactory.threeClustersDivergence(sizes, within, between, noise, seed);

        if (divAttachCohorts.isSelected()) {
            int N = ival(divCohN, 400);
            int D = ival(divCohD, 16);
            double mu = dval(divCohMu, 0.0);
            double sigma = dval(divCohSigma, 1.0);
            double min = dval(divCohMin, -2.0);
            double max = dval(divCohMax, 2.0);
            long seedA = lval(divCohSeedA, 100L);
            long seedB = lval(divCohSeedB, 101L);

            Cohorts c = SyntheticMatrixFactory.makeCohorts_GaussianVsUniform(N, D, mu, sigma, min, max, seedA, seedB);
            sm = sm.withCohorts(c.cohortA(), c.cohortB()).withTitle(" + Cohorts(Gauss μ=" + mu + ",σ=" + sigma + " vs Uniform[" + min + "," + max + "], N=" + N + ", D=" + D + ")");
        }

        return Result.divergence(sm);
    }

    // ------------------------------
    // Cohorts tab (standalone)
    // ------------------------------
    private Node buildCohortsContent() {
        VBox top = new VBox(10,
            section("Shape",
                row("N (vectors)", cohN),
                row("D (dimensions)", cohD)
            ),
            section("Gaussian A (μ, σ)", new HBox(6, cohMu, cohSigma)),
            section("Uniform B [min, max]", new HBox(6, cohMin, cohMax)),
            section("Seeds",
                row("Seed A", cohSeedA),
                row("Seed B", cohSeedB)
            )
        );
        top.setPadding(new Insets(8));
        return new ScrollPane(top);
    }

    private Result buildCohorts() {
        int n = ival(cohN, 200);
        int d = ival(cohD, 16);
        double mu = dval(cohMu, 0.0);
        double sigma = dval(cohSigma, 1.0);
        double min = dval(cohMin, -2.0);
        double max = dval(cohMax, 2.0);
        long seedA = lval(cohSeedA, 100L);
        long seedB = lval(cohSeedB, 101L);

        Cohorts c = SyntheticMatrixFactory.makeCohorts_GaussianVsUniform(n, d, mu, sigma, min, max, seedA, seedB);
        return Result.cohorts(c);
    }

    // ------------------------------
    // Utils
    // ------------------------------
    private static int[] parseSizes(String csv, int enforceK) {
        if (csv == null || csv.isBlank()) {
            if (enforceK > 0) {
                int[] def = new int[enforceK];
                for (int i = 0; i < enforceK; i++) def[i] = 8;
                return def;
            }
            return new int[]{10, 10, 10};
        }
        String[] toks = csv.split("[,;\\s]+");
        List<Integer> vals = new ArrayList<>();
        for (String t : toks) {
            try {
                int v = Integer.parseInt(t.trim());
                if (v > 0) vals.add(v);
            } catch (Exception ignore) { /* skip */ }
        }
        if (vals.isEmpty()) {
            if (enforceK > 0) {
                int[] def = new int[enforceK];
                for (int i = 0; i < enforceK; i++) def[i] = 8;
                return def;
            }
            return new int[]{10, 10, 10};
        }
        if (enforceK > 0 && vals.size() != enforceK) {
            // Resize to K
            int[] out = new int[enforceK];
            for (int i = 0; i < enforceK; i++) out[i] = (i < vals.size()) ? vals.get(i) : vals.get(vals.size() - 1);
            return out;
        }
        int[] out = new int[vals.size()];
        for (int i = 0; i < vals.size(); i++) out[i] = vals.get(i);
        return out;
    }
}
