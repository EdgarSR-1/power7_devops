package com.springboot.MyTodoList.service.ai;

import com.springboot.MyTodoList.dto.ai.AiChatRequestDTO;
import com.springboot.MyTodoList.dto.ai.AiChatResponseDTO;
import com.springboot.MyTodoList.service.tools.TaskAiTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final TaskAiTools taskAiTools;

    public AiChatService(
            ChatClient.Builder chatClientBuilder,
            TaskAiTools taskAiTools
    ) {
        this.chatClient = chatClientBuilder.build();
        this.taskAiTools = taskAiTools;
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

        try {
            AiUserContextHolder.set(authentication.getName());

            String answer = chatClient
                    .prompt()
                    .system(systemPrompt())
                    .user(request.getMessage())
                    .tools(taskAiTools)
                    .call()
                    .content();

            return new AiChatResponseDTO(answer);

        } catch (Exception e) {
            return new AiChatResponseDTO(
                    "Ocurrió un error al procesar tu pregunta: " + e.getMessage()
            );
        } finally {
            AiUserContextHolder.clear();
        }
    }

    private String systemPrompt() {
        return "Eres un asistente de gestión de proyectos dentro de una aplicación web.\n\n"
                + "Reglas obligatorias:\n"
                + "- Responde siempre en español.\n"
                + "- Usa tools cuando la pregunta requiera datos reales.\n"
                + "- No inventes tareas, usuarios, grupos, sprints ni métricas.\n"
                + "- No generes SQL.\n"
                + "- No muestres información sensible.\n"
                + "- Da respuestas breves, claras y accionables.";
    }
}