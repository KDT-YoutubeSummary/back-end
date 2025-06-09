package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuestionWithOptionsResponse {
    private int questionId;
    private String questionText;
    private List<OptionDto> options;
}
