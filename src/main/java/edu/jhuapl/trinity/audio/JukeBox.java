package edu.jhuapl.trinity.audio;

import edu.jhuapl.trinity.javafx.components.JukeBoxControlBox;
import edu.jhuapl.trinity.javafx.events.AudioEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.LoggerFactory;

/**
 *
 * @author phillsm1
 */
public class JukeBox implements EventHandler<AudioEvent>{
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(JukeBox.class);
    public static String DEFAULT_MUSIC_PATH = "music/";
    //These are all the known tracks
    public static String NEON_SHADOWS_UNVEIL = "NeonShadowUnveil.mp3";
    public static String DEFAULT_MUSIC_TRACK = NEON_SHADOWS_UNVEIL;
    
    MediaPlayer currentMediaPlayer;
    Media defaultMedia;
    boolean transitioning = false;
    
    List<Media> mediaFiles;
    Scene scene;
    boolean enabled = true;
    double currentVolume = 0.1;
    
    public JukeBox(Scene scene) {
        this.scene = scene;
        defaultMedia = new Media(JukeBox.class.getResource(
            "/edu/jhuapl/trinity/audio/" + DEFAULT_MUSIC_TRACK)
                .toExternalForm());
        mediaFiles = new ArrayList<>();
        loadMusic();
        currentMediaPlayer = new MediaPlayer(mediaFiles.get(0));
        System.out.println("Music loaded.");
    }
    private void setMedia(Media media) {
        if(null != currentMediaPlayer) {
            currentMediaPlayer.stop();
        }
        currentMediaPlayer = new MediaPlayer(media);
        // Sets the audio playback volume. 
        //Its effect will be clamped to the range [0.0, 1.0].
        currentMediaPlayer.setVolume(currentVolume);
        // by setting this property to true, the audio will be played
        currentMediaPlayer.setAutoPlay(true);
        // Play the music in loop
        currentMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        if(enabled) {
            currentMediaPlayer.play();
        } else {
            currentMediaPlayer.pause();
        }
    }
    public void setEnableMusic(boolean enabled) {
        this.enabled = enabled;
        if(enabled) {
            currentMediaPlayer.play();
        } else {
            currentMediaPlayer.pause();
        }
    }
    public void setMusicVolume(double volume) {
        currentVolume = volume;
        //clamp logic
        if(currentVolume < 0) 
            currentVolume = 0;
        else if(currentVolume > 1)
            currentVolume = 1;
        currentMediaPlayer.setVolume(currentVolume);
    }
    private Media getRandomMedia() {
        if(mediaFiles.isEmpty()) return defaultMedia;
        Random rando = new Random();
        return mediaFiles.get(rando.nextInt(mediaFiles.size()));
    }
    public void setRandomTrack(boolean rightNow){
        Media media = getRandomMedia();
        if(!rightNow && null != currentMediaPlayer) {
            transitioning = true;
            Timeline tailoff = new Timeline(
                new KeyFrame(Duration.seconds(1), 
                new KeyValue(currentMediaPlayer.volumeProperty(), 0))
            );
            tailoff.setOnFinished(fin -> {
                setMedia(media);
                transitioning = false;
            });
            tailoff.play();
        } else
            setMedia(media);
    }
    public void setMediaByName(String name) {
        Media media = getBySourceName(name);
        setMedia(media);               
    }
    private String getNameFromURI(String uriString) {
        try {        
            return Paths.get(new URI(uriString)).getFileName().toString();
        } catch (URISyntaxException ex) {
            LOG.error("Could not load URI from: " + uriString);
        }
        return "";
    }
    private Media getBySourceName(String name){
        return mediaFiles.stream()
            .filter(m -> getNameFromURI(m.getSource()).contentEquals(name))
            .findFirst().get();
    }
    @Override
    public void handle(AudioEvent event) {
        if(event.getEventType() == AudioEvent.PLAY_MUSIC_TRACK)
            setMediaByName((String)event.object);
        else if(event.getEventType() == AudioEvent.RELOAD_MUSIC_FILES)
            loadMusic();
        else if(event.getEventType() == AudioEvent.ENABLE_MUSIC_TRACKS)
            setEnableMusic((boolean)event.object);
        else if(event.getEventType() == AudioEvent.SET_MUSIC_VOLUME)
            setMusicVolume((double)event.object);
    }
    
    private void loadMusic(){
        File folder = new File(DEFAULT_MUSIC_PATH);
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            mediaFiles.add(defaultMedia);
            return;
        }
        mediaFiles.clear();
        File[] files = folder.listFiles();
        for(File file : files) {
            if(file.getName().endsWith(".mp3") 
            || file.getName().endsWith(".MP3")) {
                try {
                    Media media = new Media(file.toURI().toURL().toString());
                    mediaFiles.add(media);
                } catch (MalformedURLException ex) {
                    LOG.error("Could not load music file: " + file.getName());
                }
            }
        }
        Platform.runLater(()-> {
            scene.getRoot().fireEvent(new AudioEvent(AudioEvent.MUSIC_FILES_RELOADED, mediaFiles));
        });
    }    
    
}
