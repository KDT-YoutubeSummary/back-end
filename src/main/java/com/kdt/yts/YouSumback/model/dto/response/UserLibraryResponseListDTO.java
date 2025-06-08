package com.kdt.yts.YouSumback.model.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// UserLibraryResponseListDTO는 사용자의 라이브러리 정보 목록을 응답하기 위한 DTO 클래스입니다.
public class UserLibraryResponseListDTO {

    @JsonProperty("library_id")
    private Long userLibraryId;

    @JsonProperty("summary_id")
    private Long summaryId;

    @JsonProperty("video_title")
    private String videoTitle;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("saved_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime savedAt;

    @JsonProperty("last_viewed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastViewedAt;

    @JsonProperty("user_notes")
    private String userNotes;

    // 썸네일 추가를 위한 유튜브 URL 필드
    @JsonProperty("original_url")
    private String originalUrl;

    public static UserLibraryResponseListDTO fromEntity(UserLibrary entity, List<String> tags) {
        return UserLibraryResponseListDTO.builder()
                .userLibraryId(entity.getId())
                .summaryId(entity.getSummary().getId())
                .videoTitle(entity.getSummary().getAudioTranscript().getVideo().getTitle())
                .tags(tags)
                .savedAt(entity.getSavedAt())
                .lastViewedAt(entity.getLastViewedAt())
                .userNotes(entity.getUserNotes())
                .originalUrl(entity.getSummary().getAudioTranscript().getVideo().getOriginalUrl()) // 유튜브 URL 추가
                .build();
    }
}
