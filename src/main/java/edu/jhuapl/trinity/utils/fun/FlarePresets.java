package edu.jhuapl.trinity.utils.fun;

import edu.jhuapl.trinity.utils.fun.FlarePatternFactory.BlurredDiskSpec;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createAnalogGlitchImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createBlurredDiskImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createCoronaRing;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createHaloImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createHexGridImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createPixelBurstImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createPlasmaRing;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createRainbowImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createRaysImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createRotatingSpikeImage;
import static edu.jhuapl.trinity.utils.fun.FlarePatternFactory.createStarImage;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Sean Phillips
 */
public class FlarePresets {

    public static double DEFAULT_OPACITY = 0.5;
    public enum FlarePresetType {
        DEFAULT, CLASSIC_GLOW, SOLAR_CORE, CORONA_FLARE,
        CRT_AURORA_DREAM, TACTICAL_HEX, CRYO_LENS_VORTEX,
        SYNTHWAVE_ELECTRIC_DUSK, SYNTHWAVE_NOVA, JJ_ABRAMS,
        CRT_OVERLOAD_PULSE, PIXEL_SHOCK_PULSE, NEON_WARP_SPIRAL;

        public List<FlareSprite> create() {
            return switch (this) {
                case CLASSIC_GLOW -> createClassicGlow();
                case SOLAR_CORE -> createSolarCore();
                case CORONA_FLARE -> createCoronaFlare();
                case CRT_AURORA_DREAM -> createCrtAuroraDream();
                case TACTICAL_HEX -> createTacticalHexGrid();
                case CRYO_LENS_VORTEX -> createCryoLensVortex();
                case SYNTHWAVE_ELECTRIC_DUSK -> createSynthwaveElectricDusk();
                case JJ_ABRAMS -> createJJAbrams();
                case CRT_OVERLOAD_PULSE -> createCRTOverloadPulse();
                case SYNTHWAVE_NOVA -> createSynthwaveNovaCollider();
                case PIXEL_SHOCK_PULSE -> createPixelShockPulse();
                case NEON_WARP_SPIRAL -> createNeonWarpSpiral();
                case DEFAULT -> createDefault();
            };
        }

        @Override
        public String toString() {
            return name();
        }
    }

