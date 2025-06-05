package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Integer videoId; // 비디오 식별자 (PK)

    @Column(name = "youtube_id", length = 255, nullable = false, unique = true)
    private String youtubeId; // 유튜브 영상 ID

    @Column(name = "title", length = 255, nullable = false)
    private String title; // 영상 제목

    @Column(name = "original_url", length = 512, nullable = false, unique = true)
    private String originalUrl; // 영상 원본 링크

    @Column(name = "uploader_name", length = 100)
    private String uploaderName; // 채널명 (업로더)

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl; // 썸네일 이미지 URL

    @Column(name = "view_count")
    private Long viewCount; // 조회수

    @Column(name = "published_at")
    private java.time.LocalDateTime publishedAt; // 영상 업로드 시각
}
