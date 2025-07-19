package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
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
        RANDOMIZED_AI,
        NIGHTSIDE_EARTH,
        URBAN_WORLD,
        RANDOMIZED_WORLD
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
//                new VaporwaveEffect(
//                    List.of(
//                        Color.web("#D1C4E9"),
//                        Color.web("#B39DDB"),
//                        Color.web("#9575CD"),
//                        Color.web("#80DEEA"),
//                        Color.web("#CE93D8")
//                    ),
//                    6,                             // noiseRingCount
//                    Color.web("#BA68C8"),          // noiseRingColor
//                    0.25,                          // noiseRingOpacity
//                    true                           // includeScanlines
//                ),
//                new VHSScanlineEffect.Builder()
//                    .size(512, 512)
//                    .autoSize(true)
//                    .flicker(true)
//                    .chunkyMode(true)
//                    .scanlineSpacing(3)
//                    .bandCount(3)
//                    .build()                
                new PixelationEffect(
                    12,       // basePixelSize
                    3000.0,   // updateInterval in ms
                    true     // jitter the pixel size for glitchy look
                )
//                new GlitchDisplacementEffect(
//                    0.3,   // intensity
//                    3,    // glitch bands
//                    200.0,  //duration of each glitch  
//                    2000.0,   // ms between glitches 
//                    0.02  // thicc ass glitch ratio
//                )
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
                new RetrowaveEffect(
                    List.of(Color.web("#FF5F6D"), Color.web("#FFC371")), // Top sunset gradient
                    List.of(                                           // Bottom stripes
                        Color.web("#FF0080"),
                        Color.web("#FF8C00"),
                        Color.web("#FFD700"),
                        Color.web("#FF69B4"),
                        Color.web("#DA70D6")
                    ),
                    4.0,  // bandGap
                    12.0,  // bandHeight
                    true // useTopGradient
                ),                        
                new AuraPulseEffect(Color.web("#FF33AA", 0.45)),
                new ConcentricRingsEffect(Color.web("#FF33AA", 0.4))
                );

            case AI_OVERLORD ->
                List.of(
                new GlowRimEffect(Color.web("#00CCFF")),
                new ScanlineEffect(35, Color.web("#00CCFF", 0.2)),
                new CircuitBoardEffect.Builder()
                .baseLineCount(8)
                .maxLineCount(24)
                .horizontalDensity(0.5) // ~18 lines
                .verticalDensity(0.2) // ~9 lines
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
                .maxLineCount(32) // ↓ slightly for performance
                .horizontalDensity(0.45) // ~19 lines
                .verticalDensity(0.6) // ~25 lines
                .lineColor(Color.web("#FF3300"))
                .lineThickness(1.8) // was 2.2 → small perf win
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
                        .baseLineCount(6 + (int) (Math.random() * 6)) // 6–11
                        .maxLineCount(20 + (int) (Math.random() * 20)) // 20–40
                        .horizontalDensity(0.3 + Math.random() * 0.5) // 0.3–0.8
                        .verticalDensity(0.3 + Math.random() * 0.5) // 0.3–0.8
                        .lineColor(baseColor)
                        .lineThickness(0.9 + Math.random() * 1.2) // 0.9–2.1
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
            case NIGHTSIDE_EARTH ->
                List.of(
                new GlowRimEffect(Color.CYAN, 2),
                new ContinentLightsEffect.Builder()
                    .numContinents(4)
                    .fillRatio(0.99) // 60–70% disc area
                    .continentScale(0.65)
                    .minDistanceMultiplier(1.4) //moderate separation
                    .vertexRange(10, 20) // Smoother curves, larger landmasses
                    .lightsPerContinent(250 + (int)(Math.random() * 100)) // natural variation
                    .clustersPerContinent(4 + (int)(Math.random() * 4)) // 4–7 city clusters
                    .fillColor(Color.web("#001522"))  // Dark blue landmass (deep ocean hue)
                    .outlineColor(Color.web("#223344", 0.05)) // less sharp outline
                    .lightColors(List.of(Color.web("#FFDDAA", 0.9), Color.ALICEBLUE)) 
                    .clusterColors(List.of(Color.GREENYELLOW, Color.CYAN)) 
                    .maxLightSize(2.2)
                    .build()
                );
            case URBAN_WORLD ->
                List.of(
              new CircuitBoardEffect.Builder()
                        .baseLineCount(6 + (int) (Math.random() * 6)) // 6–11
                        .maxLineCount(20 + (int) (Math.random() * 20)) // 20–40
                        .horizontalDensity(0.3 + Math.random() * 0.5) // 0.3–0.8
                        .horizontalLines(10)
                        .verticalDensity(0.3 + Math.random() * 0.5) // 0.3–0.8
                        .verticalLines(10)
                        .lineColor(Color.web("#220099", 0.2))
                        .lineThickness(0.9 + Math.random() * 1.2) // 0.9–2.1
                        .drawPads(Math.random() < 0.6)
                        .drawDots(Math.random() < 0.4)
                        .drawJumpers(Math.random() < 0.4)
                        .glowEnabled(false)
                        .animateGlow(false)
                        .build(),                                  
                new GlowRimEffect(Color.web("#220099")),
                new ScanlineEffect(20, Color.web("#FF9900", 0.5)),
                new ContinentLightsEffect.Builder()
                .numContinents(1)
                .fillRatio(1.99)        
                .continentScale(1.1)
                .vertexRange(6, 26)
                .lightsPerContinent(800)
                .clustersPerContinent(40)
                .fillColor(Color.web("#100010", 0.1))
                .outlineColor(Color.web("#221100", 0.1))
                .lightColor(Color.web("#FFCC33", 0.9))
                .lightColors(List.of(Color.web("#FFDDAA", 0.9))) 
                .clusterColors(List.of(Color.FIREBRICK, Color.GOLD, Color.DEEPPINK)) 
                .maxLightSize(3.0)
                .build(),
                new AuraPulseEffect(Color.web("#111100", 0.35))
                );
            case RANDOMIZED_WORLD -> {
                Color fillColor = Color.hsb(
                    Math.random() * 360,       // hue
                    0.4 + Math.random() * 0.5, // saturation
                    0.05 + Math.random() * 0.15 // very dark value
                );

                Color base = Color.hsb(Math.random() * 360, 0.6, 1.0);
                double alpha = 0.6 + Math.random() * 0.3;

                int numContinents = 3 + (int) (Math.random() * 4);
                double fillRatio = 0.5 + Math.random() * 0.5; 

                List<PlanetaryEffect> effects = new ArrayList<>();
                effects.add(new GlowRimEffect(base));
                effects.add(new ScanlineEffect(
                    25 + (int) (Math.random() * 30),
                    base.deriveColor(0, 1, 1, 0.15))
                );

                effects.add(new ContinentLightsEffect.Builder()
                        .numContinents(numContinents)
                        .fillRatio(fillRatio) 
                        .minDistanceMultiplier(1.4) //moderate separation
                        .vertexRange(
                            5 + (int) (Math.random() * 6),
                            10 + (int) (Math.random() * 6)
                        )
                        .lightsPerContinent(150 + (int) (Math.random() * 200))
                        .clustersPerContinent(2 + (int) (Math.random() * 4))
                        .fillColor(fillColor)
                        .outlineColor(base.darker())
                        .lightColor(base.deriveColor(0, 1, 1, alpha))
                        .maxLightSize(1.5 + Math.random() * 2.0)
                        .build());

                effects.add(new AuraPulseEffect(base.deriveColor(0, 1, 1, 0.2)));

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
            case NIGHTSIDE_EARTH ->
                new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.NAVY), // deep ocean blue
                new Stop(1, Color.BLACK));  // black

            case URBAN_WORLD ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#000000")), // black 
                new Stop(1, Color.web("#100000")));  // dark reddish tint for sci-fi urban vibe

            case RANDOMIZED_WORLD -> {
                Color base = Color.hsb(Math.random() * 360, 0.4 + Math.random() * 0.2, 0.2 + Math.random() * 0.3);
                Color end = base.deriveColor(0, 1, 1, 1.0).darker().darker();  // deeper shade
                yield new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, base),
                new Stop(1, end));
            }

        };
    }
}
