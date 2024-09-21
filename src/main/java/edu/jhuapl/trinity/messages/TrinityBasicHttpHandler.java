package edu.jhuapl.trinity.messages;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class TrinityBasicHttpHandler implements HttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrinityBasicHttpHandler.class);

    private final MessageProcessor processor;

    public TrinityBasicHttpHandler(Scene scene) {
        this.processor = new MessageProcessor(scene);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Check HTTP Method
        if (!(exchange.getRequestMethod().equals("POST"))) {
            handleInvalidMethodResponse(exchange);
            return;
        }

        // Process Request
        String rawContent = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        boolean success = processMessage(rawContent);

        // Generate the response
        if (success) {
            handleOkResponse(exchange);
        } else {
            handleErrorResponse(exchange, "Malformed JSON");
        }
    }

    private void handleInvalidMethodResponse(HttpExchange exchange) throws IOException {
        String response = "{\"error\":\"Not Found\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(404, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private void handleErrorResponse(HttpExchange exchange, String errorMsg) throws IOException {
        String response = String.format("{\"error\":\"%s\"}", errorMsg);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(400, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private void handleOkResponse(HttpExchange exchange) throws IOException {
        String response = "{\"message\":\"OK\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    public boolean processMessage(String messageBody) {
        try {
            processor.process(messageBody);
            return true;
        } catch (IOException ex) {
            LOGGER.info("Malformed JSON from injectMessage", ex);
            return false;
        }
    }
}
