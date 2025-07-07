package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.javafx.components.radial.CircleProgressIndicator;
import java.util.HashMap;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Sean Phillips
 */
public abstract class HyperdriveTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(HyperdriveTask.class);
    //these refereces make this a tad hacky but...
    Scene scene;
    CircleProgressIndicator progressIndicator = null;
    AtomicInteger requestNumber;
    HashMap<Integer, REQUEST_STATUS> outstandingRequests;
    private boolean cancelledByUser = false;
    private int batchSize = 1; //default one at a time
    private long requestDelay = 25; //default 25ms between requests
    
    public enum REQUEST_STATUS { REQUESTED, SUCCEEDED, FAILED }

    public HyperdriveTask(Scene scene, CircleProgressIndicator progressIndicator,
        AtomicInteger requestNumber, HashMap<Integer, REQUEST_STATUS> outstandingRequests) {
        this.scene = scene;
        this.progressIndicator = progressIndicator;
        this.requestNumber = requestNumber;
        this.outstandingRequests = outstandingRequests;

//        setOnSucceeded(e -> {
//            Platform.runLater(() -> {
//                scene.getRoot().fireEvent(
//                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
//            });
//        });
//        setOnFailed(e -> {
//            Platform.runLater(() -> {
//                scene.getRoot().fireEvent(
//                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
//            });
//        });
//        setOnCancelled(e -> {
//            Platform.runLater(() -> {
//                scene.getRoot().fireEvent(
//                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
//            });
//        });

    }

    protected abstract void processTask() throws Exception;

    @Override
    protected Void call() throws Exception {
        if (isCancelled()) return null;
        processTask();
        return null;
    }

    /**
     * @return the cancelledByUser
     */
    public boolean isCancelledByUser() {
        return cancelledByUser;
    }

    /**
     * @param cancelledByUser the cancelledByUser to set
     */
    public void setCancelledByUser(boolean cancelledByUser) {
        this.cancelledByUser = cancelledByUser;
    }

    /**
     * @return the batchSize
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * @param batchSize the batchSize to set
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * @return the requestDelay
     */
    public long getRequestDelay() {
        return requestDelay;
    }

    /**
     * @param requestDelay the requestDelay to set
     */
    public void setRequestDelay(long requestDelay) {
        this.requestDelay = requestDelay;
    }

}
