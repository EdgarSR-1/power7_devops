package com.springboot.MyTodoList.dto.kpi;

public class CompletedBySprintDTO {
    private Long sprintId;
    private String sprintName;
    private Long completedTasks;

    public CompletedBySprintDTO(Long sprintId, String sprintName, Long completedTasks) {
        this.sprintId = sprintId;
        this.sprintName = sprintName;
        this.completedTasks = completedTasks;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public String getSprintName() {
        return sprintName;
    }

    public Long getCompletedTasks() {
        return completedTasks;
    }
}