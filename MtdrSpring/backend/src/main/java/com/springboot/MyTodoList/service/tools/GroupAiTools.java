package com.springboot.MyTodoList.service.tools;

import com.springboot.MyTodoList.dto.ai.GroupMemberSummaryDTO;
import com.springboot.MyTodoList.dto.ai.GroupSummaryDTO;
import com.springboot.MyTodoList.service.GroupService;
import com.springboot.MyTodoList.service.ai.AiUserContextHolder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupAiTools {

    private final GroupService groupService;

    public GroupAiTools(GroupService groupService) {
        this.groupService = groupService;
    }

    @Tool(description = "Obtiene los grupos a los que pertenece el usuario autenticado.")
    public List<GroupSummaryDTO> getMyGroups() {
        String userEmail = AiUserContextHolder.get();

        return groupService.getGroupsForUser(userEmail);
    }

    @Tool(description = "Obtiene los miembros de un grupo específico. "
            + "Solo devuelve miembros si el usuario autenticado pertenece al grupo o tiene permisos.")
    public List<GroupMemberSummaryDTO> getGroupMembers(Long groupId) {
        String userEmail = AiUserContextHolder.get();

        return groupService.getMembersForGroupIfAuthorized(userEmail, groupId);
    }
}