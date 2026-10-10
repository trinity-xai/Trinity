package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.javafx.javafx3d.Vert3D;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.DataUtils.HeightMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.List;

/**
 * Owns Hypersurface data-processing configuration and source-to-heightfield
 * transformations.
 *
 * <p>This controller deliberately contains no scene-graph, renderer, control,
 * or LOD scheduling behavior. It transforms source data owned by
 * {@link HypersurfaceSourceModel}, builds the processed HeightField pyramid,
 * and provides interpolation/sampling behavior used by the legacy mesh path.</p>
 *
 * @author Sean Phillips
 */
public final class HypersurfaceProcessingController {

    private static final int DEFAULT_MAX_RENDER_RESOLUTION = 2048;
    private static final int DEFAULT_MIN_RENDER_RESOLUTION = 512;
    private static final int TILED_LOD_LEVEL_COUNT = 5;

    private final HypersurfaceSourceModel sourceModel;

    private HeightMode heightMode = HeightMode.RAW;
    private boolean smoothingEnabled;
    private SurfaceUtils.Smoothing smoothingMethod = SurfaceUtils.Smoothing.GAUSSIAN;
    private int smoothingRadius = 2;
    private double gaussianSigma = 1.0;
    private SurfaceUtils.Interpolation interpolationMode = SurfaceUtils.Interpolation.NEAREST;
    private boolean toneEnabled;
    private SurfaceUtils.ToneMap toneOperator = SurfaceUtils.ToneMap.NONE;
    private double toneParam = 2.0;

    private int maxRenderResolution = DEFAULT_MAX_RENDER_RESOLUTION;
    private int minRenderResolution = DEFAULT_MIN_RENDER_RESOLUTION;

    public HypersurfaceProcessingController(HypersurfaceSourceModel sourceModel) {
        if (sourceModel == null) {
            throw new IllegalArgumentException("sourceModel cannot be null");
        }
        this.sourceModel = sourceModel;
    }

    public void setHeightMode(HeightMode heightMode) {
        this.heightMode = heightMode != null ? heightMode : HeightMode.RAW;
    }

    public void setSmoothingEnabled(boolean smoothingEnabled) {
        this.smoothingEnabled = smoothingEnabled;
    }

    public void setSmoothingMethod(SurfaceUtils.Smoothing smoothingMethod) {
        this.smoothingMethod = smoothingMethod != null
            ? smoothingMethod
            : SurfaceUtils.Smoothing.GAUSSIAN;
    }

    public void setSmoothingRadius(int smoothingRadius) {
        this.smoothingRadius = smoothingRadius;
    }

    public void setGaussianSigma(double gaussianSigma) {
        this.gaussianSigma = gaussianSigma;
    }

    public void setInterpolationMode(SurfaceUtils.Interpolation interpolationMode) {
        this.interpolationMode = interpolationMode != null
            ? interpolationMode
            : SurfaceUtils.Interpolation.NEAREST;
    }

    public void setToneEnabled(boolean toneEnabled) {
        this.toneEnabled = toneEnabled;
    }

    public void setToneOperator(SurfaceUtils.ToneMap toneOperator) {
        this.toneOperator = toneOperator != null
            ? toneOperator
            : SurfaceUtils.ToneMap.NONE;
    }

    public void setToneParam(double toneParam) {
        this.toneParam = toneParam;
    }

    public int getMaxRenderResolution() {
        return maxRenderResolution;
    }

    public boolean setMaxRenderResolution(int maxRenderResolution) {
        if (maxRenderResolution <= 0) {
            throw new IllegalArgumentException("maxRenderResolution must be > 0");
        }
        if (maxRenderResolution < minRenderResolution) {
            throw new IllegalArgumentException("maxRenderResolution must be >= minRenderResolution");
        }
        if (this.maxRenderResolution == maxRenderResolution) return false;
        this.maxRenderResolution = maxRenderResolution;
        return true;
    }

    public int getMinRenderResolution() {
        return minRenderResolution;
    }

    public boolean setMinRenderResolution(int minRenderResolution) {
        if (minRenderResolution <= 0) {
            throw new IllegalArgumentException("minRenderResolution must be > 0");
        }
        if (minRenderResolution > maxRenderResolution) {
            throw new IllegalArgumentException("minRenderResolution must be <= maxRenderResolution");
        }
        if (this.minRenderResolution == minRenderResolution) return false;
        this.minRenderResolution = minRenderResolution;
        return true;
    }

