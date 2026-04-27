package com.springboot.MyTodoList.dto;

import java.time.LocalDateTime;

public class GroupMemberResponseDTO {

    private Long id;
    private Long groupId;
    private String groupName;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long roleId;
    private String roleName;
    private LocalDateTime joinedAt;

    public GroupMemberResponseDTO() {
    }

    public GroupMemberResponseDTO(
            Long id,
            Long groupId,
            String groupName,
            Long userId,
            String userName,
            String userEmail,
            Long roleId,
            String roleName,
            LocalDateTime joinedAt
    ) {
        this.id = id;
        this.groupId = groupId;
        this.groupName = groupName;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.roleId = roleId;
        this.roleName = roleName;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}