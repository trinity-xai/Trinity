package edu.jhuapl.trinity.javafx.components.timeline;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

/* Derived from the following open source implementation.
 *
 * Copyright (c) 2021 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.List;


public class MissionTimerXBuilder<B extends MissionTimerXBuilder<B>> {
    private HashMap<String, Property> properties = new HashMap<>();


    // ******************** Constructors **************************************
    protected MissionTimerXBuilder() {
    }


    // ******************** Methods *******************************************
    public static final MissionTimerXBuilder create() {
        return new MissionTimerXBuilder();
    }

    public final B iconFitWidth(final double iconFitWidth) {
        properties.put("iconFitWidth", new SimpleDoubleProperty(iconFitWidth));
        return (B) this;
    }


    public final B title(final String title) {
        properties.put("title", new SimpleStringProperty(title));
        return (B) this;
    }

    public final B backgroundColor(final Color color) {
        properties.put("backgroundColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B ringBackgroundColor(final Color color) {
        properties.put("ringBackgroundColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B ringColor(final Color color) {
        properties.put("ringColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B clockColor(final Color color) {
        properties.put("clockColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B titleColor(final Color color) {
        properties.put("titleColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B itemColor(final Color color) {
        properties.put("itemColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B itemInnerColor(final Color color) {
        properties.put("itemInnerColor", new SimpleObjectProperty<>(color));
        return (B) this;
    }

    public final B startTime(final long startTime) {
        properties.put("startTime", new SimpleLongProperty(startTime));
        return (B) this;
    }

    public final B timeFrame(final long timeFrame) {
        properties.put("timeFrame", new SimpleLongProperty(timeFrame));
        return (B) this;
    }

    public final B items(final Item... items) {
        properties.put("itemArray", new SimpleObjectProperty<>(items));
        return (B) this;
    }

    public final B items(final List<Item> items) {
        properties.put("itemList", new SimpleObjectProperty<>(items));
        return (B) this;
    }

    public final B prefSize(final double width, final double height) {
        properties.put("prefSize", new SimpleObjectProperty<>(new Dimension2D(width, height)));
        return (B) this;
    }

    public final B minSize(final double width, final double height) {
        properties.put("minSize", new SimpleObjectProperty<>(new Dimension2D(width, height)));
        return (B) this;
    }

    public final B maxSize(final double width, final double height) {
        properties.put("maxSize", new SimpleObjectProperty<>(new Dimension2D(width, height)));
        return (B) this;
    }

    public final B prefWidth(final double prefWidth) {
        properties.put("prefWidth", new SimpleDoubleProperty(prefWidth));
        return (B) this;
    }

    public final B prefHeight(final double prefHeight) {
        properties.put("prefHeight", new SimpleDoubleProperty(prefHeight));
        return (B) this;
    }

    public final B minWidth(final double minWidth) {
        properties.put("minWidth", new SimpleDoubleProperty(minWidth));
        return (B) this;
    }

    public final B minHeight(final double minHeight) {
        properties.put("minHeight", new SimpleDoubleProperty(minHeight));
        return (B) this;
    }

    public final B maxWidth(final double maxWidth) {
        properties.put("maxWidth", new SimpleDoubleProperty(maxWidth));
        return (B) this;
    }

    public final B maxHeight(final double maxHeight) {
        properties.put("maxHeight", new SimpleDoubleProperty(maxHeight));
        return (B) this;
    }

    public final B scaleX(final double scaleX) {
        properties.put("scaleX", new SimpleDoubleProperty(scaleX));
        return (B) this;
    }

    public final B scaleY(final double scaleY) {
        properties.put("scaleY", new SimpleDoubleProperty(scaleY));
        return (B) this;
    }

    public final B layoutX(final double layoutX) {
        properties.put("layoutX", new SimpleDoubleProperty(layoutX));
        return (B) this;
    }

    public final B layoutY(final double layoutY) {
        properties.put("layoutY", new SimpleDoubleProperty(layoutY));
        return (B) this;
    }

    public final B translateX(final double translateX) {
        properties.put("translateX", new SimpleDoubleProperty(translateX));
        return (B) this;
    }

    public final B translateY(final double translateY) {
        properties.put("translateY", new SimpleDoubleProperty(translateY));
        return (B) this;
    }

    public final B padding(final Insets insets) {
        properties.put("padding", new SimpleObjectProperty<>(insets));
        return (B) this;
    }


    public final MissionTimerX build() {
        final MissionTimerX timer = new MissionTimerX();

        for (String key : properties.keySet()) {
            if ("iconFitWidth".equals(key)) {
                timer.setIconFitWidth(((DoubleProperty) properties.get(key)).get());
            } else if ("prefSize".equals(key)) {
                Dimension2D dim = ((ObjectProperty<Dimension2D>) properties.get(key)).get();
                timer.setPrefSize(dim.getWidth(), dim.getHeight());
            } else if ("minSize".equals(key)) {
                Dimension2D dim = ((ObjectProperty<Dimension2D>) properties.get(key)).get();
                timer.setMinSize(dim.getWidth(), dim.getHeight());
            } else if ("maxSize".equals(key)) {
                Dimension2D dim = ((ObjectProperty<Dimension2D>) properties.get(key)).get();
                timer.setMaxSize(dim.getWidth(), dim.getHeight());
            } else if ("prefWidth".equals(key)) {
                timer.setPrefWidth(((DoubleProperty) properties.get(key)).get());
            } else if ("prefHeight".equals(key)) {
                timer.setPrefHeight(((DoubleProperty) properties.get(key)).get());
            } else if ("minWidth".equals(key)) {
                timer.setMinWidth(((DoubleProperty) properties.get(key)).get());
            } else if ("minHeight".equals(key)) {
                timer.setMinHeight(((DoubleProperty) properties.get(key)).get());
            } else if ("maxWidth".equals(key)) {
                timer.setMaxWidth(((DoubleProperty) properties.get(key)).get());
            } else if ("maxHeight".equals(key)) {
                timer.setMaxHeight(((DoubleProperty) properties.get(key)).get());
            } else if ("scaleX".equals(key)) {
                timer.setScaleX(((DoubleProperty) properties.get(key)).get());
            } else if ("scaleY".equals(key)) {
                timer.setScaleY(((DoubleProperty) properties.get(key)).get());
            } else if ("layoutX".equals(key)) {
                timer.setLayoutX(((DoubleProperty) properties.get(key)).get());
            } else if ("layoutY".equals(key)) {
                timer.setLayoutY(((DoubleProperty) properties.get(key)).get());
            } else if ("translateX".equals(key)) {
                timer.setTranslateX(((DoubleProperty) properties.get(key)).get());
            } else if ("translateY".equals(key)) {
                timer.setTranslateY(((DoubleProperty) properties.get(key)).get());
            } else if ("padding".equals(key)) {
                timer.setPadding(((ObjectProperty<Insets>) properties.get(key)).get());
            } else if ("title".equals(key)) {
                timer.setTitle(((StringProperty) properties.get(key)).get());
            } else if ("backgroundColor".equals(key)) {
                timer.setBackgroundColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("ringBackgroundColor".equals(key)) {
                timer.setRingBackgroundColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("ringColor".equals(key)) {
                timer.setRingColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("clockColor".equals(key)) {
                timer.setClockColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("titleColor".equals(key)) {
                timer.setTitleColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("itemColor".equals(key)) {
                timer.setItemColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("itemInnerColor".equals(key)) {
                timer.setItemInnerColor(((ObjectProperty<Color>) properties.get(key)).get());
            } else if ("startTime".equals(key)) {
                timer.setStartTime(((LongProperty) properties.get(key)).get());
            } else if ("timeFrame".equals(key)) {
                timer.setTimeFrame(((LongProperty) properties.get(key)).get());
            }
        }

        if (properties.keySet().contains("itemArray")) {
            timer.getItems().addAll(((ObjectProperty<Item[]>) properties.get("itemArray")).get());
        }
        if (properties.keySet().contains("itemList")) {
            timer.getItems().addAll(((ObjectProperty<List<Item>>) properties.get("itemList")).get());
        }

        properties.clear();
        return timer;

    }
}
