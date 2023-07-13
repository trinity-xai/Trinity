package edu.jhuapl.trinity.javafx.components.radial;

import edu.jhuapl.trinity.javafx.javafx3d.Projections3DPane;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Shadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.FileChooser;
import lit.litfx.controls.menus.LitRadialMenuItem;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * @author Sean Phillips
 */
public class PointDistanceMenu extends RadialEntity {
    //momma view this will control and attach to
    Projections3DPane projections3DPane;

    //defaults
    public static double IMAGE_FIT_HEIGHT = 64;
    public static double IMAGE_FIT_WIDTH = 64;
    public static double ITEM_SIZE = 40;
    public static double INNER_RADIUS = 70.0;
    public static double ITEM_FIT_WIDTH = 64.0;
    public static double MENU_RADIUS = 200.0;
    public static double OFFSET = 10.0;
    public static double INITIAL_ANGLE = 0.0;
    public static double STROKE_WIDTH = 2.5;

    static Color bgLg1Color = Color.DARKCYAN.deriveColor(1, 1, 1, 0.2);
    static Color bgLg2Color = Color.LIGHTBLUE.deriveColor(1, 1, 1, 0.5);
    static Color bgMoLg1Color = Color.LIGHTSKYBLUE.deriveColor(1, 1, 1, 0.3);
    static Color bgMoLg2Color = Color.DARKBLUE.deriveColor(1, 1, 1, 0.6);
    static Color strokeColor = Color.ALICEBLUE;
    static Color strokeMouseOnColor = Color.YELLOW;
    static Color outlineColor = Color.GREEN;
    static Color outlineMouseOnColor = Color.LIGHTGREEN;

    static LinearGradient background = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, bgLg1Color), new Stop(0.8, bgLg2Color));
    static LinearGradient backgroundMouseOn = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, bgMoLg1Color), new Stop(0.8, bgMoLg2Color));
    ImageView iv;

    public PointDistanceMenu(Projections3DPane projections3DPane) {
        this(INITIAL_ANGLE, ITEM_SIZE, MENU_RADIUS, OFFSET, projections3DPane);
    }

    public PointDistanceMenu(double initialAngle, double itemSize, double menuSize, double offset, Projections3DPane projections3DPane) {
        super("Metrics", IMAGE_FIT_WIDTH);
        this.projections3DPane = projections3DPane;
        this.scene = projections3DPane.scene;
        setScene(scene);
        buildMenu();

        setMenuItemSize(itemSize);
        setInnerRadius(INNER_RADIUS);
        setGraphicsFitWidth(ITEM_FIT_WIDTH);
        setRadius(menuSize);
        setOffset(offset);
        setInitialAngle(initialAngle);
        setStrokeWidth(STROKE_WIDTH);

        setStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeFill(outlineColor);
        setOutlineStrokeMouseOnFill(outlineMouseOnColor);
        setHideMenuOnItemClick(true);

        setEmitterColors(Color.CYAN.deriveColor(1, 1, 1, 0.5),
            Color.CYAN.deriveColor(1, 1, 1, 0.15));
        setShowEmitter(false);
        setManaged(false);

        iv = ResourceUtils.loadIcon("metric", ITEM_FIT_WIDTH);
        if (null != iv) {
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
            iv.setFitWidth(IMAGE_FIT_WIDTH);
            iv.setFitHeight(IMAGE_FIT_HEIGHT);
            setCenterGraphic(iv);
            getCenterGraphic().setTranslateX(-iv.getFitWidth() / 2.0);
            getCenterGraphic().setTranslateY(-IMAGE_FIT_HEIGHT / 2.0);
        }
        getCenterGroup().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isControlDown() && event.getClickCount() > 1) {
                hideRadialMenu();
                setVisible(false);
                event.consume();
            }
        });
        getCenterGroup().addEventHandler(SwipeEvent.SWIPE_RIGHT, event -> {
            hideRadialMenu();
            event.consume();
        });
        addEventHandler(RotateEvent.ROTATE, event -> {
            setInitialAngle(getInitialAngle() - event.getAngle());
            event.consume();
        });
        itemGroup.visibleProperty().addListener(cl -> {
            if (itemGroup.isVisible()) {
                orbitingCircle.setRadius(getRadius() + ORBITING_CIRCLE_OFFSET);
            } else {
                orbitingCircle.setRadius(getInnerRadius() + ORBITING_CIRCLE_OFFSET);
            }
        });
    }

    public void resetMenuColors() {
        setBackgroundFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, bgLg1Color), new Stop(0.8, bgLg2Color)));
        setBackgroundMouseOnFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, bgMoLg1Color), new Stop(0.8, bgMoLg2Color)));
        setStrokeColor(strokeColor);
        setStrokeMouseOnColor(strokeMouseOnColor);
        setOutlineStrokeFill(outlineColor);
        setOutlineStrokeMouseOnFill(outlineMouseOnColor);
    }

    public void resetMenuSizes() {
        setMenuItemSize(ITEM_SIZE);
        setGraphicsFitWidth(ITEM_FIT_WIDTH);
        setRadius(MENU_RADIUS);
        setOffset(OFFSET);
        setInitialAngle(INITIAL_ANGLE);
        setStrokeWidth(STROKE_WIDTH);
    }


    public void buildMenu() {
        Glow glow = new Glow(0.5);
        Shadow shadow = new Shadow(BlurType.GAUSSIAN, Color.ALICEBLUE, 50);
        setOutlineEffect(shadow);

        ImageView manifold = ResourceUtils.loadIcon("manifold", ITEM_FIT_WIDTH);
        manifold.setEffect(glow);

        ImageView search = ResourceUtils.loadIcon("search", ITEM_FIT_WIDTH);
        search.setEffect(glow);

        ImageView filter = ResourceUtils.loadIcon("filter", ITEM_FIT_WIDTH);
        filter.setEffect(glow);

        ImageView copy = ResourceUtils.loadIcon("copy", ITEM_FIT_WIDTH);
        copy.setEffect(glow);

        ImageView save = ResourceUtils.loadIcon("save", ITEM_FIT_WIDTH);
        save.setEffect(glow);

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Metadata Search", search, e -> {
            System.out.println("Search by Point Type...");
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Metadata Filter", filter, e -> {
            System.out.println("Filter by Point Type...");
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE, "Manifolds", manifold, e -> {
            System.out.println("Select nearest Manifold...");
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Copy Snapshot", copy, e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(projections3DPane.snapshot(new SnapshotParameters(), null));
            clipboard.setContent(content);
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Save as Image", save, e -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save scene as...");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                WritableImage image = this.snapshot(new SnapshotParameters(), null);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException ioe) {
                    // TODO: handle exception here
                }
            }
        }));
    }
}
