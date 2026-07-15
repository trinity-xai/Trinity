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
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Simple music jukebox driven by AudioEvent. Supports enable/disable,
 * volume, random track cycling, and fade in/out transitions.
 */
public class JukeBox implements EventHandler<AudioEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(JukeBox.class);

    /** Folder on disk (relative to working dir) to scan for .mp3 files. */
    public static String DEFAULT_MUSIC_PATH = "music/";

    // Known tracks (packaged resources). Keep your provider for defaults.
    public static String NEON_SHADOWS_UNVEIL = "NeonShadowUnveil.mp3";
    public static String DEFAULT_MUSIC_TRACK = NEON_SHADOWS_UNVEIL;

    /** Seconds for volume fades. */
    public static int FADE_OUT_SECONDS = 2;
    public static int FADE_IN_SECONDS = 2;

    private MediaPlayer currentMediaPlayer;
    private Media defaultMedia;

    private boolean transitioning = false;
    private boolean fade = true;
    private boolean cycle = true;
    private boolean enabled = false;

    private final List<Media> mediaFiles = new ArrayList<>();
    private final Scene scene;

    private double currentVolume = 0.25;

    public JukeBox(Scene scene) {
        this.scene = Objects.requireNonNull(scene, "scene");
        try {
            // Use your provider to get a packaged default; fall back if needed.
            defaultMedia = new Media(
                AudioResourceProvider.getResource(DEFAULT_MUSIC_TRACK).toExternalForm()
            );
        } catch (Exception ex) {
            LOG.warn("Default packaged media not found: {}", DEFAULT_MUSIC_TRACK, ex);
            defaultMedia = null;
        }

        // Preload disk library (non-fatal if missing).
        loadMusic();

        // Choose an initial media to bind a player to.
        Media first = !mediaFiles.isEmpty()
                ? mediaFiles.get(0)
                : defaultMedia;
        if (first != null) {
            setMedia(first);
        }
    }

    // ---------------------------
    // Public controls (invokable via AudioEvent or directly)
    // ---------------------------

    public void setEnableMusic(boolean enabled) {
        this.enabled = enabled;
        if (currentMediaPlayer == null) return;

        if (enabled) {
            tryPlay();
            fireNowPlaying();
        } else {
            currentMediaPlayer.pause();
        }
    }

    public void setMusicVolume(double volume) {
        currentVolume = Math.max(0.0, Math.min(1.0, volume));
        if (currentMediaPlayer != null) {
            currentMediaPlayer.setVolume(currentVolume);
        }
    }

    public void setRandomTrack(boolean rightNow) {
        Media media = getRandomMedia();
        if (media == null) {
            LOG.warn("No media available to play.");
            return;
        }
        LOG.info("Now playing: {}", safeName(media));
        if (!rightNow && fade && currentMediaPlayer != null) {
            fadeOutThen(media);
        } else {
            setMedia(media);
        }
    }

    public void setMediaByName(String name) {
        if (name == null) return;
        Media media = getBySourceName(name);
        if (media == null) {
            LOG.warn("Requested track not found: {}", name);
            return;
        }
        if (fade && currentMediaPlayer != null) {
            fadeOutThen(media);
        } else {
            setMedia(media);
        }
    }

    public void setFadeEnabled(boolean fade) { this.fade = fade; }
    public void setCycleEnabled(boolean cycle) { this.cycle = cycle; }

    // ---------------------------
    // AudioEvent handler
    // ---------------------------

    @Override
    public void handle(AudioEvent event) {
        if (event == null || event.getEventType() == null) return;

        if (event.getEventType() == AudioEvent.PLAY_MUSIC_TRACK) {
            setMediaByName((String) event.object);
        } else if (event.getEventType() == AudioEvent.RELOAD_MUSIC_FILES) {
            loadMusic();
        } else if (event.getEventType() == AudioEvent.ENABLE_MUSIC_TRACKS) {
            setEnableMusic(Boolean.TRUE.equals(event.object));
        } else if (event.getEventType() == AudioEvent.SET_MUSIC_VOLUME) {
            if (event.object instanceof Number n) setMusicVolume(n.doubleValue());
        } else if (event.getEventType() == AudioEvent.ENABLE_FADE_TRACKS) {
            setFadeEnabled(Boolean.TRUE.equals(event.object));
        } else if (event.getEventType() == AudioEvent.CYCLE_MUSIC_TRACKS) {
            setCycleEnabled(Boolean.TRUE.equals(event.object));
        }
    }

    // ---------------------------
    // Internals
    // ---------------------------

    private void setMedia(Media media) {
        if (media == null) return;

        // Stop and discard the old player.
        if (currentMediaPlayer != null) {
            try {
                currentMediaPlayer.stop();
            } catch (Exception ignore) {
                // Safe-guard: MediaPlayer may throw if already disposed
            }
        }

        // Create a new player for the media.
        currentMediaPlayer = new MediaPlayer(media);

        // Notify "now playing".
        fireNowPlaying();

        currentMediaPlayer.setVolume(currentVolume);
        currentMediaPlayer.setAutoPlay(true);
        currentMediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);

        currentMediaPlayer.setOnEndOfMedia(() -> {
            if (cycle) setRandomTrack(false);
        });

        // If disabled, keep paused even if AutoPlay was set.
        if (enabled) {
            tryPlay();
        } else {
            currentMediaPlayer.pause();
        }
    }

    private void tryPlay() {
        if (currentMediaPlayer == null) return;
        try {
            currentMediaPlayer.play();
        } catch (MediaException mex) {
            LOG.error("Failed to play media: {}", safeName(currentMediaPlayer.getMedia()), mex);
        }
    }

    private void fireNowPlaying() {
        if (currentMediaPlayer == null) return;
        String sourceName = ResourceUtils.getNameFromURI(currentMediaPlayer.getMedia().getSource());
        Platform.runLater(() ->
            scene.getRoot().fireEvent(new AudioEvent(AudioEvent.CURRENTLY_PLAYING_TRACK, sourceName))
        );
    }

    private Media getRandomMedia() {
        if (!mediaFiles.isEmpty()) {
            Random rando = new Random();
            return mediaFiles.get(rando.nextInt(mediaFiles.size()));
        }
        return defaultMedia;
    }

    private Media getBySourceName(String name) {
        if (name == null) return null;
        for (Media m : mediaFiles) {
            if (name.equals(ResourceUtils.getNameFromURI(m.getSource()))) return m;
        }
        // allow choosing the packaged default by its file name as well
        if (defaultMedia != null && name.equals(ResourceUtils.getNameFromURI(defaultMedia.getSource()))) {
            return defaultMedia;
        }
        return null;
    }

    private void fadeOutThen(Media nextMedia) {
        if (transitioning || currentMediaPlayer == null) {
            setMedia(nextMedia);
            return;
        }
        transitioning = true;
        Timeline tailOff = new Timeline(
            new KeyFrame(Duration.seconds(FADE_OUT_SECONDS),
                new KeyValue(currentMediaPlayer.volumeProperty(), 0.0))
        );
        tailOff.setOnFinished(fin -> {
            if (fade) {
                fadeInFromZero(nextMedia);
            } else {
                setMedia(nextMedia);
                transitioning = false;
            }
        });
        tailOff.play();
    }

    private void fadeInFromZero(Media nextMedia) {
        // Swap to the next media, then fade up to currentVolume.
        setMedia(nextMedia);
        if (currentMediaPlayer == null) {
            transitioning = false;
            return;
        }
        currentMediaPlayer.setVolume(0.0);
        Timeline tailOn = new Timeline(
            new KeyFrame(Duration.seconds(FADE_IN_SECONDS),
                new KeyValue(currentMediaPlayer.volumeProperty(), currentVolume))
        );
        tailOn.setOnFinished(fin -> transitioning = false);
        tailOn.play();
    }

    private static String safeName(Media media) {
        try {
            return ResourceUtils.getNameFromURI(media.getSource());
        } catch (Exception e) {
            return String.valueOf(media);
        }
    }

    /**
     * Load an mp3 file either from classpath (preferred) or filesystem fallback.
     * Returns null if it cannot be resolved to a URL/Media.
     */
    public Media loadMp3AsMedia(File file) {
        if (file == null) return null;
        try {
            URL url = getClass().getClassLoader().getResource(file.getPath());
            if (url == null) {
                if (file.exists()) url = file.toURI().toURL();
            }
            if (url == null) {
                LOG.warn("Could not resolve media URL for {}", file.getPath());
                return null;
            }
            return new Media(url.toString());
        } catch (MalformedURLException | MediaException ex) {
            LOG.error("Could not load music file: {}", file.getName(), ex);
            return null;
        }
    }

    private void loadMusic() {
        mediaFiles.clear();

        // Scan disk folder if present; else fall back to packaged default.
        File folder = new File(DEFAULT_MUSIC_PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            if (defaultMedia != null) mediaFiles.add(defaultMedia);
        } else {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.endsWith(".mp3") || name.endsWith(".MP3")) {
                        Media media = loadMp3AsMedia(file);
                        if (media != null) mediaFiles.add(media);
                    }
                }
            }
            if (mediaFiles.isEmpty() && defaultMedia != null) {
                mediaFiles.add(defaultMedia);
            }
        }

        // Let UI know the library is ready.
        Platform.runLater(() ->
            scene.getRoot().fireEvent(new AudioEvent(AudioEvent.MUSIC_FILES_RELOADED, new ArrayList<>(mediaFiles)))
        );
    }
}
