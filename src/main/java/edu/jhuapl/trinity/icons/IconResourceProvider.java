package edu.jhuapl.trinity.icons;

import java.io.InputStream;

public class IconResourceProvider {

    public static InputStream getResourceAsStream(String name) {
        return IconResourceProvider.class.getResourceAsStream(name);
    }

}
