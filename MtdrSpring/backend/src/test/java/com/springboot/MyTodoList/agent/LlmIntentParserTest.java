package com.springboot.MyTodoList.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MyTodoList.config.AiProps;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LlmIntentParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callsChatCompletionsEndpointUnderConfiguredBasePath() throws Exception {
        AtomicReference<String> requestedPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/openai/v1/chat/completions", exchange -> {
            requestedPath.set(exchange.getRequestURI().getPath());
            requestBody.set(readBody(exchange));
            respondJson(exchange, openAiCompatibleResponse());
        });
        server.start();

        AiProps props = new AiProps();
        props.setEnabled(true);
        props.setBaseUrl("http://localhost:" + server.getAddress().getPort() + "/openai/v1/");
        props.setApiKey("test-key");
        props.setModel("test-model");

        LlmIntentParser parser = new LlmIntentParser(props, objectMapper, new RuleBasedIntentParser());
        ParsedIntent parsedIntent = parser.parse("Dame la lista de tareas pendientes");

        assertEquals(IntentType.LIST_TASKS_BY_STATUS, parsedIntent.getIntent());
        assertEquals("PENDING", parsedIntent.getStatus());
        assertEquals("/openai/v1/chat/completions", requestedPath.get());

        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertEquals("test-model", payload.path("model").asText());
        assertEquals("json_object", payload.path("response_format").path("type").asText());
        assertEquals("Dame la lista de tareas pendientes", payload.path("messages").path(1).path("content").asText());
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private void respondJson(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private byte[] openAiCompatibleResponse() throws IOException {
        String content = "{\n" +
            "  \"intent\": \"LIST_TASKS_BY_STATUS\",\n" +
            "  \"assignee\": null,\n" +
            "  \"status\": \"PENDING\",\n" +
            "  \"title\": null,\n" +
            "  \"storyPoints\": null,\n" +
            "  \"sprintName\": null,\n" +
            "  \"clarificationNeeded\": false,\n" +
            "  \"clarificationQuestion\": null\n" +
            "}";

        Map<String, Object> message = Map.of("content", content);
        Map<String, Object> choice = Map.of("message", message);
        Map<String, Object> response = Map.of("choices", List.of(choice));
        return objectMapper.writeValueAsBytes(response);
    }
}
