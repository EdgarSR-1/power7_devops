package com.springboot.MyTodoList.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MyTodoList.config.AiProps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class LlmIntentParser implements IntentParser {

    private static final Logger logger = LoggerFactory.getLogger(LlmIntentParser.class);

    private final AiProps aiProps;
    private final ObjectMapper objectMapper;
    private final RuleBasedIntentParser fallbackParser;

    public LlmIntentParser(AiProps aiProps, ObjectMapper objectMapper, RuleBasedIntentParser fallbackParser) {
        this.aiProps = aiProps;
        this.objectMapper = objectMapper;
        this.fallbackParser = fallbackParser;
    }

    @Override
    public ParsedIntent parse(String messageText) {
        if (!isLlmConfigured()) {
            logger.debug("LLM deshabilitado. Usando parser fallback.");
            return fallbackParser.parse(messageText);
        }

        try {
            String endpoint = chatCompletionsEndpoint();
            RestClient client = RestClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProps.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

            String systemPrompt = "Eres un clasificador de intenciones inteligente para un asistente de gestion de sprints y tareas en metodologia agile.\n" +
                "Tu tarea es identificar la intencion del usuario y extraer parametros relevantes.\n" +
                "\n" +
                "Responde SOLO con JSON valido, sin explicaciones adicionales.\n" +
                "\n" +
                "Intenciones permitidas:\n" +
                "- HELP: Usuario pide ayuda, ejemplos, o informacion sobre comandos disponibles\n" +
                "- LIST_TASKS: Usuario quiere ver todas las tareas o la lista completa\n" +
                "- LIST_TASKS_BY_ASSIGNEE: Usuario quiere tareas asignadas a una persona especifica\n" +
                "- LIST_TASKS_BY_STATUS: Usuario quiere tareas con un estado especifico (PENDING, IN_PROGRESS, DONE)\n" +
                "- LIST_TASKS_BY_SPRINT: Usuario quiere tareas de un sprint especifico o del sprint actual\n" +
                "- CREATE_TASK: Usuario quiere crear una nueva tarea\n" +
                "- START_TASK: Usuario quiere iniciar una tarea y pasarla a IN_PROGRESS\n" +
                "- COMPLETE_TASK: Usuario quiere marcar una tarea como terminada\n" +
                "- REOPEN_TASK: Usuario quiere quitar estado de terminada y devolverla a pendiente\n" +
                "- DELETE_TASK: Usuario quiere eliminar una tarea\n" +
                "- CURRENT_SPRINT_SUMMARY: Usuario quiere resumen o estado del sprint actual\n" +
                "- TEAM_LOAD_SUMMARY: Usuario quiere ver la carga de trabajo del equipo\n" +
                "- GUACAMOLE_RECIPE: Usuario pregunta como hacer, preparar o servir guacamole\n" +
                "- UNKNOWN: No se puede clasificar la intencion\n" +
                "\n" +
                "Contexto y alcance:\n" +
                "- Este asistente solo debe responder sobre tareas, sprints y carga de equipo.\n" +
                "- La unica pregunta fuera de ese dominio que se permite es como hacer guacamole.\n" +
                "- Si el usuario pregunta cualquier otro tema fuera de gestion agile o guacamole, usa UNKNOWN con clarificationNeeded=true.\n" +
                "\n" +
                "Estructura JSON esperada:\n" +
                "{\n" +
                "  \"intent\": \"NOMBRE_DE_INTENCION\",\n" +
                "  \"assignee\": \"nombre_persona_o_null\",\n" +
                "  \"status\": \"PENDING|IN_PROGRESS|DONE_o_null\",\n" +
                "  \"title\": \"titulo_de_tarea_o_null\",\n" +
                    "  \"description\": \"descripcion_detallada_o_null\",\n" +
                "  \"taskId\": numero_o_null,\n" +
                "  \"storyPoints\": numero_o_null,\n" +
                "  \"groupName\": \"nombre_grupo_o_null\",\n" +
                "  \"sprintName\": \"nombre_sprint_o_null\",\n" +
                "  \"clarificationNeeded\": true|false,\n" +
                "  \"clarificationQuestion\": \"pregunta_si_necesita_aclaracion_o_null\"\n" +
                "}\n" +
                "\n" +
                "Ejemplos de entrada y salida esperada:\n" +
                "Entrada: \"¿Qué tareas tiene Ana?\"\n" +
                "Salida: {\"intent\":\"LIST_TASKS_BY_ASSIGNEE\",\"assignee\":\"Ana\",\"status\":null,\"title\":null,\"taskId\":null,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Dame la lista de tareas pendientes\"\n" +
                "Salida: {\"intent\":\"LIST_TASKS_BY_STATUS\",\"assignee\":null,\"status\":\"PENDING\",\"title\":null,\"taskId\":null,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Crea una tarea para revisar el codigo en grupo Backend y asignala a Juan con 5 puntos\"\n" +
                    "Salida: {\"intent\":\"CREATE_TASK\",\"assignee\":\"Juan\",\"status\":null,\"title\":\"revisar el codigo\",\"description\":\"Revisar el codigo de la API para asegurar calidad\",\"taskId\":null,\"storyPoints\":5,\"groupName\":\"Backend\",\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Inicia la tarea 23\"\n" +
                "Salida: {\"intent\":\"START_TASK\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":23,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Marca la tarea #23 como terminada\"\n" +
                "Salida: {\"intent\":\"COMPLETE_TASK\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":23,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Reabre la tarea 23\"\n" +
                "Salida: {\"intent\":\"REOPEN_TASK\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":23,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Elimina la tarea 23\"\n" +
                "Salida: {\"intent\":\"DELETE_TASK\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":23,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"Dame tareas del sprint Beta\"\n" +
                "Salida: {\"intent\":\"LIST_TASKS_BY_SPRINT\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":null,\"storyPoints\":null,\"groupName\":null,\"sprintName\":\"Beta\",\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"¿Cómo va el sprint?\"\n" +
                "Salida: {\"intent\":\"CURRENT_SPRINT_SUMMARY\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":null,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"¿Quién tiene más trabajo?\"\n" +
                "Salida: {\"intent\":\"TEAM_LOAD_SUMMARY\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":null,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Entrada: \"¿Cómo hacer un guacamole?\"\n" +
                "Salida: {\"intent\":\"GUACAMOLE_RECIPE\",\"assignee\":null,\"status\":null,\"title\":null,\"taskId\":null,\"storyPoints\":null,\"groupName\":null,\"sprintName\":null,\"clarificationNeeded\":false,\"clarificationQuestion\":null}\n" +
                "\n" +
                "Reglas importantes:\n" +
                "- Si la intencion es CREATE_TASK y falta titulo o groupName, establece clarificationNeeded=true\n" +
                    "- Si la intencion es CREATE_TASK y faltan horas estimadas, NO establecas clarificationNeeded; el sistema las estimara automaticamente\n" +
                "- Si la intencion es COMPLETE_TASK y faltan horas reales, establece clarificationNeeded=true pidiendo 'con N puntos'\n" +
                "- Si la intencion es START_TASK, COMPLETE_TASK, REOPEN_TASK o DELETE_TASK y no hay taskId, establece clarificationNeeded=true\n" +
                "- Identifica nombres de personas con flexibilidad (Ana, ana, ANA, etc.)\n" +
                "- Identifica estados con sinonimos: (done, completada, hecha, terminada) => DONE; (pendiente, por hacer, sin hacer) => PENDING; (progreso, en proceso, haciendo, in progress) => IN_PROGRESS\n" +
                "- No inventes nuevas intenciones fuera de la lista permitida\n" +
                "- Si no esta clara la intencion, establece intent=UNKNOWN y clarificationNeeded=true con una pregunta util\n";

            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", messageText == null ? "" : messageText);

            List<Map<String, Object>> messages = new ArrayList<>();
            messages.add(systemMsg);
            messages.add(userMsg);

            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_object");

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", aiProps.getModel());
            payload.put("messages", messages);
            payload.put("temperature", 0);
            payload.put("max_tokens", 500);
            payload.put("response_format", responseFormat);

            logger.debug("Enviando a LLM: modelo={}, mensaje={}", aiProps.getModel(), messageText);
            
            String responseBody = client.post()
                .uri(endpoint)
                .body(payload)
                .retrieve()
                .body(String.class);

            logger.debug("Respuesta LLM recibida: {}", responseBody);
            
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            
            if (content.isMissingNode() || content.asText().isBlank()) {
                logger.warn("Contenido vacio de LLM. Usando fallback.");
                return fallbackParser.parse(messageText);
            }

            String jsonContent = content.asText().trim();
            logger.debug("JSON parseado del LLM: {}", jsonContent);
            
            return objectMapper.readValue(jsonContent, ParsedIntent.class);
        } catch (Exception ex) {
            logger.warn("Fallo el parser LLM. Usando fallback local. Error: {}", ex.getMessage(), ex);
            return fallbackParser.parse(messageText);
        }
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
