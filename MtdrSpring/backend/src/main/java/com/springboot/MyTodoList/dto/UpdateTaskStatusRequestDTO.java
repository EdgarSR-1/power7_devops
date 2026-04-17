package com.springboot.MyTodoList.dto;

public class UpdateTaskStatusRequestDTO {
    private String status;

    public UpdateTaskStatusRequestDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
