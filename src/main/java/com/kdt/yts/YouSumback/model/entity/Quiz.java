package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id", nullable = false)
    private int quizId; // 퀴즈 식별자

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();


    @ManyToOne
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary; // 요약 식별자

    @Column(name = "title", length = 255, nullable = true)
    private String title; // 퀴즈 제목

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 일시

    public int getQuizId() {
        return quizId;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
