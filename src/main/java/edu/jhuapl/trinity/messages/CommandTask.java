package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.data.messages.CommandRequest;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;

import java.time.Duration;

/**
 * @author Sean Phillips
 */
public class CommandTask extends Task {

    double delaySeconds = 0.0;
    String command;
    Scene scene;

    public CommandTask(CommandRequest commandRequest, Scene scene) {
        this.scene = scene;
        delaySeconds = commandRequest.getDelaySeconds();
        command = commandRequest.getRequest();
    }
    public static void execute(Scene scene, String command, double delaySeconds) throws InterruptedException {
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
        }
    }

    @Override
    protected Object call() throws Exception {
        execute(scene, command, delaySeconds);
        return null;
    }

}
