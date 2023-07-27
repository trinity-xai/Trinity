/*
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
package edu.jhuapl.trinity.javafx.components.timeline;

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

import edu.jhuapl.trinity.javafx.events.MissionTimerXEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.utils.Fonts;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.LongProperty;
import javafx.beans.property.LongPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * User: hansolo Date: 17.09.21 Time: 12:31
 *
 * @author Sean Phillips
 */
@DefaultProperty("children")
public class MissionTimerX extends Region {
    public DropShadow backwardIndicatorGlow, forwardIndicatorGlow,
        pauseIndicatorGlow, resetIndicatorGlow, playIndicatorGlow, playBackIndicatorGlow;
    private double iconFitWidth = 30.0;
    private static final double PREFERRED_WIDTH = 400;
    private static final double PREFERRED_HEIGHT = 150;
    private static final double MINIMUM_WIDTH = 20;
    private static final double MINIMUM_HEIGHT = 20;
    private static final double MAXIMUM_WIDTH = 4096;
    private static final double MAXIMUM_HEIGHT = 4096;
    private static final double ASPECT_RATIO = PREFERRED_HEIGHT / PREFERRED_WIDTH;
    private double outerCircleDiameterAspect = 1.84; //this.outerCircleDiameter = 1.84 * width;
    private double middleCircleDiameterAspect = 1.75; //this.middleCircleDiameter = 1.75 * width;
    private double innerCircleDiameterAspect = 1.66; //this.innerCircleDiameter = 1.66 * width;
    private static final long REALTIME_REDRAW = 1_000_000l;
    private static final long REALTIME_TIMER = 1_000_000_000l;
    public int timeDialator = 2; //@TODO SMP... I am not sure yet why I need this hack
    private LocalDateTime currentPropagationTime;
    private String stringCurrentPropRate;
    private LocalDateTime initialPropagationTime;

    private double width;
    private double height;
    private Canvas canvas;
    private GraphicsContext ctx;
    private Pane pane;
    private double outerCircleDiameter;
    private double middleCircleDiameter;
    private double innerCircleDiameter;
    private double centerX;
    private double centerY;
    private double outerOriginX;
    private double outerOriginY;
    private double middleOriginX;
    private double middleOriginY;
    private double innerOriginX;
    private double innerOriginY;
    private double clockFontSize;
    private double titleFontSize;
    private double itemFontSize;
    private double itemDiameter;
    private double itemRadius;
    private double itemLength;
    private double itemDotDiameter;
    private double itemDotRadius;
    private double itemTextDistance;
    private Font clockFont;
    private Font titleFont;
    private Font itemFont;
    private String _title;
    private StringProperty title;
    private Color _backgroundColor;
    private ObjectProperty<Color> backgroundColor;
    private Color _ringBackgroundColor;
    private ObjectProperty<Color> ringBackgroundColor;
    private Color _ringColor;
    private ObjectProperty<Color> ringColor;
    private Color _clockColor;
    private ObjectProperty<Color> clockColor;
    private Color _titleColor;
    private ObjectProperty<Color> titleColor;
    private Color _itemColor;
    private ObjectProperty<Color> itemColor;
    private Color _itemInnerColor;
    private ObjectProperty<Color> itemInnerColor;

    private long _startTime;
    private LongProperty startTime;
    private long _timeFrame;
    private LongProperty timeFrame;
    private boolean _running;
    private BooleanProperty running;
    private ObservableList<Item> items;
    private boolean itemToggle;
    private double totalAngle;
    private double angleStep;
    private long duration;
    private int days;
    private long hours;
    private long minutes;
    private long seconds;
    private InvalidationListener sizeListener;
    private long redrawStepSize;
    private long timerStepSize;
    private long lastRedrawCall;
    private long lastTimerCall;
    private double milliAngle;
    private AnimationTimer timer;
    private boolean pauseOn;
    private boolean playOn;
    private boolean playBackOn;
    public boolean internalClock = true;

