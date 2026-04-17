package com.springboot.MyTodoList.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GroupTaskDTO {
    private Long id;
    private String title;
    private String status;
    private LocalDateTime dueDate;
    private List<GroupMemberDTO> assignees;

    public GroupTaskDTO() {
    }

    public GroupTaskDTO(Long id, String title, String status, LocalDateTime dueDate, List<GroupMemberDTO> assignees) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.dueDate = dueDate;
        this.assignees = assignees;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public List<GroupMemberDTO> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<GroupMemberDTO> assignees) {
        this.assignees = assignees;
    }
}
