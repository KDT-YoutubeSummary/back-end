package com.kdt.yts.YouSumback.model.dto.request;

import com.kdt.yts.YouSumback.model.entity.ReminderType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderUpdateRequestDTO {
    private ReminderType reminderType;
    private Integer frequencyInterval;
    private Integer dayOfWeek;
    private Integer dayOfMonth;
    private LocalDateTime baseDatetimeForRecurrence;
    private String reminderNote;
    private Boolean isActive; // 리마인더 활성화/비활성화
}