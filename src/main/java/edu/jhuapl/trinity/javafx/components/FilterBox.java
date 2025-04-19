package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.FilterSet;
import edu.jhuapl.trinity.javafx.events.SearchEvent;
import edu.jhuapl.trinity.utils.PrecisionConverter;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.function.UnaryOperator;

/**
 * @author Sean Phillips
 */
public class FilterBox extends VBox {

    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;

    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    Background transFillBack = new Background(new BackgroundFill(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.222), CornerRadii.EMPTY, Insets.EMPTY));

    NumberFormat doubleFormat = new DecimalFormat("0.00000");

    public FilterBox() {
        Glow glow = new Glow(0.666);

        ToggleGroup tg = new ToggleGroup();
        RadioButton scoreButton = new RadioButton("Filter by Score");
        scoreButton.setToggleGroup(tg);
        scoreButton.setSelected(true);
        RadioButton probabilityButton = new RadioButton("Filter by Probablity");
        probabilityButton.setToggleGroup(tg);
        HBox toggleHBox = new HBox(10, scoreButton, probabilityButton);

        UnaryOperator<TextFormatter.Change> doubleFilter = c -> {
            if (c.isContentChange()) {
                ParsePosition parsePosition = new ParsePosition(0);
                // NumberFormat evaluates the beginning of the text
                doubleFormat.parse(c.getControlNewText(), parsePosition);
                if (parsePosition.getIndex() == 0 ||
                    parsePosition.getIndex() < c.getControlNewText().length())
                    return null; // reject parsing the complete text failed
            }
            return c;
        };
        TextFormatter<Double> minFormatter = new TextFormatter<>(new PrecisionConverter(5), -1.0, doubleFilter);
        TextField minimumTextField = new TextField("-1.0");
        minimumTextField.setTextFormatter(minFormatter);
        minimumTextField.setPrefWidth(100);
        VBox minimumVBox = new VBox(5, new Label("Minimum"), minimumTextField);

        TextFormatter<Double> maxFormatter = new TextFormatter<>(new PrecisionConverter(5), 1.0, doubleFilter);
        TextField maximumTextField = new TextField("1.0");
        maximumTextField.setTextFormatter(maxFormatter);
        maximumTextField.setPrefWidth(100);
        VBox maximumVBox = new VBox(5, new Label("Maximum"), maximumTextField);

        ImageView filterIV = new ImageView(ResourceUtils.loadIconFile("filter"));
        filterIV.setSmooth(true);
        filterIV.setPreserveRatio(true);
        filterIV.setFitWidth(ICON_FIT_WIDTH);
        filterIV.setFitHeight(ICON_FIT_HEIGHT);
        filterIV.setOnMouseEntered(e -> filterIV.setCursor(Cursor.HAND));
        filterIV.setOnMouseExited(e -> filterIV.setCursor(Cursor.DEFAULT));
//        searchIV.disableProperty().bind(searchTextField.textProperty().isEmpty());
        filterIV.setEffect(glow);
        filterIV.setOnMouseClicked(e -> {
            FilterSet fs = new FilterSet(Double.valueOf(minimumTextField.getText()),
                Double.valueOf(maximumTextField.getText()), FilterSet.Inclusion.INNER);
            if (scoreButton.isSelected())
                filterIV.getScene().getRoot().fireEvent(
                    new SearchEvent(SearchEvent.FILTER_BY_SCORE, fs));
            else
                filterIV.getScene().getRoot().fireEvent(
                    new SearchEvent(SearchEvent.FILTER_BY_PROBABILITY, fs));
        });
        ImageView resetIV = new ImageView(ResourceUtils.loadIconFile("reset"));
        resetIV.setSmooth(true);
        resetIV.setPreserveRatio(true);
        resetIV.setFitWidth(ICON_FIT_WIDTH);
        resetIV.setFitHeight(ICON_FIT_HEIGHT);
        resetIV.setOnMouseEntered(e -> resetIV.setCursor(Cursor.HAND));
        resetIV.setOnMouseExited(e -> resetIV.setCursor(Cursor.DEFAULT));
        resetIV.setEffect(glow);
        resetIV.setOnMouseClicked(e -> {
            resetIV.getScene().getRoot().fireEvent(
                new SearchEvent(SearchEvent.CLEAR_ALL_FILTERS));
        });

        HBox searchHBox = new HBox(filterIV);
        searchHBox.setPrefWidth(ICON_FIT_WIDTH);
        searchHBox.setOnMouseEntered(e -> searchHBox.setBackground(transFillBack));
        searchHBox.setOnMouseExited(e -> searchHBox.setBackground(transBack));
        HBox resetHBox = new HBox(resetIV);
        resetHBox.setPrefWidth(ICON_FIT_WIDTH);
        resetHBox.setOnMouseEntered(e -> resetHBox.setBackground(transFillBack));
        resetHBox.setOnMouseExited(e -> resetHBox.setBackground(transBack));

        HBox controlHBox = new HBox(10, minimumVBox, maximumVBox, searchHBox, resetHBox);
        controlHBox.setAlignment(Pos.CENTER);

        Label textFieldLabel = new Label("Range");
        textFieldLabel.setPrefWidth(200);
        Label searchLabel = new Label("Apply");
        searchLabel.setPrefWidth(ICON_FIT_WIDTH);
        Label resetLabel = new Label("Reset");
        resetLabel.setPrefWidth(ICON_FIT_WIDTH);
        HBox labelHBox = new HBox(10, textFieldLabel, searchLabel, resetLabel);

        setSpacing(10);
        setPadding(new Insets(5, 0, 0, 0));
        getChildren().addAll(labelHBox, controlHBox, toggleHBox);
    }
}
