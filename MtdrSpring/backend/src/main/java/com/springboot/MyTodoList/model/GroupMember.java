package com.springboot.MyTodoList.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"})
)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK to taskgroups
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private TaskGroup group;

    // FK to users
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // FK to roles table
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Plain text role 
    @Column(name = "role", length = 255)
    private String roleName;

    // Timestamp handled by DB
    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public GroupMember() {
    }

    public GroupMember(Long id, TaskGroup group, User user, Role role, String roleName, LocalDateTime joinedAt) {
        this.id = id;
        this.group = group;
        this.user = user;
        this.role = role;
        this.roleName = roleName;
        this.joinedAt = joinedAt;
    }

    @PrePersist
    public void prePersist() {
        // keep roleName in sync automatically
        if (this.role != null && (this.roleName == null || this.roleName.isBlank())) {
            this.roleName = this.role.getName();
        }
    }

    // getters & setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TaskGroup getGroup() {
        return group;
    }

    public void setGroup(TaskGroup group) {
        this.group = group;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}