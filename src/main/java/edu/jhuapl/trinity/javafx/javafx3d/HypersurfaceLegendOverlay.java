package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Passive 2D HUD that explains the current Hypersurface height and color semantics.
 * The overlay is intended to live in Trinity's desktopPane rather than inside the
 * 3D SubScene so it remains camera-independent and visually stable.
 */
public final class HypersurfaceLegendOverlay extends VBox {

    private static final double LEGEND_WIDTH = 252.0;
    private static final double PALETTE_WIDTH = 112.0;
    private static final double PALETTE_HEIGHT = 12.0;

    private final Label titleLabel = new Label("Surface");
    private final Label colorModeLabel = new Label("Color: -");
    private final Label colorMinimumLabel = new Label("-");
    private final Label colorMaximumLabel = new Label("-");
    private final ImageView paletteImageView = new ImageView(
        SurfaceColorPalette.createPaletteImage(256));
    private final HBox paletteRow = new HBox(5);
    private final Label heightModeLabel = new Label("Height: -");
    private final Label heightRangeLabel = new Label("Range: -");
    private final Label scaleLabel = new Label("Scale: -");
    private final Label orientationLabel = new Label("High Values: -");

    public HypersurfaceLegendOverlay() {
        setSpacing(4.0);
        setPadding(new Insets(8.0, 10.0, 8.0, 10.0));
        setPrefWidth(LEGEND_WIDTH);
        setMaxWidth(LEGEND_WIDTH);
        setMouseTransparent(true);

        Color backgroundColor = Color.color(0.02, 0.03, 0.05, 0.78);
        Color borderColor = Color.color(0.55, 0.80, 1.0, 0.62);
        CornerRadii radii = new CornerRadii(4.0);
        setBackground(new Background(new BackgroundFill(
            backgroundColor, radii, Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(
            borderColor,
            BorderStrokeStyle.SOLID,
            radii,
            new BorderWidths(1.0))));

        Font normalFont = Font.font("Consolas", 12.0);
        titleLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 13.0));
        titleLabel.setTextFill(Color.ALICEBLUE);
        colorModeLabel.setFont(normalFont);
        colorModeLabel.setTextFill(Color.WHITE);
        colorMinimumLabel.setFont(normalFont);
        colorMinimumLabel.setTextFill(Color.LIGHTGRAY);
        colorMaximumLabel.setFont(normalFont);
        colorMaximumLabel.setTextFill(Color.LIGHTGRAY);
        heightModeLabel.setFont(normalFont);
        heightModeLabel.setTextFill(Color.WHITE);
        heightRangeLabel.setFont(normalFont);
        heightRangeLabel.setTextFill(Color.LIGHTGRAY);
        scaleLabel.setFont(normalFont);
        scaleLabel.setTextFill(Color.LIGHTGRAY);
        orientationLabel.setFont(normalFont);
        orientationLabel.setTextFill(Color.LIGHTGRAY);

        paletteImageView.setFitWidth(PALETTE_WIDTH);
        paletteImageView.setFitHeight(PALETTE_HEIGHT);
        paletteImageView.setPreserveRatio(false);
        paletteImageView.setSmooth(false);
        paletteImageView.setMouseTransparent(true);

        paletteRow.setAlignment(Pos.CENTER_LEFT);
        paletteRow.getChildren().addAll(
            colorMinimumLabel,
            paletteImageView,
            colorMaximumLabel);

        HBox heightDetails = new HBox(12.0, scaleLabel, orientationLabel);
        heightDetails.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(
            titleLabel,
            colorModeLabel,
            paletteRow,
            heightModeLabel,
            heightRangeLabel,
            heightDetails);
    }

    public void update(HypersurfaceLegendState state) {
        if (state == null) return;

        colorModeLabel.setText("Color: " + safe(state.colorLabel()));
        heightModeLabel.setText("Height: " + safe(state.heightLabel()));
        heightRangeLabel.setText("Range: "
            + formatRange(state.heightMinimum(), state.heightMaximum()));
        scaleLabel.setText("Scale: " + formatNumber(state.heightScale()) + "x");
        orientationLabel.setText("High Values: "
            + (state.heightOrientation() == SurfaceHeightOrientation.HIGH_VALUES_DOWN
                ? "Down"
                : "Up"));

        boolean showPalette = state.numericColorRange()
            && Double.isFinite(state.colorMinimum())
            && Double.isFinite(state.colorMaximum());
        paletteRow.setVisible(showPalette);
        paletteRow.setManaged(showPalette);
        if (showPalette) {
            colorMinimumLabel.setText(formatNumber(state.colorMinimum()));
            colorMaximumLabel.setText(formatNumber(state.colorMaximum()));
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String formatRange(double minimum, double maximum) {
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum)) return "-";
        return formatNumber(minimum) + " to " + formatNumber(maximum);
    }

    private static String formatNumber(double value) {
        if (!Double.isFinite(value)) return "-";
        double absolute = Math.abs(value);
        if ((absolute > 0.0 && absolute < 0.001) || absolute >= 10000.0) {
            return String.format("%.3e", value);
        }
        if (absolute >= 100.0) return String.format("%.1f", value);
        if (absolute >= 10.0) return String.format("%.2f", value);
        return String.format("%.3f", value);
    }
}
