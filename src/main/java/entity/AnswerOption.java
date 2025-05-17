package entity;

import jakarta.persistence.*;

@Entity
public class AnswerOption {

    public AnswerOption(){}

    @Id
    @Column(name = "answer_option_id", nullable = false)
    private long answerOptionId; // 답변 선택지 식별자

    @ManyToOne
    @JoinColumn (name ="question_id")
    private Question question; // 질문 식별자

    @Column (name = "option_text", columnDefinition = "TEXT", nullable = false)
    private String optionText; // 선택지 내용

    @Column (name = "is_correct", nullable = false)
    private boolean isCorrect; // 정답 여부
}
