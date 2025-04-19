package edu.jhuapl.trinity.javafx.components;

import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * @author Sean Phillips
 */
public class Projector {
    IntBuffer buffer;
    int[] pixels;
    PixelBuffer<IntBuffer> pixelBuffer;
    public WritableImage image;
    public int width, height;
    public int xCoordinate, yCoordinate;

    public Projector(int width, int height) {
        this(width, height, 0, 0);
    }

    public Projector(int width, int height, int xCoordinate, int yCoordinate) {
        this.width = width;
        this.height = height;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        buffer = IntBuffer.allocate(width * height);
        pixels = buffer.array();
        pixelBuffer = new PixelBuffer<>(width, height, buffer, PixelFormat.getIntArgbPreInstance());
        image = new WritableImage(pixelBuffer);
    }

    private int transformX(double xInput, double minX) {
        return (int) (xInput - minX) / width;
    }

    private int transformY(double yInput, double minY) {
        return (int) (yInput - minY) / width;
    }

    public void project(double[] xValues, double[] yValues) {
//        double maxX = Arrays.stream(xValues).max().getAsDouble();
        double minX = Arrays.stream(xValues).min().getAsDouble();
//        double maxY = Arrays.stream(yValues).max().getAsDouble();
        double minY = Arrays.stream(yValues).min().getAsDouble();

        // draw a single _blue_ pixel
        int alpha = 255;
        int red = 0;
        int green = 0;
        int blue = 255;
        int colorARGB = alpha << 24 | red << 16 | green << 8 | blue;
        int transformedX = 0;
        int transformedY = 0;
        for (int x = 0; x < xValues.length; x++) {
            transformedX = transformX(xValues[x], minX);
            for (int y = 0; y < yValues.length; y++) {
                transformedY = transformY(yValues[y], minY);
                pixels[(transformedX % width) + (transformedY * width)] = colorARGB;
            }
        }
        // tell the buffer that the entire area needs redrawing
        pixelBuffer.updateBuffer(b -> null);
    }
}
