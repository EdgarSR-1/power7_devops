package com.springboot.MyTodoList.dto.kpi;

import com.springboot.MyTodoList.model.TaskStatus;

public class StatusDistributionDTO {
    private String status;
    private Long count;

    public StatusDistributionDTO(TaskStatus status, Long count) {
        this.status = status.name();
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public Long getCount() {
        return count;
    }
}