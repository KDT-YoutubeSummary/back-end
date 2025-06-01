package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class AudioTranscript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id; // 음성텍스트 식별자

    @Column(name = "transcript_text", columnDefinition = "LONGTEXT", nullable = false)
    private String transcriptText; // 추출된 음성 텍스트

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt; // 생성 일자

    @ManyToOne
    @JoinColumn(name = "video_id", nullable = false)
    private Video video; // 비디오 식별자
}
