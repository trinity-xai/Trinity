package edu.jhuapl.trinity.javafx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ClipTestApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane pane = new StackPane();
        Canvas canvas = new Canvas(400, 400);
        pane.getChildren().add(canvas);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Make cyan background
        gc.setFill(Color.CYAN);
        gc.fillRect(0, 0, 400, 400);

        // Draw a green rectangle at the center with alpha 0.6
//        gc.save();
//        gc.setGlobalAlpha(0.6);
        gc.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.6));
        gc.fillRect(190, 190, 20, 20);
//        gc.restore();

        gc.beginPath();
        gc.rect(0, 0, 400, 400);
        gc.arc(200, 200, 50, 50, 0, 360);
        gc.closePath();
        gc.clip(); // To cut a 50x50 circle from the scene
        //Global Blend Mode
        gc.setGlobalBlendMode(BlendMode.SRC_ATOP);
        gc.setFill(Color.BLACK);
//        System.out.println("Alpha is " + gc.getGlobalAlpha()); // 1.0
        gc.fillRect(0, 0, 400, 400); // Drawn rectangle has alpha 0.6 instead of 1.0

        Scene scene = new Scene(pane, 400, 400);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
