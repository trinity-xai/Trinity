package edu.jhuapl.trinity.messages;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.messages.llm.AliveModels;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.RestEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;

/**
 * @author Sean Phillips
 */
public class IsAliveCallback extends RestConsumer {

    public IsAliveCallback(Scene scene) {
        super(scene);
    }

    @Override
    public void onFailure() {
        Platform.runLater(() -> {
            CommandTerminalEvent cte = new CommandTerminalEvent(
                "Embedding service could not be reached: ", // + response.code() + " - " + response.message(),
                new Font("Consolas", 20), Color.RED);
            scene.getRoot().fireEvent(cte);
            Alert alert = new Alert(Alert.AlertType.WARNING,
                "Functionality will be limited without Embedding Service access.",
                ButtonType.OK);
            alert.setTitle("Embedding Service Failure");
            alert.setHeaderText("Is Alive check for LLM Service failed");
            alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
            alert.initStyle(StageStyle.TRANSPARENT);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setBackground(Background.EMPTY);
            dialogPane.getScene().setFill(Color.TRANSPARENT);
            String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
            dialogPane.getStylesheets().add(DIALOGCSS);
            alert.showAndWait();
        });
    }

    @Override
    protected void processResponse(String responseBodyString) {
        LOG.info("IsAliveConsumer works: \n {}", responseBodyString);
        try {
            AliveModels isAliveReesponse = objectMapper.readValue(responseBodyString, AliveModels.class);
            Platform.runLater(() -> {
                CommandTerminalEvent cte = new CommandTerminalEvent(
                    "Embedding Service is alive.",
                    new Font("Consolas", 20), Color.LIME);
                scene.getRoot().fireEvent(cte);
                scene.getRoot().fireEvent(
                    new RestEvent(RestEvent.EMBEDDING_MODELS_ALIVE, isAliveReesponse));
            });
        } catch (Exception ex) {
            LOG.error(null, ex);
        }
    }
}
