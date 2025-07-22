package edu.jhuapl.trinity.utils.fun.solar;

import edu.jhuapl.trinity.utils.fun.solar.FlarePatternFactory.SunSlice;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public class RetrowavePresetFactory {
    public static final int TOTAL_SUN_HEIGHT = 256;
    public static final double SUN_REGION_HEIGHT = 128;
    public static final double COLOR_THICKNESS = 2.5;
    public static final double GAP_THICKNESS = 1.0;
    public static final int STRIPE_COUNT = 40;
    public static final double BLACK_GAP_THICKNESS = 1.5;
    public static final Color DEFAULT_GAP_COLOR = Color.TRANSPARENT;

    public enum RetrowavePresetType {
        RETROWAVE_CLASSIC,
        MIDNIGHT_MOON,
        ULTRAVIOLET_GRID,
        RETRO_SYNTH_NOVA,
        NEON_VAPOR_SUNSET,
        MIAMI_HEAT_BURST,
        NIGHT_STALKER;

        public List<FlareSprite> create() {
            return switch (this) {
                case RETROWAVE_CLASSIC -> createRetrowaveClassic();
                case MIDNIGHT_MOON -> createMidnightMoon();
                case ULTRAVIOLET_GRID -> createNeonGridRetrowave();
                case RETRO_SYNTH_NOVA -> createRetroSynthNova();
                case NEON_VAPOR_SUNSET -> createNeonVaporSunset();
                case MIAMI_HEAT_BURST -> createMiamiHeatBurst();
                case NIGHT_STALKER -> createNightStalker();
            };
        }
    }

    public static Set<String> availablePresets() {
        return Arrays.stream(RetrowavePresetType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());
    }

    public static List<FlareSprite> createRetrowaveClassic() {
        List<FlareSprite> flares = new ArrayList<>();

        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#FF0080"), // top gradient
            Color.web("#FFA500"), // bottom
            CLASSIC_RETROWAVE_SUN
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.7, true, "Retrowave Sun"));
        flares.add(new FlareSprite(FlarePatternFactory.createHaloImage(256, Color.HOTPINK, 2.0), 1.5, 0.0, 0.25, true, "Synth Halo 1"));
        flares.add(new FlareSprite(FlarePatternFactory.createHaloImage(256, Color.PURPLE, 0.5), 2.75, 0.0, 0.15, true, "Synth Halo 2"));

        // Chain A (Left)
        for (int i = 0; i < 12; i++) {
            double pos = 0.4 + i * 0.15;
            double scale = 0.2 + 0.02 * i;
            Color c = Color.hsb(300 + i * 5, 1, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(32, c),
                scale, pos, 0.07, true, "ChainA #" + i
            ));
        }

        // Chain B (Right)
        for (int i = 0; i < 10; i++) {
            double pos = 1.5 + i * 0.15;
            double scale = 0.2 - 0.01 * i;
            Color c = Color.hsb(200 + i * 4, 1, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(24, c),
                scale, pos, 0.05, true, "ChainB #" + i
            ));
        }

        return flares;
    }

    public static List<FlareSprite> createMidnightMoon() {
        List<FlareSprite> flares = new ArrayList<>();

        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#0a0a0a"),
            Color.web("#5d00a2"),
            MIDNIGHT_MOON
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.7, true, "Retrowave Sun"));
        flares.add(new FlareSprite(FlarePatternFactory.createHaloImage(256, Color.HOTPINK, 2.0), 1.3, 0.0, 0.25, true, "Synth Halo"));
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(256), 1.0, 0.0, 0.1, true, "Outer Rainbow"));

        // Chain A (Left)
        for (int i = 0; i < 12; i++) {
            double pos = 0.4 + i * 0.15;
            double scale = 0.2 + 0.02 * i;
            Color c = Color.hsb(300 + i * 5, 1, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(32, c),
                scale, pos, 0.07, true, "ChainA #" + i
            ));
        }

        // Chain B (Right)
        for (int i = 0; i < 10; i++) {
            double pos = 1.5 + i * 0.15;
            double scale = 0.2 - 0.01 * i;
            Color c = Color.hsb(200 + i * 4, 1, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(24, c),
                scale, pos, 0.05, true, "ChainB #" + i
            ));
        }

        return flares;
    }

    public static List<FlareSprite> createNeonGridRetrowave() {
        List<FlareSprite> flares = new ArrayList<>();

        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#FF00FF"),
            Color.web("#FFA500"),
            ULTRAVIOLET
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.75, true, "Neon Sun"));
        flares.add(new FlareSprite(FlarePatternFactory.createHexGridImage(128, Color.CYAN, 20, 1), 1.4, 0.0, 0.2, true, "Hex Grid"));
        flares.add(new FlareSprite(FlarePatternFactory.createRaysImage(256, 10, Color.web("#00FFFF80")), 2.5, 0.0, 0.25, true, "Neon Rays"));

        // Chain with glitch bursts
        for (int i = 0; i < 15; i++) {
            double pos = 0.5 + i * 0.1;
            double scale = 0.25 + (i % 3) * 0.05;
            Color c = (i % 2 == 0) ? Color.HOTPINK : Color.CYAN;
            flares.add(new FlareSprite(
                FlarePatternFactory.createPixelBurstImage(32, c, 10),
                scale, pos, 0.08, true, "Burst #" + i
            ));
        }

        return flares;
    }

    public static List<FlareSprite> createRetroSynthNova() {
        List<FlareSprite> flares = new ArrayList<>();

        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#FF3366"),
            Color.web("#FFDD33"),
            SYNTH_MIRAGE
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.8, true, "Core Sun"));
        flares.add(new FlareSprite(FlarePatternFactory.createPlasmaRing(256, Color.HOTPINK, 12, 4.0), 1.6, 0.0, 0.3, true, "Plasma Ring"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(256, Color.YELLOW, 2, 0), 1.7, 0.0, 0.1, true, "Cross Star 1"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(256, Color.CYAN, 2, 90), 2.5, 0.0, 0.1, true, "Cross Star 2"));

        // Diagonal chain
        for (int i = 0; i < 14; i++) {
            double pos = 0.3 + i * 0.12;
            double scale = 0.25 - 0.01 * i;
            Color c = Color.hsb(280 + i * 4, 1, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(28, c),
                scale, pos, 0.07, true, "Diagonal Chain #" + i
            ));
        }

        return flares;
    }

    public static List<FlareSprite> createNeonVaporSunset() {
        List<FlareSprite> flares = new ArrayList<>();
        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#4c00ff"),
            Color.web("#00fff7"),
            NEON_VAPOR
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.75, true, "Neon Vapor Sun (Hi-Res)"));
        flares.add(new FlareSprite(FlarePatternFactory.createHaloImage(256, Color.AQUA, 1.5), 1.4, 0.0, 0.2, true, "Aqua Halo"));
        flares.add(new FlareSprite(FlarePatternFactory.createRaysImage(256, 12, Color.web("#00ffff80")), 2.5, 0.0, 0.2, true, "Radiant Rays"));

        for (int i = 0; i < 12; i++) {
            double pos = 0.45 + i * 0.14;
            double scale = 0.22 - i * 0.01;
            Color c = Color.hsb(190 + i * 5, 1, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(28, c),
                scale, pos, 0.06, true, "Vapor Chain #" + i
            ));
        }

        return flares;
    }

    public static List<FlareSprite> createMiamiHeatBurst() {
        List<FlareSprite> flares = new ArrayList<>();

        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#ff0080"),
            Color.web("#ffd700"),
            MIAMI_HEAT
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.8, true, "Miami Sun"));
        flares.add(new FlareSprite(FlarePatternFactory.createPlasmaRing(256, Color.web("#ff3399"), 8, 3.5), 1.5, 0.0, 0.3, true, "Plasma Bloom"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(256, Color.LIGHTGOLDENRODYELLOW), 1.7, 0.0, 0.1, true, "Cross Flash"));

        for (int i = 0; i < 10; i++) {
            double pos = 0.35 + i * 0.14;
            double scale = 0.24 - i * 0.015;
            Color c = Color.hsb(40 + i * 7, 0.9, 1);
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(26, c),
                scale, pos, 0.05, true, "Heat Chain #" + i
            ));
        }

        return flares;
    }

    public static List<FlareSprite> createNightStalker() {
        List<FlareSprite> flares = new ArrayList<>();

        Image sun = FlarePatternFactory.createRetrowaveSun(
            TOTAL_SUN_HEIGHT, TOTAL_SUN_HEIGHT,
            Color.web("#2e003e"),
            Color.web("#8b0000"),
            NIGHT_STALKER
        );

        flares.add(new FlareSprite(sun, 1.0, 0.0, 0.75, true, "Nightstalker Sun"));
        flares.add(new FlareSprite(FlarePatternFactory.createPlasmaRing(256, Color.web("#ff003c"), 10, 5.0), 1.6, 0.0, 0.25, true, "Blood Halo"));
        flares.add(new FlareSprite(FlarePatternFactory.createRaysImage(256, 8, Color.web("#ff003c55")), 2.8, 0.0, 0.3, true, "Horror Rays"));

        for (int i = 0; i < 10; i++) {
            double pos = 0.5 + i * 0.13;
            double scale = 0.22 - 0.012 * i;
            Color c = (i % 2 == 0) ? Color.web("#ff003c") : Color.web("#0a0a0a");
            flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(24, c),
                scale, pos, 0.06, true, "Stalker Chain #" + i
            ));
        }

        return flares;
    }


    public static List<SunSlice> CLASSIC_RETROWAVE_SUN = generateStripedSlices(List.of(
            Color.web("#ff0080"),  // Neon pink
            Color.web("#ffb347"),  // Light orange
            Color.web("#ffd700")   // Golden yellow
        ), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );
    public static List<SunSlice> MIDNIGHT_MOON = generateStripedSlices(List.of(
            Color.web("#0a0a0a"),
            Color.web("#1a1a2e"),
            Color.web("#3f0071"),
            Color.web("#5d00a2")
        ), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );

    public static List<SunSlice> NEON_VAPOR = generateStripedSlices(List.of(Color.web("#00fff7"), Color.web("#4c00ff")), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );

    public static List<SunSlice> MIAMI_HEAT = generateStripedSlices(List.of(
            Color.web("#ff0080"),  // Magenta
            Color.web("#ffb347"),  // Peach
            Color.web("#ffd700"),  // Golden yellow
            Color.web("#ffa07a")   // Salmon
        ), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );

    public static List<SunSlice> SYNTH_MIRAGE = generateStripedSlices(List.of(
            Color.web("#8efff3"),  // Aqua mint
            Color.web("#a3a9ff"),  // Lavender blue
            Color.web("#d6f1ff"),  // Ice white
            Color.web("#b5fff2")   // Cyan pastel
        ), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );

    public static List<SunSlice> ULTRAVIOLET = generateStripedSlices(List.of(
            Color.web("#4b0082"),  // Indigo
            Color.web("#8a2be2"),  // Electric purple
            Color.web("#ff00ff"),  // Fuchsia
            Color.web("#2e003e")   // Deep violet
        ), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );

    public static List<SunSlice> NIGHT_STALKER = generateStripedSlices(List.of(
            Color.web("#8b0000"),  // Blood red
            Color.web("#111111"),  // Dark gray
            Color.web("#ff003c"),  // Slasher pink
            Color.web("#0a0a0a")   // Deep shadow
        ), DEFAULT_GAP_COLOR,
        STRIPE_COUNT,
        COLOR_THICKNESS,
        GAP_THICKNESS,
        SUN_REGION_HEIGHT,
        true
    );

    public static List<SunSlice> generateStripedSlices(
        List<Color> palette,
        int stripeCount,
        double colorThickness,
        double gapThickness,
        double totalHeight
    ) {
        List<SunSlice> slices = new ArrayList<>();
        for (int i = 0; i < stripeCount; i++) {
            Color color = palette.get(i % palette.size());
            slices.add(new SunSlice(Color.BLACK, gapThickness));
            slices.add(new SunSlice(color, colorThickness));
        }
        return normalizeSlicesToHeight(slices, totalHeight);
    }

    public static List<SunSlice> generateStripedSlices(
        List<Color> keyColors,
        Color gapColor,
        int stripeCount,
        double colorThickness,
        double gapThickness,
        double totalHeight,
        boolean interpolateColors // true = gradient between key colors
    ) {
        List<Color> stripeColors = new ArrayList<>();

        if (interpolateColors && keyColors.size() >= 2) {
            int segments = keyColors.size() - 1;
            int stripesPerSegment = stripeCount / segments;
            int remainder = stripeCount % segments;

            for (int i = 0; i < segments; i++) {
                Color c0 = keyColors.get(i);
                Color c1 = keyColors.get(i + 1);
                int count = stripesPerSegment + (i < remainder ? 1 : 0); // spread remainder

                for (int j = 0; j < count; j++) {
                    double t = (double) j / count;
                    stripeColors.add(interpolateColor(c0, c1, t));
                }
            }
        } else {
            for (int i = 0; i < stripeCount; i++) {
                stripeColors.add(keyColors.get(i % keyColors.size()));
            }
        }

        List<SunSlice> slices = new ArrayList<>();
        for (Color color : stripeColors) {
            slices.add(new SunSlice(gapColor, gapThickness));
            slices.add(new SunSlice(color, colorThickness));
        }

        return normalizeSlicesToHeight(slices, totalHeight);
    }

    public static Color interpolateColor(Color c0, Color c1, double t) {
        double r = c0.getRed() + (c1.getRed() - c0.getRed()) * t;
        double g = c0.getGreen() + (c1.getGreen() - c0.getGreen()) * t;
        double b = c0.getBlue() + (c1.getBlue() - c0.getBlue()) * t;
        double a = c0.getOpacity() + (c1.getOpacity() - c0.getOpacity()) * t;
        return new Color(r, g, b, a);
    }


    public static List<SunSlice> normalizeSlicesToHeight(List<SunSlice> raw, double targetHeight) {
        double total = raw.stream().mapToDouble(SunSlice::thickness).sum();
        double scale = targetHeight / total;

        return raw.stream()
            .map(s -> new SunSlice(s.color(), s.thickness() * scale))
            .collect(Collectors.toList());
    }

}
