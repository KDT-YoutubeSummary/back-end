package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class UserLibrary {
    @Id
    @Column(name = "user_library_id", nullable = false)
    private int userLibraryId; // 라이브러리 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 유저 ID

    @ManyToOne
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary; // 요약 식별자

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt; // 저장 일시

    @Column(name = "last_viewed_at", nullable = true)
    private LocalDateTime lastViewedAt; // 최근 시청 일시

    @Column(name = "user_notes", columnDefinition = "TEXT", nullable = true)
    private String userNotes; // 사용자 메모


}
