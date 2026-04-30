package com.springboot.MyTodoList.dto.kpi;

public class CompletedTasksByUserSprintGroupDTO {

    private Long groupId;
    private String groupName;

    private Long sprintId;
    private String sprintName;

    private Long userId;
    private String userName;

    private Long completedTasks;

    public CompletedTasksByUserSprintGroupDTO(
            Long groupId,
            String groupName,
            Long sprintId,
            String sprintName,
            Long userId,
            String userName,
            Long completedTasks
    ) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.sprintId = sprintId;
        this.sprintName = sprintName;
        this.userId = userId;
        this.userName = userName;
        this.completedTasks = completedTasks;
    }

    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }

    public Long getSprintId() { return sprintId; }
    public String getSprintName() { return sprintName; }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }

    public Long getCompletedTasks() { return completedTasks; }
}