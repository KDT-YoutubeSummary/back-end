package com.YouSumback.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class UserActivityLog {

    @Id
    @Column(name = "log_id", nullable = false)
    private int log_id; // 로그 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 활동을 수행한 사용자

    @Column(name = "activity_type", length = 50, nullable = false)
    private String activityType; // 활동 유형

    @Column(name = "target_entity_type", length = 50, nullable = true)
    private String targetEntityType; // 활동 대상 유형

    @Column(name = "target_entity_id_str", length = 255, nullable = true)
    private String targetEntityIdStr; // 활동 대상 문자열 ID

    @Column(name = "target_entity_id_int", nullable = true)
    private int targetEntityIdInt; // 활동 대상 정수형 ID

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 활동 발생 일자

    @Column(name = "activity_detail", columnDefinition = "TEXT", nullable = true)
    private String activityDetail; // 활동 상세 내용
}
