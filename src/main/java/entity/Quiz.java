package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Quiz {

    @Id
    @Column(name = "quiz_id", nullable = false)
    private long quizId; // 퀴즈 아이디

    @ManyToOne
    @JoinColumn(name="summary_id")
    private Summary summary; // summary 참조

    @Column(name = "quiz_title", length = 255, nullable = true)
    private String quizTitle;

    @Column (name= "created_at")
    private LocalDateTime createdAt;


}
