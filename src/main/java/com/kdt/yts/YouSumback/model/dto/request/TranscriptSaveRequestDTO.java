package com.kdt.yts.YouSumback.model.dto.request;

import com.kdt.yts.YouSumback.model.entity.SummaryType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptSaveRequestDTO {
    private String originalUrl;   // 유튜브 링크
    private String userPrompt;       // 요약 목적 (e.g. REVIEW, EXAM, STUDY)
    private SummaryType summaryType;   // 요약 형식 (e.g. BULLET, PARAGRAPH, QA)
    // ENUM 타입으로 변경할 예정

}

