package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TranscriptLookupResponseDto {
    private Long transcriptId;
    private Long videoId;
    private String createdAt;
    private String text;
}
