package com.YouSumback.model.entity;

import jakarta.persistence.*;

@Entity
public class Question {
    @Id
    @Column(name = "question_id", nullable = false)
    private int questionId; // 질문 식별자

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz; // 퀴즈 식별자

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText; // 질문 내용

    @Column(name = "language_code", length = 255, nullable = false)
    private String languageCode; // 질문 언어 코드
}
