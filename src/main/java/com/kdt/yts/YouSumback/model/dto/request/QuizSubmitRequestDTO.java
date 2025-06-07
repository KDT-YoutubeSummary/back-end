package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// QuizSubmitRequestDTO는 사용자가 푼 퀴즈 문제의 답안을 제출하기 위한 요청 DTO입니다.
public class QuizSubmitRequestDTO {
    private Long questionId;       // 사용자가 푼 문제 ID
    private Long selectedOptionId; // 사용자가 고른 보기 ID
}
