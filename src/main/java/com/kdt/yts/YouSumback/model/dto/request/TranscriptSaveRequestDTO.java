package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptSaveRequestDTO {
    private String originalUrl;
    private String videoUrl;     // 👈 새로 추가
    private String youtubeId;    // 👈 새로 추가
    private String userPrompt;
    private String summaryType;
}