    // ******************** Constructors **************************************
    public MissionTimerX() {
        this.outerCircleDiameter = outerCircleDiameterAspect * PREFERRED_WIDTH;
        this.middleCircleDiameter = middleCircleDiameterAspect * PREFERRED_WIDTH;
        this.innerCircleDiameter = innerCircleDiameterAspect * PREFERRED_WIDTH;
        this.centerX = 0.5 * PREFERRED_WIDTH;
        this.centerY = 0.92 * PREFERRED_WIDTH;
        this.outerOriginX = (PREFERRED_WIDTH - outerCircleDiameter) * 0.5;
        this.outerOriginY = 0;
        this.middleOriginX = (PREFERRED_WIDTH - middleCircleDiameter) * 0.5;
        this.middleOriginY = PREFERRED_HEIGHT - middleCircleDiameter * 0.09142857;
        this.innerOriginX = (PREFERRED_WIDTH - innerCircleDiameter) * 0.5;
        this.innerOriginY = PREFERRED_HEIGHT - innerCircleDiameter * 0.06927711;
        this.clockFontSize = 0.035 * PREFERRED_WIDTH;
        this.titleFontSize = 0.028 * PREFERRED_WIDTH;
        this.itemFontSize = 12.0; //0.0125 * PREFERRED_WIDTH;
        this.itemDiameter = 0.0125 * PREFERRED_WIDTH;
        this.itemRadius = itemDiameter * 0.5;
        this.itemLength = itemDiameter * 0.8;
        this.itemDotDiameter = 0.005 * PREFERRED_WIDTH;
        this.itemDotRadius = itemDotDiameter * 0.5;
        this.itemTextDistance = itemDiameter;
        this.clockFont = Fonts.robotoRegular(clockFontSize);
        this.titleFont = Fonts.robotoRegular(titleFontSize);
        this.itemFont = Fonts.robotoBlack(itemFontSize);
        this._title = "TITLE";
        this._backgroundColor = Color.BLACK;
        this._ringBackgroundColor = Color.rgb(15, 15, 15);
        this._ringColor = Color.rgb(45, 45, 45);
        this._clockColor = Color.WHITE;
        this._titleColor = Color.WHITE;
        this._itemColor = Color.WHITE;
        this._itemInnerColor = Color.CYAN;
        this._startTime = 0;
        this._timeFrame = Duration.ofMinutes(15).getSeconds(); // visible area => 70 degrees
        this._running = false;
        this.items = FXCollections.observableArrayList();
        this.itemToggle = true;
        this.totalAngle = 120.0;
        this.angleStep = totalAngle / _timeFrame;
        this.duration = 0;
        this.days = 0;
        this.hours = 0;
        this.minutes = 0;
        this.seconds = 0;
        this.sizeListener = o -> resize();
        this.redrawStepSize = REALTIME_REDRAW;
        this.timerStepSize = REALTIME_TIMER;
        this.lastRedrawCall = System.nanoTime();
        this.lastTimerCall = System.nanoTime();
        this.timer = new AnimationTimer() {
            @Override
            public void handle(final long now) {
                if (now > lastRedrawCall + redrawStepSize) {
                    milliAngle += angleStep / 100.0;
//                    redraw();
                    lastRedrawCall = now;
                }
                if (now > lastTimerCall + timerStepSize) {
                    long timeSpan = Math.abs((getStartTime() + duration) < 0 ? getStartTime() + duration : (duration + getStartTime()));
                    days = (int) TimeUnit.SECONDS.toDays(timeSpan);
                    hours = TimeUnit.SECONDS.toHours(timeSpan) - (days * 24);
                    minutes = TimeUnit.SECONDS.toMinutes(timeSpan) - (TimeUnit.SECONDS.toHours(timeSpan) * 60);
                    seconds = TimeUnit.SECONDS.toSeconds(timeSpan) - (TimeUnit.SECONDS.toMinutes(timeSpan) * 60);
                    if (internalClock)
                        duration++;
                    milliAngle = 0;
                    //redraw();
                    lastTimerCall = now;
                }
            }
        };
        initGraphics();
        registerListeners();
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> requestFocus());

