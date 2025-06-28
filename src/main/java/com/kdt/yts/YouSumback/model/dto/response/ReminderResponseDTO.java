package com.kdt.yts.YouSumback.model.dto.response;

import com.kdt.yts.YouSumback.model.entity.Reminder;
import com.kdt.yts.YouSumback.model.entity.ReminderType;
import com.kdt.yts.YouSumback.model.entity.User;
import com.kdt.yts.YouSumback.model.entity.SummaryArchive;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponseDTO {

    private Long reminderId; // 리마인더 고유 ID
    private Long user_id; // 리마인더를 소유한 사용자 ID
    private Long summaryArchiveId; // 리마인더가 연결된 요약 저장소 항목 ID
    private ReminderType reminderType; // 리마인더 타입
    private Integer frequencyInterval; // 반복 간격
    private Integer dayOfWeek; // 주중 요일
    private Integer dayOfMonth; // 월중 일자
    private LocalDateTime baseDatetimeForRecurrence; // 반복 기준 날짜/시간
    private LocalDateTime nextNotificationDatetime; // 다음 알림 예정 날짜/시간
    private String reminderNote; // 리마인더 메모
    private Boolean isActive; // 활성화 여부
    private LocalDateTime createdAt; // 생성 시간
    private LocalDateTime lastSentAt; // 마지막 알림 발송 시간

    public ReminderResponseDTO(Reminder reminder) {
        if (reminder == null) {
            throw new IllegalArgumentException("Reminder cannot be null");
        }
        
        this.reminderId = reminder.getId();
        this.reminderType = reminder.getReminderType();
        this.frequencyInterval = reminder.getFrequencyInterval();
        this.dayOfWeek = reminder.getDayOfWeek();
        this.dayOfMonth = reminder.getDayOfMonth();
        this.baseDatetimeForRecurrence = reminder.getBaseDatetimeForRecurrence();
        this.nextNotificationDatetime = reminder.getNextNotificationDatetime();
        this.reminderNote = reminder.getReminderNote();
        this.isActive = reminder.getIsActive();
        this.createdAt = reminder.getCreatedAt();
        this.lastSentAt = reminder.getLastSentAt();
        
        // ✅ 안전한 User 접근 - Lazy Loading 고려
        try {
            User user = reminder.getUser();
            this.user_id = (user != null) ? user.getId() : null;
        } catch (org.hibernate.LazyInitializationException e) {
            this.user_id = null;
            System.err.println("User Lazy Loading 실패: " + e.getMessage());
        }
        
        // ✅ 안전한 SummaryArchive 접근 - Lazy Loading 고려
        try {
            SummaryArchive summaryArchive = reminder.getSummaryArchive();
            this.summaryArchiveId = (summaryArchive != null) ? summaryArchive.getId() : null;
        } catch (org.hibernate.LazyInitializationException e) {
            this.summaryArchiveId = null;
            System.err.println("SummaryArchive Lazy Loading 실패: " + e.getMessage());
        }
    }
}