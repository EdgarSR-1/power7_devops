package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.ai.AiEstimateRequestDTO;
import com.springboot.MyTodoList.dto.ai.AiEstimateResponseDTO;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import com.springboot.MyTodoList.service.AiEstimationService;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AiEstimationController {

    private final AiEstimationService aiEstimationService;
    private final UserRepository userRepository;

    public AiEstimationController(AiEstimationService aiEstimationService, UserRepository userRepository) {
        this.aiEstimationService = aiEstimationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/estimate")
    public AiEstimateResponseDTO estimate(@RequestBody AiEstimateRequestDTO request, Authentication authentication) {
        User currentUser = resolveCurrentUser(authentication).orElse(null);
        return aiEstimationService.answerQuestion(request, currentUser);
    }

    private Optional<User> resolveCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return Optional.empty();
        }

        return userRepository.findByEmail(email);
    }
}
