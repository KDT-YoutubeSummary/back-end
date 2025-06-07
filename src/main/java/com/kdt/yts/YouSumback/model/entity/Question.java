package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import java.util.List;

import lombok.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "question")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id", nullable = false)
    private Long id; // 질문 식별자

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "language_code", length = 255, nullable = false)
    private String languageCode;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<AnswerOption> options;

    @Column(length = 255)
    private String explanation; // 질문에 대한 설명 (선택적)

    // 생성자, 게터, 세터 등은 Lombok 어노테이션으로 자동 생성됨
//    public void setQuiz(Quiz quiz) {
//        this.quiz = quiz;
//    }
//
//    public void setQuestionText(String questionText) {
//        this.questionText = questionText;
//    }
//
//    public void setLanguageCode(String languageCode) {
//        this.languageCode = languageCode;
//    }
//
//    public void setOptions(List<AnswerOption> options) {
//        this.options = options;
//    }
}
