package edu.jhuapl.trinity.javafx.components.listviews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.utils.MessageUtils;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
            String fileString = Files.readString(file.toPath());
            //if JSON attempt intelligent object wise chunking
            if (MessageUtils.probablyJSON(fileString)) {
                final ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(fileString);
                if (jsonNode.getNodeType() == JsonNodeType.ARRAY) {
                    for (final JsonNode objNode : jsonNode) {
                        //System.out.println(objNode);
                        EmbeddingsTextListItem item = new EmbeddingsTextListItem(file, false);
                        item.contents = objNode.toPrettyString();
                        item.getFeatureVector().setText(item.contents);
                        item.setFeatureVectorLabel(file.getName());
                        objNode.fields().forEachRemaining(obj -> {
                            String lower = obj.getKey().toLowerCase();
                            //last found matching field will be used
                            if (lower.contains("name") || lower.contains("label")) {
                                item.setFeatureVectorLabel(obj.getValue().toString());
                            } else if(lower.equalsIgnoreCase("text")) {
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
                //use naive bruteforce chunking
                long total = file.length();

                if (total <= LARGEFILE_SPLIT_SIZE) {
                    list.add(new EmbeddingsTextListItem(file, true));
                    return list;
                }
                //String fileString = Files.readString(file.toPath());
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
            }
        } catch (Exception ex) {
        }
        return list;
    };
}
