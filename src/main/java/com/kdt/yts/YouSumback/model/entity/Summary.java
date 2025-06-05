package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Getter
@Setter
public class Summary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Integer summaryId; // 요약 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 식별자

    @ManyToOne
    @JoinColumn(name = "transcript_id", nullable = false)
    private AudioTranscript audioTranscript; // 오디오 트랜스크립트 식별자

    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText; // 요약 내용

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt; // 사용자 프롬프트

    @Column(name = "language_code", length = 10, nullable = false)
    private String languageCode; // 요약 언어 코드

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt; // 생성 일자

    @Column(name = "summary_type", length = 50)
    private String summaryType; // 요약 유형
}
