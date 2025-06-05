package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;

import java.sql.Timestamp;

@Entity
public class UserActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_type", length = 50, nullable = false)
    private String activityType;

    @Column(name = "target_entity_type", length = 50)
    private String targetEntityType;

    @Column(name = "target_entity_id_str", length = 255)
    private String targetEntityIdStr;

    @Column(name = "target_entity_id_int")
    private Long targetEntityIdInt;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "activity_detail", columnDefinition = "TEXT")
    private String activityDetail;

    @Column(name = "details", columnDefinition = "JSON")
    private String details;
}
