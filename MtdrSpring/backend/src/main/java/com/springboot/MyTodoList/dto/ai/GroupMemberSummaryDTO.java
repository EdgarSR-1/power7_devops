package com.springboot.MyTodoList.dto.ai;

public class GroupMemberSummaryDTO {

    private Long userId;
    private String name;
    private String role;

    public GroupMemberSummaryDTO() {
    }

    public GroupMemberSummaryDTO(Long userId, String name, String role) {
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }
}