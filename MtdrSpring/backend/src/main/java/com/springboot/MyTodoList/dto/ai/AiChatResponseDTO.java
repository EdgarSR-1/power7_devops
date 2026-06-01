package com.springboot.MyTodoList.dto.ai;

public class AiChatResponseDTO {

    private String answer;

    public AiChatResponseDTO() {
    }

    public AiChatResponseDTO(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public String answer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}