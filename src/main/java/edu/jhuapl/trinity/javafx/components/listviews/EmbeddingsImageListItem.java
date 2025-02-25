package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.FeatureVector;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class EmbeddingsImageListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 200;
//    private ImageView imageView;
    private Label label;
    private FeatureVector featureVector;

    public EmbeddingsImageListItem(FeatureVector featureVector) {
        this.featureVector = featureVector;
        StringBuilder sb = new StringBuilder("Index: " + featureVector.getMessageId());
        sb.append(" Embeddings: ").append(featureVector.getData().size());
        
        label = new Label(sb.toString());
        label.setPrefWidth(PREF_LABEL_WIDTH);

//        getChildren().addAll(label, colorPicker, visibleCheckBox);
        setSpacing(20);
//        colorPicker.valueProperty().addListener(cl -> {
//            if (null != colorPicker.getScene()) {
//                featureLayer.setColor(colorPicker.getValue());
//                FeatureLayer.updateFeatureLayer(featureLayer.getIndex(), featureLayer);
//                getScene().getRoot().fireEvent(new HyperspaceEvent(
//                    HyperspaceEvent.UPDATED_FEATURE_LAYER, featureLayer));
//            }
//        });
    }

}
