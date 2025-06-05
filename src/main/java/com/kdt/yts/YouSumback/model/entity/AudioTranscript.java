package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
public class AudioTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transcript_id")
    private Integer transcriptId; // 오디오 트랜스크립트 식별자

    @OneToOne
    @JoinColumn(name = "video_id", referencedColumnName = "video_id", nullable = false, unique = true)
    private Video video; // 비디오 식별자 (unique, not null)

    @Column(name = "transcript_text", columnDefinition = "TEXT", nullable = false)
    private String transcriptText; // 추출된 음성 텍스트

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt; // 생성 일자
}
