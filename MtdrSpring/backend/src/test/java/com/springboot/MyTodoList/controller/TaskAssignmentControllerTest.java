package com.springboot.MyTodoList.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.MyTodoList.model.Task;
import com.springboot.MyTodoList.model.TaskAssignment;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.TaskAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskAssignmentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private TaskAssignmentService service;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(TaskAssignmentService.class);

        TaskAssignmentController controller = new TaskAssignmentController(service);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private Task task(Long id) {
        Task task = new Task();
        task.setId(id);
        return task;
    }

    private User user(Long id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TaskAssignment assignment(Long assignmentId, Long taskId, Long userId) {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(assignmentId);
        assignment.setTask(task(taskId));
        assignment.setUser(user(userId));
        return assignment;
    }

    @Test
    void shouldAssignTaskToUser() throws Exception {
        TaskAssignment request = assignment(null, 1L, 2L);
        TaskAssignment saved = assignment(10L, 1L, 2L);

        Mockito.when(service.save(any(TaskAssignment.class)))
                .thenReturn(saved);

        mockMvc.perform(post("/api/task-assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void shouldGetAssignmentsByUserId() throws Exception {
        Mockito.when(service.getByUserId(2L))
                .thenReturn(List.of(assignment(10L, 1L, 2L)));

        mockMvc.perform(get("/api/task-assignments/user/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void shouldGetAssignmentsByTaskId() throws Exception {
        Mockito.when(service.getByTaskId(1L))
                .thenReturn(List.of(assignment(10L, 1L, 2L)));

        mockMvc.perform(get("/api/task-assignments/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void shouldDeleteTaskAssignment() throws Exception {
        Mockito.doNothing().when(service).delete(10L);

        mockMvc.perform(delete("/api/task-assignments/10"))
                .andExpect(status().isNoContent());

        Mockito.verify(service).delete(10L);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingInvalidAssignment() throws Exception {
        Mockito.doThrow(new RuntimeException("Assignment not found"))
                .when(service)
                .delete(99L);

        mockMvc.perform(delete("/api/task-assignments/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Assignment not found"));
    }
}