package edu.jhuapl.trinity.javafx.components;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Random;
import java.util.function.BiFunction;

/**
 * Matrix effect drawn on a canvas.
 * TODO parameterize fonts etc.
 * Ported from https://dev.to/gnsp/making-the-matrix-effect-in-javascript-din
 *
 * @author Carl Dea, Sean Phillips
 * taken from LitFX
 */
public class MatrixEffect {
    private int fontSize = 20; // width pixels
    private Canvas canvas;
    public Color backgroundFill = Color.rgb(0, 0, 0, 0.1);  //Color.web("#0001");
    public Color textFill = Color.GREEN; //Color.web("#0f0");
    public Color textHighlightFill = Color.CYAN;
    private AnimationTimer animationTimer;

    public MatrixEffect(Canvas canvas) {
        this.canvas = canvas;
        init();
    }

    private int[] resize(GraphicsContext gc) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.save();
        gc.setFill(Color.web("#000"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        int cols = (int) Math.floor(canvas.getWidth() / fontSize) + 1;
        int[] ypos = new int[cols];
        return ypos;
    }

    private void init() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        int[] initYpos = resize(gc);
        Random rando = new Random();
        // generate
        this.animationTimer = new AnimationTimer() {
            long lastTimerCall = 0;
            final long NANOS_PER_MILLI = 1000000; //nanoseconds in a millisecond
            final long ANIMATION_DELAY = 30 * NANOS_PER_MILLI; //>>>>>>parameterize
            int prevWidth = (int) canvas.getWidth();
            int prevHeight = (int) canvas.getHeight();
            int[] ypos = initYpos;

            @Override
            public void handle(long now) {
                if (now > lastTimerCall + ANIMATION_DELAY) {
                    lastTimerCall = now;    //update for the next animation

                    int w, h;
                    w = (int) canvas.getWidth();
                    h = (int) canvas.getHeight();
                    if (w != prevWidth || h != prevHeight) {
                        System.out.println("resizing " + w + " prevw " + prevWidth);
                        ypos = resize(gc);
                        prevWidth = w;
                        prevHeight = h;
                    }
                    // Draw a semitransparent black rectangle on top of previous drawing
                    gc.setFill(backgroundFill);
                    gc.fillRect(0, 0, w, h);

                    // Set color to green and font to monospace in the drawing context
                    gc.setFill(textFill);
                    gc.setFont(new Font("monospace", fontSize));

                    // for each column put a random character at the end
                    for (int i = 0; i < ypos.length; i++) {
                        // generate a random character
                        String text = Character.toString((int) (Math.random() * 128));

                        // x coordinate of the column, y coordinate is already given
                        double x = i * fontSize;
                        int y = ypos[i];

                        //semi randomly set color to a brighter color
                        if (rando.nextDouble() > 0.99)
                            gc.setFill(textHighlightFill);
                        // render the character at (x, y)
                        gc.fillText(text, x, ypos[i]);

                        // randomly reset the end of the column if it's at least 100px high
                        if (y > 500 + Math.random() * 5000) {
                            ypos[i] = 0;
                        } else {
                            // otherwise just move the y coordinate for the column 20px down,
                            ypos[i] = y + fontSize;
                        }

                    }
                }
            }
        };

    }

    public void start() {
        this.animationTimer.start();
    }

    public void stop() {
        this.animationTimer.stop();
    }

    private static BiFunction<String, Object, String> row = (label, value) ->
        String.format(" %s: %s\n", label, value);

    private static String out(String name, Object value) {
        if (value instanceof Object[]) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (Object obj : (Object[]) value) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(obj);
                i++;
            }
            return row.apply(name, sb.toString());
        }
        return row.apply(name, value);
    }

    private static String sb(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String pair : args) {
            sb.append(pair);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return sb(
//                out("bandColorProperty", bandColor.get())
        );
    }
}
