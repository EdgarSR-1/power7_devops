package com.springboot.MyTodoList.dto.ai;

import com.springboot.MyTodoList.model.Task;

public class TaskSummaryDTO {

    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String dueDate;
    private String assigneeName;
    private String sprintName;
    private String groupName;

    public TaskSummaryDTO() {
    }

    public TaskSummaryDTO(
            Long id,
            String title,
            String description,
            String status,
            String priority,
            String dueDate,
            String assigneeName,
            String sprintName,
            String groupName
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.assigneeName = assigneeName;
        this.sprintName = sprintName;
        this.groupName = groupName;
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

        if (task.getPriority() != null) {
            dto.setPriority(task.getPriority().toString());
        }

        if (task.getDueDate() != null) {
            dto.setDueDate(task.getDueDate().toString());
        }

        /*
         * Ajusta estos getters según tus relaciones reales.
         * Por ejemplo:
         * - task.getAssignedUser()
         * - task.getUser()
         * - task.getAssignee()
         * - task.getSprint()
         * - task.getGroup()
         */

        try {
            if (task.getAssignedUser() != null) {
                dto.setAssigneeName(task.getAssignedUser().getName());
            }
        } catch (Exception ignored) {
        }

        try {
            if (task.getSprint() != null) {
                dto.setSprintName(task.getSprint().getName());
            }
        } catch (Exception ignored) {
        }

        try {
            if (task.getGroup() != null) {
                dto.setGroupName(task.getGroup().getName());
            }
        } catch (Exception ignored) {
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

    public String getPriority() {
        return priority;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public String getSprintName() {
        return sprintName;
    }

    public String getGroupName() {
        return groupName;
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

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}