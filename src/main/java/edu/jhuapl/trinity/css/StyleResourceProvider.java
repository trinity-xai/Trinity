package edu.jhuapl.trinity.css;

import java.io.InputStream;
import java.net.URL;

public class StyleResourceProvider {

    public static InputStream getResourceAsStream(String name) {
        return StyleResourceProvider.class.getResourceAsStream(name);
    }

    public static URL getResource(String name) {
        return StyleResourceProvider.class.getResource(name);
    }

}
