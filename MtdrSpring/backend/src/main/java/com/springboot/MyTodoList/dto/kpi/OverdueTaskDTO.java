package com.springboot.MyTodoList.dto.kpi;

import com.springboot.MyTodoList.model.TaskStatus;
import java.time.LocalDateTime;

public class OverdueTaskDTO {
    private Long taskId;
    private String title;
    private String status;
    private LocalDateTime dueDate;

    public OverdueTaskDTO(Long taskId, String title, TaskStatus status, LocalDateTime dueDate) {
        this.taskId = taskId;
        this.title = title;
        this.status = status.name();
        this.dueDate = dueDate;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }
}