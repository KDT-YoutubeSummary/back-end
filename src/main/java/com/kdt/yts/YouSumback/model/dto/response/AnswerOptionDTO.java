package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnswerOptionDTO {
    private String optionText; // 답변 옵션의 텍스트
    private boolean isCorrect; // 해당 옵션이 정답인지 여부
}
