package com.springboot.MyTodoList.service.tools;

import com.springboot.MyTodoList.dto.ai.GroupSummaryDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GroupAiTools {

    @Tool(description = "Obtiene los grupos del usuario autenticado. Actualmente está en modo de prueba.")
    public List<GroupSummaryDTO> getMyGroups() {
        return new ArrayList<GroupSummaryDTO>();
    }
}