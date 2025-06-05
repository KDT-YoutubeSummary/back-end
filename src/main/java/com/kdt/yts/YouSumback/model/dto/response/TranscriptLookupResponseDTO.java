package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class TranscriptLookupResponseDTO {
    private Long transcriptId;
    private Long videoId;
    private LocalDateTime createdAt;
    private String text;
}
