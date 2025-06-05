package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "answer_option")
public class AnswerOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_option_id", nullable = false)
    private Long id; // 답변 선택지 식별자

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // 질문 식별자

    @Column(name = "option_text", columnDefinition = "TEXT", nullable = false)
    private String optionText;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect; // 정답 여부

    @Column(name = "transcript_id", nullable = false)
    private Long transcriptId;

    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    @Column(name = "summary_type", length = 50)
    private String summaryType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public void setIsCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
}

