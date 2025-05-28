package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Summary")
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id", nullable = false)
    private int summaryId; // 요약 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 식별자

    @ManyToOne
    @JoinColumn(name = "transcript_id", nullable = false)
    private AudioTranscript audioTranscript; // 음성텍스트 식별자

    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText; // 요약 내용

    @Column(name = "language_code", length = 225, nullable = false)
    private String languageCode; // 요약 언어 코드

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 일자

    @Column(name = "summary_type", length = 50, nullable = true)
    private String summaryType; // 요약 유형

}
