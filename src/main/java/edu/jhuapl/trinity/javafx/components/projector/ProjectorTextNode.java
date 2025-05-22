package edu.jhuapl.trinity.javafx.components.projector;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class ProjectorTextNode extends ProjectorNode {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorTextNode.class);
    String textContent; 
    
    public static WritableImage convertTextToImage(String textContent) {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        
        //create a node to render the actual string    
        TextArea textArea = new TextArea(textContent);
        textArea.setWrapText(true);
        BorderPane bpOilSpill = new BorderPane(textArea);
        bpOilSpill.getStyleClass().add("projector-text-node");

        Scene scene = new Scene(bpOilSpill, 512, 512, Color.DARKSLATEGRAY);
        //freaky styley
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        //snatch the image
        SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setFill(Color.TRANSPARENT);
        WritableImage writableImage = bpOilSpill.snapshot(snapshotParams, null);
        return writableImage;
    }

    public ProjectorTextNode(String textContent) {
        super(convertTextToImage(textContent));
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
//            if (e.getButton() == MouseButton.PRIMARY) {
//                if (e.getClickCount() == 1 && e.isControlDown()) {
//                    if (null != umapConfig) {
//                        try {
//                            getScene().getRoot().fireEvent(new ApplicationEvent(
//                                ApplicationEvent.SHOW_TEXT_CONSOLE, umapConfig.prettyPrint()));
//                        } catch (JsonProcessingException ex) {
//                            LOG.error(null, ex);
//                        }
//                    }
//                } else if (e.getClickCount() == 1) {
//                    if (null != analysisConfig) {
//                        getScene().getRoot().fireEvent(new ApplicationEvent(
//                            ApplicationEvent.SHOW_ANALYSISLOG_PANE, analysisConfig, umapConfig));
//                    }
//                }
//            }
        });
//        Tooltip.install(imageView, new Tooltip(analysisConfig.getAnalysisName()));
    }
}
