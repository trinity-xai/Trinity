package edu.jhuapl.trinity.javafx.javafx3d;

import java.util.Arrays;
import java.util.Objects;

/**
 * Lightweight primitive height field container (row-major float array).
 *
 * <p>Indexing: (x,y) where x in [0,width), y in [0,height) and
 * linear index = y * width + x.</p>
 */
public final class HeightField {

    private final int width;
    private final int height;
    private final float[] heights; // row-major, length = width * height

    /**
     * Creates a new HeightField that wraps the provided array.
     * The array is NOT copied.
     *
     * @param width  width in samples (x dimension), must be > 0
     * @param height height in samples (y dimension), must be > 0
     * @param heights row-major float array of length width*height
     */
    public HeightField(int width, int height, float[] heights) {
        if (width <= 0) throw new IllegalArgumentException("width must be > 0");
        if (height <= 0) throw new IllegalArgumentException("height must be > 0");
        Objects.requireNonNull(heights, "heights");
        int expected = Math.multiplyExact(width, height);
        if (heights.length != expected) {
            throw new IllegalArgumentException(
                "heights length mismatch: expected " + expected + " but got " + heights.length);
        }
        this.width = width;
        this.height = height;
        this.heights = heights;
    }

    /**
     * Allocates a new HeightField with zero-filled heights.
     */
    public static HeightField allocate(int width, int height) {
        int len = Math.multiplyExact(width, height);
        return new HeightField(width, height, new float[len]);
    }

    /**
     * Creates a new HeightField by copying the provided array.
     */
    public static HeightField copyOf(int width, int height, float[] heights) {
        Objects.requireNonNull(heights, "heights");
        return new HeightField(width, height, Arrays.copyOf(heights, heights.length));
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * Direct access to the underlying row-major array.
     * This is intentionally exposed for performance-sensitive operations.
     */
    public float[] data() {
        return heights;
    }

    public int size() {
        return heights.length;
    }

    public float get(int x, int y) {
        checkBounds(x, y);
        return heights[indexOf(x, y)];
    }

    public void set(int x, int y, float value) {
        checkBounds(x, y);
        heights[indexOf(x, y)] = value;
    }

    public int indexOf(int x, int y) {
        // no bounds check here; callers can use checkBounds if needed
        return y * width + x;
    }

    public void fill(float value) {
        Arrays.fill(heights, value);
    }

    /**
     * Returns a deep copy of this HeightField.
     */
    public HeightField copy() {
        return HeightField.copyOf(width, height, heights);
    }

    /**
     * Computes {min, max} across the height data.
     * @return float[2] where [0]=min, [1]=max. If empty (should not happen), returns {0,0}.
     */
    public float[] minMax() {
        if (heights.length == 0) return new float[]{0f, 0f};
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (float v : heights) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        return new float[]{min, max};
    }

    private void checkBounds(int x, int y) {
        if (x < 0 || x >= width) throw new IndexOutOfBoundsException("x out of range: " + x);
        if (y < 0 || y >= height) throw new IndexOutOfBoundsException("y out of range: " + y);
    }

    @Override
    public String toString() {
        return "HeightField{width=" + width + ", height=" + height + ", size=" + heights.length + "}";
    }
}