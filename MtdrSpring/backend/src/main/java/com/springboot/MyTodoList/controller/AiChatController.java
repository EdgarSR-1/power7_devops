package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.ai.AiChatRequestDTO;
import com.springboot.MyTodoList.dto.ai.AiChatResponseDTO;
import com.springboot.MyTodoList.service.ai.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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