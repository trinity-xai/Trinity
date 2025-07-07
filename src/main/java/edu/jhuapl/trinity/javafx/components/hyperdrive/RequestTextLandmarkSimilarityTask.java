package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import static edu.jhuapl.trinity.data.messages.xai.FeatureVector.mapToStateArray;
import edu.jhuapl.trinity.javafx.components.listviews.EmbeddingsTextListItem;
import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import edu.jhuapl.trinity.utils.metric.Metric;
import java.util.List;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Sean Phillips
 */
public class RequestTextLandmarkSimilarityTask extends HyperdriveTask {
    private static final Logger LOG = LoggerFactory.getLogger(RequestTextLandmarkSimilarityTask.class);
    List<EmbeddingsTextListItem> items;
    List<FeatureVector> landmarkFeatures;
    Metric metric;
    
    public RequestTextLandmarkSimilarityTask(Scene scene, CircleProgressIndicator progressIndicator, 
        List<EmbeddingsTextListItem> items, List<FeatureVector> landmarkFeatures, Metric metric) {
        super(scene, progressIndicator, null, null);
        this.items = items;
        this.landmarkFeatures = landmarkFeatures;
        this.metric = metric;
    }

    @Override
    protected void processTask() throws Exception {
        if(null != progressIndicator) {
            progressIndicator.setFadeTimeMS(250);
            progressIndicator.setLabelLater("Computing Landmark Similarity Distances...");
            progressIndicator.spin(true);
            progressIndicator.fadeBusy(false);
        }
        final double total = items.size();
        int currentIndex = 0;
        List<double[]> landmarkVectors = landmarkFeatures.stream()
            .map(mapToStateArray).toList();
        for (EmbeddingsTextListItem item : items) {
            double[] itemVector = mapToStateArray.apply(item.getFeatureVector());
            Double shortestDistance = null;
            Integer shortestLandmarkIndex = null;
            for (int i = 0; i < landmarkVectors.size(); i++) {
                double currentDistance = metric.distance(itemVector, landmarkVectors.get(i));
                //System.out.println(i + " : " + currentDistance);
                if (null == shortestDistance || currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                    shortestLandmarkIndex = i;
                }
            }
            item.setFeatureVectorLabel(
                landmarkFeatures.get(shortestLandmarkIndex).getLabel());
            currentIndex++;    
            double completed = Integer.valueOf(currentIndex).doubleValue();
            if(null != progressIndicator) {            
                progressIndicator.setPercentComplete(completed / total);
                progressIndicator.setLabelLater("Computed " + currentIndex + " of " + total);
            }              
        }
        if(null != progressIndicator) {
            progressIndicator.setLabelLater("Complete");
            progressIndicator.spin(false);
            progressIndicator.fadeBusy(true);
        }        
    }
}
