package com.YouSumback.model.dto.response;

import com.YouSumback.model.entity.ReminderType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponse {

    private Long reminderId; // 리마인더 고유 ID
    private Long user_id; // 리마인더를 소유한 사용자 ID
    private Long userLibraryId; // 리마인더가 연결된 사용자 라이브러리 항목 ID
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

    public ReminderResponse(com.YouSumback.model.entity.Reminder reminder) {
        this.reminderId = reminder.getReminderId();
        this.user_id = reminder.getUser() != null ? reminder.getUser().getId() : null;
        this.userLibraryId = reminder.getUserLibrary() != null ? reminder.getUserLibrary().getUserLibraryId() : null;
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
    }
}