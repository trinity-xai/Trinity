package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.javafx.components.listviews.PointListItem;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static edu.jhuapl.trinity.utils.JavaFX3DUtils.toFX;
import static edu.jhuapl.trinity.utils.JavaFX3DUtils.toFXYZ3D;
import edu.jhuapl.trinity.utils.clustering.ClusterMethod;
import static java.util.stream.Collectors.toList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.ToggleGroup;

/**
 * @author Sean Phillips
 */
public class Shape3DControlPane extends LitPathPane {
    public static double SPINNER_PREF_WIDTH = 150;
    public static double SELECTION_PREF_WIDTH = 250;
    BorderPane bp;
    TabPane tabPane;
    Tab editorTab;
    Tab clusterBuilderTab;
    Tab findClustersTab;
    private Slider scaleSlider;
    private Slider rotateXSlider;
    private Slider rotateYSlider;
    private Slider rotateZSlider;
    private Label scaleLabel;
    private Label rotateXLabel;
    private Label rotateYLabel;
    private Label rotateZLabel;
    private ListView<PointListItem> pointListView;
    private ChoiceBox labelChoiceBox;
    private Spinner iterationsSpinner;
    private Spinner convergenceSpinner;
    private Spinner componentsSpinner;
    private Spinner epsilonAlphaSpinner;
    private Spinner minimumPointsSpinner;
    private Spinner minimumClusterSizeSpinner;
    private Spinner minimumLeafSizeSpinner;
    private CheckBox parallelCheckBox;
    private CheckBox verboseCheckBox;
    private RadioButton diagonalRadioButton;
    private RadioButton fullCovarianceRadioButton;
    private SplitMenuButton clusterMethodMenuButton;    
    private RadioButton hypersurfaceRadioButton;
    private RadioButton projectionsRadioButton;

    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00");
    private Manifold3D manifold3D = null;
    private Manifold currentManifold = null;
    private final String ALL = "ALL";
    public static Color DEFAULT_MANIFOLD_COLOR = Color.WHITE;
    ClusterMethod selectedMethod = ClusterMethod.KMEANS;
    
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public Shape3DControlPane(Scene scene, Pane parent) {
        super(scene, parent, 600, 400, createContent(),
            "Geometry Controls", "", 300.0, 400.0);
        this.scene = scene;

        bp = (BorderPane) this.contentPane;
        buildEditorTab();
        buildFindClustersTab();
        buildClusterBuilderTab();
        tabPane = new TabPane(clusterBuilderTab, findClustersTab, editorTab);
        tabPane.setPadding(Insets.EMPTY);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.FIXED);
        bp.setCenter(tabPane);
    }
    private void buildFindClustersTab() {
        findClustersTab = new Tab("Find Clusters");
        BorderPane findClusterBorderPane = new BorderPane();
        findClustersTab.setContent(findClusterBorderPane);        
        
        componentsSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 20, 5, 1));
        componentsSpinner.setPrefWidth(SPINNER_PREF_WIDTH);        
        componentsSpinner.setEditable(true);
        iterationsSpinner = new Spinner(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 500, 50, 5));
        iterationsSpinner.setPrefWidth(SPINNER_PREF_WIDTH);        
        iterationsSpinner.setEditable(true);
        convergenceSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory( 0.001, 0.999,  0.005, 0.001));
        convergenceSpinner.setPrefWidth(SPINNER_PREF_WIDTH);
        convergenceSpinner.setEditable(true);

        epsilonAlphaSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 1, 0.5, 0.01));
        epsilonAlphaSpinner.setPrefWidth(SPINNER_PREF_WIDTH);
        epsilonAlphaSpinner.setEditable(true);
        
        minimumPointsSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 1000, 10, 10));
        minimumPointsSpinner.setPrefWidth(SPINNER_PREF_WIDTH);
        minimumPointsSpinner.setEditable(true);

        minimumClusterSizeSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 1000, 100, 50));
        minimumClusterSizeSpinner.setPrefWidth(SPINNER_PREF_WIDTH);
        minimumClusterSizeSpinner.setEditable(true);
        
        minimumLeafSizeSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 1000, 100, 50));
        minimumLeafSizeSpinner.setPrefWidth(SPINNER_PREF_WIDTH);
        minimumLeafSizeSpinner.setEditable(true);
        
        parallelCheckBox = new CheckBox("Force Parallel");
        verboseCheckBox = new CheckBox("Verbose Output");
        
        ToggleGroup covarianceToggleGroup = new ToggleGroup();
        diagonalRadioButton = new RadioButton("Diagonal");
        diagonalRadioButton.setToggleGroup(covarianceToggleGroup);
        fullCovarianceRadioButton = new RadioButton("Full");
        fullCovarianceRadioButton.setToggleGroup(covarianceToggleGroup);
        diagonalRadioButton.setSelected(true);
        
        ToggleGroup datasourceToggleGroup = new ToggleGroup();
        hypersurfaceRadioButton = new RadioButton("Hypersurface");
        hypersurfaceRadioButton.setToggleGroup(datasourceToggleGroup);
        projectionsRadioButton = new RadioButton("Projections");
        projectionsRadioButton.setToggleGroup(datasourceToggleGroup);
        projectionsRadioButton.setSelected(true);

        MenuItem dbscan = new MenuItem("DBSCAN");
        MenuItem hdbscan = new MenuItem("HDBSCAN");
        MenuItem kmeans = new MenuItem("KMeans");
        MenuItem kmedoids = new MenuItem("KMedoids");
        MenuItem exmax = new MenuItem("Expectation Maximization");
        MenuItem affinity = new MenuItem("Affinity Propagation");
        clusterMethodMenuButton = new SplitMenuButton(dbscan, hdbscan, 
            kmeans, kmedoids, exmax, affinity);  
        clusterMethodMenuButton.setText("Select method");
        clusterMethodMenuButton.setOnAction(e->findClusters());

        dbscan.setOnAction((e) -> {
            selectedMethod = ClusterMethod.DBSCAN;
            clusterMethodMenuButton.setText(dbscan.getText());
        });
        hdbscan.setOnAction((e) -> {
            selectedMethod = ClusterMethod.HDDBSCAN;
            clusterMethodMenuButton.setText(hdbscan.getText());
        });
        kmeans.setOnAction((e) -> {
            selectedMethod = ClusterMethod.KMEANS;
            clusterMethodMenuButton.setText(kmeans.getText());
        });
        kmedoids.setOnAction((e) -> {
            selectedMethod = ClusterMethod.KMEDIODS;
            clusterMethodMenuButton.setText(kmedoids.getText());
        });
        exmax.setOnAction((e) -> {
            selectedMethod = ClusterMethod.EX_MAX;
            clusterMethodMenuButton.setText(exmax.getText());
        });
        affinity.setOnAction((e) -> {
            selectedMethod = ClusterMethod.AFFINITY;
            clusterMethodMenuButton.setText(affinity.getText());
        });
        clusterMethodMenuButton.setPrefWidth(SELECTION_PREF_WIDTH);
        
        Label sourceLabel = new Label("Data Source");
        sourceLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label componentsLabel = new Label("Components");
        componentsLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label iterationsLabel = new Label("Iterations");
        iterationsLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label convergenceLabel = new Label("Convergence");
        convergenceLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label covarianceLabel = new Label("Covariance");
        covarianceLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label epsilonAlphaLabel = new Label("Epsilon/Alpha");
        epsilonAlphaLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label minimumPointsLabel = new Label("Minimum Points");
        minimumPointsLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label minimumClusterSizeLabel = new Label("Minimum Cluster Size");
        minimumClusterSizeLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label minimumLeafSizeLabel = new Label("Minimum Leaf Size");
        minimumLeafSizeLabel.setPrefWidth(SPINNER_PREF_WIDTH);
        Label clusteringLabel = new Label("Clustering Method");        
        clusteringLabel.setPrefWidth(SPINNER_PREF_WIDTH);

        VBox vbox = new VBox(10,
            new HBox(10, sourceLabel, hypersurfaceRadioButton, projectionsRadioButton),
            new HBox(10, clusteringLabel, clusterMethodMenuButton),
            new HBox(10, componentsLabel, componentsSpinner),
            new HBox(10, iterationsLabel, iterationsSpinner),
            new HBox(10, convergenceLabel, convergenceSpinner),
            new HBox(10, epsilonAlphaLabel, epsilonAlphaSpinner),    
            new HBox(10, minimumPointsLabel, minimumPointsSpinner),    
            new HBox(10, minimumClusterSizeLabel, minimumClusterSizeSpinner),    
            new HBox(10, minimumLeafSizeLabel, minimumLeafSizeSpinner),    
            new HBox(10, parallelCheckBox, verboseCheckBox),    
            new HBox(10, covarianceLabel, diagonalRadioButton, fullCovarianceRadioButton)
        );
        vbox.setPadding(new Insets(10,5,5,5));
        findClusterBorderPane.setCenter(vbox);
    }

    public void findClusters() {
        System.out.println("Find Clusters...");
        ProjectionConfig pc = new ProjectionConfig();
        pc.dataSource = hypersurfaceRadioButton.isSelected() 
            ? ProjectionConfig.DATA_SOURCE.HYPERSURFACE
            : ProjectionConfig.DATA_SOURCE.PROJECTIONS;
        pc.components = (int) componentsSpinner.getValue();
        pc.clusterMethod = selectedMethod;
//        pc.useVisiblePoints = useVisibleRadioButton.isSelected();
        pc.useVisiblePoints = true;
        pc.covariance = diagonalRadioButton.isSelected()
            ? ProjectionConfig.COVARIANCE_MODE.DIAGONAL
            : ProjectionConfig.COVARIANCE_MODE.FULL;
        pc.toleranceConvergence = (double) convergenceSpinner.getValue();
        pc.maxIterations = (int) iterationsSpinner.getValue();
        pc.epsilonAlpha = (double) epsilonAlphaSpinner.getValue();
        pc.minimumPoints = (int) minimumPointsSpinner.getValue();
        pc.minimumClusterSize = (int) minimumClusterSizeSpinner.getValue();
        pc.minimumLeafSize = (int) minimumLeafSizeSpinner.getValue();
        
        if(hypersurfaceRadioButton.isSelected())
            clusterMethodMenuButton.getScene().getRoot().fireEvent(
            new ManifoldEvent(ManifoldEvent.FIND_HYPERSURFACE_CLUSTERS, pc));
        else
            clusterMethodMenuButton.getScene().getRoot().fireEvent(
            new ManifoldEvent(ManifoldEvent.FIND_PROJECTION_CLUSTERS, pc));
    }    
    
    private void buildClusterBuilderTab() {
        clusterBuilderTab = new Tab("Cluster Builder");
        BorderPane clusterBorderPane = new BorderPane();
        clusterBuilderTab.setContent(clusterBorderPane);

        ToggleButton activate = new ToggleButton("Activate Selection");
        activate.setOnAction(e -> {
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.CLUSTER_SELECTION_MODE, activate.isSelected()));
        });

        Button refreshOctree = new Button("Refresh Octree");
        refreshOctree.setOnAction(e -> refreshOctree());
        Button startManifold = new Button("New Manifold");
        startManifold.setOnAction(e -> startNewManifold());
        TextField pointCount = new TextField("0");
        pointCount.setEditable(false);
        pointCount.setPrefWidth(100);
        Spinner searchRange = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 1000.0, 10.0, 1.0));
        searchRange.setEditable(true);
        searchRange.setPrefWidth(100);
        searchRange.valueProperty().addListener(e -> {
            //do something
        });
        Spinner nearestNeighbors = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5000, 10, 5));
        nearestNeighbors.setEditable(true);
        nearestNeighbors.setPrefWidth(100);
        nearestNeighbors.valueProperty().addListener(e -> {
            //do something
        });
        CheckBox filterVisible = new CheckBox("Filter Visible");
        filterVisible.setSelected(true);
        CheckBox filterLabel = new CheckBox("Filter Label");
        filterLabel.setSelected(false);
        labelChoiceBox = new ChoiceBox();
        labelChoiceBox.disableProperty().bind(filterLabel.selectedProperty().not());
        getCurrentLabels();
        labelChoiceBox.getSelectionModel().selectFirst();
        labelChoiceBox.setOnShown(e -> getCurrentLabels());
        labelChoiceBox.setPrefWidth(150);
        VBox controlsVBox = new VBox(10, new Label("Settings"),
            new HBox(5, refreshOctree, startManifold),
            new HBox(5, new Label("Accumulated Points"), pointCount),

            new HBox(5, filterVisible, filterLabel),
            labelChoiceBox,
            new HBox(5, new Label("Search Range"), searchRange),
            new HBox(5, new Label("Nearest Neighbors"), nearestNeighbors),
            activate
        );
        controlsVBox.setPadding(new Insets(5));
        clusterBorderPane.setCenter(controlsVBox);

        scene.addEventHandler(ManifoldEvent.ADD_CLUSTER_SELECTION, e -> {
            List<Point3D> points = (List<Point3D>) e.object1;
            List<PointListItem> newItems = new ArrayList<>();
            //Do we have an existing Manifold started yet?
            if (null == currentManifold) {
                //initialize a new object to begin adding points to
                currentManifold = new Manifold(new ArrayList<>(), "New Label", "New Manifold Name", DEFAULT_MANIFOLD_COLOR);
            }
            //if we don't have any points yet just add them all
            if (pointListView.getItems().isEmpty()) {
                for (Point3D p3D : points) {
                    currentManifold.getPoints().add(p3D);
                    newItems.add(new PointListItem(currentManifold, toFXYZ3D.apply(p3D), true));
                }
            } else {
                for (int i = 0; i < pointListView.getItems().size(); i++) {
                    PointListItem item = pointListView.getItems().get(i);
                    //go through all points to be added
                    for (Point3D p3D : points) {
                        //is it already in the current point list?
                        if (JavaFX3DUtils.matches(toFX.apply(item.getPoint3D()), p3D)) {
                            break; //no need to process... already in the list
                        }
                        //if we get this far its not in the list
                        newItems.add(new PointListItem(currentManifold, toFXYZ3D.apply(p3D), true));
                        //update the current Manifold's points
                        currentManifold.getPoints().add(p3D);
                    }
                }
            }
            //update the gui listview with all the new items we created.
            pointListView.getItems().addAll(newItems);
        });
    }

    private void buildEditorTab() {
        editorTab = new Tab("Point Editor");
        BorderPane editorBorderPane = new BorderPane();
        editorTab.setContent(editorBorderPane);
        scaleLabel = new Label("Scale: ");
        scaleSlider = new Slider(0.25, 2, 1.0);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setMajorTickUnit(0.25);
        scaleSlider.setSnapToTicks(true);
        scaleLabel.setText("Scale " + format.format(scaleSlider.getValue()));
        scaleSlider.valueProperty().addListener((ov, t, t1) -> {
            scaleLabel.setText("Scale " + format.format(scaleSlider.getValue()));
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_SCALE, t.doubleValue(), manifold3D.getManifold()));
        });

        rotateXLabel = new Label("Rotate X: ");
        rotateXSlider = new Slider(-180, 180, 0);
        rotateXSlider.setMajorTickUnit(10);
        rotateXSlider.setShowTickMarks(true);
        rotateXSlider.setSnapToTicks(true);
        rotateXLabel.setText("Rotate X: " + format.format(rotateXSlider.getValue()));
        rotateXSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateXLabel.setText("Rotate X: " + format.format(rotateXSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr, manifold3D.getManifold()));
        });

        rotateYLabel = new Label("Rotate Y: ");
        rotateYSlider = new Slider(-180, 180, 0);
        rotateYSlider.setMajorTickUnit(10);
        rotateYSlider.setShowTickMarks(true);
        rotateYSlider.setSnapToTicks(true);
        rotateYLabel.setText("Rotate Y: " + format.format(rotateYSlider.getValue()));
        rotateYSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateYLabel.setText("Rotate Y: " + format.format(rotateYSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr, manifold3D.getManifold()));
        });

        rotateZLabel = new Label("Rotate Z: ");
        rotateZSlider = new Slider(-180, 180, 0);
        rotateZSlider.setMajorTickUnit(10);
        rotateZSlider.setShowTickMarks(true);
        rotateZSlider.setSnapToTicks(true);
        rotateZLabel.setText("Rotate Z: " + format.format(rotateZSlider.getValue()));
        rotateZSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateZLabel.setText("Rotate Z: " + format.format(rotateZSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr, manifold3D.getManifold()));
        });

        Button refresh = new Button("Refresh Hull");
        CheckBox auto = new CheckBox("Auto");
        auto.setSelected(true);
        refresh.setOnAction(e -> updateManifold());
        auto.selectedProperty().addListener(cl -> {
            if (auto.isSelected())
                updateManifold();
        });

        VBox controlsVBox = new VBox(10, new Label("Controls"),
            new HBox(5, refresh, auto),
            scaleLabel, scaleSlider,
            rotateXLabel, rotateXSlider,
            rotateYLabel, rotateYSlider,
            rotateZLabel, rotateZSlider);
        controlsVBox.setPadding(new Insets(5));
        editorBorderPane.setRight(controlsVBox);

        pointListView = new ListView<>();
        ImageView iv = ResourceUtils.loadIcon("point3D", 200);
        VBox placeholder = new VBox(10, iv, new Label("No Shape Point3Ds Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        pointListView.setPlaceholder(placeholder);
        VBox pointVBOX = new VBox(10, new Label("Points"), pointListView);
        pointVBOX.setPrefWidth(350);
        editorBorderPane.setLeft(pointVBOX);
        scene.addEventHandler(ManifoldEvent.TOGGLE_HULL_POINT, e -> {
            if (auto.isSelected()) {
                Manifold eventManifold = (Manifold) e.object1;
                Manifold manifold = manifold3D.getManifold();
                if (null != eventManifold && null != manifold
                    && eventManifold == manifold) {
                    updateManifold();
                }
            }
        });
        scene.addEventHandler(ManifoldEvent.SELECT_PROJECTION_POINT3D, e -> {
            Point3D p3D = (Point3D) e.object1;
            int shortestIndex = 0;
            Double shortestDistance = null;
            for (int i = 0; i < pointListView.getItems().size(); i++) {
                PointListItem item = pointListView.getItems().get(i);
                double currentDistance = p3D.distance(item.getPoint3D().getX(),
                    item.getPoint3D().getY(), item.getPoint3D().getZ());
                if (null == shortestDistance || currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                    shortestIndex = i;
                }
            }
            pointListView.getSelectionModel().select(shortestIndex);
            pointListView.scrollTo(shortestIndex);
        });
    }

    private void getCurrentLabels() {
        labelChoiceBox.getItems().clear();
        labelChoiceBox.getItems().add(ALL);
        labelChoiceBox.getItems().addAll(
            FactorLabel.getFactorLabels().stream()
                .map(f -> f.getLabel()).sorted().toList());
    }

    private void refreshOctree() {
        //Todo request points
//            List<javafx.geometry.Point3D> points =
//                getOriginalPoint3DList().stream().map(fxyzPoint3DTofxPoint3D).toList();
//            List<javafx.geometry.Point3D> points
//            // ****
//            int n = 5; // number of neighbors
//            int i = 6; // if you want to find the nearest neighbors of the ith point.
//            Octree octree = new Octree();
//            octree.buildIndex(points);
//            int[] neighborIndices = octree.searchNearestNeighbors(n, i);
        System.out.println("Octree stuff done.");
    }

    private void startNewManifold() {
        System.out.println("Refreshing Manifold...");
        manifold3D = null;
        pointListView.getItems().clear();
    }

    private void updateManifold() {
        System.out.println("Refreshing Manifold...");
        //build list of points from listview
        List<org.fxyz3d.geometry.Point3D> points = new ArrayList<>();
        pointListView.getItems().forEach(item -> {
            if (item.isSelected())
                points.add(item.getPoint3D());
        });
        manifold3D.refreshMesh(points, false, true, false, null);
    }

    public void setShape3D(Manifold3D manifold3D) {
        this.manifold3D = manifold3D;
        Manifold manifold = manifold3D.getManifold();
        if (null != manifold) {
            pointListView.getItems().clear();
            List<PointListItem> pointListItems = this.manifold3D
                .getOriginalPoint3DList().stream()
                //.map(fxyzPoint3DTofxPoint3D)
                .map((p) -> new PointListItem(manifold, p, true))
                .collect(toList());
            pointListView.getItems().addAll(pointListItems);
        }
    }
}
