package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;

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
        SPACE_HORROR,
        AI_OVERLORD,
        HOSTILE_AI,
        RANDOMIZED_AI
    }

    public static List<PlanetaryEffect> createEffectsFor(PlanetStyle style) {
        return switch (style) {
            case OUTRUN ->
                List.of(
                new GlowRimEffect(Color.web("#FF007F")),
                new ScanlineEffect(30, Color.web("#FF007F", 0.2)),
                new HorizonSliceEffect(12, Color.web("#000000", 0.4)),
                new ConcentricRingsEffect(Color.web("#FF007F", 0.15)),
                new AuraPulseEffect(Color.web("#FF66AA", 0.3))
                );

            case VAPORWAVE ->
                List.of(
                new GlowRimEffect(Color.web("#ff77ff")),
                new ScanlineEffect(20, Color.web("#ffffff", 0.2)),
                new VaporGradientOverlayEffect(),
                new AuraPulseEffect(Color.web("#ffbbe0", 0.25)),
                new ConcentricRingsEffect(Color.web("#ccffff", 0.1))
                );

            case SCIFI ->
                List.of(
                new GlowRimEffect(Color.web("#00FFFF")),
                new ScanlineEffect(40, Color.web("#00FFFF", 0.1)),
                new TechnoGridEffect(Color.web("#00FFFF", 0.1)),
                new TargetScannerEffect(Color.web("#00FFFF", 0.2)),
                new AuraPulseEffect(Color.web("#00FFFF", 0.15))
                );

            case SPACE_HORROR ->
                List.of(
                new GlowRimEffect(Color.web("#990000")),
                new SparseLineEffect(6, Color.web("#330000", 0.3), true),
                new DripShadowEffect(),
                new AuraPulseEffect(Color.web("#440000", 0.15)),
                new ConcentricRingsEffect(Color.web("#660000", 0.08))
                );

            case RETROWAVE ->
                List.of(
                new GlowRimEffect(Color.web("#FF00CC")),
                new ScanlineEffect(15, Color.web("#FF00CC", 0.2)),
                new HorizonSliceEffect(10, Color.web("#000000", 0.4)),
                new AuraPulseEffect(Color.web("#FF33AA", 0.25)),
                new ConcentricRingsEffect(Color.web("#FF33AA", 0.1))
                );

case AI_OVERLORD ->
    List.of(
        new GlowRimEffect(Color.web("#00CCFF")),
        new ScanlineEffect(35, Color.web("#00CCFF", 0.2)),
        new CircuitBoardEffect.Builder()
            .baseLineCount(8)
            .maxLineCount(24)
            .horizontalDensity(0.5)  // ~18 lines
            .verticalDensity(0.2)    // ~9 lines
            .lineColor(Color.web("#00CCFF"))
            .lineThickness(1.0)
            .drawPads(true)
            .drawDots(false)
            .drawJumpers(true)
            .glowEnabled(true)
            .animateGlow(true)
            .build(),
        new AuraPulseEffect(Color.web("#00CCFF", 0.2))
    );

case HOSTILE_AI ->
    List.of(
        new GlowRimEffect(Color.web("#FF3300")),
        new ScanlineEffect(25, Color.web("#FF3300", 0.25)),
        new CircuitBoardEffect.Builder()
            .baseLineCount(10)
            .maxLineCount(32)            // ↓ slightly for performance
            .horizontalDensity(0.45)     // ~19 lines
            .verticalDensity(0.6)        // ~25 lines
            .lineColor(Color.web("#FF3300"))
            .lineThickness(1.8)          // was 2.2 → small perf win
            .drawPads(false)
            .drawDots(true)
            .drawJumpers(false)
            .glowEnabled(true)
            .animateGlow(true)
            .build(),
        new TargetScannerEffect(Color.web("#FF3300", 0.3)),
        new AuraPulseEffect(Color.web("#FF2200", 0.3))
    );

case RANDOMIZED_AI -> {
    Color baseColor = Color.hsb(Math.random() * 360, 0.9, 1.0);
    double alpha = 0.15 + Math.random() * 0.3;

    List<PlanetaryEffect> effects = new ArrayList<>();

    effects.add(new GlowRimEffect(baseColor));
    effects.add(new ScanlineEffect(
            15 + (int) (Math.random() * 40),
            baseColor.deriveColor(0, 1, 1, alpha)
    ));

    CircuitBoardEffect circuit = new CircuitBoardEffect.Builder()
            .baseLineCount(6 + (int) (Math.random() * 6))            // 6–11
            .maxLineCount(20 + (int) (Math.random() * 20))           // 20–40
            .horizontalDensity(0.3 + Math.random() * 0.5)            // 0.3–0.8
            .verticalDensity(0.3 + Math.random() * 0.5)              // 0.3–0.8
            .lineColor(baseColor)
            .lineThickness(0.9 + Math.random() * 1.2)                // 0.9–2.1
            .drawPads(Math.random() < 0.6)
            .drawDots(Math.random() < 0.4)
            .drawJumpers(Math.random() < 0.4)
            .glowEnabled(true)
            .animateGlow(Math.random() < 0.65)
            .build();
    effects.add(circuit);

    if (Math.random() < 0.5) {
        effects.add(new TargetScannerEffect(baseColor.deriveColor(0, 1, 1, alpha + 0.1)));
    }

    effects.add(new AuraPulseEffect(baseColor.deriveColor(0, 1, 1, alpha)));

    yield effects;
}
        };
    }

    public static Paint getFillForStyle(PlanetStyle style) {
        return switch (style) {
            case OUTRUN ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF003C")),
                new Stop(1, Color.web("#FF7F50")));
            case VAPORWAVE ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffccff")),
                new Stop(1, Color.web("#66ccff")));
            case SCIFI ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00FFFF")),
                new Stop(1, Color.web("#003366")));
            case SPACE_HORROR ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#220000")),
                new Stop(1, Color.web("#550000")));
            case RETROWAVE ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF00CC")),
                new Stop(1, Color.web("#6600FF")));
            case AI_OVERLORD ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#003344")),
                new Stop(1, Color.web("#00CCFF")));
            case HOSTILE_AI ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#330000")),
                new Stop(1, Color.web("#FF2200")));
        case RANDOMIZED_AI -> {
            Color start = Color.hsb(Math.random() * 360, 0.8 + Math.random() * 0.2, 0.9);
            Color end = Color.hsb(Math.random() * 360, 0.8 + Math.random() * 0.2, 0.5 + Math.random() * 0.4);
            yield new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, start),
                new Stop(1, end));
        }                
        };
    }
}
