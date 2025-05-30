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
public class UserLibraryResponseDTO {

    @JsonProperty("library_id")
    private Long userLibraryId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("summary_id")
    private Long summaryId;

    @JsonProperty("video_title")
    private String videoTitle;

    @JsonProperty("summary_text")
    private String summaryText;

    @JsonProperty("tags")
    private List<String> tags;

    // 📺 비디오 메타데이터
    @JsonProperty("youtube_id")
    private String youtubeId;

    @JsonProperty("original_url")
    private String originalUrl;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    @JsonProperty("uploader_name")
    private String uploaderName;

    @JsonProperty("view_count")
    private Long viewCount;

    @JsonProperty("published_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonProperty("saved_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime savedAt;

    @JsonProperty("last_viewed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastViewedAt;

    @JsonProperty("user_notes")
    private String userNotes;

    public static UserLibraryResponseDTO fromEntity(UserLibrary entity, List<String> tags) {
        var summary = entity.getSummary();
        var transcript = summary.getTranscript();
        var video = transcript.getVideo();

        return UserLibraryResponseDTO.builder()
                .userLibraryId(entity.getUserLibraryId())
                .userId(entity.getUser().getUserId())
                .summaryId(summary.getSummaryId())
                .videoTitle(video.getTitle())
                .summaryText(summary.getSummaryText())
                .tags(tags)

                // 비디오 메타데이터
                .youtubeId(video.getYoutubeId())
                .originalUrl(video.getOriginalUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .uploaderName(video.getUploaderName())
                .viewCount(video.getViewCount())
                .publishedAt(video.getPublishedAt())

                .savedAt(entity.getSavedAt())
                .lastViewedAt(entity.getLastViewedAt())
                .userNotes(entity.getUserNotes())
                .build();
    }
}
