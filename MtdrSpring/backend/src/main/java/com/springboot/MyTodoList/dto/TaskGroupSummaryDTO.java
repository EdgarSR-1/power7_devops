package com.springboot.MyTodoList.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TaskGroupSummaryDTO {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private GroupMemberDTO createdBy;
    private long completedTasks;
    private long totalTasks;
    private List<GroupMemberDTO> members;

    public TaskGroupSummaryDTO() {
    }

    public TaskGroupSummaryDTO(Long id, String name, String description, LocalDateTime createdAt,
                               GroupMemberDTO createdBy, long completedTasks, long totalTasks,
                               List<GroupMemberDTO> members) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.members = members;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public GroupMemberDTO getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(GroupMemberDTO createdBy) {
        this.createdBy = createdBy;
    }

    public long getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(long completedTasks) {
        this.completedTasks = completedTasks;
    }

    public long getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(long totalTasks) {
        this.totalTasks = totalTasks;
    }

    public List<GroupMemberDTO> getMembers() {
        return members;
    }

    public void setMembers(List<GroupMemberDTO> members) {
        this.members = members;
    }
}
