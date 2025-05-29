package com.YouSumback.model.dto.request;

import com.YouSumback.model.entity.ReminderType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderUpdateRequest {
    private ReminderType reminderType;
    @Min(value = 1, message = "반복 간격은 1 이상이어야 합니다.")
    private Integer frequencyInterval;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    @FutureOrPresent(message = "반복 기준 날짜/시간은 현재 또는 미래여야 합니다.")
    private LocalDateTime baseDatetimeForRecurrence;
    private String reminderNote;
    private Boolean isActive; // 리마인더 활성화/비활성화
}