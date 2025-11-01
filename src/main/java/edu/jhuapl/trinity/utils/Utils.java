package edu.jhuapl.trinity.utils;

import edu.jhuapl.trinity.utils.loaders.CounterThread;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
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
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

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

    public static Properties readProps(String propsPath, Properties defaults) throws IOException {
        Properties newProps = new Properties();
        if (null != defaults)
            newProps = new Properties(defaults);
        try (InputStream is = new FileInputStream(new File(propsPath))) {
            newProps.load(is);
        }
        return newProps;
    }

    public static Properties readProps(File file, Properties defaults) throws IOException {
        Properties newProps = new Properties();
        if (null != defaults)
            newProps = new Properties(defaults);
        try (InputStream is = new FileInputStream(file)) {
            newProps.load(is);
        }
        return newProps;
    }

    public static void writeProps(String propsPath, Properties madProps, String comments) throws IOException {
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

    public static void logTotalTime(long startTime) {
        LOG.info(totalTimeString(startTime));
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

    //fun parallel FileChannel way to count occurrences such as new line
    public static long charCount(File inputFile, char letter) throws IOException, InterruptedException {
        FileChannel fileChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ);
        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        long chunkSize = fileChannel.size() / numberOfThreads;
        //System.out.println("Using chunkSize " + chunkSize + " with " + numberOfThreads + " threads.");
        List<Thread> threads = new ArrayList<>();
        List<CounterThread> runnables = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            long start = i * chunkSize;
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, start, chunkSize);
            CounterThread runnable = new CounterThread(buffer, (byte) letter);
            runnables.add(runnable);
            Thread counterThread = Thread.startVirtualThread(runnable);
            threads.add(counterThread);
        }
        for (Thread thread : threads) {
            thread.join();
        }
        long result = 0L;
        for (CounterThread runnable : runnables) {
            result += runnable.count();
        }
        return result;
    }

    //non parallel old school way to count lines.
    public static long getFileLineCount(File file) throws IOException {
        long count = -1;
        try (FileInputStream stream = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            count = 0;
            int n;
            while ((n = stream.read(buffer)) > 0) {
                for (int i = 0; i < n; i++) {
                    if (buffer[i] == '\n') count++;
                }
            }
        }
        return count;
    }
}
