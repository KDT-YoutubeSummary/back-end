package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity
public class Video {
    @Id
    @Column(name = "video_id", length = 30, nullable = false)
    private String videoId; // 비디오 식별자

    @Column(name = "title", length = 255, nullable = false)
    private String title; // 비디오 제목

    @Column(name = "original_url", length = 2048, nullable = false)
    private String originalUrl; // 원본 URL

    @Column(name = "uploader_name", length = 100, nullable = true)
    private String uploaderName; // 업로드명

    @Column(name = "original_language_code", length = 255, nullable = false)
    private String originalLanguageCode; // 원본 언어 코드

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds; // 영상 길이 (초)
}
