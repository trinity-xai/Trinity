package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.ShapleyVector;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the data/source state represented by a Hypersurface.
 *
 * <p>This model deliberately contains no renderer, scene-graph, control, or LOD
 * scheduling behavior. It tracks the source collections/image region, primitive
 * HeightField state derived from that source, source/render dimensions, stable
 * world extents, and the row/height orientation semantics shared by renderers and
 * interaction code.</p>
 *
 * @author Sean Phillips
 */
public final class HypersurfaceSourceModel {

    private final List<ShapleyVector> shapleyVectors = new ArrayList<>();
    private final List<FeatureVector> featureVectors = new ArrayList<>();
    private final List<List<Double>> dataGrid = new ArrayList<>();
    private final List<String> featureLabels = new ArrayList<>();
    private List<List<Double>> originalGrid = new ArrayList<>();

    private final ObjectProperty<SurfaceRowOrientation> rowOrientation =
        new SimpleObjectProperty<>(SurfaceRowOrientation.FIRST_ROW_NEAR);
    private final ObjectProperty<SurfaceHeightOrientation> heightOrientation =
        new SimpleObjectProperty<>(SurfaceHeightOrientation.HIGH_VALUES_UP);
    private Image sourceImage;
    private int imageSourceStartX;
    private int imageSourceStartY;
    private int imageSourceWidth;
    private int imageSourceHeight;
    private boolean imageBackedSurface;

    private HeightField processingSourceHeightField;
    private List<HeightField> lodProcessedLevels = List.of();
    private int activeLodIndex = -1;
    private HeightField activeHeightField;

    private double baseWorldWidth = Double.NaN;
    private double baseWorldDepth = Double.NaN;
    private double currentLodSurfScaleX = Double.NaN;
    private double currentLodSurfScaleZ = Double.NaN;

    private final double imageWorldExtentReferenceScale;
    private double nominalImageWorldExtent;

    public HypersurfaceSourceModel(int defaultXWidth, int defaultSurfaceScale) {
        imageWorldExtentReferenceScale = Math.max(1.0e-6, defaultSurfaceScale);
        nominalImageWorldExtent = defaultXWidth * (double) defaultSurfaceScale;
    }

    public List<ShapleyVector> getShapleyVectors() {
        return shapleyVectors;
    }

    public List<FeatureVector> getFeatureVectors() {
        return featureVectors;
    }


    public List<List<Double>> getDataGrid() {
        return dataGrid;
    }

    public List<String> getFeatureLabels() {
        return featureLabels;
    }


    public List<List<Double>> getOriginalGrid() {
        return originalGrid;
    }

    public void clearOriginalGrid() {
        originalGrid.clear();
    }

    public boolean captureDataGridAsSource() {
        boolean wasImageBacked = imageBackedSurface;
        imageBackedSurface = false;
        if (dataGrid.isEmpty()) {
            originalGrid = new ArrayList<>();
            clearPrimitiveSourceState();
            return wasImageBacked;
        }
        originalGrid = deepCopyGrid(dataGrid);
        processingSourceHeightField = null;
        clearImageRegion();
        return wasImageBacked;
    }

    public void activateImageSource(Image image,
                                    int startX,
                                    int startY,
                                    int width,
                                    int height,
                                    HeightField rawL0) {
        sourceImage = image;
        setImageSourceRegion(startX, startY, width, height);
        processingSourceHeightField = rawL0;
        featureVectors.clear();
        dataGrid.clear();
        originalGrid.clear();
        imageBackedSurface = true;
        lodProcessedLevels = List.of();
        activeLodIndex = -1;
        activeHeightField = null;
        invalidateWorldExtents();
        clearCurrentLodSurfScale();
    }

    public Image getSourceImage() {
        return sourceImage;
    }

    public void setSourceImage(Image sourceImage) {
        this.sourceImage = sourceImage;
    }



    public int getImageSourceStartX() {
        return imageSourceStartX;
    }

    public int getImageSourceStartY() {
        return imageSourceStartY;
    }

    public int getImageSourceWidth() {
        return imageSourceWidth;
    }

    public int getImageSourceHeight() {
        return imageSourceHeight;
    }

    public void setImageSourceRegion(int startX, int startY, int width, int height) {
        imageSourceStartX = startX;
        imageSourceStartY = startY;
        imageSourceWidth = Math.max(0, width);
        imageSourceHeight = Math.max(0, height);
    }

    public boolean isImageBackedSurface() {
        return imageBackedSurface;
    }


    public HeightField getProcessingSourceHeightField() {
        return processingSourceHeightField;
    }

    public void setProcessingSourceHeightField(HeightField processingSourceHeightField) {
        this.processingSourceHeightField = processingSourceHeightField;
    }

    public List<HeightField> getLodProcessedLevels() {
        return lodProcessedLevels;
    }

