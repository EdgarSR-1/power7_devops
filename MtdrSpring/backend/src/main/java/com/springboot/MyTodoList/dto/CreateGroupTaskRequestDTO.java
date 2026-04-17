package com.springboot.MyTodoList.dto;

import java.time.LocalDateTime;

public class CreateGroupTaskRequestDTO {
    private String title;
    private LocalDateTime dueDate;
    private Long assigneeId;

    public CreateGroupTaskRequestDTO() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
