package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.messages.bci.SemanticMap;
import edu.jhuapl.trinity.data.messages.bci.SemanticMapCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.GaussianMixture;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.components.timeline.Item;
import edu.jhuapl.trinity.javafx.events.SemanticMapEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.javafx.renderers.SemanticMapRenderer;
import edu.jhuapl.trinity.utils.DataUtils;
import javafx.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class SemanticMapEventHandler implements EventHandler<SemanticMapEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SemanticMapEventHandler.class);

    public boolean pcaEnabled;
    List<SemanticMapRenderer> renderers;
    List<FeatureVectorRenderer> fvRenderers;
    ColorMap colorMap;
    int colorIndex = 0;
    int colorCount = 0;

    public SemanticMapEventHandler(boolean pcaEnabled) {
        this.pcaEnabled = pcaEnabled;
        renderers = new ArrayList<>();
        fvRenderers = new ArrayList<>();
        colorMap = ColorMap.jet();
        colorCount = colorMap.getFixedColorCount();
    }

    public void addSemanticMapRenderer(SemanticMapRenderer renderer) {
        renderers.add(renderer);
    }

    public void addFeatureVectorRenderer(FeatureVectorRenderer renderer) {
        fvRenderers.add(renderer);
    }

    public void handleSemanticMapEvent(SemanticMapEvent event) {
        SemanticMap semanticMap = (SemanticMap) event.object;
//        if(event.getEventType().equals(SemanticMapEvent.LOCATE_FEATURE_VECTOR)) {
//            for(SemanticMapRenderer renderer : renderers) {
//                renderer.locateSemanticMap(semanticMap);
//            }
//        }
        if (event.getEventType().equals(SemanticMapEvent.NEW_SEMANTIC_MAP)) {
            for (SemanticMapRenderer renderer : renderers) {
                renderer.addSemanticMap(semanticMap);
            }
        }
    }

    public void handleSemanticMapCollectionEvent(SemanticMapEvent event) {
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_CLEAR_ITEMS));
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_SET_VISIBLE, true));
        SemanticMapCollection semanticMapCollection = (SemanticMapCollection) event.object;
        LOG.info("Semantic Space data is {} x {}", semanticMapCollection.getSemantic_space().getData().size(), semanticMapCollection.getSemantic_space().getData().get(0).size());
        LOG.info("Reconstruction data is {} x {}", semanticMapCollection.getReconstruction().getData_vars()
            .getPrediction().getData().size(), semanticMapCollection.getReconstruction().getData_vars()
            .getPrediction().getData().get(0).size());

        GaussianMixture gm = DataUtils.convertSemanticSpace(
            semanticMapCollection.getSemantic_space(), 10.0);
        LOG.info("GaussianMixture.getData().size(): {}", gm.getData().size());

        //20220825T1416
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm");
        LocalDateTime startTime = LocalDateTime.parse(
            semanticMapCollection.getReconstruction().getAttrs().getRec(), formatter);
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_SET_INITIALTIME, startTime));
        List<FeatureVector> featureVectors = DataUtils.convertSemanticReconstruction(
            semanticMapCollection.getReconstruction());
        //get framerate and set the timelineanimation with it
        //@TODO SMP In the future convert to milliseconds
        int sampleFramerate = semanticMapCollection.getReconstruction()
            .getAttrs().getFramerate().intValue(); //for now truncate to seconds
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_SET_SAMPLEFRAMERATE, sampleFramerate));

        List<String> labelStrings = semanticMapCollection.getSemantic_space()
            .getCoords().getFeature_desc().getData();
        LOG.info("FeatureVectors.size(): {}", featureVectors.size());
        FeatureCollection fc = new FeatureCollection();
        fc.setFeatures(featureVectors);
        fc.setType(FeatureCollection.TYPESTRING);
        //update labels
        List<FactorLabel> newlabels = new ArrayList<>();
        fc.getFeatures().forEach(featureVector -> {
            //Have we seen this label before?
            //FactorLabel matchingLabel = FactorLabel.getFactorLabel(featureVector.getLabel());
            boolean labelExists = newlabels.stream().anyMatch((FactorLabel t) -> featureVector.getLabel().contentEquals(t.getLabel()));
            //The label is new... add a new FactorLabel row
            if (!labelExists) {
                if (colorIndex > colorCount) {
                    colorIndex = 0;
                }
                FactorLabel fl = new FactorLabel(featureVector.getLabel(),
                    colorMap.getColorByIndex(colorIndex));
                newlabels.add(fl);
                colorIndex++;
            }
        });
        Trajectory trajectory = new Trajectory("SemanticMap " + semanticMapCollection.getReconstruction().getAttrs().getRec());
        trajectory.totalStates = semanticMapCollection.getReconstruction().getData_vars().getNeural_timeseries().getData().size();
        Trajectory.addTrajectory(trajectory);
        Trajectory.globalTrajectoryToFeatureCollectionMap.put(trajectory, fc);
        App.getAppScene().getRoot().fireEvent(
            new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory, fc));

        LOG.info("Total New Factor Labels: {}", newlabels.size());
        FactorLabel.addAllFactorLabels(newlabels);

        for (FeatureVectorRenderer renderer : fvRenderers) {
            renderer.setDimensionLabels(labelStrings);
            renderer.addFeatureCollection(fc, false);
            renderer.setSpheroidAnchor(true, 0);
        }

        List<Item> timelineItems = DataUtils.extractReconstructionEvents(
            semanticMapCollection.getReconstruction().getAttrs());
        timelineItems.add(0, new Item(0, "Start"));
        App.getAppScene().getRoot().fireEvent(
            new TimelineEvent(TimelineEvent.TIMELINE_ADD_ITEMS, timelineItems));


        for (SemanticMapRenderer renderer : renderers) {
            renderer.addSemanticMapCollection(semanticMapCollection);
            //@TODO SMP Need to convert to a centralized shared FeatureVector source
            //this is a hack but it gets the labeled features into the view
            renderer.setFeatureCollection(fc);
        }
    }

    @Override
    public void handle(SemanticMapEvent event) {
        if (event.getEventType().equals(SemanticMapEvent.NEW_SEMANTIC_MAP))
            handleSemanticMapEvent(event);
        else if (event.getEventType().equals(SemanticMapEvent.NEW_SEMANTICMAP_COLLECTION))
            handleSemanticMapCollectionEvent(event);
    }
}
