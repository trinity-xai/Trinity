package edu.jhuapl.trinity.audio;

import java.net.URL;

public class AudioResourceProvider {

    public static URL getResource(String name) {
        return AudioResourceProvider.class.getResource(name);
    }

}
