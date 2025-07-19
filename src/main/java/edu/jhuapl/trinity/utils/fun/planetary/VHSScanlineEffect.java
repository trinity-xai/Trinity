package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.VHSScanline;

/**
 *
 * @author Sean Phillips
 */
import javafx.scene.Node;

public class VHSScanlineEffect implements PlanetaryEffect {

    private VHSScanline scanline;
    private double width = 512;
    private double height = 512;
    private boolean autoSize = false;
    private int scanlineSpacing = 3;
    private double scanlineOpacity = 0.08;
    private boolean flicker = true;
    private boolean chunkyMode = true;
    private int bandCount = 3;
    private double bandHeight = 20;
    private double bandOpacity = 0.15;
    private double bandSpeed = 0.5;

    public VHSScanlineEffect(Builder builder) {
        width = builder.width;
        height = builder.height;
        autoSize = builder.autoSize;
        scanlineSpacing = builder.scanlineSpacing;
        scanlineOpacity = builder.scanlineOpacity;
        flicker = builder.flicker;
        chunkyMode = builder.chunkyMode;
        bandCount = builder.bandCount;
        bandHeight = builder.bandHeight;
        bandOpacity = builder.bandOpacity;
        bandSpeed = builder.bandSpeed;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        scanline = new VHSScanline(disc, width, height);
        scanline.setAutoSize(autoSize);
        scanline.setScanlineSpacing(scanlineSpacing);
        scanline.setBaseScanlineOpacity(scanlineOpacity);
        scanline.setRetroFlickerEnabled(flicker);
        scanline.setChunkyModeEnabled(chunkyMode);
        scanline.setBandCount(bandCount);
        scanline.setBandHeight(bandHeight);
        scanline.setBandOpacity(bandOpacity);
        scanline.setBandSpeed(bandSpeed);                
        scanline.setDrawIntervalMillis(50);
        ClipUtils.applyCircularClip(scanline, disc.getPlanetCircle(), 4.0);
        scanline.start();
    }

    @Override
    public void update(double occlusion) {
        scanline.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return scanline;
    }

    // =====================
    // === Builder class ===
    // =====================

    public static class Builder {
        private double width = 512;
        private double height = 512;
        private boolean autoSize = false;
        private int scanlineSpacing = 3;
        private double scanlineOpacity = 0.08;
        private boolean flicker = true;
        private boolean chunkyMode = true;
        private int bandCount = 3;
        private double bandHeight = 20;
        private double bandOpacity = 0.15;
        private double bandSpeed = 0.5;

        public Builder autoSize(boolean enableAutosize) {
            this.autoSize = enableAutosize;
            return this;
        }
        public Builder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder scanlineSpacing(int spacing) {
            this.scanlineSpacing = spacing;
            return this;
        }

        public Builder scanlineOpacity(double opacity) {
            this.scanlineOpacity = opacity;
            return this;
        }

        public Builder flicker(boolean enabled) {
            this.flicker = enabled;
            return this;
        }

        public Builder chunkyMode(boolean enabled) {
            this.chunkyMode = enabled;
            return this;
        }

        public Builder bandCount(int count) {
            this.bandCount = count;
            return this;
        }

        public Builder bandHeight(double height) {
            this.bandHeight = height;
            return this;
        }

        public Builder bandOpacity(double opacity) {
            this.bandOpacity = opacity;
            return this;
        }

        public Builder bandSpeed(double speed) {
            this.bandSpeed = speed;
            return this;
        }

        public VHSScanlineEffect build() {
            return new VHSScanlineEffect(this);
        }
    }
}