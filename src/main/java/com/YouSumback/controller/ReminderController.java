package com.YouSumback.controller;

import com.YouSumback.model.entity.Reminder;
import com.YouSumback.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ReminderController {
    @Autowired
    ReminderService reminderService;

    // 리마인더 등록
    @PostMapping
    public String createReminder(
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "libraryId", required = true) Long libraryId,
            @RequestParam(value = "reminderType", required = true) String reminderType,
            @RequestParam(value = "frequencyInterval", required = true) int frequencyInterval,
            @RequestParam(value = "dayOfWeek", required = false) Integer dayOfWeek,
            @RequestParam(value = "dayOfMonth", required = false) Integer dayOfMonth,
            @RequestParam(value = "baseDatetimeForRecurrence", required = true) String baseDatetimeForRecurrence,
            @RequestParam(value = "nextNotificationDatetime", required = true) String nextNotificationDatetime,
            @RequestParam(value = "reminderNote", required = false) String reminderNote
    ) {
        // 입력받은 값을 REMINDER 테이블에 입력
        Reminder reminder = reminderService.createReminder(
                userId,
                libraryId,
                reminderType,
                frequencyInterval,
                dayOfWeek != null ? dayOfWeek : 0, // 기본값 설정
                dayOfMonth != null ? dayOfMonth : 0, // 기본값 설정
                baseDatetimeForRecurrence,
                nextNotificationDatetime,
                reminderNote
        );
        if (reminder != null) {
            return "리마인더가 성공적으로 등록되었습니다.";
        } else {
            return "리마인더 등록에 실패했습니다.";
        }
    }

    // 사용자로 리마인더 조회
    @GetMapping("/reminders")
    public List<Reminder> getRemindersByUser(
            @RequestParam(value = "userId", required = true) Long userId
    ) {
        return reminderService.getRemindersByUser(userId);
    }

    // 리마인더 수정
    @PutMapping("/reminder/{reminderId}")
    public String updateReminder(
            @PathVariable Long reminderId,
            @RequestParam(value = "userId", required = true) Long userId,
            @RequestParam(value = "libraryId", required = true) Long libraryId,
            @RequestParam(value = "reminderType", required = true) String reminderType,
            @RequestParam(value = "frequencyInterval", required = true) int frequencyInterval,
            @RequestParam(value = "dayOfWeek", required = false) Integer dayOfWeek,
            @RequestParam(value = "dayOfMonth", required = false) Integer dayOfMonth,
            @RequestParam(value = "baseDatetimeForRecurrence", required = true) String baseDatetimeForRecurrence,
            @RequestParam(value = "nextNotificationDatetime", required = true) String nextNotificationDatetime,
            @RequestParam(value = "reminderNote", required = false) String reminderNote
    ) {
        Reminder updatedReminder = reminderService.updateReminder(
                reminderId,
                userId,
                libraryId,
                reminderType,
                frequencyInterval,
                dayOfWeek != null ? dayOfWeek : 0, // 기본값 설정
                dayOfMonth != null ? dayOfMonth : 0, // 기본값 설정
                baseDatetimeForRecurrence,
                nextNotificationDatetime,
                reminderNote
        );
        if (updatedReminder != null) {
            return "리마인더가 성공적으로 수정되었습니다.";
        } else {
            return "리마인더 수정에 실패했습니다.";
        }
    }

    // 리마인더 삭제
    @DeleteMapping("/reminder/{reminderId}")
    public String deleteReminder(
            @PathVariable Long reminderId
    ) {
        reminderService.deleteReminder(reminderId);
        return "리마인더가 성공적으로 삭제되었습니다.";
    }
}