    public static List<FlareSprite> createDefault() {
        List<FlareSprite> flares = new ArrayList<>();
        // Load flare images first
        Image sunDisk = FlarePatternFactory.createHaloImage(128, Color.rgb(255, 255, 210), 2f);
        Image disk = FlarePatternFactory.createDiskImage(128, Color.WHITE);
        Image star = FlarePatternFactory.createStarImage(128, Color.ALICEBLUE);
        Image rainbow = FlarePatternFactory.createRainbowImage(128);
        Image rays = FlarePatternFactory.createRaysImage(128, 12, Color.rgb(255, 255, 255, 0.5));
        Image halo = FlarePatternFactory.createHaloImage(128, Color.WHITE, 2.0);

        List<BlurredDiskSpec> specs = List.of(
                new BlurredDiskSpec(256, Color.rgb(255, 255, 180), 0.1, 0, 0, 10) // soft outer corona
                ,
                 new BlurredDiskSpec(180, Color.rgb(255, 220, 100), 0.2, 0, 0, 8) // mid haze
                ,
                 new BlurredDiskSpec(120, Color.GOLD, 0.3, 0, 0, 6) // main glow
        //    new BlurredDiskSpec(64, Color.LIGHTYELLOW, 0.6, 0, 0, 2)                 // bright sun core
        );

        Image solarComposite = FlarePatternFactory.createCompositeBlurredDisks(specs, 512);

        // Sun dressing
        flares.add(new FlareSprite(solarComposite, 1.0, 0.0, 0.333, true, "Sun Core"));

        flares.add(new FlareSprite(rays, 2.2, 0.0, 0.15, true, "Sun Rays"));
        flares.add(new FlareSprite(star, 1.6, 0.0, 0.05, true, "Sun Starburst"));

        flares.add(new FlareSprite(halo, 1.1, 0.0, 0.1, true, "Inner Halo"));
        flares.add(new FlareSprite(halo, 3.0, 0.0, 0.1, true, "Outer Halo"));

        flares.add(new FlareSprite(rainbow, 1.2, 0.0, 0.23, true, "Rainbow 1"));
        flares.add(new FlareSprite(rainbow, 2.0, 0.0, 0.23, true, "Rainbow 2"));

        // Diagonal flare chain
        flares.add(new FlareSprite(disk, .1, .4, .1, true, "Chain 1"));
        flares.add(new FlareSprite(disk, .15, .6, .1, true, "Chain 2"));
        flares.add(new FlareSprite(disk, .2, .7, .1, true, "Chain 3"));
        flares.add(new FlareSprite(disk, .5, 1.1, .2, true, "Chain 4"));
        flares.add(new FlareSprite(disk, .2, 1.3, .1, true, "Chain 5"));
        flares.add(new FlareSprite(disk, .1, 1.4, .05, true, "Chain 6"));
        flares.add(new FlareSprite(disk, .1, 1.5, .1, true, "Chain 7"));
        flares.add(new FlareSprite(disk, .1, 1.6, .1, true, "Chain 8"));
        flares.add(new FlareSprite(disk, .2, 1.65, .1, true, "Chain 9"));
        flares.add(new FlareSprite(disk, .12, 1.71, .1, true, "Chain 10"));
        flares.add(new FlareSprite(disk, 2, 2.2, .05, true, "Chain 11 (aka lil' fatty)"));
        flares.add(new FlareSprite(disk, .5, 2.4, .2, true, "Chain 12"));
        flares.add(new FlareSprite(disk, .7, 2.6, .1, true, "Chain 13"));
        flares.add(new FlareSprite(rainbow, 4, 3.0, .23, true, "Far Rainbow"));
        flares.add(new FlareSprite(disk, .2, 3.5, .1, true, "Chain 14"));

        return flares;
    }

