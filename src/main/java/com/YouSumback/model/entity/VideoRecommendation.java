package com.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class VideoRecommendation {
    @Id
    @Column(name = "recommendation_id", nullable = false)
    private long recommendationId; // 영상 추천 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 추천을 받는 사용자

    @ManyToOne
    @JoinColumn(name = "source_video_id", nullable = false)
    private Video video; // 추천되는 영상

    @ManyToOne
    @JoinColumn(name = "recommended_video_id", nullable = true)
    private Video video2; // 추천의 계기가 된 영상

    @Column(name = "recommendation_ai_version", length = 50, nullable = true)
    private String recommendationAiVersion; // 추천을 생성한 AI 모델/버전

    @Column(name = "recommendation_reason", columnDefinition = "TEXT", nullable = true)
    private String recommendationReason; // 추천 이유

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt; // 추천 생성 시간

    @Column(name = "is_clicked", nullable = false)
    private boolean isClicked; // 사용자가 추천영상 클릭여부

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt; // 클릭 시간
}
