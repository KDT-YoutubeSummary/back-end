package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Getter
@Entity
@Setter
public class Reminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Integer reminderId; // 리마인더 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 식별자

    @ManyToOne
    @JoinColumn(name = "user_library_id", nullable = false)
    private UserLibrary userLibrary; // 라이브러리 식별자

    @Column(name = "reminder_type", length = 50, nullable = false)
    private String reminderType; // 알림 주기 유형

    @Column(name = "frequency_interval", nullable = false)
    private Integer frequencyInterval = 1; // 알림 간격 (기본값 1)

    @Column(name = "day_of_week")
    private Integer dayOfWeek; // 주간 반복 시 요일

    @Column(name = "day_of_month")
    private Integer dayOfMonth; // 월간 반복 시 날짜

    @Column(name = "base_datetime_for_recurrence", nullable = false)
    private Timestamp baseDatetimeForRecurrence; // 반복 패턴 기준 날짜/시간

    @Column(name = "next_notification_datetime", nullable = false)
    private Timestamp nextNotificationDatetime; // 다음 알림 예정 시간

    @Column(name = "reminder_note", columnDefinition = "TEXT")
    private String reminderNote; // 리마인더 메모

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // 활성화 여부 (기본값 true)

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt; // 생성 일자

    @Column(name = "last_sent_at")
    private Timestamp lastSentAt; // 마지막 발송 일시
}
