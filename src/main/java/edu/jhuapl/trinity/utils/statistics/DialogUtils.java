package edu.jhuapl.trinity.utils.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Simple dialog utilities for entering custom vectors or precomputed scalars.
 *
 * Features:
 * - Supports single or multiple vectors (multi-line paste).
 * - Pads/truncates vectors to an expected dimension if provided.
 * - Parses scalars (score or score+info) for direct PDF/CDF charts.
 * - Shows warnings for padding, truncation, or invalid tokens.
 *
 * @author Sean Phillips
 */
public final class DialogUtils {

    private DialogUtils() { }

    // ----------------- Vector Input -----------------

    /** Show a dialog that collects a single vector (no enforced dimension). */
    public static List<Double> showCustomVectorDialog() {
        List<List<Double>> all = showCustomVectorsDialog(null);
        if (all == null || all.isEmpty()) return null;
        return all.get(0);
    }

    /**
     * Show a dialog that collects a single vector with an expected dimension.
     * Pads with 0.0 or truncates to match expectedDim. Shows warnings if applied.
     */
    public static List<Double> showCustomVectorDialog(Integer expectedDim) {
        if (expectedDim == null) {
            return showCustomVectorDialog();
        }
        List<List<Double>> all = showCustomVectorsDialog(expectedDim);
        if (all == null || all.isEmpty()) return null;
        return all.get(0);
    }

