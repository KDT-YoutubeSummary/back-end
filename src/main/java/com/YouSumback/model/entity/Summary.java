package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long summaryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcript_id")
    private AudioTranscript audioTranscript;

    @Column(nullable = false)
    private String summaryText;

    @Column(nullable = false)
    private String languageCode;

    @Column(name = "summary_type", length = 50)
    private String summaryType;  // 요약 유형

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 생성 일자
}