    public List<List<Double>> normalizeAndScale(List<List<Double>> grid, double userScale) {
        return DataUtils.normalizeAndScale(grid, heightMode, userScale);
    }

    /**
     * Build the capped raw image L0 directly from the supplied PixelReader.
     */
    public HeightField buildImageRawL0(PixelReader pixelReader,
                                       int startX,
                                       int startY,
                                       int sourceWidth,
                                       int sourceHeight) {
        return buildImageRawL0(pixelReader, startX, startY,
            sourceWidth, sourceHeight, maxRenderResolution);
    }

    /**
     * Rebuild the raw image L0 for the currently active image source after a
     * render-resolution change.
     *
     * @return true when the current image source was rebuilt
     */
    public boolean rebuildCurrentImageRawL0() {
        if (!sourceModel.isImageBackedSurface()
            || sourceModel.getSourceImage() == null
            || sourceModel.getImageSourceWidth() <= 0
            || sourceModel.getImageSourceHeight() <= 0) {
            return false;
        }
        Image image = sourceModel.getSourceImage();
        PixelReader reader = image.getPixelReader();
        if (reader == null) return false;

        sourceModel.setProcessingSourceHeightField(buildImageRawL0(
            reader,
            sourceModel.getImageSourceStartX(),
            sourceModel.getImageSourceStartY(),
            sourceModel.getImageSourceWidth(),
            sourceModel.getImageSourceHeight()));
        return true;
    }

    /**
     * Rebuild the processed L0 and lower-resolution pyramid from the current
     * authoritative source state.
     *
     * <p>The source model is updated atomically from the perspective of callers:
     * a new pyramid replaces the previous one, active LOD state is invalidated,
     * and stable world extents are recomputed. Renderer selection and LOD
     * scheduling remain the responsibility of Hypersurface3DPane.</p>
     *
     * @return true when a processed pyramid was produced
     */
    public boolean rebuildProcessedPyramid(int fallbackWidth,
                                           int fallbackHeight,
                                           double surfaceScale) {
        if (sourceModel.getProcessingSourceHeightField() == null) {
            if (sourceModel.getOriginalGrid() == null || sourceModel.getOriginalGrid().isEmpty()) {
                return false;
            }
            sourceModel.setProcessingSourceHeightField(
                SurfaceUtils.toHeightField(sourceModel.getOriginalGrid()));
        }

        HeightField l0Raw = sourceModel.isImageBackedSurface()
            ? sourceModel.getProcessingSourceHeightField()
            : SurfaceUtils.buildL0Raw(
                sourceModel.getProcessingSourceHeightField(), maxRenderResolution);

        SurfaceUtils.Smoothing smoothing = smoothingEnabled
            ? smoothingMethod
            : SurfaceUtils.Smoothing.NONE;
        SurfaceUtils.ToneMap toneMap = toneEnabled
            ? toneOperator
            : SurfaceUtils.ToneMap.NONE;

        HeightField l0Processed = SurfaceUtils.processL0(
            l0Raw,
            smoothing,
            smoothingRadius,
            1,
            gaussianSigma,
            toneMap,
            toneParam);

        sourceModel.setLodProcessedLevels(sourceModel.isImageBackedSurface()
            ? SurfaceUtils.buildPyramidLevels(l0Processed, TILED_LOD_LEVEL_COUNT)
            : SurfaceUtils.buildPyramid(l0Processed, minRenderResolution));

        sourceModel.recomputeBaseWorldExtents(
            fallbackWidth, fallbackHeight, surfaceScale);
        sourceModel.setActiveLod(-1, null);
        sourceModel.clearCurrentLodSurfScale();
        return true;
    }

    public String buildPyramidSummary() {
        StringBuilder summary = new StringBuilder("Hypersurface tiled LOD pyramid: ");
        List<HeightField> levels = sourceModel.getLodProcessedLevels();
        for (int i = 0; i < levels.size(); i++) {
            if (i > 0) summary.append(", ");
            HeightField level = levels.get(i);
            summary.append('L').append(i).append('=')
                .append(level.width()).append('x').append(level.height());
        }
        return summary.toString();
    }

