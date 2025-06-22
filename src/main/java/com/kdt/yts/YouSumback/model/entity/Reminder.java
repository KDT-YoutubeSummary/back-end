package com.kdt.yts.YouSumback.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate; // 엔티티 생성 시 자동으로 현재 시간을 주입
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "reminder")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "summary_archive_id", nullable = false)
    private SummaryArchive summaryArchive;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 50) // 리마인더의 반복 타입 (예: ONE_TIME, DAILY, WEEKLY 등)
    private ReminderType reminderType;

    @Column(name = "frequency_interval") // 반복 주기의 간격 (예: DAILY 타입일 때 2이면 2일마다)
    private Integer frequencyInterval;

    @Column(name = "day_of_week") // 주간 반복 시 알림이 울릴 요일 (1=월요일, 7=일요일) / 선택 사항
    private Integer dayOfWeek;

    @Column(name = "day_of_month") // 월간 반복 시 알림이 울릴 일자 (1~31) /선택 사항
    private Integer dayOfMonth;

    @Column(name = "base_datetime_for_recurrence", nullable = false) // 반복 리마인더의 기준이 되는 날짜 및 시간
    private LocalDateTime baseDatetimeForRecurrence;

    @Column(name = "next_notification_datetime", nullable = false) // 다음 알림이 발송될 예정 날짜 및 시간
    private LocalDateTime nextNotificationDatetime;

    @Column(name = "reminder_note", columnDefinition = "TEXT") // 리마인더에 대한 사용자의 메모
    private String reminderNote;

    @Column(name = "is_active", nullable = false) // 리마인더의 활성화 여부 (true: 활성, false: 비활성)
    private Boolean isActive;

    @CreatedDate // 자동으로 현재 시간으로 설정 (최초 생성 시간)
    @Column(name = "created_at", nullable = false, updatable = false) // 생성 시간. 한 번 생성되면 업데이트되지 않음
    private LocalDateTime createdAt;

    @Column(name = "last_sent_at") // 마지막으로 알림이 발송된 시간
    private LocalDateTime lastSentAt;
}