package edu.jhuapl.trinity.audio;

import java.io.InputStream;
import java.net.URL;

public class AudioResourceProvider {

    public static InputStream getResourceAsStream(String name) {
        return AudioResourceProvider.class.getResourceAsStream(name);
    }

    public static URL getResource(String name) {
        return AudioResourceProvider.class.getResource(name);
    }

}
