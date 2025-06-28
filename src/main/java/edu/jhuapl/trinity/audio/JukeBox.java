package edu.jhuapl.trinity.audio;

import edu.jhuapl.trinity.javafx.events.AudioEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Sean Phillips
 */
public class JukeBox implements EventHandler<AudioEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(JukeBox.class);
    public static String DEFAULT_MUSIC_PATH = "music/";
    //These are all the known tracks
    public static String NEON_SHADOWS_UNVEIL = "NeonShadowUnveil.mp3";
    public static String DEFAULT_MUSIC_TRACK = NEON_SHADOWS_UNVEIL;
    public static int FADE_OUT_SECONDS = 2;
    public static int FADE_IN_SECONDS = 2;

    MediaPlayer currentMediaPlayer;
    Media defaultMedia = null;
    boolean transitioning = false;
    boolean fade = true;
    boolean cycle = true;

    List<Media> mediaFiles;
    Scene scene;
    boolean enabled = false;
    double currentVolume = 0.25;

    public JukeBox(Scene scene) {
        this.scene = scene;
        defaultMedia = new Media(AudioResourceProvider.getResource(DEFAULT_MUSIC_TRACK).toExternalForm());
        mediaFiles = new ArrayList<>();
        try {
            if (null != defaultMedia)
                setMedia(defaultMedia);
            loadMusic();
            if (!mediaFiles.isEmpty())
                setMedia(mediaFiles.get(0));
        } catch (Exception ex) {
            LOG.warn("No music files loaded at JukeBox startup.");
        }
    }

    private void setMedia(Media media) {
        if (null != currentMediaPlayer) {
            currentMediaPlayer.stop();
        }
        currentMediaPlayer = new MediaPlayer(media);
        //@DEBUG SMP
        //System.out.println(media.getSource());
        String sourceName = ResourceUtils.getNameFromURI(media.getSource());
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new AudioEvent(AudioEvent.CURRENTLY_PLAYING_TRACK, sourceName));
        });
        // Sets the audio playback volume.
        //Its effect will be clamped to the range [0.0, 1.0].
        currentMediaPlayer.setVolume(currentVolume);
        // by setting this property to true, the audio will be played
        currentMediaPlayer.setAutoPlay(true);
        // Play the music in loop
        currentMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        currentMediaPlayer.setOnEndOfMedia(() -> {
            if (cycle)
                setRandomTrack(false);
        });
        if (enabled) {
            currentMediaPlayer.play();
        } else {
            currentMediaPlayer.pause();
        }
    }

    public void setEnableMusic(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            currentMediaPlayer.play();
            String sourceName = ResourceUtils.getNameFromURI(currentMediaPlayer.getMedia().getSource());
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new AudioEvent(AudioEvent.CURRENTLY_PLAYING_TRACK, sourceName));
            });
        } else {
            currentMediaPlayer.pause();
        }
    }

    public void setMusicVolume(double volume) {
        currentVolume = volume;
        //clamp logic
        if (currentVolume < 0)
            currentVolume = 0;
        else if (currentVolume > 1)
            currentVolume = 1;
        currentMediaPlayer.setVolume(currentVolume);
    }

    private Media getRandomMedia() {
        if (mediaFiles.isEmpty()) return defaultMedia;
        Random rando = new Random();
        return mediaFiles.get(rando.nextInt(mediaFiles.size()));
    }

    public void setRandomTrack(boolean rightNow) {
        Media media = getRandomMedia();
        if (null != media) {
            LOG.info("Now playing: {}", media.getSource());
            if (!rightNow && fade && null != currentMediaPlayer) {
                fadeOut(media);
            } else
                setMedia(media);
        }
    }

    private void fadeIn(Media nextMedia) {
        transitioning = true;
        currentMediaPlayer.setVolume(0);
        Timeline tailOn = new Timeline(
            new KeyFrame(Duration.seconds(0.1), e -> setMedia(nextMedia)),
            new KeyFrame(Duration.seconds(FADE_IN_SECONDS),
                new KeyValue(currentMediaPlayer.volumeProperty(), currentVolume))
        );
        tailOn.setOnFinished(fin -> {
            transitioning = false;
        });
        tailOn.play();
    }

    private void fadeOut(Media nextMedia) {
        transitioning = true;
        Timeline tailoff = new Timeline(
            new KeyFrame(Duration.seconds(FADE_OUT_SECONDS),
                new KeyValue(currentMediaPlayer.volumeProperty(), 0))
        );
        tailoff.setOnFinished(fin -> {
            if (fade)
                fadeIn(nextMedia);
            else
                setMedia(nextMedia);
            transitioning = false;
        });
        tailoff.play();
    }

    public void setMediaByName(String name) {
        Media media = getBySourceName(name);
        if (fade && null != currentMediaPlayer) {
            fadeOut(media);
        } else
            setMedia(media);
    }

    private Media getBySourceName(String name) {
        return mediaFiles.stream()
            .filter(m -> ResourceUtils.getNameFromURI(m.getSource()).contentEquals(name))
            .findFirst().get();
    }

    @Override
    public void handle(AudioEvent event) {
        if (event.getEventType() == AudioEvent.PLAY_MUSIC_TRACK)
            setMediaByName((String) event.object);
        else if (event.getEventType() == AudioEvent.RELOAD_MUSIC_FILES)
            loadMusic();
        else if (event.getEventType() == AudioEvent.ENABLE_MUSIC_TRACKS)
            setEnableMusic((boolean) event.object);
        else if (event.getEventType() == AudioEvent.SET_MUSIC_VOLUME)
            setMusicVolume((double) event.object);
        else if (event.getEventType() == AudioEvent.ENABLE_FADE_TRACKS)
            fade = (boolean) event.object;
        else if (event.getEventType() == AudioEvent.CYCLE_MUSIC_TRACKS)
            cycle = (boolean) event.object;
    }

    public Media loadMp3AsMedia(File file) throws FileNotFoundException, MalformedURLException {
        URL url = getClass().getClassLoader().getResource(file.getPath());
        if (url == null) {
            // If the mp3 file is not found as a resource, try to load it as a file
            if (file.exists()) {
                url = file.toURI().toURL();
            }
        }
        Media media = new Media(url.toString());
        return media;
    }

    private void loadMusic() {
        mediaFiles.clear();
        File folder = new File(DEFAULT_MUSIC_PATH);
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            if (null != defaultMedia)
                mediaFiles.add(defaultMedia);
        } else {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (file.getName().endsWith(".mp3")
                    || file.getName().endsWith(".MP3")) {
                    try {
                        //Media media = new Media(file.toURI().toURL().toString());
                        Media media = loadMp3AsMedia(file);
                        mediaFiles.add(media);
                    } catch (MalformedURLException | FileNotFoundException ex) {
                        LOG.error("Could not load music file: " + file.getName());
                    }
                }
            }
        }
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new AudioEvent(AudioEvent.MUSIC_FILES_RELOADED, mediaFiles));
        });
    }
}
