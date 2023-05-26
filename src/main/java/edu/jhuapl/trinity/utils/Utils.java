package edu.jhuapl.trinity.utils;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.scene.paint.Color;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Enum that provides general helper methods used sparsely throughout the application
 * but don't belong to a specific package location.
 *
 * @author Sean Phillips
 */
public enum Utils {
    INSTANCE;

    public static final double EPSILON = 1E-10;
    public static String DEFAULT_SETTINGS_JSON = "settings.json";

    /**
     * @param min
     * @param value
     * @param max
     * @return
     * @adapted ControlsFX 11.0.1
     * @link https://github.com/controlsfx/controlsfx/blob/master/controlsfx/src/main/java/org/controlsfx/tools/Utils.java
     * Simple utility function which clamps the given value to be strictly
     * between the min and max values.
     */
    public static double clamp(double min, double value, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    /**
     * @param less
     * @param value
     * @param more
     * @return
     * @adapted ControlsFX 11.0.1
     * @link https://github.com/controlsfx/controlsfx/blob/master/controlsfx/src/main/java/org/controlsfx/tools/Utils.java
     * Utility function which returns either {@code less} or {@code more}
     * depending on which {@code value} is closer to.If {@code value} is perfectly between them, then either may be returned.
     */
    public static double nearest(double less, double value, double more) {
        double lessDiff = value - less;
        double moreDiff = more - value;
        if (lessDiff < moreDiff) return less;
        return more;
    }

    public static int factorial(int n) {
        int factorialResult = 1;
        for (int i = 1; i <= n; i++) {
            factorialResult = factorialResult * i;
        }
        return factorialResult;
    }

    public static double[] linspace(double min, double max, int points) {
        double[] d = new double[points];
        for (int i = 0; i < points; i++) {
            d[i] = min + i * (max - min) / (points - 1);
        }
        return d;
    }

    public static Properties readProps(String propsPath, Properties defaults) throws FileNotFoundException, IOException {
        Properties newProps = new Properties();
        if (null != defaults)
            newProps = new Properties(defaults);
        try (InputStream is = new FileInputStream(new File(propsPath))) {
            newProps.load(is);
        }
        return newProps;
    }

    public static Properties readProps(File file, Properties defaults) throws FileNotFoundException, IOException {
        Properties newProps = new Properties();
        if (null != defaults)
            newProps = new Properties(defaults);
        try (InputStream is = new FileInputStream(file)) {
            newProps.load(is);
        }
        return newProps;
    }

    public static void writeProps(String propsPath, Properties madProps, String comments) throws FileNotFoundException, IOException {
        try (OutputStream os = new FileOutputStream(new File(propsPath))) {
            madProps.store(os, comments);
        }
    }

    public static String totalTimeString(long startTime) {
        long estimatedTime = System.nanoTime() - startTime;
        long totalNanos = estimatedTime;
        long s = totalNanos / 1000000000;
        totalNanos -= s * 1000000000;
        long ms = totalNanos / 1000000;
        totalNanos -= ms * 1000000;

        long us = totalNanos / 1000;
        totalNanos -= us * 1000;
        return "Total elapsed time: " + s + ":s:" + ms + ":ms:" + us + ":us:" + totalNanos + ":ns";
    }

    public static void printTotalTime(long startTime) {
        System.out.println(totalTimeString(startTime));
    }

    public static double roundTo(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public static double roundFormat(double value) {
        DecimalFormat df = new DecimalFormat("#.####");
        return Double.parseDouble(df.format(value));
    }

    public static double roundFormat(double value, int places) {
        String format = "#.";
        for (int i = 0; i < places; i++)
            format += "#";
        DecimalFormat df = new DecimalFormat(format);
        return Double.parseDouble(df.format(value));
    }

    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        return map.entrySet()
            .stream()
            .filter(entry -> Objects.equals(entry.getValue(), value))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    public static double calculateNthRoot(double base, double n) {
        return Math.pow(Math.E, Math.log(base) / n);
    }

    public static int getRandom(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("Min " + min + " greater than max " + max);
        }
        return (int) ((long) min + Math.random() * ((long) max - min + 1));
    }

    public static String convertColorToString(Color color) {
        int green = (int) (color.getGreen() * 255);
        int red = (int) (color.getRed() * 255);
        int blue = (int) (color.getBlue() * 255);
        int opacity = (int) (color.getOpacity() * 255);
        return Integer.toHexString(red) + Integer.toHexString(green)
            + Integer.toHexString(blue) + Integer.toHexString(opacity);
    }

    public static String millisToString(long millis) {
        return String.format("%02d:%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1),
            TimeUnit.MILLISECONDS.toMillis(millis) % TimeUnit.SECONDS.toMillis(1));
    }
}
