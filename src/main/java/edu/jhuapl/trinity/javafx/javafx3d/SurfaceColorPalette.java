package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Shared color palette used by height-based Hypersurface rendering and its legend.
 */
public final class SurfaceColorPalette {

    private SurfaceColorPalette() {
    }

    public static Color colorAt(double normalized) {
        double value = Math.max(0.0, Math.min(1.0, normalized));
        if (value <= 0.0) return Color.BLACK;
        if (value >= 1.0) return Color.WHITE;
        return Color.hsb(360.0 * value, 1.0, 1.0, 1.0);
    }

    public static WritableImage createPaletteImage(int colors) {
        if (colors < 2) {
            throw new IllegalArgumentException("colors must be >= 2");
        }
        WritableImage palette = new WritableImage(colors, 1);
        PixelWriter writer = palette.getPixelWriter();
        for (int i = 0; i < colors; i++) {
            double normalized = (double) i / (double) (colors - 1);
            writer.setColor(i, 0, colorAt(normalized));
        }
        return palette;
    }
}
