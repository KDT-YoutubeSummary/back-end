package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptSaveRequestDTO {
    private String originalUrl; // 또는 videoUrl
    private String purpose; // 요약 목적
    // 필요하면 추가 필드 있음 (예: username, videoId 등)
}

