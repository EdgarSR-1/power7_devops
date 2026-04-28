package com.springboot.MyTodoList.dto;

public class ChatResponse {

    private final String response;

    public ChatResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }
}

