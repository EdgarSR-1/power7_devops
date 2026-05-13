package com.springboot.MyTodoList.dto.ai;

public class AiEstimateResponseDTO {

    private String answer;
    private boolean generatedByAi;

    public AiEstimateResponseDTO() {
    }

    public AiEstimateResponseDTO(String answer, boolean generatedByAi) {
        this.answer = answer;
        this.generatedByAi = generatedByAi;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isGeneratedByAi() {
        return generatedByAi;
    }

    public void setGeneratedByAi(boolean generatedByAi) {
        this.generatedByAi = generatedByAi;
    }
}
