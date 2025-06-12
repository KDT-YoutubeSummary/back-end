package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
// QuestionDTO는 퀴즈의 각 문제를 나타내는 DTO입니다.
public class QuestionDTO {
    private Long questionId; // 문제 ID
    private String questionText; // 질문 텍스트
    private List<OptionDTO> options; // 답변 옵션 리스트
}
