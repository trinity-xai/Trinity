package edu.jhuapl.trinity.utils.fun;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javax.imageio.ImageIO;

/**
 *
 * @author Sean Phillips
 */
public class FlarePatternFactory {

    public record BlurredDiskSpec(
            int diskSize, // Diameter of the oval
            Color color,
            double opacity,
            double offsetX, // Offset from canvas center
            double offsetY,
            double blurRadius
            ) {

    }
public record SunSlice(Color color, double thickness) {}

public static Image createRetrowaveSun(
        int width,
        int height,
        Color topColor,
        Color bottomColor,
        List<SunSlice> slices
) {
    Canvas canvas = new Canvas(width, height);
    GraphicsContext gc = canvas.getGraphicsContext2D();

    double centerX = width / 2.0;
    double centerY = height / 2.0;
    double radius = Math.min(width, height) / 2.0;
    double sunTop = centerY - radius;

    // Top gradient arc (upper semicircle)
    gc.setFill(new LinearGradient(
            0, sunTop, 0, sunTop + radius,
            false, CycleMethod.NO_CYCLE,
            new Stop(0, topColor),
            new Stop(1, bottomColor)
    ));
    gc.fillArc(centerX - radius, sunTop, radius * 2, radius * 2, 0, 180, ArcType.ROUND);

    // Start drawing slices just below midline
    double currentY = centerY + 2;

    for (SunSlice slice : slices) {
        double sliceCenterY = currentY + slice.thickness() / 2.0;
        double normalizedY = (sliceCenterY - centerY) / radius;
        normalizedY = Math.max(-1.0, Math.min(1.0, normalizedY));

        double arcWidth = Math.sqrt(1.0 - normalizedY * normalizedY) * radius;

        gc.setFill(slice.color());
        gc.fillRect(centerX - arcWidth, currentY, arcWidth * 2, slice.thickness());

        // Optional debug stroke
        // gc.setStroke(Color.RED);
        // gc.strokeRect(centerX - arcWidth, currentY, arcWidth * 2, slice.thickness());

        currentY += slice.thickness() + 2;
    }

    return FlarePatternFactory.snapshot(canvas);
}
    public static Image createHexGridImage(int size, Color color, double spacing, double lineWidth) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double hexHeight = spacing;
        double hexWidth = Math.sqrt(3) / 2 * hexHeight;
        double yOffset = hexHeight * 0.75;

        gc.setStroke(color);
        gc.setLineWidth(lineWidth);

        for (double y = 0; y < size + hexHeight; y += yOffset) {
            for (double x = (y / yOffset) % 2 == 0 ? 0 : hexWidth / 2; x < size + hexWidth; x += hexWidth) {
                drawHex(gc, x, y, hexHeight / 2);
            }
        }

