package com.kdt.yts.YouSumback.model.entity;

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

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "transcript_id", nullable = false)
    private AudioTranscript transcript;

    @Lob
    @Column(name = "summary_text", nullable = false)
    private String summaryText;

    @Lob
    @Column(name = "user_prompt")
    private String userPrompt;

    @Column(name = "language_code", length = 10, nullable = false)
    private String languageCode;

    @Column(name = "summary_type", length = 50)
    private String summaryType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}