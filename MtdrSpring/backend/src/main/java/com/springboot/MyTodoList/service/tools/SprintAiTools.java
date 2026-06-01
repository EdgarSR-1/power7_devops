package com.springboot.MyTodoList.service.tools;

import com.springboot.MyTodoList.dto.ai.SprintSummaryDTO;
import com.springboot.MyTodoList.service.KpiService;
import com.springboot.MyTodoList.service.ai.AiUserContextHolder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class SprintAiTools {

    private final KpiService kpiService;

    public SprintAiTools(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @Tool(description = "Obtiene el resumen del sprint actual para el usuario autenticado. "
            + "Incluye total de tareas, completadas, pendientes, en progreso y porcentaje de avance.")
    public SprintSummaryDTO getCurrentSprintSummary() {
        String userEmail = AiUserContextHolder.get();

        return kpiService.getCurrentSprintSummaryForUser(userEmail);
    }

    @Tool(description = "Obtiene el resumen de un sprint específico donde el usuario autenticado tiene acceso.")
    public SprintSummaryDTO getSprintSummary(Long sprintId) {
        String userEmail = AiUserContextHolder.get();

        return kpiService.getSprintSummaryForUser(userEmail, sprintId);
    }
}