package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Quiz {
    @Id
    @Column(name = "quiz_id", nullable = false)
    private Long quizId; // 퀴즈 식별자

    @ManyToOne
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary; // 요약 식별자

    @Column(name = "title", length = 255, nullable = true)
    private String title; // 퀴즈 제목

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 일시

}
