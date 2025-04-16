package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.JukeBoxControlBox;
import edu.jhuapl.trinity.javafx.events.AudioEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaView;

import java.util.ArrayList;
import javafx.application.Platform;

/**
 * @author Sean Phillips
 */
public class JukeBoxPane extends LitPathPane {
    public static int PANE_WIDTH = 500;
    public static int PANE_HEIGHT = 600;
    BorderPane bp;
    JukeBoxControlBox controlBox;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        MediaView mediaView = new MediaView();
        bpOilSpill.setCenter(mediaView);
        return bpOilSpill;
    }

    public JukeBoxPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Juke Box", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;
        controlBox = new JukeBoxControlBox();
        bp.setCenter(controlBox);
        scene.addEventHandler(AudioEvent.MUSIC_FILES_RELOADED, e-> {
            ArrayList<Media> mediaFiles = (ArrayList<Media>) e.object;
            controlBox.reloadTracks(mediaFiles);
        });
        scene.addEventHandler(AudioEvent.CURRENTLY_PLAYING_TRACK, e-> {
            String sourceName = (String) e.object;
            controlBox.selectTrackBySourceName(sourceName);
        });
        
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new AudioEvent(
                AudioEvent.RELOAD_MUSIC_FILES));
        });
    }
    public void setEnableMusic(boolean enabled) {
        controlBox.setEnableMusic(enabled);
    }
}