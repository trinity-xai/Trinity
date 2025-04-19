package edu.jhuapl.trinity.javafx.gleam;

import edu.jhuapl.trinity.javafx.starfield.Star;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Gleam extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(Gleam.class);

    public static ObservableList<Node> child;
    //
    private static final String title = "JellyBeanci";
    public static final int width = 800;
    public static final int height = 800;
    public static double speed = 0;
    private static Color backcolor = Color.rgb(13, 13, 13);

    private static Timeline update;

    @Override
    public void start(Stage stage) throws Exception {
        Pane root = new Pane();
        child = root.getChildren();
        //
        for (int i = 0; i < 500; i++) {
            new Star();
        }
        for (Star star : Star.stars) {
            child.add(star.getBody());
        }
        //

        root.setOnScroll(e -> {
            speed += e.getDeltaY() * 0.1;
        });

        root.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case F1: {
                    //PLAY
                    update.play();
                    break;
                }
                case F2: {
                    //PAUSE
                    update.pause();
                    break;
                }
                case F3: {
                    //Show Child Count
                    LOG.info("Child Count: {}", child.size());
                    break;
                }
            }
        });
        update = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            //60 fps
            for (Star star : Star.stars) {
                star.update();
            }

        }));
        update.setCycleCount(Timeline.INDEFINITE);
        update.setRate(1);
        update.setAutoReverse(false);
        //update.play(); //uncomment for play when start
        //
        stage.setTitle(title);
        stage.setResizable(false);
        stage.setScene(new Scene(root, width - 10, height - 10, backcolor));
        stage.show();
        root.requestFocus();

//window.onload = function() {
//  var canvas = document.getElementById("canvas"),
//    context = canvas.getContext("2d"),
//    w = canvas.width = window.innerWidth,
//    h = canvas.height = window.innerHeight,
//    centerX = w / 2,
//    centerY = h / 2;
//
//  var lines = {},
//    lineIndex = 0;
//
//  function Line() {
//    this.start = { x: centerX, y: centerY };
//    this.end = { x: centerX, y: centerY };
//    this.vx = Math.random() * 16 - 8;
//    this.vy = Math.random() * 16 - 8;
//    this.life = 0;
//    this.maxLife = Math.random() * 10 + 20;
//    lineIndex++;
//    lines[lineIndex] = this;
//    this.id = lineIndex;
//  }
//
//  Line.prototype.draw = function() {
//    this.end.x += this.vx;
//    this.end.y += this.vy;
//    this.life++;
//    if (this.life >= this.maxLife) {
//      delete lines[this.id];
//    }
//    //context.fillStyle = "#000";
//    //context.fillRect(this.x, this.y, 1, 1)
//    context.beginPath();
//    context.moveTo(this.start.x, this.start.y);
//    context.lineTo(this.end.x, this.end.y);
//    context.lineWidth = 1;
//    context.stroke();
//    context.strokeStyle = "#000";
//  }
//
//  setInterval(function() {
//    context.fillStyle = 'rgba(255,255,255,.05)';
//    context.fillRect(0, 0, w, h);
//    new Line();
//    for (var i in lines) {
//      lines[i].draw();
//    }
//  }, 30)
//};
//
    }

    public static void main(String[] args) {
        launch(args);
    }
}
