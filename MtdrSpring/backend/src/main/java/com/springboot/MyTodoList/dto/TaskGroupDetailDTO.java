package com.springboot.MyTodoList.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TaskGroupDetailDTO extends TaskGroupSummaryDTO {
    private List<GroupTaskDTO> tasks;

    public TaskGroupDetailDTO() {
    }

    public TaskGroupDetailDTO(Long id, String name, String description, LocalDateTime createdAt,
                              GroupMemberDTO createdBy, long completedTasks, long totalTasks,
                              List<GroupMemberDTO> members, List<GroupTaskDTO> tasks) {
        super(id, name, description, createdAt, createdBy, completedTasks, totalTasks, members);
        this.tasks = tasks;
    }

    public List<GroupTaskDTO> getTasks() {
        return tasks;
    }

    public void setTasks(List<GroupTaskDTO> tasks) {
        this.tasks = tasks;
    }
}
