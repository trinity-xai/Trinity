package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.utils.fun.FlareOcclusionUtil;
import edu.jhuapl.trinity.utils.fun.LensFlareControls;
import edu.jhuapl.trinity.utils.fun.LensFlareGroup;
import edu.jhuapl.trinity.utils.fun.SunPositionControls;
import edu.jhuapl.trinity.utils.fun.SunPositionTimer;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.collections.ListChangeListener;
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
    public static double DEFAULT_FIT_WIDTH = 512;
    public static double DEFAULT_TITLEDPANE_WIDTH = 256;
    public static double DEFAULT_LABEL_WIDTH = 64;
    public static int PANE_WIDTH = 600;
    public static int PANE_HEIGHT = 600;

    BorderPane bp;
    SunPositionControls sunPositionControls;
    LensFlareControls lensFlareControls;
    private double dragOffsetX, dragOffsetY;
    private LensFlareGroup flareGroup;    
    List<Node> occluders;
    boolean enabled = false;

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
        parent.getChildren().add(flareGroup);
        lensFlareControls = new LensFlareControls(flareGroup);        
        lensFlareTab.setContent(lensFlareControls);
        TabPane tabPane = new TabPane(solarCycleTab, lensFlareTab);

        VBox contentVBox = new VBox(5, tabPane);

        bp.setCenter(contentVBox);
        
        occluders = List.of(this);
        parent.getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(ListChangeListener.Change<? extends Node> change) {
                change.next();
                System.out.println("New Child added: " + change.getAddedSubList().toString());
            }
        });
        
        // Simulated sun disk
        Circle sun = new Circle(15, Color.LIGHTYELLOW);
        sun.setOpacity(1.0);
        sun.setTranslateX(200);
        sun.setTranslateY(200);
        parent.getChildren().add(sun);
        enableSolarDrag(sun);        

        Circle sunNeonRim = new Circle(sun.getRadius(), Color.TRANSPARENT);
        sunNeonRim.setStroke(Color.CYAN);
        sunNeonRim.setStrokeWidth(1);
        sunNeonRim.setOpacity(0.1); //inverted to alphaMultiplier
        sun.opacityProperty().addListener(cl -> sunNeonRim.setOpacity(1-sun.getOpacity()));
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
            }
        }.start();        
        
        SunPositionTimer sunPositionTimer = new SunPositionTimer(parent, sun);
        sunPositionTimer.start();        
        scene.getRoot().addEventHandler(EffectEvent.SUN_ARTIFACT_ENABLED, e -> {
            enabled  = (boolean) e.object;
            sun.setVisible(enabled);
            flareGroup.setVisible(enabled);
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
}