        return snapshot(canvas);
    }
    private static void drawHex(GraphicsContext gc, double cx, double cy, double r) {
        gc.beginPath();
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i;
            double x = cx + r * Math.cos(angle);
            double y = cy + r * Math.sin(angle);
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.closePath();
        gc.stroke();
    }
    public static Image createAnalogGlitchImage(int width, int height, Color baseColor, int lineCount) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Random random = new Random();

        for (int i = 0; i < lineCount; i++) {
            double y = random.nextDouble() * height;
            double h = 1 + random.nextDouble() * 3;
            double xOffset = (random.nextDouble() - 0.5) * 10;

            gc.setFill(baseColor.deriveColor(0, 1, 1, 0.2 + random.nextDouble() * 0.5));
            gc.fillRect(xOffset, y, width, h);
        }

        return snapshot(canvas);
    }
    public static Image createPixelBurstImage(int size, Color color, int count) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double center = size / 2.0;
        Random random = new Random();

        gc.setFill(color);

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * random.nextDouble();
            double dist = center * random.nextDouble();
            int px = (int) (center + dist * Math.cos(angle));
            int py = (int) (center + dist * Math.sin(angle));
            int w = 2 + random.nextInt(4);
            int h = 2 + random.nextInt(4);
            gc.fillRect(px, py, w, h);
        }

        return snapshot(canvas);
    }
    public static Image createPlasmaRing(int size, Color baseColor, int arcCount, double noiseScale) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pw = image.getPixelWriter();

        double center = size / 2.0;
        double radius = center * 0.8;
        double ringThickness = radius * 0.2;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - center;
                double dy = y - center;
                double dist = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);

                double ringPos = Math.abs(dist - radius);
                if (ringPos < ringThickness) {
                    // Apply angular modulation
                    double modulation = Math.sin(angle * arcCount + Math.cos(dist * noiseScale));
                    modulation = 0.5 + 0.5 * modulation;

                    double alpha = 1.0 - (ringPos / ringThickness);
                    alpha *= modulation;
                    alpha = Math.min(1.0, Math.max(0.0, alpha));

                    Color c = baseColor.deriveColor(0, 1, 1, alpha);
                    pw.setColor(x, y, c);
                } else {
                    pw.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        return image;
    }
    public static Image createRotatingSpikeImage(int size, int spikes, Color color) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double center = size / 2.0;
        gc.setStroke(color);
        gc.setLineWidth(1.5);
        gc.setLineCap(StrokeLineCap.ROUND);

        for (int i = 0; i < spikes; i++) {
            double angle = 2 * Math.PI * i / spikes;
            double x = center + Math.cos(angle) * center;
            double y = center + Math.sin(angle) * center;
            gc.strokeLine(center, center, x, y);
        }

        canvas.setEffect(new GaussianBlur(size * 0.05));
        return snapshot(canvas);
    }
    public static Image createCoronaRing(int size, Color color, int waves) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pw = image.getPixelWriter();
        double center = size / 2.0;
        double baseRadius = center * 0.7;
        double thickness = baseRadius * 0.2;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - center;
                double dy = y - center;
                double dist = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);
                double wave = Math.sin(angle * waves) * 4.0;

                double ringDist = Math.abs(dist - (baseRadius + wave));
                if (ringDist < thickness) {
                    double alpha = 1.0 - (ringDist / thickness);
                    pw.setColor(x, y, color.deriveColor(0, 1, 1, alpha));
                } else {
                    pw.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        return image;
    }
    public static Image createDiskImage(int size, Color color) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(color);
        gc.fillOval(0, 0, size, size);

        return snapshot(canvas);
    }
    public static Image createBlurredDiskImage(int canvasSize, int diskSize, Color color, double opacity, double blurRadius, int blurIterations) {
        return createBlurredDiskImage(canvasSize, diskSize, color, opacity, 0, 0, blurRadius);
    }
    public static Image createBlurredDiskImage(int canvasSize, int diskSize, Color color,
        double opacity, double offsetX, double offsetY, double blurRadius) {
        Canvas canvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double center = canvasSize / 2.0;

        double x = center - diskSize / 2.0 - blurRadius + offsetX;
        double y = center - diskSize / 2.0 - blurRadius + offsetY;

        gc.setFill(color.deriveColor(0, 1, 1, opacity));
        gc.fillOval(x, y, diskSize, diskSize);

        canvas.setEffect(new GaussianBlur(blurRadius));

        return snapshot(canvas);
    }
    public static Image createCompositeBlurredDisks(List<BlurredDiskSpec> specs, int canvasSize) {
        Canvas canvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        for (BlurredDiskSpec spec : specs) {
            // Use the new fully parameterized method
            Image disk = createBlurredDiskImage(
                    canvasSize,
                    spec.diskSize(),
                    spec.color(),
                    spec.opacity(),
                    spec.offsetX(),
                    spec.offsetY(),
                    spec.blurRadius()
            );

            gc.drawImage(disk, 0, 0); // Disk already centered in image
        }

        return snapshot(canvas);
    }
    public static Image createStarImage(int size, Color color) {
        return createStarImage(size, color, 8, 0);
    }
    public static Image createStarImage(int size, Color color, int points, double rotation) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(color);
        gc.setStroke(color);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineWidth(0.5);

        double center = size / 2.0;

        // Outer and inner radii
        double outerRadius = center * 0.9;
        double innerRadius = center * 0.1;

        // Draw base star (4-point cross)
        drawStar(gc, center, center, outerRadius, innerRadius, points, rotation);

        // Draw rotated + smaller overlay
        gc.save();
        gc.translate(center, center);
        gc.rotate(45);
        gc.scale(0.7, 0.7);
        drawStar(gc, 0, 0, outerRadius, innerRadius, points, 0);
        gc.restore();

        return snapshot(canvas);
    }
    /**
     * Draws a star shape using the given GraphicsContext.
     *
     * @param gc the GraphicsContext to draw with
     * @param centerX center X coordinate
     * @param centerY center Y coordinate
     * @param outerR radius to the star's outer tips
     * @param innerR radius to the inner valleys between tips
     * @param points number of points (minimum 2)
     * @param rotation initial rotation in degrees (e.g. 0 or 45)
     */
    public static void drawStar(GraphicsContext gc, double centerX, double centerY,
            double outerR, double innerR, int points, double rotation) {
        if (points < 2) {
            return;
        }

        double angleStep = Math.PI / points; // half angle: outer-inner-outer...
        double startAngle = Math.toRadians(rotation);

        gc.beginPath();
        for (int i = 0; i < points * 2; i++) {
            double r = (i % 2 == 0) ? outerR : innerR;
            double angle = startAngle + i * angleStep;
            double x = centerX + r * Math.cos(angle);
            double y = centerY + r * Math.sin(angle);
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.closePath();
        gc.fill();
    }
    public static Image createRaysImage(int size, int rayCount, Color color) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double center = size / 2.0;
        double r1 = center * 0.9;
        double r2 = center * 0.1;

        for (int i = 0; i < rayCount; i++) {
            double angle = i * 360.0 / rayCount;

            gc.save();
            gc.translate(center, center);
            gc.rotate(angle);
            gc.setFill(new LinearGradient(0, 0, 0, -r1, false, CycleMethod.NO_CYCLE,
                    new Stop(0, color),
                    new Stop(1, Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0))));
            gc.fillPolygon(
                    new double[]{0, -r2, r2},
                    new double[]{0, -r1, -r1},
                    3
            );
            gc.restore();
        }

        return snapshot(canvas);
    }
    public static Image createHaloImage(int size, Color color, double alphaFalloff) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double center = size / 2.0;
        double maxR = center * 0.9;

        for (double r = 0; r <= maxR; r += 1.0) {
            double alpha = 1.0 - Math.pow(r / maxR, alphaFalloff);
            gc.setStroke(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            gc.strokeOval(center - r, center - r, r * 2, r * 2);
        }

        return snapshot(canvas);
    }
    public static Image createRainbowImage(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        double center = size / 2.0;
        double r1 = center * 0.7;
        double r2 = center * 0.9;

        for (double r = r1; r <= r2; r++) {
            float hue = (float) ((r - r1) / (r2 - r1));
            Color color = Color.hsb(hue * 360, 1.0, 1.0);
            gc.setStroke(color);
            gc.strokeOval(center - r, center - r, r * 2, r * 2);
        }

        return snapshot(canvas);
    }
    
    public static Image snapshot(Canvas canvas, int x, int y, int w, int h) {
        WritableImage full = canvas.snapshot(null, null);
        return new WritableImage(full.getPixelReader(), x, y, w, h);
    }
    private static Image snapshot(Canvas canvas) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight()));
    }
    public static void saveImage(Image image, String path) {
        WritableImage writableImage = new WritableImage((int) image.getWidth(), (int) image.getHeight());
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        new ImageView(image).snapshot(params, writableImage);
        File file = new File(path);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
