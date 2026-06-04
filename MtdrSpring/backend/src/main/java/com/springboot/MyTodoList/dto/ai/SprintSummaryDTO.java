package com.springboot.MyTodoList.dto.ai;

public class SprintSummaryDTO {

    private Long sprintId;
    private String sprintName;
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer pendingTasks;
    private Integer inProgressTasks;
    private Double completionPercentage;

    public SprintSummaryDTO() {
    }

    public SprintSummaryDTO(
            Long sprintId,
            String sprintName,
            Integer totalTasks,
            Integer completedTasks,
            Integer pendingTasks,
            Integer inProgressTasks,
            Double completionPercentage
    ) {
        this.sprintId = sprintId;
        this.sprintName = sprintName;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.pendingTasks = pendingTasks;
        this.inProgressTasks = inProgressTasks;
        this.completionPercentage = completionPercentage;
    }

    public Long getSprintId() {
        return sprintId;
    }

    public String getSprintName() {
        return sprintName;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public Integer getCompletedTasks() {
        return completedTasks;
    }

    public Integer getPendingTasks() {
        return pendingTasks;
    }

    public Integer getInProgressTasks() {
        return inProgressTasks;
    }

    public Double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }

    public void setCompletedTasks(Integer completedTasks) {
        this.completedTasks = completedTasks;
    }

    public void setPendingTasks(Integer pendingTasks) {
        this.pendingTasks = pendingTasks;
    }

    public void setInProgressTasks(Integer inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
    }

    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
}