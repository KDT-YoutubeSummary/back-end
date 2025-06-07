package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAnswer {
    private int questionId;
    private int answerOptionId;  // ← 이게 핵심
}
