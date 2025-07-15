package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.utils.fun.solar.FlareOcclusionUtil;
import edu.jhuapl.trinity.utils.fun.solar.LensFlareControls;
import edu.jhuapl.trinity.utils.fun.solar.LensFlareGroup;
import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryDisc;
import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryDiscControls;
import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryEffectFactory;
import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryEffectFactory.PlanetStyle;
import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryStyleControls;
import edu.jhuapl.trinity.utils.fun.solar.SunPositionControls;
import edu.jhuapl.trinity.utils.fun.solar.SunPositionTimer;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class SpecialEffectsPane extends LitPathPane {

    private static final Logger LOG = LoggerFactory.getLogger(SpecialEffectsPane.class);
    public static int PANE_WIDTH = 600;
    public static int PANE_HEIGHT = 600;

    BorderPane bp;
    SunPositionControls sunPositionControls;
    LensFlareControls lensFlareControls;
    private LensFlareGroup flareGroup;
    ObservableList<Node> occluders;
    Circle sun;
    Circle sunNeonRim;
    boolean enabled = false;
    PlanetaryDisc[] disc = new PlanetaryDisc[1]; // Mutable reference
    PlanetaryDiscControls[] controlsRef = new PlanetaryDiscControls[1];
    PlanetaryEffectFactory.PlanetStyle selectedStyle; 
            
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public SpecialEffectsPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
                "Special Effects", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;

        Tab solarCycleTab = new Tab("Solar Cycle");
        solarCycleTab.setClosable(false);
        sunPositionControls = new SunPositionControls();
        solarCycleTab.setContent(sunPositionControls);

        Tab lensFlareTab = new Tab("Lens Flare Effects");
        lensFlareTab.setClosable(false);
        flareGroup = new LensFlareGroup();
        flareGroup.setVisible(false);
        parent.getChildren().add(flareGroup);
        lensFlareControls = new LensFlareControls(flareGroup);
        lensFlareTab.setContent(lensFlareControls);

        Tab planetaryTab = new Tab("Planetary Disc");
        planetaryTab.setClosable(false);

        selectedStyle = PlanetStyle.RETROWAVE;
        disc[0] = new PlanetaryDisc(200, selectedStyle);
        disc[0].setTranslateY(200); // optional placement
        disc[0].setVisible(false);
        parent.getChildren().add(disc[0]); //The disc render should be in the pane
        //but not actually used for occlusion
        disc[0].toBack();

        occluders = FXCollections.observableArrayList();
        parent.getChildren().addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    occluders.removeAll(change.getRemoved());
                }
                if (change.wasAdded()) {
                    occluders.addAll(change.getAddedSubList());
                }
                occluders.remove(sun);
                occluders.remove(sunNeonRim);
                occluders.remove(disc[0]); //only use the occluder shape itself
            }
        });
        //this is the shape that should be used for occlusion
        occluders.add(disc[0].getOccluderShape());

        PlanetaryDiscControls controls = new PlanetaryDiscControls(
            radius -> regenerateDisc(disc, parent, controlsRef[0], selectedStyle, occluders),
            yOffset -> {
                if (disc[0] != null) disc[0].setTranslateY(yOffset);
            },
            xOffset -> {
                if (disc[0] != null) disc[0].setTranslateX(xOffset);
            },
            visible -> {
                if (disc[0] != null) {
                    disc[0].setVisible(visible);
                    disc[0].getOccluderShape().setVisible(visible);
                }
            },
            debug -> {
                if (disc[0] != null) disc[0].setDebugVisible(debug);
            },
            scatteringEnabled -> {
                if (disc[0] != null) disc[0].setScatteringEnabled(scatteringEnabled);
            },
            color -> {
                if (disc[0] != null) disc[0].setScatteringColor(color);
            },
            shadowStrength -> {
                if (disc[0] != null) disc[0].setScatteringShadowStrength(shadowStrength);
            },
            blurRadius -> {
                if (disc[0] != null) disc[0].setScatteringBlurRadius(blurRadius);
            }
        );

        controlsRef[0] = controls;
        planetaryTab.setContent(controls);

        Tab planetaryStyleTab = new Tab("Planetary Styles");
        planetaryStyleTab.setClosable(false);
        PlanetaryStyleControls styleControls = new PlanetaryStyleControls(disc[0]);                
        planetaryStyleTab.setContent(styleControls);
        scene.getRoot().addEventHandler(EffectEvent.NEW_PLANETARY_DISC, e -> {
            if (e.object instanceof PlanetaryDisc newDisc) {
                styleControls.setPlanetaryDisc(newDisc);
            }
        });                
        
        TabPane tabPane = new TabPane(solarCycleTab, lensFlareTab, planetaryTab, planetaryStyleTab);

        VBox contentVBox = new VBox(5, tabPane);

        bp.setCenter(contentVBox);

        // Simulated sun disk
        sun = new Circle(15, Color.LIGHTYELLOW);
        sun.setOpacity(1.0);
        sun.setTranslateX(200);
        sun.setTranslateY(200);
        sun.setVisible(false); //needs to be enabled from controls
        parent.getChildren().add(sun);
        enableSolarDrag(sun);

        sunNeonRim = new Circle(sun.getRadius(), Color.TRANSPARENT);
        sunNeonRim.setStroke(Color.CYAN);
        sunNeonRim.setStrokeWidth(1);
        sunNeonRim.setOpacity(0.1); //inverted to alphaMultiplier
        sun.opacityProperty().addListener(cl -> sunNeonRim.setOpacity(1 - sun.getOpacity()));
        sunNeonRim.translateXProperty().bind(sun.translateXProperty());
        sunNeonRim.translateYProperty().bind(sun.translateYProperty());
        sunNeonRim.setMouseTransparent(true);
        sunNeonRim.visibleProperty().bind(sun.visibleProperty());
        parent.getChildren().add(sunNeonRim);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                Point2D sunScene = sun.localToScene(0, 0);
                Point2D center = new Point2D(scene.getWidth() / 2, scene.getHeight() / 2);

                double flareAlpha = FlareOcclusionUtil.computeFlareAlpha(
                        sunScene.getX(), sunScene.getY(),
                        center.getX(), center.getY(),
                        scene.getWidth(), scene.getHeight());
                double occlusionFactor = FlareOcclusionUtil.computeSunOcclusionFactor(
                        sunScene, sun.getRadius(), occluders); // fade radius in pixels
                sun.setOpacity(flareAlpha * occlusionFactor);
                flareGroup.updateOpacity(flareAlpha, occlusionFactor);
                if(controlsRef[0].scatteringEnabledProperty().get())
                    disc[0].updateScattering(1.0 - occlusionFactor); 
            }
        }.start();

        SunPositionTimer sunPositionTimer = new SunPositionTimer(parent, sun);
        sunPositionTimer.start();
        flareGroup.update(sun.getTranslateX(), sun.getTranslateY(),
                sun.getScene().getWidth() / 2.0,
                sun.getScene().getHeight() / 2.0);
        
        scene.getRoot().addEventHandler(EffectEvent.SUN_ARTIFACT_ENABLED, e -> {
            enabled = (boolean) e.object;
            sun.setVisible(enabled);
        });
        scene.getRoot().addEventHandler(EffectEvent.LENSFLARE_ARTIFACT_ENABLED, e -> {
            enabled = (boolean) e.object;
            flareGroup.setVisible(enabled);
        });
        scene.getRoot().addEventHandler(EffectEvent.PLANETARY_STYLE_CHANGE, e -> {
            selectedStyle = (PlanetaryEffectFactory.PlanetStyle ) e.object;
        });
        
    }

    private void enableSolarDrag(Circle sun) {
        final double[] dragOffset = new double[2];

        sun.setOnMousePressed(e -> {
            dragOffset[0] = sun.getTranslateX() - e.getSceneX();
            dragOffset[1] = sun.getTranslateY() - e.getSceneY();
        });

        sun.setOnMouseDragged(e -> {
            double newX = e.getSceneX() + dragOffset[0];
            double newY = e.getSceneY() + dragOffset[1];
            sun.setTranslateX(newX);
            sun.setTranslateY(newY);
        });
        sun.translateXProperty().addListener(e -> {
            flareGroup.update(sun.getTranslateX(), sun.getTranslateY(),
                    sun.getScene().getWidth() / 2.0,
                    sun.getScene().getHeight() / 2.0);
        });
        sun.translateYProperty().addListener(e -> {
            flareGroup.update(sun.getTranslateX(), sun.getTranslateY(),
                    sun.getScene().getWidth() / 2.0,
                    sun.getScene().getHeight() / 2.0);
        });
    }

    private void regenerateDisc(
            PlanetaryDisc[] discRef,
            Pane parent,
            PlanetaryDiscControls controls,
            PlanetaryEffectFactory.PlanetStyle style,
            List<Node> occluders
    ) {
    // Remove existing disc from parent and occluders
        if (discRef[0] != null) {
            occluders.remove(discRef[0].getOccluderShape());
            parent.getChildren().remove(discRef[0]);
        }

        // Create new disc
        double radius = controls.discRadiusProperty().get();
        // Update reference
        discRef[0] = new PlanetaryDisc(radius, style);
        // Position and visibility
        discRef[0].setTranslateY(controls.verticalOffsetProperty().get());
        discRef[0].setTranslateX(controls.horizontalOffsetProperty().get());        
        discRef[0].setVisible(controls.discVisibleProperty().get());
        discRef[0].setScatteringEnabled(controls.scatteringEnabledProperty().get());
        discRef[0].setScatteringColor(controls.scatteringColorProperty().get());
        discRef[0].setScatteringBlurRadius(controls.scatteringBlurRadiusProperty().get());

        // Set debug overlay
        if (controls.debugOccluderProperty().get()) {
            discRef[0].setDebugVisible(true);
        }
        // Ensure occluder shape is valid and detectable
        Node occluder = discRef[0].getOccluderShape();
        occluder.setMouseTransparent(true);   // do not block input

        // Add to scene and back of rendering order
        parent.getChildren().add(discRef[0]);
        discRef[0].toBack();

        // Add occluder shape to logic list
        occluders.add(occluder);
        // NOtify folks with an event
        EffectEvent event = new EffectEvent(EffectEvent.NEW_PLANETARY_DISC, discRef[0]);
        getScene().getRoot().fireEvent(event);
    }
}
