package com.YouSumback.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TranscriptSaveResponseDto {

    @JsonProperty("transcript_id")
    private Long transcriptId;

    @JsonProperty("created_at")
    private String createdAt;

    public TranscriptSaveResponseDto(Long transcriptId, String createdAt) {
        this.transcriptId = transcriptId;
        this.createdAt = createdAt;
    }
}

