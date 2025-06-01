package com.YouSumback.model.dto.response;

// TranscriptLookupResponseDto.java
public class TranscriptLookupResponseDto {

    private Long transcriptId;
    private String videoId;
    private String createdAt;
    private String text;

    public TranscriptLookupResponseDto(Long transcriptId, String videoId, String createdAt, String text) {
        this.transcriptId = transcriptId;
        this.videoId = videoId;
        this.createdAt = createdAt;
        this.text = text;
    }

    // getter
}

