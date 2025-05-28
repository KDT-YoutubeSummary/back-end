package com.kdt.yts.YouSumback.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kdt.yts.YouSumback.model.entity.UserLibrary;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder

public class UserLibraryResponseDTO {
    private Long userLibraryId;
    @JsonProperty("user_id")
    private int userId;
    @JsonProperty("summary_id")
    private int summaryId;
    @JsonProperty("user_notes")
    private String userNotes;
    @JsonProperty("saved_at")
    private LocalDateTime savedAt;
    @JsonProperty("last_viewed_at")
    private LocalDateTime lastViewedAt;

    public static UserLibraryResponseDTO fromEntity(UserLibrary entity) {
        return UserLibraryResponseDTO.builder()
                .userLibraryId((long) entity.getUserLibraryId())
                .userId(entity.getUser().getUserId())
                .summaryId(entity.getSummary().getSummaryId())
                .userNotes(entity.getUserNotes())
                .savedAt(entity.getSavedAt())
                .lastViewedAt(entity.getLastViewedAt())
                .build();
    }

}