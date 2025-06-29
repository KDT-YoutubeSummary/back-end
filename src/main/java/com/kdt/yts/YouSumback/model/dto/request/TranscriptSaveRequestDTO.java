package com.kdt.yts.YouSumback.model.dto.request;

import com.kdt.yts.YouSumback.model.entity.SummaryType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptSaveRequestDTO {
    private String videoUrl;
    private String userPrompt;
    private SummaryType summaryType;
}
