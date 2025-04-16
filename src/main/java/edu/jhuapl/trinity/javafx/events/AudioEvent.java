package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class AudioEvent extends Event {

    public static final EventType<AudioEvent> NEW_AUDIO_FILE = new EventType(ANY, "NEW_AUDIO_FILE");
    public static final EventType<AudioEvent> PLAY_MUSIC_TRACK = new EventType(ANY, "PLAY_MUSIC_TRACK");
    public static final EventType<AudioEvent> RELOAD_MUSIC_FILES = new EventType(ANY, "RELOAD_MUSIC_FILES");
    public static final EventType<AudioEvent> MUSIC_FILES_RELOADED = new EventType(ANY, "MUSIC_FILES_RELOADED");
    public static final EventType<AudioEvent> ENABLE_MUSIC_TRACKS = new EventType(ANY, "ENABLE_MUSIC_TRACKS");
    public static final EventType<AudioEvent> SET_MUSIC_VOLUME = new EventType(ANY, "SET_MUSIC_VOLUME");
    public static final EventType<AudioEvent> ENABLE_FADE_TRACKS = new EventType(ANY, "ENABLE_CROSSFADE_TRACKS");
    public static final EventType<AudioEvent> CYCLE_MUSIC_TRACKS = new EventType(ANY, "CYCLE_MUSIC_TRACKS");
    public static final EventType<AudioEvent> CURRENTLY_PLAYING_TRACK = new EventType(ANY, "CURRENTLY_PLAYING_TRACK");
    
    public Object object = null;

    public AudioEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public AudioEvent(EventType<? extends Event> arg0, Object object) {
        this(arg0);
        this.object = object;
    }
}
