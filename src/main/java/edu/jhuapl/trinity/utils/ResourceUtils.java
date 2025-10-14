package edu.jhuapl.trinity.utils;

import edu.jhuapl.trinity.audio.AudioResourceProvider;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.files.CdcCsvFile;
import edu.jhuapl.trinity.data.files.CdcTissueGenesFile;
import edu.jhuapl.trinity.data.files.ClusterCollectionFile;
import edu.jhuapl.trinity.data.files.CocoAnnotationFile;
import edu.jhuapl.trinity.data.files.CyberReporterFile;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.files.GaussianMixtureCollectionFile;
import edu.jhuapl.trinity.data.files.GraphDirectedCollectionFile;
import edu.jhuapl.trinity.data.files.LabelConfigFile;
import edu.jhuapl.trinity.data.files.ManifoldDataFile;
import edu.jhuapl.trinity.data.files.McclodSplitDataTsvFile;
import edu.jhuapl.trinity.data.files.SaturnFile;
import edu.jhuapl.trinity.data.files.SemanticMapCollectionFile;
import edu.jhuapl.trinity.data.files.ShapleyCollectionFile;
import edu.jhuapl.trinity.data.files.TextEmbeddingCollectionFile;
import edu.jhuapl.trinity.data.files.VectorMaskCollectionFile;
import edu.jhuapl.trinity.data.files.ZeroPilotLatentsFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.terrain.FireAreaTextFile;
import edu.jhuapl.trinity.data.terrain.TerrainTextFile;
import edu.jhuapl.trinity.icons.IconResourceProvider;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.AudioEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.GaussianMixtureEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.SemanticMapEvent;
import edu.jhuapl.trinity.javafx.events.TerrainEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.javafx.javafx3d.images.ImageResourceProvider;
import edu.jhuapl.trinity.utils.loaders.CdcTissueGenesLoader;
import edu.jhuapl.trinity.utils.loaders.FeatureCollectionLoader;
import edu.jhuapl.trinity.utils.loaders.GraphDirectedCollectionLoader;
import edu.jhuapl.trinity.utils.loaders.McclodSplitDataLoader;
import edu.jhuapl.trinity.utils.loaders.SaturnLoader;
import edu.jhuapl.trinity.utils.loaders.ShapleyCollectionLoader;
import edu.jhuapl.trinity.utils.loaders.TextEmbeddingsLoader;
import edu.jhuapl.trinity.utils.loaders.VectorMaskCollectionLoader;
import edu.jhuapl.trinity.utils.loaders.ZeroPilotLatentsLoader;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author Sean Phillips
 */
