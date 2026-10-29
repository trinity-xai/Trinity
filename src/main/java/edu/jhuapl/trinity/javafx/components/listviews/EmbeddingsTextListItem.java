package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.components.hyperdrive.TextChunkParser;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class EmbeddingsTextListItem extends HBox {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingsTextListItem.class);
    public static boolean ENABLE_JSON_PROCESSING = false;
    public static boolean ENABLE_CSV_EXPANSION = false;
    public static boolean AUTOLABEL_FROM_CSVCOLUMN = false;
    public static boolean BREAK_ON_NEWLINES = false;
    public static int CSV_DEFAULTLABEL_COLUMN = 0;
    public static double PREF_DIMLABEL_WIDTH = 100;
    public static double PREF_FILELABEL_WIDTH = 250;
    public static int LARGEFILE_SPLIT_SIZE = 16384;
    public static AtomicInteger atomicID = new AtomicInteger();
    public static NumberFormat format = new DecimalFormat("0000");

    public boolean embeddingsReceived = false;
    public boolean parseFile = false;
    public int textID;
    public String contents = null;
    private Label fileLabel;
    private File file;
    private Label dimensionsLabel;
    private TextField labelTextField;
    private FeatureVector featureVector = null;

    public EmbeddingsTextListItem(File file) {
        this(file, true);
    }

    public EmbeddingsTextListItem(File file, boolean parseFile) {
        textID = atomicID.getAndIncrement();
        this.file = file;
        this.parseFile = parseFile;
        fileLabel = new Label(file.getName());
        fileLabel.setPrefWidth(PREF_FILELABEL_WIDTH);
        featureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
        addMetaData("file", file.getAbsolutePath());
        if (parseFile)
            readText();

        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_FILELABEL_WIDTH);
        labelTextField.setOnAction(e -> featureVector.setLabel(labelTextField.getText()));
        labelTextField.textProperty().addListener(e -> featureVector.setLabel(labelTextField.getText()));
        dimensionsLabel = new Label(format.format(0));

        getChildren().addAll(fileLabel, dimensionsLabel, labelTextField);
        setSpacing(20);
        setPrefHeight(32);
        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));

        setFeatureVectorLabel(file.getName());
        setOnMouseClicked(e -> {
            if (e.getClickCount() > 1) {
                getScene().getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, featureVector));
                getScene().getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, featureVector));
            }
        });
    }

    public boolean embeddingsReceived() {
        return embeddingsReceived;
    }

    public void readText() {
        try {
            setEmbeddingContents(TextChunkParser.extractText(file));
        } catch (IOException ex) {
            LOG.error("Unable to read text from {}", file, ex);
            setEmbeddingContents("");
        }
    }

    /** Sets both the submitted embedding input and its FeatureVector provenance. */
    public void setEmbeddingContents(String text) {
        contents = text == null ? "" : text;
        featureVector.setText(contents);
    }

    public File getFile() {
        return file;
    }

    /** Create UI nodes only on the JavaFX Application Thread. */
    public static EmbeddingsTextListItem fromParsedChunk(File file, TextChunkParser.ParsedChunk chunk) {
        EmbeddingsTextListItem item = new EmbeddingsTextListItem(file, false);
        item.setEmbeddingContents(chunk.text());
        item.setFeatureVectorLabel(chunk.label());
        chunk.metadata().forEach(item::addMetaData);
        // JSON record metadata may itself contain a 'file' key. Always preserve
        // the actual imported source path under an unambiguous provenance key.
        item.addMetaData("source_file_path", file.getAbsolutePath());
        return item;
    }

    public static TextChunkParser.Options parsingOptions() {
        return new TextChunkParser.Options(ENABLE_JSON_PROCESSING, ENABLE_CSV_EXPANSION,
            AUTOLABEL_FROM_CSVCOLUMN, CSV_DEFAULTLABEL_COLUMN,
            BREAK_ON_NEWLINES, LARGEFILE_SPLIT_SIZE);
    }

    public void setEmbeddings(List<Double> data) {
        featureVector.getData().clear();
        featureVector.getData().addAll(data);
        dimensionsLabel.setText(format.format(data.size()));
        embeddingsReceived = true;
    }

    public void setFeatureVectorLabel(String text) {
        if (Platform.isFxApplicationThread()) {
            labelTextField.setText(text);
        } else {
            Platform.runLater(() -> labelTextField.setText(text));
        }
        featureVector.setLabel(text);
    }

    public String getFeatureVectorLabel() {
        return labelTextField.getText();
    }

    public void setLabelWidth(double width) {
        fileLabel.setPrefWidth(width);
    }

    public void addMetaData(String key, String value) {
        featureVector.getMetaData().put(key, value);
    }

    public void addExplanation(String explanation) {
        addMetaData("explanation", explanation);
    }

    public void addDescription(String description) {
        featureVector.setText(description);
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    public String getFeatureVectorEntityID() {
        return featureVector.getEntityId();
    }

    public void setFeatureVectorEntityID(String entityID) {
        featureVector.setEntityId(entityID);
    }

    public static Function<File, EmbeddingsTextListItem> itemFromFile = file -> {
        return new EmbeddingsTextListItem(file);
    };
    public static Function<File, EmbeddingsTextListItem> itemNoParseFromFile = file -> {
        return new EmbeddingsTextListItem(file, false);
    };
    /** Compatibility adapter; callers should invoke it from the FX thread. */
    public static Function<File, List<EmbeddingsTextListItem>> itemsSplitFromFile = file -> {
        try {
            return TextChunkParser.parse(file, parsingOptions()).stream()
                .map(chunk -> fromParsedChunk(file, chunk)).toList();
        } catch (IOException ex) {
            LOG.error("Could not parse text file {}", file, ex);
            return List.of();
        }
    };

    public static List<EmbeddingsTextListItem> expandCSVAndChunk(File file, String fileString) throws IOException {
        TextChunkParser.Options options = new TextChunkParser.Options(false, true,
            AUTOLABEL_FROM_CSVCOLUMN, CSV_DEFAULTLABEL_COLUMN, false, LARGEFILE_SPLIT_SIZE);
        return TextChunkParser.parseText(file, fileString, options).stream()
            .map(chunk -> fromParsedChunk(file, chunk)).toList();
    }

    public static String expandCSV(String[] labels, String csvRow) {
        try {
            List<List<String>> parsed = TextChunkParser.parseCsvRecords(csvRow);
            if (parsed.isEmpty()) return "";
            List<String> row = parsed.get(0);
            StringBuilder expanded = new StringBuilder();
            for (int i = 0; i < Math.min(labels.length, row.size()); i++) {
                expanded.append(labels[i].trim()).append(" = ")
                    .append(row.get(i).trim()).append(System.lineSeparator());
            }
            return expanded.toString();
        } catch (IOException ex) {
            LOG.error("Invalid CSV row", ex);
            return "";
        }
    }

    public static List<EmbeddingsTextListItem> chunkByDelimiter(File file, String text, String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) return chunkString(file, text);
        List<EmbeddingsTextListItem> result = new ArrayList<>();
        for (String piece : text.split(java.util.regex.Pattern.quote(delimiter), -1)) {
            if (piece.isBlank()) continue;
            result.addAll(chunkString(file, piece));
        }
        return result;
    }

    public static List<EmbeddingsTextListItem> chunkString(File file, String text) {
        try {
            TextChunkParser.Options options = new TextChunkParser.Options(false, false,
                false, 0, false, LARGEFILE_SPLIT_SIZE);
            return TextChunkParser.parseText(file, text, options).stream()
                .map(chunk -> fromParsedChunk(file, chunk)).toList();
        } catch (IOException ex) {
            // Plain string processing does not perform file I/O or JSON parsing.
            throw new IllegalStateException("Unable to chunk string", ex);
        }
    }
}
