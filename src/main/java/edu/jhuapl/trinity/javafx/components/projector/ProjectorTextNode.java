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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class ProjectorTextNode extends ProjectorNode {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorTextNode.class);
    String textContent; 
    public static Color colorZero = Color.BLUE.deriveColor(1, 1, 1, 0.25);
    public static Color colorOne = Color.SKYBLUE.deriveColor(1, 1, 1, 0.5);
    public static RadialGradient radialGradient = 
        new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, colorZero),
                new Stop(1.0, colorOne));
    
    public static TextArea convertTextToArea(String textContent) {
        //create a node to render the actual string    
        TextArea textArea = new TextArea(textContent);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setMouseTransparent(true);
        return textArea;
    }
    public static Text convertTextToText(String textContent) {
        //create a node to render the actual string    
        Text text = new Text(textContent.length() > 1000 
            ? textContent.substring(0,1000):textContent);
        text.setWrappingWidth(500);
        text.setMouseTransparent(true);
        text.setFill(Color.ALICEBLUE);
        return text;
    }
    
    public static WritableImage convertTextToImage(String textContent) {
//        //make transparent so it doesn't interfere with subnode transparency effects
//        Background transBack = new Background(new BackgroundFill(
//            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        
        BorderPane bpOilSpill = new BorderPane(convertTextToArea(textContent));
//        bpOilSpill.getStyleClass().add("projector-text-node");

//        Scene scene = new Scene(bpOilSpill, 512, 512, Color.DARKSLATEGRAY);
//        //freaky styley
//        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
//        scene.getStylesheets().add(CSS);
        //snatch the image
        SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setFill(Color.TRANSPARENT);
        WritableImage writableImage = bpOilSpill.snapshot(snapshotParams, null);
        return writableImage;
    }

    public ProjectorTextNode(String textContent) {
        super(convertTextToText(textContent));
        this.textContent = textContent;
        //make transparent so it doesn't interfere with subnode transparency effects
        Background back = new Background(new BackgroundFill(
            radialGradient, CornerRadii.EMPTY, Insets.EMPTY));
        setBackground(back);
        
//        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
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
//        });
//        Tooltip.install(imageView, new Tooltip(analysisConfig.getAnalysisName()));
    }
}
