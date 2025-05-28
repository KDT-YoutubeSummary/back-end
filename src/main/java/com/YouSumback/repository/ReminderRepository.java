package com.YouSumback.repository;

import com.YouSumback.model.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    // 리마인더 등록
    Reminder createReminder(
            Long userId,
            Long userLibraryId,
            String reminderType,
            int frequencyInterval,
            int dayOfWeek,
            int dayOfMonth,
            String baseDatetimeForRecurrence,
            String nextNotificationDatetime,
            String reminderNot
    );

    // 사용자로 리마인더 조회
    List<Reminder> findByUser(Long user); // 사용자 ID로 리마인더 조회

    // 리마인더 수정
    Reminder updateReminder(
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
    );

    // 리마인더 삭제
    void deleteById(Long reminderId); // 리마인더 ID로 삭제
}