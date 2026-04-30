package com.springboot.MyTodoList.dto.kpi;

public class HoursBySprintDTO {

    private Long groupId;
    private String groupName;

    private Long sprintId;
    private String sprintName;

    private Long userId;
    private String userName;

    private Double estimatedHours;

    public HoursBySprintDTO(
            Long groupId,
            String groupName,
            Long sprintId,
            String sprintName,
            Long userId,
            String userName,
            Double estimatedHours
    ) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.sprintId = sprintId;
        this.sprintName = sprintName;
        this.userId = userId;
        this.userName = userName;
        this.estimatedHours = estimatedHours;
    }

    public Long getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }

    public Long getSprintId() { return sprintId; }
    public String getSprintName() { return sprintName; }

    public Long getUserId() { return userId; }
    public String getUserName() { return userName; }

    public Double getEstimatedHours() { return estimatedHours; }
}