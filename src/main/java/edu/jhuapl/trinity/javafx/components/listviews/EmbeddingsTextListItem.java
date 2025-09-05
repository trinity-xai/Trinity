package edu.jhuapl.trinity.javafx.components.listviews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.utils.MessageUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

//        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
//            if (e.getClickCount() > 1) {
//                getScene().getRoot().fireEvent(new FeatureVectorEvent(
//                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, featureVector));
//                getScene().getRoot().fireEvent(
//                    new FeatureVectorEvent(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, featureVector));
//            }
//        });
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
        if (null != file && file.isFile() && file.canRead()) {
            try {
                contents = Files.readString(file.toPath());
                featureVector.setText(contents);
            } catch (IOException ex) {
                contents = "";
            }
        } else
            contents = "";
    }

    public void setEmbeddings(List<Double> data) {
        featureVector.getData().clear();
        featureVector.getData().addAll(data);
        dimensionsLabel.setText(format.format(data.size()));
        embeddingsReceived = true;
    }

    public void setFeatureVectorLabel(String text) {
        Platform.runLater(() -> {
            labelTextField.setText(text);
        });
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
    public static Function<File, List<EmbeddingsTextListItem>> itemsSplitFromFile = file -> {
        List<EmbeddingsTextListItem> list = new ArrayList<>();
        try {
            //First extract usable text
            String fileString = null;
            if (ResourceUtils.isPDF(file)) {
                try (PDDocument document = Loader.loadPDF(file)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    fileString = stripper.getText(document);
                    //System.out.println(text);
                }
            } else {
                fileString = Files.readString(file.toPath());
            }
            //if JSON attempt intelligent object wise chunking
            if (ENABLE_CSV_EXPANSION && MessageUtils.probablyCSV(file.getName())) {
                //assumes first row is the column labels
                return expandCSVAndChunk(file, fileString);
            } else if (ENABLE_JSON_PROCESSING && MessageUtils.probablyJSON(fileString)) {
                //if JSON attempt intelligent object wise chunking
                final ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(fileString);
                if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
                    for (final JsonNode objNode : jsonNode) {
                        //System.out.println(objNode);
                        EmbeddingsTextListItem item = new EmbeddingsTextListItem(file, false);
                        item.contents = objNode.toPrettyString();
                        item.getFeatureVector().setText(item.contents);
                        item.setFeatureVectorLabel(file.getName());
                        objNode.properties().forEach(obj -> {
                            String lower = obj.getKey().toLowerCase();
                            //last found matching field will be used
                            if (lower.contains("name") || lower.contains("label")) {
                                item.setFeatureVectorLabel(obj.getValue().toString());
                            } else if (lower.equalsIgnoreCase("text")) {
                                //overrides the above full contents
                                item.getFeatureVector().setText(obj.getValue().toPrettyString());
                            } else {
                                item.addMetaData(obj.getKey(), obj.getValue().toString());
                            }
                        });
                        list.add(item);
                    }
                }
            } else {
                //not smart but smarter than dumb newline delimited chunks
                if(BREAK_ON_NEWLINES){
                    return chunkByDelimiter(file, fileString, System.lineSeparator());
                }
                //use naive bruteforce chunking
                long total = file.length();
                if (total <= LARGEFILE_SPLIT_SIZE) {
                    list.add(new EmbeddingsTextListItem(file, true));
                    return list;
                }
                return chunkString(file, fileString);
            }
        } catch (Exception ex) {
            LOG.error("Wierdness trying to read in " + file.getName());
        }
        return list;
    };
    public static List<EmbeddingsTextListItem> expandCSVAndChunk(File file, String fileString) throws IOException {
        String [] csvRows = fileString.split(System.lineSeparator());
        if(csvRows.length <= 1) return Collections.EMPTY_LIST;
        //get labels
        String [] labels = csvRows[0].split(",");
        List<EmbeddingsTextListItem> list = Arrays.asList(csvRows).stream()
            .skip(1) //skip the first row (label row)
            .map(s -> {
                EmbeddingsTextListItem item = new EmbeddingsTextListItem(file, false);
                String expandedContent = expandCSV(labels, s);
                item.contents = expandedContent;
                item.getFeatureVector().setText(expandedContent);
                String [] csvTokens = s.split(",");
                if(AUTOLABEL_FROM_CSVCOLUMN && csvTokens.length > CSV_DEFAULTLABEL_COLUMN)
                    item.setFeatureVectorLabel(csvTokens[CSV_DEFAULTLABEL_COLUMN]);
                else
                    item.setFeatureVectorLabel(file.getName());
                return item;
            }).toList();
        return list;
    }
    public static String expandCSV(String [] labels, String csvRow) {
        StringBuilder sb = new StringBuilder();
        String [] tokens = csvRow.split(",");
        for(int i=0;i<labels.length;i++){
            if(i>=tokens.length) break; //in case a row is incomplete
            sb.append(labels[i].trim()).append(" = ").append(tokens[i].trim()).append(System.lineSeparator());
        }
        return sb.toString();
    }
    public static List<EmbeddingsTextListItem> chunkByDelimiter(File file, String fileString, String delimiter) throws IOException {
        List<EmbeddingsTextListItem> list = Arrays.asList(fileString.split(delimiter)).stream()
            .map(s -> {
                EmbeddingsTextListItem item = new EmbeddingsTextListItem(file, false);
                item.contents = s;
                item.getFeatureVector().setText(s);
                item.setFeatureVectorLabel(file.getName());
                return item;
            }).toList();
        return list;
    }
    public static List<EmbeddingsTextListItem> chunkString(File file, String fileString) {
        List<EmbeddingsTextListItem> list = new ArrayList<>();
        int len = fileString.length();
        int currentStart = 0;
        int currentEnd = LARGEFILE_SPLIT_SIZE;
        while (currentStart <= len) {
            if (currentEnd > len)
                currentEnd = len;
            String sub = fileString.substring(currentStart, currentEnd);
            EmbeddingsTextListItem item = new EmbeddingsTextListItem(file, false);
            item.contents = sub;
            item.getFeatureVector().setText(sub);
            item.setFeatureVectorLabel(file.getName());
            list.add(item);
            currentStart += LARGEFILE_SPLIT_SIZE;
            currentEnd += LARGEFILE_SPLIT_SIZE;
        }
        return list;
    }
}
