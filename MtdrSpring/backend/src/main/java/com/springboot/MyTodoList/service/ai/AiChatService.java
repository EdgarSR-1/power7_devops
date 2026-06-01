package com.springboot.MyTodoList.service.ai;

import com.springboot.MyTodoList.dto.ai.AiChatRequestDTO;
import com.springboot.MyTodoList.dto.ai.AiChatResponseDTO;
import com.springboot.MyTodoList.service.tools.GroupAiTools;
import com.springboot.MyTodoList.service.tools.SprintAiTools;
import com.springboot.MyTodoList.service.tools.TaskAiTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final TaskAiTools taskAiTools;
    private final SprintAiTools sprintAiTools;
    private final GroupAiTools groupAiTools;

    public AiChatService(
            ChatClient.Builder chatClientBuilder,
            TaskAiTools taskAiTools,
            SprintAiTools sprintAiTools,
            GroupAiTools groupAiTools
    ) {
        this.chatClient = chatClientBuilder.build();
        this.taskAiTools = taskAiTools;
        this.sprintAiTools = sprintAiTools;
        this.groupAiTools = groupAiTools;
    }

    public AiChatResponseDTO chat(
            AiChatRequestDTO request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AiChatResponseDTO(
                    "No pude verificar tu sesión. Inicia sesión nuevamente para usar el asistente."
            );
        }

        String userEmail = authentication.getName();

        try {
            AiUserContextHolder.set(userEmail);

            String answer = chatClient
                    .prompt()
                    .system(systemPrompt())
                    .user(request.message())
                    .tools(taskAiTools, sprintAiTools, groupAiTools)
                    .call()
                    .content();

            return new AiChatResponseDTO(answer);

        } catch (Exception e) {
            return new AiChatResponseDTO(
                    "Ocurrió un error al procesar tu pregunta. Intenta reformularla o vuelve a intentarlo más tarde."
            );
        } finally {
            AiUserContextHolder.clear();
        }
    }

    private String systemPrompt() {
    return "Eres un asistente de gestión de proyectos dentro de una aplicación web.\n\n"
            + "Tu objetivo es ayudar al usuario a consultar información sobre sus tareas, "
            + "grupos, sprints, métricas y pendientes del proyecto.\n\n"
            + "Reglas obligatorias:\n"
            + "- Responde siempre en español.\n"
            + "- Usa tools cuando la pregunta requiera datos reales.\n"
            + "- No inventes tareas, usuarios, grupos, sprints ni métricas.\n"
            + "- No generes SQL.\n"
            + "- No menciones detalles internos de la base de datos.\n"
            + "- No muestres información sensible.\n"
            + "- No muestres contraseñas, hashes, tokens ni datos internos.\n"
            + "- No muestres información de grupos donde el usuario no pertenece.\n"
            + "- Si una tool devuelve una lista vacía, explica que no se encontraron datos.\n"
            + "- Si el usuario pide información fuera de sus permisos, responde que no tienes acceso a esa información.\n"
            + "- Da respuestas breves, claras y accionables.\n\n"
            + "Formato recomendado:\n"
            + "- Empieza con una respuesta directa.\n"
            + "- Después resume los datos importantes.\n"
            + "- Si aplica, agrega una recomendación concreta.\n\n"
            + "Ejemplos de preguntas que puedes resolver:\n"
            + "- ¿Qué tareas tengo vencidas?\n"
            + "- ¿Qué tareas tengo pendientes?\n"
            + "- ¿Cómo va el sprint actual?\n"
            + "- ¿Qué miembros tiene mi grupo?\n"
            + "- ¿Qué tareas están completadas?\n"
            + "- ¿Qué debería priorizar hoy?";
    }
}