public enum ResourceUtils {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtils.class);

    public static List<String> getClasspathEntriesByPath(String path) throws NullPointerException, IOException {
        InputStream is = ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        StringBuilder sb = new StringBuilder();
        byte[] buffer = is.readAllBytes();
        sb.append(new String(buffer, Charset.defaultCharset()));

        return Arrays
            .asList(sb.toString().split("\n")) // Convert StringBuilder to individual lines
            .stream() // Stream the list
            .filter(line -> line.trim().length() > 0) // Filter out empty lines
            .collect(Collectors.toList());              // Collect remaining lines into a List again
    }

    /**
     * List directory contents for a resource folder. Not recursive. This is
     * basically a brute-force implementation. Works for regular files and also
     * JARs.
     *
     * @param clazz Any java class that lives in the same place as the resources
     *              you want.
     * @param path  Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException
     * @throws IOException
     * @author Greg Briggs
     */
    public static String[] getResourceListing(Class clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            LOG.info("dirURL {}", dirURL.toString());
            /* A file path: easy enough */
            return new File(dirURL.toURI()).list();
        }

        if (dirURL == null) {
            LOG.info("dirURL is null...");

            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL.getProtocol().equals("jar")) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
            Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
            Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(path)) { //filter according to the path
                    String entry = name.substring(path.length());
                    int checkSubdir = entry.indexOf("/");
                    if (checkSubdir >= 0) {
                        // if it is a subdirectory, we just return the directory name
                        entry = entry.substring(0, checkSubdir);
                    }
                    result.add(entry);
                }
            }
            return result.toArray(String[]::new);
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    public static List<String> getFilenamesForDirnameFromCP(String directoryName) throws URISyntaxException, UnsupportedEncodingException, IOException {
        List<String> filenames = new ArrayList<>();

        URL url = ResourceUtils.class.getResource(directoryName);
        if (url != null) {
            if (url.getProtocol().equals("file")) {
                File file = Paths.get(url.toURI()).toFile();
                if (file != null) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File filename : files) {
                            //filenames.add(filename.toString());
                            filenames.add(filename.getName());
                        }
                    }
                }
            } else if (url.getProtocol().equals("jar")) {
                String dirname = directoryName + "/";
                String path = url.getPath();
                String jarPath = path.substring(5, path.indexOf("!"));
                try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()))) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(dirname) && !dirname.equals(name)) {
                            URL resource = ResourceUtils.class.getResource(name);
                            filenames.add(resource.getFile());
                        }
                    }
                }
            }
        }
        return filenames;
    }

    public static File saveImageFile(Image image) throws IOException {

        File newFile = new File("imagery/Trinity-scan-" + UUID.randomUUID().toString() + ".png");
        BufferedImage buff = SwingFXUtils.fromFXImage(image, null);
        ImageIO.write(buff, "PNG", newFile);
        return newFile;
    }

    public static WritableImage loadImageFile(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        WritableImage wi = SwingFXUtils.toFXImage(image, null);
        return wi;
    }

    public static WritableImage loadImageFile(String filename) throws IOException {
        File imageFile = new File(filename);
        return loadImageFile(imageFile);
    }

    public static Image load3DTextureImage(String filename) throws IOException {
        try {
            return new Image(ImageResourceProvider.getResourceAsStream(filename + ".png"));
        } catch (NullPointerException e) {
            throw new IOException("Failed to open " + filename + ".png");
        }
    }

    public static WritableImage loadImageFileSubset(String filename,
                                                    int x1, int y1, int x2, int y2) throws IOException {
        File imageFile = new File(filename);
        BufferedImage image = ImageIO.read(imageFile);
        return loadImageSubset(image, x1, y1, x2, y2);
    }

    public static WritableImage loadImageSubset(BufferedImage image, int x1, int y1, int x2, int y2) {
        BufferedImage subImage = image.getSubimage(x1, y1, x2 - x1, y2 - y1);
        WritableImage wi = SwingFXUtils.toFXImage(subImage, null);
        return wi;
    }

    public static WritableImage cropImage(Image image, double x1, double y1, double x2, double y2) {
        PixelReader r = image.getPixelReader();
        WritablePixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
        int x1Index = Double.valueOf(x1).intValue();
        int y1Index = Double.valueOf(y1).intValue();
        int x2Index = Double.valueOf(x2).intValue();
        int y2Index = Double.valueOf(y2).intValue();
        int width = x2Index - x1Index;
        int height = y2Index - y1Index;
        int[] pixels = new int[width * height];
        r.getPixels(x1Index, y1Index, width, height, pixelFormat, pixels, 0, width);
        WritableImage out = new WritableImage(width, height);
        PixelWriter w = out.getPixelWriter();
        w.setPixels(0, 0, width, height, pixelFormat, pixels, 0, width);
        return out;
    }

    public static WritableImage loadIconAsWritableImage(String iconName) throws IOException {
        InputStream is = IconResourceProvider.getResourceAsStream(iconName + ".png");
        BufferedImage image = ImageIO.read(is);
        WritableImage wi = SwingFXUtils.toFXImage(image, null);
        return wi;
    }

    public static Image loadIconFile(String iconName) {
        try {
            return new Image(IconResourceProvider.getResourceAsStream(iconName + ".png"));
        } catch (NullPointerException e) {
            return new Image(IconResourceProvider.getResourceAsStream("noimage.png"));
        }
    }

    public static ImageView loadIcon(String iconName, double FIT_WIDTH) {
        ImageView iv = new ImageView(loadIconFile(iconName));
        iv.setPreserveRatio(true);
        iv.setFitWidth(FIT_WIDTH);
        return iv;
    }

    /**
     * Checks whether the file is an extractable PDF type.
     *
     * @param file The File object to check.
     * @return boolean true if it is a pdf file, can be read and is a supported type
     */
    public static boolean isPDF(File file) {
        if (file.isFile() && file.canRead()) {
            try {
                String contentType = Files.probeContentType(file.toPath());
                if (null != contentType) {
                    //check for explicit mime types we want to guarantee work
                    switch (contentType) {
                        case "application/pdf" -> {
                            return true;
                        }
                    }
//                    //now look explicitly for type categories we like
//                    if (contentType.startsWith("text") || contentType.endsWith("json"))
//                        return true;
                }
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
//            //now fuck it just check for certain byte types. (good but not guaranteed)
//            boolean ascii = asciiBytesCheck(file);
//            return ascii;
        }
        return false;

    }

    /**
     * Checks whether the file can read as ASCII text.
     *
     * @param file The File object to check.
     * @return boolean true if it is a file, can be read and is a supported text type
     */
    public static boolean isTextFile(File file) {
        if (file.isFile() && file.canRead()) {
            try {
                String contentType = Files.probeContentType(file.toPath());
                if (null != contentType) {
                    //check for explicit mime types we want to guarantee work
                    switch (contentType) {
                        case "text/plain", "application/json" -> {
                            return true;
                        }
                    }
                    //now look explicitly for type categories we like
                    if (contentType.startsWith("text") || contentType.endsWith("json"))
                        return true;
                }
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
            //now fuck it just check for certain byte types. (good but not guaranteed)
            boolean ascii = asciiBytesCheck(file);
            return ascii;
        }
        return false;
    }

    public static boolean asciiBytesCheck(File f) {
        if (!f.exists())
            return false;
        byte[] data;
        try (FileInputStream in = new FileInputStream(f)) {
            int size = in.available();
            if (size > 1000)
                size = 1000;
            data = new byte[size];
            in.read(data);
            String s = new String(data, "ISO-8859-1");
            String s2 = s.replaceAll(
                "[a-zA-Z0-9ßöäü\\.\\*!\"§\\$\\%&/()=\\?@~'#:,;\\" +
                    "+><\\|\\[\\]\\{\\}\\^°²³\\\\ \\n\\r\\t_\\-`´âêîô" +
                    "ÂÊÔÎáéíóàèìòÁÉÍÓÀÈÌÒ©‰¢£¥€±¿»«¼½¾™ª]", "");
            // will delete all text signs
            double d = (double) (s.length() - s2.length()) / (double) (s.length());
            // percentage of text signs in the text
            return d > 0.95;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Checks whether the file can be used as an image.
     *
     * @param file The File object to check.
     * @return boolean true if it is an Image that JavaFX supports
     */
    public static boolean isImageFile(File file) {
        if (file.isFile() && file.canRead()) {
            try {
                String contentType = Files.probeContentType(file.toPath());
                switch (contentType) {
                    case "image/png":
                    case "image/bmp":
                    case "image/gif":
                    case "image/tiff":
                    case "image/jpg":
                    case "image/jpeg":
                        return true;
                }
                //System.out.println(contentType);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        }
        return false;
    }

    /**
     * Checks whether the file can be used as audio.
     *
     * @param file The File object to check.
     * @return boolean true if it is a file, can be read and is a supported audio type
     */
    public static boolean isAudioFile(File file) {
        if (file.isFile() && file.canRead()) {
            try {
                String contentType = Files.probeContentType(file.toPath());
                switch (contentType) {
                    case "audio/x-flac":
                    case "audio/flac":
                    case "audio/wav":
                    case "audio/mp3":
                        return true;
                }
                //System.out.println(contentType);
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        }
        return false;
    }

    public static AudioClip loadAudioClipWav(String filename) {
        return new AudioClip(AudioResourceProvider.getResource(filename + ".wav").toExternalForm());
    }

    public static Media loadMediaWav(String filename) throws IOException {
        return new Media(AudioResourceProvider.getResource(filename + ".wav").toExternalForm());
    }

    public static Media loadMediaMp4(String filename) throws MalformedURLException {
        File folder = new File("video/");
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            return null;
        }
        for (File file : folder.listFiles()) {
            if (file.getName().contentEquals(filename)) {
                Media media = new Media(file.toURI().toURL().toString());
                return media;
            }
        }

        return null;
    }

    public static Media loadRandomMediaMp4() throws URISyntaxException, IOException {
        File folder = new File("video/");
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            return null;
        }
        File[] files = folder.listFiles();
        Random rando = new Random();
        File file = files[rando.nextInt(files.length)];
        Media media = new Media(file.toURI().toURL().toString());
        return media;
    }

    public static boolean canDragOverDirectory(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            File file = db.getFiles().get(0); //only support the first
            if (file.isDirectory()) {
                event.acceptTransferModes(TransferMode.COPY);
                return true;
            }
        } else {
            event.consume();
        }
        return false;
    }

    public static boolean canDragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        DataFormat dataFormat = DataFormat.lookupMimeType("application/x-java-file-list");
        try {
            if (db.hasFiles() || db.hasContent(dataFormat)) {
                List<File> files = db.getFiles();
                //workaround for Swing JFXPanel
                if (db.hasContent(dataFormat)) {
                    //Swing containers require a registered mime type
                    //since we don't have that, we need to accept the drag
                    event.acceptTransferModes(TransferMode.COPY);
                    return true;
                }
            } else {
                event.consume();
            }
        } catch (Exception ex) {
            LOG.error(null, ex);
            event.consume();
        }
        return false;
    }

    /**
     * Any time a drop event occurs this attempts to process the object.
     *
     * @param event DragEvent.DragDropped
     * @param scene
     */
    public static void onDragDropped(DragEvent event, Scene scene) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            final List<File> files = db.getFiles();
            Task task = new Task() {
                @Override
                protected Void call() throws Exception {
                    Platform.runLater(() -> {
                        ProgressStatus ps1 = new ProgressStatus("Receiving Data Drop...", -1);
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps1));
                    });
                    //do we have multiple files? if so then don't offer to clear any queues
                    boolean offerToClear = files.size() <= 1; //if more than one file is detected... don't bother to ask to clear. (assume group load)
                    for (File file : files) {
                        try {
                            if (JavaFX3DUtils.isTextureFile(file)) {
                                Image image = new Image(file.toURI().toURL().toExternalForm());
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, image)));
                            } else if (isAudioFile(file)) {
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new AudioEvent(AudioEvent.NEW_AUDIO_FILE, file)));
                            } else if (SaturnFile.isSaturnFile(file)) {
                                SaturnLoader task = new SaturnLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (CdcCsvFile.isCdcCsvFile(file)) {
                                CdcCsvFile cdcCsvFile = new CdcCsvFile(file.getAbsolutePath(), true);
                                //convert to Feature Vector Collection for the lulz
                                FeatureCollection fc = DataUtils.convertCdcCsv(cdcCsvFile.cdcCsvList, true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc, file.getName())));
                                Trajectory trajectory = new Trajectory(file.getName());
                                trajectory.totalStates = fc.getFeatures().size();
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory, fc)));
                            } else if (CdcTissueGenesFile.isCdcTissueGenesFile(file)) {
                                CdcTissueGenesLoader task = new CdcTissueGenesLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (FeatureCollectionFile.isFeatureCollectionFile(file)) {
                                FeatureCollectionLoader task = new FeatureCollectionLoader(scene, file);
                                task.setClearQueue(offerToClear); //if there is more than one file just load all
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (ShapleyCollectionFile.isShapleyCollectionFile(file)) {
                                ShapleyCollectionLoader task = new ShapleyCollectionLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (GraphDirectedCollectionFile.isGraphDirectedCollectionFile(file)) {
                                GraphDirectedCollectionLoader task = new GraphDirectedCollectionLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (VectorMaskCollectionFile.isVectorMaskCollectionFile(file)) {
                                VectorMaskCollectionLoader task = new VectorMaskCollectionLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (LabelConfigFile.isLabelConfigFile(file)) {
                                LabelConfigFile labelConfigFile = new LabelConfigFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new FeatureVectorEvent(FeatureVectorEvent.NEW_LABEL_CONFIG, labelConfigFile.labelConfig)));
                            } else if (TerrainTextFile.isTerrainTextFile(file)) {
                                TerrainTextFile terrainTextFile = new TerrainTextFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new TerrainEvent(TerrainEvent.NEW_TERRAIN_TEXTFILE, terrainTextFile.dataGrid)));
                            } else if (FireAreaTextFile.isFireAreaTextFile(file)) {
                                FireAreaTextFile fireAreaTextFile = new FireAreaTextFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new TerrainEvent(TerrainEvent.NEW_FIREAREA_TEXTFILE, fireAreaTextFile.dataGrid)));
                            } else if (SemanticMapCollectionFile.isSemanticMapCollectionFile(file)) {
                                SemanticMapCollectionFile smcFile = new SemanticMapCollectionFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new SemanticMapEvent(SemanticMapEvent.NEW_SEMANTICMAP_COLLECTION, smcFile.semanticMapCollection)));
                                //Trajectory logic handled by SemanticMapEventHandler
                            } else if (ClusterCollectionFile.isClusterCollectionFile(file)) {
                                ClusterCollectionFile ccFile = new ClusterCollectionFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new ManifoldEvent(ManifoldEvent.NEW_CLUSTER_COLLECTION, ccFile.clusterCollection)));
                            } else if (GaussianMixtureCollectionFile.isGaussianMixtureCollectionFile(file)) {
                                GaussianMixtureCollectionFile gmcFile = new GaussianMixtureCollectionFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new GaussianMixtureEvent(GaussianMixtureEvent.NEW_GAUSSIAN_COLLECTION, gmcFile.gaussianMixtureCollection)));
                            } else if (TextEmbeddingCollectionFile.isTextEmbeddingCollection(file)) {
                                Platform.runLater(() -> {
                                    TextEmbeddingsLoader task = new TextEmbeddingsLoader(scene, file);
                                    Thread thread = new Thread(task);
                                    thread.setDaemon(true);
                                    thread.start();
                                });
                            } else if (McclodSplitDataTsvFile.isMcclodSplitDataTsvFile(file)) {
                                McclodSplitDataLoader task = new McclodSplitDataLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (ZeroPilotLatentsFile.isZeroPilotLatentsFile(file)) {
                                ZeroPilotLatentsLoader task = new ZeroPilotLatentsLoader(scene, file);
                                Thread thread = new Thread(task);
                                thread.setDaemon(true);
                                thread.start();
                            } else if (ManifoldDataFile.isManifoldDataFile(file)) {
                                ManifoldDataFile mdFile = new ManifoldDataFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new ManifoldEvent(ManifoldEvent.NEW_MANIFOLD_DATA, mdFile.manifoldData)));
                            } else if (CocoAnnotationFile.isCocoAnnotationFile(file)) {
                                CocoAnnotationFile cocoFile = new CocoAnnotationFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new ImageEvent(ImageEvent.NEW_COCO_ANNOTATION, cocoFile.cocoObject)));
                            } else if (CyberReporterFile.isFileType(file)) {
                                CyberReporterFile cyberReportFile = new CyberReporterFile(file.getAbsolutePath(), true);
                                Platform.runLater(() -> scene.getRoot().fireEvent(
                                    new FeatureVectorEvent(FeatureVectorEvent.NEW_CYBER_REPORT, cyberReportFile.cyberReports, file.getName())));
                            }
                        } catch (IOException ex) {
                            LOG.error(null, ex);
                        }
                    }
                    Platform.runLater(() -> {
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
                    });
                    return null;
                }
            };
            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
            event.setDropCompleted(true);
            event.consume();
        }
    }

    public static String detectDropType(DragEvent event) {
        Dragboard db = event.getDragboard();
        String type = "UNKNOWN";
        if (db.hasFiles()) {
            final File file = db.getFiles().get(0);
            try {
                if (JavaFX3DUtils.isTextureFile(file)) {
                    type = "Hypersurface";
                } else if (isAudioFile(file)) {
                    type = "Hypersurface";
                } else if (FeatureCollectionFile.isFeatureCollectionFile(file)) {
                    type = "Hyperspace";
                } else if (ShapleyCollectionFile.isShapleyCollectionFile(file)) {
                    type = "Hypersurface";
                } else if (GraphDirectedCollectionFile.isGraphDirectedCollectionFile(file)) {
                    type = "Hypersurface";
                } else if (VectorMaskCollectionFile.isVectorMaskCollectionFile(file)) {
                    type = "Hypersurface";
                } else if (LabelConfigFile.isLabelConfigFile(file)) {
                    type = "Hyperspace";
                } else if (TerrainTextFile.isTerrainTextFile(file)) {
                    type = "Hypersurface";
                } else if (FireAreaTextFile.isFireAreaTextFile(file)) {
                    type = "Hypersurface";
                } else if (SemanticMapCollectionFile.isSemanticMapCollectionFile(file)) {
                    type = "Hyperspace";
                } else if (ClusterCollectionFile.isClusterCollectionFile(file)) {
                    type = "Projections";
                } else if (GaussianMixtureCollectionFile.isGaussianMixtureCollectionFile(file)) {
                    type = "Hyperspace";
                } else if (TextEmbeddingCollectionFile.isTextEmbeddingCollection(file)) {
                    type = "Hyperspace";
                } else if (CdcCsvFile.isCdcCsvFile(file)) {
                    type = "Hyperspace";
                } else if (CdcTissueGenesFile.isCdcTissueGenesFile(file)) {
                    type = "Hyperspace";
                } else if (McclodSplitDataTsvFile.isMcclodSplitDataTsvFile(file)) {
                    type = "Hyperspace";
                } else if (ZeroPilotLatentsFile.isZeroPilotLatentsFile(file)) {
                    type = "Hyperspace";
                } else if (ManifoldDataFile.isManifoldDataFile(file)) {
                    type = "Projections";
                } else if (CocoAnnotationFile.isCocoAnnotationFile(file)) {
                    type = "Hypersurface";
                }
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        }
        return type;
    }

    public static boolean promptUserOnCommand(String commandType) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Switch Views Now?",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Your primary file type suggests using the " + commandType + " view.");
        alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
        alert.initStyle(StageStyle.TRANSPARENT);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setBackground(Background.EMPTY);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
        dialogPane.getStylesheets().add(DIALOGCSS);
        Optional<ButtonType> optBT = alert.showAndWait();
        return optBT.get().equals(ButtonType.YES);
    }

    public static String removeExtension(String filename) {
        return filename.substring(0, filename.lastIndexOf("."));
    }

    public static String getNameFromURI(String uriString) {
        try {
            return Paths.get(new URI(uriString)).getFileName().toString();
        } catch (URISyntaxException ex) {
            LOG.error("Could not load URI from: " + uriString);
        }
        return "";
    }

    public static Image bytesToImage(byte[] image) {
        byte[] rayray = new byte[image.length];
        System.arraycopy(image, 0,
            rayray, 0, rayray.length);
        Image imageObject = new Image(new ByteArrayInputStream(rayray));
        return imageObject;
    }

    /**
     * https://stackoverflow.com/questions/24038524/how-to-get-byte-from-javafx-imageview
     *
     * @param image
     * @return byte [] da bytes
     */
    public static byte[] imageToBytes(Image image) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", byteOutput);
        return byteOutput.toByteArray();
    }

    public static byte[] byteMe(Image me) throws IOException {
        int w = (int) me.getWidth();
        int h = (int) me.getHeight();
        int[] intBuf = new int[w * h];
        me.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), intBuf, 0, w);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int i = 0; i < intBuf.length; ++i) {
            dos.writeInt(intBuf[i]);
        }
        return baos.toByteArray();
    }

    public static String imageToBase64(Image image) throws IOException {
        return Base64.getEncoder().encodeToString(imageToBytes(image));
    }
}