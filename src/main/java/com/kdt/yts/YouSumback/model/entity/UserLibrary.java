package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class UserLibrary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_library_id")
    private Integer userLibraryId; // 라이브러리 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 유저 ID

    @ManyToOne
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary; // 요약 식별자

    @Column(name = "saved_at", nullable = false)
    private java.sql.Timestamp savedAt; // 저장 일시

    @Column(name = "user_notes", columnDefinition = "TEXT")
    private String userNotes; // 사용자 메모

    @Column(name = "last_viewed_at")
    private java.sql.Timestamp lastViewedAt; // 최근 시청 일시
}
