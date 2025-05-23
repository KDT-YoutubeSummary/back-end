package com.YouSumback.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class AudioTranscript {
    @Id
    @Column(name = "transcript_id", nullable = false)
    private long transcript_id; // 음성텍스트 식별자

    @Column(name = "transcript_text", columnDefinition = "TEXT", nullable = false)
    private String transcript_text; // 추출된 음성 텍스트

    @Column(name = "create_at", nullable = false)
    private LocalDateTime create_at; // 생성 일자

    @OneToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video; // 비디오 식별자
}
