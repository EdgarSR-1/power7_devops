package com.springboot.MyTodoList.dto.kpi;

public class VelocityDTO {
    private Double averageCompletedTasksPerSprint;

    public VelocityDTO(Double averageCompletedTasksPerSprint) {
        this.averageCompletedTasksPerSprint = averageCompletedTasksPerSprint;
    }

    public Double getAverageCompletedTasksPerSprint() {
        return averageCompletedTasksPerSprint;
    }
}