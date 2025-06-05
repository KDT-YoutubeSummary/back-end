package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Video")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @Column(name = "video_id", length = 255)
    private String videoId;

    @Column(name = "youtube_id", length = 255, nullable = false, unique = true)
    private String youtubeId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "original_url", length = 2048, nullable = false, unique = true)
    private String originalUrl;

    @Column(name = "uploader_name", length = 100)
    private String uploaderName;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "original_language_code", length = 255, nullable = false)
    private String originalLanguageCode; // 원본 언어 코드

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds; // 영상 길이 (초)
}
