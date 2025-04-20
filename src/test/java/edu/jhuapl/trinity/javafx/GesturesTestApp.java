package edu.jhuapl.trinity.javafx;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.handlers.ExpandTouchGestureHandler;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GesturesTestApp extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(GesturesTestApp.class);

    //    RadialEntityOverlayPane radialPane;
    Pane touchPointOverlayPane;
    ExpandTouchGestureHandler expandGestureHandler;
    EventHandler touchFilter;

    @Override
    public void start(Stage stage) {
        System.setProperty("com.sun.javafx.touch", "true");
        System.setProperty("com.sun.javafx.isEmbedded", "true");
        touchPointOverlayPane = new Pane();
//        touchPointOverlayPane.setBackground(new Background(
//            new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        expandGestureHandler = new ExpandTouchGestureHandler(touchPointOverlayPane, 50);
        touchPointOverlayPane.addEventHandler(TouchEvent.TOUCH_PRESSED, expandGestureHandler);
        touchPointOverlayPane.addEventHandler(TouchEvent.TOUCH_RELEASED, expandGestureHandler);
        touchPointOverlayPane.addEventHandler(TouchEvent.TOUCH_MOVED, expandGestureHandler);
        touchPointOverlayPane.addEventHandler(TouchEvent.TOUCH_STATIONARY, expandGestureHandler);

        BorderPane bpOilSpill = new BorderPane(touchPointOverlayPane);
//        bpOilSpill.setBackground(Background.EMPTY);
        bpOilSpill.addEventHandler(TouchEvent.TOUCH_PRESSED, e -> {
            LOG.info("BP Oil Spill touch pressed.");
            e.consume();
        });
        Scene scene = new Scene(bpOilSpill, Color.BLACK);
        touchPointOverlayPane.addEventHandler(TouchEvent.TOUCH_PRESSED, e -> {
            LOG.info("touchPointOverlay touch pressed.");
            e.consume();
        });
        touchPointOverlayPane.addEventHandler(ZoomEvent.ZOOM, e -> {
            LOG.info("Overlay zoom.");
        });

//        List<FeatureVector> featureVectors = new ArrayList<>();
//        radialPane = new RadialEntityOverlayPane(scene, featureVectors);
//        RadialEntity radialEntity = createEntity("Gesture Test Entity", 5);
//        radialEntity.setScene(scene);
//        radialPane.addEntity(radialEntity);
//
//        radialEntity.setEmitterColors(Color.CYAN.deriveColor(1, 1, 1, 0.5),
//            Color.CYAN.deriveColor(1, 1, 1, 0.15));
//        radialEntity.setShowEmitter(false);
//
//        radialEntity.setManaged(false);
//        radialEntity.layoutXProperty().bind(bpOilSpill.widthProperty().divide(2.0));
//        radialEntity.layoutYProperty().bind(bpOilSpill.heightProperty().divide(2.0));
//
//        Reflection reflection = new Reflection(10, 0.9, 0.1, 0.4);
//        radialEntity.setEffect(reflection);
//
//        Button addItemButton = new Button("Push Item");
//        addItemButton.setOnAction(e-> {
//            radialEntity.itemReticuleAnimation(90, 180, 360, 0.5, 1.0, 2.0);
//            addItem(radialEntity, "Gesture Item");
//        });
//        Button removeItemButton = new Button("Pop Item");
//        removeItemButton.setOnAction(e-> {
//            if(radialEntity.getTotalItems()>0)
//                removeItem(radialEntity);
//        });
//        Button clearAllButton = new Button("Clear All");
//        clearAllButton.setOnAction(e-> {
//            clearAllItems(radialEntity);
//        });
//
//        CheckBox emitters = new CheckBox("Emitters");
//        emitters.setOnAction(e -> radialEntity.setShowEmitter(emitters.isSelected()));
//        HBox hbox = new HBox(10, addItemButton, removeItemButton, clearAllButton
//            , emitters);
//        bpOilSpill.setTop(hbox);

        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage.setTitle("Gesture tester");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
//    public void clearAllItems(RadialEntity re) {
//        re.clearAllItems();
//        re.resizeItemsToFit();
//        re.showRadialMenu();
//    }
//    public void removeItem(RadialEntity re) {
//        re.removeMenuItem(re.getTotalItems()-1);
//        re.resizeItemsToFit();
//        re.showRadialMenu();
//    }
//    public void addItem(RadialEntity re, String name) {
//        Glow glow = new Glow(0.9);
//        ImageView imageView = ResourceUtils.loadIcon(RadialEntity.DEFAULT_IMAGE_CHIP,
//            RadialEntity.ITEM_FIT_WIDTH);
//        imageView.setEffect(glow);
//        LitRadialMenuItem submenuitem = new LitRadialMenuItem(
//            RadialEntity.ITEM_SIZE,
//            name + "-" + re.getTotalItems(),
//            imageView
//        );
//        re.addMenuItem(submenuitem);
//        re.resizeItemsToFit();
//        re.showRadialMenu();
//    }
//    private Circle dashedCircle(double radius, double strokeWidth,
//            double dashSpacing,  Color strokeColor) {
//        Glow glow = new Glow(10);
//
//        Circle c = new Circle(radius, Color.TRANSPARENT);
//        c.setEffect(glow);
//        c.setStroke(strokeColor);
//        c.setStrokeWidth(strokeWidth);
//        c.setStrokeLineJoin(StrokeLineJoin.MITER);
//        c.setStrokeMiterLimit(50);
//        c.getStrokeDashArray().addAll(dashSpacing);
//        c.setMouseTransparent(true);
//        return c;
//    }
//    public RadialEntity createEntity(String name, int chips) {
//        RadialEntity re = new RadialEntity(name, RadialEntity.ITEM_FIT_WIDTH);
//        re.setText(name);
//        return re;
//    }

    @Override
    public void stop() {
        System.exit(0);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
