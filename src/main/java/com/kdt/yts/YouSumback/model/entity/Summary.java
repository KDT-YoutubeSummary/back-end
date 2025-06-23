package com.kdt.yts.YouSumback.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "summary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transcript_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_summary_audiotranscript"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AudioTranscript audioTranscript;

    @Lob
    @Column(name = "summary_text", nullable = false, columnDefinition = "LONGTEXT")
    private String summaryText;

    @Lob
    @Column(name = "user_prompt", columnDefinition = "LONGTEXT")
    private String userPrompt;

    @Column(name = "language_code", length = 10, nullable = true)
    private String languageCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 요약 타입을 나타내는 Enum
    @Enumerated(EnumType.STRING)
    @Column(name = "summary_type", length = 50)
    private SummaryType summaryType;

}