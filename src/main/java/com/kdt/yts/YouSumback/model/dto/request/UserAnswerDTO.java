package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAnswerDTO {
    private Long questionId;
    private Long answerOptionId;  // ← 이게 핵심
}
