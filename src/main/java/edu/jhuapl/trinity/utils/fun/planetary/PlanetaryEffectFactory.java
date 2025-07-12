package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.List;
import javafx.scene.paint.Color;

/**
 *
 * @author Sean Phillips
 */
public class PlanetaryEffectFactory {

    public enum PlanetStyle {
        RETROWAVE,
        OUTRUN,
        VAPORWAVE,
        SCIFI,
        SPACE_HORROR;
    }

    public static List<PlanetaryEffect> createEffectsFor(PlanetStyle style) {
        return switch (style) {
            case OUTRUN -> List.of(
                new GlowRimEffect(Color.web("#FF007F")),
                new ScanlineEffect(30, Color.web("#FF007F", 0.2)),
                new HorizonSliceEffect(12, Color.web("#000000", 0.4)),
                new ConcentricRingsEffect(Color.web("#FF007F", 0.15)),
                new AuraPulseEffect(Color.web("#FF66AA", 0.3))
            );

            case VAPORWAVE -> List.of(
                new GlowRimEffect(Color.web("#ff77ff")),
                new ScanlineEffect(20, Color.web("#ffffff", 0.2)),
                new VaporGradientOverlayEffect(),
                new AuraPulseEffect(Color.web("#ffbbe0", 0.25)),
                new ConcentricRingsEffect(Color.web("#ccffff", 0.1))
            );

            case SCIFI -> List.of(
                new GlowRimEffect(Color.web("#00FFFF")),
                new ScanlineEffect(40, Color.web("#00FFFF", 0.1)),
                new TechnoGridEffect(Color.web("#00FFFF", 0.1)),
                new TargetScannerEffect(Color.web("#00FFFF", 0.2)),
                new AuraPulseEffect(Color.web("#00FFFF", 0.15))
            );

            case SPACE_HORROR -> List.of(
                new GlowRimEffect(Color.web("#990000")),
                new SparseLineEffect(6, Color.web("#330000", 0.3), true), // vertical sparse lines
                new DripShadowEffect(),
                new AuraPulseEffect(Color.web("#440000", 0.15)),
                new ConcentricRingsEffect(Color.web("#660000", 0.08))
            );

            case RETROWAVE -> List.of(
                new GlowRimEffect(Color.web("#FF00CC")),
                new ScanlineEffect(15, Color.web("#FF00CC", 0.2)),
                new HorizonSliceEffect(10, Color.web("#000000", 0.4)),
                new AuraPulseEffect(Color.web("#FF33AA", 0.25)),
                new ConcentricRingsEffect(Color.web("#FF33AA", 0.1))
            );
        };
    }
}

