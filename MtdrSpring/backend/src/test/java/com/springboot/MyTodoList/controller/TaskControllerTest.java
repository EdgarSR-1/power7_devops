package com.springboot.MyTodoList.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MyTodoList.dto.TaskRequestDTO;
import com.springboot.MyTodoList.dto.TaskResponseDTO;
import com.springboot.MyTodoList.model.TaskStatus;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import com.springboot.MyTodoList.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private TaskService taskService;
    private UserRepository userRepository;

    private User currentUser;

    @BeforeEach
    void setUp() {
        taskService = Mockito.mock(TaskService.class);
        userRepository = Mockito.mock(UserRepository.class);

        TaskController controller = new TaskController(taskService, userRepository);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        currentUser = new User();
        ReflectionTestUtils.setField(currentUser, "id", 1L);
        currentUser.setName("Developer Test");
        currentUser.setEmail("developer@test.com");
        currentUser.setPassword("password");
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(
                "developer@test.com",
                null,
                List.of()
        );
    }

    @Test
    void shouldCreateTask() throws Exception {
        TaskRequestDTO request = new TaskRequestDTO();
        request.setListId(1L);
        request.setTitle("Crear endpoint de tareas");
        request.setDescription("Implementar POST /api/tasks");
        request.setStatus("pending");
        request.setPriority("high");
        request.setDueDate(LocalDateTime.now().plusDays(3));

        TaskResponseDTO response = new TaskResponseDTO();
        response.setId(1L);
        response.setTitle("Crear endpoint de tareas");
        response.setDescription("Implementar POST /api/tasks");
        response.setStatus("pending");
        response.setPriority("high");

        Mockito.when(userRepository.findByEmail("developer@test.com"))
                .thenReturn(Optional.of(currentUser));

        Mockito.when(taskService.createTask(any(TaskRequestDTO.class), eq(currentUser)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Crear endpoint de tareas"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.priority").value("high"));
    }

    @Test
    void shouldGetAllTasksForCurrentUser() throws Exception {
        TaskResponseDTO task = new TaskResponseDTO();
        task.setId(1L);
        task.setTitle("Tarea del desarrollador");
        task.setDescription("Asignada al usuario autenticado");
        task.setStatus("pending");
        task.setPriority("medium");

        Mockito.when(userRepository.findByEmail("developer@test.com"))
                .thenReturn(Optional.of(currentUser));

        Mockito.when(taskService.getAllTasks(currentUser))
                .thenReturn(List.of(task));

        mockMvc.perform(get("/api/tasks")
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Tarea del desarrollador"))
                .andExpect(jsonPath("$[0].status").value("pending"));
    }

    @Test
    void shouldGetTaskById() throws Exception {
        TaskResponseDTO task = new TaskResponseDTO();
        task.setId(1L);
        task.setTitle("Tarea específica");
        task.setDescription("Detalle de tarea");
        task.setStatus("in_progress");
        task.setPriority("high");

        Mockito.when(userRepository.findByEmail("developer@test.com"))
                .thenReturn(Optional.of(currentUser));

        Mockito.when(taskService.getTaskById(1L, currentUser))
                .thenReturn(task);

        mockMvc.perform(get("/api/tasks/1")
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Tarea específica"))
                .andExpect(jsonPath("$.status").value("in_progress"));
    }

    @Test
    void shouldCompleteTaskByUpdatingStatus() throws Exception {
        TaskResponseDTO response = new TaskResponseDTO();
        response.setId(1L);
        response.setTitle("Tarea completada");
        response.setStatus("completed");
        response.setPriority("medium");

        Mockito.when(userRepository.findByEmail("developer@test.com"))
                .thenReturn(Optional.of(currentUser));

        Mockito.when(taskService.updateTaskStatus(1L, TaskStatus.completed, currentUser))
                .thenReturn(response);

        Map<String, String> payload = Map.of(
                "status", "completed"
        );

        mockMvc.perform(put("/api/tasks/1/status")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsMissing() throws Exception {
        Map<String, String> payload = Map.of();

        mockMvc.perform(put("/api/tasks/1/status")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenStatusIsInvalid() throws Exception {
        Mockito.when(userRepository.findByEmail("developer@test.com"))
                .thenReturn(Optional.of(currentUser));

        Map<String, String> payload = Map.of(
                "status", "DONE"
        );

        mockMvc.perform(put("/api/tasks/1/status")
                        .principal(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }
}