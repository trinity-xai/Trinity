package edu.jhuapl.trinity.utils.statistics;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;

/**
 * Small utility dialog helpers used by statistics panels.
 *
 * @author Sean Phillips
 */
public final class DialogUtils {

    private DialogUtils() { }

    /**
     * Shows a simple dialog to collect a custom reference vector.
     * Accepts numbers separated by commas, spaces, or newlines.
     *
     * @return a list of parsed doubles, or null if the user canceled/empty.
     */
    public static List<Double> showCustomVectorDialog() {
        Dialog<List<Double>> dialog = new Dialog<>();
        dialog.setTitle("Set Custom Reference Vector");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea ta = new TextArea();
        ta.setPromptText("Paste numbers separated by commas or spaces, e.g.\n0.12, -1.3, 2.5, 0.0");
        ta.setPrefRowCount(6);
        dialog.getDialogPane().setContent(ta);

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String text = ta.getText();
                return parseDoubles(text);
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    /**
     * Parses doubles from a free-form string (commas/spaces/newlines).
     */
    public static List<Double> parseDoubles(String s) {
        if (s == null || s.isBlank()) return null;
        String norm = s.replaceAll("[\\n\\t]", " ").replace(",", " ");
        String[] parts = norm.trim().split("\\s+");
        List<Double> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            try {
                out.add(Double.parseDouble(p));
            } catch (NumberFormatException ignore) {
                // skip tokens that aren't valid doubles
            }
        }
        return out.isEmpty() ? null : out;
    }
}
