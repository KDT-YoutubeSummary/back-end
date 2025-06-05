package com.kdt.yts.YouSumback.model.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranscriptSaveRequestDto {
    private String originalUrl; // 또는 videoUrl
    // 필요하면 추가 필드 있음 (예: username, videoId 등)
}