    /**
     * Height lookup used by the legacy HyperSurfacePlotMesh callback.
     */
    public Number sampleHeight(Vert3D point,
                               boolean surfaceRender,
                               double surfaceScale,
                               double yScale) {
        if (!surfaceRender) {
            if (sourceModel.getDataGrid().isEmpty()) return 0.0;
            return findBlerpHeight(point, yScale);
        }

        HeightField activeHeightField = sourceModel.getActiveHeightField();
        if (activeHeightField == null) {
            if (sourceModel.getDataGrid().isEmpty()) return 0.0;
            return switch (interpolationMode) {
                case BILINEAR, BICUBIC -> {
                    double gx = point.xIndex + frac(point.getX() / Math.max(1.0, surfaceScale));
                    double gy = point.yIndex + frac(point.getY() / Math.max(1.0, surfaceScale));
                    gy = sourceModel.orientRowCoordinate(gy, sourceModel.getDataGrid().size());
                    yield SurfaceUtils.sample(
                        sourceModel.getDataGrid(), gx, gy, interpolationMode);
                }
                case NEAREST -> lookupPoint(point);
            };
        }

        return switch (interpolationMode) {
            case BILINEAR, BICUBIC -> {
                double scaleX = Double.isFinite(sourceModel.getCurrentLodSurfScaleX())
                    ? sourceModel.getCurrentLodSurfScaleX()
                    : surfaceScale;
                double scaleZ = Double.isFinite(sourceModel.getCurrentLodSurfScaleZ())
                    ? sourceModel.getCurrentLodSurfScaleZ()
                    : surfaceScale;
                double gx = point.xIndex + frac(point.getX() / Math.max(1.0, scaleX));
                double gy = point.yIndex + frac(point.getY() / Math.max(1.0, scaleZ));
                gy = sourceModel.orientRowCoordinate(gy, activeHeightField.height());
                yield SurfaceUtils.sample(activeHeightField, gx, gy, interpolationMode);
            }
            case NEAREST -> lookupPoint(point);
        };
    }

    private Number lookupPoint(Vert3D point) {
        HeightField activeHeightField = sourceModel.getActiveHeightField();
        if (activeHeightField != null) {
            int width = activeHeightField.width();
            int height = activeHeightField.height();
            if (point.yIndex < 0 || point.yIndex >= height
                || point.xIndex < 0 || point.xIndex >= width) {
                return 0.0;
            }
            int dataRow = sourceModel.orientRowIndex(point.yIndex, height);
            return (double) activeHeightField.data()[dataRow * width + point.xIndex];
        }

        List<List<Double>> dataGrid = sourceModel.getDataGrid();
        if (dataGrid.isEmpty()) return 0.0;
        if (point.yIndex < 0 || point.yIndex >= dataGrid.size()
            || point.xIndex < 0 || point.xIndex >= dataGrid.get(0).size()) {
            return 0.0;
        }
        int dataRow = sourceModel.orientRowIndex(point.yIndex, dataGrid.size());
        return dataGrid.get(dataRow).get(point.xIndex);
    }

