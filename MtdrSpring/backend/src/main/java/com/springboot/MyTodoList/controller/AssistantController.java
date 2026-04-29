package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.agent.AgentOrchestrator;
import com.springboot.MyTodoList.dto.ChatRequest;
import com.springboot.MyTodoList.dto.ChatResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@ConditionalOnProperty(prefix = "app.bot", name = "mode", havingValue = "llm")
@RequestMapping("/api/assistant")
public class AssistantController {

    private static final int STREAM_CHAR_DELAY_MS = 28;
    private static final int STREAM_COMMA_DELAY_MS = 85;
    private static final int STREAM_PUNCTUATION_DELAY_MS = 140;
    private static final int STREAM_LINE_DELAY_MS = 180;

    private final AgentOrchestrator orchestrator;

    public AssistantController(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return new ChatResponse(orchestrator.handleMessage(request.getMessage()));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<StreamingResponseBody> chatStream(@RequestBody ChatRequest request) {
        StreamingResponseBody body = outputStream -> {
            String response = orchestrator.handleMessage(request.getMessage());
            streamText(outputStream, response == null || response.isBlank() ? "Listo." : response);
        };

        return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-cache")
            .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
            .body(body);
    }

    private void streamText(OutputStream outputStream, String text) throws IOException {
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            outputStream.write(new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            sleepAfter(codePoint);
            index += Character.charCount(codePoint);
        }
    }

    private void sleepAfter(int codePoint) throws IOException {
        try {
            Thread.sleep(delayFor(codePoint));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Response streaming interrupted", ex);
        }
    }

    private int delayFor(int codePoint) {
        if (codePoint == '\n') {
            return STREAM_LINE_DELAY_MS;
        }
        if (codePoint == ',') {
            return STREAM_COMMA_DELAY_MS;
        }
        if (".!?;:".indexOf(codePoint) >= 0) {
            return STREAM_PUNCTUATION_DELAY_MS;
        }
        return STREAM_CHAR_DELAY_MS;
    }
}
