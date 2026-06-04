package com.springboot.MyTodoList.dto.ai;

public class AiChatRequestDTO {

    private String message;

    public AiChatRequestDTO() {
    }

    public AiChatRequestDTO(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String message() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}