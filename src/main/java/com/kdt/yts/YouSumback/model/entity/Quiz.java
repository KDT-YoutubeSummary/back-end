package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Integer quizId; // 퀴즈 식별자

    @ManyToOne
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary; // 요약 식별자

    @Column(name = "title", length = 255)
    private String title; // 퀴즈 제목

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt; // 생성 일시

}
