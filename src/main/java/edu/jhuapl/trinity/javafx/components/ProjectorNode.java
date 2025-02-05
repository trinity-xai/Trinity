/* Copyright (C) 2021 - 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.components;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.UmapConfig;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
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

/**
 *
 * @author Sean Phillips
 */
public class ProjectorNode extends Pane {
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
            new Insets(-borderWidth/2.0, -borderWidth/2.0, -borderWidth/2.0, -borderWidth/2.0)));
        selectedBorder = new Border(new BorderStroke(Color.CYAN.deriveColor(1, 1, 1, 0.75), 
            BorderStrokeStyle.SOLID, CornerRadii.EMPTY, 
            new BorderWidths(borderWidth), 
            new Insets(-borderWidth/2.0, -borderWidth/2.0, -borderWidth/2.0, -borderWidth/2.0)));

        this.setBorder(Border.EMPTY);
        addEventHandler(MouseEvent.MOUSE_ENTERED, e-> {
            setBorder(hoverBorder);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e-> {
            setBorder(emptyBorder);
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, e-> {
            setBorder(selectedBorder);
            if(e.getButton() == MouseButton.PRIMARY) {
                if(e.getClickCount()==1 && e.isControlDown()) {
                    if(null != umapConfig) {
                        try {
                            getScene().getRoot().fireEvent(new ApplicationEvent(                
                                ApplicationEvent.SHOW_TEXT_CONSOLE, umapConfig.prettyPrint()));
                        } catch (JsonProcessingException ex) {
                            Logger.getLogger(ProjectorNode.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                } else if(e.getClickCount()==1) {
                    if(null != analysisConfig) {
                        getScene().getRoot().fireEvent(new ApplicationEvent(                
                            ApplicationEvent.SHOW_ANALYSISLOG_PANE, analysisConfig, umapConfig));
                    }
                }
            }
        });
    }
}
