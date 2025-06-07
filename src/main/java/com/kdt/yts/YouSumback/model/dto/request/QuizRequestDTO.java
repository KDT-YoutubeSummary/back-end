package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// QuizRequestDTO는 퀴즈 생성을 위한 요청 DTO입니다.
public class QuizRequestDTO {
    private Long transcriptId; // 요약된 텍스트의 ID

//    private String summaryText;
//    private int numberOfQuestions;
//    private Long transcriptId;
//    private Long summaryId;
}
