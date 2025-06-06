package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "video_recommendation")
public class VideoRecommendation{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "source_video_id")
    private Video sourceVideo; // 추천되는 영상

    @ManyToOne
    @JoinColumn(name = "recommended_video_id", nullable = false)
    private Video recommendedVideo; // 추천의 계기가 된 영상

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason; // 추천 이유

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(); // 추천 생성 시간 (기본값: 현재 시간)

    @Column(name = "is_clicked")
    private boolean isClicked = false; // 사용자가 추천 영상 클릭 여부 (기본값: false)

    @Column(name = "clicked_at")
    private LocalDateTime clickedAt; // 클릭 시간 (nullable)
}
