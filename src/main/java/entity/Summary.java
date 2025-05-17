package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Summary {

    @Id
    @Column (name="summary_id", nullable = false)
    private long summaryId; // 요약 아이디

    @ManyToOne
    @JoinColumn(name = "vide_id")
    private Video video; // 비디오 식별자 참조

    @ManyToOne
    @JoinColumn(name = "user_library_id")
    private UserLibrary userLibrary; // 유저 라이브러리 참조

    @Column (name = "summary_text", columnDefinition = "TEXT", nullable = false)
    private String summaryText; // 요약 텍스트

    @Column (name= "language_code", length = 255, nullable = false)
    private String languageCode; // 언어 코드

    @Column (name= "generated_at", nullable = false)
    private LocalDateTime generatedAt; // 생성 시간

    @Column (name= "summary_type", length = 255, nullable = true)
    private String  summaryType; // 요약타입
}
