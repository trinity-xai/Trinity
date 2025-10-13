package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Hover-reveal micro toolbar for export actions (Copy, Save, More).
 * Intended for placement in a title bar (e.g., mainTitleView).
 * <p>
 * Sticky behavior: clicking the Export icon toggles a "sticky hold".
 * - Sticky ON: toolbar remains expanded even when the mouse leaves (export icon shows pinned indicator).
 * - Sticky OFF: toolbar collapses on mouse exit (with a short delay).
 */
public class ExportMicroToolbar {

    private static final Logger LOG = LoggerFactory.getLogger(ExportMicroToolbar.class);

    private final Pane titleBarParent;      // mainTitleView (Pane or AnchorPane)
    private final Node chromeNode;
    private final Node contentNode;
    private final Node contextTarget;
    private final javafx.scene.Scene scene;
    private final double iconFitWidth;

    private HBox microToolbar;
    private Label mtExport;
    private Label mtCopy;
    private Label mtSave;
    private Label mtOptions;

    private boolean expanded = false;
    private boolean stickyHold = false;

    private PauseTransition hideDelay;
    private ContextMenu optionsMenu;

    // Guard and geometry caches for robust animations
    private ParallelTransition currentAnim;
    private double collapsedWidth = -1;
    private double expandedWidth = -1;

    // Options state
    private boolean includeChrome = false;
    private boolean transparentPng = true;
    private int exportScale = 2; // 1, 2, or 3

    // Styling radii
    private final CornerRadii pillRadii = new CornerRadii(6);
    private final CornerRadii shellRadii = new CornerRadii(10);

    // Shared visuals (no inline CSS)
    private Background baseBg;
    private Background pinnedBg;
    private Border hoverBorder;
    private Border pinnedBorder;

    public ExportMicroToolbar(
        Pane titleBarParent,
        Node chromeNode,
        Node contentNode,
        Node contextTarget,
        javafx.scene.Scene scene,
        double iconFitWidth
    ) {
        this.titleBarParent = Objects.requireNonNull(titleBarParent);
        this.chromeNode = Objects.requireNonNull(chromeNode);
        this.contentNode = Objects.requireNonNull(contentNode);
        this.contextTarget = Objects.requireNonNull(contextTarget);
        this.scene = Objects.requireNonNull(scene);
        this.iconFitWidth = (iconFitWidth > 0) ? iconFitWidth : 32.0;
    }

    /**
     * Attach to the right side of the title bar (default right inset = 12, vertical center).
     */
    public void installInTitleBarRight() {
        installInTitleBarRight(12.0, 0.0);
    }

