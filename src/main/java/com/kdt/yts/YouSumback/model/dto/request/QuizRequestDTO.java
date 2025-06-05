package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizRequestDTO {
    private String summaryText;
    private int numberOfQuestions;
    private Long transcriptId;
    private Long summaryId;
}
