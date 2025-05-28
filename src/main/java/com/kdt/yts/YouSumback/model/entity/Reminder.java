package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Reminder {
    @Id
    @Column(name = "reminder_id", nullable = false)
    private int reminderId; // 리마인드 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 식별자

    @ManyToOne
    @JoinColumn(name = "user_library", nullable = false)
    private UserLibrary userLibrary; // 라이브러리 식별자

    @Column(name = "reminder_type", length = 50, nullable = false)
    private String reminderType; // 알림 주기 유형

    @Column(name = "frequency_interval", nullable = false)
    private int frequencyInterval; // 알림 간격

    @Column(name = "day_of_week", nullable = true)
    private int dayOfWeek; // 주간 반복 시 요일

    @Column(name = "day_of_month", nullable = true)
    private int dayOfMonth; // 월간 반복 시 날짜

    @Column(name = "base_datetime_for_recurrence", nullable = false)
    private LocalDateTime baseDatetimeForRecurrence; // 반복 패턴의 기준이 되는 날짜/시간

    @Column(name = "next_notification_datetome", nullable = false)
    private LocalDateTime nextNotificationDatetime; // 다음 알림 예정 시간

    @Column(name = "reminder_note", columnDefinition = "TEXT", nullable = true)
    private String reminderNote; // 사용자 메모

    @Column(name = "is_active", nullable = false)
    private boolean isActive; // 알림 활성화 여부

    @Column(name = "create_at", nullable = false)
    private LocalDateTime createAt; // 생성 일시

    @Column(name = "last_sent_at", nullable = true)
    private LocalDateTime lastSentAt; // 마지막 알림 발송 시간

}