    public void setLodProcessedLevels(List<HeightField> lodProcessedLevels) {
        this.lodProcessedLevels = lodProcessedLevels != null ? lodProcessedLevels : List.of();
    }

    public int getActiveLodIndex() {
        return activeLodIndex;
    }


    public HeightField getActiveHeightField() {
        return activeHeightField;
    }


    public void setActiveLod(int activeLodIndex, HeightField activeHeightField) {
        this.activeLodIndex = activeLodIndex;
        this.activeHeightField = activeHeightField;
    }

    public double getBaseWorldWidth() {
        return baseWorldWidth;
    }

    public double getBaseWorldDepth() {
        return baseWorldDepth;
    }

    public void invalidateWorldExtents() {
        baseWorldWidth = Double.NaN;
        baseWorldDepth = Double.NaN;
    }

    public double getCurrentLodSurfScaleX() {
        return currentLodSurfScaleX;
    }

    public double getCurrentLodSurfScaleZ() {
        return currentLodSurfScaleZ;
    }

    public void setCurrentLodSurfScale(double scaleX, double scaleZ) {
        currentLodSurfScaleX = scaleX;
        currentLodSurfScaleZ = scaleZ;
    }

    public void clearCurrentLodSurfScale() {
        currentLodSurfScaleX = Double.NaN;
        currentLodSurfScaleZ = Double.NaN;
    }

    public double getRenderScaleX(double fallbackScale) {
        return Double.isFinite(currentLodSurfScaleX) ? currentLodSurfScaleX : fallbackScale;
    }

    public double getRenderScaleZ(double fallbackScale) {
        return Double.isFinite(currentLodSurfScaleZ) ? currentLodSurfScaleZ : fallbackScale;
    }

    public int getRenderL0Width(int fallbackWidth) {
        return !lodProcessedLevels.isEmpty()
            ? lodProcessedLevels.get(0).width()
            : getRenderWidth(fallbackWidth);
    }

    public int getRenderL0Height(int fallbackHeight) {
        return !lodProcessedLevels.isEmpty()
            ? lodProcessedLevels.get(0).height()
            : getRenderHeight(fallbackHeight);
    }

    public int getSourceWidth(int fallbackWidth) {
        if (imageBackedSurface && imageSourceWidth > 0) return imageSourceWidth;
        if (processingSourceHeightField != null) return processingSourceHeightField.width();
        if (!originalGrid.isEmpty()) return originalGrid.get(0).size();
        if (!dataGrid.isEmpty()) return dataGrid.get(0).size();
        return Math.max(1, fallbackWidth);
    }

    public int getSourceHeight(int fallbackHeight) {
        if (imageBackedSurface && imageSourceHeight > 0) return imageSourceHeight;
        if (processingSourceHeightField != null) return processingSourceHeightField.height();
        if (!originalGrid.isEmpty()) return originalGrid.size();
        if (!dataGrid.isEmpty()) return dataGrid.size();
        return Math.max(1, fallbackHeight);
    }

    public int getRenderWidth(int fallbackWidth) {
        return activeHeightField != null ? activeHeightField.width() : Math.max(1, fallbackWidth);
    }

    public int getRenderHeight(int fallbackHeight) {
        return activeHeightField != null ? activeHeightField.height() : Math.max(1, fallbackHeight);
    }

    public double getWorldWidth(int fallbackWidth, int fallbackHeight, double surfaceScale) {
        if (Double.isFinite(baseWorldWidth)) return baseWorldWidth;
        return calculateWorldWidth(
            getSourceWidth(fallbackWidth), getSourceHeight(fallbackHeight), surfaceScale);
    }

    public double getWorldDepth(int fallbackWidth, int fallbackHeight, double surfaceScale) {
        if (Double.isFinite(baseWorldDepth)) return baseWorldDepth;
        return calculateWorldDepth(
            getSourceWidth(fallbackWidth), getSourceHeight(fallbackHeight), surfaceScale);
    }

    public void recomputeBaseWorldExtents(int fallbackWidth,
                                          int fallbackHeight,
                                          double surfaceScale) {
        int sourceWidth = getSourceWidth(fallbackWidth);
        int sourceHeight = getSourceHeight(fallbackHeight);
        baseWorldWidth = calculateWorldWidth(sourceWidth, sourceHeight, surfaceScale);
        baseWorldDepth = calculateWorldDepth(sourceWidth, sourceHeight, surfaceScale);
    }

    public double getNominalImageWorldExtent() {
        return nominalImageWorldExtent;
    }

    public void setNominalImageWorldExtent(double nominalImageWorldExtent) {
        if (!(nominalImageWorldExtent > 0.0) || !Double.isFinite(nominalImageWorldExtent)) {
            throw new IllegalArgumentException("nominalImageWorldExtent must be finite and > 0");
        }
        this.nominalImageWorldExtent = nominalImageWorldExtent;
    }

    public SurfaceRowOrientation getRowOrientation() {
        return rowOrientation.get();
    }

