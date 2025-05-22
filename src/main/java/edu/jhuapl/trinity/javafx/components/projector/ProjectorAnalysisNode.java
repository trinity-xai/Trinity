package edu.jhuapl.trinity.javafx.components.projector;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.jhuapl.trinity.data.messages.xai.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class ProjectorAnalysisNode extends ProjectorNode {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorAnalysisNode.class);
    UmapConfig umapConfig;
    AnalysisConfig analysisConfig;

    public ProjectorAnalysisNode(Image image, UmapConfig umapConfig, AnalysisConfig analysisConfig) {
        super(image);
        this.umapConfig = umapConfig;
        this.analysisConfig = analysisConfig;
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                if (e.getClickCount() == 1 && e.isControlDown()) {
                    if (null != umapConfig) {
                        try {
                            getScene().getRoot().fireEvent(new ApplicationEvent(
                                ApplicationEvent.SHOW_TEXT_CONSOLE, umapConfig.prettyPrint()));
                        } catch (JsonProcessingException ex) {
                            LOG.error(null, ex);
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
