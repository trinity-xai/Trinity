package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.javafx.events.AudioEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class JukeBoxControlBox extends VBox {
    private static final Logger LOG = LoggerFactory.getLogger(JukeBoxControlBox.class);
    ListView<String> musicTracks;
    CheckBox enableFadeCheckBox;

    public JukeBoxControlBox() {
        musicTracks = new ListView();

        ImageView iv = ResourceUtils.loadIcon("waitingforimage", 128);
        VBox placeholder = new VBox(10, iv, new Label("No soundtrack loaded"));
        placeholder.setAlignment(Pos.CENTER);
        musicTracks.setPlaceholder(placeholder);

        Button reloadTrackList = new Button("Reload Track List");
        reloadTrackList.setOnAction(e -> {
            reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
                AudioEvent.RELOAD_MUSIC_FILES));
        });
        Button playTrack = new Button("Play Selected Track");
        playTrack.setOnAction(e -> {
            String trackName = musicTracks.getSelectionModel().getSelectedItem();
            if (null != trackName) {
                reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
                    AudioEvent.PLAY_MUSIC_TRACK, trackName));
            }
        });
        CheckBox enableMusicCheckBox = new CheckBox("Enable Music");
        enableMusicCheckBox.setOnAction(e -> {
            reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
                AudioEvent.ENABLE_MUSIC_TRACKS, enableMusicCheckBox.isSelected()));
        });
        enableFadeCheckBox = new CheckBox("Enable Fade");
        enableFadeCheckBox.setSelected(true);
        enableFadeCheckBox.setOnAction(e -> {
            reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
                AudioEvent.ENABLE_FADE_TRACKS, enableFadeCheckBox.isSelected()));
        });
        CheckBox cycleTracksCheckBox = new CheckBox("Cycle Tracks");
        cycleTracksCheckBox.setSelected(true);
        cycleTracksCheckBox.setOnAction(e -> {
            reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
                AudioEvent.CYCLE_MUSIC_TRACKS, cycleTracksCheckBox.isSelected()));
        });

        HBox volumeLabelHBox = new HBox(50, new Label("Music Volume"),
            enableMusicCheckBox, enableFadeCheckBox, cycleTracksCheckBox);

        Slider volumeSlider = new Slider(0, 1, 0.25);
        volumeSlider.valueProperty().addListener(e -> {
            reloadTrackList.getScene().getRoot().fireEvent(new AudioEvent(
                AudioEvent.SET_MUSIC_VOLUME, volumeSlider.getValue()));
        });
        VBox volumeVBox = new VBox(5, volumeLabelHBox, volumeSlider);
        setSpacing(10);

        getChildren().addAll(
            volumeVBox,
            new HBox(5, reloadTrackList, playTrack),
            musicTracks
        );
    }

    public void setEnableMusic(boolean enabled) {
        enableFadeCheckBox.setSelected(enabled);
    }

    public void selectTrackBySourceName(String name) {
        for (int i = 0; i < musicTracks.getItems().size(); i++) {
            if (musicTracks.getItems().get(i).contentEquals(name)) {
                if (musicTracks.getSelectionModel().getSelectedIndex() != i) {
                    musicTracks.scrollTo(i);
                    musicTracks.getSelectionModel().select(i);
                }
                return;
            }
        }
    }

    public void reloadTracks(ArrayList<Media> mediaFiles) {
        musicTracks.getItems().clear();
        mediaFiles.forEach(m -> {
            try {
                musicTracks.getItems().add(
                    Paths.get(new URI(m.getSource())).getFileName().toString()
                );
            } catch (URISyntaxException ex) {
                LOG.error(null, ex);
            }
        });
    }
}
