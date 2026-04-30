package com.springboot.MyTodoList.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MyTodoList.config.AiProps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Servicio para estimar automáticamente el tiempo de ejecución de tareas
 * utilizando IA. El LLM analiza el título y descripción y devuelve una
 * estimación de horas recomendadas.
 */
@Service
public class TaskEstimationService {

    private static final Logger logger = LoggerFactory.getLogger(TaskEstimationService.class);

    private final AiProps aiProps;
    private final ObjectMapper objectMapper;

    public TaskEstimationService(AiProps aiProps, ObjectMapper objectMapper) {
        this.aiProps = aiProps;
        this.objectMapper = objectMapper;
    }

    /**
     * Estima automáticamente las horas recomendadas para una tarea basándose
     * en su título y descripción. Si el LLM no está configurado, devuelve
     * una estimación por defecto.
     *
     * @param title Título de la tarea (requerido)
     * @param description Descripción de la tarea (opcional)
     * @return Estimación en horas (Float)
     */
    public Float estimateTaskHours(String title, String description) {
        if (!isLlmConfigured()) {
            logger.debug("LLM deshabilitado. Usando estimación por defecto.");
            return getDefaultEstimation(title, description);
        }

        try {
            String endpoint = chatCompletionsEndpoint();
            RestClient client = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProps.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

            String systemPrompt = "Eres un experto en estimación de esfuerzo en desarrollo de software.\n" +
                "Tu tarea es estimar inteligentemente el tiempo que requiere completar una tarea basándote en su descripción.\n" +
                "\n" +
                "Responde SOLO con un JSON con este formato:\n" +
                "{\n" +
                "  \"estimatedHours\": <numero>,\n" +
                "  \"reasoning\": \"explicacion breve\"\n" +
                "}\n" +
                "\n" +
                "Guía de estimación:\n" +
                "- Tareas triviales (cambios de texto, ajustes menores): 0.5 - 1 hora\n" +
                "- Tareas pequeñas (bug fix simple, una característica pequeña): 1 - 3 horas\n" +
                "- Tareas medianas (desarrollo de una feature moderada, refactorización): 3 - 8 horas\n" +
                "- Tareas grandes (integración compleja, nueva funcionalidad mayor): 8 - 16 horas\n" +
                "- Tareas muy grandes (proyecto completo, arquitectura): > 16 horas\n" +
                "\n" +
                "Factores a considerar:\n" +
                "- Complejidad técnica: menciones de palabras como 'integración', 'API', 'BD', 'arquitectura', 'rewrite', etc. aumentan el tiempo\n" +
                "- Testing: si menciona 'test', 'validar', 'QA', suma tiempo\n" +
                "- Documentación: si menciona 'documentar', suma tiempo\n" +
                "- Revisión de código: si menciona 'revisar', 'code review', suma tiempo\n" +
                "\n" +
                "Siempre devuelve una estimación razonable entre 0.5 y 40 horas.\n" +
                "Usa valores con decimales cuando sea apropiado (2.5, 3.5, etc).";

            String taskContext = String.format("Título: %s\nDescripción: %s",
                title != null ? title : "",
                description != null ? description : "");

            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", taskContext);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(systemMsg);
            messages.add(userMsg);

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", aiProps.getModel());
            payload.put("messages", messages);
            payload.put("temperature", 0.3);
            payload.put("max_tokens", 200);
            payload.put("response_format", responseFormat);

            logger.debug("Estimando horas para tarea: title='{}', description='{}'", title, description);

            String responseBody = client.post()
                .uri(endpoint)
                .body(payload)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");

            if (content.isMissingNode() || content.asText().isBlank()) {
                logger.warn("Contenido vacío del LLM para estimación. Usando estimación por defecto.");
                return getDefaultEstimation(title, description);
            }

            String jsonContent = content.asText().trim();
            JsonNode estimationNode = objectMapper.readTree(jsonContent);
            Float estimatedHours = estimationNode.path("estimatedHours").asText().isEmpty() ?
                null :
                (float) estimationNode.path("estimatedHours").asDouble();

            if (estimatedHours != null && estimatedHours > 0) {
                String reasoning = estimationNode.path("reasoning").asText("");
                logger.info("Estimación LLM: {} horas. Razón: {}", estimatedHours, reasoning);
                return estimatedHours;
            }

            return getDefaultEstimation(title, description);
        } catch (Exception ex) {
            logger.warn("Fallo en estimación LLM. Usando estimación por defecto. Error: {}", ex.getMessage());
            return getDefaultEstimation(title, description);
        }
    }

    /**
     * Estimación por defecto cuando LLM no está disponible.
     * Utiliza heurística simple basada en palabras clave.
     */
    private Float getDefaultEstimation(String title, String description) {
        String combined = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase();

        // Palabras clave de alta complejidad
        if (contains(combined, "integración", "integration", "arquitectura", "architecture", "rewrite", "refactor", "api", "base de datos")) {
            return 8.0f;
        }
        // Palabras clave de complejidad media
        if (contains(combined, "bug", "fix", "feature", "implementar", "implement", "revisar", "review")) {
            return 4.0f;
        }
        // Palabras clave triviales
        if (contains(combined, "typo", "cambio de texto", "ajuste", "adjustment")) {
            return 1.0f;
        }
        // Por defecto, estimación conservadora
        return 3.0f;
    }

    private boolean contains(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLlmConfigured() {
        return aiProps.isEnabled()
            && hasText(aiProps.getApiKey())
            && hasText(aiProps.getBaseUrl())
            && hasText(aiProps.getModel());
    }

    private String chatCompletionsEndpoint() {
        return aiProps.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
