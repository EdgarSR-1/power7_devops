package com.springboot.MyTodoList.dto;

import java.util.List;

public class UserGroupResponseDTO {

    private Long id;
    private String title;
    private String description;
    private int progress;
    private int total;
    private List<String> members;

    public UserGroupResponseDTO() {
    }

    public UserGroupResponseDTO(Long id, String title, String description, int progress, int total, List<String> members) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.progress = progress;
        this.total = total;
        this.members = members;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}