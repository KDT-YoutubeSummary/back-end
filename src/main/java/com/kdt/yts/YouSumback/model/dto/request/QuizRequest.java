package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizRequest {
    private String summaryText;
    private int numberOfQuestions;

    // ↓ Long → Integer 로 변경합니다.
    private Integer transcriptId;
    private Integer summaryId;
}