        // USE THESE TO KEEP SOME OF THE BUTTONS ON
        pauseOn = true;
        playOn = false;
        playBackOn = false;
        stringCurrentPropRate = "1x";
        initialPropagationTime = LocalDateTime.now();
        currentPropagationTime = initialPropagationTime.plusSeconds(0);
    }

    public void clearItems() {
        items.clear();
        redraw();
    }

    public void addItems(List<Item> newItems) {
        items.addAll(newItems);
        redraw();
    }

    // ******************** Initialization ************************************
    private void initGraphics() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 || Double.compare(getWidth(), 0.0) <= 0
            || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        canvas = new Canvas(PREFERRED_WIDTH, PREFERRED_HEIGHT);
        ctx = canvas.getGraphicsContext2D();

        pane = new Pane(canvas);
        backwardIndicatorGlow = new DropShadow();
        backwardIndicatorGlow.setBlurType(BlurType.GAUSSIAN);
        backwardIndicatorGlow.setOffsetX(0f);
        backwardIndicatorGlow.setOffsetY(0f);
        backwardIndicatorGlow.setRadius(10);
        backwardIndicatorGlow.setSpread(0.5);
        backwardIndicatorGlow.colorProperty().set(Color.CYAN.deriveColor(1, 1, 1, 0.5));

        forwardIndicatorGlow = new DropShadow();
        forwardIndicatorGlow.setBlurType(BlurType.GAUSSIAN);
        forwardIndicatorGlow.setOffsetX(0f);
        forwardIndicatorGlow.setOffsetY(0f);
        forwardIndicatorGlow.setRadius(10);
        forwardIndicatorGlow.setSpread(0.5);
        forwardIndicatorGlow.colorProperty().set(Color.CYAN.deriveColor(1, 1, 1, 0.5));

        pauseIndicatorGlow = new DropShadow();
        pauseIndicatorGlow.setBlurType(BlurType.GAUSSIAN);
        pauseIndicatorGlow.setOffsetX(0f);
        pauseIndicatorGlow.setOffsetY(0f);
        pauseIndicatorGlow.setRadius(10);
        pauseIndicatorGlow.setSpread(0.5);
        pauseIndicatorGlow.colorProperty().set(Color.ALICEBLUE);

        resetIndicatorGlow = new DropShadow();
        resetIndicatorGlow.setBlurType(BlurType.GAUSSIAN);
        resetIndicatorGlow.setOffsetX(0f);
        resetIndicatorGlow.setOffsetY(0f);
        resetIndicatorGlow.setRadius(10);
        resetIndicatorGlow.setSpread(0.5);
        resetIndicatorGlow.colorProperty().set(Color.CYAN.deriveColor(1, 1, 1, 0.5));

        playIndicatorGlow = new DropShadow();
        playIndicatorGlow.setBlurType(BlurType.GAUSSIAN);
        playIndicatorGlow.setOffsetX(0f);
        playIndicatorGlow.setOffsetY(0f);
        playIndicatorGlow.setRadius(10);
        playIndicatorGlow.setSpread(0.5);
        playIndicatorGlow.colorProperty().set(Color.CYAN.deriveColor(1, 1, 1, 0.5));

        playBackIndicatorGlow = new DropShadow();
        playBackIndicatorGlow.setBlurType(BlurType.GAUSSIAN);
        playBackIndicatorGlow.setOffsetX(0f);
        playBackIndicatorGlow.setOffsetY(0f);
        playBackIndicatorGlow.setRadius(10);
        playBackIndicatorGlow.setSpread(0.5);
        playBackIndicatorGlow.colorProperty().set(Color.CYAN.deriveColor(1, 1, 1, 0.5));

        // SET THE DECREASE PLAY RATE BUTTON
        ImageView backwardImageView = ResourceUtils.loadIcon("fastForward", iconFitWidth);
        HBox backwardImageHBox = new HBox(3, backwardImageView);
        backwardImageHBox.setAlignment(Pos.CENTER_LEFT);
        backwardImageHBox.setRotate(180);

        // SET THE INCREASE PLAY RATE BUTTON
        ImageView forwardImageView = ResourceUtils.loadIcon("fastForward", iconFitWidth);
        HBox forwardImageHBox = new HBox(3, forwardImageView);
        forwardImageHBox.setAlignment(Pos.CENTER_RIGHT);

        // SET THE PLAY, RESET, PAUSE, AND BACKWARDS PLAY BUTTON
        ImageView playForwardImageView = ResourceUtils.loadIcon("forward", iconFitWidth);
        ImageView playBackwardImageView = ResourceUtils.loadIcon("backward", iconFitWidth);
        ImageView pauseImageView = ResourceUtils.loadIcon("pause", iconFitWidth);
        ImageView resetImageView = ResourceUtils.loadIcon("resettime", iconFitWidth);
        HBox centerBackwardImageHBox = new HBox(3, playBackwardImageView);
        HBox centerForwardImageHBox = new HBox(3, playForwardImageView);
        HBox centerPauseImageHBox = new HBox(3, pauseImageView);
        HBox centerResetImageHBox = new HBox(3, resetImageView);

        // SET HE INDICATO GLOW EFFECT
        backwardImageHBox.setEffect(backwardIndicatorGlow);
        forwardImageHBox.setEffect(forwardIndicatorGlow);
        centerBackwardImageHBox.setEffect(playBackIndicatorGlow);
        centerForwardImageHBox.setEffect(playIndicatorGlow);
        centerPauseImageHBox.setEffect(pauseIndicatorGlow);
        centerResetImageHBox.setEffect(resetIndicatorGlow);
        HBox bottomHBox = new HBox(25, backwardImageHBox, centerBackwardImageHBox,
            centerResetImageHBox, centerPauseImageHBox, centerForwardImageHBox, forwardImageHBox);

        getChildren().setAll(pane);
        getChildren().add(bottomHBox);

        // ADJUST THE PLACEMENT OF THE BOTTOM HBOX
        bottomHBox.layoutXProperty().bind(pane.widthProperty().divide(2.0).subtract(150));
        bottomHBox.layoutYProperty().bind(pane.heightProperty().subtract(40));

        // SET EFFECT ON THE DECREASE RATE
        backwardImageHBox.setOnMouseClicked(e -> {
            Platform.runLater(() -> {
                backwardImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_DECREASE_PROPRATE));
                backwardImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_GET_CURRENT_PROP_RATE));
                backwardIndicatorGlow.setColor(Color.ALICEBLUE);
            });
        });
        backwardImageHBox.setOnMouseEntered(e -> {
            Platform.runLater(() -> {
                backwardIndicatorGlow.setColor(Color.ALICEBLUE);
            });
        });
        backwardImageHBox.setOnMouseExited(e -> {
            Platform.runLater(() -> {
                backwardIndicatorGlow.setColor(Color.CYAN.deriveColor(1, 1, 1, 0.5));
            });
        });

        // *********************************************************************
        // SET EFFECT ON THE INCREASE RATE
        forwardImageHBox.setOnMouseClicked(e -> {
            Platform.runLater(() -> {
                forwardImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_INCREASE_PROPRATE));
                forwardImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_GET_CURRENT_PROP_RATE));
            });
        });
        forwardImageHBox.setOnMouseEntered(e -> {
            Platform.runLater(() -> {
                forwardIndicatorGlow.setColor(Color.ALICEBLUE);
            });
        });
        forwardImageHBox.setOnMouseExited(e -> {
            Platform.runLater(() -> {
                forwardIndicatorGlow.setColor(Color.CYAN.deriveColor(1, 1, 1, 0.5));
            });
        });

        // *********************************************************************
        // SET EFFECT ON THE PLAY BACKWARDS BUTTON
        centerBackwardImageHBox.setOnMouseClicked(e -> {
            Platform.runLater(() -> {
                centerBackwardImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_PLAY_BACKWARDS));
                playBackOn = true;
                playOn = false;
                pauseOn = false;

                playBackIndicatorGlow.setColor(Color.ALICEBLUE);
                playIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                pauseIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
            });
        });
        centerBackwardImageHBox.setOnMouseEntered(e -> {
            Platform.runLater(() -> {
                if (!playBackOn) {
                    playBackIndicatorGlow.setColor(Color.ALICEBLUE);
                }
            });
        });
        centerBackwardImageHBox.setOnMouseExited(e -> {
            Platform.runLater(() -> {
                if (!playBackOn) {
                    playBackIndicatorGlow.setColor(Color.CYAN.deriveColor(1, 1, 1, 0.5));
                }
            });
        });
        // *********************************************************************
        // SET EFFECT ON THE RESET BUTTON
        centerResetImageHBox.setOnMouseClicked(e -> {
            Platform.runLater(() -> {
                playBackOn = false;
                playOn = false;
                pauseOn = false;
                resetIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                playIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                pauseIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                resetIndicatorGlow.setColor(Color.ALICEBLUE);
                centerResetImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_RESTART));
            });
        });
        centerResetImageHBox.setOnMouseEntered(e -> {
            Platform.runLater(() -> {
//                if (!pauseOn){
                resetIndicatorGlow.setColor(Color.ALICEBLUE);
//                }
            });
        });
        centerResetImageHBox.setOnMouseExited(e -> {
            Platform.runLater(() -> {
//                if (!pauseOn){
                resetIndicatorGlow.setColor(Color.CYAN.deriveColor(1, 1, 1, 0.5));
//                }
            });
        });

        // *********************************************************************
        // SET EFFECT ON THE PAUSE BUTTON
        centerPauseImageHBox.setOnMouseClicked(e -> {
            Platform.runLater(() -> {
                playBackOn = false;
                playOn = false;
                pauseOn = true;
                playBackIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                playIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                pauseIndicatorGlow.setColor(Color.ALICEBLUE);
                centerPauseImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_PAUSE));
            });
        });
        centerPauseImageHBox.setOnMouseEntered(e -> {
            Platform.runLater(() -> {
                if (!pauseOn) {
                    pauseIndicatorGlow.setColor(Color.ALICEBLUE);
                }
            });
        });
        centerPauseImageHBox.setOnMouseExited(e -> {
            Platform.runLater(() -> {
                if (!pauseOn) {
                    pauseIndicatorGlow.setColor(Color.CYAN.deriveColor(1, 1, 1, 0.5));
                }
            });
        });

        // *********************************************************************
        // SET EFFECT ON THE PLAY BUTTON
        centerForwardImageHBox.setOnMouseClicked(e -> {
            Platform.runLater(() -> {
                playBackOn = false;
                playOn = true;
                pauseOn = false;
                centerForwardImageHBox.getScene().getRoot().fireEvent(
                    new TimelineEvent(TimelineEvent.TIMELINE_PLAY));
                playBackIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
                playIndicatorGlow.setColor(Color.ALICEBLUE);
                pauseIndicatorGlow.setColor((Color.CYAN.deriveColor(1, 1, 1, 0.5)));
            });
        });
        centerForwardImageHBox.setOnMouseEntered(e -> {
            Platform.runLater(() -> {
                if (!playOn) {
                    playIndicatorGlow.setColor(Color.ALICEBLUE);
                }
            });
        });
        centerForwardImageHBox.setOnMouseExited(e -> {
            Platform.runLater(() -> {
                if (!playOn) {
                    playIndicatorGlow.setColor(Color.CYAN.deriveColor(1, 1, 1, 0.5));
                }
            });
        });

    }

    private void registerListeners() {
        widthProperty().addListener(sizeListener);
        heightProperty().addListener(sizeListener);
    }

    // ******************** Methods *******************************************
    @Override
    protected double computeMinWidth(final double height) {
        return MINIMUM_WIDTH;
    }

    @Override
    protected double computeMinHeight(final double width) {
        return MINIMUM_HEIGHT;
    }

    @Override
    protected double computePrefWidth(final double height) {
        return super.computePrefWidth(height);
    }

    @Override
    protected double computePrefHeight(final double width) {
        return super.computePrefHeight(width);
    }

    @Override
    protected double computeMaxWidth(final double height) {
        return MAXIMUM_WIDTH;
    }

    @Override
    protected double computeMaxHeight(final double width) {
        return MAXIMUM_HEIGHT;
    }

    @Override
    public ObservableList<Node> getChildren() {
        return super.getChildren();
    }

    public void resetTimelineSpeed() {
        redrawStepSize = REALTIME_REDRAW;
        timerStepSize = REALTIME_TIMER;
    }

    public void increaseTimelineSpeed() {
        redrawStepSize /= 10;
        timerStepSize /= 10;
    }

    public void decreaseTimelineSpeed() {
        redrawStepSize *= 10;
        timerStepSize *= 10;
    }

    public String getTitle() {
        return null == title ? _title : title.get();
    }

    public void setTitle(final String title) {
        if (null == this.title) {
            _title = title;
//            redraw();
        } else {
            this.title.set(title);
        }
    }

    public StringProperty titleProperty() {
        if (null == title) {
            title = new StringPropertyBase(_title) {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "title";
                }
            };
            _title = null;
        }
        return title;
    }

    public Color getBackgroundColor() {
        return null == backgroundColor ? _backgroundColor : backgroundColor.get();
    }

    public void setBackgroundColor(final Color color) {
        if (null == backgroundColor) {
            _backgroundColor = color;
//            redraw();
        } else {
            backgroundColor.set(color);
        }
    }

    public ObjectProperty<Color> backgroundColorProperty() {
        if (null == backgroundColor) {
            backgroundColor = new ObjectPropertyBase() {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "backgroundColor";
                }
            };
            _backgroundColor = null;
        }
        return backgroundColor;
    }

    public Color getRingBackgroundColor() {
        return null == ringBackgroundColor ? _ringBackgroundColor : ringBackgroundColor.get();
    }

    public void setRingBackgroundColor(final Color color) {
        if (null == ringBackgroundColor) {
            _ringBackgroundColor = color;
//            redraw();
        } else {
            ringBackgroundColor.set(color);
        }
    }

    public ObjectProperty<Color> ringBackgroundColorProperty() {
        if (null == ringBackgroundColor) {
            ringBackgroundColor = new ObjectPropertyBase() {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "ringBackgroundColor";
                }
            };
            _ringBackgroundColor = null;
        }
        return ringBackgroundColor;
    }

    public Color getRingColor() {
        return null == ringColor ? _ringColor : ringColor.get();
    }

    public void setRingColor(final Color color) {
        if (null == ringColor) {
            _ringColor = color;
//            redraw();
        } else {
            ringColor.set(color);
        }
    }

    public ObjectProperty<Color> ringColorProperty() {
        if (null == ringColor) {
            ringColor = new ObjectPropertyBase() {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "ringColor";
                }
            };
            _ringColor = null;
        }
        return ringColor;
    }

    public Color getClockColor() {
        return null == clockColor ? _clockColor : clockColor.get();
    }

    public void setClockColor(final Color color) {
        if (null == clockColor) {
            _clockColor = color;
//            redraw();
        } else {
            clockColor.set(color);
        }
    }

    public ObjectProperty<Color> clockColorProperty() {
        if (null == clockColor) {
            clockColor = new ObjectPropertyBase(_clockColor) {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "clockColor";
                }
            };
            _clockColor = null;
        }
        return clockColor;
    }

    public Color getTitleColor() {
        return null == titleColor ? _titleColor : titleColor.get();
    }

    public void setTitleColor(final Color color) {
        if (null == titleColor) {
            _titleColor = color;
//            redraw();
        } else {
            titleColor.set(color);
        }
    }

    public ObjectProperty<Color> titleColorProperty() {
        if (null == titleColor) {
            titleColor = new ObjectPropertyBase(_titleColor) {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "titleColor";
                }
            };
            _titleColor = null;
        }
        return titleColor;
    }

    public Color getItemColor() {
        return null == itemColor ? _itemColor : itemColor.get();
    }

    public void setItemColor(final Color color) {
        if (null == itemColor) {
            _itemColor = color;
//            redraw();
        } else {
            itemColor.set(color);
        }
    }

    public ObjectProperty<Color> itemColorProperty() {
        if (null == itemColor) {
            itemColor = new ObjectPropertyBase(_itemColor) {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "itemColor";
                }
            };
            _itemColor = null;
        }
        return itemColor;
    }

    public Color getItemInnerColor() {
        return null == itemInnerColor ? _itemInnerColor : itemInnerColor.get();
    }

    public void setItemInnerColor(final Color color) {
        if (null == itemInnerColor) {
            _itemInnerColor = color;
//            redraw();
        } else {
            itemInnerColor.set(color);
        }
    }

    public ObjectProperty<Color> itemInnerColorProperty() {
        if (null == itemInnerColor) {
            itemInnerColor = new ObjectPropertyBase(_itemInnerColor) {
                @Override
                protected void invalidated() {
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "itemInnerColor";
                }
            };
            _itemInnerColor = null;
        }
        return itemInnerColor;
    }

    public void updateTime(long secondsFromStart, String stringCurrentPropRate) {
        duration = secondsFromStart;
        currentPropagationTime = initialPropagationTime.plusSeconds(secondsFromStart);
        this.stringCurrentPropRate = stringCurrentPropRate;
        redraw();
    }

    public void setInitialTime(LocalDateTime initialTime) {
        initialPropagationTime = initialTime;
        redraw();
    }

    public void updatePropRate(String stringCurrentPropRate) {
        this.stringCurrentPropRate = stringCurrentPropRate;
        redraw();
    }

    public long getStartTime() {
        return null == startTime ? _startTime : startTime.get();
    }

    public void setStartTime(final long startTime) {
        if (null == this.startTime) {
            stop();
            _startTime = startTime;
            long timeSpan = Math.abs((getStartTime() + duration) < 0 ? getStartTime() + duration : (duration + getStartTime()));
            days = (int) TimeUnit.SECONDS.toDays(timeSpan);
            hours = TimeUnit.SECONDS.toHours(timeSpan) - (days * 24);
            minutes = TimeUnit.SECONDS.toMinutes(timeSpan) - (TimeUnit.SECONDS.toHours(timeSpan) * 60);
            seconds = TimeUnit.SECONDS.toSeconds(timeSpan) - (TimeUnit.SECONDS.toMinutes(timeSpan) * 60);
            redraw();
        } else {
            this.startTime.set(startTime);
        }
    }

    public LongProperty startTimeProperty() {
        if (null == startTime) {
            startTime = new LongPropertyBase(_startTime) {
                @Override
                protected void invalidated() {
//                    stop();
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "startTime";
                }
            };
        }
        return startTime;
    }

    public long getTimeFrame() {
        return null == timeFrame ? _timeFrame : timeFrame.get();
    }

    public void setTimeFrame(final long timeFrame) {
        if (null == this.timeFrame) {
            _timeFrame = timeFrame;
            angleStep = totalAngle / _timeFrame;
//            redraw();
        } else {
            this.timeFrame.set(timeFrame);
        }
    }

    public LongProperty timeFrameProperty() {
        if (null == timeFrame) {
            timeFrame = new LongPropertyBase(_timeFrame) {
                @Override
                protected void invalidated() {
                    angleStep = totalAngle / get();
//                    redraw();
                }

                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "timeFrame";
                }
            };
        }
        return timeFrame;
    }

    public boolean isRunning() {
        return null == running ? _running : running.get();
    }

    private void setRunning(final boolean running) {
        if (null == this.running) {
            _running = running;
        } else {
            this.running.set(running);
        }
    }

    public ReadOnlyBooleanProperty runningProperty() {
        if (null == running) {
            running = new BooleanPropertyBase(_running) {
                @Override
                public Object getBean() {
                    return MissionTimerX.this;
                }

                @Override
                public String getName() {
                    return "running";
                }
            };
        }
        return running;
    }

    public ObservableList<Item> getItems() {
        return items;
    }

    public void start() {
        redraw();
        if (isRunning()) {
            return;
        }
        duration = 0;
        setRunning(true);
        timer.start();
    }

    public void pause() {
        stop();
    }

    public void resume() {
        if (isRunning()) {
            return;
        }
        setRunning(true);
        timer.start();
    }

    public void stop() {
        if (!isRunning()) {
            return;
        }
        setRunning(false);
        timer.stop();
    }

    public void dispose() {
        widthProperty().removeListener(sizeListener);
        heightProperty().removeListener(sizeListener);
        //items.removeListener(itemListener);
    }

    // ******************** Layout *******************************************
    private void resize() {
        width = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();

//        if (ASPECT_RATIO * width > height) {
//             width = 1 / (ASPECT_RATIO / height);
//        } else if (1 / (ASPECT_RATIO / height) > width) {
//             height = ASPECT_RATIO * width;
//        }

        if (width > 0 && height > 0) {
            canvas.setWidth(width);
            canvas.setHeight(height);

            pane.setMaxSize(width, height);
            pane.setPrefSize(width, height);
            pane.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

            this.outerCircleDiameter = outerCircleDiameterAspect * width;
            this.middleCircleDiameter = middleCircleDiameterAspect * width;
            this.innerCircleDiameter = innerCircleDiameterAspect * width;
            this.centerX = 0.50 * width;
            this.centerY = outerCircleDiameter * 0.5;
            this.outerOriginX = (width - outerCircleDiameter) * 0.5;
            this.outerOriginY = 0;
            this.middleOriginX = (width - middleCircleDiameter) * 0.5;
            this.middleOriginY = height - middleCircleDiameter * 0.09142857;
            this.innerOriginX = (width - innerCircleDiameter) * 0.5;
            this.innerOriginY = height - innerCircleDiameter * 0.06927711;
            this.clockFontSize = 0.035 * PREFERRED_WIDTH;
            this.titleFontSize = 0.028 * PREFERRED_WIDTH;
            this.itemFontSize = 10.0; //0.0125 * PREFERRED_WIDTH;

            this.itemDiameter = 0.0125 * width;
            this.itemRadius = 0.5 * itemDiameter;
            this.itemLength = itemDiameter * 0.85;
            this.itemDotDiameter = 0.005 * width;
            this.itemDotRadius = itemDotDiameter * 0.5;
            this.itemTextDistance = itemDiameter;
            this.clockFont = Fonts.robotoRegular(clockFontSize);
            this.titleFont = Fonts.robotoRegular(titleFontSize);
            this.itemFont = Fonts.robotoBlack(itemFontSize);
            redraw();
        }
    }

    private void redraw() {
        ctx.clearRect(0, 0, width, height);

        // Draw outer background circle
        ctx.setFill(getRingBackgroundColor());
        ctx.fillOval(outerOriginX, outerOriginY, outerCircleDiameter, outerCircleDiameter);

        // Stroke timeline ring
        ctx.setStroke(getRingColor());
        ctx.setLineWidth(width * 0.0025);
        ctx.strokeOval(middleOriginX, middleOriginY, middleCircleDiameter, middleCircleDiameter);

        // Draw inner background circle
        ctx.setFill(getBackgroundColor());
        ctx.fillOval(innerOriginX, innerOriginY, innerCircleDiameter, innerCircleDiameter);

        ctx.setLineWidth(width * 0.0025);
        ctx.strokeOval(innerOriginX, innerOriginY, innerCircleDiameter, innerCircleDiameter);

        // Draw Completed arc
        ctx.setStroke(getItemColor());
        ctx.strokeArc(middleOriginX, middleOriginY, middleCircleDiameter, middleCircleDiameter, 90, 90, ArcType.OPEN);
        ctx.strokeLine(centerX, middleOriginY - width * 0.0025, centerX, middleOriginY + width * 0.0025);

        ctx.setTextAlign(TextAlignment.CENTER);
        ctx.setTextBaseline(VPos.CENTER);

        // Draw title
        ctx.setFill(getTitleColor());
        ctx.setFont(titleFont);
        ctx.fillText(new StringBuilder().
                append("[").append(stringCurrentPropRate).append("]  ").
                append(currentPropagationTime.getMonth()).
                append(" ").
                append(String.format("%02d", currentPropagationTime.getDayOfMonth())).
                append(" ").
                append(String.format("%04d", currentPropagationTime.getYear())).
                append(" ").
                append(String.format("%02d", currentPropagationTime.getHour())).
                append(":").append(String.format("%02d", currentPropagationTime.getMinute())).
                append(":").append(String.format("%02d", currentPropagationTime.getSecond())).
                append(" UTC").
                toString(),
            centerX, height * 0.60);


        // Iterate over all items
        ctx.setFont(itemFont);
        for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            Item item = items.get(itemIndex);
            double itemAngle = -getStartTime() * angleStep + (item.getTime() * timeDialator * angleStep) - duration * angleStep - milliAngle;
            if (itemAngle > -35 && itemAngle < 35) {
                // Save canvas
                ctx.save();

                // Pre-rotate the canvas dependent on the item angle
                ctx.translate(centerX, centerY);
                ctx.rotate(itemAngle);
                ctx.translate(-centerX, -centerY);

                // Fill item circle with background color
                ctx.setLineWidth(0.0015 * width);
                ctx.setFill(getRingBackgroundColor());
                ctx.fillOval(centerX - itemRadius, middleOriginY - itemRadius, itemDiameter, itemDiameter);

                // Define item color dependent on current angle
                Color itemColor;
                if (itemAngle < -24) {
                    itemColor = getItemColor().darker().darker();
                } else if (itemAngle < -12) {
                    itemColor = getItemColor().darker().darker();
                } else if (itemAngle < 12) {
                    itemColor = getItemColor();
                } else if (itemAngle < 24) {
                    itemColor = getItemColor().darker().darker();
                } else {
                    itemColor = getItemColor().darker().darker().darker().darker();
                }

                // Draw item extension
                ctx.setLineCap(StrokeLineCap.ROUND);
                ctx.setStroke(itemColor);
                ctx.setFill(itemColor);
                if (item.isUp()) {
                    ctx.strokeLine(centerX, middleOriginY - itemRadius, centerX, middleOriginY - itemLength);
                    ctx.setTextBaseline(VPos.BOTTOM);
                    ctx.fillText(item.getName(), centerX, middleOriginY - itemTextDistance);
                } else {
                    ctx.strokeLine(centerX, middleOriginY + itemRadius, centerX, middleOriginY + itemLength);
                    ctx.setTextBaseline(VPos.TOP);
                    ctx.fillText(item.getName(), centerX, middleOriginY + itemTextDistance);
                }

                // Draw outer item circle
                ctx.setStroke(itemColor);
                ctx.strokeOval(centerX - itemRadius, middleOriginY - itemRadius, itemDiameter, itemDiameter);

                //Depending on playback direction how should we deal with this item?
                //Is the current time past the this item's time?
                if (getStartTime() + (duration - 1) >= item.getTime() * timeDialator) {
                    if (!item.isProcessed()) {
                        item.setProcessed(true);
                        fireEvent(new MissionTimerXEvent(item, MissionTimerX.this, null, MissionTimerXEvent.TRIGGERED));
                        getScene().getRoot().fireEvent(new MissionTimerXEvent(item, itemIndex, MissionTimerXEvent.NEW_ITEM_INDEX));
                    }
                    // Draw inner item circle if item is processed
                    ctx.setFill(getItemInnerColor());
                    ctx.fillOval(centerX - itemDotRadius, middleOriginY - itemDotRadius, itemDotDiameter, itemDotDiameter);
                } else {
                    boolean wasProcessed = item.isProcessed();
                    //if it was processed before we need to unprocess it and step backwards
                    if (wasProcessed) {
                        item.setProcessed(false);
                        getScene().getRoot().fireEvent(new MissionTimerXEvent(item, itemIndex, MissionTimerXEvent.NEW_ITEM_INDEX));
                    }
                }

                // Restore canvas for next item
                ctx.restore();
            }
        }
    }

    /**
     * @return the iconFitWidth
     */
    public double getIconFitWidth() {
        return iconFitWidth;
    }

    /**
     * @param iconFitWidth the iconFitWidth to set
     */
    public void setIconFitWidth(double iconFitWidth) {
        this.iconFitWidth = iconFitWidth;
    }
}
