package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Reminder {
    @Id
    @Column(name = "reminder_id", nullable = false)
    private int reminder_id; // 리마인드 식별자

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자 식별자

    @ManyToOne
    @JoinColumn(name = "user_library", nullable = false)
    private UserLibrary user_library; // 라이브러리 식별자

    @Column(name = "reminder_type", length = 50, nullable = false)
    private String reminder_type; // 알림 주기 유형

    @Column(name = "frequency_interval", nullable = false)
    private int frequency_interval; // 알림 간격

    @Column(name = "day_of_week", nullable = true)
    private int day_of_week; // 주간 반복 시 요일

    @Column(name = "day_of_month", nullable = true)
    private int day_of_month; // 월간 반복 시 날짜

    @Column(name = "base_datetime_for_recurrence", nullable = false)
    private LocalDateTime base_datetime_for_recurrence; // 반복 패턴의 기준이 되는 날짜/시간

    @Column(name = "next_notification_datetome", nullable = false)
    private LocalDateTime next_notification_datetime; // 다음 알림 예정 시간

    @Column(name = "reminder_not", columnDefinition = "TEXT", nullable = true)
    private String reminder_not; // 사용자 메모

    @Column(name = "is_active", nullable = false)
    private boolean is_active; // 알림 활성화 여부

    @Column(name = "create_at", nullable = false)
    private LocalDateTime create_at; // 생성 일시

    @Column(name = "last_sent_at", nullable = true)
    private LocalDateTime last_sent_at; // 마지막 알림 발송 시간

}
