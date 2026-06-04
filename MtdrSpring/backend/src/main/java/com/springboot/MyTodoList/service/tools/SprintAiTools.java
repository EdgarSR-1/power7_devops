package com.springboot.MyTodoList.service.tools;

import com.springboot.MyTodoList.dto.ai.SprintSummaryDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class SprintAiTools {

    @Tool(description = "Obtiene un resumen básico de prueba del sprint actual.")
    public SprintSummaryDTO getCurrentSprintSummary() {
        return new SprintSummaryDTO(
                null,
                "Sprint actual",
                0,
                0,
                0,
                0,
                0.0
        );
    }
}