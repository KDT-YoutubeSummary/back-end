package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class QuestionWithOptionsResponseDTO {
    private Long questionId;
    private List<OptionDTO> options;
}
