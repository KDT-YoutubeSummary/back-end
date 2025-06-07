package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SummaryResponse {
    private Integer summaryId;   // Long → Integer
    private String summaryText;
}


