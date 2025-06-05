package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuestionDTO {
    private String questionText; // 질문 텍스트
    private List<AnswerOptionDTO> options; // 답변 옵션 리스트
}