    /**
     * Show a dialog that collects one or more vectors (multi-row).
     * Pads/truncates each row to expectedDim if provided.
     */
    public static List<List<Double>> showCustomVectorsDialog(Integer expectedDim) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enter Custom Vector(s)");
        dialog.setHeaderText("Paste one vector per line. Use commas, semicolons, or spaces.");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Examples:\n" +
                "0.12, -1.3, 2.5\n" +
                "1.0 2.0 3.0\n" +
                "0.5; -1.2; 3");
        textArea.setPrefColumnCount(60);
        textArea.setPrefRowCount(10);

        Label hint = new Label(expectedDim == null
                ? "No fixed dimension enforced."
                : ("Expected dimension: " + expectedDim + "  (rows will be padded/truncated)"));

        VBox content = new VBox(8, hint, textArea);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("OK", ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        ButtonType result = dialog.showAndWait().orElse(cancelType);
        if (result != okType) return null;

        String raw = textArea.getText();
        ParseOutcome parsed = parseMultiRow(raw, expectedDim);

        if (!parsed.warnings.isEmpty()) {
            showWarnings(parsed.warnings, "Vector Input Warnings",
                    "Some rows required padding/truncation or had invalid tokens.");
        }

        return parsed.vectors.isEmpty() ? null : parsed.vectors;
    }

    // ----------------- Scalars Input -----------------

    /** Result container for scalar inputs (score + optional info fraction). */
    public static final class ScalarInputResult {
        public final List<Double> scores = new ArrayList<>();
        public final List<Double> infos  = new ArrayList<>(); // may be empty
    }

    /**
     * Show a dialog to paste precomputed scalars.
     * Each line: "score" or "score, infoPercent".
     * Values are clamped to [0,1]. Warnings shown for issues.
     */
    public static ScalarInputResult showScalarSamplesDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Enter Precomputed Scalars");
        dialog.setHeaderText("One per line: score or score, infoPercent (both in [0,1]).");

        TextArea textArea = new TextArea();
        textArea.setPromptText("Examples:\n" +
                "0.73\n" +
                "0.91, 0.62\n" +
                "0.50 0.25\n" +
                "0.33; 1.0");
        textArea.setPrefColumnCount(60);
        textArea.setPrefRowCount(12);

        VBox content = new VBox(8,
                new Label("Separators: comma, semicolon, or whitespace."),
                textArea);
        content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("OK", ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        ButtonType result = dialog.showAndWait().orElse(cancelType);
        if (result != okType) return null;

        String raw = textArea.getText();
        return parseScalarPairs(raw);
    }

    // ----------------- Parsing helpers -----------------

    private static final class ParseOutcome {
        final List<List<Double>> vectors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
    }

    private static ParseOutcome parseMultiRow(String raw, Integer expectedDim) {
        ParseOutcome out = new ParseOutcome();
        if (raw == null || raw.trim().isEmpty()) return out;

        String[] lines = raw.split("\\R");
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            // Normalize separators
            String normalized = trimmed.replace(',', ' ').replace(';', ' ');
            String[] tokens = normalized.trim().split("\\s+");

            List<Double> row = new ArrayList<>();
            int skippedTokens = 0;
            for (String tok : tokens) {
                if (tok.isEmpty()) continue;
                try {
                    row.add(Double.parseDouble(tok));
                } catch (NumberFormatException nfe) {
                    skippedTokens++;
                }
            }

            if (row.isEmpty()) {
                out.warnings.add("Line " + lineNumber + ": no valid numeric tokens; skipped.");
                continue;
            }

            // Enforce dimension
            if (expectedDim != null && expectedDim > 0) {
                if (row.size() < expectedDim) {
                    int missing = expectedDim - row.size();
                    for (int i = 0; i < missing; i++) row.add(0.0);
                    out.warnings.add("Line " + lineNumber + ": padded with " + missing + " zero(s).");
                } else if (row.size() > expectedDim) {
                    int extra = row.size() - expectedDim;
                    while (row.size() > expectedDim) row.remove(row.size() - 1);
                    out.warnings.add("Line " + lineNumber + ": truncated " + extra + " value(s).");
                }
            }

            if (skippedTokens > 0) {
                out.warnings.add("Line " + lineNumber + ": skipped " + skippedTokens + " unparsable token(s).");
            }

            out.vectors.add(row);
        }
        return out;
    }

    private static ScalarInputResult parseScalarPairs(String raw) {
        ScalarInputResult out = new ScalarInputResult();
        List<String> warnings = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;

        String[] lines = raw.split("\\R");
        int lineNo = 0;

        for (String line : lines) {
            lineNo++;
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            String normalized = trimmed.replace(',', ' ').replace(';', ' ');
            String[] tokens = normalized.trim().split("\\s+");

            Double score = null, info = null;
            try { if (tokens.length >= 1) score = Double.parseDouble(tokens[0]); } catch (NumberFormatException ignore) {}
            try { if (tokens.length >= 2) info  = Double.parseDouble(tokens[1]); } catch (NumberFormatException ignore) {}

            if (score == null) {
                warnings.add("Line " + lineNo + ": invalid score; skipped.");
                continue;
            }

            // Clamp
            if (score < 0.0 || score > 1.0) {
                warnings.add("Line " + lineNo + ": score " + score + " clamped to [0,1].");
                score = Math.max(0.0, Math.min(1.0, score));
            }
            if (info != null && (info < 0.0 || info > 1.0)) {
                warnings.add("Line " + lineNo + ": info " + info + " clamped to [0,1].");
                info = Math.max(0.0, Math.min(1.0, info));
            }

            out.scores.add(score);
            if (info != null) out.infos.add(info);
        }

        if (!warnings.isEmpty()) {
            showWarnings(warnings, "Scalar Input Warnings",
                    "Some rows had invalid values or were clamped to [0,1].");
        }
        return out;
    }

    // ----------------- Warning helper -----------------

    private static void showWarnings(List<String> warnings, String title, String header) {
        if (warnings == null || warnings.isEmpty()) return;

        final int MAX_LINES = 12;
        StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < Math.min(MAX_LINES, warnings.size()); i++) {
            sj.add(warnings.get(i));
        }
        if (warnings.size() > MAX_LINES) {
            sj.add("…and " + (warnings.size() - MAX_LINES) + " more.");
        }

        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        TextArea area = new TextArea(sj.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(Math.min(MAX_LINES, warnings.size()));
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }
}
