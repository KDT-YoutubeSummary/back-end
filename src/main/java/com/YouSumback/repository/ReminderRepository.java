package com.YouSumback.repository;

import com.YouSumback.model.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // 날짜와 시간 정보
import java.util.List; // 리스트 컬렉션
import com.YouSumback.model.entity.User;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUser_Id(Long userId);
    List<Reminder> findByIsActiveTrueAndNextNotificationDatetimeLessThanEqual(LocalDateTime localDateTime);
}