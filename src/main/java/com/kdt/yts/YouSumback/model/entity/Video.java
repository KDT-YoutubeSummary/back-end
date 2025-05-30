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
    private String videoId; // UUID로 생성

    @Column(name = "youtube_id", nullable = false, unique = true, length = 255)
    private String youtubeId; // YouTube 영상 ID

    @Column(nullable = false, length = 255)
    private String title; // 영상 제목

    @Column(name = "original_url", nullable = false, unique = true, length = 2048)
    private String originalUrl; // 영상 원본 링크

    @Column(name = "uploader_name", length = 100)
    private String uploaderName; // 채널명

    @Lob
    @Column(name = "thumbnail_url")
    private String thumbnailUrl; // 썸네일 URL

    @Column(name = "view_count")
    private Long viewCount; // 조회수

    @Column(name = "published_at")
    private LocalDateTime publishedAt; // 업로드 날짜
}
