package edu.jhuapl.trinity.javafx.javafx3d.images;

import java.io.InputStream;
import java.net.URL;

public class ImageResourceProvider {

    public static InputStream getResourceAsStream(String name) {
        return ImageResourceProvider.class.getResourceAsStream(name);
    }

    public static URL getResource(String name) {
        return ImageResourceProvider.class.getResource(name);
    }

}
