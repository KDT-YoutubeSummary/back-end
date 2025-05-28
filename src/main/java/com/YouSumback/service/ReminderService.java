package com.YouSumback.service;

import com.YouSumback.model.entity.Reminder;
import com.YouSumback.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReminderService {
    @Autowired
    private ReminderRepository reminderRepository;

    // 리마인더 등록
    public Reminder createReminder(
            Long userId,
            Long userLibraryId,
            String reminderType,
            int frequencyInterval,
            int dayOfWeek,
            int dayOfMonth,
            String baseDatetimeForRecurrence,
            String nextNotificationDatetime,
            String reminderNot
    ) {
        return reminderRepository.createReminder(userId, userLibraryId, reminderType, frequencyInterval,
                dayOfWeek, dayOfMonth, baseDatetimeForRecurrence, nextNotificationDatetime, reminderNot);
    }

    // 사용자로 리마인더 조회
    public List<Reminder> getRemindersByUser(Long userId) {
        return reminderRepository.findByUser(userId);
    }

    // 리마인더 수정
    public Reminder updateReminder(
            Long reminderId,
            Long userId,
            Long userLibraryId,
            String reminderType,
            int frequencyInterval,
            int dayOfWeek,
            int dayOfMonth,
            String baseDatetimeForRecurrence,
            String nextNotificationDatetime,
            String reminderNot
    ) {
        return reminderRepository.updateReminder(reminderId, userId, userLibraryId, reminderType, frequencyInterval,
                dayOfWeek, dayOfMonth, baseDatetimeForRecurrence, nextNotificationDatetime, reminderNot);
    }

    // 리마인더 삭제
    public void deleteReminder(Long reminderId) {
        reminderRepository.deleteById(reminderId);
    }

}
