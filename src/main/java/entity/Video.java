package entity;

import jakarta.persistence.*;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Video {

    @Id
    @Column (name = "video_id", length = 20, nullable = false)
    private String videoId; // 비디오 아이디

    @Column(name="title", length = 225, nullable = false)
    private String title; // 타이틀 제목

    @Column(name="original_url", length = 2048, nullable = false)
    private String originalUrl; // 원본 주소

    @Column(name="uploader_name", length = 100, nullable = true)
    private String uploaderName; // 업로드 이름

    @Column(name="original_language_code", length = 225, nullable = false)
    private String originalLanguageCode; // 원본 언어 코드

    @Column(name="duration_seconds", nullable = false)
    private int durationSeconds; // 지속 시간
}