    public void setRowOrientation(SurfaceRowOrientation orientation) {
        rowOrientation.set(orientation != null
            ? orientation
            : SurfaceRowOrientation.FIRST_ROW_NEAR);
    }

    public ObjectProperty<SurfaceRowOrientation> rowOrientationProperty() {
        return rowOrientation;
    }

    public SurfaceHeightOrientation getHeightOrientation() {
        return heightOrientation.get();
    }

    public void setHeightOrientation(SurfaceHeightOrientation orientation) {
        heightOrientation.set(orientation != null
            ? orientation
            : SurfaceHeightOrientation.HIGH_VALUES_UP);
    }

    public ObjectProperty<SurfaceHeightOrientation> heightOrientationProperty() {
        return heightOrientation;
    }


    public int orientRowIndex(int logicalRow, int rowCount) {
        if (rowCount <= 0) return 0;
        int clamped = Math.max(0, Math.min(logicalRow, rowCount - 1));
        return getRowOrientation() == SurfaceRowOrientation.FIRST_ROW_FAR
            ? rowCount - 1 - clamped
            : clamped;
    }

    public double orientRowCoordinate(double logicalRow, int rowCount) {
        if (rowCount <= 1) return 0.0;
        double clamped = Math.max(0.0, Math.min(logicalRow, rowCount - 1.0));
        return getRowOrientation() == SurfaceRowOrientation.FIRST_ROW_FAR
            ? (rowCount - 1.0) - clamped
            : clamped;
    }

    public double getActiveRenderValue(int row, int column) {
        if (activeHeightField == null) return 0.0;
        int r = orientRowIndex(row, activeHeightField.height());
        int c = Math.max(0, Math.min(column, activeHeightField.width() - 1));
        return activeHeightField.get(c, r);
    }

    public List<Double> getActiveRenderRow(int row) {
        if (activeHeightField == null) return List.of();
        int r = orientRowIndex(row, activeHeightField.height());
        int width = activeHeightField.width();
        float[] values = activeHeightField.data();
        List<Double> out = new ArrayList<>(width);
        int offset = r * width;
        for (int x = 0; x < width; x++) out.add((double) values[offset + x]);
        return out;
    }

    public Double[] getActiveRenderColumn(int column) {
        if (activeHeightField == null) return new Double[0];
        int c = Math.max(0, Math.min(column, activeHeightField.width() - 1));
        int width = activeHeightField.width();
        int height = activeHeightField.height();
        float[] values = activeHeightField.data();
        Double[] out = new Double[height];
        for (int visualRow = 0; visualRow < height; visualRow++) {
            int sourceRow = orientRowIndex(visualRow, height);
            out[visualRow] = (double) values[sourceRow * width + c];
        }
        return out;
    }

    /** Clears primitive/image/LOD state without changing the source collections. */
    public void clearPrimitiveSourceState() {
        processingSourceHeightField = null;
        clearImageRegion();
        lodProcessedLevels = List.of();
        activeLodIndex = -1;
        activeHeightField = null;
        invalidateWorldExtents();
        clearCurrentLodSurfScale();
    }

    /** Clears the previous image reference and all render-side state derived from it. */
    public void releaseImageBackedStateForReplacement() {
        clearPrimitiveSourceState();
        sourceImage = null;
    }

    public void resetLodDataState() {
        imageBackedSurface = false;
        clearPrimitiveSourceState();
    }

    private void clearImageRegion() {
        imageSourceStartX = 0;
        imageSourceStartY = 0;
        imageSourceWidth = 0;
        imageSourceHeight = 0;
    }

    private double calculateWorldWidth(int sourceWidth, int sourceHeight, double surfaceScale) {
        if (!imageBackedSurface) return sourceWidth * surfaceScale;
        if (sourceWidth <= 0 || sourceHeight <= 0) return nominalImageWorldExtent;
        double longAxis = imageWorldLongAxisExtent(surfaceScale);
        if (sourceWidth >= sourceHeight) return longAxis;
        return longAxis * sourceWidth / (double) sourceHeight;
    }

    private double calculateWorldDepth(int sourceWidth, int sourceHeight, double surfaceScale) {
        if (!imageBackedSurface) return sourceHeight * surfaceScale;
        if (sourceWidth <= 0 || sourceHeight <= 0) return nominalImageWorldExtent;
        double longAxis = imageWorldLongAxisExtent(surfaceScale);
        if (sourceHeight >= sourceWidth) return longAxis;
        return longAxis * sourceHeight / (double) sourceWidth;
    }

    private double imageWorldLongAxisExtent(double surfaceScale) {
        return nominalImageWorldExtent * (surfaceScale / imageWorldExtentReferenceScale);
    }

    private static List<List<Double>> deepCopyGrid(List<List<Double>> src) {
        List<List<Double>> out = new ArrayList<>(src.size());
        for (List<Double> row : src) out.add(new ArrayList<>(row));
        return out;
    }
}
