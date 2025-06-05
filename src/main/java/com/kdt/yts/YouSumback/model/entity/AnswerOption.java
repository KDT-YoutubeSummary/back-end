package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "answer_option")
public class AnswerOption {
    @Id
    @Column(name = "answer_option_id", nullable = false)
    private Long answerOptionId; // 답변 선택지 식별자

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question; // 질문 식별자

    @Column(name = "option_text", length = 255, nullable = false)
    private String optionText; // 선택지 내용

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect; // 정답 여부
}
