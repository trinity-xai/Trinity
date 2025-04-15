package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.JukeBoxControlBox;
import edu.jhuapl.trinity.javafx.events.AudioEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class JukeBoxPane extends LitPathPane {
//    private static final Logger LOG = LoggerFactory.getLogger(JukeBoxPane.class);
    BorderPane bp;
    JukeBoxControlBox controlBox;
    public static int PANE_WIDTH = 600;
//    public static double NODE_WIDTH = PANE_WIDTH - 50;
//    public static double NODE_HEIGHT = NODE_WIDTH / 2.0;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        MediaView mediaView = new MediaView();
        bpOilSpill.setCenter(mediaView);
        return bpOilSpill;
    }

    public JukeBoxPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, 750, createContent(),
            "Juke Box", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;
        controlBox = new JukeBoxControlBox();
        bp.setCenter(controlBox);
        scene.addEventHandler(AudioEvent.MUSIC_FILES_RELOADED, e-> {
            ArrayList<Media> mediaFiles = (ArrayList<Media>) e.object;
            controlBox.reloadTracks(mediaFiles);
        });
    }

}
