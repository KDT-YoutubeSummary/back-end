package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
public class UserLibrary {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_library_id")
    private Long userLibraryId; // 리마인더에서 참조하는 user_library_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // UserLibrary가 참조하는 User@

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_id", nullable = false)
    private Summary summary; // UserLibrary가 참조하는 Summary

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt; // 저장 일시

    @Column(name = "last_viewed_at", nullable = true)
    private LocalDateTime lastViewedAt; // 최근 시청 일시

    @Column(name = "user_notes", columnDefinition = "TEXT", nullable = true)
    private String userNotes; // 사용자 메모


}
