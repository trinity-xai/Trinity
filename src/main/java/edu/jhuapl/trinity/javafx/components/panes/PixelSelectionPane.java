package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;

/**
 * @author Sean Phillips
 */
public class PixelSelectionPane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(PixelSelectionPane.class);
    public static int PANE_WIDTH = 550;
    public static int PANE_HEIGHT = 550;
    public BorderPane borderPane;
    public StackPane centerStack;
    public ImageView imageView;
    public Image image;
    ScrollPane scrollPane;
    public Rectangle selectionRectangle;
    private double mousePosX;
    private double mousePosY;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public PixelSelectionPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Pixel Selection", "", 300.0, 400.0);
        setBackground(Background.EMPTY);
        this.scene = scene;
        image = ResourceUtils.loadIconFile("waitingforimage");
        borderPane = (BorderPane) this.contentPane;
        centerStack = new StackPane();
        centerStack.setAlignment(Pos.CENTER);
        borderPane.setCenter(centerStack);
        imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        scrollPane = new ScrollPane(imageView);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(512, 512);
        centerStack.getChildren().add(scrollPane);

        Button tessellateSelectionButton = new Button("Tessellate");
        tessellateSelectionButton.setOnAction(e -> {
            if (selectionRectangle.getWidth() > 1 && selectionRectangle.getHeight() > 1) {
                Point2D sceneP2D = selectionRectangle.localToScene(
                    selectionRectangle.getX(), selectionRectangle.getY());
                Point2D localP2D = imageView.sceneToLocal(sceneP2D);
                WritableImage wi = ResourceUtils.cropImage(image,
                    localP2D.getX(), localP2D.getY(),
                    localP2D.getX() + selectionRectangle.getWidth(),
                    localP2D.getY() + selectionRectangle.getHeight());
                tessellateSelectionButton.getScene().getRoot().fireEvent(
                    new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, wi));
            }
        });
        HBox powerBottomHBox = new HBox(10, tessellateSelectionButton);
        powerBottomHBox.setPadding(new Insets(10));
        powerBottomHBox.setAlignment(Pos.CENTER);
        borderPane.setBottom(powerBottomHBox);

        //Setup selection rectangle and event handling
        selectionRectangle = new Rectangle(1, 1, Color.CYAN.deriveColor(1, 1, 1, 0.5));
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);
        selectionRectangle.setVisible(false);
        borderPane.getChildren().add(selectionRectangle); // a little hacky but...

        imageView.setOnMouseEntered(e -> imageView.setCursor(Cursor.CROSSHAIR));
        imageView.setOnMouseExited(e -> imageView.setCursor(Cursor.DEFAULT));

        imageView.setOnMousePressed((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                selectionRectangle.setWidth(1);
                selectionRectangle.setHeight(1);
                Point2D localP2D = borderPane.sceneToLocal(mousePosX, mousePosY);
                selectionRectangle.setX(localP2D.getX());
                selectionRectangle.setY(localP2D.getY());
                selectionRectangle.setVisible(true);
            }
        });
        //Start Tracking mouse movements only when a button is pressed
        imageView.setOnMouseDragged((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                Point2D localP2D = borderPane.sceneToLocal(mousePosX, mousePosY);
                selectionRectangle.setWidth(
                    localP2D.getX() - selectionRectangle.getX());
                selectionRectangle.setHeight(
                    localP2D.getY() - selectionRectangle.getY());
            }
        });
        imageView.setOnMouseReleased((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
            }
        });
        borderPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        borderPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                final File file = db.getFiles().get(0);
                if (JavaFX3DUtils.isTextureFile(file)) {
                    try {
                        setImage(new Image(file.toURI().toURL().toExternalForm()));
                    } catch (MalformedURLException ex) {
                        LOG.error(ex.getMessage());
                    }
                }
            }
        });
    }

    public void setImage(Image image) {
        this.image = image;
        imageView.setImage(this.image);
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }
}
