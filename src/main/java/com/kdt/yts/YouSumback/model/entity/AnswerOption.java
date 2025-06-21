package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "answer_option")
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_option_id", nullable = false)
    private Long id; // 답변 선택지 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", columnDefinition = "TEXT", nullable = false)
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "transcript_id", nullable = false)
    private Long transcriptId;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Enumerated(EnumType.STRING)
    @Column(name = "summary_type")
    private SummaryType summaryType;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
