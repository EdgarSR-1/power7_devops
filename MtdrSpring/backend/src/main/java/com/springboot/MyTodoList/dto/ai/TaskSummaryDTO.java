package com.springboot.MyTodoList.dto.ai;

import com.springboot.MyTodoList.model.Task;

public class TaskSummaryDTO {

    private Long id;
    private String title;
    private String description;
    private String status;
    private String dueDate;

    public TaskSummaryDTO() {
    }

    public TaskSummaryDTO(Long id, String title, String description, String status, String dueDate) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.dueDate = dueDate;
    }

    public static TaskSummaryDTO fromEntity(Task task) {
        if (task == null) {
            return null;
        }

        TaskSummaryDTO dto = new TaskSummaryDTO();

        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());

        if (task.getStatus() != null) {
            dto.setStatus(task.getStatus().toString());
        }

        if (task.getDueDate() != null) {
            dto.setDueDate(task.getDueDate().toString());
        }

        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}