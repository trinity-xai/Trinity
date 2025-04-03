/* Copyright (C) 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.xai.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class ProjectorNode extends Pane {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorNode.class);
    ImageView imageView;
    UmapConfig umapConfig;
    AnalysisConfig analysisConfig;
    Border hoverBorder;
    Border emptyBorder;
    Border selectedBorder;
    int borderWidth = 50;

    public ProjectorNode(Image image, UmapConfig umapConfig, AnalysisConfig analysisConfig) {
        imageView = new ImageView(image);
        this.umapConfig = umapConfig;
        this.analysisConfig = analysisConfig;
        getChildren().add(imageView);
        emptyBorder = Border.EMPTY;
        hoverBorder = new Border(new BorderStroke(Color.LIGHTCYAN.deriveColor(1, 1, 1, 0.75),
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(borderWidth),
            new Insets(-borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0)));
        selectedBorder = new Border(new BorderStroke(Color.CYAN.deriveColor(1, 1, 1, 0.75),
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
            new BorderWidths(borderWidth),
            new Insets(-borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0, -borderWidth / 2.0)));

        setBorder(Border.EMPTY);
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            requestFocus();
            setBorder(hoverBorder);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            setBorder(emptyBorder);
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            requestFocus();
            setBorder(selectedBorder);
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 1 && e.isControlDown()) {
                    if (null != umapConfig) {
                        try {
                            getScene().getRoot().fireEvent(new ApplicationEvent(
                                ApplicationEvent.SHOW_TEXT_CONSOLE, umapConfig.prettyPrint()));
                        } catch (JsonProcessingException ex) {
                            LOG.error(ex.getMessage());
                        }
                    }
                } else if (e.getClickCount() == 1) {
                    if (null != analysisConfig) {
                        getScene().getRoot().fireEvent(new ApplicationEvent(
                            ApplicationEvent.SHOW_ANALYSISLOG_PANE, analysisConfig, umapConfig));
                    }
                }
            }
        });
        Tooltip.install(imageView, new Tooltip(analysisConfig.getAnalysisName()));
    }
}
