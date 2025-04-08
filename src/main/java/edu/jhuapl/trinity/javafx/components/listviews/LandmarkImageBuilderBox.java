package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class LandmarkImageBuilderBox extends VBox {

    public static double LISTVIEW_PREF_WIDTH = 400;
    public static double LISTVIEW_PREF_HEIGHT = 210;
    public static double BUTTON_PREF_WIDTH = 150;

    Background transFillBack = new Background(new BackgroundFill(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.1), CornerRadii.EMPTY, Insets.EMPTY));

    protected ListView<LandmarkImageListItem> landmarksListView;

    File currentDirectory = new File(".");

    public LandmarkImageBuilderBox() {

        Button addButton = new Button("Add Images");
        addButton.setPrefWidth(BUTTON_PREF_WIDTH);
        addButton.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setInitialDirectory(currentDirectory);
            List<File> files = fc.showOpenMultipleDialog(null);
            if (null != files) {
            }
        });
        Button clearButton = new Button("Clear");
        clearButton.setPrefWidth(BUTTON_PREF_WIDTH);
        clearButton.setOnAction(e -> landmarksListView.getItems().clear());
        HBox buttonHBox = new HBox(10, addButton, clearButton);
        buttonHBox.setAlignment(Pos.CENTER);

        landmarksListView = new ListView<>();
        landmarksListView.setMinWidth(LISTVIEW_PREF_WIDTH);
        landmarksListView.setPrefHeight(LISTVIEW_PREF_HEIGHT);
        landmarksListView.setEditable(true);
        ImageView iv = ResourceUtils.loadIcon("waitingforimage", 128);
        VBox placeholder = new VBox(10, iv, new Label("No Image Landmarks Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        landmarksListView.setPlaceholder(placeholder);

        setSpacing(10);
        setPadding(new Insets(5, 0, 0, 0));
        getChildren().addAll(
            buttonHBox,
            landmarksListView
        );
        setBackground(transFillBack);
        addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            event.consume();
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                addFiles(db.getFiles());
            }
        });
    }

    public void addFiles(List<File> files) {
        files.stream().filter(f -> ResourceUtils.isImageFile(f))
            .forEach(imageFile -> {
                landmarksListView.getItems().add(
                    new LandmarkImageListItem(imageFile));
                currentDirectory = imageFile; //a little hacky but it works
            });
    }

    public List<LandmarkImageListItem> getItems() {
        return landmarksListView.getItems();
    }
}
