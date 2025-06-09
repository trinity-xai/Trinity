package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.CommandRequest;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.SearchEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;

/**
 * @author Sean Phillips
 */
public class CommandTask extends Task {

    private static final Logger LOG = LoggerFactory.getLogger(CommandTask.class);
    double delaySeconds = 0.0;
    String command;
    Scene scene;
    HashMap<String, String> properties;

    public CommandTask(CommandRequest commandRequest, Scene scene) {
        this.scene = scene;
        delaySeconds = commandRequest.getDelaySeconds();
        command = commandRequest.getRequest();
        properties = commandRequest.getProperties();

    }

    public static void execute(Scene scene, String command, double delaySeconds,
                               HashMap<String, String> properties) throws InterruptedException {
        if (delaySeconds > 0) {
            Thread.sleep(Duration.ofMillis(Double.valueOf(delaySeconds * 1000).longValue()));
        }
        if (command.contentEquals(CommandRequest.COMMANDS.VIEW_HYPERSPACE.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_HYPERSPACE));
            });
        } else if (command.contentEquals(CommandRequest.COMMANDS.VIEW_HYPERSURFACE.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_HYPERSURFACE));
            });
        } else if (command.contentEquals(CommandRequest.COMMANDS.VIEW_PROJECTIONS.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_PROJECTIONS));
            });
        } else if (command.contentEquals(CommandRequest.COMMANDS.EXECUTE_UMAP.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ManifoldEvent(ManifoldEvent.GENERATE_NEW_UMAP));
            });
        } else if (command.contentEquals(CommandRequest.COMMANDS.USER_ATTENTION.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new EffectEvent(EffectEvent.OPTICON_USER_ATTENTION));
            });
        } else if (command.contentEquals(CommandRequest.COMMANDS.LASER_SWEEP.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new EffectEvent(EffectEvent.OPTICON_LASER_SWEEP));
            });
        } else if (command.contentEquals(CommandRequest.COMMANDS.FIND.name())) {
            if (null != properties && properties.containsKey(CommandRequest.PAYLOAD)) {
                LOG.info("need to find: {}", properties.get(CommandRequest.PAYLOAD));
                Platform.runLater(() -> {
                    scene.getRoot().fireEvent(new SearchEvent(SearchEvent.FIND_BY_QUERY, properties.get(CommandRequest.PAYLOAD)));
                });
            }
        } else if (command.contentEquals(CommandRequest.COMMANDS.CLEAR_FILTERS.name())) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new SearchEvent(SearchEvent.CLEAR_ALL_FILTERS));
            });
        } else {
            LOG.info("Unknown command received: {}", command);
        }
    }

    @Override
    protected Object call() throws Exception {
        execute(scene, command, delaySeconds, properties);
        return null;
    }

}
