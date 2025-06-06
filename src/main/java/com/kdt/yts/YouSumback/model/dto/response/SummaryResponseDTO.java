package com.kdt.yts.YouSumback.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class SummaryResponseDTO {
    private Long summaryId;
    private Long transcriptId;
    private Long videoId;

    private String summary;
    private List<String> tags;

    private String title;
    private String thumbnailUrl;
    private String uploaderName;
    private Long viewCount;
    private String languageCode;
    private LocalDateTime createdAt;
}

//    private Long answerId;
//    private String summary;}

