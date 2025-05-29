package com.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizRequest {
    private String summaryText;
    private int numberOfQuestions;
    private Long transcriptId;
}
