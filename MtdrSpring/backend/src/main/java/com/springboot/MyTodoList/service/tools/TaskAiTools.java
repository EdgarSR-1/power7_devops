package com.springboot.MyTodoList.service.tools;

import com.springboot.MyTodoList.dto.ai.TaskSummaryDTO;
import com.springboot.MyTodoList.service.TaskService;
import com.springboot.MyTodoList.service.ai.AiUserContextHolder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskAiTools {

    private final TaskService taskService;

    public TaskAiTools(TaskService taskService) {
        this.taskService = taskService;
    }

    @Tool(description = "Obtiene las tareas vencidas del usuario autenticado. "
            + "Úsala cuando el usuario pregunte por tareas atrasadas, vencidas, "
            + "pendientes fuera de fecha u overdue tasks.")
    public List<TaskSummaryDTO> getMyOverdueTasks() {
        String userEmail = AiUserContextHolder.get();

        return taskService.getOverdueTasksForUser(userEmail)
                .stream()
                .map(TaskSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Tool(description = "Obtiene las tareas del usuario autenticado filtradas por estado. "
            + "Estados permitidos: pending, in_progress, completed. "
            + "Úsala cuando el usuario pregunte por tareas pendientes, en progreso o completadas.")
    public List<TaskSummaryDTO> getMyTasksByStatus(String status) {
        String userEmail = AiUserContextHolder.get();

        return taskService.getTasksForUserByStatus(userEmail, status)
                .stream()
                .map(TaskSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Tool(description = "Obtiene las tareas de alta prioridad visibles para el usuario autenticado. "
            + "Úsala cuando el usuario pregunte qué tareas debería priorizar o cuáles son urgentes.")
    public List<TaskSummaryDTO> getMyHighPriorityTasks() {
        String userEmail = AiUserContextHolder.get();

        return taskService.getHighPriorityTasksForUser(userEmail)
                .stream()
                .map(TaskSummaryDTO::fromEntity)
                .collect(Collectors.toList());
    }
}