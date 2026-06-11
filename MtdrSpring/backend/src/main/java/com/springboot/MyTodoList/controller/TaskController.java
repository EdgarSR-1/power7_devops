package com.springboot.MyTodoList.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.springboot.MyTodoList.dto.TaskRequestDTO;
import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import com.springboot.MyTodoList.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    @PostMapping
    public TaskResponseDTO createTask(@RequestBody TaskRequestDTO dto, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return taskService.createTask(dto, currentUser);
    }

    @GetMapping
    public List<TaskResponseDTO> getAllTasks(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return taskService.getAllTasks(currentUser);
    }

    @GetMapping("/{id}")
    public TaskResponseDTO getTaskById(@PathVariable Long id, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return taskService.getTaskById(id, currentUser);
    }

    @PutMapping("/{id}")
    public TaskResponseDTO updateTask(
            @PathVariable Long id,
            @RequestBody TaskRequestDTO dto,
            Authentication authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        return taskService.updateTask(id, dto, currentUser);
    }

    @PutMapping("/{id}/status")
    public TaskResponseDTO updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication authentication
    ) {
        String rawStatus = payload.get("status");
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        try {
            User currentUser = getCurrentUser(authentication);
            return taskService.updateTaskStatus(id, TaskStatus.valueOf(rawStatus), currentUser);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value");
        }
    }
}