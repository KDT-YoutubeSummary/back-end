package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Table(name = "audiotranscript")
@Entity
public class AudioTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_id", nullable = false)
    private long transcriptId; // 음성텍스트 식별자

    @Column(name = "transcript_text", columnDefinition = "TEXT", nullable = false)
    private String transcriptText; // 추출된 음성 텍스트

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createAt; // 생성 일자

    @ManyToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video; // 비디오 식별자
}
