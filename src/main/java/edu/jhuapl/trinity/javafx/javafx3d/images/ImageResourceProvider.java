package edu.jhuapl.trinity.javafx.javafx3d.images;

import java.io.InputStream;

public class ImageResourceProvider {

    public static InputStream getResourceAsStream(String name){
        return ImageResourceProvider.class.getResourceAsStream(name);
    }

}
