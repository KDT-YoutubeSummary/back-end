package com.kdt.yts.YouSumback.model.dto.request;

import com.kdt.yts.YouSumback.model.entity.ReminderType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderCreateRequestDTO {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId; // 리마인더를 생성할 사용자의 ID

    @NotNull(message = "요약 저장소 ID는 필수입니다")
    private Long summaryArchiveId; // 리마인더를 연결할 요약 저장소 항목의 ID

    @NotNull(message = "리마인더 타입은 필수입니다")
    private ReminderType reminderType; // 리마인더의 반복 타입 (Enum: ONE_TIME, DAILY, WEEKLY 등)

    @NotBlank(message = "리마인더 메시지는 필수입니다")
    private String reminderMessage;

    @NotNull(message = "리마인더 시간은 필수입니다")
    private LocalDateTime reminderTime;

    private Integer frequencyInterval; // 반복 주기의 간격 (예: 2일마다, 3주마다)
    private Integer dayOfWeek; // 주간 반복 시 알림이 울릴 요일. null 허용 (선택 사항)
    private Integer dayOfMonth; // 월간 반복 시 알림이 울릴 일자. null 허용 (선택 사항)
    private LocalDateTime baseDatetimeForRecurrence; // 리마인더의 첫 알림 또는 반복 시작 기준 날짜/시간
    private String reminderNote; // 리마인더에 대한 사용자 메모. null 허용
    private Boolean isActive = true; // 리마인더 활성화 여부. 기본값은 true
}