    public static List<FlareSprite> createClassicGlow() {
        List<FlareSprite> flares = new ArrayList<>();
        //Sun    
        flares.add(new FlareSprite(
                FlarePatternFactory.createBlurredDiskImage(256, 96, Color.rgb(255, 245, 200), 0.9, 0, 0, 20),
                1.0, 0.0, 0.9, true, "Core Glow"));
        flares.add(new FlareSprite(
                FlarePatternFactory.createHaloImage(128, Color.rgb(255, 240, 180), 2.5),
                2.0, 0.0, 0.75, true, "Glow Halo"));
        //Ghost Lens Artifacts
        // 🔗 Chain 
        Random rando = new Random();
        for (int i = 0; i < 6; i++) {
            double pos = 0.4 + i * 0.5;
            double scale = rando.nextDouble() + i * 0.5;
            flares.add(new FlareSprite(
                FlarePatternFactory.createBlurredDiskImage(64, 48, 
                    Color.rgb(255, 220, 100), DEFAULT_OPACITY, 0, 0, 10),
                    scale, pos, DEFAULT_OPACITY*rando.nextDouble(), true, "Lens Ghost " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createSolarCore() {
        List<FlareSprite> flares = new ArrayList<>();

        Image composite = FlarePatternFactory.createCompositeBlurredDisks(List.of(
                new FlarePatternFactory.BlurredDiskSpec(220, Color.rgb(255, 245, 180), 0.4, 0, 0, 10),
                new FlarePatternFactory.BlurredDiskSpec(160, Color.rgb(255, 230, 100), 0.6, 0, 0, 8),
                new FlarePatternFactory.BlurredDiskSpec(90, Color.rgb(255, 200, 50), 0.8, 0, 0, 5)
        ), 512);

        flares.add(new FlareSprite(composite, 1.0, 0.0, 0.9, true, "Solar Core Composite"));

        flares.add(new FlareSprite(
                FlarePatternFactory.createRainbowImage(128),
                1.8, 0.0, 0.7, true, "Diffraction Ring"));

        flares.add(new FlareSprite(
                FlarePatternFactory.createDiskImage(16, Color.rgb(255, 220, 120)),
                0.2, 1.5, 0.5, true, "Sun Artifact"));

        return flares;
    }

    public static List<FlareSprite> createCoronaFlare() {
        List<FlareSprite> flares = new ArrayList<>();
        flares.add(new FlareSprite(
                FlarePatternFactory.createCoronaRing(128, Color.GOLD, 10),
                1.5, 0.0, 0.85, true, "Corona Ring"));
        flares.add(new FlareSprite(
                FlarePatternFactory.createStarImage(128, Color.rgb(255, 255, 240)),
                1.2, 0.0, 0.7, true, "Solar Spark"));
        flares.add(new FlareSprite(
                FlarePatternFactory.createBlurredDiskImage(64, 48, Color.rgb(255, 200, 100), 0.5, 0, 0, 6),
                0.3, 1.3, 0.4, true, "Corona Echo"));
        // 🔗 Chain 
        Random rando = new Random();
        for (int i = 0; i < 6; i++) {
            double pos = 0.4 + i * 0.5;
            double scale = rando.nextDouble() + i * 0.25;
            flares.add(new FlareSprite(
                FlarePatternFactory.createCoronaRing(128, Color.GOLD, 10), 
                    scale, pos, 0.25*rando.nextDouble(), true, "Lens Ghost " + i));
        }        
        return flares;
    }

    public static List<FlareSprite> createCrtAuroraDream() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group A — Retro Core
        flares.add(new FlareSprite(FlarePatternFactory.createDiskImage(96, Color.ORANGE), 1.0, 0.0, 0.5, true, "Retro Core"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(128, Color.WHITE), 1.2, 0.0, 0.3, true, "Starburst"));
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 1.5, 0.0, 0.25, true, "Rainbow Aura"));
        flares.add(new FlareSprite(FlarePatternFactory.createCoronaRing(128, Color.ORANGERED, 8), 1.8, 0.0, 0.2, true, "Retro Corona"));

        // 🌞 Sun Group B — Outer glow
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 2.5, 0.0, 0.15, true, "Fringe"));
        flares.add(new FlareSprite(FlarePatternFactory.createAnalogGlitchImage(128, 128, Color.ORANGE, 40), 2.0, 0.0, 0.1, true, "CRT Ripple"));

        Random rando = new Random();
        // 🔗 Chain — Vintage Trail
        for (int i = 0; i < 10; i++) {
            double pos = rando.nextDouble()+ 0.4 + i * 0.15;
            Color color = i % 3 == 0 ? Color.CYAN : (i % 3 == 1 ? Color.MAGENTA : Color.ORANGE);
            flares.add(new FlareSprite(FlarePatternFactory.createDiskImage(64, color), 
                0.2 + i * rando.nextDouble(), pos , 0.25, true, "Retro Ghost " + i));
        }

