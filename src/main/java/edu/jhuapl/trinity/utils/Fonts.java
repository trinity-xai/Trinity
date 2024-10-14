/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import javafx.scene.text.Font;


public class Fonts {
    private static final String ROBOTO_BLACK;
    private static String robotoBlackName;

    private static final String ROBOTO_REGULAR;
    private static String robotoRegularName;

    private static final String ROBOTO_THIN;
    private static String robotoThinName;


    static {
        try {
            robotoBlackName = Font.loadFont(Fonts.class.getResourceAsStream("/edu/jhuapl/trinity/css/Roboto-Black.ttf"), 16).getName();
        } catch (Exception exception) {
        }
        try {
            robotoRegularName = Font.loadFont(Fonts.class.getResourceAsStream("/edu/jhuapl/trinity/css/Roboto-Regular.ttf"), 14).getName();
        } catch (Exception exception) {
        }
        try {
            robotoThinName = Font.loadFont(Fonts.class.getResourceAsStream("/edu/jhuapl/trinity/css/Roboto-Thin.ttf"), 12).getName();
        } catch (Exception exception) {
        }

        ROBOTO_BLACK = robotoBlackName;
        ROBOTO_REGULAR = robotoRegularName;
        ROBOTO_THIN = robotoThinName;
    }

    public static Font robotoBlack(final double size) {
        return new Font(ROBOTO_BLACK, size);
    }

    public static Font robotoRegular(final double size) {
        return new Font(ROBOTO_REGULAR, size);
    }

    public static Font robotoThin(final double size) {
        return new Font(ROBOTO_THIN, size);
    }
}
