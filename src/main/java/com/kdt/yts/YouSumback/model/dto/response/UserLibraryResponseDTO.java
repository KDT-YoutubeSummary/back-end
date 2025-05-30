package com.kdt.yts.YouSumback.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kdt.yts.YouSumback.model.entity.Tag;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder

public class UserLibraryResponseDTO {
    private Long userLibraryId; // 사용자 라이브러리 식별자

    @JsonProperty("user_id")
    private int userId; // 사용자 식별자

    @JsonProperty("summary_id")
    private int summaryId; // 요약 식별자

    @JsonProperty("video_title")
    private String videoTitle; // 비디오 제목

    @JsonProperty("tags")
    private List<String> tags; // 태그 목록

//    @JsonProperty("user_notes")
//    private String userNotes;
    @JsonProperty("saved_at")  // 저장 일시
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime savedAt;

    @JsonProperty("last_viewed_at") // 최근 시청 일시
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastViewedAt;


    public static UserLibraryResponseDTO fromEntity(UserLibrary entity, List<String> tags) {
        return UserLibraryResponseDTO.builder()
                .userLibraryId((long) entity.getUserLibraryId()) // 사용자 라이브러리 식별자
                .userId(entity.getUser().getUserId()) // 사용자 식별자
                .summaryId(entity.getSummary().getSummaryId()) // 요약 식별자
                // .userNotes(entity.getUserNotes())
                .videoTitle(entity.getSummary().getAudioTranscript().getVideo().getTitle()) // 비디오 제목
                .tags(tags)
                .savedAt(entity.getSavedAt())
                .lastViewedAt(entity.getLastViewedAt())
                .build();
    }

}