package com.kdt.yts.YouSumback.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Summary")
public class Summary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Integer summaryId;    // Long → Integer


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcript_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AudioTranscript audioTranscript;

    /** DDL: summary_text TEXT NOT NULL */
    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    /** DDL: language_code VARCHAR(10) NOT NULL */
    @Column(name = "language_code", length = 10, nullable = false)
    private String languageCode;

    /** DDL: summary_type VARCHAR(50) */
    @Column(name = "summary_type", length = 50)
    private String summaryType;

    /**
     * DDL: created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * @Builder.Default을 쓰면 Builder로 생성 시에도 현재 시각이 기본값이 됩니다.
     */
    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
