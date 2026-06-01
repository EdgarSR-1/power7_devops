package com.springboot.MyTodoList.controller;

// 1. Imports de Java estándar

import java.util.Map;

// 2. Imports de Spring Web y Core

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;

// 3. Imports base de Spring AI (Clases comunes de Chat)

import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.ai.chat.model.ChatResponse;

import org.springframework.ai.chat.prompt.Prompt;

// 4. Import específico del nuevo modelo de Google GenAI

import org.springframework.ai.google.genai.GoogleGenAiChatModel;

// 5. Import de Project Reactor (Para el manejo de flujos asíncronos con Flux)

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(
            @RequestBody AiChatRequestDTO request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(aiChatService.chat(request, authentication));
    }
}