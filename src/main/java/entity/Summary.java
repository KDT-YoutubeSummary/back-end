package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Summary {

    @Id
    @Column(name = "summary_id", nullable = false)
    private int summary_id; // 요약 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 식별자

    @ManyToOne
    @JoinColumn(name = "transcript_id", nullable = false)
    private AudioTranscript audioTranscript; // 음성텍스트 식별자

    @Column(name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summary_text; // 요약 내용

    @Column(name = "language_code", length = 225, nullable = false)
    private String language_code; // 요약 언어 코드

    @Column(name = "create_at", nullable = false)
    private LocalDateTime create_at; // 생성 일자

    @Column(name = "summary_type", length = 50, nullable = true)
    private String summary_type; // 요약 유형
}