    /**
     * Attach to the right side of the title bar with custom right inset and vertical offset.
     * For non-AnchorPane parents, the toolbar is kept right-aligned and vertically centered;
     * verticalOffset shifts it up/down from the center (positive moves it down).
     */
    public void installInTitleBarRight(double rightInset, double verticalOffset) {
        buildUI();
        titleBarParent.getChildren().add(microToolbar);

        if (titleBarParent instanceof AnchorPane) {
            AnchorPane.setRightAnchor(microToolbar, rightInset);
            titleBarParent.heightProperty().addListener((obs, ov, nv) ->
                AnchorPane.setTopAnchor(microToolbar,
                    Math.max(0.0, (nv.doubleValue() - microToolbar.getHeight()) / 2.0 + verticalOffset)));
            microToolbar.applyCss();
            microToolbar.layout();
            AnchorPane.setTopAnchor(microToolbar,
                Math.max(0.0, (titleBarParent.getHeight() - microToolbar.getHeight()) / 2.0 + verticalOffset));
        } else {
            Runnable relayout = () -> {
                microToolbar.applyCss();
                microToolbar.layout();
                double nx = Math.max(0.0, titleBarParent.getWidth() - microToolbar.getWidth() - rightInset);
                double ny = Math.max(0.0, (titleBarParent.getHeight() - microToolbar.getHeight()) / 2.0 + verticalOffset);
                microToolbar.setLayoutX(nx);
                microToolbar.setLayoutY(ny);
            };
            titleBarParent.widthProperty().addListener((o, ov, nv) -> relayout.run());
            titleBarParent.heightProperty().addListener((o, ov, nv) -> relayout.run());
            microToolbar.layoutBoundsProperty().addListener((o, ov, nv) -> relayout.run());
            relayout.run();
        }

        // Right-click anywhere on the contextTarget opens the options menu
        contextTarget.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.isSecondaryButtonDown()) {
                ensureMenu();
                optionsMenu.show(contextTarget, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        // Compute widths and start fully collapsed & unpinned
        Platform.runLater(() -> {
            computeWidths();
            forceCollapsedVisuals();
            stickyHold = false;
            updateStickyVisuals();
        });
    }

    // ---------- UI ----------

    private void buildUI() {
        // Shared visuals
        baseBg = new Background(new BackgroundFill(
            Color.CYAN.deriveColor(1, 1, 1, 0.10), pillRadii, Insets.EMPTY));
        pinnedBg = new Background(new BackgroundFill(
            Color.CYAN.deriveColor(1, 1, 1, 0.20), pillRadii, Insets.EMPTY));
        hoverBorder = new Border(new BorderStroke(
            Color.WHITE, BorderStrokeStyle.SOLID, pillRadii, new BorderWidths(1),
            new Insets(0, -3, 0, -3)));
        pinnedBorder = new Border(new BorderStroke(
            Color.CYAN, BorderStrokeStyle.SOLID, pillRadii, new BorderWidths(1.5)));

        ImageView exportIv = ResourceUtils.loadIcon("export", iconFitWidth);
        mtExport = new Label("Tools", exportIv);
        mtExport.setContentDisplay(ContentDisplay.TOP);

        ImageView copyIv = ResourceUtils.loadIcon("snapshot", iconFitWidth);
        mtCopy = new Label("Copy", copyIv);
        mtCopy.setContentDisplay(ContentDisplay.TOP);

        ImageView saveIv = ResourceUtils.loadIcon("save", iconFitWidth);
        mtSave = new Label("Save", saveIv);
        mtSave.setContentDisplay(ContentDisplay.TOP);

        ImageView optsIv = ResourceUtils.loadIcon("configuration", iconFitWidth);
        mtOptions = new Label("More", optsIv);
        mtOptions.setContentDisplay(ContentDisplay.TOP);

        // Base visuals + generic hover for non-export buttons
        Label[] all = new Label[]{mtExport, mtCopy, mtSave, mtOptions};
        for (Label l : all) {
            l.setBackground(baseBg);
            l.setPadding(new Insets(4, 6, 4, 6));
        }
        for (Label l : new Label[]{mtCopy, mtSave, mtOptions}) {
            l.setOnMouseEntered(e -> l.setBorder(hoverBorder));
            l.setOnMouseExited(e -> l.setBorder(null));
        }

        // Export icon hover respects sticky pin
        mtExport.setOnMouseEntered(e -> {
            if (!stickyHold) mtExport.setBorder(hoverBorder);
        });
        mtExport.setOnMouseExited(e -> {
            if (stickyHold) mtExport.setBorder(pinnedBorder);
            else mtExport.setBorder(null);
        });

        // Actions
        mtCopy.setOnMouseClicked(e -> copySnapshotToClipboard());
        mtSave.setOnMouseClicked(e -> saveSnapshotToFile());
        mtOptions.setOnMouseClicked(e -> {
            ensureMenu();
            optionsMenu.show(mtOptions, e.getScreenX(), e.getScreenY());
        });

        // Sticky toggle behavior on the export icon
        mtExport.setOnMouseClicked(e -> {
            if (!expanded) {
                stickyHold = true;
                updateStickyVisuals();
                setExpanded(true, true);
            } else {
                stickyHold = !stickyHold;
                updateStickyVisuals();
                if (!stickyHold && !microToolbar.isHover()) {
                    hideDelay.playFromStart();
                }
            }
        });

        microToolbar = new HBox(8, mtExport, mtCopy, mtSave, mtOptions);
        microToolbar.setPadding(new Insets(2));
        microToolbar.setAlignment(Pos.CENTER);
        microToolbar.setBackground(new Background(new BackgroundFill(
            Color.rgb(0, 0, 0, 0.35), shellRadii, Insets.EMPTY)));
        microToolbar.setBorder(new Border(new BorderStroke(
            Color.rgb(255, 255, 255, 0.15), BorderStrokeStyle.SOLID, shellRadii, BorderWidths.DEFAULT)));
        microToolbar.setOpacity(0.85);
        microToolbar.setPickOnBounds(false);

        // Start collapsed; visibility enforced after attachment
        setExpanded(false, false);

        hideDelay = new PauseTransition(Duration.millis(200));
        hideDelay.setOnFinished(e -> {
            if (!stickyHold) setExpanded(false, true);
        });

        EventHandler<MouseEvent> enter = e -> {
            hideDelay.stop();
            setExpanded(true, true);
        };
        EventHandler<MouseEvent> exit = e -> {
            if (!stickyHold) hideDelay.playFromStart();
        };

        microToolbar.addEventHandler(MouseEvent.MOUSE_ENTERED, enter);
        microToolbar.addEventHandler(MouseEvent.MOUSE_EXITED, exit);
    }

    // Pinned indicator visuals
    private void updateStickyVisuals() {
        if (stickyHold) {
            mtExport.setBackground(pinnedBg);
            mtExport.setBorder(pinnedBorder);
        } else {
            mtExport.setBackground(baseBg);
            mtExport.setBorder(null);
        }
    }

    // Compute and cache definitive collapsed/expanded widths to avoid drift
    private void computeWidths() {
        // Measure expanded width
        showExtrasImmediate(true);
        microToolbar.applyCss();
        microToolbar.layout();
        expandedWidth = microToolbar.prefWidth(-1);

        // Measure collapsed width
        showExtrasImmediate(false);
        microToolbar.applyCss();
        microToolbar.layout();
        collapsedWidth = microToolbar.prefWidth(-1);
    }

    private void forceCollapsedVisuals() {
        cancelCurrentAnim();
        showExtrasImmediate(false);
        microToolbar.setOpacity(0.85);
        microToolbar.setPrefWidth(collapsedWidth > 0 ? collapsedWidth : Region.USE_COMPUTED_SIZE);
        microToolbar.applyCss();
        microToolbar.layout();
        expanded = false;
    }

    private void showExtrasImmediate(boolean show) {
        mtCopy.setManaged(show);
        mtCopy.setVisible(show);
        mtCopy.setOpacity(show ? 1.0 : 0.0);

        mtSave.setManaged(show);
        mtSave.setVisible(show);
        mtSave.setOpacity(show ? 1.0 : 0.0);

        mtOptions.setManaged(show);
        mtOptions.setVisible(show);
        mtOptions.setOpacity(show ? 1.0 : 0.0);
    }

    private void cancelCurrentAnim() {
        if (currentAnim != null) {
            currentAnim.stop();
            currentAnim = null;
            double w = microToolbar.getWidth() > 0 ? microToolbar.getWidth() : microToolbar.prefWidth(-1);
            microToolbar.setPrefWidth(w);
        }
    }

    private void setExpanded(boolean expand, boolean animate) {
        boolean changed = (this.expanded != expand);

        if (!changed && !animate) {
            microToolbar.setOpacity(expand ? 1.0 : 0.85);
            return;
        }

        cancelCurrentAnim();

        if (collapsedWidth < 0 || expandedWidth < 0) {
            computeWidths();
        }

        double startW = microToolbar.getWidth() > 0 ? microToolbar.getWidth() : microToolbar.prefWidth(-1);
        double targetW = expand ? expandedWidth : collapsedWidth;

        FadeTransition shellFade = new FadeTransition(Duration.millis(150), microToolbar);
        shellFade.setToValue(expand ? 1.0 : 0.85);

        ParallelTransition itemsFade = new ParallelTransition();

        if (expand) {
            showExtrasImmediate(true);
            microToolbar.applyCss();
            microToolbar.layout();

            FadeTransition ft1 = new FadeTransition(Duration.millis(120), mtCopy);
            ft1.setFromValue(0.0);
            ft1.setToValue(1.0);
            FadeTransition ft2 = new FadeTransition(Duration.millis(120), mtSave);
            ft2.setFromValue(0.0);
            ft2.setToValue(1.0);
            FadeTransition ft3 = new FadeTransition(Duration.millis(120), mtOptions);
            ft3.setFromValue(0.0);
            ft3.setToValue(1.0);
            itemsFade.getChildren().addAll(ft1, ft2, ft3);
        } else {
            FadeTransition ft1 = new FadeTransition(Duration.millis(100), mtCopy);
            ft1.setToValue(0.0);
            FadeTransition ft2 = new FadeTransition(Duration.millis(100), mtSave);
            ft2.setToValue(0.0);
            FadeTransition ft3 = new FadeTransition(Duration.millis(100), mtOptions);
            ft3.setToValue(0.0);
            itemsFade.getChildren().addAll(ft1, ft2, ft3);
        }

        SimpleDoubleProperty pw = new SimpleDoubleProperty(startW);
        pw.addListener((o, ov, nv) -> microToolbar.setPrefWidth(nv.doubleValue()));
        Timeline wtl = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(pw, startW)),
            new KeyFrame(Duration.millis(150), new KeyValue(pw, targetW))
        );