        return flares;

    }

    public static List<FlareSprite> createCryoLensVortex() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group A — Energy Core
        flares.add(new FlareSprite(FlarePatternFactory.createHaloImage(128, Color.AQUA, 2.5), 1.2, 0.0, 0.4, true, "Aqua Halo"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(128, Color.LIGHTCYAN), 1.5, 0.0, 0.3, true, "Pulse Star"));
        flares.add(new FlareSprite(FlarePatternFactory.createRaysImage(128, 12, Color.ALICEBLUE), 1.8, 0.0, 0.25, true, "Icy Rays"));
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 2.0, 0.0, 0.15, true, "Icy Fringe"));

        // 🌞 Sun Group B — Crystal aura
        flares.add(new FlareSprite(FlarePatternFactory.createHexGridImage(128, Color.AZURE, 20, 1), 1.4, 0.0, 0.12, true, "Hex Overlay"));

        // 🔗 Chain — Alien Echoes
        Random rando = new Random();
        for (int i = 0; i < 14; i++) {
            double pos = 0.5 + i * 0.2;
            double scale = rando.nextDouble() + i * 0.05;
            Color color = i % 2 == 0 ? Color.AQUAMARINE : Color.LIGHTBLUE;
            flares.add(new FlareSprite(FlarePatternFactory.createStarImage(64, color), 
                scale, pos, DEFAULT_OPACITY, true, "Cryo Echo " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createSynthwaveElectricDusk() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group A — Central Synth Core
        flares.add(new FlareSprite(FlarePatternFactory.createHaloImage(128, Color.HOTPINK, 2.2), 1.2, 0.0, 0.5, true, "Synth Halo"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(128, Color.MAGENTA), 1.5, 0.0, 0.3, true, "Synth Star"));
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 1.8, 0.0, 0.25, true, "Neon Halo"));

        // 🌞 Sun Group B — Outer Spikes
        flares.add(new FlareSprite(FlarePatternFactory.createPlasmaRing(128, Color.DEEPPINK, 10, 3.0), 2.4, 0.0, 0.2, true, "Plasma Ring"));
        flares.add(new FlareSprite(FlarePatternFactory.createRotatingSpikeImage(128, 10, Color.HOTPINK), 2.6, 0.0, 0.25, true, "Pink Spikes"));

        // 🔗 Chain A — Pink Pop Trail
        for (int i = 0; i < 15; i++) {
            double pos = 0.3 + i * 0.15;
            Color color = i % 2 == 0 ? Color.HOTPINK : Color.MAGENTA;
            flares.add(new FlareSprite(FlarePatternFactory.createDiskImage(24, color), 
                0.2 + i * 0.25, pos, 0.35, true, "Synth Trail " + i));
        }

        // 🔗 Chain B — Rainbow Reflections
        for (int i = 0; i < 10; i++) {
            double pos = 0.6 + i * 0.25;
            flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 
                0.25 + i * 0.3, pos, 0.1, true, "Rainbow Chain " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createJJAbrams() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group A — Composite Core
        Image solarComposite = FlarePatternFactory.createCompositeBlurredDisks(List.of(
                new BlurredDiskSpec(256, Color.rgb(255, 255, 180), 0.15, 0, 0, 10),
                new BlurredDiskSpec(180, Color.rgb(255, 220, 100), 0.25, 0, 0, 8),
                new BlurredDiskSpec(120, Color.GOLD, 0.35, 0, 0, 6)
        ), 512);
        flares.add(new FlareSprite(solarComposite, 1.0, 0.0, 0.4, true, "Solar Core"));
        flares.add(new FlareSprite(FlarePatternFactory.createStarImage(128, Color.WHITE), 1.6, 0.0, 0.2, true, "Optical Spike"));
        flares.add(new FlareSprite(FlarePatternFactory.createRaysImage(128, 20, Color.WHITE), 2.0, 0.0, 0.25, true, "Lens Rays"));

        // 🌞 Sun Group B — Inner rainbow bloom
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 1.2, 0.0, 0.2, true, "Chromatic Bloom"));
        flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 2.4, 0.0, 0.15, true, "Lens Ring"));

        // 🔗 Chain A — Prism Echo Trail
        for (int i = 0; i < 14; i++) {
            double pos = 0.4 + i * 0.18;
            flares.add(new FlareSprite(FlarePatternFactory.createRainbowImage(128), 0.25 + i * 0.012, pos, 0.25, true, "Prism Echo " + i));
        }

        // 🔗 Chain B — Colored Artifact Dots
        Color[] chainColors = {Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.PINK};
        for (int i = 0; i < 10; i++) {
            double pos = 0.6 + i * 0.2;
            Color color = chainColors[i % chainColors.length];
            flares.add(new FlareSprite(FlarePatternFactory.createDiskImage(24, color), 0.2 + i * 0.01, pos, 0.24, true, "Artifact Dot " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createTacticalHexGrid() {
        List<FlareSprite> list = new ArrayList<>();

        Image grid = createHexGridImage(64, Color.LIGHTBLUE, 8, 1.0);
        Image star = createStarImage(128, Color.CYAN);
        Image glow = createBlurredDiskImage(256, 128, Color.AQUA, 0.4, 4, 0);

        list.add(new FlareSprite(glow, 1.0, 0.0, 0.3, true, "Glow"));
        list.add(new FlareSprite(grid, 1.5, 0.0, 0.25, true, "HUD Grid"));
        list.add(new FlareSprite(star, 1.4, 0.0, 0.2, true, "Star Overlay"));

        // 🔗 Chain — Colored Hex
        Color[] chainColors = {Color.CYAN, Color.YELLOW, Color.MAGENTA, Color.PINK};
        Random rando = new Random();
        for (int i = 0; i < 10; i++) {
            double scale = rando.nextDouble() + i * 0.25;
            double pos = 0.6 + i * 0.2;
            Color color = chainColors[i % chainColors.length];
            list.add(new FlareSprite(FlarePatternFactory.createHexGridImage(64, color, 8, 1.0), 
                scale, pos, i == 0 ? 0.8 : 0.75/i, true, "Hex Chain " + i));
        }        
        
        return list;
    }

    public static List<FlareSprite> createCRTOverloadPulse() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group
        flares.add(new FlareSprite(createBlurredDiskImage(256, 120, Color.LAWNGREEN, 0.5, 0, 0, 5), 1.0, 0.0, 0.4, true, "CRT Core"));
        flares.add(new FlareSprite(createAnalogGlitchImage(256, 256, Color.LAWNGREEN, 80), 1.6, 0.0, 0.1, true, "Scanlines"));
        flares.add(new FlareSprite(createHexGridImage(256, Color.GREENYELLOW, 24, 1.2), 1.6, 0.0, 0.1, true, "Hex Overlay"));
        flares.add(new FlareSprite(createRainbowImage(256), 2.5, 0.0, 0.1, true, "CRT Fringe"));
        flares.add(new FlareSprite(createHaloImage(256, Color.LIME, 2.5), 1.6, 0.0, 0.25, true, "Lime Halo"));

        // 🔗 Chain 1 – Ghost Echo (green trail)
        for (int i = 0; i < 12; i++) {
            double pos = 0.4 + i * 0.2;
            flares.add(new FlareSprite(createAnalogGlitchImage(128, 128, Color.LIMEGREEN, 20), 0.2 + i * 0.02, pos, 0.27, true, "Ghost Trail " + i));
        }

        // 🔗 Chain 2 – Rainbow Scanner
        for (int i = 0; i < 10; i++) {
            double pos = 0.6 + i * 0.25;
            flares.add(new FlareSprite(createRainbowImage(128), 0.3 + i * 0.015, pos, 0.25, true, "Rainbow Scanner " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createSynthwaveNovaCollider() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group A — Hot core
        flares.add(new FlareSprite(createBlurredDiskImage(256, 100, Color.HOTPINK, 0.5, 0, 0, 6), 1.0, 0.0, 0.4, true, "Core Glow"));
        flares.add(new FlareSprite(createStarImage(256, Color.ALICEBLUE), 1.4, 0.0, 0.25, true, "Star Overlay"));
        flares.add(new FlareSprite(createPlasmaRing(256, Color.FUCHSIA, 14, 5.0), 1.8, 0.0, 0.25, true, "Plasma Shell"));
        flares.add(new FlareSprite(createHaloImage(256, Color.DEEPPINK, 2.0), 2.2, 0.0, 0.15, true, "Outer Halo"));

        // 🌞 Sun Group B — Contrast ring
        flares.add(new FlareSprite(createRotatingSpikeImage(256, 12, Color.FUCHSIA), 2.8, 0.0, 0.2, true, "Rotating Spikes"));
        flares.add(new FlareSprite(createRainbowImage(256), 3.0, 0.0, 0.1, true, "Iridescent Rim"));

        // 🔗 Chain 1 — Neon Flare Trail
        for (int i = 0; i < 15; i++) {
            double pos = 0.3 + i * 0.18;
            flares.add(new FlareSprite(createBlurredDiskImage(128, 64, Color.HOTPINK, 0.3, 0, 0, 4), 0.2 + i * 0.02, pos, 0.25, true, "Neon Trail " + i));
        }

        // 🔗 Chain 2 — Rainbow Lens Bounce
        for (int i = 0; i < 12; i++) {
            double pos = 0.6 + i * 0.2;
            flares.add(new FlareSprite(createRainbowImage(128), 0.25 + i * 0.015, pos, 0.24, true, "Rainbow Bounce " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createPixelShockPulse() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group
        flares.add(new FlareSprite(createRainbowImage(256), 0.5, 0.0, 0.05, true, "Iridescent Rim"));
        flares.add(new FlareSprite(createBlurredDiskImage(256, 100, Color.CYAN, 0.4, 0, 0, 5), 1.0, 0.0, 0.35, true, "Cyan Core"));
        flares.add(new FlareSprite(createPixelBurstImage(256, Color.CYAN, 120), 1.5, 0.0, 0.3, true, "Pixel Burst"));
        flares.add(new FlareSprite(createAnalogGlitchImage(256, 256, Color.AQUA, 60), 0.4, 0.0, 0.2, true, "Glitch"));
        flares.add(new FlareSprite(createRaysImage(256, 20, Color.AQUAMARINE), 2.3, 0.0, 0.25, true, "Burst Rays"));

        // 🔗 Chain — Glitch Trail
        Random rando = new Random();
        for (int i = 0; i < 13; i++) {
            double pos = 0.5 + i * 0.22;
            double scale = rando.nextDouble() * (i/13);
            flares.add(new FlareSprite(createPixelBurstImage(128, Color.AQUA, 50), scale + 0.25, pos, 0.95 - i/13 + 0.05, true, "Shock Fragment " + i));
            flares.add(new FlareSprite(createRaysImage(128, 20, Color.AQUAMARINE), scale, pos+0.1, 0.5 - i/13 + 0.05, true, "Shock Rays echo " + i));
        }

        return flares;
    }

    public static List<FlareSprite> createNeonWarpSpiral() {
        List<FlareSprite> flares = new ArrayList<>();

        // 🌞 Sun Group
        flares.add(new FlareSprite(createBlurredDiskImage(256, 100, Color.VIOLET, 0.4, 0, 0, 4), 1.0, 0.0, 0.35, true, "Warp Core"));
        flares.add(new FlareSprite(createCoronaRing(256, Color.PURPLE, 32), 1.5, 0.0, 0.25, true, "Corona"));
        flares.add(new FlareSprite(createPlasmaRing(256, Color.MEDIUMPURPLE, 12, 5.0), 1.9, 0.0, 0.25, true, "Plasma Envelope"));
        flares.add(new FlareSprite(createRotatingSpikeImage(256, 32, Color.LIGHTPINK), 2.3, 0.0, 0.2, true, "Spikes"));
        
        Random rando = new Random();
        // 🔗 Spiral Chain A
        for (int i = 0; i < 14; i++) {
            double pos = 0.4 + i * 0.18;
            flares.add(new FlareSprite(createPlasmaRing(256, Color.HOTPINK, 12, 16), 
                0.2 + i * 0.15, pos, rando.nextDouble(), true, "Chain Ring " + i));
        }

        // 🔗 Spiral Chain B (Offset)
        for (int i = 0; i < 10; i++) {
            double pos = 0.6 + i * 0.22;
            flares.add(new FlareSprite(createCoronaRing(256, Color.VIOLET, 12), 
                0.5 + i * 0.15, pos + 0.2, rando.nextDouble(), true, "Offset Echo " + i));
        }

        return flares;
    }
}
