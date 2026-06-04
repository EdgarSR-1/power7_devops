package com.springboot.MyTodoList.dto.ai;

public class GroupSummaryDTO {

    private Long groupId;
    private String groupName;
    private String description;
    private Integer memberCount;

    public GroupSummaryDTO() {
    }

    public GroupSummaryDTO(
            Long groupId,
            String groupName,
            String description,
            Integer memberCount
    ) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.memberCount = memberCount;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getDescription() {
        return description;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}