        currentAnim = new ParallelTransition(shellFade, wtl, itemsFade);
        currentAnim.setOnFinished(ev -> {
            if (!expand) {
                showExtrasImmediate(false);
            }
            microToolbar.setPrefWidth(targetW);
            currentAnim = null;
        });
        currentAnim.play();

        this.expanded = expand;
    }

    // ---------- Options menu ----------

    private void ensureMenu() {
        if (optionsMenu != null) return;

        CheckMenuItem include = new CheckMenuItem("Include window frame");
        include.setSelected(includeChrome);
        include.selectedProperty().addListener((o, ov, nv) -> includeChrome = nv);

        CheckMenuItem transparent = new CheckMenuItem("Transparent background (PNG)");
        transparent.setSelected(transparentPng);
        transparent.selectedProperty().addListener((o, ov, nv) -> transparentPng = nv);

        Menu scale = new Menu("Scale");
        ToggleGroup tg = new ToggleGroup();
        RadioMenuItem s1 = new RadioMenuItem("1×");
        s1.setToggleGroup(tg);
        RadioMenuItem s2 = new RadioMenuItem("2×");
        s2.setToggleGroup(tg);
        RadioMenuItem s3 = new RadioMenuItem("3×");
        s3.setToggleGroup(tg);

        switch (exportScale) {
            case 1:
                s1.setSelected(true);
                break;
            case 3:
                s3.setSelected(true);
                break;
            default:
                s2.setSelected(true);
        }

        s1.setOnAction(e -> exportScale = 1);
        s2.setOnAction(e -> exportScale = 2);
        s3.setOnAction(e -> exportScale = 3);
        scale.getItems().addAll(s1, s2, s3);

        MenuItem copyNow = new MenuItem("Copy snapshot");
        copyNow.setOnAction(e -> copySnapshotToClipboard());
        MenuItem saveNow = new MenuItem("Save snapshot…");
        saveNow.setOnAction(e -> saveSnapshotToFile());

        optionsMenu = new ContextMenu(copyNow, saveNow, new SeparatorMenuItem(), include, transparent, scale);
    }

    // ---------- Snapshot actions ----------

    private void copySnapshotToClipboard() {
        WritableImage wi = snapshot(includeChrome ? chromeNode : contentNode, exportScale, transparentPng);
        ClipboardContent cc = new ClipboardContent();
        cc.putImage(wi);
        Clipboard.getSystemClipboard().setContent(cc);
        toast("Snapshot copied to clipboard");
    }

    private void saveSnapshotToFile() {
        WritableImage wi = snapshot(includeChrome ? chromeNode : contentNode, exportScale, transparentPng);
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fc.setInitialFileName("snapshot_content-" + exportScale + "x.png");
        File f = fc.showSaveDialog(scene.getWindow());
        if (f == null) return;
        try {
            BufferedImage bi = SwingFXUtils.fromFXImage(wi, null);
            ImageIO.write(bi, "png", f);
            toast("Saved: " + f.getName());
        } catch (IOException ex) {
            LOG.error("Failed to save snapshot", ex);
            toast("Failed to save snapshot");
        }
    }

    private WritableImage snapshot(Node target, int scale, boolean transparent) {
        // Ensure CSS/layout are current without assuming Node#layout()
        target.applyCss();
        if (target instanceof Parent) {
            ((Parent) target).layout();
        } else if (target.getParent() != null) {
            target.getParent().applyCss();
            if (target.getParent() instanceof Parent) {
                ((Parent) target.getParent()).layout();
            }
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(Transform.scale(scale, scale));
        Paint fill = transparent ? Color.TRANSPARENT : Color.WHITE;
        params.setFill(fill);

        Bounds b = target.getLayoutBounds();
        int w = Math.max(1, (int) Math.ceil(b.getWidth() * scale));
        int h = Math.max(1, (int) Math.ceil(b.getHeight() * scale));

        return target.snapshot(params, new WritableImage(w, h));
    }

    private void toast(String msg) {
        Platform.runLater(() -> {
            Window owner = (scene != null) ? scene.getWindow() : null;
            if (owner == null || microToolbar == null || microToolbar.getScene() == null) return;

            // Build toast node (no inline CSS)
            Label toast = new Label(msg);
            toast.setTextFill(Color.WHITE);
            toast.setBackground(new Background(new BackgroundFill(
                Color.rgb(0, 0, 0, 0.75), new CornerRadii(6), Insets.EMPTY)));
            toast.setPadding(new Insets(6, 10, 6, 10));
            toast.setMouseTransparent(true);
            toast.setOpacity(0.0); // start transparent for fade-in

            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(false);
            popup.setHideOnEscape(false);
            popup.getContent().add(toast);

            // Position: right-aligned under the microToolbar, in screen coords
            Bounds tb = microToolbar.localToScreen(microToolbar.getBoundsInLocal());
            double px = (tb != null) ? tb.getMaxX() : owner.getX() + owner.getWidth() - 24.0;
            double py = (tb != null) ? tb.getMaxY() + 6.0 : owner.getY() + 48.0;

            // Show first, then measure and adjust for exact width to right-align
            popup.show(owner, px, py);
            toast.applyCss();
            toast.layout();
            double w = toast.prefWidth(-1);
            popup.setX(px - w); // right-align under the toolbar

            // Fade in → hold → fade out, then hide
            FadeTransition in = new FadeTransition(Duration.millis(150), toast);
            in.setFromValue(0.0);
            in.setToValue(1.0);

            PauseTransition hold = new PauseTransition(Duration.millis(600));

            FadeTransition out = new FadeTransition(Duration.millis(300), toast);
            out.setFromValue(1.0);
            out.setToValue(0.0);
            out.setOnFinished(e -> popup.hide());

            new SequentialTransition(in, hold, out).play();
        });
    }
}
