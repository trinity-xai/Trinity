package edu.jhuapl.trinity.javafx.controllers;

import edu.jhuapl.trinity.utils.Configuration;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import lit.litfx.controls.output.LitLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Sean Phillips
 */
public class AboutController implements Initializable {
    private static final Logger LOG = LoggerFactory.getLogger(AboutController.class);

    @FXML
    public TextField titleTextField;
    @FXML
    public TextField versionTextField;
    @FXML
    public TextField buildDateTextField;
    @FXML
    public HBox aboutHBox;
    public LitLog litLog;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        litLog = new LitLog();
        aboutHBox.getChildren().add(litLog);
        litLog.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        litLog.vbox.setMaxWidth(450);
        litLog.setMinWidth(500);
        litLog.setMinHeight(500);
        convertMarkdown();

        Properties buildProps = Configuration.getBuildProps();
        String title = (String) buildProps.getOrDefault("title", "UNKNOWN");
        String version = (String) buildProps.getOrDefault("version", "UNKNOWN");
        String revision = (String) buildProps.getOrDefault("revision", "UNKNOWN");
        String buildDate = (String) buildProps.getOrDefault("timestamp", "UNKNOWN");
        titleTextField.setText(title);
        versionTextField.setText(version);
        buildDateTextField.setText(buildDate);
    }

    public void convertMarkdown() {
        try (InputStream resource = AboutController.class.getResourceAsStream("/edu/jhuapl/trinity/wiki/about.md")) {
            String text =
                new BufferedReader(new InputStreamReader(resource,
                    StandardCharsets.UTF_8)).lines()
                    //.collect(Collectors.toList());
                    .reduce("", (t, u) -> t + u + System.lineSeparator());
            litLog.addLine(text);
        } catch (IOException ex) {
            LOG.error(null, ex);
            String aboutMarkdown = "Unable to load about markdown. Check Exception log.";
            litLog.addLine(aboutMarkdown);
        }
    }
}