    private Number findBlerpHeight(Vert3D point, double yScale) {
        HeightField activeHeightField = sourceModel.getActiveHeightField();
        if (activeHeightField != null) {
            int width = activeHeightField.width();
            int height = activeHeightField.height();
            if (width <= 0 || height <= 0) return 0.0;

            int x1Index = point.xIndex <= 0 ? 0 : point.xIndex - 1;
            if (x1Index >= width - 1) x1Index = width - 1;
            int x2Index = point.xIndex >= width - 1 ? width - 1 : point.xIndex + 1;

            int y1Index = point.yIndex <= 0 ? 0 : point.yIndex - 1;
            if (y1Index >= height - 1) y1Index = height - 1;
            int y2Index = point.yIndex >= height - 1 ? height - 1 : point.yIndex + 1;

            float[] data = activeHeightField.data();
            double c11 = data[y1Index * width + x1Index] * yScale;
            double c21 = data[y1Index * width + x2Index] * yScale;
            double c12 = data[y2Index * width + x1Index] * yScale;
            double c22 = data[y2Index * width + x2Index] * yScale;
            return quickBlerp(c11, c21, c12, c22, point.getX(), point.getY());
        }

        List<List<Double>> dataGrid = sourceModel.getDataGrid();
        if (dataGrid.isEmpty()) return 0.0;

        int width = dataGrid.get(0).size();
        int height = dataGrid.size();
        int x1Index = point.xIndex <= 0 ? 0 : point.xIndex - 1;
        if (x1Index >= width - 1) x1Index = width - 1;
        int x2Index = point.xIndex >= width - 1 ? width - 1 : point.xIndex + 1;
        int y1Index = point.yIndex <= 0 ? 0 : point.yIndex - 1;
        if (y1Index >= height - 1) y1Index = height - 1;
        int y2Index = point.yIndex >= height - 1 ? height - 1 : point.yIndex + 1;

        double c11 = dataGrid.get(y1Index).get(x1Index) * yScale;
        double c21 = dataGrid.get(y1Index).get(x2Index) * yScale;
        double c12 = dataGrid.get(y2Index).get(x1Index) * yScale;
        double c22 = dataGrid.get(y2Index).get(x2Index) * yScale;
        return quickBlerp(c11, c21, c12, c22, point.getX(), point.getY());
    }

    private static double frac(double value) {
        value = value - Math.floor(value);
        return value < 0 ? value + 1.0 : value;
    }

    private static double quickBlerp(double f1,
                                     double f2,
                                     double f3,
                                     double f4,
                                     double x,
                                     double y) {
        double xratio = x - Math.floor(x);
        double yratio = y - Math.floor(y);
        double f12 = f1 + (f2 - f1) * xratio;
        double f34 = f3 + (f4 - f3) * xratio;
        return f12 + (f34 - f12) * yratio;
    }

    private static HeightField buildImageRawL0(PixelReader pixelReader,
                                                int startX,
                                                int startY,
                                                int sourceWidth,
                                                int sourceHeight,
                                                int maxRender) {
        int[] dimensions = SurfaceUtils.computeL0Dimensions(
            sourceWidth, sourceHeight, maxRender);
        int targetWidth = dimensions[0];
        int targetHeight = dimensions[1];
        float[] output = new float[Math.multiplyExact(targetWidth, targetHeight)];

        if (targetWidth == sourceWidth && targetHeight == sourceHeight) {
            int index = 0;
            for (int y = 0; y < sourceHeight; y++) {
                int sourceY = startY + y;
                for (int x = 0; x < sourceWidth; x++) {
                    output[index++] = grayscale(
                        pixelReader.getArgb(startX + x, sourceY));
                }
            }
            return new HeightField(targetWidth, targetHeight, output);
        }

        double xRatio = sourceWidth / (double) targetWidth;
        double yRatio = sourceHeight / (double) targetHeight;
        for (int y = 0; y < targetHeight; y++) {
            double gy = (y + 0.5) * yRatio - 0.5;
            gy = Math.max(0.0, Math.min(sourceHeight - 1.0, gy));
            int y0 = (int) Math.floor(gy);
            int y1 = Math.min(sourceHeight - 1, y0 + 1);
            double ty = y0 == y1 ? 0.0 : gy - y0;

            for (int x = 0; x < targetWidth; x++) {
                double gx = (x + 0.5) * xRatio - 0.5;
                gx = Math.max(0.0, Math.min(sourceWidth - 1.0, gx));
                int x0 = (int) Math.floor(gx);
                int x1 = Math.min(sourceWidth - 1, x0 + 1);
                double tx = x0 == x1 ? 0.0 : gx - x0;

                double c00 = grayscale(pixelReader.getArgb(startX + x0, startY + y0));
                double c10 = grayscale(pixelReader.getArgb(startX + x1, startY + y0));
                double c01 = grayscale(pixelReader.getArgb(startX + x0, startY + y1));
                double c11 = grayscale(pixelReader.getArgb(startX + x1, startY + y1));
                double top = c00 + (c10 - c00) * tx;
                double bottom = c01 + (c11 - c01) * tx;
                output[y * targetWidth + x] = (float) (top + (bottom - top) * ty);
            }
        }
        return new HeightField(targetWidth, targetHeight, output);
    }

    private static float grayscale(int argb) {
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return (float) (((red + green + blue) / 3.0) / 255.0);
    }
}
