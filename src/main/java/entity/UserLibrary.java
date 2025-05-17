package entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class UserLibrary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long userLibraryId; // 유저 라이브러리 아이디

    @ManyToOne
    @JoinColumn(name ="user_id", nullable = false)
    private User user; // 유저 아이디 참조

    @ManyToOne
    @JoinColumn(name="summary_id")
    private Summary summary; // 비디오 아이디 참조

    @Column(name="saved_at", nullable = true)
    private LocalDateTime savedAt; // 저장 시간

    @Column(name="last_viewed_at", nullable = true)
    private LocalDateTime lastViewedAt; // 마지막 시청 시간

    @Column(name= "user_notes", columnDefinition = "TEXT", nullable = true)
    private String userNotes;  // 유저 노트
}
